# Mukhtabir Android


Documentation: [current map](docs/README.md) · [remaining work](docs/workPlan.md) · [source status](docs/workDone.md). Archived findings are historical; current contracts and verification gates are linked from the map.
Native Kotlin/Compose client sharing Django session/CSRF/capability/question/study/analytics contracts through Android UI.

## Current capabilities

- Login, encrypted persistent session cookies, startup restoration, logout, and password change.
- Arabic and English resources with RTL support.
- Dashboard, tips, profile, author rank, preferences, and connectivity awareness.
- Exam, study, and recall setup/sessions with navigation, timers, confidence, reflection, and results.
- Question library, details/editor, categories, authenticated images, and Markdown.
- Bookmarks, strictly user-scoped personal history, mistakes, fragile questions, analytics, planner, groups, and knowledge map.
- Capability-gated administration surfaces for users, settings, permissions, and all-user history with user filtering and session-detail review.
- Runtime limits, pagination size, and administrator role options synchronized from Django's public configuration endpoint with safe local defaults.

Master-exam management, private-draft library/editing, author preview, lifecycle, results/matrix/CSV and the selected database/maintenance tools are implemented in source. Other administration work remains in `docs/workPlan.md`; current-source build and live/device verification are separate gates.

## Technology and compatibility

- Kotlin 1.9
- Jetpack Compose and Material 3
- AndroidX Navigation, Lifecycle, and Security Crypto
- Kotlin coroutines and serialization
- OkHttp and Markwon
- Android Gradle Plugin 8.5 and Java 17
- Minimum Android 6.0 / API 23
- Compile and target SDK API 34

No native libraries/ABI filters/splits: normal APK/AAB supports ARM32/ARM64/x86/x86_64.

## Prerequisites

- JDK 17
- Android SDK Platform 34
- Compatible Android SDK build/platform tools
- A running Mukhtabir backend
- An emulator or Android 6+ physical device

Use the included Gradle wrapper so the configured Gradle version is selected.

Set `ANDROID_HOME` in your development environment to your Android SDK installation. For example, if it is installed under your home directory:

```bash
export ANDROID_HOME="$HOME/Android/Sdk"
```

Use actual SDK location via shell/IDE; no committed machine path. Android Studio may set ignored `local.properties` `sdk.dir` as unshared local override.

## API configuration

The API URL is compiled into `BuildConfig.API_BASE_URL`. Configure it in the untracked `local.properties`:

```properties
api.base.url=http://10.0.2.2:5004/api/v1/
```

`10.0.2.2` routes from the standard emulator to the development computer. For a physical device, use the computer’s reachable LAN address or reverse the backend port over USB:

```bash
adb reverse tcp:5004 tcp:5004
```

Then configure:

```properties
api.base.url=http://127.0.0.1:5004/api/v1/
```

The app now asks for the server before sign-in. HTTP requires explicit confirmation and is restricted to the selected HTTP origin; private HTTPS can use a CA installed in Android. Copy `server.properties.example` to ignored `server.properties` to set the initial field outside Kotlin. See [server configuration](docs/contracts/serverConfiguration.md).

A Gradle property overrides `local.properties`:

```bash
./gradlew -PMUKHTABIR_API_BASE_URL=https://quiz.example.com/api/v1/ bundleRelease
```

Keep the trailing `/` in the base URL.

### Runtime configuration

At startup, Android reads `GET /config/` and applies Django's current values for:

- maximum questions per quiz;
- maximum choices per authored question;
- API list page size; and
- available administrator role options.

Fallbacks: `200` questions, `8` choices, `20` page size, standard admin/moderator/member roles. Temporary config failure must not block startup; avoid screen-local constants for `/config/` values.

## Build and run

Typical commands are:

```bash
./gradlew assembleDebug
./gradlew installDebug
adb shell monkey -p com.mukhtabir.android -c android.intent.category.LAUNCHER 1
```

Release bundle:

```bash
./gradlew -PMUKHTABIR_API_BASE_URL=https://quiz.example.com/api/v1/ bundleRelease
```

Release minification is enabled. Configure and protect signing credentials outside the repository before distribution. Local project instructions require explicit approval before running Gradle commands.

## Structure

```text
app/src/main/java/com/mukhtabir/android/
├── core/           Container, preferences, capabilities, navigation, networking
├── data/auth/      Authentication repository
├── data/remote/    API client, endpoints, wire models, services, cookies, images
├── ui/             Compose application, feature screens, navigation, components
├── util/           Date and presentation utilities
├── MainActivity.kt
└── MukhtabirApplication.kt

app/src/main/res/
├── values/         Default/English resources and themes
├── values-ar/      Arabic resources
├── values-night/   Night-mode resources
└── drawable/       Launcher and drawable assets
```

`AppContainer` wires repositories and services without a separate DI framework. `MukhtabirApp` owns top-level Compose navigation. Remote models and endpoints under `data/remote` must stay aligned with Django.

## Networking, sessions, and CSRF

OkHttp stores encrypted cookies; mutations fetch/send CSRF and configured origin for HTTPS. Startup restores auth; envelopes map to user errors. Invalid credentials stay on Login; protected API 401 clears session. Request cancellation cancels HTTP; worker closes responses.

When changing an endpoint or wire model:

1. Confirm the backend route and serializer shape.
2. Update the endpoint/model/service under `data/remote`.
3. Preserve nullable/default compatibility with existing responses.
4. Verify session and CSRF behavior for mutations.
5. Add Arabic and English UI text when behavior changes.

Never log cookies, CSRF tokens, passwords, or private response bodies.

## Offline and local storage policy

Online-only study: readable content may remain in memory, but no offline packs/results/answer queue. Reconnect/Retry; ambiguous-write retry reads server status before further writes, never replays action. Connectivity banner advisory; Django authoritative.

Quiz setup, library query/mode/page/selection, bookmark/mistake/fragile notebook requested pages, History page/applied and draft user filter/expanded record ID, and the knowledge-map status filter use account-bound Android `SavedStateHandle` Bundles. A lightweight session ID/mode selects the session to recover through authenticated status/question reads. These values can survive system process recreation after Android saves the task state; they do not guarantee restoration after force-stop, task removal, or a crash before state saving. They contain no question snapshots, answers, recall text, explanations, or results. Restoration rejects a different account and rechecks blueprint/review access. History metadata is separate per own/all-users destination; personal History always requests the signed-in user, and detail snapshots reload only for rows in the restored page. Server configuration changes refresh library/notebook data preserving saved selections/requested pages. Notebook reads recover pages beyond the new last page. Bookmark changes use a non-idempotent toggle endpoint: after uncertain response, refresh the list before another change; Retry reads the list and never repeats the toggle automatically.

One unresolved question draft is now committed to separate encrypted preferences before an answer/reveal/reflection write. Recall text is also kept locally. Recovery reads the server question first, clears acknowledged drafts, and offers an explicit same-question retry only against unchanged saved progress. Django checks the submitted index and retry baseline under the session lock (deploy the matching backend change). Changed or missing sessions retain the local draft for review/confirmed discard. Login, logout and expiry attempt draft cleanup and revoke local storage access; account switching cannot reuse another account's draft. This is recovery storage, not an offline answer queue. Synchronous encryption/disk writes need device review. A lost finish response still requires checking History; direct result recovery remains planned.

Persistent application storage includes encrypted session/CSRF cookies, learner recovery/offline storage, bounded encrypted question/exam/private-draft/image authoring recovery, and plain theme/language preferences. Saved authoring drafts can be reviewed/discarded from the drawer; restoring never automatically sends a backend save. Logout, login and session expiry clear cookies and cancel HTTP calls. The HTTP client has no disk cache and attempts legacy-cache cleanup; image previews remain in memory; selected authoring image bytes can be retained in account-bound encrypted recovery. Camera output uses a restricted private temporary file that is removed after reading/cancellation and during account/server cleanup. The main manifest disables Android backup and explicitly excludes shared preferences from legacy/cloud backup and device transfer. Android task state is OS-managed, not encrypted application storage; queries, tags, selection IDs and the session identifier should be treated as private metadata. Logout removes the authenticated navigation UI; saved-state account checks prevent another account from consuming these values, without claiming forensic removal of OS task state. Device/account-switch/backup behavior remains unverified.

Authoring drafts and approved offline packs use account-scoped encryption and account/server/logout cleanup. Unknown-outcome mutations still require review before an explicit new action; no automatic replay occurs. Matching backend schema/deployment and phone verification remain prerequisites for the prepared offline synchronization and exact-result recovery features.

## Authorization and data scoping

Django remains the final authorization authority, while Android also applies the resolved capability list to navigation and actions:

- destinations for quizzes, personal history, question creation/editing, user administration, permissions, and all-user history are registered only when the required capability is present;
- `questions.edit_own` is checked against the question's current `owned_by` value inside the editor before content is shown or a mutation is submitted;
- the normal History tab always sends the signed-in user's ID; and
- the separate all-user history destination requires `tests.view_all_history` and omits the user filter only when intentionally requesting all records.

Navigation visibility is not sufficient authorization. New protected workflows must gate both the destination/action and rely on Django to enforce the request.

## Localization and accessibility

RTL declared; Arabic resources in `values-ar`. Use resource strings; check both directions/large fonts; label nondecorative icons/images.

## Debugging

See `docs/guides/Debugging.md` for ADB commands covering device discovery, logcat, activities, process/memory/CPU state, networking, port forwarding, storage, screenshots, UI inspection, permissions, and bug reports.

Quick app-focused session:

```bash
adb devices -l
adb logcat -c
adb shell monkey -p com.mukhtabir.android -c android.intent.category.LAUNCHER 1
adb logcat --pid=$(adb shell pidof com.mukhtabir.android) -v threadtime
```

## Troubleshooting

- **Emulator cannot connect:** use `10.0.2.2`, verify the port, and ensure the backend accepts the request host.
- **Physical device cannot connect:** use the host LAN address and reachable bind, or use `adb reverse` over USB.
- **HTTP is blocked:** select and confirm the actual HTTP server origin; external HTTP hosts and HTTPS-to-HTTP downgrades are blocked. See [server configuration](docs/contracts/serverConfiguration.md).
- **Login works but mutations fail:** inspect CSRF acquisition, cookies, backend trusted origins, and host consistency.
- **Images are unauthorized:** use the authenticated image repository.
- **Arabic layout is wrong:** verify Arabic resources and avoid hard-coded left/right spacing or directional icons.
- **Release-only failure:** inspect R8 output/mapping and serialization behavior.

## Release checklist

- Configure a production HTTPS API URL.
- Configure release signing outside version control.
- Verify minified navigation and serialization.
- Verify authentication, CSRF mutations, and authenticated images.
- Check Arabic RTL, English LTR, TalkBack, and large text.
- Exercise API 23 and a current Android version.
- Exercise representative ARM and x86 targets.
- Confirm no secrets or development endpoints are embedded.

## Working documents

See the [documentation index](docs/README.md) for all supporting guides.

- [Agents.md](Agents.md) — local contributor constraints
- [workPlan.md](docs/workPlan.md) — practical beta fixes and release preparation
- [laterWork.md](docs/reference/laterWork.md) — deferred feature/parity ideas, outside beta scope
- [manualVerification.md](docs/verification/manualVerification.md) — deferred manual/device checks for later review
- [workDone.md](docs/workDone.md) — completed work log
- [Debugging.md](docs/guides/Debugging.md) — general ADB debugging reference
