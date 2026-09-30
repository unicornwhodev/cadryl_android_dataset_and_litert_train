import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from build_android import source_manifest

class SourceManifestCleanupTest(unittest.TestCase):
    def test_deleted_tracked_input_is_recorded_and_new_input_is_hashed(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory)
            subprocess.run(['git','init','-q'],cwd=root,check=True)
            source=root/'app/src/main/old.kt'
            source.parent.mkdir(parents=True)
            source.write_text('old',encoding='utf-8')
            subprocess.run(['git','add','.'],cwd=root,check=True)
            subprocess.run(['git','-c','user.name=QA','-c','user.email=qa@example.invalid','commit','-qm','fixture'],cwd=root,check=True)
            before=source_manifest(root)
            source.unlink()
            new=source.with_name('new.kt');new.write_text('new',encoding='utf-8')
            after=source_manifest(root)
            self.assertEqual(['app/src/main/old.kt'],after['deleted_tracked_files'])
            self.assertNotIn('app/src/main/old.kt',after['files'])
            self.assertIn('app/src/main/new.kt',after['files'])
            self.assertNotEqual(before,after)

if __name__=='__main__': unittest.main()
