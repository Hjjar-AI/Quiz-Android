# Verification status and remaining checks

Canonical record consolidating the former `checking.md` and UI review. Reusable implementation guidance stays in the [native UI](../guides/native-ui.md) and [offline/recovery](../guides/offline-and-recovery.md) guides.

## Current evidence

| Scope | Result and limit |
| --- | --- |
| Production source | Source/whitespace, XML and cached Material3 1.1.2 signature checks were followed by the supplied successful code-13 debug build below. Preview rendering and device/runtime checks remain pending. |
| Resources/APIs | 1,817 translatable English/Arabic keys/placeholders match; bundled font and installed Compose/icon metadata inspected. Translation quality, clipping and device font behavior remain unverified. |
| Native contrast | Source calculations across 17 concrete themes reached 4.5:1 for adjusted feedback/error ink on checked surfaces and 3:1 for unselected question/choice borders. No rendered/disabled/selected/image contrast claim. |
| Build/runtime | Supplied user-run `./gradlew assembleDebug` for **0.1.12.20261022 (code 13)** succeeded in **10m 12s**: 38 actionable tasks, all executed; configuration cache stored. Confirms debug compilation/packaging of the accumulated design/API/icon changes. No suite/device/live API result; Gradle deprecations remain unspecified. |
| Dependency reuse | Created a persistent 331 MB snapshot of the existing shared `modules-2` cache, excluding locks/GC state; metadata format is 2.106. Ignored local configuration now supplies it through `GRADLE_RO_DEP_CACHE`. Shell syntax and `./gradlew --version` launcher selection passed. The supplied code-13 build selected this seed and completed dependency resolution/packaging. Its normal-mode log does not establish zero downloads, complete cache coverage or future offline success. |
| Gradle launcher | POSIX wrapper now reuses the pinned installed distribution via ignored `local.properties`/`MUKHTABIR_GRADLE_HOME`, retaining account-local caches. `sh -n gradlew` passed; `./gradlew --version` returned Gradle 8.7 without downloading after sandbox cache-access restrictions were lifted. The subsequent supplied code-13 build also selected installed Gradle 8.7 and succeeded. |
| Documentation | Local links/heading anchors and whitespace checked; no build needed for documentation edits. |

## Review disposition

Source corrections cover isolated quiz clocks/lifecycle collection; shared fields/password reveal/progress/status; selection groups/headings; explicit font weights/fallback; cached/localized results and bounded review panes; normalized choice/sync presentation; native contrast/clickable edges; simpler Home/shared notebooks; local geometry/import cleanup; motion helpers and previews. Frozen/withheld feedback, account ownership and explicit write recovery remain authoritative. These are implementation findings, not runtime outcomes.

Material progress bars already had range semantics; unchanged Markdown already avoided reparsing and used a regular native Typeface. Route length, absent stability annotations, mixed icons or missing skeletons/transitions alone did not justify a rewrite or dependency upgrade.

## Design-suite review disposition

Checked `revvvv.md` against current source and the native UI guide; its archive counts and visual impressions are not runtime evidence.

| Review topic | Disposition |
| --- | --- |
| Profile rows and font weights | Replaced private rows with adaptive `AppMetricRow`; removed forced profile weights. Theme emphasis now uses the shared font-capability decision, including the API 23–25 regular fallback, for all heading/label roles. |
| Shell feedback | Pending-link, offline and expiry notices use `AppBanner`: shared status icons/live regions and tone-derived fill with ink checked against that fill. Inline status icons use the small icon token. |
| Selection controls | Simple repeated checkbox rows now use `AppCheckboxRow`, preserving callback/enabled guards and giving the label the same target. Category radios share `AppRadioRow` and selectable groups; icon selection also respects the editable guard. Weight fields remain outside checkbox semantics. Composite permission descriptions and user-card headings retain their specialized layouts. |
| Repeated constants | Shared disabled alpha, choice tints, explanation/scroll opacity, inline/small icon sizes, reading-surface blends and font-scale thresholds replace duplicated values without changing the existing scales. Distinct content widths remain distinct. |
| Startup | Light launch background/wordmark now match the default light Compose surface/primary. Night launch already matches the dark surface. OS splash resources follow system appearance; they cannot promise a saved custom palette before Compose starts. |
| Previews | Existing gallery now includes banners, disabled selection controls, radio groups, metric rows and combined narrow Arabic/200% text/default and Compact cases. Rendering remains pending. |
| Density, borders, animation | Tight default/Compact spacing, 48dp targets, subdued unrevealed selection, 1dp revealed-answer borders and no answer-feedback animations are intentional native UI policies. Shared semantic spacing values and local geometry are valid; equality/off-grid values alone are not defects. |
| Typography and colors | Titles now use 22/19/17sp, labels 15/14/13sp and headlines 34/30/26sp, with Arabic floors and real weights retained. New hierarchy/wrapping remains unrendered. Base palettes and density remain unchanged; reading preferences target reading content, not every UI caption. |
| Charts | Current line chart uses primary and bar chart tertiary (the review reverses them). They are separate single-series charts with markers/summaries/detailed values, not unlabeled competing series. Extra categorical/diverging palettes and size modes are feature proposals. |
| Shared design APIs | Explicit Small/Medium/Large buttons share Filled/Secondary/Outlined/Text and Default/Danger modes; existing entry points retain their defaults. Shared screen bar, tab/rail icon pill, semantic shape/surface aliases and Half/Standard/Full sheet caps are implemented; the question map uses Full. Preview sources cover button modes/type hierarchy and interactive sheet/bar cases. Debug compilation/packaging passed in the supplied code-13 build; rendering remains pending. |
| Future modes/patterns | Common chart-series, progress sizes, state motion and haptic intents are a bounded shortlist in the native UI guide. Independent theme axes, universal component modes, page/async/form/filter wrappers and a combined sound/haptic event remain optional. Named themes, recovery ownership and system motion settings remain authoritative. No new CI automation or test files were created. |

## Remaining checks

| Area | Required scenarios |
| --- | --- |
| Build follow-up | Debug compilation/packaging passed for code 13; no repeat needed for the same source. Identify individual Gradle deprecations with `--warning-mode all` during a future authorized run; the generic warning alone does not identify a fix. Offline cache completeness remains unverified. Future builds follow confirmation/contention/RAM/toolchain/cache/version-preservation policy. |
| Startup/access | Transparent emblem on API 23/current launchers and APK/file explorers; compare normal and themed/normalized icon modes and refreshed launcher caches. PNG master and all seven density exports have alpha-zero exterior pixels; adaptive launcher overrides and round-icon declaration removed. Installed rendering remains unverified. Airplane-mode demo; splash/welcome/login/server Back and keyboard; connection files; downloads after failed saved-login reads; rotation/process restoration; profile isolation and actual HTTP/HTTPS/CSRF configuration. |
| Learning | Study/exam/recall setup filters/counts, shared/targeted practice, flags/bookmarks/maps and bilingual reading; withheld feedback/results. Measure timer recomposition, deadlines/local finish, master grace and overlapping exams. |
| Failure/recovery | Lost answer/start/pause/Finish/sync responses; failed reads/storage/review; duplicate/late writes; draft revisions, category exit flush and permission target changes. Retain drafts/selections/confirmed success/frozen identities; no replay or exit after failed pause. |
| Offline/accounts | Permissions/budgets/checkpoints, legacy/text-only copies/omitted images, titles/dates/rename, local completion/sync; logout/expiry/account/server isolation and no duplicate History/learning credit. |
| Accessibility/layout | Arabic/English/RTL, 48dp targets, 200% text, keyboard/TalkBack groups/headings/status, password reveal/reset/Next/Done and repeated validation focus. Themes/densities/contrast/gaps, API 23–25 font fallback/API 26+ weights, short-height/IME sheets/setup, rail/pane resizing/reader retention and drawer capability gates. Never announce timer ticks. |
| Reading/media | Reading preferences/reset, Help/Privacy, Markdown/code/tables/selection, explanations, haptics/reduced motion, invalid/bounded images and zoom/pan/sliders/reset/close, launcher on API 23/current device. |
| Server/content | Planner midnight/overlapping evidence, imports/partial outcomes/review, merges/editor conflicts/destructive labels, exports/admin permissions and large-bank/import performance. Matching schema/setup requires its own authorized scope. |

## Verification workflow

Follow [agent policy](../../Agents.md#efficient-verification) and the [build guide](../guides/build-and-setup.md#verification-workflow): use installed tools/persistent caches, preserve versions, check contention/RAM and ask before installing missing requirements. Documentation-only edits need link/consistency checks.

Replace superseded results with exact command/scenario, outcome and revision/environment when known. Keep remaining gates explicit; compilation does not establish device, accessibility, storage races or HTTP/concurrency behavior. Do not append bulk logs or recreate separate review histories.
