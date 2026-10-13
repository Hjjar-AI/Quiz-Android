> Historical snapshot before the 2026-10-10 documentation reorganization. Current status: [documentation index](../../README.md). Do not use unchecked items here as the current queue.

# Android remaining work

Updated 2026-10-09. Current source comparison: [functionalityReview.md](../../reference/functionalityReview.md). Completed work: [workDone.md](../../workDone.md). Detailed prior plan: [archive](../2026-10-09-workPlan.md).

## Implementation queue

Priority meanings: P0 evidenced failures; P1 learner workflows; P2 contributor/moderator workflows; P3 administration; P4 operator/optional parity. This review found no wholly absent P2 management family, which does not establish defect-free behavior.

The earlier six bounded parity gaps are implemented in source. The broader [cross-cutting review](../reviews/2026-10-09-finalCrossCuttingReview.md) identified this additional queue:

- P0: explicit bookmark state and question create/duplication receipts/reconciliation are implemented in source; verify response loss, explicit same-identity retries and encrypted new-question restoration. Detail duplication and Vue pending identities now have explicit restart recovery; verify restoration and account/server isolation.
- P1: structured validation errors, safe profile refresh and library/quiz metadata refresh are implemented in source; verify read failures, revoked permissions, retained drafts/sessions and removed selections on-device.
- P2/P3: category icons, encrypted standalone editor restoration and feedback/category/settings write recovery are implemented in source. Verify storage failures, process death and explicit outcome/conflict review.

Source presence is not runtime approval. Full evidence and recommended boundaries are in the linked review.

The identified normal-user analytics and library-card gaps are implemented in source; see [workDone.md](../../workDone.md). Device/build verification remains pending.

Management, master-exam authoring, tags/content CRUD and planner recovery are represented in source. Do not recreate them from stale unchecked historical lists. Current review is bounded, not proof that no other defect exists.

## Release and verification gates

- User performs manual debug builds. The latest supplied build failed; its reported Kotlin error causes are corrected in source and await a user rebuild. No agent build is authorized by supplied logs.
- Verify manual/file-import phone backend selection, three numbered imported-file slots/Settings switching/replacement, imported-address hiding across setup/confirmation/Settings/restart and Android admin file export, alternate-address same-server profiles/retained encrypted recovery, chosen HTTP/private HTTPS origin, login CSRF/cookies/expiry and account/server isolation.
- Reload matching backend for permission lookup, linked knowledge questions, exact result recovery, offline receipts and own-session endpoints. Recorded local SQLite structural readiness does not establish readiness of another database; any setup/migration work needs explicit authorization.
- Verify per-user tests.download_full_bank grants/denials, fresh download-permission reads and revoked in-flight bulk requests without deleting cached files; ordinary selected downloads remain tests.start.
- Verify full-bank download/catalogue visibility and closed-book protection, encrypted-file legacy recovery, pause/process-death resume, partial exclusions, disk-full/commit errors and explicit completion synchronization; see [offline bank contract](../../contracts/offlineQuestionBank.md).
- Run the [manual matrix](../../verification/manualVerification.md): learner flow/recovery, permissions/ownership, interrupted writes, PDFs, RTL/large text/TalkBack and destructive operator scenarios only when explicitly chosen.

Source completion, deployment and device validation are separate. Keep drafts/readable results on failure; never automatically replay uncertain writes. Follow [agent rules](../../../Agents.md); no suites, migrations, builds or version changes without explicit request.

## 2026-10-09 learning review integration

Implemented native selection strategies, frozen offline occurrence/answer times, richer learning metrics, archived master weighted scores, explicit bookmark state, question write-receipt recovery and knowledge/group revision payloads. Matching [backend schema/contracts](../../../../backend/docs/contracts/learningConsistency.md) are required. Verify clocks/late uploads, grants/revocation, mixed-language option validation, response-loss recovery, conflicts and large-text/RTL/accessibility on a rebuilt app. Source checks do not establish compilation/device behavior.

The four requested recovery/category/concurrent-edit scopes are source-complete; see [work log](../../workDone.md). Matching clients/backend schema and the manual release matrix remain required.

Full Android case/knowledge management pagination is implemented in source. Verify large result sets, page failures/retry, searches and deleted final pages with matching backend/app; see the latest [work log](../../workDone.md).

## 2026-10-09 project rescan follow-up

The [new source review](../reviews/2026-10-09-projectRescan.md) identifies seven additional bounded gaps: web account-generation guards; orphan pending-create/form association; transactional question ownership checks; knowledge-delete revisions; terminal/deleted receipt recovery; web conflict resolution; learner case pagination/read retention. All seven are now addressed in source; remaining checks are runtime release gates.

The seven rescan findings are source-complete. Validate coordinated DELETE/receipt contracts, account-switch cancellation, conflict review, orphan-save recovery and learner paging; see the latest [work log](../../workDone.md).

## 2026-10-09 deeper transaction/lifecycle findings

The [deeper source review](../reviews/2026-10-09-deepTransactionReview.md) records eight new follow-up areas: knowledge/learning lock order, import revision reuse, master attempt resume/makeup and fresh access gates, master answer/navigation concurrency and web uncertainty handling, polling disposal/attempt scope, and planner revision/scope epochs. All eight findings are now source-complete; verify coordinated contracts, fresh planner schema and live interleavings. See the latest work log.
