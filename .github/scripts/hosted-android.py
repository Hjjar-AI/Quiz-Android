#!/usr/bin/env python3
"""Explicit hosted operations only; definitions do not authorize local execution."""

import hashlib
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[2]
OPERATIONS = {
    'compile-debug': [':app:compileDebugSources'],
    'compile-release': [':app:compileReleaseSources'],
    'jvm': [':app:testDebugUnitTest'],
    'lint': ['-I', '.github/production-lint.init.gradle', ':app:lintDebug'],
    'debug-apk': [':app:assembleDebug'],
    'release-apk': [':app:assembleRelease'],
    'dependencies': ['-I', '.github/dependency-inventory.init.gradle', ':app:ciDependencyInventory'],
    'codeql': ['--no-build-cache', '--rerun-tasks', ':app:compileDebugSources'],
}


def require_hosted():
    if os.environ.get('GITHUB_ACTIONS') != 'true' or sys.platform != 'linux':
        raise ValueError('This helper requires an explicitly dispatched Linux GitHub Actions job')
    return Path(os.environ['RUNNER_TEMP']).resolve()


def prepare():
    temporary = require_hosted()
    wrapper = (ROOT / 'gradle/wrapper/gradle-wrapper.properties').read_text()
    if ('gradle-8.7-bin.zip' not in wrapper
            or not re.search(r'^distributionSha256Sum=[0-9a-f]{64}$', wrapper, re.M)):
        raise ValueError('Review CI setup when the pinned wrapper changes')
    app = (ROOT / 'app/build.gradle.kts').read_text()
    if not re.search(r'compileSdk\s*=\s*34\b', app):
        raise ValueError('Review SDK setup when compileSdk changes')
    if not os.access(ROOT / 'gradlew', os.X_OK):
        raise ValueError('Tracked wrapper must be executable')
    namespace = temporary / 'mukhtabir-hosted'
    namespace.mkdir(exist_ok=True)
    with Path(os.environ['GITHUB_ENV']).open('a', encoding='utf-8') as stream:
        stream.write(f'CI_PROJECT_CACHE={namespace / "project-cache"}\n')
        stream.write(f'CI_APP_BUILD={namespace / "build/app"}\n')
        stream.write(f'MUKHTABIR_DESKTOP_DIR={namespace / "exports"}\n')


def preflight():
    memory = Path('/proc/meminfo').read_text()
    available = int(re.search(r'^MemAvailable:\s+(\d+)', memory, re.M)[1])
    if available < 2 * 1024 * 1024:
        raise ValueError('Less than 2 GiB available RAM; do not start another Gradle operation')
    for entry in Path('/proc').iterdir():
        if not entry.name.isdecimal():
            continue
        try:
            command = (entry / 'cmdline').read_bytes().replace(b'\0', b' ')
        except (OSError, ProcessLookupError):
            continue
        if any(marker in command for marker in (
            b'org.gradle.launcher.GradleMain', b'GradleWrapperMain',
            b'GradleWorkerMain', b'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler',
        )):
            raise ValueError('An active build/compiler is already present; operation skipped')
    print(f'Hosted preflight: {available // 1024} MiB available RAM; no competing build launcher/worker observed.')


def run(operation):
    temporary = require_hosted()
    expected = temporary / 'mukhtabir-hosted'
    cache = Path(os.environ['CI_PROJECT_CACHE']).resolve()
    build = Path(os.environ['CI_APP_BUILD']).resolve()
    if cache != expected / 'project-cache' or build != expected / 'build/app':
        raise ValueError('Unexpected hosted output namespace')
    preflight()
    directory = ROOT / 'ci-output' / operation
    directory.mkdir(parents=True, exist_ok=True)
    version = ROOT / 'gradle/app-version.properties'
    before = hashlib.sha256(version.read_bytes()).digest()
    command = [str(ROOT / 'gradlew'), '--project-cache-dir', str(cache),
               '--no-daemon', '--no-parallel', '--max-workers=1', '--console=plain',
               '--no-configuration-cache', '-Pmukhtabir.autoVersion=false',
               '-Pkotlin.compiler.execution.strategy=in-process', *OPERATIONS[operation]]
    print('Hosted operation:', operation, flush=True)
    with (directory / 'gradle.log').open('w', encoding='utf-8') as stream:
        result = subprocess.run(command, cwd=ROOT, stdout=stream, stderr=subprocess.STDOUT)
    unchanged = hashlib.sha256(version.read_bytes()).digest() == before
    if not unchanged:
        raise ValueError('Stored version changed unexpectedly; preserve evidence and investigate')
    # Collect reports from the actual redirected module output, never app/build.
    if (build / 'reports').is_dir():
        shutil.copytree(build / 'reports', directory / 'reports', dirs_exist_ok=True)
    if operation == 'jvm' and (build / 'test-results').is_dir():
        shutil.copytree(build / 'test-results', directory / 'test-results', dirs_exist_ok=True)
    if operation in ('debug-apk', 'release-apk') and result.returncode == 0:
        variant = 'debug' if operation == 'debug-apk' else 'release'
        packages = sorted((build / 'outputs/apk' / variant).glob('*.apk'))
        if len(packages) != 1:
            raise ValueError('Expected exactly one fresh universal APK')
        package = packages[0]
        shutil.copy2(package, directory / package.name)
        checksum = hashlib.sha256(package.read_bytes()).hexdigest()
        (directory / 'SHA256SUMS.txt').write_text(f'{checksum}  {package.name}\n')
        if variant == 'release' and (build / 'outputs/mapping/release').is_dir():
            shutil.copytree(build / 'outputs/mapping/release', directory / 'mapping', dirs_exist_ok=True)
    text = (f'## {operation}\n\nGradle exit code: {result.returncode}. Stored version unchanged.\n\n'
            'Hosted output only; no device installation, publication, production signing or live API verification.\n')
    if operation == 'debug-apk':
        text += 'Debug APK uses an ephemeral runner signer; it is not an update for an existing differently signed installation.\n'
    if operation == 'release-apk':
        text += 'Release APK is unsigned and is not an installable production distribution.\n'
    (directory / 'result.md').write_text(text)
    with Path(os.environ['GITHUB_STEP_SUMMARY']).open('a', encoding='utf-8') as stream:
        stream.write(text)
    if result.returncode:
        print(f'Operation failed with exit code {result.returncode}; see retained gradle.log.', file=sys.stderr)
    return result.returncode


if __name__ == '__main__':
    try:
        selected = sys.argv[1]
        if selected == 'prepare':
            prepare()
        elif selected in OPERATIONS:
            sys.exit(run(selected))
        else:
            raise ValueError('Unknown hosted operation')
    except (OSError, ValueError, KeyError, IndexError) as error:
        sys.exit(f'Hosted operation incomplete: {error}')
