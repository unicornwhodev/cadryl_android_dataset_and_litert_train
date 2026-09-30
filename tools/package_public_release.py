#!/usr/bin/env python3
"""Package a public release from actual builds and exact installed-APK evidence.

Raw workstation/device logs remain local. Public summaries contain only the
reviewed measurements and hashes; signing never changes application payloads.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import zipfile
from build_android import ROOT, APP_ID, digest, source_manifest, sdk_dir, sdk_tool, weight_inventory
from package_native_release import artifact, apk_payload, git, read
from qa.check_monetization_free_apk import audit
from qa.run_device_qualification import parse_instrumentation


def complete_suite(folder, app_sha, test_sha, outcome, count):
    state = read(folder / 'status.json')
    parsed = parse_instrumentation((folder / 'instrumentation.txt').read_text(encoding='utf-8'))
    app_field = 'main_sha256' if outcome.startswith('release_') else None
    if (state.get('outcome') != outcome or not parsed['complete'] or parsed['passed'] != count
            or parsed != state.get('tests', state.get('instrumentation'))
            or state.get('art_crashes_before') != [] or state.get('art_crashes_after') != []):
        raise ValueError('Incomplete Android suite: ' + str(folder))
    if app_field:
        if state.get(app_field) != app_sha or state.get('test_sha256') != test_sha:
            raise ValueError('Device suite is bound to different APKs')
    else:
        recorded = state['build']['artifacts']
        if recorded['app']['sha256'] != app_sha or recorded['tests']['sha256'] != test_sha:
            raise ValueError('Debug suite is bound to different APKs')
    return dict(passed=count, failed=0, skipped=0, app_sha256=app_sha, tests_sha256=test_sha,
                abi=state.get('package_abi', state.get('abi')), api=state.get('api', state.get('android_api')),
                physical_device='emulator' in state and state['emulator'] != '1',
                raw_receipt_sha256=digest(folder / 'status.json'),
                raw_terminal_sha256=digest(folder / 'instrumentation.txt'))


def verify_host_ui(folder, app_sha):
    state = read(folder / 'status.json')
    expected = ['home_renders', 'model_import_export_quality_navigation',
                'created_project_survives_process_restart', 'display_preference_persists_and_is_restored']
    if (state.get('outcome') != 'host_release_ui_passed' or state.get('main_sha256') != app_sha
            or state.get('passed') != expected or state.get('instrumentation') is not False
            or state.get('freezing_exemption') is not False
            or len(list(folder.glob('ui-*.xml'))) != state.get('sequence')):
        raise ValueError('Independent UI evidence is incomplete')
    return dict(passed=4, instrumentation=False, app_sha256=app_sha,
                raw_receipt_sha256=digest(folder / 'status.json'))


def create_output(path):
    path.mkdir(parents=True, exist_ok=False)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('debug-build', 'release-build', 'signed', 'debug-core', 'release-core', 'host-ui', 'database-update', 'output'):
        parser.add_argument('--' + name, type=Path, required=True)
    parser.add_argument('--version', required=True)
    parser.add_argument('--core-count', type=int, required=True)
    args = parser.parse_args()
    if not re.fullmatch(r'\d+\.\d+\.\d+', args.version):
        raise ValueError('Public release version must be a stable numeric version')
    if git('status', '--porcelain').strip():
        raise ValueError('Commit the reviewed source before packaging')
    head = git('rev-parse', 'HEAD').decode().strip()
    current = source_manifest(ROOT)
    for name, expected in current['files'].items():
        if hashlib.sha256(git('show', head + ':' + name)).hexdigest() != expected:
            raise ValueError('Compiled source differs from Git: ' + name)
    debug, release, signed = [read(p / 'status.json') for p in (args.debug_build, args.release_build, args.signed)]
    for folder in (args.debug_build, args.release_build):
        manifest = read(folder / 'source-manifest.json')
        if any(manifest.get(k) != current.get(k) for k in ('files', 'native_runtime', 'native_runtimes')):
            raise ValueError('Sources or native dependencies changed after build')
    if (not debug.get('all_build_checks_passed') or debug.get('outcome') != 'build_checks_passed'
            or debug['jvm_tests']['failures'] or debug['jvm_tests']['errors'] or debug['jvm_tests']['skipped']
            or debug['lint'].get('Error', 0) or not debug['ksp_generated_files']):
        raise ValueError('Actual JVM/lint/KSP checks did not pass')
    pin = read(ROOT / 'config/release-signing.json')['certificate_sha256']
    if (release.get('outcome') != 'release_test_apks_built' or release.get('abi') != 'arm64-v8a'
            or signed.get('outcome') != 'signed_release_test_apks' or signed.get('certificate_sha256') != pin
            or signed.get('build_receipt_sha256') != digest(args.release_build / 'status.json')
            or signed.get('source_manifest_sha256') != digest(args.release_build / 'source-manifest.json')):
        raise ValueError('Release build/signature receipts do not match')
    sdk = sdk_dir(ROOT, os.environ) / 'build-tools/36.0.0'
    pairs = {}
    for kind in ('app', 'tests'):
        original = artifact(args.release_build, release['artifacts'][kind])
        apk = artifact(args.signed, signed['artifacts'][kind])
        if apk_payload(original) != apk_payload(apk):
            raise ValueError('Signing changed APK payloads')
        cert = subprocess.check_output([str(sdk_tool(sdk, 'apksigner')), 'verify', '--print-certs', str(apk)], text=True)
        if re.findall(r'certificate SHA-256 digest: ([a-f0-9]{64})', cert) != [pin]:
            raise ValueError('Wrong signing identity')
        pairs[kind] = apk
    app_sha, test_sha = digest(pairs['app']), digest(pairs['tests'])
    badging = subprocess.check_output([str(sdk_tool(sdk, 'aapt')), 'dump', 'badging', str(pairs['app'])], text=True, encoding='utf-8')
    expected_code = read(ROOT / 'QUALIFICATION_STATUS.json')['version_code']
    if (f"name='{APP_ID}'" not in badging or f"versionName='{args.version}'" not in badging
            or f"versionCode='{expected_code}'" not in badging or 'application-debuggable' in badging
            or "native-code: 'arm64-v8a'" not in badging):
        raise ValueError('Wrong release identity, version, ABI or build type')
    sdk_audit = audit(pairs['app'], sdk_tool(sdk, 'aapt'))
    if weight_inventory(pairs['app'])['weight_files']:
        raise ValueError('Embedded model weights are not allowed')
    alignment_path = ROOT / 'dist' / ('release-' + args.version + '-alignment.json')
    subprocess.run([sys.executable, str(ROOT / 'tools/qa/check_apk_page_sizes.py'), '--apk', str(pairs['app']), '--output', str(alignment_path)], check=True)
    native = read(alignment_path)
    if not native['statically_compatible'] or len(native['native_64bit_libraries']) != 4:
        raise ValueError('Incomplete native alignment audit')
    core = complete_suite(args.release_core, app_sha, test_sha, 'release_core_suite_passed', args.core_count)
    if core['abi'] != 'arm64-v8a' or not core['physical_device']:
        raise ValueError('Release suite must run on physical ARM64')
    debug_pair = {k: artifact(args.debug_build, debug['artifacts'][k]) for k in ('app', 'tests')}
    emulator = complete_suite(args.debug_core, digest(debug_pair['app']), digest(debug_pair['tests']), 'core_suite_passed', args.core_count)
    ui = verify_host_ui(args.host_ui, app_sha)
    database = read(args.database_update / 'status.json')
    if (database.get('outcome') != 'installed_database_digest_verified'
            or database.get('all_installed_tables_preserved') is not True
            or database.get('user_contents_exported') is not False
            or database.get('apk_sha256') != app_sha or database.get('test_sha256') != test_sha):
        raise ValueError('Installed database preservation is unverified')
    package = dict(schema=1, product='Cadryl', version=args.version, application_id=APP_ID,
                   source_commit=head, compiled_sources_byte_exact=True, compiled_files_verified=len(current['files']),
                   kind='signed-minified-public-release', stable_release=True, monetization_sdk_audit=sdk_audit,
                   signature=dict(certificate_sha256=pin, durable=True),
                   artifacts=signed['artifacts'], jvm=debug['jvm_tests'], lint=debug['lint'], ksp_generated_files=debug['ksp_generated_files'],
                   android=dict(physical_arm_release=core, emulator_debug=emulator, independent_ui=ui),
                   installed_database_preserved=True, user_contents_exported=False, ci_executed=False,
                   native_alignment=native, native_notice_clearance_complete=False,
                   scope='Exact APK core and independent UI checks; prior external-service campaigns remain historical.',
                   raw_workstation_and_device_logs_public=False)
    create_output(args.output)
    (args.output / 'PACKAGE.json').write_text(json.dumps(package, indent=2) + '\n', encoding='utf-8')
    shutil.copyfile(pairs['app'], args.output / 'vision-dataset-studio.apk')
    tracked = set(filter(None, git('ls-files', '-z').decode().split('\0')))
    docs = {n for n in tracked if n.startswith('third_party/') or n in (
        'README.md', 'README.en.md', 'LICENSE', 'NOTICE', 'LICENSING_STATUS.md', 'KNOWN_LIMITATIONS.md',
        'TEST_REPORT.md', 'QUALIFICATION_STATUS.json', 'DATA_SCHEMA.md', 'docs/RELEASE_001.md',
        'docs/RELEASE_001_EVIDENCE.json', 'docs/PRIVACY_REDACTION_2026_09.json', 'docs/PRIVACY_REDACTION_2026_09.md',
        'docs/GETTING_STARTED.md', 'docs/en/GETTING_STARTED.md', 'docs/FIRST_LAUNCH_TUTORIAL.md',
        'docs/DEVELOPMENT_RESUME.md', 'docs/en/DEVELOPMENT_RESUME.md', 'docs/PRIVACY_POLICY.md', 'docs/PRIVACY_POLICY.en.md')}
    with zipfile.ZipFile(args.output / f'vision-dataset-studio-{args.version}-qualification.zip', 'w', zipfile.ZIP_DEFLATED, compresslevel=3) as z:
        z.write(args.output / 'PACKAGE.json', 'PACKAGE.json')
        for kind, apk in pairs.items():
            z.write(apk, 'apks/arm/' + kind + '.apk')
        for name in sorted(docs):
            z.write(ROOT / name, name)
    for runtime in ('flex', 'graphics', 'litert'):
        aar = ROOT / current['native_runtime']['file'] if runtime == 'flex' else next(ROOT / n for n in current['native_runtimes'] if '/native-' + runtime + '/' in n)
        with zipfile.ZipFile(args.output / f'vision-dataset-studio-{runtime}-{aar.parent.name}.zip', 'w', zipfile.ZIP_DEFLATED, compresslevel=1) as z:
            for path in sorted(aar.parent.iterdir()):
                if path.is_file():
                    z.write(path, path.relative_to(ROOT).as_posix())
            for name in sorted(n for n in tracked if n.startswith(('config/', 'third_party/')) or n in (
                'LICENSE', 'NOTICE', 'tools/build_flex_runtime.py', 'tools/build_graphics_path.py', 'tools/build_litert_runtime.py',
                'docs/FLEX_16K.md', 'docs/GRAPHICS_PATH_16K.md', 'docs/LITERT_16K_STATUS.md')):
                z.write(ROOT / name, name)
    assets = sorted(p for p in args.output.iterdir() if p.is_file())
    (args.output / 'SHA256SUMS').write_text(''.join(digest(p) + '  ' + p.name + '\n' for p in assets), encoding='utf-8')
    print('Created:', args.output)


if __name__ == '__main__':
    main()
