"""Explicit optional QA ADB server port. No server shutdown or other-device commands."""
import os

def server_port():
    value=os.environ.get('VDS_ADB_SERVER_PORT','5037')
    if not value.isascii() or not value.isdigit() or not 1<=int(value)<=65535:
        raise ValueError('Invalid VDS_ADB_SERVER_PORT')
    return int(value)

def prefix(serial):
    return ['adb','-P',str(server_port()),'-s',serial]
