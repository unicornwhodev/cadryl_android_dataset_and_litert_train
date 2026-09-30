#!/usr/bin/env python3
"""Collect notices from retained native linker inputs; verify stripped binary provenance.

Run in the existing Linux native-build environment. Does not rebuild or publish.
Header-only dependencies and official unreconstructed 32-bit Flex remain separate gates.
"""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
import tempfile
import zipfile

ROOT=Path(__file__).resolve().parents[1]

def sha(data): return hashlib.sha256(data).hexdigest()

def collect(work,kind,aar,output):
    caches=list((work/'bazel-cache').glob('*/external'))
    if len(caches)!=1: raise ValueError('Select the exact retained native build cache')
    cache=caches[0].parent
    execroot=cache/'execroot'/('litert' if kind=='litert' else 'org_tensorflow')
    parameters=list((execroot/'bazel-out').glob('*/bin/**/libtensorflowlite*so-2.params'))
    if not parameters: raise ValueError('Missing retained linker parameters')
    repositories=set(); receipts=[]; texts=[]
    ndk=work/('android-ndk-r26d' if kind=='litert' else 'android-ndk-r25b')
    strip=ndk/'toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-strip'
    with zipfile.ZipFile(aar) as archive, tempfile.TemporaryDirectory(dir=output) as temporary:
        for param in sorted(parameters):
            data=param.read_bytes()
            repositories.update(re.findall(r'(?:^|/)external/([^/\s]+)',data.decode()))
            config=param.relative_to(execroot/'bazel-out').parts[0]
            abi={'arm64-v8a-opt':'arm64-v8a','armeabi-v7a-opt':'armeabi-v7a','x86-opt':'x86','x86_64-opt':'x86_64'}[config]
            binary=param.with_name(param.name.removesuffix('-2.params'))
            candidate=Path(temporary)/(abi+'.so')
            subprocess.run([str(strip),'--strip-unneeded','-o',str(candidate),str(binary)],check=True,capture_output=True)
            current=sha(candidate.read_bytes())
            packaged=sha(archive.read('jni/'+abi+'/'+binary.name))
            if current!=packaged: raise ValueError('Retained native binary differs from the packaged AAR: '+kind+'/'+abi)
            receipts.append({'abi':abi,'parameter_file':str(param.relative_to(cache)),'parameters_sha256':sha(data),
                'native_packaged_sha256':packaged,'retained_binary_matches_packaged':True})
    rows=[]
    for repo in sorted(repositories):
        source=(cache/'external'/repo).resolve()
        notices=[p for p in source.iterdir() if p.is_file() and re.search(r'^(LICENSE|NOTICE|COPYING|COPYRIGHT)([._-]|$)',p.name,re.I)]
        if (source/'LICENSES').is_dir(): notices.extend(p for p in (source/'LICENSES').iterdir() if p.is_file())
        if repo=='KleidiAI' and (source/'REUSE.toml').is_file(): notices.append(source/'REUSE.toml')
        if repo=='fft2d': notices.extend(source.glob('readme*.txt'))
        if not notices:
            notices=[p for p in source.glob('*/*') if p.is_file() and re.search(r'^(LICENSE|NOTICE|COPYING|COPYRIGHT)([._-]|$)',p.name,re.I)]
        entries=[]
        for p in sorted(set(notices)):
            data=p.read_bytes();relative=p.relative_to(source)
            target=output/'originals'/kind/repo/relative
            target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(data)
            entries.append({'path':relative.as_posix(),'sha256':sha(data)})
            texts.append('\n=== '+kind+'/'+repo+'/'+relative.as_posix()+' ===\n\n'+data.decode('utf-8'))
        rows.append({'repository':repo,'notices':entries})
    return {'linker_inputs':receipts,'repositories':rows,'missing_notices':[r['repository'] for r in rows if not r['notices']]},texts

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--litert-work',type=Path,required=True)
    parser.add_argument('--flex-work',type=Path,required=True)
    parser.add_argument('--output',type=Path,required=True)
    args=parser.parse_args();args.output.mkdir(parents=True,exist_ok=True)
    data={'schema':1,'legal_clearance':False,'native_transitive_review_complete':False,'runtimes':{},
        'remaining':['Header-only dependencies and per-source attribution review','Official unchanged 32-bit Flex libraries','Graphics Path and DataStore native dependency closure']}
    text=[]
    version=json.loads((ROOT/'config/litert-source.json').read_text())['version']
    for kind,work,pattern in [('litert',args.litert_work,f'dist/native-litert/maven/**/litert-interpreter-{version}.aar'),('flex',args.flex_work,'dist/native-flex/maven/**/tensorflow-lite-select-tf-ops-2.16.1-vds16k1.aar')]:
        aars=list(ROOT.glob(pattern))
        if len(aars)!=1: raise ValueError('Ambiguous native AAR')
        result,sections=collect(work,kind,aars[0],args.output)
        data['runtimes'][kind]=result;text.extend(sections)
    (args.output/'inventory.json').write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8')
    (args.output/'NOTICES.native.txt').write_text('\n'.join(text),encoding='utf-8')
    print(json.dumps({kind:{'verified_binaries':len(r['linker_inputs']),'missing_notices':r['missing_notices']} for kind,r in data['runtimes'].items()}))

if __name__=='__main__':main()
