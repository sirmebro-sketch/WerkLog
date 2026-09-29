"""Fail CI if a transitive dependency grants the packaged app network access."""
import os
import subprocess
from pathlib import Path
sdk = Path(os.environ['ANDROID_HOME'])
aapt = sdk / 'build-tools' / '35.0.0' / 'aapt'
apk = 'app/build/outputs/apk/debug/app-debug.apk'
permissions = subprocess.check_output([str(aapt), 'dump', 'permissions', apk], text=True)
for forbidden in ('android.permission.INTERNET', 'android.permission.ACCESS_NETWORK_STATE'):
    assert forbidden not in permissions, f'Unexpected network permission: {forbidden}'
print('Packaged APK: no Internet or network-state permission.')
