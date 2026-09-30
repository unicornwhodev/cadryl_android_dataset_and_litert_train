"""UTF-8 source labels must remain readable after Windows editing."""
from pathlib import Path
import re
import unittest

class SourceTextEncodingTest(unittest.TestCase):
    def test_android_labels_have_no_mojibake(self):
        root=Path(__file__).resolve().parents[2]/'app/src'
        pattern=re.compile(r"\u00c3[\u0080-\u00bf]|\u00e2\u20ac[\u2122\u201c\u201d\u00a6]|\u00c2\u00b7|\ufffd")
        failures=[]
        for suffix in ('*.kt','*.xml'):
            for path in root.rglob(suffix):
                text=path.read_text(encoding='utf-8',errors='strict')
                if pattern.search(text): failures.append(path.relative_to(root).as_posix())
        self.assertEqual([],failures,'Broken source label encoding')

if __name__=='__main__':unittest.main()
