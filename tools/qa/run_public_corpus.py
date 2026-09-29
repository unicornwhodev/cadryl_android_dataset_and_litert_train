#!/usr/bin/env python3
"""Import 1,000 explicitly authorized public images through real DocumentsUI.

Dedicated Debug emulator only. No reset, private corpus, inference accuracy
claim, or external writes. The manifest contains the pinned public provenance.
"""
import argparse
from datetime import datetime, timezone
import hashlib
import io
import json
import os
from pathlib import Path
import re
import subprocess
import tarfile
import time
import xml.etree.ElementTree as ET
from resolve_apks import APP_ID, ROOT, resolve
from run_device_qualification import parse_instrumentation


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--serial', required=True)
    p.add_argument('--case', required=True)
    p.add_argument('--manifest', type=Path, required=True)
    p.add_argument('--output', type=Path, required=True)
    p.add_argument('--signed-receipt', type=Path)
    p.add_argument('--allow-physical-device', action='store_true')
    a = p.parse_args()
    if os.environ.get('VDS_ALLOW_TEST_INSTALL') != '1' or not re.fullmatch('[a-f0-9]{12}', a.case):
        p.error('Explicit QA install authorization and a new case are required.')
    manifest = json.loads(a.manifest.read_text(encoding='utf-8'))
    if manifest.get('license') != 'cc0-1.0' or len(manifest.get('images', [])) != 1000:
        p.error('A pinned, 1,000-image CC0 manifest is required.')
    a.output.mkdir(parents=True, exist_ok=False)
    adb = ['adb', '-s', a.serial]
    state = dict(outcome='running', started_at=datetime.now(timezone.utc).isoformat(),
                 case=a.case, physical_device=False, accuracy_evaluated=False)
    process = None
    def run(*args, data=None):
        return subprocess.check_output([*adb, *args], input=data, timeout=120).decode('utf-8', errors='replace')
    def save():
        (a.output / 'status.json').write_text(json.dumps(state, indent=2) + '\n', encoding='utf-8')
    save()
    try:
        emulator = run('shell', 'getprop', 'ro.kernel.qemu').strip() == '1'
        if not emulator and not a.allow_physical_device:
            raise ValueError('Use a dedicated QA emulator.')
        state['physical_device'] = not emulator
        state['abi'] = run('shell', 'getprop', 'ro.product.cpu.abi').strip()
        state['page_size'] = int(run('shell', 'getconf', 'PAGE_SIZE'))
        if a.signed_receipt:
            from sign_qualification_apks import resolve_signed
            app, tests, build = resolve_signed(a.signed_receipt)
        else:
            app, tests = resolve(ROOT / 'dist/android')
            build = json.loads((app.parent / 'status.json').read_text(encoding='utf-8'))
        if not build.get('all_build_checks_passed'):
            raise ValueError('The latest Debug build must pass all checks.')
        for name, apk in [('main', app), ('test', tests)]:
            state[name + '_sha256'] = hashlib.sha256(apk.read_bytes()).hexdigest()
            if not re.search(r'^Success\s*$', run('install', '--no-streaming', '-r', str(apk)), re.M):
                raise ValueError('Verified APK update failed.')
        if 'DEBUGGABLE' not in run('shell', 'dumpsys', 'package', APP_ID):
            raise ValueError('This DocumentsUI bridge requires Debug.')
        run('shell', 'am', 'force-stop', APP_ID)
        uid = int(run('shell', 'run-as', APP_ID, 'id', '-u'))
        buffer = io.BytesIO()
        data = a.manifest.read_bytes()
        with tarfile.open(fileobj=buffer, mode='w') as archive:
            item = tarfile.TarInfo('files/qa-corpus-input-' + a.case + '.json')
            item.size = len(data); item.mode = 0o600; item.uid = item.gid = uid
            archive.addfile(item, io.BytesIO(data))
        run('exec-in', 'run-as', APP_ID, 'tar', '-x', '-f', '-', data=buffer.getvalue())
        state['manifest_sha256'] = hashlib.sha256(data).hexdigest()
        state['source'] = {k: manifest[k] for k in ('dataset', 'revision', 'license')}
        log = a.output / 'instrumentation.txt'
        with log.open('wb') as stream:
            process = subprocess.Popen([*adb, 'shell', 'am', 'instrument', '-w', '-r',
                '-e', 'class', APP_ID + '.HfModelRuntimeTest#realPublicCorpus1000SurvivesRoomReopen',
                '-e', 'publicCorpusAudit', 'true', '-e', 'corpusCase', a.case,
                APP_ID + '.test/androidx.test.runner.AndroidJUnitRunner'], stdout=stream, stderr=subprocess.STDOUT)
        deadline = time.monotonic() + 1200
        selected = allowed = False
        sequence = 0
        foreground = 0.0
        while process.poll() is None:
            if time.monotonic() > deadline:
                raise TimeoutError('Corpus test exceeded its limit.')
            if not allowed:
                raw = run('exec-out', 'uiautomator', 'dump', '/dev/tty')
                if '<?xml' in raw:
                    raw = raw[raw.index('<?xml'):raw.index('</hierarchy>') + 12]
                    tree = ET.fromstring(raw)
                    # Only retain the dedicated system file picker, never another app.
                    for parent in list(tree.iter()):
                        for child in list(parent):
                            if child.tag == 'node' and child.get('package') not in ('com.google.android.documentsui', 'com.android.documentsui'):
                                parent.remove(child)
                    sequence += 1
                    (a.output / f'documentsui-{sequence:03}.xml').write_text(ET.tostring(tree, encoding='unicode'), encoding='utf-8')
                    desired = 'ALLOW|AUTORISER|Autoriser' if selected else 'USE THIS FOLDER|UTILISER CE DOSSIER|Utiliser ce dossier'
                    nodes = [n for n in tree.iter('node') if re.fullmatch(desired, n.get('text', '')) and n.get('enabled') == 'true']
                    if len(nodes) == 1:
                        x1, y1, x2, y2 = map(int, re.findall(r'\d+', nodes[0].get('bounds', '')))
                        run('shell', 'input', 'tap', str((x1+x2)//2), str((y1+y2)//2))
                        if selected: allowed = True
                        else: selected = True
                        print('DocumentsUI: ' + desired, flush=True)
            if allowed and time.monotonic() - foreground >= 30:
                run('shell', 'am', 'start', '-f', '0x20000000', '-n', APP_ID + '/.MainActivity')
                foreground = time.monotonic()
                state['app_kept_visible'] = True
            time.sleep(1)
        parsed = parse_instrumentation(log.read_text(encoding='utf-8', errors='replace'))
        state['tests'] = parsed
        if not parsed['complete'] or parsed['passed'] != 1:
            raise ValueError('The 1,000-image native corpus test did not pass.')
        match = re.search(r'^INSTRUMENTATION_RESULT: public_corpus_evidence=(.+)$', log.read_text(encoding='utf-8'), re.M)
        state['measurement'] = json.loads(match.group(1))
        state['outcome'] = 'public_cc0_1000_native_import_passed'
    except Exception as error:
        state.update(outcome='failed', error=str(error))
        raise
    finally:
        if process is not None and process.poll() is None:
            run('shell', 'am', 'force-stop', APP_ID)
            process.wait(timeout=30)
        state['finished_at'] = datetime.now(timezone.utc).isoformat()
        save()


if __name__ == '__main__':
    main()
