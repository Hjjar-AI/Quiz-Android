> Historical snapshot before the 2026-10-10 documentation reorganization. Current status: [documentation index](../../README.md). Do not use unchecked items here as the current queue.

# Android / Vue / Django source comparison

Reviewed 2026-10-09. This is a production-source review of route coverage plus selected UI, service, DTO, serializer and authorization paths. It is not an exhaustive audit of every function, a build, HTTP verification or device approval. Historical findings are in [the archived review](../2026-10-09-functionalityReview.md).

## Follow-up implementation status

The subsequent [cross-cutting review](../reviews/2026-10-09-finalCrossCuttingReview.md) found six additional recovery, error-contract, freshness and category-control gaps. Its findings 1–6 and the selected restart/concurrent-edit follow-ups are now addressed in source; remaining work and verification are in [workPlan.md](../../workPlan.md); the completed scope below is the earlier bounded comparison.

All six concrete gaps from the 2026-10-09 bounded source review are implemented in source:

- Independent analytics report loading/retries, account-bound 7/30/90-day selection and collapsible full daily values.
- Library batch rating summaries/guarded rating writes and author-rank cards.
- Active-user trend with independent `admin.active_users` gate, matching summary period and separate read/error/retry state; `analytics.view_all` summary fallback remains supported by Django.
- All five admin reports use bilingual curated labels, localized counts/percentages/week dates/duration ranges, accessible paged rows and retained duration totals/average. Every returned tag row is accessible; backend author-report top-20 default is stated explicitly. Advanced report time windows remain their backend defaults and are labeled separately from the summary selector.
- Manual export selections can move up/down/remove using current stable IDs, preserve order across formats and remain guarded during export.

These changes complete the review's identified scope, not an exhaustive guarantee of Vue equivalence or defect-free runtime behavior. See [workDone.md](../../workDone.md) and [manualVerification.md](../../verification/manualVerification.md).

## Current workflow coverage

“Represented” means reachable Android source exists, not that every option or failure path works on a device. Coverage was checked against [Vue routes](../../../../frontend/src/router/index.js), [Vue endpoints](../../../../frontend/src/services/api/endpoints.js), [Android route wiring](../../../app/src/main/java/com/mukhtabir/android/ui/MukhtabirApp.kt) and current Django app routes.

| Family | Android source coverage / relevant limit |
| --- | --- |
| Authentication, profile, password and server selection | Represented; actual origin/cookie/CSRF/TLS/account-expiry integration pending. |
| Ordinary study/exam/recall, results/history | Represented, including pause/resume and encrypted uncertain Finish recovery; exact recovery requires matching deployed source/schema and legacy History fallback. |
| Library, bookmarks, mistakes, fragile and smart review | Represented, including filters, translations, question/case/image context and practice; library card ratings/ranks are now represented. |
| Knowledge map, reference details and cases | Represented; capped case/knowledge management queries are backend limits, not full server pagination. |
| Planner, member groups, streak/activity | Represented, including encrypted planner recovery and group periods/visibility. |
| Learner master exams | Represented: acknowledgement, saved attempts, grace timing, navigation/flags/results and encrypted recovery. |
| Question authoring | Represented: rich fields/translations, exact tags, images/camera, cases/knowledge, Markdown and encrypted draft recovery. |
| Moderator tools | Represented: single/bulk verification, notes/tags, report resolution, rating summaries and verification statistics. |
| User/permission administration | Represented: search, roles/overrides, activation/expiry/reset/delete and current activity; narrow permission lookup needs matching backend. |
| Category/tag/group/blueprint management | Represented. Blueprint weights are category weights; backend does not persist difficulty allocation. |
| Standalone knowledge/case management | Represented; fresh preflight is not atomic revision protection. New management drafts/uncertainty markers are memory-only. |
| Master-exam management | Represented: composition/audience/co-authors, embedded/private drafts, preview/lifecycle, results/breakdowns/matrix and CSV. Reviewed result UI keys match current backend result payloads. |
| Imports/exports/operator tools | Represented: simple/Telegram/state import, JSON/CSV/XLSX/PDF/state export, backup/restore/clear, quality flags and seed/rank tools. Export reorder remains above; operator actions were not executed. |
| Themes/preferences/help/navigation | Represented: drawer sections, appearance controls, bilingual content, fonts/density and study defaults. Visual/RTL/accessibility approval remains pending. |
| Native offline/reminders/session inventory | Native source exists beyond Vue's main route catalog; matching backend/session engine, receipt schema and device integration pending. |

## Finding evidence

- Analytics: [Android UI/load logic](../../../app/src/main/java/com/mukhtabir/android/ui/analytics/AnalyticsScreen.kt), [Android service](../../../app/src/main/java/com/mukhtabir/android/data/remote/FeatureServices.kt), [current activity snapshot](../../../app/src/main/java/com/mukhtabir/android/ui/admin/ActiveAccountsScreen.kt), [Vue UI](../../../../frontend/src/features/analytics/views/Analytics.vue), [Vue store](../../../../frontend/src/stores/analyticsStore.js), [Django gates](../../../../backend/apps/analytics/views.py). Android advanced reports exist; the gaps are their presentation, independent loading and exposed controls.
- Export ordering: [Android selection/request](../../../app/src/main/java/com/mukhtabir/android/ui/transfer/TransferScreen.kt), [Vue reorder controls](../../../../frontend/src/features/admin/components/ManualExportQuestions.vue), [Django export serializer](../../../../backend/apps/database/serializers.py).
- Library cards: [Android list/detail](../../../app/src/main/java/com/mukhtabir/android/ui/questions/QuestionLibraryScreen.kt), [Android question DTO](../../../app/src/main/java/com/mukhtabir/android/data/remote/FeatureModels.kt), [Vue question card](../../../../frontend/src/features/questions/components/QuestionCard.vue), [Django read serializer](../../../../backend/apps/questions/serializers/question_read.py).
- Management cross-check: [native content contracts](../../../app/src/main/java/com/mukhtabir/android/data/remote/ContentManagementService.kt), [native ownership/preflight](../../../app/src/main/java/com/mukhtabir/android/ui/admin/ContentManagementViewModel.kt), [knowledge endpoint](../../../../backend/apps/questions/views/knowledge_object_views.py), [case endpoints](../../../../backend/apps/questions/views/case_views.py), [native results UI](../../../app/src/main/java/com/mukhtabir/android/ui/masterexams/ExamReviewScreen.kt), [Django result payload](../../../../backend/apps/master_exams/services/master_exam_results_service.py), [Vue results](../../../../frontend/src/features/masterExams/views/MasterExamResults.vue).

## Separate deployment/runtime limits

The recorded read-only local SQLite schema check does not verify a phone-selected remote database or its running backend. Older results can legitimately lack source-session identity. Backend knowledge/permission/session additions need matching running source. No migration inspection/work, suites, compilation/builds, versions, deployment or destructive operator actions occurred in this review. Follow [remaining plan](../../workPlan.md) and [manual verification](../../verification/manualVerification.md).
