#!/usr/bin/env python3
"""Build a real signed AAB and its signed, bundle-derived APK/test pair.

Every attempt retains its source manifest, logs, signatures and hashes. Signing
does not imply device qualification or an upload. The private key stays external.
"""
import argparse
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import uuid
import xml.etree.ElementTree as ET
import zipfile
from build_android import ROOT, APP_ID, atomic_json, digest, sdk_dir, sdk_tool, source_manifest, verify_local_flex, verify_badging
from qa.check_graphics_runtime import verify_graphics
from qa.check_litert_runtime import verify_litert
from qa.check_monetization_free_apk import audit


def payload(archive):
    with zipfile.ZipFile(archive) as entries:
        return {name: hashlib.sha256(entries.read(name)).hexdigest()
                for name in entries.namelist() if not name.startswith('META-INF/')}


def build_bundle(bundletool, gradle_properties=(), suffix='', optional_sdks=False, metadata=None):
    names = ('VDS_RELEASE_KEYSTORE', 'VDS_RELEASE_KEY_ALIAS',
             'VDS_RELEASE_STORE_PASSWORD', 'VDS_RELEASE_KEY_PASSWORD')
    if any(not os.environ.get(name) for name in names):
        raise ValueError('The external signing environment is required.')
    key = Path(os.environ[names[0]]).resolve()
    if not key.is_file() or key.is_relative_to(ROOT):
        raise ValueError('The private signing key must remain outside this checkout.')
    bundletool = bundletool.resolve()
    if not bundletool.is_file():
        raise ValueError('The official bundletool CLI is required.')
    java = Path(os.environ['JAVA_HOME']) / ('bin/java.exe' if os.name == 'nt' else 'bin/java')
    signer = java.with_name('jarsigner.exe' if os.name == 'nt' else 'jarsigner')
    keytool = java.with_name('keytool.exe' if os.name == 'nt' else 'keytool')
    sdk = sdk_dir(ROOT, os.environ) / 'build-tools/36.0.0'
    pin = json.loads((ROOT / 'config/release-signing.json').read_text(encoding='utf-8'))
    run = datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%SZ-') + uuid.uuid4().hex[:12]
    out = ROOT / 'dist/app-bundle/runs' / run
    out.mkdir(parents=True, exist_ok=False)
    state = dict(schema=1, outcome='running', application_id=APP_ID, device_qualified=False,
                 uploaded=False, bundletool_sha256=digest(bundletool),
                 recipe_sha256=digest(Path(__file__)), started_at=datetime.now(timezone.utc).isoformat())
    state.update(metadata or {})
    atomic_json(out / 'status.json', state)

    def command(name, parts):
        with (out / (name + '.log')).open('wb') as log:
            result = subprocess.run(parts, cwd=ROOT, stdout=log, stderr=subprocess.STDOUT)
        if result.returncode:
            raise RuntimeError(name + ' failed; inspect the retained log.')
        return (out / (name + '.log')).read_text(encoding='utf-8', errors='replace')

    def sign_apk(source, label):
        aligned = out / (label + '-aligned.apk')
        target = out / (label + '.apk')
        command(label + '-align', [str(sdk_tool(sdk, 'zipalign')), '-P', '16', '-f', '4', str(source), str(aligned)])
        command(label + '-sign', [str(sdk_tool(sdk, 'apksigner')), 'sign', '--ks', str(key),
            '--ks-key-alias', os.environ[names[1]], '--ks-pass', 'env:'+names[2],
            '--key-pass', 'env:'+names[3], '--out', str(target), str(aligned)])
        verification = command(label + '-signature', [str(sdk_tool(sdk, 'apksigner')), 'verify',
            '--verbose', '--print-certs', str(target)])
        if re.findall(r'certificate SHA-256 digest: ([0-9a-f]+)', verification) != [pin['certificate_sha256']]:
            raise RuntimeError('APK signer differs from the pinned durable key.')
        command(label + '-alignment', [str(sdk_tool(sdk, 'zipalign')), '-c', '-P', '16', '4', str(target)])
        if payload(source) != payload(target):
            raise RuntimeError('APK signing/alignment changed executable payload.')
        badging = command(label + '-manifest', [str(sdk_tool(sdk, 'aapt')), 'dump', 'badging', str(target)])
        verify_badging(badging, APP_ID + ('.test' if label == 'tests' else ''))
        if label == 'app' and 'application-debuggable' in badging:
            raise RuntimeError('The bundle-derived application is debuggable.')
        return target

    try:
        state['flex'] = verify_local_flex(ROOT)
        state['graphics'] = verify_graphics()
        state['litert'] = verify_litert()
        before = source_manifest(ROOT)
        atomic_json(out / 'source-manifest.json', before)
        generated = ROOT / 'app/build/outputs/bundle/release/app-release.aab'
        generated.unlink(missing_ok=True)  # This generated output only, never an older attempt.
        command('bundle-build', [sys.executable, str(ROOT/'tools/gradle_bootstrap.py'),
            ':app:bundleRelease', ':app:assembleReleaseAndroidTest', '-PvdsTestBuildType=release',
            '-PvdsReleaseAbis=arm64-v8a', *gradle_properties, '--console=plain', '--stacktrace'])
        if source_manifest(ROOT) != before:
            raise RuntimeError('Compiled inputs changed during this attempt.')
        if not generated.is_file() or not generated.stat().st_size:
            raise RuntimeError('Gradle did not produce a real bundle.')
        unsigned = out / 'unsigned.aab'
        shutil.copyfile(generated, unsigned)
        manifest = command('manifest', [str(java), '-jar', str(bundletool), 'dump', 'manifest', '--bundle='+str(unsigned)])
        root = ET.fromstring(manifest)
        android = '{http://schemas.android.com/apk/res/android}'
        if root.attrib['package'] != APP_ID or root.find('application').get(android+'debuggable') == 'true':
            raise RuntimeError('Unexpected or debuggable application identity.')
        version = root.attrib[android+'versionName']
        if not re.fullmatch(r'[0-9A-Za-z.-]+', version):
            raise RuntimeError('Invalid version name.')
        state.update(version_name=version, version_code=int(root.attrib[android+'versionCode']))
        target = out / ('cadryl-' + version + suffix + '-signed.aab')
        command('sign', [str(signer), '-keystore', str(key), '-storetype', 'PKCS12',
            '-storepass:env', names[2], '-keypass:env', names[3], '-sigalg', 'SHA256withRSA',
            '-digestalg', 'SHA-256', '-signedjar', str(target), str(unsigned), os.environ[names[1]]])
        if 'jar verified.' not in command('verify-signature', [str(signer), '-verify', str(target)]):
            raise RuntimeError('Bundle signature verification was not confirmed.')
        certificate = command('certificate', [str(keytool), '-J-Duser.language=en', '-J-Duser.country=US',
            '-printcert', '-jarfile', str(target)])
        expected = ':'.join(pin['certificate_sha256'][i:i+2] for i in range(0,64,2)).upper()
        if 'SHA256: ' + expected not in certificate or payload(unsigned) != payload(target):
            raise RuntimeError('Unexpected bundle certificate or changed payload.')
        command('bundle-validate', [str(java), '-jar', str(bundletool), 'validate', '--bundle='+str(target)])
        with zipfile.ZipFile(target) as archive:
            state['abis'] = sorted({n.split('/')[2] for n in archive.namelist() if n.startswith('base/lib/') and n.endswith('.so')})
        if state['abis'] != ['arm64-v8a']:
            raise RuntimeError('Unexpected native ABIs.')
        # bundletool's temporary SDK signature is replaced with the durable key below.
        # Its genuine release payload, extracted from this exact AAB, is preserved.
        apks = out / 'derived.apks'
        command('derive-apks', [str(java), '-jar', str(bundletool), 'build-apks', '--bundle='+str(target),
            '--output='+str(apks), '--mode=universal'])
        with zipfile.ZipFile(apks) as archive:
            source = out / 'bundle-derived.apk'
            source.write_bytes(archive.read('universal.apk'))
        app = sign_apk(source, 'app')
        tests_source = out / 'tests-unsigned.apk'
        shutil.copyfile(ROOT/'app/build/outputs/apk/androidTest/release/app-release-androidTest.apk', tests_source)
        tests = sign_apk(tests_source, 'tests')
        for variant in ('release', 'releaseAndroidTest'):
            shutil.copyfile(ROOT/f'app/build/outputs/mapping/{variant}/mapping.txt', out/(variant+'-mapping.txt'))
        state['packaged_litert'] = verify_litert(app, ['arm64-v8a'])
        if not optional_sdks:
            state['optional_sdk_audit'] = audit(app, sdk_tool(sdk, 'aapt'))
        if source_manifest(ROOT) != before:
            raise RuntimeError('Compiled inputs changed during signing or APK derivation.')
        state.update(outcome='signed_app_bundle_built', source_manifest_sha256=digest(out/'source-manifest.json'),
            artifact=dict(file=target.name, sha256=digest(target), bytes=target.stat().st_size),
            artifacts={label:dict(file=p.name, sha256=digest(p), bytes=p.stat().st_size) for label,p in [('app',app),('tests',tests)]},
            unsigned_sha256=digest(unsigned), certificate_sha256=pin['certificate_sha256'])
    except Exception as error:
        state.update(outcome='failed', error=str(error))
        raise
    finally:
        state['finished_at'] = datetime.now(timezone.utc).isoformat()
        atomic_json(out/'status.json', state)
        print('Evidence: '+str(out), flush=True)
    return out


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--bundletool', type=Path, required=True)
    args = parser.parse_args()
    build_bundle(args.bundletool)
