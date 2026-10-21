"""Windows counterpart to the POSIX wrapper's account/version preparation."""
import hashlib
import os
from pathlib import Path
import re
import subprocess
import sys
from next_app_version import reserve


def main():
    root = Path(sys.argv[1]).resolve()
    args = sys.argv[2:]
    home = Path.home()
    checkout_id = hashlib.sha256(str(root).encode("utf-8")).hexdigest()[:16]
    options = ["--gradle-user-home", str(home / ".gradle"), "--project-cache-dir",
               str(home / ".gradle/mukhtabir" / checkout_id / "project-cache")]
    has_code = any(a.startswith("-Pmukhtabir.versionCode=") for a in args)
    has_name = any(a.startswith("-Pmukhtabir.versionName=") for a in args)
    if has_code != has_name:
        raise ValueError("Supply both mukhtabir.versionCode and mukhtabir.versionName, or neither.")
    skip = any(a in {"--dry-run", "-m", "--help", "-h", "--version", "-v", "-Pmukhtabir.autoVersion=false"} for a in args)
    packaging = any(re.fullmatch(r"(?:assemble|bundle|install|package|export).*|build|buildNeeded|buildDependents",
                               a.rsplit(":", 1)[-1]) for a in args)
    if packaging and not skip and not has_code:
        code, name = reserve(root / "gradle/app-version.properties")
        options += [f"-Pmukhtabir.versionCode={code}", f"-Pmukhtabir.versionName={name}"]
        print(f"Mukhtabir build: {name} (code {code})", file=sys.stderr, flush=True)
    environment = os.environ.copy()
    environment["MUKHTABIR_WRAPPER_PREPARED"] = "1"
    return subprocess.run([str(root / "gradlew.bat"), *options, *args], cwd=root, env=environment).returncode


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (OSError, ValueError, KeyError) as error:
        raise SystemExit(f"Build preparation failed: {error}")
