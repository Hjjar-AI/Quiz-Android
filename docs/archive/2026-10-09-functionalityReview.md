> Historical record; may be superseded. Current state: [documentation index](../README.md).

# Android functionality compared with Vue and Django

Reviewed: 2026-10-07. Source review only; no build, live API or device validation.

Implementation follow-up on 2026-10-07 completed library filters/reset/page sizes, translated browsing/context, user/permission save hardening, learner master exams, expiry warning, and native help/privacy/support. A further source pass added source/ translation editing, explicit field clears and master-exam draft conflict guards. Question image management and case/knowledge associations are implemented too. The moderation follow-up adds bulk verification/notes, tag changes and pending-report resolution. The integration review also hardens bulk tagging visibility/transactions and protects editor revisions and untouched tag names. The initial findings below record what the comparison uncovered before these changes; the coverage table reflects current source. See `workDone.md` and `manualVerification.md` for implementation, limits and remaining live/device checks.

Comparison covers actions/outcomes, not design. Android implements the ordinary question-bank study loop, but it is not yet a functional replacement for every Vue workflow. An endpoint URL alone is insufficient: navigation, request/response models, authorization, actions, recovery and results must also exist.

## Initial findings before implementation (historical)

1. **Master-exam participation is absent.** Vue users can discover active/upcoming exams, see acknowledgement counts, acknowledge, start/resume an attempt, navigate, answer/flag questions, finish and view personal results. Django implements a separate master-exam domain. Android's ordinary Exam mode cannot substitute for it: there are no master-exam endpoints, DTOs, services or destinations in Android. This is the largest learner workflow gap, and belongs in the active learner plan.
2. **Finding questions is only partially equivalent.** Vue offers search, category, difficulty, verified and tag filters, reset and page-size selection. Android's library caller supplies search/page/page size, with separate review mode. The Android service already supports several filters, but the library does not expose them. Quiz setup filters do not provide filtered library browsing.
3. **Translated question content works inconsistently in Android.** Vue question cards use `localizedQuestion()` to select translated text/choices/explanation. Django returns base fields plus `translations`; it does not replace the base fields under Accept-Language in `QuestionSerializer`. Android's active session renderer handles translations, but `QuestionDto` omits them and library, detail and notebook renderers use the base content. An Arabic/English UI therefore does not guarantee the same translated learning material as Vue outside sessions.
4. **Question authoring is partial.** Basic text, choices, answer, explanation, tags, category, difficulty and version conflict handling exist. Vue additionally supports image upload/removal, translations, source/document/page metadata, knowledge-object association/inline creation, and case association/stems. Android cannot create or manage equivalent rich questions. This finding does not assert that editing base fields deletes omitted metadata on the backend.
5. **Existing administration is not complete or equally reliable.** Android has basic user create/edit, settings and permissions, but lacks user delete/reset, active-session management, moderation queues and other management domains below. Its user and permission handlers also need synchronous busy guards, captured submissions, cancellation handling and input validation. For example, user creation substitutes 60/0 for invalid expiry/renewal input, and permission selection remains editable while requests are in flight. The earlier settings fix does not fix these separate screens.

## Functional coverage

“Present” means the workflow is represented in production source, not runtime verified or identical in every secondary option.

| User workflow | Vue / Django capability | Android status | Remaining functional work |
| --- | --- | --- | --- |
| Authentication | Login, restore session, logout, forced/self-service password change | Present | Validate actual HTTPS host, cookies, expiry and password flow on device |
| Ordinary learning | Study, exam, recall; material selection; answers/navigation; timers; pause/resume; finish/results | Present | Validate end to end; do not rebuild this domain as master exams |
| Learning recovery | Saved server progress and completed history | Present, with Android encrypted unresolved-question drafts | Ordinary uncertain Finish now retains a task recovery marker and opens personal History through an independent capability gate; exact automatic result lookup needs a backend session-to-history identity link |
| Bookmarks and review | Bookmark toggles, mistakes/fragile lists, practice launches, smart/SRS review | Present | Device-check list refresh after practice and bookmark changes |
| Personal/admin history | User-scoped history, privileged all-user history and details | Present | Device-check fresh results and filters; preserve own-user scoping |
| Question browsing | Search, multiple filters, page size, selection and drill | Present after follow-up | Verify combined filters, restoration and pagination against real data |
| Translated library/notebook content | Locale-selected question/choices/explanation | Present after follow-up | Verify locale fallback, choice indices and answer hiding with real translated questions |
| Question context | Cases, images, explanations, source/knowledge metadata | Present after follow-up | Verify source/document/page and knowledge-object content on device |
| Master-exam participation | List, acknowledgement, attempts, timing, flags, personal results | Present after follow-up | Verify timing/recovery/flags against a live backend. Restored-task detail/runner/result reopening, list filter restoration and Previous/Next navigation are now implemented in source. Fresh-launch reopening from Home, encrypted unresolved learner answers and a durable Finish marker are now implemented. Deep links and preview/remaining authoring stay separate; report/authoring drafts remain memory-only. |
| Question authoring | Rich questions, images, translations, sources, cases and knowledge objects | Partial | Base/source/translation editing, case/knowledge association and saved-question image upload/removal are present. Inline knowledge creation is now present too. Dedicated shared-stem editing is now present; structured tags are now implemented too; formatting controls, camera capture, revised date and encrypted question/image recovery are implemented in source. Device checks are pending. |
| Verification/moderation | Single/bulk verification, notes, bulk tags, ratings, flags and flag resolution | Partial | Single verify/rate/report, bulk verify/unverify with notes, and paginated report triage/resolution exist. Bulk tag actions are implemented too; batch rating reads and verification statistics remain. |
| Categories | List/create/edit/delete | Present | Device verification |
| Knowledge map and details | Objectives, mastery/coverage, full reference content and linked practice | Present | Detail and public-question links are implemented; verify regional translation fallback and practice limits on device |
| Knowledge/case management | Backend CRUD; Vue editor lookups/association and inline knowledge creation | Partial | Read-only knowledge details, case search/detail/ordered questions and practice are implemented, alongside authoring associations. Inline creation and dedicated shared-stem writes are present too; broader CRUD remains |
| Planner | Targets, category/tag scope, dates, progress, delete | Present | Device verification; account/date behavior must match backend |
| Member groups | Membership list, leaderboard and personal visibility | Present in source | 7/30/90-day periods, returned rank/current streak/self marker, account-bound selection restoration and read-first visibility recovery are implemented; group administration remains separate |
| Analytics and activity | Summary/mastery/streak reports, heatmap, privileged reports | Present in source | Core member/admin reports plus own-user 30/90/365-day activity calendar, counts and accessible date list; device verification pending |
| Preferences/access/help | Theme/language, study defaults, expiry warning, manual/privacy | Present in source | All web theme/font choices, browsing defaults/density, ordinary-quiz auto-advance/sound, full bilingual manual/privacy and native guidance; existing frontend contact/policy approved by user |
| User/permission administration | User management, role matrix, overrides, resets/deletion, active sessions | Partial | Fresh account detail, activation, supplied-password reset and independently gated five-minute activity snapshot implemented; confirmed deletion and generated temporary-password reset are now implemented in source; expiry/input polish and better permission lookup remain |
| Group/tag/blueprint administration | Manage groups/members, tag hierarchy/rename/merge/delete, blueprints | Missing as management workflows | Learner groups, tags and blueprint selection do not replace management |
| Master-exam authoring | Composition/drafts, preview, publish/cancel, bank publication, results/matrix/CSV | Present in source | All selected management surfaces are implemented; independently gated drafts/preview/results, ownership, uncertain outcomes and lifecycle timing need live/device checks. Separate from learner participation. |
| Data operations | Import, export including PDF/state, quality reports, backups/restore/clear | Partial | Native simple/Telegram/state imports and JSON/CSV/XLSX/PDF/state exports, mappings/conflicts, verified-only and exam CSV are implemented; database info/quality flags/backups/restore/clear and admin.seed utilities are implemented in source; operator actions remain unexecuted |

## Source evidence

- Navigation and overall surfaces: [Vue router](../../../frontend/src/router/index.js), [Vue endpoints](../../../frontend/src/services/api/endpoints.js), [Android navigation](../../app/src/main/java/com/mukhtabir/android/ui/MukhtabirApp.kt), [Android endpoints](../../app/src/main/java/com/mukhtabir/android/data/remote/ApiEndpoints.kt).
- Master exams: [Vue list/actions](../../../frontend/src/features/masterExams/views/MasterExamList.vue), [Vue acknowledgement store](../../../frontend/src/stores/masterExamStore.js), [Django routes](../../../backend/apps/master_exams/urls.py), [Django acknowledgement views](../../../backend/apps/master_exams/views/crud_views.py).
- Library filters: [Vue filter controls](../../../frontend/src/features/questions/components/FilterBar.vue), [Android library and detail callers](../../app/src/main/java/com/mukhtabir/android/ui/questions/QuestionLibraryScreen.kt), [Android services](../../app/src/main/java/com/mukhtabir/android/data/remote/FeatureServices.kt), [Django list/count views](../../../backend/apps/questions/views/question_views.py).
- Translations/context: [Vue question cards](../../../frontend/src/features/questions/components/QuestionCard.vue), [Vue localization helper](../../../frontend/src/utils/localizedQuestion.js), [Django read serializer](../../../backend/apps/questions/serializers/question_read.py), [Android DTOs](../../app/src/main/java/com/mukhtabir/android/data/remote/FeatureModels.kt), [Android session renderer](../../app/src/main/java/com/mukhtabir/android/ui/quiz/QuizScreen.kt).
- Authoring: [Vue question form](../../../frontend/src/features/questions/components/QuestionForm.vue), [Android editor](../../app/src/main/java/com/mukhtabir/android/ui/questions/QuestionEditorScreen.kt).
- Administration: [Vue users](../../../frontend/src/features/admin/views/Users.vue), [Android users/permissions](../../app/src/main/java/com/mukhtabir/android/ui/admin/AdminScreens.kt), [Django user routes](../../../backend/apps/users/urls.py), [Django permissions](../../../backend/apps/users/views/permission_views.py).
- Other surfaces: [Vue preferences](../../../frontend/src/features/preferences/views/Preferences.vue), [Vue expiry warning](../../../frontend/src/components/layout/ExpiryWarning.vue), [Vue database operations](../../../frontend/src/features/admin/views/DatabaseInfo.vue), [Django database routes](../../../backend/apps/database/urls.py), [Android About](../../app/src/main/java/com/mukhtabir/android/ui/about/AboutScreen.kt).

## Delivery order (source stages 1–2 now implemented)

1. Fix confirmed issues in existing supported actions, especially user/permission writes; retain the already implemented learner/backend corrections.
2. Complete learner functionality: master-exam participation, translated browsing, library filters and required question context, followed by account/help surfaces.
3. Complete contributor/moderator workflows: rich question editing, moderation, master-exam authoring and group/tag/blueprint management.
4. Add remaining operator/file workflows and optional preferences/activity features.

Each stage needs its own end-to-end validation. Backend-only capabilities should not automatically become new mobile screens. Design duplication is not a requirement. The initial comparison was read-only. Follow-up production-source changes are logged separately in `workDone.md`; they are not yet runtime verified.


## Account lifecycle parity follow-up — 2026-10-08

Android account tools now implement Vue/Django generated temporary-password reset and password-confirmed permanent deletion. The native flow retains returned secrets through failed follow-up reads, requires explicit acknowledgement of uncertain reset outcomes and reconciles deletion with read-only account lookup. Django supports no deletion-mode options and rejects question/protected-record links. Source implementation only; expiry/input polish remains under P3.1 and device checks remain pending.


## Normal-user continuation — 2026-10-08

Compared Vue MyGroups and assigned-exam runner/store with current Django group views/leaderboard service and attempt status/answer/sweeper contracts. Android now honors returned assigned-exam grace seconds rather than finishing at the main deadline, with an accessible grace notice/countdown and authoritative status recheck. Member groups now expose matching 7/30/90-day periods and returned rank/streak/self data, restore only account-bound navigation metadata and refresh on foreground. Fixed source-confirmed list-key collisions and visibility recovery that could repeat a write without first reading current membership state. Source completion only; learner device/backend checks remain pending.


## Learner assigned-exam restoration and navigation — 2026-10-08

Android now preserves selected exam/list filter as account-bound SavedStateHandle metadata, matching Vue's exam-ID route/list-tab navigation intent. On Android task/process recreation it re-reads detail, caller-owned status and current frozen question or completed personal result; failures retain the exam for Retry. It never auto-starts, acknowledges, submits an answer or replays navigation/finish to restore the page. Added Previous/Next controls through the existing guarded goto path, accessible answered/unanswered numbered controls and shared active-session screen-awake behavior. Unsaved answers/reports remain memory-only; cold launches without restored navigation state, deep links and encrypted master drafts remain separate work.


## Critical normal-user recovery follow-up — 2026-10-08

Implemented account-bound encrypted unresolved assigned-exam answer/baseline recovery, fresh-launch destination reopening from Home and durable assigned-exam Finish uncertainty tied to the original attempt session. Restoration reconciles server answers/results without automatically submitting drafts or replaying Finish. Ordinary quiz Finish now retains an uncertainty marker within Android task saved state and requires original-session reads plus explicit review, with separately gated personal History access. Django deletes ordinary sessions at Finish and does not retain their session IDs in TestHistory; exact automatic result identity remains a backend prerequisite. Optional offline/deep-link/notification/session-management work is unchanged.


## Exact ordinary Finish-result recovery — prepared source, 2026-10-08

Django now has prepared source for a nullable unique History source-session identity recorded atomically by Finish and a caller-owned GET on the existing results endpoint. Android verifies session/mode/history identity and reopens the frozen result after an uncertain Finish and missing active session, without another POST. Older backends and unmapped historical/discarded sessions keep the existing personal History fallback. Requires explicit migration authorization/application before deployment; source checks alone do not establish HTTP/runtime correctness.


## Ordinary Finish cold-launch recovery — 2026-10-08

Ordinary uncertain Finish identity is now stored in the existing encrypted account store before the Finish POST, rather than relying only on Android task saved state. Fresh launch exposes a Home review shortcut and prioritizes the original session/mode during read-only discovery. Failed commits prevent the POST; failed cleanup preserves confirmed results and exposes local-only Retry. Exact GET-result recovery still awaits the separately required backend migration/deployment; old backend/historical sessions retain personal History fallback.


## Requested enhancement parity — 2026-10-08

Source now includes group/blueprint management and account presets against current Django/Vue contracts, extended Markdown controls/rendering, server-bound custom question/exam links, opt-in local reminders/in-app notices, caller-owned session inventory/revocation, encrypted offline study packs/first answers and explicit idempotent synchronization. Native read gestures/About/case query restoration are also present. Earlier entries saying these families are wholly absent are historical. Blueprint difficulty is not persisted by the current backend; category weights are the supported allocation. Offline synchronization is prepared source and requires additive migration authorization/application for source-session identity and durable receipts plus matching deployment. See workDone.md for bounds and source-only verification; manualVerification.md retains runtime gates.
