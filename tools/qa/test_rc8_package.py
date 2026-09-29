"""RC8 publication guards cannot transfer or invent successful test evidence."""
import json
from pathlib import Path
import sys
import tempfile
import unittest
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from package_rc8_release import core, ui
from run_device_qualification import parse_instrumentation


class Rc8PackageTests(unittest.TestCase):
    def fixture(self, folder):
        text = ''.join(f'INSTRUMENTATION_STATUS: class=Flow\nINSTRUMENTATION_STATUS: test=c{i}\nINSTRUMENTATION_STATUS: numtests=53\nINSTRUMENTATION_STATUS_CODE: 0\n' for i in range(53)) + 'OK (53 tests)\n'
        state = dict(outcome='release_core_suite_passed',main_sha256='a'*64,test_sha256='b'*64,
            tests=parse_instrumentation(text),page_size=4096,abi='arm64-v8a',art_crashes_before=[],art_crashes_after=[])
        (folder/'instrumentation.txt').write_text(text,encoding='utf-8')
        (folder/'status.json').write_text(json.dumps(state),encoding='utf-8')
        return state
    def test_matching_53_on_physical_4k_is_accepted(self):
        with tempfile.TemporaryDirectory() as tmp:
            p=Path(tmp); self.fixture(p)
            self.assertEqual(53,core(p,'a'*64,'b'*64,4096,'arm64-v8a')['passed'])
    def test_different_apk_or_page_size_is_refused(self):
        with tempfile.TemporaryDirectory() as tmp:
            p=Path(tmp); self.fixture(p)
            for sha,page in [('c'*64,4096),('a'*64,16384)]:
                with self.assertRaises(ValueError): core(p,sha,'b'*64,page,'arm64-v8a')
    def test_truncated_or_art_crashed_suite_is_refused(self):
        with tempfile.TemporaryDirectory() as tmp:
            p=Path(tmp); s=self.fixture(p)
            (p/'instrumentation.txt').write_text('INSTRUMENTATION_FAILED\n',encoding='utf-8')
            with self.assertRaises(ValueError): core(p,'a'*64,'b'*64,4096,'arm64-v8a')
            s=self.fixture(p); s['art_crashes_after']=['system_server']
            (p/'status.json').write_text(json.dumps(s),encoding='utf-8')
            with self.assertRaises(ValueError): core(p,'a'*64,'b'*64,4096,'arm64-v8a')
    def test_ui_receipt_without_fresh_trees_is_refused(self):
        with tempfile.TemporaryDirectory() as tmp:
            p=Path(tmp); (p/'status.json').write_text(json.dumps(dict(outcome='host_release_ui_passed',main_sha256='a'*64,passed=[],instrumentation=False,freezing_exemption=False,sequence=0)),encoding='utf-8')
            with self.assertRaises(ValueError): ui(p,'a'*64)


if __name__=='__main__': unittest.main()
