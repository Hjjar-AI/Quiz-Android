> Historical source review. All listed findings are now source-complete; see the closure below and [current status](../../workDone.md).

# Android parity review — 2026-10-10

Current production-source comparison with Vue and Django. [Feature map](../../reference/functionalityReview.md) · [Implementation queue](../../workPlan.md) · [Manual matrix](../../verification/manualVerification.md).

## Assessment and method

Every major production Vue route family has a native counterpart. The remaining evidenced gaps are inside existing screens, rather than wholly missing learner/admin modules. Route presence alone does not establish parity.

Reviewed Vue route/component/controller/service paths against native navigation, screens/ViewModels, DTOs/transports and relevant Django serializers/services/views. Compared reachable options, request fields, permission/ownership gates, retained state, read refresh and write recovery. Excluded actual automated suites and migrations; no build, database, HTTP/device/browser or destructive operator actions. This is a broad source comparison, not exhaustive certification of every branch or platform behavior.

## Confirmed native implementation gaps

| ID / priority | Function | Gap and observable consequence |
| --- | --- | --- |
| A1 / P1 | Knowledge map freshness | No normal Refresh control or resume refresh. After studying a concept and returning to the retained map, its mastery/status/coverage can remain the earlier values. |
| A2 / P1 | Planner weekly progress | Renders the latest seven stored dates, not the last seven calendar days. Sparse practice can show old activity and omit zero days, unlike Vue's weekly view. |
| A3 / P2 | Master preview | Native preview reads questions and reveals correct answers; it cannot select an answer/confidence, retain rehearsal answers or show immediate correctness as Vue's preview runner does. |
| A4 / P2 | Master results freshness | Results are initial/manual reads only. Vue refreshes active exam results every 30 seconds; an open native results screen does not follow new answers/finishes automatically. |
| A5 / P2 | Knowledge-map information | Native omits started-object summary, attempted/total variant counts and average-confidence label already provided by Django and shown by Vue. Category labels exist, but Vue's grouped overview does not. |
| A6 / P3 | Admin user overview | Native user list is name/role/active only; web shows contributions/latest exam accuracy/last login. Native DTO drops the backend's latest_exam_accuracy/latest_exam_tag fields. |
| A7 / P3 | Batch account activation | Web has selected-user batch actions; native manages one account at a time. A safe batch desired-state workflow is missing. The web implementation itself uses toggles and must not be copied blindly. |

### A1: refresh retained knowledge-map data

[Native loader](../../../app/src/main/java/com/mukhtabir/android/ui/knowledge/KnowledgeMapScreen.kt#L77) runs at ViewModel initialization; the [route](../../../app/src/main/java/com/mukhtabir/android/ui/knowledge/KnowledgeMapScreen.kt#L113) exposes retry only when there is an error, with no `RefreshOnResume` or normal Refresh action. Other learner lists already use [the shared lifecycle hook](../../../app/src/main/java/com/mukhtabir/android/ui/components/RefreshOnResume.kt). [Vue map](../../../../frontend/src/features/knowledge/views/KnowledgeMap.vue) reloads on mounted route entry.

Reproduction: open map → start concept study → answer/finish → return to the still-retained map. Add lifecycle/explicit refresh preserving the selected status and readable data on failure. Device reproduction remains pending.

### A2: show seven calendar days in the planner

[Native progress rendering](../../../app/src/main/java/com/mukhtabir/android/ui/planner/PlannerScreen.kt#L491) sorts the response dictionary and takes seven entries. Django's [daily_progress](../../../../backend/apps/planning/serializers.py#L59) is a sparse 90-day ledger; it does not promise seven consecutive dates. [Vue weekDays](../../../../frontend/src/features/planner/views/StudyPlanner.vue#L124) constructs today-minus-six through today and defaults missing counts to zero.

Example: activity on October 1 and 10 should show October 4–10 with zero days; native instead shows both recorded dates. Build the calendar window from the returned planner `today`, localize dates, fill zeros and keep scope-history totals intact. Vue currently anchors its calendar to browser time, so the server's `today` is the stronger reference.

### A3: interactive preview rehearsal

[Native preview branch](../../../app/src/main/java/com/mukhtabir/android/ui/masterexams/ExamReviewScreen.kt#L151) offers previous/next and reveal/hide, but no answer/confidence state. [Vue preview submit](../../../../frontend/src/stores/masterExamAttemptStore.js#L288) stores local answers/confidence and generates correctness/explanation feedback; its [runner](../../../../frontend/src/features/masterExams/composables/useMasterExamRunner.js) supports navigation and explicit finish back to editing. Vue preview does not create a graded participant attempt or a final persisted score.

Use the existing backend preview snapshot and manage/preview ownership gates for native rehearsal; keep answers local and never call participant answer/finish endpoints for it. Static preview already exists and must be preserved.

### A4: live author results

[Native results ViewModel](../../../app/src/main/java/com/mukhtabir/android/ui/masterexams/ExamReviewScreen.kt#L41) loads initially and through [manual Refresh](../../../app/src/main/java/com/mukhtabir/android/ui/masterexams/ExamReviewScreen.kt#L144). No live/resume scheduler is present. [Vue results polling](../../../../frontend/src/features/masterExams/views/MasterExamResults.vue#L326) refreshes while the exam is active every 30 seconds.

Add foreground/view-scoped read refresh, stale-response cancellation and permission rechecks; retain readable results on transient failure. Avoid repeatedly discarding an already loaded matrix during background updates. Existing result breakdowns, paging and CSV are implemented.

### A5: complete knowledge-map metrics

[Native card](../../../app/src/main/java/com/mukhtabir/android/ui/knowledge/KnowledgeMapScreen.kt#L166) shows mastery, coverage percentage and attempts; [summary](../../../app/src/main/java/com/mukhtabir/android/ui/knowledge/KnowledgeMapScreen.kt#L131) shows mastered/total/average. [Native DTOs](../../../app/src/main/java/com/mukhtabir/android/data/remote/ExtendedModels.kt#L17) already decode startedObjects, attemptedQuestions, questionsCount and averageConfidence. [Vue cards/summary](../../../../frontend/src/features/knowledge/views/KnowledgeMap.vue#L78) use these fields, supplied by [Django map](../../../../backend/apps/learning/knowledge_map.py).

Add the omitted metrics without inferring unattempted confidence as zero. Category grouping is lower-priority presentation parity; category names already appear natively.

### A6: administrative user performance context

[Vue user cards](../../../../frontend/src/features/admin/views/Users.vue#L80) display question count, latest-exam accuracy and last login. [Backend enrichment](../../../../backend/apps/users/views/admin_user_views.py#L121) adds latest_exam_accuracy/latest_exam_tag to list/detail responses. [Native UserDto](../../../app/src/main/java/com/mukhtabir/android/data/remote/ApiModels.kt#L50) has contributions/login but no latest exam fields; [native list](../../../app/src/main/java/com/mukhtabir/android/ui/admin/AdminScreens.kt#L218) renders basic identity/role/activation only.

Add nullable fields and readable account-card context under existing admin.users policy; preserve absent scores and keep private user details out of permission-only lookup responses.

### A7: safe selected-user account actions

[Vue batch toolbar](../../../../frontend/src/features/admin/views/Users.vue#L28) invokes [bulkToggle](../../../../frontend/src/features/admin/views/Users.vue#L343). [Native account list/editor](../../../app/src/main/java/com/mukhtabir/android/ui/admin/AdminScreens.kt#L172) and [account tools](../../../app/src/main/java/com/mukhtabir/android/ui/admin/AdminAccountScreen.kt) have single-user actions only. Native account create/edit/reset/toggle/delete already exist.

Important reference limitation: Vue ignores its requested active state while iterating single-user toggle calls. Mixed selections can activate an inactive account under “Deactivate” or vice versa, and response loss cannot be retried safely as desired state. Backend currently exposes the toggle path. Define an explicit desired-state backend contract or reconcile/fresh-read each selected account before deliberate action; independently enforce admin.users/admin-password/self/admin restrictions, partial outcomes and unknown writes. This gap needs coordinated work, not a native copy of the web loop.

## Smaller workflow differences

These are convenience gaps; underlying functionality exists:

- **Master reorder shortcuts:** [Vue move-top/move-bottom](../../../../frontend/src/features/masterExams/views/MasterExamEditor.vue#L216) versus [native up/down](../../../app/src/main/java/com/mukhtabir/android/ui/masterexams/MasterExamAuthorScreens.kt#L240). Native [moveQuestion](../../../app/src/main/java/com/mukhtabir/android/ui/masterexams/MasterExamEditorViewModel.kt#L621) can reorder, but the screen only offers adjacent moves. Long lists take repeated server mutations.
- **Recent authoring choices / reopen last question:** Vue [recent choices](../../../../frontend/src/features/questions/composables/useQuestionSave.js#L75) prioritize recent categories/tags and [EditView](../../../../frontend/src/features/questions/views/EditView.vue#L55) records a last-question shortcut. Native [library](../../../app/src/main/java/com/mukhtabir/android/ui/questions/QuestionLibraryScreen.kt) retains filters/page/selection, but has no corresponding recent/last-question controls. A native shortcut should open readable detail under current permission, not blindly reproduce Vue's edit-only destination.
- **Users → that user's permissions:** [Vue user action](../../../../frontend/src/features/admin/views/Users.vue#L120) opens overrides for the selected ID. Native users/account tools do not forward a target into [PermissionsRoute](../../../app/src/main/java/com/mukhtabir/android/ui/admin/PermissionsScreen.kt); the user must search again. Permission editing itself exists and has its independent gate.
- **Results → publish to bank / verification → export:** native exposes these through management/transfers instead of directly on the corresponding result/statistics page. These are navigation shortcuts, not absent lifecycle/export implementations. Keep backend manage-plus-publish_to_bank and admin.database export gates; statistics-only permission does not authorize exporting the bank.

## Shared/reference defects found while tracing

Not Android omissions:

1. [Web initial server-offset read](../../../../frontend/src/stores/masterExamAttemptStore.js#L221) references `signal` without a parameter/local declaration. Its catch suppresses the resulting failure, leaving initial clock correction undone until a later status poll; native derives deadlines from server_now. Any fix should capture attempt identity and avoid old-offset writes after route changes.
2. Web batch activation's toggle semantics are described under A7. Do not equate a rendered button with correctly implemented behavior.

## Coverage checked and intentional boundaries

| Family | Result of this source comparison |
| --- | --- |
| Auth/profile/server/preferences | Native login/CSRF, password/expiry, refresh, manual/hidden three-file selection, same-server retained files, fonts/themes/language/density represented. |
| Ordinary study/exam/recall | Source selection, refinements/strategies/count, saved sessions, answers/confidence/reflection/pre-answer, navigation/timers, encrypted recovery/results represented. |
| Library/bookmarks/mistakes/fragile | Filters/paging/selections, rating/rank feedback, explicit bookmarks and practice represented; convenience differences above. |
| Knowledge/cases/planner/groups | Browsing/detail/context, management, learner paging, scope/revision recovery and 7/30/90 leaderboards/visibility represented; A1/A2/A5 remain. |
| Analytics | Independent reports/retries/periods, full daily values, distinct/due/mastery metrics and active-user gates represented. Differences in chart layout do not establish missing data. |
| Authoring/moderation | Rich choices/translations/source/case/knowledge/image/camera/Markdown, recovery/revisions, single/bulk verification/tagging/report/rating paths represented. |
| Accounts/permissions/taxonomy | Native user lifecycle, role/individual overrides, category icons/tags/groups/blueprints/content tools represented; A6/A7 and shortcuts remain. |
| Master participation/management | Start/resume/grace/acknowledgement/recovery, composition/audience/drafts/lifecycle/results/matrix/CSV represented; A3/A4 remain. |
| Transfer/operator/help | Import/state analysis/author mappings/conflicts, filters/manual order/PDF options/share, backups/quality/clear/rank/seed and full help/legal content represented. Destructive operations unexecuted. |
| Native additions | Encrypted offline full/selected bank, explicit synchronization, reminders and own-session controls exist beyond main web routes. |

Management cases/knowledge and learner cases now have full server pagination; bounded association autocomplete is intentional and is not the old management gap. Full-bank download means accessible offline study, not an admin backup; per-user permission and selected-ID budgets are deliberate. Blueprint difficulty allocation and master choice shuffling are not fully implemented backend behavior and cannot be claimed as native-only omissions. PostgreSQL administration remains unsupported backend scope. Runtime accessibility, compilation/schema, HTTP integration and database concurrency remain separate release gates.

## Next implementation order

A1/A2 learner correctness → A3/A4 master parity → A5 metrics → A6/A7 administration → convenience controls. Shared web findings need separate coordinated corrections. No implementation fixes were applied during this review; the current queue records these findings.

## Source closure — 2026-10-10

A1–A7, the smaller workflow differences and both shared web defects are addressed in source:

- Knowledge map refreshes on resume and through Refresh while retaining filter/readable data on failure; shows started concepts, variant counts, confidence and category grouping.
- Native/web weekly planner uses server `today` and seven consecutive calendar dates with zero-day entries.
- Native preview retains memory-only answer/confidence rehearsal and correctness/explanation, alongside static reveal; refresh/leave is explicit and no participant answer/finish write is sent.
- Native active results read every 30 seconds only in the resumed view; leaving/backgrounding cancels reads. Matrix remains available with its last-load timestamp; confirmed publication survives later read failure.
- Account cards show contributions, nullable latest exam score/tag and last login. Both clients use requested activation state; native has selected-user actions, partial counts and explicit read/review for unknown outcomes. Backend reuses canonical locks/reauth/capability/self/stub/last-admin rules and retains legacy toggle compatibility.
- Top/bottom reorder uses one revisioned composition request. Recent categories/tags and last-question identity use existing encrypted account/server storage. Users/account tools open independently gated target overrides; verified-statistics export and result bank-publication shortcuts keep their distinct gates and recovery.
- Web initial clock read captures account/attempt generation and uses separate read status, with no undefined signal or overwrite of accepted start success.

Production AST, JavaScript/extracted Vue syntax, JSON, native lexical/XML/resource/placeholder and whitespace checks only. Runtime symptoms, compilation, HTTP/device/browser/accessibility and database concurrency remain pending. No builds, suites, migration/setup/database/operator actions, deployment or versions. This batch adds no schema fields; reload the matching backend before using new set-state clients, since older backends reject the dedicated route without a mutation. Earlier schema requirements remain separate. See the current work plan/manual matrix for release gates.
