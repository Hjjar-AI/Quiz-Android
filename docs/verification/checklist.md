# Verification status and remaining checks

Current evidence and pending verification only. Reusable implementation guidance stays in the [native UI](../guides/native-ui.md) and [offline/recovery](../guides/offline-and-recovery.md) guides.

## Standalone Android demo

Moved shared learner screens, controllers and resources to `app/src/shared`. Added the separate launcher, local sample services, persistent progress, reset and connection simulation under `app/src/demo`. Both app modules reference the shared library. Hosted capture selects the standalone APK and verifies that its packaged manifest omits Internet permission. Release startup capture selects the main APK.

Checked production Kotlin syntax, resource references, bilingual demo keys, source manifests and dependency boundaries, and changed Python syntax. APK builds, merged manifests, suites and device acceptance are pending. At the user’s request, removed the main app’s Try Demo entry points, retired the embedded demo and its three test classes, and set Home as the standalone start screen. Hosted capture now checks the Home screen. Verify server setup, sign-in and access to saved downloads after the cutover. See the [remaining work](../workPlan.md#standalone-demo).

The Android JVM tests now follow module ownership. Seven existing classes moved to the shared module, main-app integration tests still exercise the production routes and storage, and six standalone demo cases cover isolation, persistence and simulated recovery. Workflow 03 collects separate reports for `app`, `shared`, and `demo` on the `jvm` and `all` selections. Test compilation and execution are pending on GitHub.

## 2026-11-15 Git/GitHub adaptation

Rechecked all 15 current Aqaqeer workflow definitions, their job dependencies and helper contracts against current Mukhtabir source. [The complete audit](../guides/workflow-audit.md) accounts for each reference workflow and intentional differences; [the repository guide](../guides/git-and-github.md) owns commands/signing setup and deferred backend/Vue suggestions. Repository foundations include attributes/editor/ignore rules, source integrity checks, optional pre-push hook, review templates/CODEOWNERS/security guidance and grouped Actions dependency proposals. No backend/Vue application or repository files were changed.

Mukhtabir has 16 manual definitions (00 plus 01–15). The final recheck added 12 configuration-cache/version checks, 14 marker-owned debug Release/tag cleanup and 15 newer actionlint pin proposals; strengthened 08 with packaged identity/SDK/manifest/network-policy/permissions/assets/alignment/native-library inspection and dependent API 35 throwaway-signed minified startup/demo capture; restored 10/11's explicit draft/prerelease/release choices with artifact-only default; restored CodeQL extended queries; added 03's combined production/JVM selection. Existing versions remain unchanged. Aqaqeer's flavors/ABI splits/KSP/crypto vectors and remote version-reservation mechanism were not invented for this client.

Workflow 07 is a serial API 29/35 startup/demo/large-text visual-capture selection, with bounded recording, source/signer/installed-byte/UID/AVD provenance and ownership-checked cleanup. It is not Aqaqeer's instrumentation regression coverage; that remains pending explicitly scoped Mukhtabir test work. The same capture helper supports 08's inspected, temporary-signed release copy and a 30-second process observation; no permanent signing key is required for either emulator definition. Captures are evidence for review, not human visual/accessibility or full functional acceptance.

Signing definitions gate builds on declared readiness and isolate step-scoped secrets in dedicated environments without Gradle/cache use. User will add keys later; no keys/environments/readiness/fingerprint values were configured or inspected. Publication verifies current run/attempt/source/checksum receipts and refuses existing Releases/tags. Debug modes never become latest; only regular production Release can. Workflow 09 previews completed-run cleanup by default and preserves active/current runs, Releases/tags/source/caches; automatic scheduling stays disabled. Workflow 14 separately preserves drafts/production/unmarked/immutable Releases and at least five newest owned debug distributions, checks fresh asset/tag/source identity and confirms mutations without destructive replay. No GitHub inventory, signing, publication or deletion was executed.

Final source checks: `python3 .github/scripts/check-repository.py` passed **384 text files / 58 local links / zero problems**; 20 external/workstation and two excluded links remain unverified. `python3 .github/scripts/check-ci-policy.py` passed **16 workflows / zero problems**, including complete 00–15 inventory, resolved job dependencies, action pins/composites, permissions, publication/signing/cleanup boundaries and serial AVD controls. All 21 YAML files parsed with duplicate-key rejection, all 39 embedded shell bodies passed `bash -n`, literal helper references/publication input bindings resolved, Python AST checks passed and `git diff --check` passed. Earlier shell syntax checks for the hook and actionlint installer remain valid. These are source checks, not actionlint or hosted acceptance.

Existing CRLF-conversion warning for `gradlew.bat` remains; no mass renormalization. No Gradle/build/compilation/packaging, suite-file inspection/execution, scanner download/execution, tool installation, version change, device/emulator operation, hook activation, agent-issued commit/push or GitHub setting mutation accompanied this work. Actual runner/setup/cache/task/OSV/CodeQL/media/signing/update/concurrency/cleanup behavior remains unverified.

## 2026-11-04 question content limits

Updated ordinary/translated question editors, case editor, embedded master drafts and public-config defaults to the matching backend limits: question/explanation/case 4,000 characters, choice 500, 2–15 choices. Shared `QuestionContentLimits.kt` constants count Unicode code points for field validation. Embedded master drafts now validate and focus explanation overflow through the existing error controls. [Shared contract](../../../backend/docs/contracts/questionContentLimits.md).

Eight changed/new Kotlin source files pass lexical delimiter/import checks; English/Arabic resource XML and changed-message placeholders match. Ten protected unrelated Android file hashes remain unchanged. No Gradle, compilation, packaging, suite/test-file work, installation or version changes were performed for this milestone; existing demo changes and their separate prepared test scope below are preserved.

Remaining: authorized focused boundary coverage; matching backend-first rollout and Android rebuild under separate permission; device editing/answering with options 10–15, 4,000-character bilingual/RTL content, case translations, focus/error recovery and actual import/display behavior. Source checks do not establish compilation or device behavior.

## 2026-11-03 main learner demo expansion

User requested a more representative demo limited to the main app flows, with fake repositories/data/connections. Expanded setup with mode/pool/category/tag/count filters; added required learning confidence, question ratings, latest-answer mistakes/uncertainty lists, mode-filtered History and categorized navigation. Library paging/filters, planner drafts and scroll survive navigation. Planner categories/start/end/target feed Home and matching progress; Profile uses the real presentation with a sample user. A simulated connection controls online starts/writes and failed refresh/retry while keeping loaded content. A seeded collection supports offline study and retained pending completions; explicit simulated upload adds each completion once to History/planner/analytics. [Ownership and limits](../guides/native-ui.md#learner-demo).

Production-source checks: six demo Kotlin files passed lexical delimiter and direct API/account-storage dependency checks; 184 referenced string keys exist in both locales; 40 demo XML keys/placeholders match. Whitespace and documentation links checked. No Gradle/compilation/packaging/installation, dependency/version changes or other-client application changes in this milestone. Earlier build/test/install evidence below predates this expansion.

After test-file approval, user explicitly requested **updates only, no execution or compilation for now**. Added **21 source cases**: ten repository, three session and eight UI cases. The focused selection now contains **56 cases**: DemoRepositoryTest 24, DemoSessionTest 13 and DemoScreenInteractionTest 19. Updated existing target-bound, confidence-before-finish and empty-recall cases. Coverage includes confidence/navigation locks, empty/intersected sources and localized counts, paging/restoration, planner drafts/date/category progress, connection failure/explicit retry, retained offline completion/upload deduplication, pending limits/retention, sample profile/exit and rating persistence/locks. Kotlin lexical delimiters, unique case names, 53 bilingual resource references, documentation links and whitespace passed source checks. These changes have not been compiled or executed.

The embedded demo and this focused selection were retired during the standalone cutover.

## 2026-11-03 debug assembly and installation

User requested debug assembly and installation. System-wide preflight found no active competing build and about 4.9 GiB available RAM; installed JDK 21/SDK 34 and cached Gradle 8.7 were available. Ran offline with version reservation disabled:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew :app:assembleDebug :app:installDebug -Pmukhtabir.autoVersion=false -Pkotlin.compiler.execution.strategy=in-process --max-workers=1 --no-parallel --no-daemon --offline --console=plain > /tmp/mukhtabir-android-debug-install.log 2>&1
```

**BUILD SUCCESSFUL in 4m 31s**, 39 tasks, seven executed/32 up-to-date; installed on the single connected PPA-LX2 (Android 10). Device package inspection confirms `com.mukhtabir.android`, `DEBUGGABLE`, version **0.1.14.20261023 (code 15)**. APK: account checkout cache `build/app/outputs/apk/debug/app-debug.apk`; exported copy: `~/Desktop/Mukhtabir/debug/Mukhtabir-0.1.14.20261023-app-debug.apk`. Existing Gradle-deprecation warning remains; task-owned build/test runners exited. No version/dependency changes, app launch, device behavior/accessibility checks or test rerun in this step. Installation does not establish the pending device/API scenarios below.

## 2026-11-03 component consolidation

Implemented all reviewed opportunities: shared choice/tag editors, authoring previews, mandatory recovery prompt, confidence/rating/app-language/translation-locale selectors, searchable picker presentation, export Save/Share actions, exam summaries and date-only pickers. Adopted shared pagination/adaptive heading/search/switch controls and screen-busy effects. Existing controllers retain validation, stable IDs, focus/reveal wrappers, staged/missing selections, permissions, deadlines, drafts and uncertain-write recovery. Authoring previews now consistently show case context before the question; the public shared previews include the new fields/selectors. [Ownership and APIs](../guides/native-ui.md#shared-feature-presentation).

Production-source checks passed: lexical delimiters/strings and whitespace for 57 changed/new Kotlin files; 1,068 referenced resource keys available in English/Arabic; local component imports/callers, documentation file links and `git diff --check`.

After user approval, added 15 focused Compose cases for stable choice IDs/removal and disabled controls, exact tags/clear confirmation, confidence/rating/locales, recovery permissions, failed-read picker selections/paging/scrolling, export locks/URI grants, busy-effect disposal, click-time dates and case-before-question previews. Ran these with the four existing quiz and two offline interaction cases: **21/21 passed, zero failures/errors/skips**. App and all JVM test-source compilation succeeded; the broader suite was not executed.

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew :app:testDebugUnitTest --tests '*.SharedComponentInteractionTest' --tests '*.QuizQuestionInteractionTest' --tests '*.OfflineStudyScreenInteractionTest' -Pmukhtabir.autoVersion=false -Pkotlin.compiler.execution.strategy=in-process --max-workers=1 --no-parallel --no-daemon --offline --console=plain >> /tmp/mukhtabir-android-components-tests.log 2>&1
```

Initial run: 20/21 passed; the date-dialog test asserted before its queued callback arrived. Moved that assertion to the next idle callback, preserving the actual positive-button click and selected-date check; no production correction was needed. Complete selected rerun: **BUILD SUCCESSFUL in 1m 59s**, 28 tasks, two executed/26 up-to-date. JUnit XML confirms 15 shared-component, four quiz and two offline cases. Log: `/tmp/mukhtabir-android-components-tests.log`; reports under the account checkout cache `build/app/test-results/testDebugUnitTest/`. Used installed JDK 21, SDK 34, cached Gradle 8.7 and Robolectric API 33 offline after system-wide build/memory preflight. Task-owned Gradle/test runners exited. Existing unused shadow-parameter/Gradle-deprecation warnings remain. No APK assembly/installation, migration, dependency/version or other-client application changes.

Device checks remain: Arabic/English/RTL, TalkBack/keyboard/200% text/themes/density, validation focus, retained drafts/selections after failed reads, picker height/scrolling, date selection across midnight and saved-document sharing through an actual provider/receiving app. JVM UI/intent assertions do not establish device, accessibility, encryption or deployed HTTP/concurrency behavior. Existing deployment/recovery gates below remain.

## 2026-10-23 broader Android regression suites prepared

User requested continuing comprehensive Android test-file work. Added **167 source cases across 17 classes**, plus a shared bounded HTTP/ViewModel harness. The later component milestone brings the JVM source total to **313 cases across 32 classes**. Coverage includes server connections/profile authentication/imported slots; account-bound editor/session/master recovery; saved navigation; bounded validation/image handling; localization/reveal guards; authoring translation/tag inputs; auth restore/profile deferral; History filters/details/pagination; planner validation/revisions/uncertain saves; device revocation/reconciliation; and theme-derived contrast. See the [suite coverage map, fixture limits and commands](../../app/src/test/README.md).

Lightweight source checks passed for the 18 new Kotlin files: lexical delimiters, unique case names, whitespace and 23 static JSON fixtures. No production application files, dependencies, versions, backend or frontend source changed in that milestone. **These 167 cases compiled in the 2026-11-03 run but remain unexecuted.** Previous passing selections below predate these files and do not validate their behavior. Device/encryption/deployed HTTP/concurrency remain unverified; major remaining automated areas are listed in the suite coverage map.

User approved one full Android JVM suite run, including app/test compilation. Toolchain preflight passed: installed JDK 21, SDK 34 and the readable pinned Gradle 8.7 launcher. Execution was deferred before invoking Gradle because system-wide checks found an active ShiftReport Gradle build throughout two spaced rechecks; available RAM declined from 2.8 GiB to 2.3 GiB. No unrelated process was stopped. Approval remains valid for this suite once the machine is available. No APK assembly or installation. Approved version-preserving command:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew :app:testDebugUnitTest -Pmukhtabir.autoVersion=false -Pkotlin.compiler.execution.strategy=in-process --max-workers=1 --no-parallel --no-daemon --offline --console=plain > /tmp/mukhtabir-android-regression-tests.log 2>&1
```

## 2026-10-23 DTO, screen/navigation and offline-sync coverage verified

User requested filling the Android coverage gaps and approved the pinned dependency download, compilation and test run. Added and ran **68 cases across nine new test classes: 68 passed, zero failures/errors/skips**:

- DtoContractTest (16): production ApiClient decoding of 12 synthetic, source-reviewed analytics/admin/moderation/groups/planner payloads, fractions/nulls/exact tags, capability denial/reset, revisions and outgoing planner/moderation/visibility/import-unlock service contracts. Fixture provenance and limits: `app/src/test/resources/contracts/README.md`.
- NavigationContractTest (5) and SessionPresentationTest (7): segment/query/draft identity, capability gates, confidence/recall/navigation/pause guards and draft acknowledgement/baselines.
- DownloadedSessionSyncTest (8): real QuizViewModel/session-store/services, restore with no automatic requests, preconditioned answer confirmation, lost committed answer response reconciled without another POST, explicit conflicts/foreign identity, failed pause, local study completion versus exam restrictions.
- OfflineStudyFlowTest (11): real offline controller/store/services, no-connection local practice/restoration, answer/completion freeze, explicit sync, lost-response retry with identical frozen body, acknowledgement identity, upload denial, failed local persistence without answer loss or HTTP replay, account/expiry/server isolation and corrupt-file handling.
- AppNavigationInteractionTest (4), DemoScreenInteractionTest (11), QuizQuestionInteractionTest (4) and OfflineStudyScreenInteractionTest (2): rendered Compose screens and real navigation stack operations, save/restore, library/reset/bookmarks, study/exam/recall, pause/history/exit confirmation, RTL/200% text, wide rail, case-before-question, 48dp answer targets, production reader feedback locks and local-only offline-screen completion without HTTP.

Compose test support follows the existing BOM (1.5.4 UI artifacts): `testImplementation(ui-test-junit4)` plus debug-only `ui-test-manifest`. The approved one-time online run cached the missing pinned Compose UI-test binaries and transitives; subsequent runs succeeded offline using Gradle 8.7, installed JDK 21, SDK 34 and Robolectric API 33. App and test compilation succeeded without version upgrades or default toolchain changes. Helpers substitute AndroidKeyStore preferences and encrypted-file I/O only inside JVM tests; real storage checksums/indexes/ownership/controllers run, but encryption is not verified.

The first 68-case run passed 61 and exposed seven demo test-harness failures. Corrected selectors to distinguish displayed controls from duplicate closed-drawer labels and editable search text from results. Text-field dialog cases now use Robolectric's default width to avoid its documented [custom-width dialog idle loop](https://github.com/robolectric/robolectric/issues/8460); explicit RTL/200%-text and wide-screen qualifiers remain on their respective cases. No production application changes were needed. Focused reruns resolved the failures, followed by the complete 68-case selection below.

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew :app:testDebugUnitTest --tests '*.DtoContractTest' --tests '*.NavigationContractTest' --tests '*.ui.quiz.*' --tests '*.ui.offline.*' --tests '*.ui.navigation.*' --tests '*.DemoScreenInteractionTest' -Pmukhtabir.autoVersion=false -Pkotlin.compiler.execution.strategy=in-process --max-workers=1 --no-parallel --no-daemon --offline --console=plain
```

Final run: **BUILD SUCCESSFUL in 1m 6s**, 28 tasks, one executed/27 up-to-date. Nine JUnit XML reports under the account checkout cache `build/app/test-results/testDebugUnitTest/` confirm all 68 cases. Log (initial failures, focused corrections and final successful selection): `/tmp/mukhtabir-android-coverage-tests.log`. Existing Gradle-deprecation warnings remain. Task-owned Gradle/test runners exited; no watcher or recurring work remains.

Source/fixture/whitespace checks passed. No APK assembly, device connection, release dependency/version change or frontend modification. Saved-state/RTL/size/semantics assertions are JVM UI tests, not actual process death, TalkBack, hardware encryption, deployed HTTP/proxy or production concurrency evidence. DTO fixtures are synthetic and source-reviewed, not a live backend serialization run. The previous 63-case Android suite and backend/Vue results below predate these additions; this milestone ran the 68 new cases only and does not establish complete app coverage.

## 2026-10-23 complete Android unit suite

User-authorized full `:app:testDebugUnitTest` selection passes **63/63**, zero failures/errors/skips: ApiClientTest 22, ApiErrorContractTest 8, PersistentCookieJarTest 9, DemoRepositoryTest 14 and DemoSessionTest 10. Initial full run: 62 passed/one failed because the newly prepared interrupted-export test had not consumed its two expected requests before checking for extra traffic. Corrected the test to validate the CSRF GET and export POST, then assert no additional request; production code and cleanup/no-replay assertions are unchanged. Complete rerun: **BUILD SUCCESSFUL in 1m**, 28 tasks, two executed/26 up-to-date. App compilation up-to-date; test Kotlin recompiled successfully.

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew :app:testDebugUnitTest -Pmukhtabir.autoVersion=false -Pkotlin.compiler.execution.strategy=in-process --max-workers=1 --no-parallel --no-daemon --offline --console=plain
```

Used installed/cached Gradle 8.7, JDK 21, SDK 34 and Robolectric API 33 offline; no versions, dependencies, defaults or caches changed. Log (initial failure followed by successful rerun): `/tmp/mukhtabir-android-full-tests.log`. JUnit XML reports under the account checkout cache `build/app/test-results/testDebugUnitTest/` confirm all five class totals. No APK assembly, device instrumentation, live HTTP, encryption or UI/accessibility evidence. No device-test suite currently exists. The subsequent verified coverage above adds 68 DTO and screen/navigation/offline-sync cases; the earlier 63 cases were not rerun in that selection. Passing the existing suite does not establish complete app coverage.

## 2026-10-23 representative offline learner demo

Implemented the four learner tabs and a drawer/large-screen rail with local fake repository state: searchable/category-filtered library, details/case-before-question, bookmarks, study/exam/recall setup, pause/discard/resume, seeded and newly finished History with review, derived accuracy/activity, editable daily target and sample Profile. Finish records once; active work cannot be overwritten by another start/review. Demo state is saved only through the public saved-state registry, separate from API/account/download/preferences stores. Confirmed exit resets sample changes. Existing Arabic/English, theme/reading controls and shared presentation components remain in use.

Source checks passed: demo Kotlin lexical delimiters/resource references, bilingual XML/unique keys/placeholders, direct dependency isolation and documentation links. After explicit milestone approval, added and ran **24/24 demo unit tests**, zero failures/errors/skips: DemoRepositoryTest (14; 6.337s) and DemoSessionTest (10; 0.439s). Covers finish deduplication/frozen history/cross-screen totals, bookmark-based selection, invalid/empty selection, paused-session protection, stale updates/discard, daily targets, bounded history, fresh-demo isolation, study/recall/exam answer and deadline guards, and a saver round trip through Android Bundle/Parcel.

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew :app:testDebugUnitTest --tests 'com.mukhtabir.android.ui.demo.*' -Pmukhtabir.autoVersion=false -Pkotlin.compiler.execution.strategy=in-process --max-workers=1 --no-parallel --no-daemon --offline --console=plain
```

**BUILD SUCCESSFUL in 4m 42s**; 28 tasks, 15 executed/13 up-to-date. App and unit-test Kotlin compilation succeeded. Existing Gradle 8.7/JDK 21/SDK 34 and cached Robolectric API 33 were used offline; no dependency/version/default changes or downloads. Log: `/tmp/mukhtabir-demo-tests.log`; XML reports under the account checkout cache `build/app/test-results/testDebugUnitTest/`. Existing unused shadow-parameter and Gradle-deprecation warnings remain. Task-owned Gradle/test Java processes exited.

No APK assembly/installation, broader test run or device verification. Pending device: airplane-mode entry and absence of demo-triggered connections, search/category/empty-filter controls, exit/reset, rotation/process restoration, exam resume/timeout, Arabic/RTL/TalkBack/200% text and rail/keyboard layouts. The later user-authorized complete-suite run above also includes the API/persistence tests.

## 2026-10-23 additional lifecycle/persistence coverage prepared

Added seven ApiClient transport cases for response loss after request receipt, interrupted export cleanup, successful-login cookie/token rotation, confirmed versus failed logout, cancellation and non-JSON proxy 401 handling. Added nine PersistentCookieJar cases for stale-generation reads/writes, persistence round trips, host/path/HTTPS restrictions, expired replacement, recognized session names, server-change clearing, path identity and corrupt storage. Android selection is now 39 source cases across ApiClientTest (22), ApiErrorContractTest (8) and PersistentCookieJarTest (9).

Prepared 13 backend database-backed API cases in `tests/api/test_android_persistence_contracts.py` for real session ownership/discard, POST user-deletion safeguards, login/CSRF/account-switch persistence and exact category/comma-tag agreement across list/count/catalog/start/JSON export. Existing test factories and isolated test settings are reused. SQLite outcomes will not establish production row-lock/concurrency behavior.

Python AST, Kotlin lexical delimiters and whitespace checks passed. All 16 additional Android lifecycle/cookie cases now pass in the complete 63-test Android run above. The 13 database-backed backend cases execute in the user-authorized full backend suite; see [current backend results](../../../backend/docs/verification/pythonTests.md). No production application code, frontend source, migrations, versions or caches changed in this milestone. The earlier 23/23 Android API and 32/32 backend results below describe the narrower preceding selections.

## 2026-10-23 build-JDK preflight alignment

The build helper now accepts installed JDK 17 or 21 and reports the selected runtime, matching the documented Gradle setup. Java/Kotlin compilation targets remain 17. Updated README/build guidance to distinguish build runtime from compilation targets and Android compatibility; no JDK installation, default changes, versions or frontend application changes.

`python3 build.py --check` passed on the current JDK 21 with SDK platform 34 and wrapper files available; Python AST, documentation links and whitespace checks passed. No Gradle command, compilation, packaging or test rerun was performed for this helper/documentation correction. The earlier 23/23 Android results below remain the latest test evidence; device/deployed gates remain.

## 2026-10-23 compatibility and test milestone

Updated agent policies across Android/backend/Vue: matching post-fix versions only, scoped milestone test approval and no recurring/watch-mode verification. Frontend application source remains unchanged. Typed `CSRF_FAILED` is the only CSRF retry signal; formats still used by current Vue callers remain supported.

Focused Android run: **23/23 passed, zero failures/errors/skips** (eight ApiErrorContractTest cases; 15 ApiClientTest cases). Covers typed CSRF refresh/retry limits, non-retryable permission/uncertain failures, expiry/login behavior, app-language headers/native messages, export artifact cleanup, bodyless discard and opaque string session revocation. The successful cached run took **37s**; JUnit case totals: 0.019s helpers and 7.259s transport. Log: `/tmp/mukhtabir-android-api-tests.log`; reports under the account checkout cache `build/app/reports/tests/testDebugUnitTest/`.

Command from Android (authorized test compilation/execution):

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew :app:testDebugUnitTest --tests com.mukhtabir.android.data.remote.ApiErrorContractTest --tests com.mukhtabir.android.data.remote.ApiClientTest -Pmukhtabir.autoVersion=false -Pkotlin.compiler.execution.strategy=in-process --max-workers=1 --no-parallel --no-daemon --offline --console=plain
```

Used existing Gradle 8.7/SDK 34 and JDK 21; Java/Kotlin targets remain 17. No JDK installation, system-default changes, dependency/version changes or cache deletion. A preceding online resolution populated the missing pinned native-runtime dependency; Robolectric also cached its API 33 runtime. Initial compilation caught/corrected the centralized revocation builder's Long-versus-String handle mismatch. The first executing run passed eight helpers but failed all 15 transports because JVM tests have no AndroidKeyStore; test-only shadows now substitute encrypted preference creation while exercising real ApiClient/CookieJar behavior. Android resources are enabled for actual bilingual resource checks. Production encryption is unchanged and not established by these tests. Harmless unused shadow-parameter and existing Gradle-deprecation warnings remain.

Backend focused contracts pass **32/32**, including 29 new behavioral cases; they exposed/corrected missing password-attempt throttling on POST user deletion and omitted offline-tag default validation. [Shared evidence](../../../backend/docs/workDone.md#2026-10-23-focused-androidbackend-regression-run). No database/schema setup, migrations, full suites, APK assembly, installation, frontend application changes or live device/proxy/concurrency verification. Build/test workers exited; no recurring runner was left. Stored-data encryption, real database locks and deployed HTTP remain manual/integration gates.

## 2026-10-23 API contract corrections

Centralized all API paths and mapped stable backend reasons through 159 new bilingual strings, including direct exception displays. Retained bounded reason tokens; CSRF retry uses typed rejection for requests/exports. DELETE identifiers/options move to query parameters; administrator deletion uses the matching password-authorized POST route. JSON arrays preserve category/tag selections across bodies/query/export filters, including commas in tag names. Existing in-app locale headers, shared local-time activity data, group/blueprint management and admin latest-exam fields were confirmed in source.

Backend Android contract checks passed **3/3** (registry routes, inline-path guard, directly identifiable verbs; no database setup). Lexical delimiters/resource references passed for 17 Kotlin files; XML/duplicates and 159 new bilingual keys/placeholders passed. Python AST and whitespace checks passed. No Android build/compilation/suite/device or deployed HTTP/proxy verification. [Shared changes and command](../../../backend/docs/workDone.md#2026-10-23-android-api-findings).

Pending device/deployed verification: matching backend-first rollout; English/Arabic field errors and language changes; CSRF rotation/login/write/export retry versus ordinary permission failures; proxy host/port/scheme/trusted origins; query/POST deletions and uncertain-write review; JSON filters, comma-containing tags and legacy web requests. Unmapped dynamic Arabic validation messages use localized general guidance for English users. Existing deployment/device/build gates remain.

## 2026-10-23 review corrections

Fixed analytics formatting/bidi, heatmap latest-week positioning, username autocorrect, Unicode code-point initials, filtered-empty Reset and bilingual required-confidence guidance. Source/XML/whitespace checks passed; no build, suites or device verification.

Pending: authorized compilation; LTR/RTL heatmap scrolling/resizing, locale/IME behavior, avatar initials, empty-filter recovery and confidence guidance with TalkBack/200% text. [Optional UI priorities](../guides/native-ui.md#optional-ui-priorities).

## Shared debug signing

Configured ignored `.signing/debug.keystore` from the existing `mhmmd` key; `developer` group includes `mhmmd`/`work`, directory 2750, key 0640. Certificate/copy identity, path permissions, Git exclusion and source/whitespace checked. Gradle execution and installation remain unverified; installed-app signer is unknown. [Configuration](../guides/build-and-setup.md#shared-debug-signing).

## Current evidence

- Shared-cache path corrected to `/opt/android-shared/gradle/dependency-seeds/mukhtabir-v1`, replacing the inaccessible work-home seed. Published 1,493 files from the existing shared cache after unchanged source/copy size/mtime checks; `android-dev` read permissions and local selection checked. No Gradle command/build/download run; cache completeness remains unverified.

- Supplied user-run `./gradlew assembleDebug`: **0.1.12.20261022 (code 13)**, **10m 12s**, 38 tasks executed; configuration cache stored. This predates the 2026-10-23 changes. No device/suite/live-API result; Gradle deprecations unspecified.
- Prior source checks: 1,817 bilingual resource keys/placeholders; 17-theme native calculations reached 4.5:1 for checked feedback/error ink and 3:1 for unselected question/choice borders. These are historical source measurements, not current rendered/disabled/selected/image results.
- Code-13 build selected installed Gradle 8.7 and the persistent 331 MB read-only dependency snapshot (metadata 2.106). Earlier launcher shell/version checks passed. Selection/resolution does not establish zero downloads, complete cache coverage or future offline success.
- Documentation consolidated into existing guides; completed-review narratives removed. Current UI behavior belongs in the [native guide](../guides/native-ui.md); cache procedures belong in [build/setup](../guides/build-and-setup.md#dependency-snapshot-setup).

## Remaining checks

| Area | Required scenarios |
| --- | --- |
| Build follow-up | Current app/test compilation and the 68-case offline selection passed. Deprecation diagnostics with `--warning-mode all` remain unrun. Follow build policy for future commands or changed dependencies. |
| Startup/access | Transparent emblem on API 23/current launchers/file explorers, including themed/normalized icons and refreshed caches. Airplane-mode demo; splash/welcome/login/server Back and keyboard; connection files; downloads after failed saved-login reads; rotation/process restoration; profile isolation and actual HTTP/HTTPS/CSRF configuration. |
| Learning | Study/exam/recall setup filters/counts, shared/targeted practice, flags/bookmarks/maps and bilingual reading; withheld feedback/results. Measure timer recomposition, deadlines/local finish, master grace and overlapping exams. |
| Failure/recovery | Lost answer/start/pause/Finish/sync responses; failed reads/storage/review; duplicate/late writes; draft revisions, category exit flush and permission target changes. Retain drafts/selections/confirmed success/frozen identities; no replay or exit after failed pause. |
| Offline/accounts | Permissions/budgets/checkpoints, legacy/text-only copies/omitted images, titles/dates/rename, local completion/sync; logout/expiry/account/server isolation and no duplicate History/learning credit. |
| Accessibility/layout | Arabic/English/RTL, 48dp targets, 200% text, keyboard/TalkBack groups/headings/status, password reveal/reset/Next/Done and repeated validation focus. Themes/densities/contrast/gaps, API 23–25 font fallback/API 26+ weights, short-height/IME sheets/setup, rail/pane resizing/reader retention and drawer capability gates. Never announce timer ticks. |
| Reading/media | Reading preferences/reset, Help/Privacy, Markdown/code/tables/selection, explanations, haptics/reduced motion, invalid/bounded images and zoom/pan/sliders/reset/close, launcher on API 23/current device. |
| Server/content | Planner midnight/overlapping evidence, imports/partial outcomes/review, merges/editor conflicts/destructive labels, exports/admin permissions and large-bank/import performance. Matching schema/setup requires its own authorized scope. |

## Verification workflow

Follow [agent policy](../../Agents.md#efficient-verification) and the [build guide](../guides/build-and-setup.md#verification-workflow): use installed tools/persistent caches, preserve versions, check contention/RAM and ask before installing missing requirements. Documentation-only edits need link/consistency checks.

Replace superseded results with exact command/scenario, outcome and revision/environment when known. Keep remaining gates explicit; compilation does not establish device, accessibility, storage races or HTTP/concurrency behavior. Do not append bulk logs or recreate separate review histories.

## Shared workstation paths — 2026-11-14

[The shared Android guide](/opt/android-shared/AGENTS.md) and [current layout](/opt/android-shared/README.md) own workstation paths. All five projects select their pinned wrappers, common Gradle home, JDK 21 and registered future work directories; helpers retain portable CI defaults. Project-local `.gradle`/`.kotlin` caches and two stale old-account JDK configuration files were removed after retention/verification; `.idea`, current project build outputs, exports, credentials, signing and version counters remain unchanged. Aqaqeer's pinned 9.8.2 distribution was downloaded with authorization, verified against the official/project SHA-256 and prepared without invoking Gradle.

The separately authorized retained-data consolidation verified 4,977 dependency files and four extracted distribution trees as byte-identical to their canonical active-cache copies, then removed redundant copies and regeneratable caches (97,856 readable files, 11,478,857,070 bytes). No artifacts were missing and no identity conflicts were found; binary resolver/cache metadata was never merged. Keystores, configuration and original verification/APK outputs are preserved privately under `maintenance/organization-20261114/retained-consolidation/preserved`. The current result/administrator receipt there records the remaining old-account daemon cleanup status. Public dependency hashes, wrapper/cache paths, distribution availability, document references and whitespace are checked separately from runtime. No Gradle/build/test/lint/device command accompanied this maintenance; compilation, IDE sync, cache reuse and runtime behavior remain unverified. Earlier stage inventories are historical; removed regeneratable caches are no longer available for byte-for-byte rollback.
