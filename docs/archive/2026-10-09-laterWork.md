> Historical record; may be superseded. Current state: [documentation index](../README.md).

# Android Supplementary Backlog and Scope Details

Updated: 2026-10-07

[workPlan.md](2026-10-09-workPlan.md) is the execution order and remaining-work authority. Preserves earlier inventory details/conditional ideas; historical priorities do not override the roadmap. Entries are not necessarily beta requirements or confirmed defects. Choose useful supported batches, not endpoint coverage alone.

Completed portions are logged in [workDone.md](2026-10-09-workDone.md). Source presence differs from [manualVerification.md](2026-10-09-manualVerification.md); this reorganization is not a fresh exhaustive review of every backend management endpoint.

## Existing-behavior polish deferred from P0

- Exact ordinary result recovery now has a prepared backend source-session identity/read-only lookup and matching Android handler. It awaits additive migration authorization/application and deployment. Legacy rows remain unmapped and use personal History; no date/content matching or Finish replay.
- Investigate specific remaining secondary selection losses on devices; member-group selection/period and case browser applied/draft queries now restore. Core session, library, notebook, knowledge-filter and History restoration already exists; theme and language already use persistent preferences.
- Additional accessibility refinement: implement specific defects found during use, rather than an open-ended requirement to redesign every control.

## Optional learner improvements — former P1

### Find and select material

Library polish requires reproduction: investigate Back-scroll only if position is lost; author/ownership filters need API support. Completed filters/reset/page size: `workDone.md`.

### Account access and help

Full web manual/policy equivalence and optional About links remain below. Completed account/help surfaces are logged in `workDone.md`.

## P2 — learning enhancements and assigned exams

### Member master-exam follow-up

See `workDone.md` for completed participation and draft guards. Follow-up must preserve list-based server-saved attempt recovery; Django's visible-exam list does not accept invented pagination.

- [ ] Optional native preview for permitted authors, separately from learner attempts.
- [ ] Optional external deep links. Fresh-launch assigned-exam reopening from Home and encrypted destination recovery are implemented in the 2026-10-08 critical recovery follow-up.
Encrypted unresolved learner master-exam answer/baseline recovery is implemented in the critical recovery follow-up (2026-10-08); unsaved authoring forms and report reasons remain memory-only.

### Activity, motivation, and navigation polish

- [ ] Fetch/display the dedicated current streak endpoint (`GET /study/streak/`).
- [ ] Add the activity heatmap (`GET /study/activity-heatmap/?days=...`).
- [ ] Add deeper dashboard drill-down only if needed; current cards already navigate to quizzes, bookmarks, History and analytics.
- [ ] Decide whether Vue's notification-bell presentation has meaningful Android equivalents; if so, implement an in-app surface and optional system notifications without exposing private content on the lock screen.

### Study preferences

- [ ] Default rows per page.
- [ ] Default quiz difficulty.
- [ ] Comfortable/compact density.
- [ ] Auto-advance after answering.
- [ ] Sound-effects preference and accessible sound/vibration behavior.
- [ ] Apply preferences consistently to lists and all three test modes.

Preferences are optional; reliable defaults/all-three-mode consistency take priority.

## P3 — later authoring, moderation, and administration

Contributor/operator features need not block learning; retain existing tools and use web for management where appropriate.

### Question management and bulk data access

- [ ] Add batch question fetch support (`POST /questions/batch/`) where master exams or large selections need it.

### Question authoring/editor

- [ ] Add Markdown formatting controls for question/explanation editing; retain the existing preview.

Django automatically updates the last-revised date on substantive edits.

- [ ] Optional explicit last-revised date editing for author workflows that require it.

Completed rich editing, image management and association workflows are in `workDone.md`.

- [ ] Optional direct camera capture alongside existing system file picker.
- [ ] Optional process-persistent editor/image-selection drafts. Current unsaved authoring drafts survive configuration changes in ViewModels, not process death.

### Verification, ratings, and feedback

Completed verification, report triage and bulk-tag workflows are in `workDone.md`; these are not missing feature families.

Batch ratings in moderation lists are implemented; see `workDone.md`. Additional learner-list summaries are optional.
Moderator detail metadata review and supported fields are implemented; see `workDone.md`.

### Master-exam author/admin workflows

- [ ] Create, view, edit, and delete master exams with delete-mode selection.
- [ ] Add existing bank questions, remove questions, and reorder them.
- [ ] Create/edit/remove embedded draft questions.
- [ ] Draft-question library with search, pagination, edit, and delete.
- [ ] Publish, cancel, publish questions to the bank, and preview an exam.
- [ ] Results dashboard for authorized exam owners/admins.
- [ ] Results summary and participant-by-question matrix.
- [ ] Download/share summary CSV and matrix CSV using Android's storage/share APIs.
- [ ] Gate every operation with the `master_exams.*` capabilities used by Vue and re-check server authorization errors.

Required Django API family: `/exam/master/**`.

### Knowledge-object management

- [ ] Paginated/filterable knowledge-object list.
- [ ] Create knowledge objects with title, learning objective, canonical answer, key facts, and category relationships supported by Django.
- [ ] Edit and delete knowledge objects.
- [ ] Show linked questions and navigate between an object and its questions.

Required endpoints: `/questions/knowledge-objects/` and `/questions/knowledge-objects/{id}/`.

### Clinical-case management

- [ ] Extend existing case list/search into management as needed; the read API supports a capped result set, not pagination.
- [ ] Rename/update a case title.
- [ ] Delete a case with clear consequences for linked questions.

Required endpoints: `/questions/cases/`, `/questions/cases/{key}/`, and `/questions/case/{key}/stem/`.

### Users and live sessions

Completed account tools and the separately gated five-minute activity snapshot are logged in `workDone.md`. The backend provides no device/session inventory or revocation endpoint; do not promise those from the activity snapshot.

- [ ] Generated temporary-password reset with secure, explicit result handling.
- [ ] User deletion with admin-password confirmation and backend delete options.
- [ ] Account expiry presets/date clarity and stronger numeric/date validation.
- [ ] Device/session inventory and revocation if corresponding backend support is added.

### Verification and moderation administration

- [ ] Verification statistics dashboard (`GET /analytics/verification-stats/`).

A resolved-report archive or additional pending-report status/search filters would need a separate backend contract. Completed pending-report triage is in `workDone.md`.

### Tag administration

- [ ] Hierarchical tag tree (`GET /questions/admin/tags/tree/`).
- [ ] Rename a tag across questions.
- [ ] Delete a tag with impact confirmation.
- [ ] Merge multiple source tags into a target tag.

### Blueprint administration

- [ ] Blueprint detail.
- [ ] Create, edit, activate/deactivate, and delete blueprints.
- [ ] Structured category/difficulty allocation editing and validation.

Required endpoints: `/exam/blueprints/` and `/exam/blueprints/{id}/`.

### Group administration

- [ ] Admin group list/search/pagination and detail.
- [ ] Create, edit, and delete groups.
- [ ] Add multiple users to a group.
- [ ] Remove individual members.
- [ ] Show membership and leaderboard visibility coherently in group detail.

Required endpoints: `/study/admin/groups/**`.

### Other administrative actions

- [ ] Refresh author ranks (`POST /admin/refresh-author-ranks/`).
- [ ] Seed sample questions (`POST /admin/seed-sample-questions/`) for authorized development/demo environments, or explicitly classify it as web-only.
Permission user search/selection and server capability grouping/role labels are implemented; see `workDone.md`.

## P4 — later additions and optional mobile operations

### Database operations and import/export

- [ ] Database information/counts/status.
- [ ] Data-quality report and server-side repair action.
- [ ] Create and list backups.
- [ ] Restore a selected backup with explicit high-risk confirmation.
- [ ] Clear database content with explicit high-risk confirmation.
- [ ] Simple file import.
- [ ] Telegram-format import and author mapping/conflict resolution.
- [ ] Export questions in every format Django exposes, including verified-only variants.
- [ ] PDF export with filters and Android download/share handling.
- [ ] Export and import complete application state.
- [ ] Display long-running export/import progress, validation errors, conflict details, and safe retry behavior.

Required endpoints: `/database/**`. These are valid mobile parity targets only if administrators are expected to operate the system from Android; otherwise mark the whole section explicitly web-only after a product decision.

### Full help/legal content and application information

- [ ] Full user manual equivalent to Vue's Arabic and English manual.
- [ ] Privacy policy equivalent to Vue's public `/privacy` content.
- [ ] Add help/privacy/support links and additional build information to About if desired. Description, version name, minimum Android and security notes exist.
- [ ] Add an Android-native not-found/deep-link fallback if external/deep links are introduced.

Full web docs/optional About remain additional scope; basic pages: `workDone.md`.

### Extended platform features

- [ ] Replace raw IDs and free-text values with pickers/search/autocomplete where Vue supplies structured controls.
- [ ] Add pull-to-refresh where stale server data is likely.
- [ ] Extend native file handling to CSV, JSON/state, import and PDF workflows; existing question-image picker/upload is logged in `workDone.md`.
- [ ] Add deep links only after authentication/capability redirects are defined.
- [ ] Consider offline question downloads and queued answer synchronization only after the online study/recovery path is reliable and an offline policy is defined.

## Endpoint coverage reference — not a delivery priority

Implemented API coverage is logged in [workDone.md](2026-10-09-workDone.md).

### Missing from Android

- [ ] Question batch and batch rating reads
- [ ] Knowledge-object CRUD
- [ ] Clinical-case list/detail/update/stem/delete
- [ ] Master-exam authoring/draft-library/publication/privileged results and CSV; learner participation endpoints are implemented
- [ ] Blueprint detail/create/update/delete
- [ ] Standalone streak and activity heatmap
- [ ] Group admin CRUD and membership endpoints
- [ ] Admin user deletion and generated temporary-password reset
- [ ] Verification stats, tag tree/rename/delete/merge
- [ ] Database info, quality, backup/list/restore/clear, import variants, export variants, and state import/export
- [ ] Refresh-author-ranks and seed-sample-questions actions

## Later target — full web/API parity

Android can be called functionally aligned when:

- Every applicable Vue route has an Android destination or an explicit, documented web-only decision.
- Every applicable Django endpoint is represented by a typed Android contract and reachable capability-gated workflow.
- Member, moderator, and administrator accounts see the correct navigation and are rejected safely if they reach an unauthorized action.
- Personal history never broadens into all-user history implicitly.
- Question and master-exam authoring preserve all domain fields without silent data loss.
- File, PDF, CSV, image, import, and export flows work through Android-native storage/share mechanisms.
- Arabic/English, RTL/LTR, accessibility, process restoration, error states, and offline behavior have been validated on the supported Android range.

This is a later completeness target, separate from the study-ready gate above.


## Inventory correction — 2026-10-09

The historical unchecked tag tree/rename/delete/merge, standalone knowledge management, case title/delete, and planner process-recovery entries are now implemented in source. Earlier inventories also predate completed master exams, users/groups/blueprints/operator tools, fonts/themes/preferences and native transfers; they are not an active missing-feature list. Use workPlan/workDone and the manual matrix. Generic additional polish requires a concrete device finding, not another implementation queue.
