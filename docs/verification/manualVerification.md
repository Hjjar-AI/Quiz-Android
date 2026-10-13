# Pending functional verification

## Extended UI consolidation adoption

- [ ] Shared chips/dialogs across settings/analytics/admin/server/device/planner/knowledge/transfer/category/bookmark/editor/link flows: selection, localized labels, compact tap regions, scrolling form/picker content, focus, password handling and existing guarded destructive/recovery actions.
- [ ] Analytics shared metric grid, responsive label/value rows and localized count-progress bars; metadata wrapping in admin/device/group/knowledge/planner screens; confirmed empty read versus refresh/failure with retained content and Retry.
- [ ] Narrow/wide screens, both densities/all themes, English/Arabic/RTL, 200% text, keyboard/system insets and TalkBack after an authorized rebuild. No runtime outcomes are inferred from source checks.

## Shared UI consolidation

- [ ] Verify ordinary/offline/master/review choices: first-line number gutters, long Arabic/English wrapping, revealed feedback/neutral exams, single inline labels, whole-choice taps and selectable completed-review Markdown.
- [ ] Verify centered adaptive Home mode/metric grids and result tiles at narrow/wide widths, densities and 100–200% text; no clipped/shrunk labels, and pending completion/finish/master recovery stays visible.
- [ ] Verify shared chip selected/removable/disabled states and tap areas, metadata wrapping/bidi/date/time, confirmed empty reads versus errors, compact local Retry and retained content, confirmation/danger actions, scrolling form/picker dialogs, focus/TalkBack and all themes.

## Home pending completion reminder

- [ ] Locally finish downloaded study; Home shows the reminder and reopens the retained session for explicit Sync & finish.
- [ ] Finish multiple offline packs; Home counts only finished/unconfirmed packs and opens offline practice for review/sync. Confirmed uploads disappear on resume/refresh; unfinished packs and exams are excluded.
- [ ] Check process restart, read failure/Retry with retained counts, rapid refresh/account/server changes, permissions, Arabic/English/RTL, 200% text and TalkBack. No automatic upload occurs.

Updated 2026-10-10. Shared Android/web/backend matrix; **all scenarios below remain pending** unless a later dated result records otherwise. [Source status](../workDone.md) · [Schema readiness](../../../backend/docs/contracts/schemaReadiness.md). Earlier detailed checklists: [archive](../archive/README.md).

## Selected offline functionality

- [ ] Full-bank capability/start checks, permission revocation during focused downloads, exact book and combined category/tag filters (including punctuation), paused frozen scope/image setting, reset without pack loss, old unfiltered calls and changed catalogue counts.
- [ ] With/without images, omitted-image labels, legacy backend rejection, pack/storage size limits, unchanged/changed/deleted/inaccessible update checks and retained current answers/completion.
- [ ] Local review before sync; independent pack uploads while another pack is opened/answered/navigated; receipt response loss, duplicate Sync, protected uploading pack, multiple out-of-order responses, storage failure/retry, account/server/logout cleanup and no progress/result overwrite.
- [ ] Ordinary session Sync while answers/confidence/reflection/navigation change: fixed initial batch, latest merge, new pending changes, partial prefix/conflicts and no question jump. Local study finish, restored timer/read-only review/exit, later Sync/server finish and lost-finish recovery; exams/recall remain gated. Verify Arabic/English/large text/TalkBack and actual server time/result semantics.

## Selected session clarity and tinted themes

- [ ] Compact Next/Finish/Retry at 200% Arabic/English text, full inset taps, consumed system/IME insets and last-choice visibility; exam neutral selection/reveal withholding including TalkBack; matching question/answer/feedback updates; local/synced/syncing/failure/conflict/storage recovery; independent image/bookmark loading; long choices and retained content. Exercise compact image-error scrolling/Retry and intentional height changes.
- [ ] Compare all tinted/renamed native themes and shared web/PDF palettes: body/card/alternate surfaces, readable feedback and disabled text, forms/focus/charts, contrast, print and persisted keys. Confirm Sky/Dark/Lime/Midnight palettes remain as before.

## Selected interaction refinements

- [ ] Rapid answer/navigation taps while busy/recovering; no empty explanation heading/card; rounded Material choice feedback and disabled taps; setup-to-session keyboard dismissal; mixed Arabic/English paragraphs and numeric punctuation; no fade replay or native selection reset on status-only updates; local pool/filter/count Retry with retained fields/selections and no replay of uncertain writes. Include collapsed filters, canceled old count reads, RTL/English, 200% text and TalkBack.

## Environment and identity

- [ ] Install the Gemini launcher replacement: confirm مُختبِر with kasra beneath ب, no painted checkerboard, whole paper/handle visible under circle/squircle masks, and matching themed silhouette. Check after clearing stale launcher caches if necessary.
- [ ] Build/install current authorized revision; confirm exact backend/client versions and schema. Check proxy/ports/media/CSRF/cookies/login-first-write, private CA/hostname/redirects and chosen HTTP origin. Interactive startup is not schema setup.
- [ ] Logout/login, expiry/password change, forced password change, server/account switch and reused numeric user IDs: revoke old callbacks/cookies/reminders/private access; account UUID/server profiles isolate encrypted data and operation identities.
- [ ] Own-session listing/password-confirmed revocation and self-sign-out; signed-cookie engines cannot individually revoke sessions. Reminder permission/timezone/delivery/denial on device.

## Server files and retained profiles

- [ ] Manual or admin-exported unencrypted JSON first launch/Settings import; imported address never appears in input, confirmation, settings/export prefill or TalkBack. Manual replacement starts blank. No decorative identity authenticates or merges servers.
- [ ] First-server language controls apply English/Arabic/system direction; fresh preferences use Iris and existing theme choices persist. Whole-choice taps work over the number/text/remaining area in ordinary/master/preview answers. Confidence defaults to Certain and wrong-answer reason to Unknown, collapsed with retained values; changing details, next/previous/finish/pause and auto-advance produce one learning event. Compact navigation/library spacing remains readable at 200% text with system/keyboard insets.
- [ ] Three numbered slots persist across restart, including identical URLs; legacy slot 1 survives initialization. Fourth import explicitly replaces a slot; canceled setup does not activate it. Invalid/oversized (>64 KiB)/unreadable files, picker recreation and failed storage commits retain old configuration. Original documents stay untouched.
- [ ] Explicit same-server alternate address preserves downloads/drafts/pending completions behind same-account reauthentication; different-server profile stays isolated. Known distinct profiles cannot silently merge. Failed login/storage save and late old-origin errors cannot delete retained files. No automatic write/upload replay.
- [ ] Admin.settings-only native/web export, phone/deployment-prefix addresses, canceled/read-only/provider failures and HTTP warning/private TLS; export leaves settings drafts intact.

## Offline bank and storage

- [ ] Member, moderator-default, per-user Allow/Deny/Inherit and admin policies; both tests.start and tests.download_full_bank gates. Existing saved role matrices are not overwritten by source defaults. Refresh/revoke during requests stops new bulk actions and keeps cached packs.
- [ ] Keyset catalogue includes caller public/own drafts, excludes private/closed-book content, and packs independently recheck. Mid-pass additions beyond initial upper bound await another pass; changed permissions/content and count differences are visible.
- [ ] More than five packs/8 MiB, images/translations/cases, empty/all-protected/partial banks; split oversized batches, report single exclusions, handle 401/403/5xx/timeouts/capacity without claiming complete download.
- [ ] Pause/restart/process death during fetch/file/index commit; explicit resume only. Legacy preferences migrate after successful commit, preserving answers/UUID/body/results. Corrupt individual files, Keystore/full disk/failed commit retain other readable packs and support Storage Retry. No plaintext/cloud-backup payloads.
- [ ] Selected downloads exceed 200 distinct IDs without capability; re-download granted IDs, clear cache, omit full_bank and revoke a prior grant. Server budget persists; local files remain. Deliberate pack deletion and fresh-bank passes report honest counts.
- [ ] Immutable completion sync: lose response, identical/altered/cross-user retry, clock bounds and legacy undated bodies; exactly one grading/learning effect. Reverse offline/online chronology and midnight/gap-filled dates preserve SRS/streak/activity.

## Ordinary and master learning

- [ ] Android bottom navigation omits More while all drawer destinations remain reachable; dashboard refresh stays beside the welcome title at the logical end on narrow screens, RTL and large text. Android/web recall: enter text, reveal the same question, select/confirm a choice, then advance; test keyboard answers, auto-advance, pause/resume and previously answered questions.
- [ ] Study/exam/recall filters/choices/strategies, blueprint quotas/case grouping, manual IDs/SRS ordering; repeated same category/tag/group prioritizes eligible alternatives per user, without impossible no-repeat guarantees for exhausted pools. Concurrent starts and large histories.
- [ ] Reveal/translation/choice-index/case/image rules; answer/navigation/timer/pause/resume/Finish, response loss/process death/cold-launch exact result identity and legacy History. Failed pause does not silently leave; no ordinary uncertain write replay.
- [ ] Master fresh audience/co-attending/group access during new start/preview; normal resume after closes_at, repeated makeup start and expiry: no second attempt or timer extension. Existing owned work retained.
- [ ] Master two-device answer/confidence slot conflicts, lost commit responses and legacy native drafts: full-slot read then explicit keep/rebase/discard. Race goto/current-question/answer/finish/expiry: stale position rejects; GET does not persist fallback; grading errors do not report false completion.
- [ ] Same-account route A→B, account switch, hide/show/unmount during polling/backoff: old status/question/write responses do not replace B, navigate an unmounted view or revive timers.
- [ ] Master management versions/composition/private/shared drafts/preview/publish/cancel/bank, results-only gates/matrix/CSV; deletion archives weighted frozen results without replaying learning.

## Planner, edits and recovery

- [ ] Planner id/version conflicts on edit/delete/recreate, accepted update response plus failed read; retained draft/baseline and explicit Keep draft/Use server. Native exact comma-bearing tags, dates/target, encrypted restore/discard/leave and checkpoint ordering.
- [ ] Late offline answers before/after scope reset credit original scope; today's reset stays reset. Tag rename/merge continues counts without copying; after midnight historical totals include pre-reset activity exactly once. Global learning events remain intact.
- [ ] Question/knowledge/group/category/case/tag/settings revisions; ownership changed between lookup and locked write, delete confirmation races, protected relationships, explicit clear versus omitted fields. Imports with equal/lower source versions invalidate stale local editors.
- [ ] Unknown question/category/knowledge creates or copies: caller/body-bound receipts, explicit same-identity retry, missing/denied/read-failed/deleted-target distinctions; acknowledge terminal UUID without clearing draft. Browser orphan creates cannot consume new text/images or navigate removed forms.
- [ ] Encrypted editor/report/settings/bookmark recovery across restart; no dispatch on failed required checkpoint or automatic restore replay. Memory draft/confirmed success survives disk/read/clear failure. Category icons retain unfamiliar values and accessible selection.
- [ ] Fresh metadata/profile/capability/rank/expiry on re-entry/foreground/explicit refresh and confirmed writes; removed selections reconcile, valid drafts/active sessions survive failed or late reads. Revoked permissions gate both controls and handlers. Safe nested validation/auth reasons render; internal/oversized diagnostics do not.

## Browsing, administration and exports

- [ ] Management beyond 100 cases/500 knowledge objects and learner cases beyond 50; stable totals/order, configured sizes, search resets only on accepted results, exact failed-page retry, final-page deletion and retained editors. Legacy autocomplete shape stays bounded.
- [ ] Analytics independent retries/7–30–90 periods/account guards, distinct/due/mastery metrics, library ratings/ranks, rating response loss/read failure; all five admin reports bilingual/paged, independent admin.active_users versus analytics.view_all, author top-20 and default report windows honestly labeled.
- [ ] Permission-only lookup versus admin.users, inactive/non-stub paging, roles/overrides/expiry/reset-secret/delete rules; own versus any content gates, moderation selections/ratings/notes/tags/reports and protected taxonomy/planners/groups/blueprints.
- [ ] Ordered manual PDF/Excel/CSV/JSON exports, visibility/verified-only, Arabic layouts/images/answer placement/fresh forward/back PDF links and downloads/share. Import validation/mapping/rollback; backup/restore/clear/seed only deliberately in isolated safe operator scenarios.
- [ ] Actual server-database interleavings: parent edits versus learning, tag merge/import/planner/finish, rollback/deadlocks and broad-import contention. SQLite does not establish production row-lock behavior; PostgreSQL integration remains separate.

## Presentation and recording results

- [ ] English/Arabic/RTL, all themes/fonts/density/reduced motion, narrow/large text, keyboard/TalkBack/focus/live errors, long content/charts/collapsed sections and file-provider controls. Web [visual checklist](../../../frontend/docs/verification/visual-qa-checklist.md).

For each executed scenario record date, source revision, backend/database/client environment, expected/actual behavior and pass/fail evidence in the relevant work log. Add concrete failures to its work plan. Do not mark a group passed from a source check or one happy-path run.

## Latest parity-fix scenarios (pending)

- [ ] Study a concept and return/foreground/Refresh the map; updated metrics/filter, category groups and nullable confidence survive failures, empty maps and large text/RTL.
- [ ] Sparse/zero/midnight planner activity with device/server date differences: exactly server-today minus six through today; old activity outside the window stays out in both clients.
- [ ] Interactive preview answer/confidence/feedback/navigation, static reveal, rotation, refresh/leave confirmation and isolation: no real attempt, answer/finish request or server grading.
- [ ] Live results: active 30-second updates, completed stop, hide/show/route departure cancels reads, failed refresh retains content and matrix timestamp, denied permission clears private results. Bank publication response loss requires explicit read/review; confirmed success survives later read failure.
- [ ] Admin cards include optional latest scores/tags, zero versus absent values, contribution counts/login dates. Direct overrides keep admin.permissions separate from admin.users; failed target reads retry without replacing drafts.
- [ ] Mixed active/inactive selected accounts in both clients: Activate/Deactivate sets desired state and never reverses a matching account. Reject self/stub/last admin, wrong password, missing target and revoked permission; preserve confirmed, rejected, uncertain and unattempted IDs. Stop at unknown/401/403/429, read actual states (including off-page/deleted accounts), acknowledge explicitly, then only deliberate new writes.
- [ ] Account switch during web dialog/sequence/clock read or native batch: old callbacks/passwords/selections cannot act as the new account. Batch route navigation is blocked while sending. No unknown replay after reopen/restart.
- [ ] Recent categories/tags and last-question/clear survive ordinary restart but follow encrypted account UUID/server-profile isolation, logout/storage-failure and approved same-server alternate-address rules. Shortcut opens readable detail, not an unauthorized editor.
- [ ] Top/bottom reorder uses one versioned request and preserves uncertain composition/draft on response loss. Statistics export preselects verified-only while retaining transfer drafts; results bank shortcut enforces both manage ownership and publish capability.
- [ ] Web initial server-offset read under skewed clocks, delayed response, account/attempt changes and read failure: no undefined signal, old offset or conversion of confirmed Start into failure.


## UI/shell fixes (pending)

- [ ] Query and non-query permissions/transfers/offline entries, knowledge details, account tools, author preview, individual results, private drafts and master attempt show meaningful toolbar titles and the correct drawer parent. More tab opens the catalog; question detail preserves Library/Bookmarks/other originating tab on Back and rotation.
- [ ] Focused editors/change password/results/master list/attempt hide drawer and bottom tabs; Back/discard/recovery and busy/unknown-write guards still work. Normal settings/planner/maps retain shell navigation; profile refresh does not replace protected drafts or sessions.
- [ ] Compact spacing changes page/section gaps throughout learner and management screens, preserving 48dp targets, RTL ordering and readable nested cards.
- [ ] Invalid question text/choice/correct selection/reference/association/translation and planner target/dates/categories show actionable field errors. Collapse the failing section and submit again: reopen it; retain the draft. Correct input clears validation without saving automatically.
- [ ] TalkBack announces new read failures and saved/notice outcomes once without interrupting question reading or announcing ticking timers. Collapsed children stay out of focus traversal.
- [ ] At 320/360dp and 200% font size in English/Arabic, choice/difficulty/recovery actions wrap and heading actions stack; all labels and enabled controls remain reachable.
- [ ] With keyboard open on the final field in question/account/planner/permission forms, scroll to Save/errors. Test gesture and three-button navigation, change password and startup/login: no keyboard overlap or duplicate blank inset, including rotation.


## Shared UI standardization (pending)

- [ ] Shared page titles/actions, text/icon/danger controls and neutral cards remain readable across themes/RTL and 200% text. Compact page/section spacing preserves minimum targets; heading command slots wrap/stack without starving the title.
- [ ] Library/bookmark/history/mistake/fragile/admin-user/managed-content/content/case/permission pagers preserve boundary and busy guards, failed-page retry and drafts; page counts follow app locale. Changes only navigate after explicit user action.
- [ ] Loading indicators have useful progress labels; saved confirmations and safe errors are announced without duplicate/interrupted announcements or ticking-timer noise. Auth busy buttons retain their small spinner behavior.
- [ ] Destructive styles preserve existing confirmations, encrypted recovery, unknown-write review and actual ownership; styling never sends a command. Verify all shared action rows including long labels and disabled controls.


## Launcher icon coverage (pending)

- [ ] Rebuild/install the current app and check home/app drawer, app info and task switcher at different densities: Android 23–25 bitmap fallback, 26+ adaptive masks, round launchers and 33+ themed icons all retain the supplied mark. Refresh a stale launcher cache without clearing app/offline data. Source asset checks do not establish installed icon behavior.


## Source-book practice

- [ ] After importing DSM/Kaplan, both clients show Source book / الكتاب المصدر with public-bank titles/counts; select each title and start exam/study/recall with only that book’s questions. Confirm punctuation/commas in titles are preserved and empty/private-draft-only titles are excluded.
- [ ] Difficulty, verification and additional tags refine within the selected book; zero matches disables start. Rapid selection changes cannot apply stale counts. Failed book reads retain selection/list and Retry works without resetting other setup or active sessions. Android recreation preserves the same-account setup and account/server changes do not expose it. Check Arabic/RTL, long titles, large text and accessibility.

## Posted audit corrections and reading language

- [ ] Rebuild/install current source; in every theme check selected navigation pill, tinted surfaces/ranks, Weather gradient and 3-button/gesture bars. Source contrast ratios do not verify disabled/alpha/image/span states.
- [ ] Normal/compact, English/Arabic/RTL and 200% text: section header/body edges, two Home tiles, list/toggle rows, auth fields/spinners, picker caps, full-page/nested errors, and minimum touch targets. Verify themed heading and option wrapping and stable choice geometry before/after selection.
- [ ] Native content with Noto Arabic/Cairo/Amiri and Inter/Outfit: paragraphs, option-letter prefixes, diacritics, superscripts/subscripts, tables, hyperlinks and selection/vertical drags. Confirm no first/last-line clipping or accumulated line multipliers when changing language/theme.
- [ ] Drawer/Home/tab roots share Back behavior; tab re-tap returns to the list; repeating question ↔ knowledge ↔ case targets reuses entries; editor save/copy and legacy More restoration work. Busy/recovery Back shows the pending-operation message while protecting writes; pause/finish confirmation and response-loss reconciliation still work.
- [ ] Question language controls in ordinary study/exam/recall, detail, offline, master attempts and author preview: switch without changing app direction, answer index/draft/timer; regional keys resolve; incomplete/malformed choices fall back together; missing explanation prompts Original; separate case notice remains honest. Recall does not reveal choices through a language chip; exam correctness/explanation remains hidden until completion. Confirm account/server/process switching does not carry stale session content/preferences across identities.
- [ ] Arabic account/content identities, custom roles, URL/password/day/count fields, localized setup/choice/calendar digits, RTL chart chronology and accessible activity detail list. Monday week attribution remains consistent with server/planner contracts.

## Downloaded study feedback (2026-10-13, pending)

- [ ] Verify smaller explicit sync over normal and interrupted connections: initial full bundle plus individual question confirmation, fresh acknowledged-slot reads, full fallback for another/completed current index or current-question 404, original indices/frozen keys, ordered confidence/reflection changes, concurrent navigation/answer conflict choices, partial confirmation and storage failure. Lost response/expired/missing/other-account cases retain unsynchronized data and never replay automatically; measure actual request counts/bytes.
- [ ] With matching backend/app, download a study session, disconnect, answer wrongly and verify immediate frozen explanation plus correct-choice highlight/label and separate wrong-choice feedback. Check correct answers, confidence/reflection, Original/Arabic/English reading, themes, RTL, large text and TalkBack. No answer-key/explanation reveal before answering or in an active exam.
- [ ] Move between locally answered questions, use auto-advance, restore after process death, and explicitly sync the ordered queue before pause/finish. Confirm first-attempt grading, partial/lost-response reconciliation and conflict choices; old downloads without keys retain recoverable server-feedback fallback. Exam answers must reach the server before its deadline, including when no per-question sync occurs.

## Selected session presentation enhancements (2026-10-13, pending)

- [ ] Selected polish: ordinary answer numbers and master passive radios align to the first line of long Arabic/English/mixed choices; number gutters stay consistent, full-width question text remains and selected state is clear without redundant checkmarks. Correct/wrong labels/borders and softer opaque fills remain readable across all named themes and density modes. Question/secondary border hierarchy and shared medium rounding/insets look consistent.
- [ ] Tap all edges of the 48dp clickable area around the compact 36dp secondary outline, including the 6dp top/bottom inset; each activates once, keyboard/focus/TalkBack announces the button and disabled controls remain noninteractive/readable. Check long labels at 200% text, shared Retry/destructive commands, menu popup scrolling and fixed icon/loading slots.
- [ ] Check medium-weight labels, quieter counts/hints, compact explanation paragraphs/lists/headings and normal/native Arabic line spacing with every bundled/system font, diacritics, mixed scripts and text selection. Real-heading jumps and reduced-motion explanation reveal still work; labels/content must not clip or overlap.
- [ ] Clock layout stays stable across digits, 59:59 to 01:00:00, longer hours, overtime and critical state. Invisible sizing samples are absent from TalkBack/focus and ticking text is not a live region. At narrow widths/enlarged fonts, caption/clock stacks without clipping or hiding tools/progress/navigation.

- [ ] Optional haptics default off and persist across restart; settings/tools stay in sync. When enabled, accepted selections and server-confirmed answer/reflection/complete queued sync give subtle feedback according to device settings. Duplicate/rejected selections, local-only saves, failed/partial sync, background confirmation and restoration must not replay confirmation feedback. Answer correctness must not affect selection feedback in an active exam.
- [ ] Result All/Incorrect/Unanswered filters preserve original question numbers, server grading and full score/accuracy totals. Incorrect excludes unanswered rows; zero matches shows a readable empty state. Collapsed review content still avoids image reads. Check RTL, large text, keyboard and TalkBack selected states.
- [ ] Session tools groups guarded pause, pending sync, download/deadline details, sound and haptics without hiding timer/progress/Next. Check popup scrolling/focus, keyboard dismissal, sync/conflict/storage failures and system insets. Quick pending Sync remains reachable.
- [ ] Actual Markdown headings (including Arabic, setext, duplicate titles, inline emphasis and fenced-code lookalikes) expose only valid section links; plain/blank explanations have none. Jump to the native heading, switch language, change questions, expand/collapse result reviews and copy text without stale offsets or lost selection. Exam/recall reveal gates remain unchanged.
- [ ] For questions with an image URL, loading/decode/error/Retry/success use the same display bounds; wide/tall images fit without cropping. Error/Retry remains reachable at 200% text. Blank/missing image URLs, cases or translation metadata reserve no dummy area. Bookmark loading stays in its fixed labelled control slot and does not move the question or block answering.

- [ ] Check fixed question/timer/progress header and very compact bottom Previous/navigator/Next-Finish row on gesture and three-button navigation, keyboard open, narrow widths, 200% text, RTL/English, themes/density and TalkBack. Picker current/answered state, disabled busy/recall navigation and finish/pause confirmation remain correct. Sync/details retains deadline guidance; failures/conflicts/storage recovery remain reachable.
- [ ] Verify inline correct/selected-wrong labels and contrast, stronger question text, explanation heading/readability and opacity-only reveal. Question/choices must not animate position; disabled system motion reveals immediately. Verify selectable native Markdown, language changes, returning to answered questions, auto-advance and unchanged active-exam/recall answer hiding.
- [ ] Bookmark a visible question, navigate while its independent request runs and return; saved/unsaved/unknown status and Retry must refer to the right question without blocking answers. Exercise offline reads, deleted/invisible questions, explicit PUT failure/lost response, failed reconciliation and failed count reads. No automatic write replay or duplicate changes; account/server/process changes reload authoritative state.
- [ ] Remove individual book/category/tag/manual-tag chips while panels are collapsed, during failed scoped metadata and after restoration. Other selections remain and availability refreshes without starting a session. Resume mode/position/answered/pending/unknown state and Continue/discard guards must match saved data. Completed results show score/accuracy/unanswered counts, with readable expandable reviews and no hidden image reads; shared history review still shows its existing headings.

## Optional case translations (pending)

- [ ] With matching schema/backend, edit title-only, stem-only and complete Arabic/English case translations in the collapsed case-management section. Save, reopen, clear individual fields/all translations, rotate, restore encrypted drafts, refresh after read failure and resolve revision conflicts without losing the draft. Unauthorized writes stay rejected; uncertain responses require review rather than automatic replay. Stem-only edits preserve translations.
- [ ] Existing question-language controls in library, ordinary study/exam/recall, offline, master attempts and preview update case title/stem. Missing translations/fields retain originals; Original disables translations; a case-only language is selectable. No extra reader buttons appear. RTL/200% text/TalkBack remain readable, and recall/exam answer hiding, timers and answer indices remain unchanged.
- [ ] Save/download snapshots, then edit case translations: the earlier session, master attempt and offline pack retain their frozen content; old snapshots without translations do not borrow live data. New downloads/sessions include translations. JSON/CSV/XLSX and state round trips preserve case translations; old Cases worksheets without the optional column remain accepted. Import fills missing fields without overwriting local translations.

## Backend strengthening and compiler correction

- [ ] Authorized Android rebuild after ActivityRoute/AppMetadataRow scoped layout opt-ins; verify metadata/activity filter wrapping.
- [ ] Backend pack revalidation with concurrent edits/permission/account/deadline restrictions and quota races, IO/encoding outside locks, no grants on failure, old hashes/default/text-only compatibility.
- [ ] Lost responses/receipt replay, conflicting identifiers and unrelated integrity errors with rollback; diagnostic reason/duration/opaque header capture without tokens/private content, and preserved local material after server rejection.

## Android 0.1.2 pause and calendar

- [ ] Rebuild/install and confirm version name 0.1.2/code 3. Verify pause from Tools/back with pending downloaded answers: explicit Sync, pause and leave; successful confirmations exit once, while failure/conflict/storage recovery retains the dialog/question/queue. Check prefetch failure, duplicate taps, lost responses, finish recovery/local study finish and server exam deadlines.
- [ ] Toggle Numbers/Heatmap in the study activity calendar and restart. Verify 30/90/365 days, seven weekday rows/horizontal week columns, padded blank cells, qualitative legend, no visible numeric counts in heatmap, TalkBack dates/counts, RTL/English, all themes/densities and 200% text.

## Language-row bookmark, startup and compact themed shell

- [ ] Bookmark is at the language header's logical end (also with no translations), never narrowing question text; independent read/loading/saved/unknown/retry states and guarded writes remain.
- [ ] Fast/slow auth and restore retries finish the Compose title animation before automatic handoff; zero motion completes immediately and cached offline remains explicitly accessible. No stuck loading after completion.
- [ ] Bottom tabs show compact frames while full 48dp inset taps, focus/selection and system/IME padding work at 200% Arabic/English. First setup is empty; file controls precede manual entry and saved/imported privacy behavior remains.
- [ ] Device and app haptics enabled/disabled, rejected/accepted selections, confirmation/background suppression, native rejection fallback, device-policy/no-motor cases. All 18 theme names in English/Arabic and personalized shared shapes across cards/choices/dialogs/buttons, RTL and themes.

## Dashboard tips and drawer theme previews

- [ ] Did you know is second after welcome, before mode selection, expanded by default; collapse/resume, reads/errors/empty tips, refresh and rotation retain their existing behavior. Pending recovery remains visible below it.
- [ ] All 18 one-word theme names are unique in English/Arabic; drawer swatches match the theme's actual background/surface/accent, selection remains readable and changing themes preserves stored keys.
- [ ] Dropdown opens/scolls/focuses without measurement errors; scrollbar is visible at the logical end when content overflows, with correct thumb position. Check 200% text, RTL/narrow screens, all themes, keyboard/TalkBack and tap regions after rebuilding.
