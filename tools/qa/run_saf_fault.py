#!/usr/bin/env python3
"""Real DocumentsUI copy, revoked permission or dedicated QA volume loss.

No app reset. Every preparation/copy phase must finish before the next test.
Volume injection requires a rooted emulator and an explicitly selected QA volume.
"""
import argparse
from datetime import datetime, timezone
import json
import os
from pathlib import Path
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from resolve_apks import APP_ID
from run_device_qualification import parse_instrumentation


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--serial', required=True)
    p.add_argument('--case', required=True)
    p.add_argument('--output', type=Path, required=True)
    p.add_argument('--qa-volume', help='Only a dedicated emulator public:N,N volume')
    a = p.parse_args()
    if os.environ.get('VDS_ALLOW_TEST_INSTALL') != '1' or not re.fullmatch('[a-f0-9]{12}', a.case):
        p.error('Explicit QA authorization and a new case are required.')
    a.output.mkdir(parents=True, exist_ok=False)
    adb = ['adb', '-s', a.serial]
    state = dict(outcome='running', case=a.case, tests={}, started_at=datetime.now(timezone.utc).isoformat())
    process = None; unmounted = False
    def run(*parts):
        return subprocess.check_output([*adb, *parts], timeout=60).decode('utf-8', errors='replace').replace('\r\n', '\n')
    def save():
        (a.output / 'status.json').write_text(json.dumps(state, indent=2) + '\n', encoding='utf-8')
    def start(label, method, options=()):
        nonlocal process
        with (a.output / (label + '.txt')).open('wb') as log:
            process = subprocess.Popen([*adb, 'shell', 'am', 'instrument', '-w', '-r',
                '-e', 'class', APP_ID + '.ExternalFaultQualificationTest#' + method,
                '-e', 'faultCase', a.case, *options, APP_ID + '.test/androidx.test.runner.AndroidJUnitRunner'],
                stdout=log, stderr=subprocess.STDOUT)
    def finish(label):
        process.wait(timeout=240)
        result = parse_instrumentation((a.output / (label + '.txt')).read_text(encoding='utf-8', errors='replace'))
        state['tests'][label] = result; save()
        if not result['complete'] or result['passed'] != 1:
            raise ValueError('Incomplete DocumentsUI phase: ' + label)
    def click(pattern, attribute='text'):
        for sequence in range(15):
            raw = run('exec-out', 'uiautomator', 'dump', '/dev/tty')
            if '<?xml' not in raw: continue
            tree = ET.fromstring(raw[raw.index('<?xml'):raw.index('</hierarchy>')+12])
            nodes = [n for n in tree.iter('node') if n.get('package') in ('com.google.android.documentsui', 'com.android.documentsui')]
            for parent in list(tree.iter()):
                for child in list(parent):
                    if child.tag == 'node' and child not in nodes: parent.remove(child)
            path = a.output / ('documentsui-' + str(time.monotonic_ns()) + '.xml')
            path.write_text(ET.tostring(tree, encoding='unicode'), encoding='utf-8')
            candidates = [n for n in nodes if re.fullmatch(pattern, n.get(attribute, '')) and n.get('enabled') == 'true']
            if len(candidates) == 1:
                x1,y1,x2,y2 = map(int,re.findall(r'\d+',candidates[0].get('bounds','')))
                run('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2)); return
            time.sleep(.5)
        raise ValueError('DocumentsUI control unavailable: ' + pattern)
    save()
    try:
        state['physical_device'] = run('shell','getprop','ro.kernel.qemu').strip() != '1'
        state['abi'] = run('shell','getprop','ro.product.cpu.abi').strip()
        state['page_size'] = int(run('shell','getconf','PAGE_SIZE'))
        for key, package in [('main',APP_ID),('test',APP_ID+'.test')]:
            path = run('shell','pm','path',package).strip().removeprefix('package:')
            state[key+'_sha256'] = run('shell','sha256sum',path).split()[0]
        if 'DEBUGGABLE' not in run('shell','dumpsys','package',APP_ID): raise ValueError('Debug bridge required.')
        if a.qa_volume:
            if state['physical_device'] or run('shell','id','-u').strip() != '0' or not re.fullmatch(r'public:\d+,\d+',a.qa_volume):
                raise ValueError('Volume faults require an exact, rooted emulator QA volume.')
            volumes = run('shell','sm','list-volumes','all')
            if not re.search('^'+re.escape(a.qa_volume)+r' mounted \S+\s*$',volumes,re.M): raise ValueError('QA volume must be mounted.')
            state['qa_volume'] = a.qa_volume
            (a.output/'volumes-before.txt').write_text(volumes,encoding='utf-8')
        run('shell','am','force-stop',APP_ID)
        start('prepare','prepareAndRequestRealDocument')
        if a.qa_volume:
            click('Show roots|Afficher les racines','content-desc')
            click('Virtual SD card')
        click('SAVE|ENREGISTRER|Enregistrer')
        finish('prepare')
        start('copy','verifyCopyAndOptionallyRevokeGrant', () if a.qa_volume else ('-e','revokeGrant','true'))
        finish('copy')
        if a.qa_volume:
            run('shell','sm','unmount',a.qa_volume); unmounted=True
            volumes = run('shell','sm','list-volumes','all')
            (a.output/'volumes-unmounted.txt').write_text(volumes,encoding='utf-8')
            if not re.search('^'+re.escape(a.qa_volume)+r' unmounted \S+\s*$',volumes,re.M): raise ValueError('Unmount not confirmed.')
        run('shell','am','force-stop',APP_ID)
        start('refused','unavailableBackupRefusesPurgeAndPreservesHumanData'); finish('refused')
        for name in ('document','copied','refused'):
            value = run('shell','run-as',APP_ID,'cat',f'files/qa-evidence/external-faults/{a.case}/{name}.json')
            (a.output/(name+'.json')).write_text(value,encoding='utf-8')
        state['outcome'] = 'real_volume_loss_passed' if a.qa_volume else 'real_documentsui_revocation_passed'
    except Exception as error:
        state.update(outcome='failed',error=str(error)); raise
    finally:
        if process is not None and process.poll() is None:
            run('shell','am','force-stop',APP_ID); process.wait(timeout=30)
        if unmounted:
            run('shell','sm','mount',a.qa_volume)
            state['volume_restored'] = True
        state['finished_at'] = datetime.now(timezone.utc).isoformat(); save()


if __name__ == '__main__': main()
