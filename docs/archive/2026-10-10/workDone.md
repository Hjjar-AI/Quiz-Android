> Historical snapshot before the 2026-10-10 documentation reorganization. Current status: [documentation index](../../README.md). Do not use unchecked items here as the current queue.

# Android completed source work

Updated 2026-10-09. Detailed dated history and earlier verification scopes are retained in [the archive](../2026-10-09-workDone.md). This summary does not promote historical checks into current runtime verification.

| Completed family | Contracts / limits retained |
| --- | --- |
| Learner core | Auth/configurable server/profile/password; study/exam/recall, rich context, bookmarks/review/history, question filters/translations, knowledge/planner/groups/activity/help/preferences. |
| Recovery and private state | Encrypted account/server-bound ordinary/master answers, Finish identity, authoring/images and planner drafts; explicit restoration and read reconciliation. Legacy History fallback remains; uncertain requests never auto-replay. |
| Master exams | Learner acknowledgement/attempts/grace/results; author composition/audience/co-authors, private and embedded drafts, preview/lifecycle, privileged results/matrix/CSV. Capabilities and ownership are independent. |
| Contributors/moderators | Rich question fields/translations/source/review date, exact tags, Markdown/camera/images, cases/knowledge associations; single/bulk verify/notes/tags, report resolution, batch ratings and metadata. |
| Administration | User lifecycle/expiry/reset/delete, capability matrix/overrides and narrow user lookup; category/group/member/blueprint/tag management; standalone knowledge/case CRUD. Fresh checks do not claim atomic revision guarantees. |
| Files/operators | Native imports, mappings/conflicts, custom/verified/PDF/state exports and CSV; DB info/quality/backup/restore/clear, seed/rank tools. Operator actions were not run. |
| Presentation/native additions | SVG launcher source organization, dashboard/drawer/collapsible sections, broad question reading, bilingual themes/fonts/density, local notices/reminders/offline synchronization and own-session inventory. |

New standalone management drafts and uncertain-write markers remain memory-only. Knowledge lists cap at 500 and case lists at 100 under current Django contracts. Offline receipt/result-identity models require the actual deployed schema; the recorded local SQLite read-only check applies only to that file. Signed-cookie engines cannot revoke individual server sessions.

The user reported one successful debug build before subsequent edits. Later source/resource/AST checks do not establish compilation, device behavior, live API correctness or database concurrency.

## 2026-10-09 source comparison and record compaction

Reviewed current Vue route/actions and Django contracts against Android wiring/services/UI/DTOs. Confirmed major workflow representation and recorded six bounded gaps in [functionalityReview.md](../../reference/functionalityReview.md): analytics read isolation/period/gate/presentation, export reordering and library card ratings/ranks. Archived previous records before replacing stale duplicate inventories. Documentation only; no production fixes, build, suite, migrations, deployment or version changes.

## 2026-10-09 normal-user parity follow-up

- Analytics now loads summary/mastery/streak and each permitted admin report independently, preserving readable data and per-report retries. Summary period is 7/30/90 days, stored as account-bound navigation metadata; canceled older-period reads cannot overwrite current results. Retained summary data identifies its actual period. Full daily trend/streak values are available in collapsible sections beside charts, with wrapping rows and bilingual labels.
- Library cards now show author identity and returned rank using the existing bilingual rank registry, plus batch-read rating averages/counts and an explicit rating dialog. Shared QuestionService batching replaces the duplicate moderator implementation. Rating drafts/results remain memory-only; busy guards stop duplicate writes, confirmed success survives failed refresh, and unknown saves require fresh read plus explicit acknowledgement before another save. Page changes and read cancellation preserve stable question identities; ratings failures leave the library usable. No per-card GET on initial list render.
- Added matching English/Arabic resources and native account-keyed state. Source/XML/resource/import/lexical/whitespace checks only; compilation, real HTTP/response-loss behavior, process/device rendering and accessibility remain unverified. No builds, suites, migrations, backend changes or versions. Remaining reviewed implementation gaps are admin analytics presentation/active-user gate and operator export reordering.

## 2026-10-09 admin analytics and ordered exports

- Wired `admin.active_users` independently of `analytics.view_all` in route/factory/handler state. Summary and active-user period reads cancel separately on changes; retained trends identify their actual period. All trend dates have accessible values. Server-denied report reads revoke that report's retained data; transport failures retain readable results.
- Replaced raw admin JSON/title rendering with curated bilingual fields using shared report cards/paging. Percentages, counts, week dates and duration buckets are formatted; duration retains total sessions/average seconds. All returned report/tag rows are accessible. Fixed backend default windows (90-day duration, 30-day cohorts, up to eight retention weeks, top-20 author report) are explicit. Stable LazyColumn keys preserve independent report controls when reads finish/fail in different orders.
- Added ordered export positions and labeled move-up/down/remove actions. Callbacks resolve current IDs and current busy state; list order feeds existing `question_ids` for PDF/Excel/CSV/JSON without new API fields. Selections remain memory-only as before.
- Current production contract review, bilingual XML/resource/import/lexical and whitespace checks passed. No builds, suites, migrations, backend changes, deployment, destructive actions or versions. Compilation, actual HTTP/data/viewer behavior and device accessibility remain pending; the six identified source parity gaps are complete.

## 2026-10-09 final cross-cutting review

Reviewed current Android/Vue/Django production contracts, ownership, navigation and failure-state paths beyond endpoint presence. Recorded six additional findings in [finalCrossCuttingReview.md](../reviews/2026-10-09-finalCrossCuttingReview.md) and reopened the implementation queue: uncertain bookmark/duplicate recovery, structured API errors, profile/capability freshness, dependent selector invalidation and category icons. Older feedback/category/settings write guards also need consistent uncertainty handling. Review/documentation only; no production fixes, builds, suites, migrations, deployment or version changes. Source comparison does not establish runtime correctness.

## 2026-10-09 supplied Kotlin build-error fixes

Corrected the causes reported by the user's failed debug build: 24 ErrorState calls now name the Throwable parameter instead of occupying Modifier; Material3 opt-ins are scoped to affected composables; two suspend persistence helpers explicitly check the current coroutine context; question-editor recovery visibility matches its internal stored type. Shared controls, dependency versions and recovery behavior remain intact. Production XML/resource/import/lexical and whitespace checks passed; no agent compilation/build or suites were run. User debug rebuild remains pending; the six cross-cutting review findings remain open.

## 2026-10-09 agent guidance maintenance

Compacted shared working rules without relaxing explicit build/suite/migration/version restrictions; added source-evidenced cross-client review and bounded-completion guidance. Captured uncertain toggle/create recovery, structured errors and dependent/profile freshness; Android also records compiler-signature/opt-in/cancellation/visibility checks. Frontend AGENTS now links its detailed instructions. Documentation links and whitespace checked; no production or runtime work.

## 2026-10-09 server connection files and retained local profiles

- First launch/Login/Settings share manual entry plus native JSON file import. Strict v1 nested public format, bounded 64 KiB reads, normal URL restrictions and existing server/HTTP confirmation; failed/canceled imports preserve input. Streams close without temporary imports.
- Android admin settings (`admin.settings` route and handler) exports an unencrypted nested connection file through CreateDocument. The administrator can specify a phone-accessible alternate address; randomized decorative identities carry no authority, account data or credentials. The web admin Settings page exports the same v1 format using its shared download helper and independent bilingual address validation; no backend request/schema.
- Address changes retain encrypted downloads/pending offline completions/recovery in independent local server profiles. Known normalized addresses reuse their profile; explicit same-server alternate-address selection aliases the current profile without automatically trusting file identities. Distinct known profiles cannot silently merge. Legacy store remains compatible. Cookies/reminders are revoked, old requests canceled and task/account UI restarted. Login transport reset no longer deletes retained recovery before authenticated account binding; different accounts/logout/expiry keep cleanup rules. Late detached-origin responses cannot erase archived data. A durable reauthentication gate prevents cached offline access after switching until matching profile/account binding succeeds.
- Production source/resource/XML and whitespace checks only; no builds, compilation, suites, migrations, deployment or versions. Native picker/provider failures, process recreation, actual account isolation and same-backend alternate IP recovery remain device/live-backend checks. Frontend JavaScript syntax/locale checks passed; Vue SFC parser is unavailable in the workspace, and browser validation remains pending. See [server configuration](../../contracts/serverConfiguration.md).

## 2026-10-09 hidden imported server controls

- File-selected addresses stay separate from manual input and are never rendered in the setup/confirmation controls. File mode displays a generic status and supports replacement by another file or blank manual entry; failed/canceled replacement retains the prior selection.
- Persisted configuration-source metadata keeps the address hidden after save, restart and reopening Change server, including signed-in Settings. Android admin export does not prefill an imported endpoint. Manual entry is an explicit new address; same-server local profiles and retained encrypted files are unaffected.
- Bilingual XML/resource/reference, production-source delimiter and whitespace checks only; no build, suites, migration, backend/Vue changes or versions in this follow-up. Device checks remain pending. The unencrypted JSON/network/stored configuration still contains the endpoint; this change hides it in the server UI.

## 2026-10-09 three saved imported connections

- Added a bounded three-slot public connection registry with numbered labels and no displayed addresses/filenames. Settings has direct saved-file selection actions; first launch/Change server supports selecting, uploading and explicitly replacing each slot. Existing single file configuration remains available as slot 1 and is retained when the registry is first written. The confirmed slot selection persists independently even when two files contain the same normalized URL.
- Import persists only a validated normalized endpoint in the requested slot without activating it or touching private profiles. Switching uses the shared hidden-address confirmation/restart/account-isolation flow. Canceled/invalid/unreadable import retains the previous slot/selection; canceled setup retains the newly stored public slot while leaving the active server unchanged. Replacing a slot retains local study files and original documents; no fourth slot or automatic eviction.
- Bilingual XML/resource/placeholder, production-source delimiter/reference and whitespace checks only; no build, compilation, suites, migrations, backend/Vue changes or versions. Settings selection, native picker process recreation, storage errors, three-slot persistence and local-profile recovery remain device checks.

## 2026-10-09 full accessible question-bank download

- Added explicit full-bank download, confirmation, saved/initial/skipped counts, pause and durable explicit resume in Offline practice. The new Django `tests.start` catalogue keyset-pages all `visible_to(user)` IDs, including the user's own drafts, while excluding closed-book protected questions; existing pack generation independently rechecks visibility/protection/readiness. Read-only pack size/content rejections split into smaller batches; unsupported single questions are counted, while transport/auth/server errors stop without discarding earlier batches.
- Replaced the five-pack/8 MiB aggregate preference storage with immutable encrypted no-backup files, encrypted index/checkpoint and lazy per-pack reads. IO persistence atomically publishes pack index plus progress; failed commits keep readable prior files and pending memory changes. Legacy packs migrate on successful writes without resetting answers/frozen completion identities/results. Account/server cleanup and explicit receipt-backed synchronization remain intact.
- Current Django/Android route/DTO/handler/storage paths, three changed Python production ASTs, bilingual XML/resources/placeholders, five changed/new Kotlin lexical/reference checks and both repository whitespace checks passed. No compilation/build, suites, migrations, setup, deployment, versions or live downloads were run. Matching backend reload is needed for the schema-free catalogue; synchronization still requires the existing deployed receipt schema. See [contract/limits](../../contracts/offlineQuestionBank.md) and [manual checks](../../verification/manualVerification.md).

## 2026-10-09 per-user full-bank capability

Added `tests.download_full_bank` to the canonical backend registry and Tests & study permission group, without member/moderator seed grants. Existing role/per-user JSON editors support Allow/Deny/Inherit; web descriptions and native labels are bilingual. The full-bank catalogue requires the new capability plus tests.start, and each `full_bank: true` snapshot request independently checks both. Selected-question downloads/completion synchronization keep their existing tests.start gate.

Android reads fresh permission on online Offline-practice entry and explicit Refresh download permission, gates full-bank buttons/confirmation/handlers, and blocks further bulk Start/Resume after server 401/403. Read/permission failures retain readable cached packs, progress and pending completions. No remote deletion of downloaded data. Registry/AST/XML/JSON/resource/placeholder/source-delimiter and whitespace checks only; build, HTTP permission revocation, admin override controls and device checks remain pending. No migrations, setup/seeding, suites, deployment or versions.

## 2026-10-09 moderator full-bank default

Updated the moderator seed/fallback role to include `tests.download_full_bank`; members remain off by default and per-user deny overrides still apply. Existing saved role rows are preserved rather than overwritten: older deployments can enable this one capability through Permissions → Roles → Moderator. Production AST/registry/default checks and whitespace passed; no database writes, setup/seeding, migrations, suites, builds or deployment.

## 2026-10-09 metadata/profile/validation and question variety

Implemented the three requested Android follow-ups: library/quiz metadata re-entry refresh with valid-selection preservation; foreground/navigation/once-per-minute foreground/confirmed-write/403-triggered profile refresh with explicit Profile control and account identity checks; bounded safe field-validation details/recognized auth reasons retained in request/download errors and bilingual shared presentation. Permission-driven navigation changes defer during protected drafts/writes/sessions. Native OfflineStudyViewModel now explicitly imports the shared UserDto.has extension discovered during source tracing.

Backend automatic ordinary study/exam/recall selection now prefers user-unseen IDs, then lower exposure counts and older exposure, with randomized ties/order. Blueprint quotas use the same preference inside existing category allocations/fallbacks. Existing user-scoped history results and ordinary/master allocations supply evidence; explicit IDs, SRS due order, fixed master-exam question sets, filters and requested caps remain intact. There is no new schema and no claim of exact screen-view tracking or concurrent no-repeat reservation. See [selection policy](../../../../backend/docs/contracts/questionSelection.md).

Production Python AST, bilingual XML/resources/placeholders, Kotlin import/reference/lexical and whitespace checks only. No builds, suites, migrations/setup, live HTTP/downloads, deployments or versions. Device validation of stale responses, revoked capabilities, drafts/active sessions and large user histories remains pending. Older uncertain bookmark/duplicate/detail-management writes and category icons remain open.

## 2026-10-09 backend learning review integration

Added coverage/balanced/review setup choices with saved account-scoped setup state; offline signed-time anchor/clock offset and encrypted frozen answer/completion timestamps, preserving legacy pending bodies; typed distinct question/concept/due-review/mastery-change metrics; archived master weighted scores in History; explicit bookmark state/removal; caller-owned creation/duplication receipt reads and explicit same-identity retry controls. New-question receipt identity uses existing encrypted recovery; detail duplication remains memory-scoped. Knowledge/group writes now send retained baseline revisions. Backend owns chronology/provenance/permission enforcement; local server profiles/files remain intact.

Source/XML/resource/Kotlin delimiter/whitespace checks only. Matching fresh backend schema, rebuild, live HTTP and device verification remain pending; no builds, suites, migrations/setup, database actions, deployment or versions. Exact policies: [learning consistency](../../../../backend/docs/contracts/learningConsistency.md).

## 2026-10-09 categories, restart recovery, write outcomes and concurrent edits

Implemented category icon selection using the same Bootstrap values as Vue, including retention of unfamiliar existing icons. Native category create/update/delete, feedback ratings/reports and runtime settings now checkpoint pending intent, retain confirmed success through read/storage failures, freeze unknown writes and expose explicit read/manual-review controls. Category and knowledge creates use caller/operation/body-bound receipts; retries retain the original identity/body. Feedback reads independently retain successful rating/report data.

Standalone knowledge/case/tag/stem drafts and detail duplicate/bookmark intent now reuse authenticated profile-bound encrypted editor storage. Restore/discard is explicit; discard confirms that a pending write may already exist. Restoring never automatically sends a mutation. Required checkpoints prevent requests when local persistence fails; recovery storage errors retain the memory draft/outcome and expose Retry. Account UUID supplements numeric ownership, and alternate addresses of the approved same-server profile retain local files. New matching backend contracts and fresh schema are required.

Category/case row revisions and a shared tag-tree/settings revision reject stale writes under backend locks. Native editors retain the revision shown with the draft; latest reads lead to explicit conflict review. Vue category/tag/settings payloads and case service methods carry matching preconditions. Browser restart recovery stores operation identities only, scoped to account UUID/API origin; no question text or images are persisted. A restored question-create identity can only be checked, opened, or explicitly acknowledged after a missing receipt, never attached to a new form/image or automatically resent.

Verification: production Python AST, JavaScript/extracted Vue script syntax, locale JSON, native lexical delimiters, bilingual XML/resource/placeholder and whitespace source checks. No compilation, builds, automated suites, migration inspection/work, database/setup/seeding, deployment or versions. Process death, disk-full, lost responses, live HTTP and server-database concurrency, RTL/large text/TalkBack remain manual release gates.

Retained native detail/content/stem editors also apply current profile capabilities directly to mutation-handler policy without replacing a draft. Final source checks covered 242 production Python files, 296 JavaScript/extracted Vue scripts, 152 native lexical files and 1,380 matched bilingual string resources; these are not compiler/runtime checks.

## 2026-10-09 full Android content-management pagination

Case and knowledge-object list endpoints now accept page/per_page for complete browsing with stable key/PK and title/PK ordering, existing filters/visibility and shared bounded page sizes. Responses include total/page/per_page/total_pages; knowledge count remains available. Requests without pagination parameters retain the legacy autocomplete contract and limits, keeping current Vue/native pickers compatible.

Android management Next/Previous now fetch server pages and render the returned batch directly. Search starts at page 1; retry retains the requested page and filters while failures preserve the previous readable page, counts, selections and editor drafts. Refresh after deletion retries the last available page when the old page is beyond the new total. Pagination controls and handlers retain recovery/permission/uncertain-write guards. The page count label is bilingual.

Verification: two affected Python production ASTs, native lexical/resource/XML/bilingual-placeholder checks and CRLF-aware whitespace checks passed. No builds/compilation, automated suites, migrations, database/setup/data generation or live HTTP/device checks. This pagination change needs matching backend/native source but introduces no schema changes. Verify datasets beyond 100 cases/500 knowledge objects, searches/filter changes, failed next-page requests/retry, final-page deletion and retained drafts against the running backend.

## 2026-10-09 project rescan

Inventoried production source and traced recovery/pagination/account/CRUD/ownership/conflict paths across Django, Android and Vue. Recorded seven prioritized findings in [project rescan](../reviews/2026-10-09-projectRescan.md). Review/documentation only; production unchanged, no builds/suites/migrations/database/runtime work. Existing completed source features remain completed; newly evidenced gaps are in the plan.

## 2026-10-09 seven rescan findings fixed in source

- Web transport uses an independent authenticated-session generation for shared requests, cancellation, success/error handling and delayed expiry redirects. Old account requests are aborted/ignored; CRUD completion guards prevent old callbacks from repopulating reset stores. Login/initial restore can publish only their own matching one-step account transition. CSRF rotation remains separate.
- Web AddView marks an orphan pending create as review-only on route departure/re-entry even when storage was already hydrated. Confirmed late results keep their receipt identity for the new view. Save/image flows also check component lifetime/account generation, so an old response cannot consume a new form's images or navigate an unmounted form. Browser draft text/images remain memory-only.
- Question update/delete services recheck current ownership/override against locked rows. Question and knowledge DELETE now require expected_version in query parameters; native detail confirmation and all reachable Vue question-delete handlers capture the displayed version before confirmation. Knowledge native deletion uses its retained editor baseline. Protected relationships/reputation behavior remain, and stale/rejected deletes retain readable items.
- Caller-scoped content receipts report whether their committed target still exists. Native category/knowledge reconciliation distinguishes deleted targets from absent operations/read failures, offers a terminal acknowledgement, preserves the typed draft and retires the exhausted UUID. Only a subsequent explicit new save creates a new identity.
- Shared bilingual Vue revision-review controls load current data independently and expose Keep draft/Use server choices for categories, tags and Settings. Drafts stay intact during read failures; tag source/target identities rebase by ID only after explicit review. Removed categories can prepare a new creation without discarding draft content. Settings dirty-state baselines adopt the reviewed server values without replacing kept inputs. Permissions/account scope gate reads and resolution; no automatic replay.
- Learner case browsing now uses server page/per_page and the configured page size, with Next/Previous. Applied query/results commit only after successful reads; failed search/page requests retain readable results and retry the requested query/page. Management pagination and bounded autocomplete contracts remain intact.

Validation: production Python AST (242 files), JavaScript/extracted Vue script syntax (299), locale JSON, native lexical delimiters (152 files), bilingual XML/resources/positional placeholders (1,382 strings per locale) and CRLF-aware whitespace checks passed. These are source checks, not compilation/runtime evidence. No builds, suites, migration inspection/work, database/setup/data generation, deployment or version changes. Matching backend/native/web rollout is required for the new DELETE preconditions and receipt status; this follow-up introduces no model/schema changes. Verify delayed responses/account switches, orphan creates, deleted receipts, stale ownership/revisions, read failures and large learner case lists over actual HTTP/device/server-database concurrency.

## 2026-10-09 deeper source review

Traced master/ordinary session transactions and callers, offline completion, learning evidence, import updates, planner configuration and web polling. Recorded eight source-evidenced follow-up areas in [deep review](../reviews/2026-10-09-deepTransactionReview.md). Review/documentation only: production unchanged, no builds/suites/migrations/database/runtime actions. Existing learning/offline protections were distinguished from new gaps; concurrency impact remains unexercised.

## 2026-10-10 eight deeper findings fixed in source

All eight bounded findings are addressed in production source; the original review remains historical evidence.

- Assessed parent saves lock linked questions before parent rows; knowledge/question/tag writers and state imports coordinate their dependency locks. Existing imported questions and knowledge objects advance the locked local revision instead of adopting the source revision. Imports deliberately take broad user/question locks; large-import contention still needs measurement.
- Master start locks fresh access relations, rechecks new-start/preview access, resumes either active normal or makeup attempts before new-start classification, and finishes expired attempts without granting extra time. Existing owned work is retained.
- Master answers require session_id and the full expected_slot (including null), compared under lock. Identical saved intent is idempotent; conflicting saved intent returns ATTEMPT_PROGRESS_CHANGED. Navigation requires session_id and expected_current_question_id; current-question reads do not persist fallback navigation. Expiry finish failures propagate.
- Web/native master recovery retains uncertain drafts, reads authoritative full answer slots, and requires explicit comparison/rebase/discard before another conflicting write. Web polling has disposal/cancellation generations and exam/session/store/progress ownership guards; route changes cannot adopt an old attempt response.
- Planner update/delete requires expected_id and expected_version; successful updates return accepted configuration/revision. Both clients retain editor baselines and provide explicit conflict resolution. StudyPlannerScope/ScopeDay preserve timestamp-based historical filter attribution: explicit filter/date changes reset today's displayed progress, taxonomy rename/merge continues it, and historical days include all their original scope activity after midnight without double counting. Global learning events remain intact.

Verification is production AST, JavaScript/extracted Vue script syntax, locale JSON, native lexical/XML/resource/placeholder and whitespace checking only. No builds, compilation, automated suites, migration inspection/work, setup/seeding, database writes, deployment or versions. Matching backend/web/native rollout and matching planner schema are required. Actual HTTP, database interleavings, browser/device recovery and accessibility remain release checks.
