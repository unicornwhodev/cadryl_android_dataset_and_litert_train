#!/usr/bin/env python3
"""Preserve a dedicated emulator's Debug certificate for exact Release payload QA.

Does not replace the durable distribution key or qualify a Debug main APK.
The original Release build and signing receipts remain immutable.
"""
import argparse
import json
import os
from pathlib import Path
import re
import subprocess
import sys
from resolve_apks import ROOT
sys.path.insert(0, str(ROOT / 'tools'))
from build_android import digest, sdk_dir, sdk_tool
from package_native_release import apk_payload, artifact


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--build', type=Path, required=True)
    p.add_argument('--output', type=Path, required=True)
    p.add_argument('--debug-keystore', type=Path, required=True)
    a = p.parse_args()
    if a.debug_keystore.resolve().is_relative_to(ROOT): p.error('External Debug keystore required.')
    build = json.loads((a.build / 'status.json').read_text(encoding='utf-8'))
    if build.get('outcome') != 'release_test_apks_built': p.error('A completed Release build is required.')
    a.output.mkdir(parents=True, exist_ok=False)
    signer = str(sdk_tool(sdk_dir(ROOT, os.environ) / 'build-tools/36.0.0', 'apksigner'))
    state = dict(outcome='running', build_receipt_sha256=digest(a.build/'status.json'),
        source_manifest_sha256=digest(a.build/'source-manifest.json'), artifacts={},
        distribution_key=False, main_build_type='minified-release', production_qualified=False)
    try:
        for kind in ('app','tests'):
            source = artifact(a.build, build['artifacts'][kind]); target = a.output / (kind+'.apk')
            subprocess.run([signer,'sign','--ks',str(a.debug_keystore),'--ks-key-alias','androiddebugkey',
                '--ks-pass','pass:android','--key-pass','pass:android','--out',str(target),str(source)],check=True,capture_output=True)
            cert = subprocess.check_output([signer,'verify','--print-certs',str(target)],text=True)
            certificates = re.findall(r'certificate SHA-256 digest: ([a-f0-9]{64})',cert)
            if len(certificates) != 1 or (kind == 'tests' and certificates[0] != state['certificate_sha256']): raise ValueError('Certificate mismatch.')
            state['certificate_sha256'] = certificates[0]
            if apk_payload(source) != apk_payload(target): raise ValueError('APK payload changed.')
            state['artifacts'][kind] = dict(file=target.name, sha256=digest(target),bytes=target.stat().st_size,source_sha256=digest(source))
        state['outcome'] = 'qa_signed_release_apks'
    except Exception as error:
        state.update(outcome='failed',error=str(error)); raise
    finally:
        (a.output/'status.json').write_text(json.dumps(state,indent=2)+'\n',encoding='utf-8')


if __name__ == '__main__': main()
