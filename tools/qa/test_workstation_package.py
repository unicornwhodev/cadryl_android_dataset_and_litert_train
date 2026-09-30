#!/usr/bin/env python3
"""Source-manifest compatibility and ambiguity guards for release packaging."""
import sys
import unittest
import tempfile
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from package_workstation_release import source_hashes, create_release_directory

class SourceManifestTests(unittest.TestCase):
    def test_new_release_directory_can_be_created(self):
        with tempfile.TemporaryDirectory() as temporary:
            output=Path(temporary)/'delivery'
            create_release_directory(output)
            self.assertTrue(output.is_dir())
    def test_existing_delivery_and_receipt_cannot_be_overwritten(self):
        with tempfile.TemporaryDirectory() as temporary:
            output=Path(temporary)/'delivery'; output.mkdir()
            receipt=output/'PACKAGE.json'; receipt.write_text('original')
            with self.assertRaises(FileExistsError): create_release_directory(output)
            self.assertEqual('original',receipt.read_text())
    def test_explicit_records_preserve_source_hashes(self):
        self.assertEqual({'app/example.kt':'a'*64},source_hashes([{'path':'app/example.kt','sha256':'a'*64}]))
    def test_historical_manifests_remain_supported(self):
        hashes={'app/example.kt':'a'*64}
        self.assertEqual(hashes,source_hashes(hashes))
    def test_duplicate_path_is_rejected_even_with_matching_digest(self):
        row={'path':'app/example.kt','sha256':'a'*64}
        with self.assertRaises(ValueError):source_hashes([row,row])
    def test_invalid_digest_is_rejected(self):
        with self.assertRaises(ValueError):source_hashes([{'path':'app/example.kt','sha256':'invalid'}])

if __name__=='__main__':unittest.main(verbosity=2)
