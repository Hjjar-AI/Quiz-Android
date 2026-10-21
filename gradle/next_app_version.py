#!/usr/bin/env python3
"""Reserve one shared patch/code pair before an Android packaging invocation."""
from datetime import datetime
import os
from pathlib import Path
import re
import sys
import tempfile
from zoneinfo import ZoneInfo


def reserve(path):
    path = Path(path).resolve()
    # Atomic replacement needs parent-directory rights, not write access to a file
    # created by the other account. Lock the stable directory across replacements.
    if os.name == "nt":
        import msvcrt
        lock_file = open(path.parent / ".app-version.lock", "a+b")
        if lock_file.seek(0, os.SEEK_END) == 0:
            lock_file.write(b"0")
            lock_file.flush()
        lock_file.seek(0)
        msvcrt.locking(lock_file.fileno(), msvcrt.LK_LOCK, 1)
        directory = None
    else:
        import fcntl
        directory = os.open(path.parent, os.O_RDONLY)
        fcntl.flock(directory, fcntl.LOCK_EX)
        lock_file = None
    try:
        values = {}
        for line in path.read_text(encoding="utf-8").splitlines():
            if line.strip() and not line.lstrip().startswith("#"):
                key, value = line.split("=", 1)
                values[key.strip()] = value.strip()
        previous_code = int(values["versionCode"])
        match = re.fullmatch(r"(\d+)\.(\d+)\.(\d+)(?:\.\d{8})?", values["versionName"])
        if not match or not 1 <= previous_code < 2_100_000_000:
            raise ValueError("Invalid app version counter; repair it explicitly rather than resetting it.")
        code = previous_code + 1
        major, minor, patch = match.groups()
        date = datetime.now(ZoneInfo("Asia/Damascus")).strftime("%Y%m%d")
        name = f"{major}.{minor}.{int(patch) + 1}.{date}"
        descriptor, temporary = tempfile.mkstemp(prefix=".app-version-", dir=path.parent)
        try:
            with os.fdopen(descriptor, "w", encoding="utf-8") as output:
                if os.name != "nt":
                    os.fchmod(output.fileno(), 0o644)
                output.write("# Last reserved app version. Keep this file when moving between PCs.\n")
                output.write(f"versionCode={code}\nversionName={name}\n")
                output.flush()
                os.fsync(output.fileno())
            os.replace(temporary, path)
            if directory is not None:
                os.fsync(directory)
        finally:
            if os.path.exists(temporary):
                os.unlink(temporary)
        return code, name
    finally:
        if directory is not None:
            os.close(directory)
        if lock_file is not None:
            lock_file.seek(0)
            msvcrt.locking(lock_file.fileno(), msvcrt.LK_UNLCK, 1)
            lock_file.close()


if __name__ == "__main__":
    try:
        if len(sys.argv) != 2:
            raise ValueError("Expected the app-version.properties path.")
        code, name = reserve(sys.argv[1])
        print(f"{code} {name}")
    except (OSError, ValueError, KeyError) as error:
        raise SystemExit(f"Could not reserve app version: {error}")
