# Build, cache and connection guide

## Verification workflow

Follow [agent authorization](../../Agents.md#efficient-verification): explicit confirmation before builds/compilation/packaging/extensive tests or new test files; no version changes. Documentation edits need link/consistency checks only.

1. Inspect wrapper pin/checksum, Python 3.9+, installed JDK 17 or 21 and SDK 34. Check `JAVA_HOME`/PATH, `ANDROID_HOME`/`ANDROID_SDK_ROOT`, ignored `local.properties` (`sdk.dir`). `python3 build.py --check` checks toolchain without Gradle/downloads and reports the selected JDK. Java/Kotlin compilation targets remain 17.
2. Confirm the pinned distribution is cached **before invoking the wrapper**; `--offline` does not prevent wrapper downloads. Use the checksum-pinned wrapper. On this workstation it selects `/opt/android-shared/gradle/user-homes/mhmmd`; the former `gradle.home` direct-install shortcut is no longer used. Other machines retain normal wrapper lookup.
3. Check system-wide active builds and available RAM/pressure (`ps`, `free -h`; Windows Task Manager). Idle daemons/reclaimable cache are not active builds/pressure. If busy or headroom unknown, wait/recheck or report skipped build plus manual command; never kill unrelated jobs/delete locks/caches.
4. After confirmation, run the smallest relevant cached-only check with preserved versions; capture one log and exit status. Broaden/repeat only for changed source/new failures. Missing tools/artifacts require a named, approved persistent installation/cache fill; continue independent work, never loop disposable downloads.

```bash
python3 build.py --check
# Inspect $GRADLE_USER_HOME/wrapper/dists/gradle-8.7-bin/*/gradle-8.7/bin/gradle
# Only after approval and successful cache/toolchain/RAM preflight:
./gradlew --offline --console=plain -Pmukhtabir.autoVersion=false assembleDebug > /tmp/mukhtabir-build.log 2>&1
```

Read the first relevant diagnostic/log tail; retain reusable caches, remove obsolete temporary logs. Windows uses `gradlew.bat`. Compilation, suites, device and HTTP/concurrency are separate evidence categories. Ask for missing JDK/SDK packages/pinned Gradle/dependency coordinates; adb/emulators only for authorized device work.

Compose screen tests use the existing Robolectric SDK 33 runner and BOM-selected `ui-test-junit4`/`ui-test-manifest` artifacts (1.5.4). A metadata POM in the cache does not establish that the AAR/JAR is cached. Obtain a named, one-time cache-fill approval if any pinned UI-test/transitive artifact is missing; do not upgrade the BOM or repeatedly download disposable runtimes. The approved 2026-10-23 cache fill completed, and the 68-case selection passed offline with these pinned versions; future dependency changes need their own cache check. JVM UI tests do not require an emulator/device installation and do not establish device accessibility or encryption.

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

Signing/keys stay external; helper runs no suites. No native libraries/ABI splits. On this workstation, the active user home is `/opt/android-shared/gradle/user-homes/mhmmd`; future project/task caches and build outputs use `/opt/android-shared/gradle/projects/mukhtabir/cf21ef8e871a281c/`, with APK/AAB outputs under `build/app/outputs/{apk,bundle}/<variant>/`. On other machines, the existing per-checkout layout follows the effective Gradle user home. All pre-existing `.gradle/`, `app/build/`, and personal-home generated data remain untouched. Copies: account/XDG `Desktop/Mukhtabir/<variant>/`, AABs `Mukhtabir/bundles/<variant>/`; APK name `Mukhtabir-<version>-app-<variant>.apk`. Override with `-Pmukhtabir.desktopDir=/absolute/path` or `MUKHTABIR_DESKTOP_DIR`; older copies retained.

### Shared debug signing

Use the workstation notes' [shared debug-signing procedure](/home/mhmmd/Desktop/%D8%AA%D9%82%D9%86%D9%8A%D8%A9/Programming/Android/Shared%20Debug%20Signing%20Across%20Accounts.md) for key selection, one-time copying, group/ACL setup, generic Kotlin DSL, fingerprint checks and update diagnosis. Sharing Gradle caches does not align signing identities; preserve the original matching key and protected backup. Keys/local properties remain ignored, and production signing is separate. Permission/fingerprint checks do not establish an installed-device update.

**Mukhtabir configuration:** ignored `.signing/debug.keystore` copies the existing `mhmmd` key; `developer` includes `mhmmd` and `work`. Directory 2750/key 0640 need trusted-group access and traversable parents; do not change either user's private home. In root `local.properties`, preserve SDK/tool settings and select:

```properties
debug.keystore=.signing/debug.keystore
```

The actual environment override is **`MUKHTABIR_DEBUG_KEYSTORE`**. Its provider fallback occurs before trimming, so even a present blank override shadows the property; unset the override to use the property. An explicitly selected unreadable path fails; no selection retains account-local debug signing. Restart Studio after environment/group changes. [Actual source](../../app/build.gradle.kts) · [project verification limits](../verification/checklist.md#shared-debug-signing).

### Version policy

`gradle/app-version.properties` stores the shared last reservation. Wrapper/helper packaging reserves one patch/code per invocation: `major.minor.patch.yyyymmdd`, Asia/Damascus date; failures/cancellation retain it. Help/clean/sync/dry runs do not increment. Verification uses `-Pmukhtabir.autoVersion=false`; overrides require both `-Pmukhtabir.versionCode=N` and `-Pmukhtabir.versionName=A.B.C.yyyymmdd`. Preserve the file when copying. POSIX locks `gradle/` and atomically replaces the counter; both accounts need source-directory write access. Automatic reservation needs Python 3.9+/timezone data. IDE/direct Gradle use stored versions and bypass launcher aliases/reservation.

### Dependency snapshot setup

Use the workstation notes' [shared-cache procedure](/home/mhmmd/Desktop/%D8%AA%D9%82%D9%86%D9%8A%D8%A9/Programming/Android/Gradle%20Caches%20Across%20Linux%20Accounts.md) for safe publication, source quiescence, complete artifacts/metadata, ACLs, fresh-login checks and failure recovery. Keep writable caches/output per account/checkout. Do not expose private dependencies, delete live locks or loosen private homes.

**Mukhtabir's selection:** standard `GRADLE_RO_DEP_CACHE` at the directory above `modules-2`, otherwise ignored `gradle.readOnlyCache`; a nonempty environment selection wins. Gradle 8.7 uses `metadata-2.106`. Current local-property selection:

```properties
gradle.readOnlyCache=/opt/android-shared/gradle/dependency-seeds/mukhtabir-v1
```

The source record identifies `/opt/android-shared/.gradle/caches/modules-2` as the seed source and `android-dev` as the historical trusted-reader group; `mhmmd` is now the sole active account. On this workstation, writable caches now use `/opt/android-shared/gradle/user-homes/mhmmd`; the old personal cache is retained intact at `/opt/android-shared/gradle/retained-user-homes/mhmmd-personal`. Other machines keep normal per-account selection; signing is independent under `.signing/`. Publication/access evidence is in the [verification checklist](../verification/checklist.md), not proof of complete offline resolution.

If an old `GRADLE_RO_DEP_CACHE` export points into the other account's private home, select the published seed or `unset GRADLE_RO_DEP_CACHE` to use the property. Update persistent shell/IDE selection and restart the relevant process. Inspect every parent, active groups, effective ACLs and representative artifact/metadata files from both login sessions before any build.

`Configured read-only dependency cache must contain readable modules-2/files-2.1: /home/work/...` is a launcher validation failure **before Gradle**. It also affects `--stop`; the error proves neither daemon shutdown nor compilation failure. Confirm both selectors and the seed parent/layout/access, then retry only the authorized command with version preservation. See [verification workflow](#verification-workflow). Offline misses need an approved durable fill; `--offline` does not constrain wrapper/custom-task/SDK/toolchain network access.

### IDE, Windows and other projects

Studio does not run the customized shell wrapper: configure Gradle JDK/User Home and inherited `GRADLE_RO_DEP_CACHE` before launch. Windows uses its pinned wrapper distribution and a Windows-readable seed; POSIX aliases/Linux paths do not apply. For other projects inspect pins/checksums/JDK/SDK/policy first; preserve their output namespaces and actual version mechanism. Moving a checkout changes its hash and can require recomputation. Do not regenerate customized wrappers as routine repair.

## Server connection

Enter a reachable HTTP/HTTPS origin or deployment-prefix `/api/v1/` URL. Manual input starts blank; saved addresses reopen. Emulator host: `10.0.2.2`; phone: reachable LAN address or `adb reverse tcp:PORT tcp:PORT` using the actual backend port. Phone localhost means the phone. Build-time fallback precedence: Gradle property `-PMUKHTABIR_API_BASE_URL=...` (not an ordinary environment export), ignored `server.properties` (`api.base.url`), then `local.properties`; it does not override phone selection. [Example](../../server.properties.example).

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

## Shared workstation paths — 2026-11-14

[The shared Android guide](/opt/android-shared/AGENTS.md) and [current layout](/opt/android-shared/README.md) own workstation paths. All five projects select their pinned wrappers, common Gradle home, JDK 21 and registered future work directories; helpers retain portable CI defaults. Project-local `.gradle`/`.kotlin` caches and two stale old-account JDK configuration files were removed after retention/verification; `.idea`, current project build outputs, exports, credentials, signing and version counters remain unchanged. Aqaqeer's pinned 9.8.2 distribution was downloaded with authorization, verified against the official/project SHA-256 and prepared without invoking Gradle.

The separately authorized retained-data consolidation verified 4,977 dependency files and four extracted distribution trees as byte-identical to their canonical active-cache copies, then removed redundant copies and regeneratable caches (97,856 readable files, 11,478,857,070 bytes). No artifacts were missing and no identity conflicts were found; binary resolver/cache metadata was never merged. Keystores, configuration and original verification/APK outputs are preserved privately under `maintenance/organization-20261114/retained-consolidation/preserved`. The current result/administrator receipt there records the remaining old-account daemon cleanup status. Public dependency hashes, wrapper/cache paths, distribution availability, document references and whitespace are checked separately from runtime. No Gradle/build/test/lint/device command accompanied this maintenance; compilation, IDE sync, cache reuse and runtime behavior remain unverified. Earlier stage inventories are historical; removed regeneratable caches are no longer available for byte-for-byte rollback.
