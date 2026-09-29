#!/usr/bin/env python3
"""Package RC8 with physical ARM/4 KB and native x86/16 KB evidence.

All compiled bytes and public evidence must match committed Git blobs. An
emulator QA certificate is accepted only with identical Release ZIP payloads.
Unfulfilled hardware/accuracy/background gates remain explicit.
"""
import argparse
import hashlib
import json
from pathlib import Path
import re
import shutil
import subprocess
import sys
import zipfile
from build_android import ROOT, digest, source_manifest, weight_inventory
from package_native_release import read, git, artifact, apk_payload
sys.path.insert(0, str(ROOT / 'tools/qa'))
from run_device_qualification import parse_instrumentation


def core(folder, app_sha, test_sha, page, abi):
    state = read(folder / 'status.json')
    parsed = parse_instrumentation((folder / 'instrumentation.txt').read_text(encoding='utf-8'))
    if (state.get('outcome') != 'release_core_suite_passed' or state.get('main_sha256') != app_sha
        or state.get('test_sha256') != test_sha or parsed != state.get('tests')
        or not parsed['complete'] or parsed['passed'] != 53 or parsed['failed'] or parsed['skipped']
        or str(state.get('page_size')) != str(page) or state.get('abi') != abi
        or state.get('art_crashes_before') != [] or state.get('art_crashes_after') != []):
        raise ValueError('Exact RC8 core evidence is incomplete or mismatched.')
    return dict(passed=53, page_size=page, abi=abi, instrumentation=True, main_sha256=app_sha, test_sha256=test_sha)


def ui(folder, app_sha):
    state = read(folder / 'status.json')
    expected = ['home_renders','model_import_export_quality_navigation',
                'created_project_survives_process_restart','display_preference_persists_and_is_restored']
    trees = list(folder.glob('ui-*.xml'))
    if (state.get('outcome') != 'host_release_ui_passed' or state.get('main_sha256') != app_sha
        or state.get('passed') != expected or state.get('instrumentation') is not False
        or state.get('freezing_exemption') is not False or len(trees) != state.get('sequence') or len(trees) < 4):
        raise ValueError('Exact independent RC8 UI evidence is incomplete or mismatched.')
    return dict(passed=4, fresh_trees=len(trees), instrumentation=False, main_sha256=app_sha)


def main():
    p = argparse.ArgumentParser(description=__doc__)
    for label in ('arm','x86'):
        p.add_argument('--'+label+'-build',type=Path,required=True)
        p.add_argument('--'+label+'-signed',type=Path,required=True)
    p.add_argument('--x86-qa-signed',type=Path,required=True)
    p.add_argument('--evidence',type=Path,required=True)
    a = p.parse_args()
    if git('status','--porcelain').strip(): raise ValueError('Commit the reviewed tree before packaging.')
    head = git('rev-parse','HEAD').decode().strip()
    tracked = set(filter(None,git('ls-files','-z').decode().split('\0')))
    current = source_manifest(ROOT)
    for name, sha in current['files'].items():
        if hashlib.sha256(git('show',head+':'+name)).hexdigest() != sha: raise ValueError('Compiled Git bytes differ: '+name)
    evidence = a.evidence.resolve(); prefix = evidence.relative_to(ROOT).as_posix()+'/'
    files = {f.relative_to(ROOT).as_posix() for f in evidence.rglob('*') if f.is_file()}
    if files != {f for f in tracked if f.startswith(prefix)}: raise ValueError('Evidence selection differs from Git.')
    rows = read(evidence/'EVIDENCE_MANIFEST.json')
    if {prefix+r['path'] for r in rows} != files-{prefix+'EVIDENCE_MANIFEST.json'}: raise ValueError('Incomplete evidence manifest.')
    for name in files:
        if hashlib.sha256(git('show',head+':'+name)).hexdigest() != digest(ROOT/name): raise ValueError('Evidence bytes differ: '+name)
    for row in rows:
        f = evidence/row['path']
        if digest(f) != row['sha256'] or f.stat().st_size != row['bytes']: raise ValueError('Evidence hash mismatch.')
    pairs = {}; builds = {}; suites = {}
    pin = read(ROOT/'config/release-signing.json')['certificate_sha256']
    from build_android import sdk_dir, sdk_tool
    import os
    sdk = sdk_dir(ROOT,os.environ)/'build-tools/36.0.0'; signer = sdk_tool(sdk,'apksigner')
    for label, abi in [('arm','arm64-v8a'),('x86','x86_64')]:
        build_dir = getattr(a,label+'_build'); signed_dir = getattr(a,label+'_signed')
        build = read(build_dir/'status.json'); signed = read(signed_dir/'status.json'); manifest = read(build_dir/'source-manifest.json')
        if build.get('outcome') != 'release_test_apks_built' or build.get('abi') != abi: raise ValueError('Wrong Release build.')
        if any(manifest.get(k) != current.get(k) for k in ('files','native_runtime','native_runtimes')): raise ValueError('Sources changed after build.')
        if (signed.get('outcome') != 'signed_release_test_apks' or signed.get('certificate_sha256') != pin
            or signed.get('build_receipt_sha256') != digest(build_dir/'status.json')
            or signed.get('source_manifest_sha256') != digest(build_dir/'source-manifest.json')): raise ValueError('Invalid durable signing receipt.')
        pairs[label] = {}
        for kind in ('app','tests'):
            original = artifact(build_dir,build['artifacts'][kind]); apk = artifact(signed_dir,signed['artifacts'][kind])
            if apk_payload(original) != apk_payload(apk): raise ValueError('Signing changed payload.')
            cert = subprocess.check_output([str(signer),'verify','--print-certs',str(apk)],text=True)
            if re.findall(r'certificate SHA-256 digest: ([a-f0-9]{64})',cert) != [pin]: raise ValueError('Unpinned certificate.')
            pairs[label][kind] = apk
        app = pairs[label]['app']
        badging = subprocess.check_output([str(sdk_tool(sdk,'aapt')),'dump','badging',str(app)],text=True,encoding='utf-8')
        if "name='com.unicornwhodev.visiondatasetstudio'" not in badging or "versionName='4.2.0-rc8'" not in badging or 'application-debuggable' in badging or f"native-code: '{abi}'" not in badging:
            raise ValueError('Wrong app identity, ABI, version or build type.')
        if weight_inventory(app)['weight_files']: raise ValueError('Embedded model weights are forbidden.')
        audit_path = ROOT/'dist'/('rc8-package-audit-'+label+'.json')
        subprocess.run([sys.executable,str(ROOT/'tools/qa/check_apk_page_sizes.py'),'--apk',str(app),'--output',str(audit_path)],check=True)
        if read(audit_path) != read(evidence/label/'strict-alignment.json'): raise ValueError('Native audit mismatch.')
        builds[label] = dict(base_commit=manifest['source_commit'],build_receipt_sha256=digest(build_dir/'status.json'),source_manifest_sha256=digest(build_dir/'source-manifest.json'),artifacts=signed['artifacts'])
    qa = read(a.x86_qa_signed/'status.json')
    if qa.get('outcome') != 'qa_signed_release_apks' or qa.get('build_receipt_sha256') != digest(a.x86_build/'status.json'): raise ValueError('Wrong emulator signing receipt.')
    qa_pair = {kind:artifact(a.x86_qa_signed,qa['artifacts'][kind]) for kind in ('app','tests')}
    for kind in qa_pair:
        if apk_payload(qa_pair[kind]) != apk_payload(pairs['x86'][kind]): raise ValueError('Emulator Release payload changed.')
    suites['arm'] = dict(core=core(evidence/'arm/core',digest(pairs['arm']['app']),digest(pairs['arm']['tests']),4096,'arm64-v8a'),ui=ui(evidence/'arm/ui',digest(pairs['arm']['app'])),physical_device=True)
    suites['x86'] = dict(core=core(evidence/'x86/core',digest(qa_pair['app']),digest(qa_pair['tests']),16384,'x86_64'),ui=ui(evidence/'x86/ui',digest(qa_pair['app'])),physical_device=False,qa_certificate=qa['certificate_sha256'],payload_identical=True)
    host = read(evidence/'host-checks.json')
    if host['jvm'] != dict(passed=106,failed=0,errors=0,skipped=0) or host['python']['passed'] < 89 or host['python']['failed'] or host['lint']['errors']: raise ValueError('Host checks incomplete.')
    long = read(evidence/'extended/prolonged-inference.json')
    if long.get('outcome') != 'prolonged_native_cpu_inference_passed' or long.get('main_sha256') != digest(pairs['arm']['app']) or long['measurement']['duration_ms'] < 600000 or not long['tests']['complete']: raise ValueError('Prolonged physical evidence mismatch.')
    corpus = read(evidence/'extended/corpus-1000.json')
    if corpus.get('outcome') != 'public_cc0_1000_native_import_passed' or not corpus['tests']['complete'] or corpus['measurement']['consumed_source_rows'] != 1000: raise ValueError('Corpus evidence incomplete.')
    for name, expected in [('saf-revocation.json','real_documentsui_revocation_passed'),('volume-loss.json','real_volume_loss_passed')]:
        state = read(evidence/'extended'/name)
        if state.get('outcome') != expected or any(not t['complete'] for t in state['tests'].values()) or len(state['tests']) != 3: raise ValueError('Real SAF/volume evidence incomplete.')
    for scenario in ('hf-live','hf-lost-response','hf-conflict'):
        state = read(evidence/'extended'/(scenario+'.json'))
        if state.get('outcome') != 'passed' or state.get('cleanup_errors') != [] or not state.get('tests'):
            raise ValueError('Real private HF scenario incomplete.')
        completed = [t for t in state['tests'].values() if t['complete']]
        if not completed or any(t['failed'] or t['skipped'] for t in state['tests'].values()):
            raise ValueError('Real private HF assertions failed.')
        for method, recorded in state['tests'].items():
            text = (evidence/'extended'/scenario/(method+'.txt')).read_text(encoding='utf-8')
            if parse_instrumentation(text) != recorded:
                raise ValueError('HF terminal output and receipt disagree.')
    required = {
        'hf-live-result.json': ['private_qa_repository','android_publication_and_readback','repeat_did_not_republish','cleanup_after_remote_verification'],
        'hf-lost-response-result.json': ['no_duplicate_commit','human_annotations_preserved','images_preserved','remote_readback_verified'],
        'hf-conflict-result.json': ['stale_parent_rejected','no_silent_rebase','local_candidate_retained']}
    for name, fields in required.items():
        result = read(evidence/'extended'/name)
        if any(result.get(field) is not True for field in fields): raise ValueError('Real HF preservation evidence incomplete.')
    out = ROOT/'dist/release-4.2.0-rc8'; out.mkdir(exist_ok=False)
    package = dict(schema=1,product='Cadryl',version='4.2.0-rc8',application_id='com.unicornwhodev.visiondatasetstudio',kind='signed-minified-release-prerelease',source_commit=head,compiled_sources_byte_exact=True,compiled_files_verified=len(current['files']),source_manifest=current,builds=builds,core_and_ui=suites,host_checks=host,physical_arm_4k_core_qualified=True,physical_arm_16k_qualified=False,stable_release=False,ci_executed=False,native_transitive_notices_review_complete=False,signature=dict(certificate_sha256=pin,durable=True),limitations=['Physical ARM 16 KB device unavailable: Honor and Oppo use 4 KB pages.','Full FireViewer/model accuracy, real complete/partial Viewer flow and background endurance remain open.','Remote CI and native transitive notice review remain open.'])
    (out/'PACKAGE.json').write_text(json.dumps(package,indent=2)+'\n',encoding='utf-8')
    for label,name in [('arm','vision-dataset-studio.apk'),('x86','vision-dataset-studio-x86_64.apk')]: shutil.copyfile(pairs[label]['app'],out/name)
    docs = {n for n in tracked if n.startswith(('docs/','third_party/')) or n in ('README.md','README.en.md','TEST_REPORT.md','KNOWN_LIMITATIONS.md','LICENSING_STATUS.md','QUALIFICATION_STATUS.json','LICENSE','NOTICE','DATA_SCHEMA.md')}
    with zipfile.ZipFile(out/'vision-dataset-studio-4.2.0-rc8-qualification.zip','w',zipfile.ZIP_DEFLATED,compresslevel=3) as z:
        z.write(out/'PACKAGE.json','PACKAGE.json')
        for label,pair in pairs.items():
            for kind,apk in pair.items(): z.write(apk,f'apks/{label}/{kind}.apk')
        for name in sorted(docs|files): z.write(ROOT/name,name)
    for runtime in ('flex','graphics','litert'):
        aar = ROOT/current['native_runtime']['file'] if runtime == 'flex' else next(ROOT/n for n in current['native_runtimes'] if '/native-'+runtime+'/' in n)
        native = sorted(f for f in aar.parent.iterdir() if f.is_file())
        if len(native) != 3 or {f.suffix for f in native} != {'.aar','.pom','.json'}: raise ValueError('Unexpected native package files.')
        with zipfile.ZipFile(out/f'vision-dataset-studio-{runtime}-{aar.parent.name}.zip','w',zipfile.ZIP_DEFLATED,compresslevel=1) as z:
            for f in native: z.write(f,f.relative_to(ROOT).as_posix())
            for name in sorted(n for n in tracked if n.startswith(('config/','third_party/patches/')) or n in ('LICENSE','NOTICE','third_party/NOTICES.runtime.txt','docs/FLEX_16K.md','docs/GRAPHICS_PATH_16K.md','docs/LITERT_16K_STATUS.md','tools/build_flex_runtime.py','tools/build_graphics_path.py','tools/build_litert_runtime.py')): z.write(ROOT/name,name)
    assets = sorted(f for f in out.iterdir() if f.is_file())
    (out/'SHA256SUMS').write_text(''.join(digest(f)+'  '+f.name+'\n' for f in assets),encoding='utf-8')
    print(out)


if __name__ == '__main__': main()
