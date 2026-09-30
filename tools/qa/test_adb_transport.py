import os
from pathlib import Path
import sys
import unittest
from unittest.mock import patch

sys.path.insert(0,str(Path(__file__).resolve().parent))
from adb_transport import prefix,server_port

class AdbTransportTest(unittest.TestCase):
    def test_default_server_is_explicit(self):
        with patch.dict(os.environ,{},clear=True):
            self.assertEqual(['adb','-P','5037','-s','qa-device'],prefix('qa-device'))
    def test_isolated_port_is_used_without_changing_the_device(self):
        with patch.dict(os.environ,{'VDS_ADB_SERVER_PORT':'55127'},clear=True):
            self.assertEqual(['adb','-P','55127','-s','qa-device'],prefix('qa-device'))
    def test_invalid_ports_fail_closed(self):
        for value in ('0','65536','-1','5037;','socket','１２３'):
            with patch.dict(os.environ,{'VDS_ADB_SERVER_PORT':value},clear=True):
                with self.assertRaises(ValueError):server_port()

if __name__=='__main__':unittest.main()
