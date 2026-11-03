# Native UI/source guide

Paths below are relative to `app/src/main/java/com/mukhtabir/android/`. Follow [working rules](../../Agents.md); verification belongs in the [checklist](../verification/checklist.md).

## Source ownership

| Owner | Responsibility |
| --- | --- |
| `core`, `data`, `ui`, `util` | Storage/preferences/navigation; auth/HTTP/domain DTOs; Compose; formatting. |
| `MukhtabirApp.kt`, `ui/navigation/AuthenticatedAppShell.kt` | Startup/auth; shell/routes/capability gates. `AppDrawerContent.kt` owns the drawer; More is legacy. |
| `ui/demo/DemoScreen.kt`, `DemoRepository.kt`, `DemoPracticeScreen.kt` | Local learner shell/fixtures, shared sample state and practice. No API/account-storage dependency. |
| Question editor State/ViewModel/Screen/Preview | Serializable state, writes, form, previews. Other features similarly separate controllers/state/screens. |
| `QuizSetupController.kt`, `QuizSetupStateStore.kt` | Filters/count jobs; account-checked lightweight restoration. |
| `QuizViewModel.kt`, session chrome | Writes/deadlines/download/sync/recovery; separate lifecycle-collected clock flow. |

Trace route → controller → service/DTO → Django/Vue before contract changes. Runtime `/config/` supplies limits/pagination/roles; optional reads must not block startup. Reuse controls/controllers; avoid abstractions justified only by line counts.

## UI contracts

- Case precedes question. Preserve frozen/withheld results and exam/recall reveal rules in every reader/translation. Revealed choices use 1dp feedback borders plus wrapping text/icons; unrevealed selection stays subdued. No correctness animation. Save overlays avoid layout shifts; navigation owns scroll reset.
- Preserve Arabic/English/RTL, 48dp targets, headings/selection groups, keyboard focus, 200% text and reduced motion. Announce outcomes, never clock ticks. Localize display numbers/dates; keep protocol values unchanged and missing translations/dates explicit.
- Default/Compact spacing stays tight. Amiri defaults in both locales; retain saved fonts, real regular/variable weights, API 23–25 Amiri fallback and 13sp metadata floor. Reading size/spacing/width/reset also apply to Help/Privacy. Coordinate shared palettes/keys with web/PDF.
- Fields preserve cursor/composition, Next/Done and memory-only password visibility. Collapsed sections retain forms. Editors protect dirty targets/exits, reveal/focus invalid fields and preserve drafts through failure; Categories flushes encrypted drafts before exit. See [recovery policy](offline-and-recovery.md).
- Home prioritizes recovery/continuation; only fresh planner progress gets a ring. Heatmap includes session-only days; charts retain localized summaries and detailed values. Bookmarks filters apply to the loaded page; library labels explain combined/any-tag restrictions. Import cards retain status/reports/explicit uncertain-outcome review.
- Welcome/login use a scrollable keyboard-aware 520dp cap; demo → server → language, with downloads/retry/help/privacy reachable. Demo learning stays separate from private progress. Personal History is user-scoped; all-user History and authoring/admin actions require their capabilities.

## Adaptive layouts

- Font scale/width choose tabs versus rail (600dp base) and library/results panes (900dp base). Preserve selected detail through resize, account/server ownership and busy/recovery locks; reuse the full detail route.
- Setup validates typed/preset 10/20/40/Max counts against availability/runtime cap. Start summary pins only when height permits; short/large-text layouts scroll chrome/actions together.
- Results keep saved summary/GET refresh even when review is withheld. Wide review uses list + frozen reader; narrow review expands rows. Settings stays reachable above drawer destinations; short drawers scroll header/menu.

## Shared design APIs

- `AppButton`: Small/Medium/Large visual heights 32/36/48dp; Filled/Secondary/Outlined/Text; Default/Danger. All retain 48dp targets and wrapping. Primary/secondary aliases use Large, compact/outlined/text Medium. Destructive labels stay explicit.
- `AppScreenBar` shares title/insets/navigation/actions; tabs/rail share the icon pill. `AppSheetSize`: Half/Standard/Full = 50/85/100%, bounded by available height; default Standard, question map Full; constrained sheets scroll actions.
- `AppShapes`/`AppSurfaces` own semantic geometry/fills; keep component-only dimensions local. Titles 22/19/17sp, labels 15/14/13sp, headlines 34/30/26sp; preserve Arabic line-height floors. `SharedComponentPreviews.kt` covers modes/narrow Arabic/large text/Compact; source is not rendered evidence.

### Shared feature presentation

- `AuthoringFields.kt` shares choice text/correct/reorder/remove controls and exact-name tag entry. Editors retain stable choice IDs, validation/focus wrappers, suggestion limits and mutation guards. `AuthoringQuestionPreview.kt` renders authoring/reconciliation case context before the question; active/frozen learner readers retain their own reveal rules.
- `LearningSelectors.kt` shares nullable/required confidence and rating controls; `LocaleChoices.kt` shares app-language choices and exact authoring locale keys. Callers retain selection state, defaults, saves, recovery, app recreation and permissions.
- `AuthoringRecoveryPrompt` is the mandatory Restore/Discard startup decision; `EditorRecoveryControls` remains the inline encrypted-recovery UI. `ScreenBusyEffect` reports a caller-defined lock and clears it on disposal; Back handling and resource cleanup remain local.
- `AppSearchPicker` supplies bounded lazy lookup presentation with search/status/content/action slots. Callers own request drafts, freshness, staging, missing selected targets and action gates. Reuse `AppPagination`, `AppSearchRow`, `AppPageHeading.action` and `AppSwitchRow` rather than rebuilding their geometry/semantics.
- `ExportArtifactActions` presents Save and shares only an already-saved document URI with read grants; file pickers, artifacts and writes remain controller-owned. `AppDateField` opens date-only pickers with caller parsing/formatting and a click-time initial date; exam timezone/DST handling stays in its date/time field. `masterexams/MasterExamSummary.kt` shares list summaries with caller-owned notices/actions and containers.

### Optional UI priorities

- Pin results New session/Done and long-editor Save/Discard; simplify exam-row actions.
- Keep library search/filters reachable; one Practice entry point.
- Simplify Home metrics/recent cards; add chart axes/inspection and heatmap date/count details.
- Compare Comfortable spacing on narrow/large-text screens before adding it.

### Future consolidation shortlist

Only with real callers: labelled multi-series chart colors/markers, progress-size roles, motion-gated state durations and preference/system-gated tick/confirm/reject haptics. Preserve defaults; avoid unused modes, automatic sounds and duplicate announcements.

## Artwork and diagnostics

Master: [app_icon.png](../../artwork/app_icon.png); reference [monochrome vector](../../app/src/main/res/drawable/ic_launcher_monochrome.xml). Regenerate via `python3 artwork/generate_launcher_icons.py` (FFmpeg) or `python3 artwork/generate_startup_wordmark.py` (Fontconfig/Pango/Cairo). Manifest selects transparent density PNGs, no adaptive/round override. Preserve transparent exterior, white paper/lens and **مُختبِر** label; launcher normalization/caches still need device verification.

Device diagnostics when authorized: `adb devices -l`, `adb shell pidof com.mukhtabir.android`, `adb logcat --pid=PID -v threadtime`. Signing stays external; verify minified serialization/navigation before distribution.
