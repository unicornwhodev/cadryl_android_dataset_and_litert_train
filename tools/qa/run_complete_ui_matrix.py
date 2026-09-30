#!/usr/bin/env python3
"""Capture the opt-in UI matrix against exact already-installed APKs, without resetting data."""
import argparse
from datetime import datetime, timezone
import hashlib
import io
import json
from pathlib import Path
import re
import subprocess
import zipfile
import tarfile
import uuid

from resolve_apks import APP_ID
from run_device_qualification import parse_instrumentation
from art_environment import art_crashes
from adb_transport import prefix,server_port


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--serial', required=True)
    parser.add_argument('--apk-sha256', required=True)
    parser.add_argument('--test-sha256', required=True)
    parser.add_argument('--orientation', choices=['portrait', 'landscape'], required=True)
    parser.add_argument('--monetization-mode',choices=['disabled'],default='disabled')
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--timeout', type=int, default=1200)
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=False)
    adb = prefix(args.serial)
    case=uuid.uuid4().hex[:12]
    state = dict(schema=1, serial=args.serial, orientation=args.orientation, case=case,adb_server_port=server_port(),
                 apk_sha256=args.apk_sha256, test_sha256=args.test_sha256,
                 started_at=datetime.now(timezone.utc).isoformat(), outcome='running',
                 human_certified=False, live_billing_qualified=False)
    state['orientation_control']='instrumented_activity'

    def run(name, *command, timeout=120, check=True, binary=False):
        print(name, flush=True)
        result = subprocess.run([*adb, *command], capture_output=True, timeout=timeout)
        (args.output / name).write_bytes(result.stdout + (b'' if binary else result.stderr))
        if check and result.returncode:
            raise RuntimeError(f'{name}: adb exit {result.returncode}')
        return result.stdout if binary else result.stdout.decode('utf-8', errors='replace')

    try:
        run('device-state.txt', 'get-state')
        run('device-model.txt', 'shell', 'getprop', 'ro.product.model')
        run('device-page-size.txt', 'shell', 'getconf', 'PAGE_SIZE')
        for package, expected in ((APP_ID, args.apk_sha256), (APP_ID + '.test', args.test_sha256)):
            paths = run(f'{package}-path.txt', 'shell', 'pm', 'path', package).splitlines()
            base = next((line.removeprefix('package:').strip() for line in paths if line.endswith('/base.apk')), None)
            if not base or not re.fullmatch(r'[0-9a-f]{64}', expected):
                raise RuntimeError('Missing installed base APK or invalid expected hash')
            actual = run(f'{package}-sha256.txt', 'shell', 'sha256sum', base).split()[0]
            if actual != expected:
                raise RuntimeError('Installed APK differs from requested exact candidate')
        before = run('crash-before.txt', 'logcat', '-d', '-b', 'crash')
        state['art_crashes_before'] = art_crashes(before)
        if state['art_crashes_before']:
            raise RuntimeError('Pre-existing ART crash: this environment cannot qualify the candidate')
        run('start-ui.txt', 'shell', 'am', 'start', '-n', APP_ID + '/.MainActivity')
        text = run('instrumentation.txt', 'shell', 'am', 'instrument', '-w', '-r', '-e', 'class',
                   APP_ID + '.CompleteUiMatrixTest', '-e', 'uiMatrixCase', case,
                   '-e','uiMatrixOrientation',args.orientation,
                   APP_ID + '.test/androidx.test.runner.AndroidJUnitRunner', timeout=args.timeout)
        state['tests'] = parse_instrumentation(text)
        if not state['tests']['complete'] or state['tests']['passed'] != 1:
            raise RuntimeError('The complete UI matrix test did not pass')
        uri='content://'+APP_ID+'.test.qa-evidence/final01-ui/'+case+'/'+args.orientation+'/archive.zip'
        data = run('screenshots.zip', 'exec-out', 'content', 'read', '--uri',uri,binary=True,check=False)
        state['artifact_transport']='test_qa_provider'
        if not data.startswith(b'PK'):
            # Older Debug test APKs use the same protected case path. Release cannot use run-as.
            data=run('screenshots.tar','exec-out','run-as',APP_ID+'.test','tar','-cf','-',
                     'files/qa-evidence/final01-ui/'+case+'/'+args.orientation,binary=True)
            converted=io.BytesIO()
            with tarfile.open(fileobj=io.BytesIO(data)) as source,zipfile.ZipFile(converted,'w') as target:
                for item in source.getmembers():
                    if item.isfile():target.writestr(Path(item.name).name,source.extractfile(item).read())
            data=converted.getvalue()
            state['artifact_transport']='debug_test_run_as'
        captures = args.output / 'captures'
        captures.mkdir()
        files = []
        with zipfile.ZipFile(io.BytesIO(data)) as archive:
            if archive.testzip() is not None:raise RuntimeError('QA ZIP integrity failed')
            for item in archive.infolist():
                name = item.filename
                if not re.fullmatch(r'[A-Za-z0-9_.-]+\.(png|txt|json)', name):
                    raise RuntimeError('Unsafe artifact name')
                target = captures / name
                if target.exists():
                    raise RuntimeError('Duplicate artifact name')
                payload = archive.read(item)
                target.write_bytes(payload)
                files.append(dict(file=name, bytes=len(payload), sha256=hashlib.sha256(payload).hexdigest()))
        matrix = json.loads((captures / 'matrix-status.json').read_text())
        expected_captures=144
        if matrix['orientation'] != args.orientation or matrix['captures'] != expected_captures or matrix.get('monetization_mode')!=args.monetization_mode:
            raise RuntimeError('Orientation or declared matrix count differs')
        if len(list(captures.glob('*.png'))) != expected_captures or len(list(captures.glob('*.txt'))) != expected_captures:
            raise RuntimeError('Missing screenshots or semantics')
        state['matrix'] = matrix
        state['artifacts'] = sorted(files, key=lambda item: item['file'])
        after = run('crash-after.txt', 'logcat', '-d', '-b', 'crash')
        state['art_crashes_after'] = art_crashes(after)
        if state['art_crashes_after']:
            raise RuntimeError('ART crash during the matrix')
        state['outcome'] = 'ui_matrix_captured'
    except Exception as error:
        state.update(outcome='failed', error=str(error))
        raise
    finally:
        if state.get('outcome')=='failed':
            try:
                uri='content://'+APP_ID+'.test.qa-evidence/final01-ui/'+case+'/'+args.orientation+'/archive.zip'
                run('partial-screenshots.zip','exec-out','content','read','--uri',uri,binary=True,check=False)
            except Exception as error:
                state['partial_capture_error']=str(error)
        state['finished_at'] = datetime.now(timezone.utc).isoformat()
        (args.output / 'status.json').write_text(json.dumps(state, indent=2) + '\n', encoding='utf-8')


if __name__ == '__main__':
    main()
