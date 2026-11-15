"""Inspect an already-built release against current Mukhtabir source; never sign it."""

import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[2]
ANDROID = '{http://schemas.android.com/apk/res/android}'


def main(apk):
    if os.environ.get('GITHUB_ACTIONS') != 'true':
        raise ValueError('Explicit hosted operation required')
    tools = Path(os.environ['ANDROID_HOME']) / 'build-tools/34.0.0'
    analyzer = Path(os.environ['ANDROID_HOME']) / 'cmdline-tools/latest/bin/apkanalyzer'
    output = ROOT / 'ci-output/release-apk'
    output.mkdir(parents=True, exist_ok=True)
    def command(arguments):
        return subprocess.run(arguments, capture_output=True, text=True, check=True, timeout=60).stdout
    badging = command([str(tools / 'aapt'), 'dump', 'badging', str(apk)])
    manifest_text = command([str(analyzer), 'manifest', 'print', str(apk)])
    manifest = ET.fromstring(manifest_text)
    source = ET.parse(ROOT / 'app/src/main/AndroidManifest.xml').getroot()
    version = dict(re.findall(r'^([^#\s=]+)=(.*)$', (ROOT / 'gradle/app-version.properties').read_text(), re.M))
    if (manifest.get('package') != 'com.mukhtabir.android'
            or manifest.get(ANDROID + 'versionCode') != version['versionCode']
            or manifest.get(ANDROID + 'versionName') != version['versionName']
            or 'application-debuggable' in badging):
        raise ValueError('Unexpected release identity/version/debuggable state')
    sdk = manifest.find('uses-sdk')
    if sdk is None or sdk.get(ANDROID + 'minSdkVersion') != '23' or sdk.get(ANDROID + 'targetSdkVersion') != '34':
        raise ValueError('Unexpected packaged SDKs')
    app = manifest.find('application')
    source_app = source.find('application')
    if app is None or source_app is None or app.get(ANDROID + 'debuggable', 'false') != 'false':
        raise ValueError('Missing/non-release application')
    # Selected-origin HTTP/private TLS is intentional here. Do not copy Aqaqeer's
    # blanket cleartext prohibition into this application's release contract.
    for attribute in ('allowBackup', 'supportsRtl', 'usesCleartextTraffic'):
        if app.get(ANDROID + attribute) != source_app.get(ANDROID + attribute):
            raise ValueError('Packaged policy differs from current source: ' + attribute)
    if not app.get(ANDROID + 'networkSecurityConfig'):
        raise ValueError('Missing configured network security policy')
    permissions = {node.get(ANDROID + 'name') for node in manifest if node.tag.startswith('uses-permission')}
    expected = {node.get(ANDROID + 'name') for node in source if node.tag.startswith('uses-permission')}
    receiver = 'com.mukhtabir.android.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'
    if permissions - {receiver} != expected:
        raise ValueError('Unexpected merged permission set; review actual shipping graph')
    if receiver in permissions:
        declaration = next((node for node in manifest.findall('permission') if node.get(ANDROID + 'name') == receiver), None)
        if declaration is None or declaration.get(ANDROID + 'protectionLevel') not in ('signature', '0x00000002', '2'):
            raise ValueError('Generated receiver permission must be signature-protected')
    with zipfile.ZipFile(apk) as package:
        if package.testzip() is not None:
            raise ValueError('Corrupt APK entry')
        if 'classes.dex' not in package.namelist() or 'resources.arsc' not in package.namelist():
            raise ValueError('Missing compiled classes/resources')
        native = [name for name in package.namelist() if name.startswith('lib/') and name.endswith('.so')]
        if native:
            raise ValueError('Unexpected native libraries; review ABI/alignment contract before distribution')
        assets = ROOT / 'app/src/main/assets'
        checked = 0
        for path in assets.rglob('*'):
            if path.is_file():
                if path.is_symlink() or package.read('assets/' + path.relative_to(assets).as_posix()) != path.read_bytes():
                    raise ValueError('Packaged public asset differs from source')
                checked += 1
    subprocess.run([str(tools / 'zipalign'), '-c', '4', str(apk)], capture_output=True, check=True, timeout=30)
    receipt = dict(package=manifest.get('package'), versionCode=int(version['versionCode']),
                   versionName=version['versionName'], apkSha256=hashlib.sha256(apk.read_bytes()).hexdigest(),
                   sourceCommit=os.environ['GITHUB_SHA'], publicAssetsChecked=checked,
                   permissions=sorted(permissions), nativeLibraries=native,
                   signature='not established: this is the unsigned inspection build')
    (output / 'inspection.json').write_text(json.dumps(receipt, indent=2) + '\n')
    (output / 'manifest.xml').write_text(manifest_text)
    (output / 'badging.txt').write_text(badging)
    print('Release source/package/resource/permission/alignment inspection completed; no production signature or device pass claimed.')


if __name__ == '__main__':
    main(Path(sys.argv[1]))
