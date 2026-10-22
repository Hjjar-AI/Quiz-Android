# Current verification and remaining gates

## Evidence

| Scope | Known result | Boundary |
| --- | --- | --- |
| Documentation | Current Markdown file links/heading anchors and whitespace checks passed. | Documentation maintenance only; no build/test result. |
| Current UI production source | Kotlin lexical/delimiter/import, shared field/progress argument, resource-reference and whitespace checks passed. | Not Kotlin compilation or runtime validation. |
| Bilingual resources | 1,817 translatable English/Arabic keys and placeholders match. | Does not establish translation quality, RTL or clipping. |
| Native palettes | Source calculations for 17 concrete themes reached 4.5:1 for adjusted feedback/error ink on checked surfaces and 3:1 for unselected question/choice borders. | Not rendered alpha/antialiasing, disabled/selected-control, image or display verification. |
| Fonts/APIs | Bundled SFNT/OS2/fvar and installed Compose/icon API metadata inspected. | Not device font rendering or API-level execution. |
| Earlier supplied debug build | User-reported `assembleDebug` succeeded for 0.1.6.20261022 (code 7). | Predates current UI corrections; does not verify current source. |
| Current build/device/API/suites | Not run for the UI corrections. | Previous restrictions have been replaced by the efficient verification policy below; no result is implied by authorization. |

Use the [verification workflow](../guides/build-and-setup.md#verification-workflow) and [agent policy](../../Agents.md#efficient-verification). Builds and relevant focused tests are allowed when useful; docs-only edits need link/consistency checks. Reuse installed tools/persistent caches and preserve versions. Do not install missing tooling or fill network caches silently.

## Remaining checks

| Area | Scenarios to verify |
| --- | --- |
| Build/toolchain | Select installed JDK 17/SDK 34 and cached pinned Gradle; run one appropriate cached-only check/debug build with version preservation. Resolve actual diagnostics. Earlier Gradle deprecations remain unspecified. |
| Startup/access | Demo cold launch in airplane mode; welcome/splash/reduced motion/rotation; login/server Back/keyboard; connection-file slots; downloads during/after failed saved-login checks; demo exit; profile isolation, HTTPS/selected-origin HTTP and actual server/CSRF settings. |
| Learning and timers | Study/exam/recall, setup book/category/tag filters/count limits, shared launchers/targeted practice, flags/bookmarks/maps and bilingual reading. Withheld feedback/results must stay hidden. Measure timer isolation with recomposition counters; check deadline, restart/rotation/process restoration, master grace and overlapping exams. |
| Failure/recovery | Lost answer/start/pause/Finish/sync responses, failed storage/read/review, stale late responses and duplicate clicks. Keep drafts, selections, confirmed success and frozen completion identities; no replay. Failed pause must not exit. Include draft revisions, category exit flush and permission target-switch/discard. |
| Offline/accounts | Permission/budget/checkpoint enforcement, legacy/text-only copies, omitted images, titles/dates/rename, local completion and explicit sync. Logout/expiry/account/server changes must preserve isolation and avoid duplicate learning/History credit. |
| Forms/accessibility | Password reveal/reset and Next/Done, repeated validation focus/scroll, keyboard/TalkBack selection positions/headings/status announcements, 48dp targets, Arabic/English/RTL and 200% text. Timers must not announce every tick. |
| Layout/theme/fonts | All themes/densities, native contrast/clickable edges/action gaps, API 23–25 fallback and API 26+ variable weights, short-height/IME sheets and setup actions, rail/pane thresholds/resizing/reader retention, drawer ordering/current selection and capability gates. |
| Reading/media | Reading controls/reset/persistence, Help/Privacy and Markdown/code/tables/selection, reveal-only explanation, haptics/motion, bounded/invalid images, zoom/pan/keyboard sliders/reset/close; launcher rendering/cache on API 23 and a current device. |
| Server/content/reporting | Planner day/midnight attribution and overlapping review counts; imports/partial outcomes/review; taxonomy merges and editor conflicts; destructive labels, exports/admin permissions. Verify matching backend/schema under separately authorized setup. Measure large-bank/import performance. |

## Recording a result

Replace the relevant row with the exact command or manual scenario, exit/result and verified revision/environment when known. Keep the remaining gates explicit. A local compile does not prove HTTP, storage races, accessibility or device behavior. Retain only evidence still useful to assess the current source; remove superseded attempts and bulk logs.
