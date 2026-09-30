import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from package_public_release import create_output, complete_suite


class PublicReleasePackageTests(unittest.TestCase):
    def test_output_cannot_overwrite_existing_release(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / 'release'
            create_output(path)
            (path / 'receipt').write_text('original')
            with self.assertRaises(FileExistsError):
                create_output(path)
            self.assertEqual('original', (path / 'receipt').read_text())

    def test_incomplete_android_suite_is_refused(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder)
            (path / 'status.json').write_text(json.dumps({'outcome': 'failed'}))
            (path / 'instrumentation.txt').write_text('INSTRUMENTATION_FAILED: interrupted')
            with self.assertRaises(ValueError):
                complete_suite(path, 'a' * 64, 'b' * 64, 'release_core_suite_passed', 62)

    def test_wrong_apk_binding_is_refused(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder)
            parsed = dict(complete=True, passed=62)
            state = dict(outcome='release_core_suite_passed', tests=parsed, art_crashes_before=[], art_crashes_after=[], main_sha256='c' * 64, test_sha256='b' * 64)
            (path / 'status.json').write_text(json.dumps(state))
            (path / 'instrumentation.txt').write_text('actual terminal retained')
            with patch('package_public_release.parse_instrumentation', return_value=parsed):
                with self.assertRaises(ValueError):
                    complete_suite(path, 'a' * 64, 'b' * 64, 'release_core_suite_passed', 62)


if __name__ == '__main__':
    unittest.main()
