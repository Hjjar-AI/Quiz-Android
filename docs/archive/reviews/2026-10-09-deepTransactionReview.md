> Historical source review; findings may have subsequent fixes. Use [current status](../../workDone.md) and [remaining work](../../workPlan.md) before acting on them.

# Deeper transaction and lifecycle review — 2026-10-09

Focused source review of ordinary/master session services and callers, offline completion, learning evidence, imports, planner configuration and web polling. Previous seven fixes remain implemented. At review time there were no production changes, builds, compilation, suites, migration inspection/work or database actions. The closure below records the subsequent source fixes. Scenarios below are source-evidenced; actual timing, database deadlock behavior and device/browser symptoms require live verification.

## 1. P1: knowledge update and learning use opposite lock order

[Knowledge serializer update](../../../../backend/apps/questions/serializers/knowledge_object.py#L73) locks KnowledgeObject before saving. The [model hook](../../../../backend/apps/questions/models.py#L314) then invalidates linked state and [updates Question counters](../../../../backend/apps/learning/evidence.py#L57), acquiring those row locks. Learning completion and question editing instead lock Question first and then [case/knowledge rows](../../../../backend/apps/learning/evidence.py#L12).

Interleaving: completion holds Q and waits for K; knowledge edit holds K and waits for Q. A server database can detect this cycle and roll back one request, producing an ordinary save/completion failure. The same parent-first pattern in direct model saves needs auditing; the corrected case HTTP/delete flows already attempt question-first order.

Fix boundary: a consistent dependency lock order for knowledge content mutation, model hooks and learning writers, including imports/admin writers. Verify with the real database; SQLite source checks cannot certify this.

## 2. P1: imports reuse local optimistic versions

[State question import](../../../../backend/apps/questions/services/importing/state_import/apply.py#L608) assigns entry.version to an existing Question; [knowledge import](../../../../backend/apps/questions/services/importing/state_import/apply.py#L414) includes imported version among the fields assigned to an existing object. They can preserve or lower the local version while changing content.

Example: an administrator opens version 5, imports changed content carrying version 5, then submits the old form with expected_version=5. The ordinary edit precondition still passes and overwrites the import. Lowering and later reusing a version also allows an older form to match a different content revision. Newly created imported rows and existing-row replacement must be distinguished.

Fix boundary: monotonically advance the current local revision for every existing-row import mutation, including assessed relationship updates; keep portable/source version metadata separate if needed. Preserve safe overwrite/merge policies and existing schema invariants.

## 3. P1: master normal-attempt resume can create a makeup attempt

[Start service](../../../../backend/apps/master_exams/services/master_exam_attempt_service/start.py#L88) classifies a request as makeup from current wall time before checking for an existing normal attempt. The makeup branch rejects a completed normal attempt but permits an unfinished one, creating a second attempt with a new start/deadline. It also rejects any existing makeup attempt instead of returning an unfinished one, so an identical lost-start retry cannot resume it through POST.

Example: start normally just before closes_at, then reopen/start just after closes_at with allow_makeup=true. A still-valid or not-yet-swept normal attempt can be replaced in latest-attempt lookup by the new makeup attempt. Native discovery/status reconciliation reduces this in its usual path, but the backend POST remains reachable; web start/recovery is weaker.

Fix boundary: resolve/resume existing attempts before classifying a new start, force-finish expired attempts consistently and apply makeup eligibility only to a genuine new attempt. Preserve unique attempt rules and explicitly define whether completed/expired normal attempts ever permit makeup.

## 4. P1: master access checks precede the locked exam

[Start view](../../../../backend/apps/master_exams/views/attempt_views.py#L94) checks audience/manage/preview against its earlier exam instance. [Start service](../../../../backend/apps/master_exams/services/master_exam_attempt_service/start.py#L56) refreshes and locks the exam but checks status/time, not current audience/manage access; preview returns before that lock.

An audience/co-attending removal between the view check and attempt creation/preview construction can leave a newly unauthorized participant starting an attempt or obtaining private preview content. User/Exam locks alone do not validate the audience relation, including group-membership changes that use separate writers.

Fix boundary: check current access for new attempts/preview within the same locking/visibility policy as their creation. Treat access to already-owned attempts separately; do not silently erase legitimate learner work. Audit membership writer serialization as part of that boundary.

## 5. P1: web master answer response loss still rolls back locally

[Web answer handler](../../../../frontend/src/stores/masterExamAttemptStore.js#L330) reverts optimistic answers on all errors, including timeouts/5xx after a server commit. It has no persistent uncertain-answer marker or freeze/reconciliation flow equivalent to [native master answer recovery](../../../app/src/main/java/com/mukhtabir/android/ui/masterexams/MasterExamsViewModel.kt#L256). [Polling](../../../../frontend/src/stores/masterExamAttemptStore.js#L421) updates position/completion but does not merge server answers, so counts and selection can remain wrong after response loss.

The [backend answer writer](../../../../backend/apps/master_exams/services/master_exam_attempt_service/answer.py#L113) serializes writes but unconditionally replaces a question's answer/confidence/timestamp. Two tabs/devices, or a retry after an uncertain response, can replace a saved answer without an expected-slot precondition. Native preflight narrows uncertainty but cannot close the interleaving between read and POST.

Fix boundary: compare retained expected answer-slot state under lock, distinguish rejection from unknown outcome in the web client, reconcile full status and preserve draft intent. Permit explicit answer changes only after the saved baseline is established; local in-flight maps are not cross-device concurrency control.

## 6. P2: master navigation writes stale rows

[current_question](../../../../backend/apps/master_exams/services/master_exam_attempt_service/question_flow.py#L24) can save current_question_id while serving GET. [goto_question](../../../../backend/apps/master_exams/services/master_exam_attempt_service/question_flow.py#L81) saves a pre-fetched attempt without locking or rechecking completion/deadline. Answer and finish services do lock their rows.

A delayed GET/goto can therefore overwrite position computed by a newer answer, or mutate navigation after finish passed the earlier view check. Browser previous/next controls can run alongside answer work; another device bypasses any local busy guard. Results need not be regraded for this to cause wrong question/progress UI.

Fix boundary: serialize navigation with answer/finish, use fresh membership/completion/time checks and an appropriate navigation-state precondition. Prefer a read-only GET when a derived fallback position need not be persisted.

## 7. P2: polling can outlive its view and overwrite another attempt

[useAutoRefresh](../../../../frontend/src/composables/useAutoRefresh.js#L49) stops only its interval on unmount; it has neither disposal generation nor request cancellation. An in-flight rejection then runs restartInterval in the catch branch; a successful completion after previous backoff can also restart it. This can create an orphan timer after unmount or visibility stop.

[Master polling/current-question callbacks](../../../../frontend/src/stores/masterExamAttemptStore.js#L421) do not capture examId/sessionId/store generation. The recent account generation guard protects account switches, but does not protect moving between exams as the same user. A late exam A status/question response can modify exam B's store; is_complete can trigger B's results navigation. The runner's leave guard allows an approved leave and does not count background polling as an active mutation.

Fix boundary: disposal/visibility generations and cancellation for polling, plus store context guards keyed to exam/attempt session and reset generation. Scope in-flight answer maps and cleanup by that identity too, and guard navigation from unmounted runners.

## 8. P2: planner configuration has no concurrency/scope epoch

[Planner update](../../../../backend/apps/planning/views.py#L103) sends a full replacement with no expected revision. [Service](../../../../backend/apps/planning/services.py#L42) serializes row writes, but two device editors can silently replace one another's target/filter/date configuration. Native read/preflight/recovery does not make the server write conditional.

There is also a late-learning boundary: changing scope resets today's ledger to zero, with the comment that earlier activity belongs to the earlier plan; [record_learning_progress](../../../../backend/apps/planning/services.py#L102) applies current categories/tags/date window to backdated offline answers. No scope effective time/history distinguishes an earlier plan from the current one. A delayed upload after a scope change can credit an old event to the new scope when it matches current filters, including today after an explicit reset.

Fix boundary: planner revision preconditions in both clients/server and an explicit scope-epoch rule for delayed evidence. Preserve historical days and activity; avoid retroactively reinterpreting them under a new plan. Decide/store the intended earlier-scope attribution rather than inferring it from current filters.

## Existing protections traced

Ordinary sessions use learner/session locks, frozen grading data, answer-slot preconditions and durable first-attempt learning markers. Offline completion verifies caller-bound signed packs, full answer sets, occurrence/answer time bounds, current visibility/content and durable completion-body receipts before recording a first completion; already committed identical receipts survive subsequent content changes. Master answer choice ceilings follow frozen snapshots, and completion grading is transactional/idempotent. These are verified source paths, not runtime certification.

Recommended order: lock order and import revisions; attempt start/access/resume; master answer/navigation recovery; polling lifetime/context; planner revisions and historical scope attribution. Broader performance, runtime accessibility, database setup and previously documented PostgreSQL administration limits remain separate.

## 2026-10-10 eight deeper findings fixed in source

All eight bounded findings are addressed in production source; the original review remains historical evidence.

- Assessed parent saves lock linked questions before parent rows; knowledge/question/tag writers and state imports coordinate their dependency locks. Existing imported questions and knowledge objects advance the locked local revision instead of adopting the source revision. Imports deliberately take broad user/question locks; large-import contention still needs measurement.
- Master start locks fresh access relations, rechecks new-start/preview access, resumes either active normal or makeup attempts before new-start classification, and finishes expired attempts without granting extra time. Existing owned work is retained.
- Master answers require session_id and the full expected_slot (including null), compared under lock. Identical saved intent is idempotent; conflicting saved intent returns ATTEMPT_PROGRESS_CHANGED. Navigation requires session_id and expected_current_question_id; current-question reads do not persist fallback navigation. Expiry finish failures propagate.
- Web/native master recovery retains uncertain drafts, reads authoritative full answer slots, and requires explicit comparison/rebase/discard before another conflicting write. Web polling has disposal/cancellation generations and exam/session/store/progress ownership guards; route changes cannot adopt an old attempt response.
- Planner update/delete requires expected_id and expected_version; successful updates return accepted configuration/revision. Both clients retain editor baselines and provide explicit conflict resolution. StudyPlannerScope/ScopeDay preserve timestamp-based historical filter attribution: explicit filter/date changes reset today's displayed progress, taxonomy rename/merge continues it, and historical days include all their original scope activity after midnight without double counting. Global learning events remain intact.

Verification is production AST, JavaScript/extracted Vue script syntax, locale JSON, native lexical/XML/resource/placeholder and whitespace checking only. No builds, compilation, automated suites, migration inspection/work, setup/seeding, database writes, deployment or versions. Matching backend/web/native rollout and matching planner schema are required. Actual HTTP, database interleavings, browser/device recovery and accessibility remain release checks.
