# Android remaining work

## 2026-10-17 draft/lifecycle verification

- After matching backend rollout and an authorized rebuild, verify draft revision decoding/save/delete, change after preflight, restored encrypted baselines, conflicts/unknown outcomes and explicit retained-draft review. Verify overdue draft/cancelled bank publication is unavailable and completed publication works; snapshot-backed attempts survive live question removal. [Shared C1–C7 source closure](../../backend/docs/archive/reviews/2026-10-17-remainingLogicReview.md#extended-findings-and-source-corrections). No compilation/device/HTTP evidence.

## 2026-10-17 completion/review verification

- After backend rollout and an authorized rebuild, verify ordinary exam/study completion during overlapping master work, authoritative saved summary versus withheld question review, later explicit owner-result GET reveal, failed reads preserving completion, account/attempt switches and existing lost-finish/storage recovery. Check Arabic/English/RTL, large text and TalkBack. [Shared B1–B4 source closure](../../backend/docs/archive/reviews/2026-10-17-backendMajorReview.md#source-corrections). No build/device/HTTP evidence yet.

## Dashboard tips and theme-picker verification

- After an authorized rebuild, verify Did you know is second after welcome and before mode selection, expanded/loading by default with manual collapse retained, successful/empty/error reads and tip rotation. Verify pending recovery cards remain visible below it.
- Verify drawer popup bounds/intrinsic measurement, three actual-palette swatches, selected check/semantics and one-word unique labels in English/Arabic. Check visible scrollbar track/thumb at top/middle/end only with overflow, pointer/keyboard scrolling and focus, 200% text, narrow widths, RTL and all themes. Source checks do not establish compilation/rendered behavior.

## Android placement/startup/haptics/theme verification

- After an authorized rebuild/install, verify fast/slow authentication, restore retries and zero/changed animation scales: full name reveal before automatic handoff, no stuck startup, explicit cached offline opening preserved. Check first setup has no prefilled development URL, saved manual edits still reopen, file import/select/replace stays above manual entry and imported addresses remain hidden.
- Check trailing bookmark control with/without translations, saved/loading/unknown/error reconciliation and retained full-width question text; compact bottom tabs/full inset taps, keyboard/system insets, RTL and 200% text. Verify enabled/disabled app and device haptic settings, accepted versus rejected/double selections, confirmation/background suppression, vibrator fallback and no-motor devices. Verify all 18 translated theme labels and shape roles across cards/buttons/dialogs/choice ripples; palettes/stored keys remain.

## Android 0.1.2 verification

- Rebuild/install when authorized; verify version 0.1.2/code 3, Tools/back pause with queued answers, successful sync-then-pause, duplicate taps, response loss/storage failure/conflicts, no exit without confirmed pause, pending Finish recovery, prefetch failure, locally finished study exit and unchanged exam deadlines. Check visible dialog errors/recovery and dismissal guards.
- Verify saved Numbers/Heatmap preference after process restart, 30/90/365 days, weekday alignment/padding, horizontal week scrolling, no visible counts/date list in heatmap, per-day TalkBack details, Arabic/English/RTL, themes/densities and 200% text. User's prior successful build does not validate these latest changes.

## Reported layout correction device verification

- The user reported a successful build after the ActivityRoute/AppMetadataRow scoped layout opt-ins. Check wrapping activity filters and metadata at enlarged Arabic text; later source changes require a new build.

## Extended UI adoption verification

- After an authorized rebuild, verify shared chips/dialogs in settings, analytics, administration, server setup, device sessions, planner, knowledge, transfers, categories, bookmark practice and link/editor recovery. Exercise existing password/danger/uncertain-write actions, capability gates, form/picker scrolling and dialog focus. Check analytics centered tiles/responsive label-value/count-progress rows, localized counts, metadata wrapping and confirmed-empty versus loading/error states across devices, themes/densities, RTL and 200% text/TalkBack. Source checks do not establish compilation or actual touch/scroll geometry.

## Selected UI consolidation verification

- After an authorized rebuild, verify shared answer gutters/long Arabic choices and inline feedback across ordinary/offline/master/reviews, reveal withholding and selectable completed-review text. Check compact filters/removal/disabled states; visible Home recovery cards; centered multi-column mode/metric tiles at narrow widths and 100–200% text; result totals/local grading; wrapping metadata/date/time; confirmed empty versus read-error states; secondary Retry, confirmation/danger actions, forms/pickers scrolling and focus. Include all themes/densities, RTL/TalkBack and system/keyboard insets. Source checks do not establish compilation or rendered geometry.

## Home completion reminder verification

- After an authorized rebuild, verify Home after study Finish on device and offline pack completion, process restart, explicit sync/server finish success and failure. Check refresh/resume, combined pending types/counts, unreadable storage with retained reminder and Retry, account/server switches and capability revocation, English/Arabic, RTL, 200% text and TalkBack. Source checks do not establish compilation/device behavior.

Updated 2026-10-10. [Documentation map](README.md) · [Current source status](workDone.md).


Pending verification: co-attending choices include moderators/admins and members granted `master_exams.create`, omit users without that capability, and preserve selections through save/read failure on the current server.


Pending verification: rebuild/install the enlarged transparent launcher icons and language-independent **مُختبِر** launcher label; check themed/non-themed launcher backgrounds, circle/squircle/other masks and APK thumbnails. Source assets do not establish installed package/cache freshness; system theming can add its own background/tint.

## Selected offline functionality verification

- Deploy/reload the additive offline catalogue/pack backend, then rebuild/install only when authorized. Verify permission grant/revocation and direct handler/server checks, combined filters and frozen resume/reset, old whole-bank requests, text-only omission/legacy rejection/byte limits, changed/unchanged/unavailable material and preservation of old packs. Exercise completed offline review, multiple independent pack uploads/local answers/navigation, lost responses and durable receipts, serialized storage/capacity failures and account/server cleanup. Verify ordinary Sync during new answers/revisions/navigation, conflict/latest queue merges, duplicate Sync, local study finish/restart/exit/finalize/lost finish response, and unchanged timed-exam deadlines/recall. Include RTL/large text/TalkBack; source checks do not establish compilation/concurrency/device or HTTP behavior.

## 2026-10-13 remaining presentation verification

- Verify the selected session clarity and richer themed UI after an authorized rebuild: compact primary/secondary frames and full tap areas; exam neutral selection and withheld feedback; matching question/answer/feedback identity; synced/local/failure/storage states with retained content and reconciliation; failed-image compact Retry/height transitions; 200% Arabic/English, keyboard/system insets, TalkBack, long choices and dominant Next/Finish. Verify all renamed/tinted themes and native/web/PDF visual parity; source colour checks are not rendered verification.

## Implementation queue

The earlier parity/rescan/deeper fixes and all A1–A7 findings, smaller workflow differences and shared web defects from the [latest parity review](archive/reviews/2026-10-10-androidParityReview.md) are source-complete. This does not establish exhaustive correctness.

All U1–U8 from the [UI/shell review](reference/androidUiReview.md#source-closure--2026-10-10) are source-complete. No implementation item remains from these bounded lists; rebuild and device verification remain pending. Newly evidenced defects should include reproduction, priority and affected contracts.

The native UI standardization pass is source-complete; use [shared conventions](guides/uiConventions.md). Responsive headers, command/control rows, feedback and pagers still require the device gates below.

Priority: P0 ordinary-use failures; P1 learner workflows; P2 contributor/moderator; P3 administration; P4 operator/optional scope.

## Pending release gates

- Verify the selected interaction refinements after an authorized rebuild: immediate busy/recovery guarding of repeated answer/navigation taps, absent blank explanation headings/cards, bounded choice ripples, setup keyboard dismissal, mixed Arabic/English paragraph and ordinal direction, explanation fade without replay on status updates, and retained native Markdown selection. Exercise local pool/filter/count failures and Retry (including collapsed filters and canceled old count reads); ensure setup fields remain and uncertain writes are never replayed. Source checks do not establish compilation/device behavior.

- Rebuild after the reported NativeTextStyle fontMetricsInt failure: the natural-height bound now reads TextView.paint metrics. Verify the larger 104sp width-fitted startup title and regenerated native mask artwork on narrow screens, 200% text, Android pre-31/31+, themes, reduced motion and TalkBack. Source/XML checks passed; compilation and device appearance remain pending.

- Rebuild/install when authorized and verify the selected presentation polish: first-line number/passive-radio alignment and stable gutters; uniform medium rounding/insets; quieter borders/feedback fills with retained labels/selected semantics; shorter gaps and explanation blocks; 36dp secondary frames inside 48dp clickable surfaces including inset taps, keyboard focus and disabled handlers; 20dp icon/loading slots; medium-weight/readable disabled labels and muted metadata; Arabic natural-font line-height bounds; reserved hours-capable clock geometry at digit/hour/critical/overtime transitions. Check all named themes, density modes, bundled/system fonts, RTL/English, 200% text and TalkBack. Twelve source/resource scans passed; compilation, rendered contrast and device interactions remain unverified.

- Rebuild/install when authorized and verify the full-width question correction: bookmark is in session tools and no longer consumes question-card width; menu saved/loading/unknown state and recovery still work. Correct/incorrect feedback appears only inside choices, with no duplicate status before the explanation heading and retained TalkBack announcements. Source checks passed; compilation/device/RTL/large-text behavior remain unverified.

- Rebuild/install when authorized and verify the smaller explicit sync path: one initial full snapshot and individual question confirmation for normal writes; fresh acknowledgement/full-read fallback at other/completed indices; ordered confidence/reflection revisions; concurrent navigation/slot conflicts; lost responses; partial prefix/storage failure; expired/missing/other-account sessions and unchanged server deadlines. Confirm actual request counts/bytes and retained encrypted queue rather than inferring runtime performance from source. Backend transport cleanup is separately source-complete; network route failures remain unresolved.

- Rebuild/install when authorized and verify optional haptics, result All/Incorrect/Unanswered filters, compact session tools, real Markdown-heading jumps and conditional image/bookmark loading areas. Check default-off/persisted haptics, device suppression, accepted versus duplicate/rejected selection, server confirmation versus local/failed/partial sync and background/restoration; original result numbering/totals and empty filters; pause/sync/menu guards; Arabic/English/RTL/200% text/TalkBack and native Markdown selection/heading scrolling. Images without URLs must reserve no space; existing images must retain stable loading/error/Retry/display geometry and fit without cropping. Source checks passed; compilation/device behavior remain unverified.

- Rebuild/install when authorized and verify the selected ordinary-session presentation changes: compact fixed header/bottom navigation and dialog picker above system insets; inline answer feedback, titled explanation fade with reduced motion, quiet Sync/details, independent bookmark saved-state/response-loss recovery, removable active setup filters, resume progress/pending status and summary-first collapsed results. Check 320/360dp, 200% text, Arabic/English/RTL, themes/density, native Markdown/TalkBack, encrypted restoration and unchanged exam/recall reveal/synchronization gates. Production source/resource checks passed; compilation and device/live HTTP remain unverified.

- Deploy the updated session-prefetch backend, then rebuild/install when authorized: study downloads must include frozen keys/original and translated explanations, with immediate local feedback and wrong/correct choice highlights after answering; exam downloads must withhold feedback. Verify no per-question sync wait, confidence/reflection and auto-advance, explicit sync before pause/finish/deadline, response loss/conflicts, encrypted process restoration and older downloads without keys. Check themes/RTL/large text/TalkBack. Source checks passed; compilation/device/live HTTP remain unverified.

- Re-run the user build after the four reported compile errors in AppWritingText and QuizViewModel: clipped drawing now uses an explicit ContentDrawScope receiver; pen height uses existing line top/bottom APIs; bookmark/SRS SetupSelection calls pass the nullable label explicitly. Production source/XML/whitespace checks passed; successful compilation remains unverified.

- Verify the dashboard welcome/username moving-dot reveal after rebuild: duration is three times the splash resource (currently 2700ms), Arabic joining/diacritics and mixed names stay shaped, long names wrap without page movement, and zero system animation scale shows the full heading immediately. Check themes, RTL/English, large text, navigation return and TalkBack; compilation/device verification remain pending.

- Verify the dashboard after rebuild: Today is the first section and starts expanded; all other sections start collapsed and every section spans the full page width; only Today reads load initially when permitted, and other datasets load on expansion; per-section first load, cached reopening, Retry, open-only refresh/resume and stale closed-section reads on next open. Check failed reads retain cached content, Study now remains usable during statistics failure, recovery cards remain reachable, and collapsed recent questions do not compose image reads. Compilation, server/device, themes, RTL, large text and TalkBack remain unverified.

- Re-run assembleDebug after the reported QuizScreen:697/700 Material API failure: SetupChoiceChip now has a local ExperimentalMaterial3Api opt-in. Source checks passed; successful compilation remains unverified.

- Verify the unified Android setup after rebuild: All questions by default, optional book/no-book restriction, shared collapsed category/tag panel for bank/bookmarks/SRS/blueprints, collapsed other options with active summaries, and count/Start with specific blocked reasons. Verify same-pool/same-book taps preserve selections, book changes prune only incompatible chips, manual legacy tags stay visible/countable, old category/tag source states restore into the bank, and removing a book preserves filters. Book-facet loading/failure must allow unfiltered whole-book sessions but block unvalidated selected chips; Retry/clear and missing-book recovery must remain available. Availability/config changes must leave typed counts untouched and invalid counts must block the handler. Source checks passed; compilation, HTTP, themes/RTL/large-text/TalkBack/device and interruption verification remain pending.

- Verify backend-first full available counts and book-specific category/tag refinements on matching clients: >200 matching questions with the unchanged session cap, combined filters, changed/cleared books, read failure/Retry, rapid switches and saved setup. Source checks passed; compilation, template/browser/device and live API verification remain pending.

- Re-run the user build after the QuizScreen:224 correction: conflict controls now use the existing experimental-layout-opted-in SelectionRow wrapper. The supplied build failed at compilation; source/whitespace checks do not establish a successful rebuild.

- Deploy the matching ordinary session-prefetch backend, then rebuild/install when authorized and verify [downloaded sessions](contracts/downloadedSessions.md): all-page/image preparation, multiple local answers/revisions, lost responses and explicit sync, partial confirmation, device/server conflict choices, encrypted process restoration, storage/capacity failures, pause/finish and exam deadline rejection. Verify 8dp gutters, responsive dashboard grid, Arabic remaining/additional/master grace clocks, large text, themes and accessibility. Source changes remain uncompiled and runtime behavior unverified.

- MiXplorer: copy the current APK to the phone under a distinct filename and refresh/reopen its folder; compare file thumbnail with installer/launcher icon. Local packaged name/icon/reference checks passed, but the screenshot's phone APK identity and explorer cache/fallback behavior remain unverified.

- Verify app-wide spacing adoption beyond offline study: auth/password/profile/tips, shared heading/loading/result components and the 43 normalized card-body sites across management, authoring, master/ordinary quiz, analytics and learning screens. Check normal/compact density, keyboard, RTL, long labels and large text; latest normalization remains uncompiled.

- Verify the screenshot-driven tighter spacing on the offline list and other shared screens: 12dp/6dp page insets, smaller normal/compact card and section gaps, asymmetric dense material-card insets and Compose font padding disabled. Check all bundled Arabic/English fonts, diacritics, multiline headings, 200% text, themes and 48dp controls. Source checks do not establish compilation or new device appearance.

- Verify cold/warm launch on Android 23–30, 31–32 and 33+, light/night and saved themes: centered Arabic native wordmark, mask/diacritics, 900ms Amiri reveal/moving dot, handoff to restoration/login/setup, zero animation scale, 200% text, TalkBack and offline action. No artificial delay was added; actual reported 2–3 second startup timing still requires device measurement. These changes remain uncompiled.

- Verify the Arabic application/installer label on an English phone after rebuilding/installing, explanation cards across ordinary/offline/master/result/detail/author previews, and tighter shared card/button insets at both densities. Check all themes, long Arabic/English Markdown, reveal timing, 200% text and TalkBack; these latest source changes remain uncompiled.

- Verify the shared three-dot loader on startup, screen reads and busy auth buttons across themes/RTL and TalkBack; check smooth looping, background/resume and system animation scale changes including zero. Source checks do not establish compilation or device appearance.

- Verify the shared shaded question/choice cards across every named/system theme, including Weather's gradient, Dark/Onyx, selected/disabled/results states, offline and authoring previews. Check long Arabic/English Markdown, RTL, 200% text, whole-choice selection and TalkBack. Source color/lexical checks do not establish rendered appearance or compilation.

- Verify downloaded study after overnight login expiry: startup with an expired cookie, online `/me` 401, offline opening/answer persistence and later same-account login/sync. Check explicit logout, confirmed account expiry, different account UUID, server switching and cleanup failure still revoke access across process restart. The user corrected the reported account to admin: its configured 8-hour online lifetime explains an overnight prompt; moderator/member lifetimes remain 7/14 days. No build/device result is inferred from source checks.

- Verify reduced shared vertical spacing, the small theme picker at the drawer header’s inline end, standalone About/Privacy links, fresh Arabic startup and collapsed settings with clarified question-page count: narrow layouts, 200% text, RTL/TalkBack/focus, themes/density and saved language choices. Source checks do not establish build/device correctness.

- Verify Android multi-file imports with native document providers: DSM-5 CSV/JSON chapter selections, mixed invalid/oversized files, per-file results, removal, duplicate selection, response loss/review, cancellation/account/server switches, the normal hourly throttle/password-authorized ten-minute bypass and Arabic/RTL/large text. Kotlin source/XML checks do not establish compilation or device/server behavior.

1. Install/device verification after the user's successful 2026-10-11 `assembleDebug` (6m 56s). The subsequent removal of two unreachable choice-number expressions in QuizScreen still awaits compilation. The generic Gradle deprecation notice lacks individual diagnostics; its origin remains unidentified.
2. Matching backend/schema and coordinated Vue/native contracts; see [schema readiness](../../backend/docs/contracts/schemaReadiness.md). No schema setup has been performed for the latest changes.
3. Execute the [functional manual matrix](verification/manualVerification.md), prioritizing server/account isolation, offline storage/sync, ordinary/master recovery, planner chronology and concurrent edits.
4. Validate English/Arabic, RTL, large text, focus/TalkBack, permissions, file providers and reminder delivery on a device.

Source checks do not establish compilation, HTTP or database concurrency. Builds, suites, migration work/setup and version changes require explicit request under [agent rules](../Agents.md). Optional new scope is in [later work](reference/laterWork.md).

## Posted audit follow-up

The source corrections from the supplied Android audits are complete; see the [review and API/design limits](reference/2026-10-10-postedAuditReview.md). Rebuild/device/accessibility/navigation/language verification is pending in the [manual matrix](verification/manualVerification.md#posted-audit-corrections-and-reading-language). Optional separate-case translations are now source-complete through the existing question-language control; see the [contract](../../backend/docs/contracts/caseTranslations.md). Matching case-translation schema/backend, rebuild and device verification remain pending. Completed-result language controls remain separate scope; Android does not infer a base language or replace frozen results with live data.

- Verify category merge and direct tag merge controls after matching backend/client rollout: all source links, planner targets/progress, overlapping blueprint weights, editor revisions, receipt/review recovery, concurrent changes, Arabic/English/RTL and large text. Source checks passed; HTTP, browser/device, concurrency and compilation remain unverified.
