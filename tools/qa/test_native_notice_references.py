import sys
from pathlib import Path
import tempfile
import unittest

sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from collect_native_notices import notice_paths

class NativeNoticeReferencesTest(unittest.TestCase):
    def test_ijg_readme_is_retained_with_primary_licence(self):
        with tempfile.TemporaryDirectory() as temp:
            source=Path(temp)
            for name in ('LICENSE.md','README.ijg','README.md'):
                (source/name).write_text(name)
            self.assertEqual(['LICENSE.md','README.ijg'],[p.name for p in notice_paths(source,'libjpeg_turbo')])

    def test_missing_delegated_licence_is_an_error(self):
        with tempfile.TemporaryDirectory() as temp:
            source=Path(temp); (source/'LICENSE.md').write_text('See README.ijg')
            with self.assertRaisesRegex(ValueError,'IJG licence'):
                notice_paths(source,'libjpeg_turbo')

    def test_product_contains_original_ijg_notice_and_offline_copy(self):
        root=Path(__file__).resolve().parents[2]
        original=root/'third_party/native-notices/originals/flex/libjpeg_turbo/README.ijg'
        self.assertTrue(original.is_file())
        content=original.read_bytes().replace(b'\r\n',b'\n')
        self.assertIn(content,(root/'third_party/native-notices/NOTICES.native.txt').read_bytes())
        self.assertIn(content,(root/'app/src/main/assets/legal/notices.txt').read_bytes())

if __name__=='__main__': unittest.main()
