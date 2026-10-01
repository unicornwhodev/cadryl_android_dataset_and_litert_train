"""Parse the real bundletool XML without treating JVM notices as XML."""
import sys
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from build_app_bundle import parse_bundle_manifest

class BundleManifestTests(unittest.TestCase):
    def test_plain_xml(self):
        self.assertEqual('example',parse_bundle_manifest('<manifest package="example"/>').attrib['package'])
    def test_jvm_notice(self):
        output='Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=D:/temp\n<manifest package="example"><application/></manifest>\n'
        self.assertIsNotNone(parse_bundle_manifest(output).find('application'))
    def test_missing_xml(self):
        with self.assertRaises(ValueError):parse_bundle_manifest('No manifest returned')
    def test_malformed_xml(self):
        with self.assertRaises(ET.ParseError):parse_bundle_manifest('<manifest broken>')
    def test_trailing_noise(self):
        with self.assertRaises(ET.ParseError):parse_bundle_manifest('<manifest/>\nUnexpected trailing content')
