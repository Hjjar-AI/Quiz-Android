# Verification status and remaining checks

Canonical record consolidating the former `checking.md` and UI review. Reusable implementation guidance stays in the [native UI](../guides/native-ui.md) and [offline/recovery](../guides/offline-and-recovery.md) guides.

## Current evidence

| Scope | Result and limit |
| --- | --- |
| Production source | Kotlin lexical/delimiter/import, shared-control argument, resource-reference and whitespace checks passed. Debug compilation/package success is recorded below; runtime remains unverified. |
| Resources/APIs | 1,817 translatable English/Arabic keys/placeholders match; bundled font and installed Compose/icon metadata inspected. Translation quality, clipping and device font behavior remain unverified. |
| Native contrast | Source calculations across 17 concrete themes reached 4.5:1 for adjusted feedback/error ink on checked surfaces and 3:1 for unselected question/choice borders. No rendered/disabled/selected/image contrast claim. |
| Build/runtime | Supplied user-run `./gradlew assembleDebug` for **0.1.9.20261022 (code 10)** succeeded in **4m 57s**: 38 tasks, 15 executed/23 up-to-date; configuration cache stored. Confirms debug compilation/packaging after the opt-in/role corrections. No suite/device/live API result; Gradle deprecations remain unspecified. |
| Documentation | Local links/heading anchors and whitespace checked; no build needed for documentation edits. |

## Review disposition

Source corrections cover isolated quiz clocks/lifecycle collection; shared fields/password reveal/progress/status; selection groups/headings; explicit font weights/fallback; cached/localized results and bounded review panes; normalized choice/sync presentation; native contrast/clickable edges; simpler Home/shared notebooks; local geometry/import cleanup; motion helpers and previews. Frozen/withheld feedback, account ownership and explicit write recovery remain authoritative. These are implementation findings, not runtime outcomes.

Material progress bars already had range semantics; unchanged Markdown already avoided reparsing and used a regular native Typeface. Route length, absent stability annotations, mixed icons or missing skeletons/transitions alone did not justify a rewrite or dependency upgrade.

## Remaining checks

| Area | Required scenarios |
| --- | --- |
| Build follow-up | Debug build passed; no repeat needed for the same source. Identify individual Gradle deprecations when requested/needed; the generic warning alone does not identify a fix. Future builds follow contention/RAM/toolchain/cache/version-preservation preflight. |
| Startup/access | Airplane-mode demo; splash/welcome/login/server Back and keyboard; connection files; downloads after failed saved-login reads; rotation/process restoration; profile isolation and actual HTTP/HTTPS/CSRF configuration. |
| Learning | Study/exam/recall setup filters/counts, shared/targeted practice, flags/bookmarks/maps and bilingual reading; withheld feedback/results. Measure timer recomposition, deadlines/local finish, master grace and overlapping exams. |
| Failure/recovery | Lost answer/start/pause/Finish/sync responses; failed reads/storage/review; duplicate/late writes; draft revisions, category exit flush and permission target changes. Retain drafts/selections/confirmed success/frozen identities; no replay or exit after failed pause. |
| Offline/accounts | Permissions/budgets/checkpoints, legacy/text-only copies/omitted images, titles/dates/rename, local completion/sync; logout/expiry/account/server isolation and no duplicate History/learning credit. |
| Accessibility/layout | Arabic/English/RTL, 48dp targets, 200% text, keyboard/TalkBack groups/headings/status, password reveal/reset/Next/Done and repeated validation focus. Themes/densities/contrast/gaps, API 23–25 font fallback/API 26+ weights, short-height/IME sheets/setup, rail/pane resizing/reader retention and drawer capability gates. Never announce timer ticks. |
| Reading/media | Reading preferences/reset, Help/Privacy, Markdown/code/tables/selection, explanations, haptics/reduced motion, invalid/bounded images and zoom/pan/sliders/reset/close, launcher on API 23/current device. |
| Server/content | Planner midnight/overlapping evidence, imports/partial outcomes/review, merges/editor conflicts/destructive labels, exports/admin permissions and large-bank/import performance. Matching schema/setup requires its own authorized scope. |

## Verification workflow

Follow [agent policy](../../Agents.md#efficient-verification) and the [build guide](../guides/build-and-setup.md#verification-workflow): use installed tools/persistent caches, preserve versions, check contention/RAM and ask before installing missing requirements. Documentation-only edits need link/consistency checks.

Replace superseded results with exact command/scenario, outcome and revision/environment when known. Keep remaining gates explicit; compilation does not establish device, accessibility, storage races or HTTP/concurrency behavior. Do not append bulk logs or recreate separate review histories.
