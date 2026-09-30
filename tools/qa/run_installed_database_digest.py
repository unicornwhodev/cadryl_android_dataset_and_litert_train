#!/usr/bin/env python3
"""Read the installed database through the opt-in platform test; never copy its contents."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
from run_device_qualification import parse_instrumentation
from resolve_apks import APP_ID
from adb_transport import prefix


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--serial',required=True)
    parser.add_argument('--apk-sha256',required=True)
    parser.add_argument('--test-sha256',required=True)
    parser.add_argument('--output',type=Path,required=True)
    parser.add_argument('--compare',type=Path)
    args=parser.parse_args()
    args.output.mkdir(parents=True,exist_ok=False)
    adb=prefix(args.serial)
    state=dict(outcome='running',apk_sha256=args.apk_sha256,test_sha256=args.test_sha256,user_contents_exported=False)
    def run(name,*command):
        result=subprocess.run([*adb,*command],capture_output=True,timeout=120)
        (args.output/name).write_bytes(result.stdout+result.stderr)
        if result.returncode:raise RuntimeError(name+' failed')
        return result.stdout.decode('utf-8',errors='strict')
    try:
        for package,expected in ((APP_ID,args.apk_sha256),(APP_ID+'.test',args.test_sha256)):
            if not re.fullmatch('[a-f0-9]{64}',expected):raise ValueError('Invalid expected APK hash')
            paths=run(package+'-path.txt','shell','pm','path',package).splitlines()
            path=next(line.removeprefix('package:').strip() for line in paths if line.endswith('/base.apk'))
            if run(package+'-sha256.txt','shell','sha256sum',path).split()[0]!=expected:raise RuntimeError('Installed bytes differ')
        result=run('instrumentation.txt','shell','am','instrument','-w','-r','-e','class',
                   APP_ID+'.InstalledDatabaseDigestTest',APP_ID+'.test/androidx.test.runner.AndroidJUnitRunner')
        state['tests']=parse_instrumentation(result)
        if not state['tests']['complete'] or state['tests']['passed']!=1:raise RuntimeError('Read-only snapshot did not pass')
        match=re.search(r'^INSTRUMENTATION_STATUS: installed_database_digest=(.*)$',result,re.M)
        if not match:raise RuntimeError('Missing inline digest receipt')
        data=match[1]
        receipt=json.loads(data)
        (args.output/'digest.json').write_text(json.dumps(receipt,indent=2)+'\n',encoding='utf-8')
        state['digest_sha256']=hashlib.sha256(data.encode()).hexdigest()
        if args.compare:
            expected=json.loads(args.compare.read_text(encoding='utf-8'))
            if receipt!=expected:raise RuntimeError('Installed logical database changed during replacement')
            state['all_installed_tables_preserved']=True
        state['outcome']='installed_database_digest_verified'
    except Exception as error:
        state.update(outcome='failed',error=str(error))
        raise
    finally:
        (args.output/'status.json').write_text(json.dumps(state,indent=2)+'\n',encoding='utf-8')


if __name__=='__main__':main()
