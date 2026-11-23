#!/usr/bin/env python3
"""Explicit hosted operations only; definitions do not authorize local execution."""

import hashlib
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import time

from ci_reports import report_summary

ROOT = Path(__file__).resolve().parents[2]
MODULES = ('app', 'shared', 'demo')
JVM_TASKS = [f':{module}:testDebugUnitTest' for module in MODULES]
DEBUG_COMPILATION = [':app:compileDebugSources', ':demo:compileDebugSources']
RELEASE_COMPILATION = [':app:compileReleaseSources']
RETRYABLE_OPERATIONS = {'compile-debug', 'compile-release', 'debug-apk', 'demo-apk', 'release-apk'}
OPERATIONS = {
    'compile-debug': DEBUG_COMPILATION,
    'compile-release': RELEASE_COMPILATION,
    'jvm': JVM_TASKS,
    'all': [*DEBUG_COMPILATION, *RELEASE_COMPILATION, *JVM_TASKS],
    'lint': ['--continue', '-I', '.github/production-lint.init.gradle',
             *[f':{module}:lintDebug' for module in MODULES]],
    'debug-apk': [':app:assembleDebug'],
    'demo-apk': [':demo:assembleDebug'],
    'release-apk': [':app:assembleRelease'],
    'dependencies': ['-I', '.github/dependency-inventory.init.gradle', ':app:ciDependencyInventory'],
    'codeql': ['--no-build-cache', '--rerun-tasks', *DEBUG_COMPILATION],
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
    for name in ('app', 'app/src/shared', 'app/src/demo'):
        module = (ROOT / name / 'build.gradle.kts').read_text()
        if not re.search(r'compileSdk\s*=\s*34\b', module):
            raise ValueError(f'Review SDK setup when {name} compileSdk changes')
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


def dependency_rate_limit(output):
    failures = re.findall(
        r'^\* What went wrong:\n(.*?)(?=^\* (?:Try|Exception is|Get more help)|^={5,}|\Z)',
        output, re.M | re.S,
    )
    if not failures or re.search(
            r'^e: |^.*error: |Kotlin compiler error|There were failing tests', output, re.M):
        return False

    for failure in failures:
        lines = [line for line in failure.splitlines() if line.strip()]
        if len(lines) < 2 or not lines[0].startswith('Execution failed for task '):
            return False
        # Inspect every reported leaf. A separate failure or an incomplete diagnostic stops retries.
        causes = []
        for line in lines[1:]:
            cause = re.fullmatch(r'( *)> (.*)', line)
            if cause is None or (not cause[1] and not cause[2].startswith('Could not resolve ')):
                return False
            causes.append((len(cause[1]), cause[2]))
        if causes[0][0] != 0:
            return False
        for index, (indent, message) in enumerate(causes):
            has_child = index + 1 < len(causes) and causes[index + 1][0] > indent
            if not has_child and 'Received status code 429 from server:' not in message:
                return False
    return True


def run_gradle(command, directory, version, retryable):
    original = hashlib.sha256(version.read_bytes()).digest()
    log = directory / 'gradle.log'
    max_attempts = 3 if retryable else 1
    for attempt in range(1, max_attempts + 1):
        if attempt > 1:
            delay = 20 * (attempt - 1)
            print(f'Dependency resolution returned HTTP 429. Attempt {attempt}/{max_attempts} after {delay}s.',
                  flush=True)
            time.sleep(delay)
        preflight()
        attempt_log = directory / f'gradle-attempt-{attempt}.log' if retryable else log
        with attempt_log.open('wb') as stream:
            result = subprocess.run(command, cwd=ROOT, stdout=stream, stderr=subprocess.STDOUT)
        if retryable:
            with log.open('ab' if attempt > 1 else 'wb') as combined, attempt_log.open('rb') as source:
                combined.write(f'\n--- Gradle attempt {attempt} ---\n'.encode())
                shutil.copyfileobj(source, combined)
        if hashlib.sha256(version.read_bytes()).digest() != original:
            raise ValueError('Stored version changed unexpectedly; preserve evidence and investigate')
        if (result.returncode <= 0 or attempt == max_attempts
                or not dependency_rate_limit(attempt_log.read_text(encoding='utf-8', errors='replace'))):
            break
    return result.returncode, attempt


def run(operation):
    temporary = require_hosted()
    expected = temporary / 'mukhtabir-hosted'
    cache = Path(os.environ['CI_PROJECT_CACHE']).resolve()
    build = Path(os.environ['CI_APP_BUILD']).resolve()
    if cache != expected / 'project-cache' or build != expected / 'build/app':
        raise ValueError('Unexpected hosted output namespace')
    directory = ROOT / 'ci-output' / operation
    directory.mkdir(parents=True, exist_ok=True)
    version = ROOT / 'gradle/app-version.properties'
    command = [str(ROOT / 'gradlew'), '--project-cache-dir', str(cache),
               '--no-daemon', '--no-parallel', '--max-workers=1', '--console=plain',
               '--no-configuration-cache', '-Pmukhtabir.autoVersion=false',
               '-Pkotlin.compiler.execution.strategy=in-process', *OPERATIONS[operation]]
    print('Hosted operation:', operation, flush=True)
    exit_code, attempts = run_gradle(command, directory, version, operation in RETRYABLE_OPERATIONS)
    # Keep the reports for each module separate.
    details = ''
    reports_ok = True
    if operation in ('jvm', 'all', 'lint'):
        folders = ('reports',) if operation == 'lint' else ('reports', 'test-results')
        for module in MODULES:
            for folder in folders:
                source = build.parent / module / folder
                if source.is_dir():
                    shutil.copytree(source, directory / module / folder, dirs_exist_ok=True)
        details, reports_ok = report_summary(directory, MODULES, operation)
    else:
        if operation == 'demo-apk':
            build = build.parent / 'demo'
        if (build / 'reports').is_dir():
            shutil.copytree(build / 'reports', directory / 'reports', dirs_exist_ok=True)
    if operation in ('debug-apk', 'demo-apk', 'release-apk') and exit_code == 0:
        variant = 'release' if operation == 'release-apk' else 'debug'
        packages = sorted((build / 'outputs/apk' / variant).glob('*.apk'))
        if len(packages) != 1:
            raise ValueError('Expected exactly one fresh universal APK')
        package = packages[0]
        if variant == 'release':
            subprocess.run([sys.executable, str(ROOT / '.github/scripts/inspect-release.py'), str(package)],
                           cwd=ROOT, check=True)
        shutil.copy2(package, directory / package.name)
        checksum = hashlib.sha256(package.read_bytes()).hexdigest()
        (directory / 'SHA256SUMS.txt').write_text(f'{checksum}  {package.name}\n')
        if variant == 'release' and (build / 'outputs/mapping/release').is_dir():
            shutil.copytree(build / 'outputs/mapping/release', directory / 'mapping', dirs_exist_ok=True)
    text = (f'## {operation}\n\nGradle exit code: {exit_code}. Attempts: {attempts}. Stored version unchanged.\n\n'
            'Hosted output only; no device installation, publication, production signing or live API verification.\n')
    if operation in ('debug-apk', 'demo-apk'):
        text += 'Debug APK uses an ephemeral runner signer; it is not an update for an existing differently signed installation.\n'
    if operation == 'release-apk':
        text += 'Release APK is unsigned and is not an installable production distribution.\n'
    text += details
    (directory / 'result.md').write_text(text)
    with Path(os.environ['GITHUB_STEP_SUMMARY']).open('a', encoding='utf-8') as stream:
        stream.write(text)
    if exit_code:
        print(f'Operation failed with exit code {exit_code}; see retained gradle.log.', file=sys.stderr)
        return exit_code if exit_code > 0 else 128 - exit_code
    if not reports_ok:
        print('Expected module reports are incomplete or indicate failure; see retained result.md.', file=sys.stderr)
        return 1
    return 0


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
