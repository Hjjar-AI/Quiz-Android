# Verification status and remaining checks

Current evidence and pending verification only. Reusable implementation guidance stays in the [native UI](../guides/native-ui.md) and [offline/recovery](../guides/offline-and-recovery.md) guides.

## 2026-10-23 review corrections

Fixed analytics formatting/bidi, heatmap latest-week positioning, username autocorrect, Unicode code-point initials, filtered-empty Reset and bilingual required-confidence guidance. Source/XML/whitespace checks passed; no build, suites or device verification.

Pending: authorized compilation; LTR/RTL heatmap scrolling/resizing, locale/IME behavior, avatar initials, empty-filter recovery and confidence guidance with TalkBack/200% text. [Optional UI priorities](../guides/native-ui.md#optional-ui-priorities).

## Shared debug signing

Configured ignored `.signing/debug.keystore` from the existing `mhmmd` key; `developer` group includes `mhmmd`/`work`, directory 2750, key 0640. Certificate/copy identity, path permissions, Git exclusion and source/whitespace checked. Gradle execution and installation remain unverified; installed-app signer is unknown. [Configuration](../guides/build-and-setup.md#shared-debug-signing).

## Current evidence

- Shared-cache path corrected to `/opt/android-shared/dependency-seeds/mukhtabir-v1`, replacing the inaccessible work-home seed. Published 1,493 files from the existing shared cache after unchanged source/copy size/mtime checks; `android-dev` read permissions and local selection checked. No Gradle command/build/download run; cache completeness remains unverified.

- Supplied user-run `./gradlew assembleDebug`: **0.1.12.20261022 (code 13)**, **10m 12s**, 38 tasks executed; configuration cache stored. This predates the 2026-10-23 changes. No device/suite/live-API result; Gradle deprecations unspecified.
- Prior source checks: 1,817 bilingual resource keys/placeholders; 17-theme native calculations reached 4.5:1 for checked feedback/error ink and 3:1 for unselected question/choice borders. These are historical source measurements, not current rendered/disabled/selected/image results.
- Code-13 build selected installed Gradle 8.7 and the persistent 331 MB read-only dependency snapshot (metadata 2.106). Earlier launcher shell/version checks passed. Selection/resolution does not establish zero downloads, complete cache coverage or future offline success.
- Documentation consolidated into existing guides; completed-review narratives removed. Current UI behavior belongs in the [native guide](../guides/native-ui.md); cache procedures belong in [build/setup](../guides/build-and-setup.md#dependency-snapshot-setup).

## Remaining checks

| Area | Required scenarios |
| --- | --- |
| Build follow-up | Authorized compilation of 2026-10-23 changes; deprecation diagnostics with `--warning-mode all`; offline-cache completeness. Follow build policy. |
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
