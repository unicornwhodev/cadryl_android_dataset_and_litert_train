"""Regression guard for the public Apache-2.0 edition."""
from pathlib import Path
import re
import subprocess
import unittest

ROOT = Path(__file__).resolve().parents[2]

class PublicSourceBoundaryTest(unittest.TestCase):
    def test_paid_implementation_is_outside_public_tree(self):
        for path in ('app/src/monetized', 'app/src/oss', 'backend',
                     'config/monetization.properties', 'docs/MONETIZATION_SETUP.md',
                     'docs/SUBSCRIPTION_TERMS.md', 'third_party/NOTICES.monetized.txt'):
            target = ROOT / path
            self.assertFalse(target.is_file() or (target.is_dir() and any(target.rglob('*'))), path)

    def test_no_paid_sdk_dependencies_or_configuration(self):
        gradle = (ROOT / 'app/build.gradle.kts').read_text(encoding='utf-8')
        for marker in ('play-services-ads', 'com.android.billingclient', 'user-messaging-platform',
                       'ADMOB_APP_ID', 'vdsMonetizationMode'):
            self.assertNotIn(marker, gradle)
        for source in (ROOT / 'app/src/main').rglob('*.kt'):
            self.assertFalse(re.search(r'import\s+(?:com\.android\.billingclient|com\.google\.android\.gms\.ads|com\.google\.android\.ump)\.', source.read_text(encoding='utf-8')), source.name)

    def test_no_ai_studio_key_in_public_files(self):
        paths = subprocess.check_output(['git', 'ls-files', '-z'], cwd=ROOT).decode().split('\0')
        pattern = re.compile(rb'AIza[0-9A-Za-z_-]{35}')
        for name in paths:
            source = ROOT / name
            if name and source.is_file():
                self.assertIsNone(pattern.search(source.read_bytes()), name)

if __name__ == '__main__':
    unittest.main()
