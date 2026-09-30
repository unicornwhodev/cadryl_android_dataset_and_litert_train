#!/usr/bin/env python3
"""Measure sustained native CPU inference on an authorized actual Release device.

Public model and photo files remain outside the APK and Git. No device reset,
private corpus, background claim or exemption from process freezing is used.
"""
import argparse
from adb_transport import prefix
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import time
import uuid
from run_device_qualification import APP_ID, parse_instrumentation, keyguard_showing
from art_environment import art_crashes


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--serial', required=True)
    p.add_argument('--sha256', required=True)
    p.add_argument('--test-apk', type=Path, required=True)
    p.add_argument('--test-sha256', required=True)
    p.add_argument('--fixtures', type=Path, required=True)
    p.add_argument('--output', type=Path, required=True)
    p.add_argument('--seconds', type=int, default=600)
    a = p.parse_args()
    if os.environ.get('VDS_ALLOW_TEST_INSTALL') != '1' or not 600 <= a.seconds <= 3600:
        p.error('Explicit QA authorization and duration 600..3600 required')
    for value in (a.sha256, a.test_sha256):
        if not re.fullmatch('[a-f0-9]{64}', value): p.error('Exact APK hashes required')
    a.output.mkdir(parents=True, exist_ok=False)
    adb = prefix(a.serial)
    case = uuid.uuid4().hex[:12]
    state = dict(outcome='running', main_sha256=a.sha256, test_sha256=a.test_sha256,
                 case=case, started_at=datetime.now(timezone.utc).isoformat(),
                 production_qualified=False, background_qualified=False,
                 instrumentation=True, app_kept_visible=True, freezing_exemption=False)
    def save(): (a.output / 'status.json').write_text(json.dumps(state, indent=2) + '\n', encoding='utf-8')
    def run(*args): return subprocess.check_output([*adb, *args], timeout=60).decode('utf-8', errors='replace')
    def capture(name, *args):
        value = run(*args); (a.output / name).write_text(value, encoding='utf-8'); return value
    save()
    process = None
    try:
        assert hashlib.sha256(a.test_apk.read_bytes()).hexdigest() == a.test_sha256, 'Test APK hash differs'
        path = run('shell', 'pm', 'path', APP_ID).strip().removeprefix('package:')
        assert run('shell', 'sha256sum', path).split()[0] == a.sha256, 'Main APK hash differs'
        assert 'DEBUGGABLE' not in capture('package.txt', 'shell', 'dumpsys', 'package', APP_ID), 'Actual Release required'
        assert not keyguard_showing(run('shell', 'dumpsys', 'window', 'policy')), 'Unlock the device'
        state['abi'] = run('shell', 'getprop', 'ro.product.cpu.abi').strip()
        state['page_size'] = int(run('shell', 'getconf', 'PAGE_SIZE'))
        state['android_api'] = run('shell', 'getprop', 'ro.build.version.sdk').strip()
        state['art_crashes_before'] = art_crashes(capture('crash-before.txt', 'logcat', '-d', '-b', 'crash'))
        assert not state['art_crashes_before'], 'Existing ART crash'
        manifest = json.loads((a.fixtures / 'staging-manifest.json').read_text(encoding='utf-8'))
        for name, info in manifest['files'].items():
            assert re.fullmatch('[A-Za-z0-9_.-]+', name), 'Unsafe fixture name'
            file = a.fixtures / name
            assert file.stat().st_size == info['bytes'] and hashlib.sha256(file.read_bytes()).hexdigest() == info['sha256'], 'Fixture hash differs'
        state['public_fixture_manifest'] = manifest
        run('shell', 'mkdir', '-p', '/data/local/tmp/vds-qa-public-' + case)
        for name in [*manifest['files'], 'staging-manifest.json']:
            run('push', str(a.fixtures / name), '/data/local/tmp/vds-qa-public-' + case + '/' + name)
        assert re.search(r'^Success\s*$', capture('install-tests.txt', 'install', '--no-streaming', '-r', str(a.test_apk)), re.M)
        with (a.output / 'instrumentation.txt').open('wb') as log:
            process = subprocess.Popen([*adb, 'shell', 'am', 'instrument', '-w', '-r',
                '-e', 'class', APP_ID + '.HfModelRuntimeTest#prolongedRealPhotoCpuInference',
                '-e', 'prolongedInference', 'true', '-e', 'prolongedSeconds', str(a.seconds),
                '-e', 'benchmarkCase', case, APP_ID + '.test/androidx.test.runner.AndroidJUnitRunner'], stdout=log, stderr=subprocess.STDOUT)
            started = time.monotonic(); foreground = 0
            while process.poll() is None:
                elapsed = time.monotonic() - started
                assert elapsed < a.seconds + 180, 'Instrumentation exceeded deadline'
                if elapsed - foreground >= (5 if foreground == 0 else 30):
                    run('shell', 'am', 'start', '-f', '0x20000000', '-n', APP_ID + '/.MainActivity')
                    foreground = elapsed
                    print(f'Native CPU inference running: {elapsed:.0f}s', flush=True)
                time.sleep(1)
        raw = (a.output / 'instrumentation.txt').read_text(encoding='utf-8', errors='replace')
        state['tests'] = parse_instrumentation(raw)
        assert state['tests']['complete'] and state['tests']['passed'] == 1, 'Prolonged inference test failed'
        match = re.search(r'^INSTRUMENTATION_RESULT: prolonged_inference_evidence=(.+)$', raw, re.M)
        assert match, 'Native report missing'
        state['measurement'] = json.loads(match.group(1))
        assert state['measurement']['success'] and state['measurement']['duration_ms'] >= a.seconds * 1000
        state['art_crashes_after'] = art_crashes(capture('crash-after.txt', 'logcat', '-d', '-b', 'crash'))
        assert not state['art_crashes_after'], 'ART crashed'
        state['outcome'] = 'prolonged_native_cpu_inference_passed'
    except Exception as error:
        state.update(outcome='failed', error=str(error)); raise
    finally:
        if process and process.poll() is None:
            run('shell', 'am', 'force-stop', APP_ID); process.kill()
        state['finished_at'] = datetime.now(timezone.utc).isoformat(); save()


if __name__ == '__main__': main()
