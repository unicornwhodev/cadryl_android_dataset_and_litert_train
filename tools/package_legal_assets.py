#!/usr/bin/env python3
"""Embed the project's actual notices and offline disclosures, without changing licences."""
from pathlib import Path
import hashlib
import json

ROOT = Path(__file__).resolve().parents[1]

def package(root=ROOT):
    output = root / 'app/src/main/assets/legal'
    output.mkdir(parents=True, exist_ok=True)
    inputs = {
        'privacy.txt': ['docs/PRIVACY_POLICY.md'],
        'privacy-en.txt': ['docs/PRIVACY_POLICY.en.md'],
        'notices.txt': ['LICENSE', 'NOTICE', 'third_party/NOTICES.runtime.txt',
                        'third_party/patches/LICENSE.cpuinfo', 'third_party/patches/README.md',
                        'third_party/native-notices/NOTICES.native.txt',
                        'third_party/supplemental/javax.inject-1-copyright.txt',
                        'third_party/design/NOTICES.design.txt',
                        'third_party/design/LICENSE.compose-phosphor-icon',
                        'third_party/design/LICENSE.phosphor',
                        'third_party/design/OFL.barlow.txt'],
    }
    result = {'schema': 1, 'legal_clearance': False, 'input_line_endings': 'canonical LF', 'files': {}}
    for name, paths in inputs.items():
        sections = []
        hashes = {}
        for source in paths:
            content = (root / source).read_bytes().replace(b'\r\n', b'\n')
            hashes[source] = hashlib.sha256(content).hexdigest()
            # Input paths belong in the provenance manifest. Readers of a single
            # disclosure should see its title and text without build metadata.
            sections.append((f'\n=== {source} ===\n\n'.encode() if name=='notices.txt' else b'') + content)
        data = b'\n'.join(sections)
        (output / name).write_bytes(data)
        result['files'][name] = {'sha256': hashlib.sha256(data).hexdigest(), 'inputs': hashes}
    (output / 'manifest.json').write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8',newline='\n')
    return result

if __name__ == '__main__':
    print(json.dumps(package(), indent=2))
