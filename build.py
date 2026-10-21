#!/usr/bin/env python3
"""Build existing Android variants through the project's Gradle wrapper."""

import argparse
import hashlib
import os
from pathlib import Path
import re
import shlex
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parent


def toolchain_check(install):
    """Read local configuration; do not write SDK paths or download tools."""
    java_name = "java.exe" if os.name == "nt" else "java"
    java_home = os.environ.get("JAVA_HOME")
    java = str(Path(java_home) / "bin" / java_name) if java_home else shutil.which(java_name)
    if not java or not Path(java).is_file():
        raise RuntimeError("JDK 17 is required. Set JAVA_HOME or add its bin directory to PATH.")
    result = subprocess.run([java, "-version"], capture_output=True, text=True, timeout=10)
    version = re.search(r'version\s+"(\d+)', result.stderr + result.stdout)
    if result.returncode or not version or int(version[1]) != 17:
        raise RuntimeError("Select JDK 17 through JAVA_HOME or PATH before building this project.")

    sdk_value = None
    local = ROOT / "local.properties"
    if local.is_file():
        for line in local.read_text(encoding="utf-8").splitlines():
            match = re.match(r"\s*sdk\.dir\s*[=:]\s*(.*?)\s*$", line)
            if match:
                sdk_value = match[1].replace(r"\:", ":").replace(r"\ ", " ").replace("\\\\", "\\")
                break
    sdk_value = sdk_value or os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
    if not sdk_value:
        raise RuntimeError("Set ANDROID_HOME or sdk.dir in local.properties to your Android SDK.")
    sdk = Path(sdk_value).expanduser()
    if not sdk.is_absolute():
        sdk = ROOT / sdk
    if not (sdk / "platforms" / "android-34" / "android.jar").is_file():
        raise RuntimeError("Android SDK platform 34 is missing. Install it with your SDK tools.")
    if install and not (sdk / "platform-tools" / ("adb.exe" if os.name == "nt" else "adb")).is_file():
        raise RuntimeError("Debug installation requires Android SDK Platform-Tools (adb).")


def choose(prompt, options, default=None):
    while True:
        answer = input(prompt).strip().lower()
        if not answer and default is not None:
            answer = default
        if answer in options:
            return answer
        print("Choose one of: " + ", ".join(options))


def interactive_options(args):
    print("\nMukhtabir Android build helper")
    print("  1  Debug APK")
    print("  2  Release APK (unsigned)")
    print("  3  Release AAB (unsigned)")
    print("  4  Debug AAB")
    print("  5  Build and install debug APK on connected compatible devices")
    print("  6  Check JDK/SDK/wrapper only (no Gradle)")
    print("  0  Exit")
    target = choose("Select an option: ", tuple("0123456"))
    if target == "0":
        return False
    targets = {
        "1": ("debug", "apk", False),
        "2": ("release", "apk", False),
        "3": ("release", "aab", False),
        "4": ("debug", "aab", False),
        "5": ("debug", "apk", True),
        "6": ("debug", "apk", False),
    }
    args.variant, args.format, args.install = targets[target]
    if target == "6":
        args.check = True
        return True
    if not args.offline and not args.check:
        args.offline = choose("Use cached dependencies only? [y/N]: ", ("y", "yes", "n", "no"), "n") in ("y", "yes")
    if not args.dry_run and not args.check:
        verb = "Build and install" if args.install else "Build"
        print("  1  " + verb + " now")
        print("  2  Preview command only (no Java/Gradle)")
        print("  0  Cancel")
        action = choose("Select an action: ", ("0", "1", "2"))
        if action == "0":
            return False
        args.dry_run = action == "2"
    return True


def main():
    parser = argparse.ArgumentParser(
        description=__doc__,
        epilog="Release outputs use the existing Gradle configuration; signing is not configured by this helper.",
    )
    parser.add_argument("--interactive", action="store_true", help="Open the input menu (also the default when no arguments are supplied).")
    parser.add_argument("variant", nargs="?", choices=("debug", "release"), default="debug")
    parser.add_argument("--format", choices=("apk", "aab"), default="apk", help="Output format (default: apk).")
    parser.add_argument("--install", action="store_true", help="Build/install a debug APK on connected compatible devices.")
    parser.add_argument("--offline", action="store_true", help="Use only cached Gradle dependencies; missing caches will fail.")
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument("--dry-run", action="store_true", help="Print the command without running Java or Gradle.")
    mode.add_argument("--check", action="store_true", help="Check JDK/SDK/wrapper availability without running Gradle.")
    args = parser.parse_args()
    if args.interactive or len(sys.argv) == 1:
        if not interactive_options(args):
            print("Cancelled.")
            return 0
    if args.install and (args.variant != "debug" or args.format != "apk"):
        parser.error("--install supports only debug APKs; release signing/distribution is handled separately.")

    wrapper = ROOT / ("gradlew.bat" if os.name == "nt" else "gradlew")
    if not wrapper.is_file() or not (ROOT / "gradle/wrapper/gradle-wrapper.jar").is_file():
        raise RuntimeError("The project's Gradle wrapper files are missing.")
    # Calling through sh also works when a checkout loses gradlew's executable bit.
    command = [str(wrapper)] if os.name == "nt" else ["sh", str(wrapper)]
    task = "installDebug" if args.install else (
        ("assemble" if args.format == "apk" else "bundle") + args.variant.capitalize()
    )
    command += [":app:" + task, "--console=plain"]
    if args.offline:
        command.append("--offline")
    display = subprocess.list2cmdline(command) if os.name == "nt" else shlex.join(command)
    print("Project:", ROOT, flush=True)
    if args.check:
        print("Action: Check JDK/SDK/wrapper availability; no Gradle execution.", flush=True)
    else:
        print("Command:", display, flush=True)
    if args.dry_run:
        return 0

    toolchain_check(args.install)
    if args.check:
        print("JDK 17, SDK platform 34 and wrapper files are available. Gradle was not run.")
        return 0
    if args.variant == "release":
        print("Release signing is not configured in this project; outputs need signing before distribution.", flush=True)
    result = subprocess.run(command, cwd=ROOT)
    if result.returncode:
        return result.returncode
    checkout_id = hashlib.sha256(str(ROOT).encode("utf-8")).hexdigest()[:16]
    build_root = Path.home() / ".gradle/mukhtabir" / checkout_id / "build/app"
    folder = build_root / "outputs" / ("apk" if args.format == "apk" else "bundle") / args.variant
    artifacts = sorted(folder.glob("*." + args.format))
    if artifacts:
        print("Build output (versioned copies are also exported to your Desktop):")
        for artifact in artifacts:
            print(artifact)
    else:
        print("Gradle succeeded. Check", build_root / "outputs", "for artifacts.")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (OSError, RuntimeError, subprocess.TimeoutExpired) as error:
        print("Build helper:", error, file=sys.stderr)
        sys.exit(2)
    except EOFError:
        print("\nCancelled: input ended.")
        sys.exit(0)
    except KeyboardInterrupt:
        print("\nInterrupted.")
        sys.exit(130)
