"""Verify this checkout's build and create a separate debug delivery. Python 3.11+."""
from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import struct
import subprocess
import sys
import xml.etree.ElementTree as ET
import zipfile
from datetime import datetime, timezone, timedelta

ROOT_FILES = {
    'README.md', 'LICENSE', '.gitignore', '.gitattributes', 'build.ps1',
    'build.gradle', 'settings.gradle', 'gradle.properties', 'gradlew', 'gradlew.bat',
}
SOURCE_DIRS = ('app', 'gradle', 'tools', 'scripts', 'docs')
EXCLUDED_DIRS = {'build', '.gradle', '.git', '.idea', '.vscode', '__pycache__'}
TEXT_SUFFIXES = {'.java', '.gradle', '.properties', '.xml', '.txt', '.md', '.csv',
                 '.json', '.py', '.ps1', '.bat', '.proposed'}
PRIVATE_NAMES = {'local.properties', 'key.properties', 'keystore.properties', 'google-services.json'}


def source_paths(root: Path) -> list[Path]:
    paths = [root / name for name in ROOT_FILES if (root / name).is_file()]
    for directory in SOURCE_DIRS:
        for path in (root / directory).rglob('*'):
            rel = path.relative_to(root)
            if any(part in EXCLUDED_DIRS for part in rel.parts) or not path.is_file():
                continue
            if path.name in PRIVATE_NAMES or path.name.startswith('.env'):
                continue
            if rel.as_posix() in {'gradle/wrapper/gradle-wrapper.jar', 'app/src/main/assets/xposed_init'} or path.suffix in TEXT_SUFFIXES:
                paths.append(path)
    required = {'gradlew', 'gradlew.bat', 'gradle/wrapper/gradle-wrapper.jar',
                'gradle/wrapper/gradle-wrapper.properties', 'app/build.gradle',
                'app/src/main/AndroidManifest.xml', 'app/src/main/assets/xposed_init'}
    if not required.issubset({path.relative_to(root).as_posix() for path in paths}):
        raise ValueError('Required build or Xposed source assets are missing')
    return sorted(paths)


def sha(path: Path) -> str:
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def dex_classes(data: bytes) -> list[str]:
    def u32(offset):
        return struct.unpack_from('<I', data, offset)[0]
    strings_off, types_off = u32(60), u32(68)
    count, classes_off = u32(96), u32(100)
    def string(index):
        pos = u32(strings_off + index * 4)
        while data[pos] & 128:
            pos += 1
        pos += 1
        return data[pos:data.index(0, pos)].decode('utf-8', errors='replace')
    return [string(u32(types_off + u32(classes_off + index * 32) * 4)) for index in range(count)]


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--project', type=Path, default=Path(__file__).resolve().parent.parent)
    parser.add_argument('--output', type=Path, help='New, empty delivery directory (default: dist/rebuilt/VERSION)')
    parser.add_argument('--android-sdk', type=Path)
    parser.add_argument('--java-home', type=Path)
    parser.add_argument('--build-tools', default='36.0.0')
    parser.add_argument('--list-source-files', action='store_true')
    args = parser.parse_args()
    root = args.project.resolve()
    paths = source_paths(root)
    if args.list_source_files:
        print('\n'.join(path.relative_to(root).as_posix() for path in paths))
        return
    gradle = (root / 'app/build.gradle').read_text(encoding='utf-8')
    version = re.search(r"versionName\s+['\"]([^'\"]+)['\"]", gradle).group(1)
    code = int(re.search(r'versionCode\s+(\d+)', gradle).group(1))
    if not re.fullmatch(r'[A-Za-z0-9._-]+', version):
        raise ValueError('Version name is not safe for a delivery filename')
    output = (args.output or root / 'dist/rebuilt' / version).resolve()
    # A delivery must never package its own output directory back into the source ZIP.
    allowed_output = root / 'dist'
    if not output.is_relative_to(allowed_output) or output == allowed_output:
        raise ValueError('Output must be a dedicated directory inside PROJECT/dist')
    if output.exists() and any(output.iterdir()):
        raise ValueError('Output directory must be empty; existing deliveries are never overwritten')
    sdk = args.android_sdk or os.environ.get('ANDROID_HOME') or os.environ.get('ANDROID_SDK_ROOT')
    if not sdk and (root / 'local.properties').exists():
        local = (root / 'local.properties').read_text(encoding='utf-8')
        match = re.search(r'^sdk\.dir=(.+)$', local, re.M)
        if match:
            sdk = match.group(1).strip().replace('\\:', ':').replace('\\\\', '\\')
    if not sdk:
        raise ValueError('Pass --android-sdk or set ANDROID_HOME')
    java_home = args.java_home or os.environ.get('JAVA_HOME')
    java = str(Path(java_home) / 'bin' / ('java.exe' if os.name == 'nt' else 'java')) if java_home else shutil.which('java')
    if not java:
        raise ValueError('Pass --java-home or set JAVA_HOME')
    build_tools = Path(sdk) / 'build-tools' / args.build_tools
    apk_input = root / 'app/build/outputs/apk/debug/app-debug.apk'
    if not apk_input.is_file():
        raise ValueError('Build assembleDebug, testDebugUnitTest and lintDebug before packaging')
    output.mkdir(parents=True, exist_ok=True)
    evidence = output / 'verification'
    evidence.mkdir()

    def command(arguments, filename):
        result = subprocess.run(arguments, cwd=root, capture_output=True, text=True, encoding='utf-8', check=True)
        text = result.stdout + result.stderr
        text = text.replace(str(root), '<project>').replace(root.as_posix(), '<project>')
        (evidence / filename).write_text(text, encoding='utf-8')
        return text

    signature = command([java, '-jar', str(build_tools / 'lib/apksigner.jar'), 'verify', '--verbose', '--print-certs', str(apk_input)], 'apk-signature.txt')
    if 'Verified using v2 scheme (APK Signature Scheme v2): true' not in signature:
        raise ValueError('Expected a verified v2 debug APK signature')
    aapt = str(build_tools / ('aapt2.exe' if os.name == 'nt' else 'aapt2'))
    badging = command([aapt, 'dump', 'badging', str(apk_input)], 'apk-badging.txt')
    manifest = command([aapt, 'dump', 'xmltree', str(apk_input), '--file', 'AndroidManifest.xml'], 'apk-manifest.txt')
    for token in ("name='dev.local.duolingoxposed'", f"versionName='{version}'", f"versionCode='{code}'", "application-label:'CleverOwl'"):
        if token not in badging:
            raise ValueError('APK metadata does not match this source checkout')
    if 'uses-permission' in badging or 'launchable-activity:' in badging:
        raise ValueError('Unexpected permission or launcher entry')
    if manifest.count('E: activity') != 1 or 'SettingsExchangeActivity' not in manifest or 'MainActivity' in manifest:
        raise ValueError('Expected the sole NoDisplay settings exchange activity')
    if 'android.intent.action.MAIN' in manifest or 'android.intent.category.LAUNCHER' in manifest:
        raise ValueError('Unexpected external settings launcher')
    if 'android:theme(0x01010000)=@0x01030055' not in manifest:
        raise ValueError('Expected the NoDisplay bridge theme')
    with zipfile.ZipFile(apk_input) as apk:
        if apk.read('assets/xposed_init').decode().strip() != 'dev.local.duolingoxposed.ModuleEntry':
            raise ValueError('Unexpected Xposed entry')
        classes = [name for entry in apk.namelist() if re.fullmatch(r'classes\d*\.dex', entry) for name in dex_classes(apk.read(entry))]
        if any(name.startswith('Lde/robv/android/xposed/') for name in classes):
            raise ValueError('Framework API classes must not be bundled')
        if 'Ldev/local/duolingoxposed/MainActivity;' in classes:
            raise ValueError('Removed external settings class is present')
        for name in ('ModuleEntry', 'SettingsClient', 'SettingsExchangeActivity'):
            if f'Ldev/local/duolingoxposed/{name};' not in classes:
                raise ValueError('Required module class missing')
        libraries = sorted(name for name in apk.namelist() if name.endswith('.so'))
        expected = sorted(f'lib/{abi}/libdexkit.so' for abi in ('arm64-v8a', 'armeabi-v7a', 'x86', 'x86_64'))
        if libraries != expected:
            raise ValueError('Unexpected native ABI set')
        with apk_input.open('rb') as stream:
            for name in libraries:
                entry = apk.getinfo(name)
                stream.seek(entry.header_offset + 26)
                name_length, extra_length = struct.unpack('<HH', stream.read(4))
                if entry.compress_type != zipfile.ZIP_STORED or (entry.header_offset + 30 + name_length + extra_length) % 16384:
                    raise ValueError('Native library is compressed or not 16KB aligned')
        if 'assets/licenses/LGPL-3.0.txt' not in apk.namelist():
            raise ValueError('Third-party license assets missing')
    suites = sorted((root / 'app/build/test-results/testDebugUnitTest').glob('TEST-*.xml'))
    if not suites:
        raise ValueError('Unit-test reports missing')
    tests = 0
    for path in suites:
        tree = ET.parse(path)
        suite = tree.getroot()
        if int(suite.get('failures', '0')) or int(suite.get('errors', '0')):
            raise ValueError('Unit tests failed')
        tests += int(suite.attrib['tests'])
        suite.attrib.pop('hostname', None)
        tree.write(evidence / path.name, encoding='utf-8', xml_declaration=True)
    lint_path = root / 'app/build/reports/lint-results-debug.txt'
    if not lint_path.exists():
        lint_path = root / 'app/build/intermediates/lint_intermediate_text_report/debug/lintReportDebug/lint-results-debug.txt'
    lint = lint_path.read_text(encoding='utf-8-sig')
    lint_result = re.search(r'(\d+) errors?, (\d+) warnings?', lint)
    if not lint_result or int(lint_result.group(1)):
        raise ValueError('Lint did not complete without errors')
    (evidence / 'lint.txt').write_text(lint.replace(str(root), '<project>').replace(root.as_posix(), '<project>'), encoding='utf-8')
    apk_output = output / f'CleverOwl-{version}-debug.apk'
    shutil.copy2(apk_input, apk_output)
    source_output = output / f'CleverOwl-{version}-source.zip'
    prefix = f'CleverOwl-{version}'
    with zipfile.ZipFile(source_output, 'w', zipfile.ZIP_DEFLATED) as source:
        for path in paths:
            archive_name = f'{prefix}/{path.relative_to(root).as_posix()}'
            if path.relative_to(root).as_posix() == 'gradlew':
                entry = zipfile.ZipInfo.from_file(path, archive_name)
                entry.create_system = 3
                entry.external_attr = 0o100755 << 16
                entry.compress_type = zipfile.ZIP_DEFLATED
                source.writestr(entry, path.read_bytes())
            else:
                source.write(path, archive_name)
    with zipfile.ZipFile(source_output) as source:
        if source.testzip() is not None:
            raise ValueError('Source archive CRC failed')
        if f'{prefix}/gradle/wrapper/gradle-wrapper.jar' not in source.namelist():
            raise ValueError('Gradle wrapper JAR missing from source archive')
    third_party = root / 'dist/third-party-sources/DexKit-2.3.0-source.zip'
    companion = output / 'third-party-sources' / third_party.name
    if third_party.exists():
        companion.parent.mkdir()
        shutil.copy2(third_party, companion)
    files = [apk_output, source_output] + ([companion] if companion.exists() else [])
    (output / 'SHA256SUMS.txt').write_text(''.join(f'{sha(path)}  {path.relative_to(output).as_posix()}\n' for path in files), encoding='utf-8')
    summary = {
        'verified_at': datetime.now(timezone(timedelta(hours=8))).isoformat(),
        'module': {'version': version, 'version_code': code, 'package': 'dev.local.duolingoxposed', 'apk_sha256': sha(apk_output)},
        'checks': {'unit_tests': tests, 'lint_errors': 0, 'lint_warnings': int(lint_result.group(2)),
                   'v2_signature': True, 'launcher': False, 'requested_permissions': [], 'native_abis_16kb_aligned': 4,
                   'source_files': len(paths), 'third_party_source_companion': companion.exists()},
        'scope': 'Existing debug build and test/lint reports verified; this command does not run Gradle or sample assertions.',
        'unverified': ['device/runtime behavior', 'server entitlements', 'cross-version compatibility', 'GitHub submission'],
    }
    (evidence / 'summary.json').write_text(json.dumps(summary, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(summary, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    try:
        main()
    except (ValueError, OSError, subprocess.CalledProcessError) as error:
        print(f'Packaging failed: {error}', file=sys.stderr)
        sys.exit(1)
