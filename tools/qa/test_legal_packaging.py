import hashlib
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest

ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('legal_packaging',ROOT/'tools/package_legal_assets.py')
module=importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class LegalPackagingTest(unittest.TestCase):
    def test_offline_documents_are_small_utf8_assets_without_disclosure_build_paths(self):
        for name in ('privacy.txt','privacy-en.txt','notices.txt'):
            data=(ROOT/'app/src/main/assets/legal'/name).read_bytes()
            self.assertLessEqual(len(data),2*1024*1024)
            text=data.decode('utf-8',errors='strict')
            self.assertTrue(text.strip())
            if name!='notices.txt':self.assertNotIn('=== docs/',text)

    def test_bundle_is_reproducible_and_retains_every_input_after_lf_normalization(self):
        receipt=json.loads((ROOT/'app/src/main/assets/legal/manifest.json').read_text())
        with tempfile.TemporaryDirectory() as folder:
            root=Path(folder)
            for item in receipt['files'].values():
                for source in item['inputs']:
                    destination=root/source
                    destination.parent.mkdir(parents=True,exist_ok=True)
                    destination.write_bytes((ROOT/source).read_bytes())
            first=module.package(root)
            self.assertEqual(receipt,first)
            output=root/'app/src/main/assets/legal'
            before={file.name:file.read_bytes() for file in output.iterdir()}
            self.assertEqual(first,module.package(root))
            self.assertEqual(before,{file.name:file.read_bytes() for file in output.iterdir()})
            for name,item in first['files'].items():
                data=(output/name).read_bytes()
                self.assertEqual(hashlib.sha256(data).hexdigest(),item['sha256'])
                for source,expected in item['inputs'].items():
                    content=(root/source).read_bytes().replace(b'\r\n',b'\n')
                    self.assertIn(content,data)
                    self.assertEqual(hashlib.sha256(content).hexdigest(),expected)


if __name__=='__main__':
    unittest.main()
