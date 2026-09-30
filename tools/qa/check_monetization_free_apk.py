"""Inspect a real APK for advertising/billing SDK code, permissions and metadata."""
import hashlib
import subprocess
from pathlib import Path
import zipfile

SDK_PREFIXES=(b'Lcom/google/android/gms/ads/',b'Lcom/google/android/gms/internal/ads/',
              b'Lcom/google/android/ump/',b'Lcom/google/ads/mediation/',b'Lcom/android/billingclient/')

def audit(apk: Path,aapt: Path):
    found=[]
    with zipfile.ZipFile(apk) as archive:
        dex=[name for name in archive.namelist() if name.startswith('classes') and name.endswith('.dex')]
        if not dex:raise ValueError('APK has no compiled DEX')
        for name in dex:
            data=archive.read(name)
            found.extend(prefix.decode() for prefix in SDK_PREFIXES if prefix in data)
    manifest=subprocess.check_output([str(aapt),'dump','xmltree',str(apk),'AndroidManifest.xml'],timeout=60).decode('utf-8',errors='replace')
    forbidden=('com.android.vending.BILLING','com.google.android.gms.ads.APPLICATION_ID',
               'com.google.android.gms.permission.AD_ID','android.permission.ACCESS_ADSERVICES_AD_ID',
               'android.permission.ACCESS_ADSERVICES_ATTRIBUTION','android.permission.ACCESS_ADSERVICES_TOPICS')
    found.extend(name for name in forbidden if name in manifest)
    if found:raise ValueError('Default source APK contains monetization SDK/manifest entries: '+', '.join(sorted(set(found))))
    with apk.open('rb') as stream:sha=hashlib.file_digest(stream,'sha256').hexdigest()
    return dict(outcome='monetization_sdk_absent',apk_sha256=sha,dex_files=len(dex),
                advertising_sdk=False,billing_sdk=False,billing_permission=False,advertising_identifier_permissions=False)
