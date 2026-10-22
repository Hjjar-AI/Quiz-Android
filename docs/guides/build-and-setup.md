# Toolchain, builds and server setup

## Verification workflow

Use tools already installed on the machine and persistent, account-owned caches. A long-running build is not inherently token intensive: redirect its output to one local log, wait/poll without dumping it, then report the exit status and relevant diagnostics. Do not run a build solely for documentation edits. Ask and receive explicit user approval before creating new automated test files or temporary test scripts; relevant existing tests can be reused under the [agent policy](../../Agents.md#efficient-verification).

1. Inspect the current project requirements and wrapper version. Check `JAVA_HOME`, PATH, `ANDROID_HOME`/`ANDROID_SDK_ROOT` and ignored `local.properties`; never commit a machine-specific path.
2. Use the existing helper's `python3 build.py --check` to validate selected JDK/SDK without Gradle or downloads. If PATH selects the wrong Java, first look for an installed JDK 17 and select it for the command. A newer JDK on PATH is not proof that JDK 17 is missing.
3. Before invoking the wrapper, confirm its pinned distribution is already cached. **Gradle `--offline` does not prevent the wrapper itself downloading a missing distribution.** An already installed matching Gradle is also usable if the repository's account-local cache/output settings are retained. Do not replace the pinned version with an arbitrary system version.
4. Check for other active builds across projects/IDEs and available RAM/memory pressure immediately before starting. Use installed process/memory tools (for example, `ps` and `free -h` on Linux or Task Manager/PowerShell on Windows). Idle Gradle daemons alone are not active builds; use available memory rather than counting reclaimable cache as pressure. If another build is running or RAM usage is high/headroom insufficient, continue independent work or wait briefly and recheck. Do not launch a competing build, kill unrelated processes or clear caches. If pressure persists or cannot be assessed, finish with an explicit skipped-build reason and the exact command for the user to run manually.
5. Start with cached-only dependencies and a relevant focused check. For native changes that require compilation/resource checking, one debug build is allowed. Pass `-Pmukhtabir.autoVersion=false` to packaging commands used for verification. Do not clean caches, force dependency refreshes or repeat successful builds without a new reason.
6. If a required tool/distribution/dependency is missing, report exactly what is missing and ask the user to install or approve a **one-time persistent** installation/cache fill. Stop dependent commands while awaiting the answer; continue independent work. Do not fetch throwaway toolchains, disposable virtual environments, SDKs, browsers or emulators repeatedly for verification.
7. Broaden checks only for new failures, affected integration boundaries or an explicit request. State what actually passed and what remains unverified; compilation does not establish device, API or concurrency behavior.

A cached-distribution check and a verification build from the Android root:

```bash
python3 build.py --check
# Inspect the existing wrapper cache before any wrapper execution:
# ~/.gradle/wrapper/dists/gradle-8.7-bin/*/gradle-8.7/bin/gradle
# Only after toolchain/cache and active-build/RAM preflight succeeds:
./gradlew --offline --console=plain -Pmukhtabir.autoVersion=false assembleDebug > /tmp/mukhtabir-build.log 2>&1
```

Inspect the log tail and relevant error context instead of pasting the full file into chat. Keep caches persistent; remove task-specific temporary logs when no longer useful. On Windows use the existing `gradlew.bat` and equivalent output redirection. The version shown here comes from the current wrapper properties; recheck those properties after a future upgrade.

## Missing tools: installation handoff

| Requirement | First action | If absent |
| --- | --- | --- |
| Python 3.9+ | Check the installed interpreter; the build helper also requires Python. | Ask for a persistent system installation. |
| JDK 17 | Inspect configured Java and installed JDKs; select the existing 17 installation. | Ask to install JDK 17 through the user's normal package manager/IDE. |
| Android platform 34/build tools | Inspect the configured SDK and the project's requested packages. | Name the missing packages and ask the user to install them using their SDK Manager. |
| Pinned Gradle distribution | Inspect the existing wrapper cache or a matching installed distribution. | Ask for a one-time cached wrapper download/install; do not invoke the wrapper silently. |
| Build/test dependencies | Try the relevant cached-only task once. | Report missing coordinates and request a persistent cache fill; do not loop downloads/retries. |
| adb/device/emulator | Needed only for explicitly requested device work. Reuse an existing installation/device. | Ask for Platform-Tools or device setup; do not provision an emulator for routine source checks. |

## Existing build helper and artifact workflow

The commands below document the normal packaging interface, which can reserve versions and fill caches. For agent verification, use the cached-only, version-preserving wrapper command above after preflight; do not substitute an unqualified packaging command. Installation and release distribution need their own task authorization.

Use JDK 17 and SDK 34; set `ANDROID_HOME` or ignored `local.properties` (`sdk.dir`). The optional [build helper](../../build.py) opens an interactive input menu when run without arguments: choose the output type, cached-only dependencies and build/install or preview. It also accepts direct command-line options, checks the toolchain and reports output paths. It works from any working directory; use `python` instead of `python3` where appropriate.

```bash
python3 build.py                                # Interactive menu
python3 build.py --interactive --dry-run         # Menu with command preview only
python3 build.py --help
python3 build.py release --format aab --dry-run  # Show command; no Java/Gradle execution
python3 build.py --check                        # Check JDK/SDK; no Gradle execution
# Packaging/install commands (device installation requires an explicit request):
python3 build.py debug                          # Debug APK without prompts
python3 build.py release                        # Release APK (unsigned)
python3 build.py release --format aab            # Release AAB (unsigned)
python3 build.py debug --format aab              # Debug AAB
python3 build.py --install                      # Debug APK; install on connected compatible devices
python3 build.py --offline                      # Debug APK using cached dependencies
```

### Artifacts and caches

Release signing stays external; the helper does not create keys, sign release outputs or run automated suites. `--offline` also requires the wrapper distribution to be cached to avoid its initial download. Direct `./gradlew assembleDebug` and `./gradlew installDebug` remain available. Gradle user/dependency/daemon caches stay in the building account’s `~/.gradle`; project/task caches and module outputs use `~/.gradle/mukhtabir/<checkout-hash>/`. APK/AAB originals are in `build/app/outputs/{apk,bundle}/<variant>/` beneath that account root. Successful packaged artifacts are copied to that account’s `Desktop/Mukhtabir/<variant>/` (including Linux XDG Desktop settings; AABs use `Mukhtabir/bundles/<variant>/`), named `Mukhtabir-<version>-app-<variant>.apk`. Override Desktop with `-Pmukhtabir.desktopDir=/absolute/path` or `MUKHTABIR_DESKTOP_DIR`; older copies are retained. Existing shared `.gradle/` and `app/build/` folders are left untouched and no longer used by the wrappers. This project has no native libraries or ABI splits, so separate CPU builds are unnecessary.

### Version policy

Versioning follows the ShiftReport launcher pattern: `gradle/app-version.properties` is the shared last-reserved version across both OS accounts. Full packaging task names through `./gradlew`, `gradlew.bat` or `build.py` reserve one patch/code pair per invocation, formatted `major.minor.patch.yyyymmdd` (Asia/Damascus date). Failed/interrupted attempts retain their reservation; help, clean, IDE sync and dry runs do not increment. The POSIX helper locks the stable `gradle/` directory and atomically replaces the counter, avoiding another account’s file ownership; both accounts need write access to that source directory. Keep the version file when copying the project; do not reset it after builds. Python 3.9+ and the Asia/Damascus timezone database are required for automatic reservation. Explicit version overrides must provide both `-Pmukhtabir.versionCode=N` and `-Pmukhtabir.versionName=A.B.C.yyyymmdd`; `-Pmukhtabir.autoVersion=false` deliberately reuses the last reservation. Android Studio/direct system Gradle use the stored version; use the wrappers/helper for automatic increments. Account settings/output paths also apply during IDE sync; the IDE's Gradle User Home should remain account-local.

## Server connection

First launch groups sample practice first, server connection second and the saved-language picker afterward; server setup uses the same bounded form width. Learning/setup confirmation buttons name their action. Login options, connection-file slots, connection help and advanced practice controls share the same disclosure component; Back returns to the welcome screen before a server is configured. Enter a reachable HTTP/HTTPS origin or full deployment-prefix `/api/v1/` URL. Manual input starts blank; saved manual addresses reopen. Emulator host access uses `10.0.2.2`; a phone needs a reachable LAN address or USB `adb reverse tcp:PORT tcp:PORT` with its actual backend port. Phone `localhost` means the phone.

A build-time URL can come from `MUKHTABIR_API_BASE_URL`, ignored `server.properties` (`api.base.url`), then `local.properties`; this does not replace the phone-selected address. See [server.properties.example](../../server.properties.example).

HTTP requires confirmation and is restricted to the selected origin; HTTPS cannot downgrade. Private HTTPS needs a trusted Android-installed CA and a certificate matching the host/IP. Never disable chain/hostname validation. Align Django hosts, CSRF origins, cookies and proxy scheme with the actual endpoint: [backend startup](../../../backend/docs/guides/START_HERE.md).

### Connection files

Admin Settings can export v1 JSON; the endpoint is `bundle.connection.endpoint.api`. Files are unencrypted convenience data, not credentials or trusted server identities. Import accepts at most 64 KiB and retains three numbered slots without original filenames/URIs. Imported addresses stay hidden in controls; manual entry never copies them. Saving/replacing a slot does not activate it; switching requires confirmation.

Address changes clear sign-in/reminders, restart navigation and require reauthentication. Encrypted files remain in their local server profile. Only explicit **Same server, different address** associates an unfamiliar alternate address with that profile; imported identities never merge profiles.

## Troubleshooting order

- **Wrong Java:** select an installed JDK 17 via a command-scoped `JAVA_HOME`/PATH; do not change the repository or download another JDK before checking existing installations.
- **Missing platform/distribution/dependency:** follow the persistent-install handoff above. Repeated offline failure with the same missing item is not progress.
- **Another build holds a lock:** identify the owning task and wait or coordinate; do not delete live locks or kill unrelated processes.
- **Permission/cache ownership failure:** inspect account-local paths and wrapper settings. Do not solve it with broad permission changes, shared caches or cache deletion.
- **Compile/resource error:** read the first relevant diagnostic with surrounding source, correct the evidenced issue and rerun the affected check once.
- **Device cannot connect:** distinguish phone localhost, backend bind/port, origin/TLS trust and permissions. Preserve the security policy; never add trust-all validation.
