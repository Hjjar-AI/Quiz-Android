# Android regression suites

The regression sources follow the three Android modules. Main-app tests remain under `app/src/test`, shared tests live under [`app/src/shared/test`](../shared/test), and standalone demo tests live under [`app/src/demo/test`](../demo/test). See the [verification checklist](../../../docs/verification/checklist.md) for historical runs and current limits. The tests updated for the standalone split are awaiting compilation and execution.

GitHub Actions workflow **03 – Checks – Compilation and JVM** runs all three suites with `operation: jvm` and `confirm: true`. The `all` option adds main-app debug and release compilation. Reports and JUnit XML are retained separately for each module.

## Coverage map

| Area | Classes | New cases | Scenarios |
| --- | --- | ---: | --- |
| Connection files and server profiles | ServerConnectionFileTest, ServerSettingsTest | 19 | URL restrictions/private HTTP, IPv6, BOM, file byte limits, public identity metadata, authentication gates, known-profile restoration, explicit address aliases, no merging distinct profiles, stale authentication callbacks, imported-slot bounds/replacement |
| Private storage and recovery | SessionDraftStoreTest, EditorRecoveryTest, MasterExamRecoveryTest | 30 | Authenticated binding, UUID/account changes, expiry/logout, retained drafts, invalid bounds/capacity without data loss, editor keys, corrupt recovery, explicit restore/discard, frozen master attempt/question/answer identities, finish markers, independent ordinary/master drafts |
| Saved navigation | LearnerSavedStateTest | 7 | Account isolation, destination keys, applied versus draft filters, page clamps, expanded identity, immutable saved snapshots |
| Validation and images | ApiValidationDetailsTest, QuestionImageDataTest | 19 | Allowed fields/locales/indices, nested-depth/message limits, control characters, machine reasons, byte bounds despite incorrect content length, cancellation, base64 and bitmap scaling |
| Read-only localization | QuestionLocalizationTest | 13 | Regional fallback, complete question/options applied together, malformed/partial translations, answer/feedback reveal guards, confirmed feedback precedence, independent case fields and original snapshot preservation |
| Authoring input | QuestionTranslationDraftTest, ModerationTagDraftTest | 17 | Stable choice identities across reorder/add/remove, surplus stored translations, partial translation validation, locale shape, exact comma-containing tags, Unicode code-point limits and contradictory tag mutations |
| Authentication | AuthViewModelTest | 14 | No-cookie startup, credential submission/failure, duplicate-login guard, saved-session read failure/retry/denial, profile ownership and deferred permission updates, forced-password gate, forget and failed logout |
| Personal/all-user History | HistoryViewModelTest | 14 | Account query scope, submitted versus draft filters, validation, pagination reconciliation, page-size clamps, failed reads preserving rows, detail identity/retry/collapse, saved filters and duplicate reads |
| Planner | PlannerValidationTest, PlannerViewModelTest | 16 | Strict Gregorian dates, local validation, tags/categories, revision preconditions, dirty drafts, uncertain writes, accepted save followed by failed read, explicit conflict resolution, stored recovery without replay and duplicate-write guard |
| Device sessions | DeviceSessionsViewModelTest | 13 | Read failure versus empty state, preflight absence/failure, confirmed/current revocation, password guards/clearing, lost response, explicit read and acknowledgement before replay, failed reconciliation and refresh, negative acknowledgement and duplicate reads |
| Native theme contrast | NativeContrastTest | 5 | Opaque ink, light/dark and multiple surfaces, derived error ink for all theme preferences, system versus fixed theme selection |
| Shared Compose components | SharedComponentInteractionTest | 15 | Stable choice IDs/removal, disabled edits, exact tags/clear confirmation, confidence/rating/locales, recovery permissions, failed-read picker selections/paging/scrolling, export action locks/URI grants, busy-effect disposal, click-time dates and case-before-question previews |
| Main learner demo expansion | DemoRepositoryTest (24), DemoSessionTest (13), DemoScreenInteractionTest (19) | 21 added; 56 total | Confidence/recall/navigation locks, category/tag/pool/count filters, library paging/restoration, retained planner drafts and validated date/category progress, connection failure/retry, offline completion/upload deduplication, bounded pending preservation, sample profile exit confirmation, rating persistence/locks and saved-state round trips. Updated classes are uncompiled/unrun. |
| Standalone demo | StandaloneDemoTest | 6 | Launcher and permission isolation, connection and one-shot reads, recall/exam reveal, lost answer/Finish receipts, persistence/reset, and downloaded upload deduplication across History/planner/analytics |
| Existing transport and DTO suites | ApiClientTest, ApiErrorContractTest, PersistentCookieJarTest, DtoContractTest | — | CSRF, error reasons, cookies, cancellation, uncertain transport, export cleanup, decoding and outgoing service contracts |
| Existing learner and rendered UI suites | NavigationContractTest, SessionPresentationTest, DownloadedSessionSyncTest, OfflineStudyFlowTest, AppNavigationInteractionTest, QuizQuestionInteractionTest, OfflineStudyScreenInteractionTest | — | Navigation/capability gates, study/exam/recall, downloaded answer sync, local completion, restoration, case order, feedback locks, RTL/large text and answer targets |

## Test architecture and fixtures

JUnit 4, Robolectric API 33, the existing Compose test artifacts and MockWebServer remain pinned to the project dependencies. The shared and demo modules reuse the project’s existing dependency pins. HTTP ViewModel suites instantiate real ViewModels, services and ApiClient. `testing/HttpViewModelTest.kt` owns their ViewModelStores, local HTTP server and Main dispatcher; bounded response gates exercise busy guards without timing sleeps. Teardown releases held responses, clears ViewModels and closes the server/dispatcher.

The existing `TransportMasterKeysShadow`/`TransportPreferencesShadow` substitute encrypted preference creation only. These suites exercise real storage policies and recovery code, but do not verify AndroidKeyStore or ciphertext. New fixtures are synthetic and checked against current Android DTOs/services, not captured from a deployed server. Existing DTO fixture provenance remains in [contracts/README.md](resources/contracts/README.md). No frontend or backend contract changes are included.

## Run commands and approval

[Project policy](../../../Agents.md#milestone-test-approval) requires explicit approval before running these commands, including their compilation work. Run once after checking the installed/cached toolchain, system-wide active builds and available memory using the [verification workflow](../../../docs/guides/build-and-setup.md#verification-workflow). Keep output in one local log. These commands preserve versions and use the existing caches; do not fill cache misses or change dependencies without separate authorization.

Full suite, including all newly prepared classes and existing regression classes:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew :app:testDebugUnitTest :shared:testDebugUnitTest :demo:testDebugUnitTest -Pmukhtabir.autoVersion=false -Pkotlin.compiler.execution.strategy=in-process --max-workers=1 --no-parallel --no-daemon --offline --console=plain > /tmp/mukhtabir-android-regression-tests.log 2>&1
```

For a focused run, select one module task and add its `--tests` filter. Use `:shared:testDebugUnitTest` for shared validation, theme contrast, and session presentation. Use `:demo:testDebugUnitTest --tests '*.StandaloneDemoTest'` for the standalone app. The remaining examples also include main-app packages:

- Storage/connections/navigation: `--tests 'com.mukhtabir.android.core.*'`
- Transport/DTO/localization/image limits: `--tests 'com.mukhtabir.android.data.remote.*'`
- Authentication: `--tests 'com.mukhtabir.android.ui.auth.*'`
- History: `--tests 'com.mukhtabir.android.ui.history.*'`
- Planner: `--tests 'com.mukhtabir.android.ui.planner.*'`
- Device sessions: `--tests 'com.mukhtabir.android.ui.sessions.*'`
- Authoring inputs: `--tests '*.QuestionTranslationDraftTest' --tests '*.ModerationTagDraftTest'`
- Existing learner/UI regression: `--tests '*.ui.demo.*' --tests '*.ui.quiz.*' --tests '*.ui.offline.*' --tests '*.ui.navigation.*'`
- Theme contrast: `--tests 'com.mukhtabir.android.ui.theme.*'`

Approved component milestone selection (15 shared-component cases plus six existing quiz/offline interaction cases):

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew :app:testDebugUnitTest --tests '*.SharedComponentInteractionTest' --tests '*.QuizQuestionInteractionTest' --tests '*.OfflineStudyScreenInteractionTest' -Pmukhtabir.autoVersion=false -Pkotlin.compiler.execution.strategy=in-process --max-workers=1 --no-parallel --no-daemon --offline --console=plain > /tmp/mukhtabir-android-components-tests.log 2>&1
```

Each module’s JVM task discovers its test classes. Package filters provide focused suites without aggregate JUnit classes that could run cases twice during full discovery. Test counts above count methods; table-driven boundary cases add multiple assertions within individual methods.

Prepared main-demo selection (56 cases). The user requested test-file updates only and deferred **all execution and compilation**; this command requires a later explicit run request:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew :app:testDebugUnitTest --tests '*.DemoRepositoryTest' --tests '*.DemoSessionTest' --tests '*.DemoScreenInteractionTest' -Pmukhtabir.autoVersion=false -Pkotlin.compiler.execution.strategy=in-process --max-workers=1 --no-parallel --no-daemon --offline --console=plain > /tmp/mukhtabir-demo-expansion-tests.log 2>&1
```

## Remaining coverage limits

This is broad regression coverage, not complete application or measured line/branch coverage. Remaining automated gaps include end-to-end authoring editor/receipt recovery, category exit flushing, master-exam participation/deadlines/management, moderation/admin/import/export ViewModel flows, dashboard/analytics/groups/notices/library filtering, and cancellation/stale-result permutations beyond the covered guards. Add focused behavioral suites as those areas are reviewed; do not infer their verification from DTO tests or shared helpers.

No device instrumentation suite exists. Actual process death, API 23 behavior, AndroidKeyStore/encrypted files, real provider/camera handling, TalkBack/keyboard/contrast for every rendered state, reminders/notification permissions, deployed proxy/HTTP/HTTPS behavior and backend concurrency remain separate gates in the verification checklist. Local MockWebServer tests are not deployed HTTP evidence, and JVM contrast checks are not device accessibility evidence.
