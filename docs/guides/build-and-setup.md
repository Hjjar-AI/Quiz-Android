# Build, cache and connection guide

## Verification workflow

Follow [agent authorization](../../Agents.md#efficient-verification): explicit confirmation before builds/compilation/packaging/extensive tests or new test files; no version changes. Documentation edits need link/consistency checks only.

1. Inspect wrapper pin/checksum, Python 3.9+, installed JDK 17 or 21 and SDK 34. Check `JAVA_HOME`/PATH, `ANDROID_HOME`/`ANDROID_SDK_ROOT`, ignored `local.properties` (`sdk.dir`). `python3 build.py --check` checks toolchain without Gradle/downloads and reports the selected JDK. Java/Kotlin compilation targets remain 17.
2. Confirm the pinned distribution is cached **before invoking the wrapper**; `--offline` does not prevent wrapper downloads. POSIX accepts `MUKHTABIR_GRADLE_HOME` or ignored `gradle.home=/absolute/path/to/gradle-8.7` (environment wins). It checks executable/pinned launcher JAR, not authenticity; invalid configuration fails. Otherwise normal wrapper lookup applies.
3. Check system-wide active builds and available RAM/pressure (`ps`, `free -h`; Windows Task Manager). Idle daemons/reclaimable cache are not active builds/pressure. If busy or headroom unknown, wait/recheck or report skipped build plus manual command; never kill unrelated jobs/delete locks/caches.
4. After confirmation, run the smallest relevant cached-only check with preserved versions; capture one log and exit status. Broaden/repeat only for changed source/new failures. Missing tools/artifacts require a named, approved persistent installation/cache fill; continue independent work, never loop disposable downloads.

```bash
python3 build.py --check
# Inspect ~/.gradle/wrapper/dists/gradle-8.7-bin/*/gradle-8.7/bin/gradle
# Only after approval and successful cache/toolchain/RAM preflight:
./gradlew --offline --console=plain -Pmukhtabir.autoVersion=false assembleDebug > /tmp/mukhtabir-build.log 2>&1
```

Read the first relevant diagnostic/log tail; retain reusable caches, remove obsolete temporary logs. Windows uses `gradlew.bat`. Compilation, suites, device and HTTP/concurrency are separate evidence categories. Ask for missing JDK/SDK packages/pinned Gradle/dependency coordinates; adb/emulators only for authorized device work.

JDK 17 and 21 are supported build runtimes for this project's pinned Gradle 8.7. Use an already-installed supported JDK; no additional installation or system-default change is needed when 21 is selected. This is separate from the Java/Kotlin target of 17 and Android SDK/minimum-device settings. The helper and direct wrapper use `JAVA_HOME` when set, otherwise Java on `PATH`; Android Studio has its own Gradle JDK selection. [Gradle Java 21 support](https://docs.gradle.org/8.5/release-notes.html#full-java-21-support) · [Android JDK/target guidance](https://developer.android.com/build/jdks).

## Build helper and artifacts

[build.py](../../build.py) works from any directory. Commands document available operations, not authorization; ordinary packaging may reserve versions/download dependencies.

| Command | Purpose |
| --- | --- |
| `python3 build.py` | Interactive output/offline/build/install menu. |
| `python3 build.py --interactive --dry-run` | Menu preview; no execution. |
| `python3 build.py --help` / `--check` | Options / toolchain check. |
| `python3 build.py debug` / `release` | Debug / unsigned release APK. |
| Add `--format aab`, `--offline`, `--dry-run` | Bundle / cached dependencies / command preview. |
| `python3 build.py --install` | Debug APK + connected-device install; requires device authorization. |

Signing/keys stay external; helper runs no suites. No native libraries/ABI splits. Account caches stay in `~/.gradle`; checkout-specific task caches/outputs in `~/.gradle/mukhtabir/<checkout-hash>/`, originals under `build/app/outputs/{apk,bundle}/<variant>/`. Shared `.gradle/` and `app/build/` remain untouched. Copies: account/XDG `Desktop/Mukhtabir/<variant>/`, AABs `Mukhtabir/bundles/<variant>/`; APK name `Mukhtabir-<version>-app-<variant>.apk`. Override with `-Pmukhtabir.desktopDir=/absolute/path` or `MUKHTABIR_DESKTOP_DIR`; older copies retained.

### Shared debug signing

Reusable procedure for multiple Linux accounts building the same Android project. Same application ID + same signing certificate lets debug APKs update one installation; Linux group membership and shared Gradle executables do not share signing identity. Android normally creates each account's key at `~/.android/debug.keystore`. See [Android signing](https://developer.android.com/studio/publish/app-signing).

#### 1. Separate shared and private state

| Item | Location/ownership |
| --- | --- |
| Compatible JDK/SDK/Gradle | Maintained shared tools, optionally under `/opt`; retain the project's pins. |
| Debug signing key | Ignored `<project-root>/.signing/debug.keystore`, readable by trusted build group. |
| Writable Gradle caches/output | Separate per account/checkout; sharing the key requires no cache changes. |
| Optional dependency seed | Shared read-only snapshot; use [snapshot setup](#dependency-snapshot-setup). |
| Release signing | Separate protected configuration; never substitute the shared debug key. |

One maintained copy per project is sufficient. Separate checkouts/machines need the same selected key via a protected copy or absolute path. Keep a protected backup outside Git; regenerating a key changes signing identity.

#### 2. Select the existing key and group

Prefer the key that signed the installed app. Compare its SHA-256 certificate with an existing APK using step 5 before choosing; an APK contains the certificate, not the private signing key. If the installed signer is unknown, do not assume update compatibility.

```bash
# Inspection only; substitute your actual group/accounts.
getent group developer
id first_account
id second_account
keytool -list -keystore "$HOME/.android/debug.keystore" -alias androiddebugkey -storepass android
```

Both accounts need membership in the selected trusted group and traversal access to every parent directory. An administrator can create a missing group/add members; new membership needs a fresh login/session. Do not change unrelated group membership or permissions.

#### 3. Copy once and restrict permissions

Run from the Android project root as the key owner, already a member of the selected group. Select the source deliberately; the example uses this account's existing debug key. The subshell stops on errors and refuses an existing destination. Review commands before applying to another project.

```bash
(
    set -eu
    BUILD_GROUP=developer
    SOURCE_DEBUG_KEY="$HOME/.android/debug.keystore"
    test -r "$SOURCE_DEBUG_KEY"
    test ! -L .signing
    test ! -e .signing/debug.keystore
    install -d -m 2750 -g "$BUILD_GROUP" .signing
    # Noclobber also refuses a target created concurrently.
    (set -C; umask 077; cat "$SOURCE_DEBUG_KEY" > .signing/debug.keystore)
    chgrp "$BUILD_GROUP" .signing/debug.keystore
    chmod 0640 .signing/debug.keystore
)
```

Directory 2750 = owner manages, group reads/traverses, others denied, group inherited. Key 0640 = owner writes, group reads, others denied. Inspect inherited ACLs with `getfacl`; group access must not grant effective key writes/other-user reads. Do not run recursive permission changes. If copying fails, verify/remove only your incomplete new copy before retrying; never replace the source or a different existing shared key.

Add `.signing/` and `local.properties` to the root `.gitignore`. Confirm with `git check-ignore .signing/debug.keystore local.properties`; ignoring does not untrack a previously committed secret. Do not commit keys or overwrite either account's original keystore.

#### 4. Configure Android Gradle signing

For another Kotlin-DSL project, add the top-level selection before `android {}` in the application module's `build.gradle.kts`. Reuse an existing properties loader if present. `PROJECT_DEBUG_KEYSTORE` is an example environment name chosen by this code, not a built-in Android option.

```kotlin
val debugSigningProperties = java.util.Properties().apply {
    val propertiesFile = rootProject.file("local.properties")
    if (propertiesFile.exists()) propertiesFile.inputStream().use { load(it) }
}
val sharedDebugKeystore = providers.environmentVariable("PROJECT_DEBUG_KEYSTORE")
    .orElse(debugSigningProperties.getProperty("debug.keystore", ""))
    .get().trim().takeIf { it.isNotEmpty() }?.let { path ->
        rootProject.file(path).also { key ->
            require(key.isFile && key.canRead()) {
                "Configured debug keystore is missing or unreadable."
            }
        }
    }
```

Merge into the existing `android` block; do not replace its other configuration:

```kotlin
android {
    signingConfigs {
        getByName("debug") {
            sharedDebugKeystore?.let { storeFile = it }
        }
    }
}
```

This retains Android's standard debug alias `androiddebugkey` and passwords `android`; a nonstandard source needs its matching credentials supplied privately. It configures debug only. An explicit unreadable/missing key fails instead of silently generating a different identity; no configured selection leaves normal per-account debug signing.

In ignored root `local.properties`, add once without replacing existing SDK/tool settings:

```properties
debug.keystore=.signing/debug.keystore
```

Or set `PROJECT_DEBUG_KEYSTORE=/absolute/path/to/debug.keystore` in the build/IDE environment; it takes precedence. Both accounts sharing this checkout read the same local property. Studio uses this Gradle configuration directly; shell environment changes require restarting Studio. On Windows use a readable local path with forward slashes in properties; Linux group permissions do not transfer automatically. [Official signing DSL guidance](https://developer.android.com/build/building-cmdline#sign_cmdline).

#### 5. Verify identity and updates

From **each account**, run these read-only checks with the same project path:

```bash
namei -l .signing/debug.keystore
stat -c '%A %U %G %n' .signing .signing/debug.keystore
getfacl -cp .signing .signing/debug.keystore
test -r .signing/debug.keystore
keytool -list -keystore .signing/debug.keystore -alias androiddebugkey -storepass android
```

SHA-256 certificate fingerprints must match. After a separately authorized build, inspect each produced APK using the installed SDK Build Tools [apksigner](https://developer.android.com/tools/apksigner):

```bash
/path/to/android-sdk/build-tools/VERSION/apksigner verify --verbose --print-certs /path/to/app-debug.apk
```

Compare the APK's signer certificate SHA-256 digest with the selected keystore and the previous installed build's APK. Only then verify an actual update on the device. Permission/fingerprint checks do not establish Gradle configuration, packaging or successful installation. Observe the target project's build/version policy; Mukhtabir's flags/helper are not universal.

| Symptom | Check/action |
| --- | --- |
| Signature mismatch persists | Wrong existing installed signer, wrong APK, different checkout/property or environment override. A higher version, clean build or cache deletion cannot repair a different signing identity. |
| Keystore unreadable | Account's active groups, directory traversal, file mode/ACL and correct path; reopen the login session after membership changes. |
| Wrong alias/password | Check source-key settings; do not regenerate/overwrite an existing signing key. |
| Version downgrade error | Separate from signatures; use the target project's legitimate version process. |
| Original signing key unavailable | Recover its backup. Otherwise a separate application ID or explicit uninstall/reinstall is needed for this ordinary debug-key setup; uninstall normally deletes local app data. |

**Mukhtabir configuration:** `.signing/debug.keystore` copies the existing `mhmmd` key; `developer` includes `mhmmd` and `work`. Local property is `debug.keystore=.signing/debug.keystore`; environment override is **`MUKHTABIR_DEBUG_KEYSTORE`**, not the generic example above. [Actual source](../../app/build.gradle.kts) · [verification limits](../verification/checklist.md#shared-debug-signing).

### Version policy

`gradle/app-version.properties` stores the shared last reservation. Wrapper/helper packaging reserves one patch/code per invocation: `major.minor.patch.yyyymmdd`, Asia/Damascus date; failures/cancellation retain it. Help/clean/sync/dry runs do not increment. Verification uses `-Pmukhtabir.autoVersion=false`; overrides require both `-Pmukhtabir.versionCode=N` and `-Pmukhtabir.versionName=A.B.C.yyyymmdd`. Preserve the file when copying. POSIX locks `gradle/` and atomically replaces the counter; both accounts need source-directory write access. Automatic reservation needs Python 3.9+/timezone data. IDE/direct Gradle use stored versions and bypass launcher aliases/reservation.

### Dependency snapshot setup

Share one published **read-only dependency snapshot**; keep writable Gradle/task/output caches account-local. Gradle's standard `GRADLE_RO_DEP_CACHE` points to the **parent of `modules-2`**. Mukhtabir's POSIX launcher also reads ignored `gradle.readOnlyCache`; environment wins. This alias is project-specific. Preserve artifact and metadata layout; Gradle 8.7 uses `metadata-2.106`. [Gradle cache guidance](https://docs.gradle.org/current/userguide/dependency_caching.html).

#### Shared location and permissions

Use `/opt/android-shared/dependency-seeds/<project>-v1` or another maintained shared directory, **outside either account's private home**. For single-account reuse, an account-local seed remains valid; never place that path in configuration shared by other accounts. Keep private home permissions intact.

A maintainer prepares a parent writable by the snapshot publisher and traversable by the trusted group; example, where that account already has permission:

```bash
getent group android-dev
id first_account
id second_account
install -d -m 2750 -g android-dev /opt/android-shared/dependency-seeds
```

Both accounts need active membership; new membership needs a fresh login. Directory 2750 lets its owner publish snapshots while the group reads/traverses. Published snapshot directories use 0550 and files 0440, group `android-dev`. Do not share writable caches or apply recursive changes to homes/tool installations.

#### Publish once from an existing cache

Choose a populated, compatible **quiescent** source. Stop/coordinate its writers first; do not stop unrelated builds or snapshot a live writable cache. Check disk space/access, then adapt source/group/project name below. It refuses an existing destination and publishes only after copying and permission setup; a failure leaves an unselected staging directory for the publisher to inspect.

```bash
python3 - <<'SNAPSHOT'
from pathlib import Path
import grp
import os
import shutil
import tempfile

source = Path('/path/to/quiescent/cache/modules-2')
parent = Path('/opt/android-shared/dependency-seeds')
target = parent / 'project-v1'
group_id = grp.getgrnam('android-dev').gr_gid
if not (source / 'files-2.1').is_dir():
    raise SystemExit('Select a populated modules-2 cache.')
if target.exists():
    raise SystemExit('Choose a fresh snapshot name.')
staging = Path(tempfile.mkdtemp(prefix='.preparing-', dir=parent))
shutil.copytree(source, staging / 'modules-2',
                ignore=shutil.ignore_patterns('*.lock', 'gc.properties'))
for item in staging.rglob('*'):
    os.chown(item, -1, group_id)
    os.chmod(item, 0o550 if item.is_dir() else 0o440)
os.chown(staging, -1, group_id)
os.chmod(staging, 0o550)
staging.rename(target)
print(target)
SNAPSHOT
```

Expected layout: `<seed>/modules-2/{files-2.1,metadata-<compatible-format>}/`. Retain complete metadata/artifacts, exclude locks/GC state, never configure partial copies or merge live metadata blindly. Consumers only read; new dependencies require an approved persistent online fill followed by a fresh snapshot. Switch between builds and retain the old seed until consumers migrate. Do not expose private dependencies to untrusted users.

#### Select and check from both accounts

For a standard Gradle project, export the shared path **before** starting the build/IDE:

```bash
export GRADLE_RO_DEP_CACHE=/opt/android-shared/dependency-seeds/project-v1
namei -l "$GRADLE_RO_DEP_CACHE/modules-2/files-2.1"
test -r "$GRADLE_RO_DEP_CACHE/modules-2/files-2.1"
getfacl -cp "$GRADLE_RO_DEP_CACHE" "$GRADLE_RO_DEP_CACHE/modules-2/files-2.1"
```

Run these read-only checks in each account's login session; also confirm metadata/artifact files are group-readable. Every parent directory needs traversal permission. A displayed path or matching group name alone does not establish access.

**Mukhtabir's actual shared configuration**, in ignored `local.properties`:

```properties
gradle.readOnlyCache=/opt/android-shared/dependency-seeds/mukhtabir-v1
```

The snapshot was copied from the existing `/opt/android-shared/.gradle/caches/modules-2`, group `android-dev` (`mhmmd`/`work`). Writable caches remain in each account's `~/.gradle`; debug signing remains independently configured under `.signing/`.

If an old environment export still points into another account's home, update it to the shared path or run `unset GRADLE_RO_DEP_CACHE` to use Mukhtabir's local property. Update persistent shell/IDE settings too; an already running IDE retains its old environment until restarted.

#### Cache access failure and recovery

`Configured read-only dependency cache must contain readable modules-2/files-2.1: /home/work/...` means launcher validation failed **before Gradle started**. The same validation currently runs for `--stop`; that error does not prove the daemon was stopped or that compilation failed.

1. Inspect both `GRADLE_RO_DEP_CACHE` and `gradle.readOnlyCache`; environment takes precedence.
2. Confirm the seed points to the parent of `modules-2`, not `files-2.1` or the live source cache.
3. Inspect `namei -l`, active groups and ACLs. Replace a private-home selection with the published shared seed; do not loosen home permissions or delete caches.
4. After access checks, retry the intended command under the project's authorization/version policy. For a separately authorized Mukhtabir verification build: `./gradlew --offline --console=plain -Pmukhtabir.autoVersion=false assembleDebug`. `./gradlew --stop` only manages the current account's daemons.

Normal mode may download missing artifacts into the writable account cache; offline fails on missing coordinates. A readable snapshot, selection message or `--version` does not prove complete dependencies/build success. Offline dependency mode is not a network sandbox for custom tasks/SDK/toolchain installers. Source/copy/permission checks remain separate from actual Gradle/device verification; [current evidence](../verification/checklist.md).

### IDE, Windows and other projects

Studio does not run the customized shell wrapper: configure Gradle JDK/User Home and inherited `GRADLE_RO_DEP_CACHE` before launch. Windows uses its pinned wrapper distribution and a Windows-readable seed; POSIX aliases/Linux paths do not apply. For other projects inspect pins/checksums/JDK/SDK/policy first; preserve their output namespaces and actual version mechanism. Moving a checkout changes its hash and can require recomputation. Do not regenerate customized wrappers as routine repair.

## Server connection

Enter a reachable HTTP/HTTPS origin or deployment-prefix `/api/v1/` URL. Manual input starts blank; saved addresses reopen. Emulator host: `10.0.2.2`; phone: reachable LAN address or `adb reverse tcp:PORT tcp:PORT` using the actual backend port. Phone localhost means the phone. Build-time fallback precedence: `MUKHTABIR_API_BASE_URL`, ignored `server.properties` (`api.base.url`), then `local.properties`; it does not override phone selection. [Example](../../server.properties.example).

HTTP requires confirmation and stays selected-origin-only; HTTPS cannot downgrade. Private HTTPS needs Android-trusted CA plus matching host/IP certificate; never disable chain/hostname validation. Align Django hosts/CSRF/cookies/proxy scheme with the endpoint: [backend startup](../../../backend/docs/guides/START_HERE.md).

### Connection files

Admin Settings exports v1 JSON: `bundle.connection.endpoint.api`. Unencrypted convenience data, not credentials/identity evidence. Import ≤64 KiB, three numbered slots, no original filenames/URIs; imported addresses stay hidden and never copy into manual input. Save/replace does not activate; switching needs confirmation.

Address changes clear sign-in/reminders, restart navigation and require reauthentication; encrypted files remain server-profile-isolated. Only explicit **Same server, different address** associates an unfamiliar alternate address; imports never merge profiles. See [offline/recovery](offline-and-recovery.md).

## Troubleshooting order

- Wrong Java: select installed JDK 17 or 21 through `JAVA_HOME` or `PATH`; check Android Studio's Gradle JDK separately. Keep Java/Kotlin compilation targets at 17.
- Missing artifacts: verify pin, seed parent/layout/readability/metadata and exact coordinates; approve persistent fill if needed. Resolution messages alone do not prove downloads.
- Locks/permissions/sandbox errors: identify owner/active writer/access cause; retain account-local writes, never root/broad chmod/live-lock deletion.
- Compile/resources: inspect first diagnostic and source; fix evidenced issue, rerun affected check once.
- Connection: distinguish phone localhost, bind/port, permissions, origin and TLS trust; preserve validation.
