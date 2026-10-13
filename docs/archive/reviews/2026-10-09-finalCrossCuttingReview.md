> Historical source review; findings may have subsequent fixes. Use [current status](../../workDone.md) and [remaining work](../../workPlan.md) before acting on them.

# Final cross-cutting source review

Reviewed 2026-10-09 against current Android, Vue and Django production source. Six additional findings were recorded after the earlier bounded parity implementation. Findings 1–5 are now addressed in source by the follow-ups below; device/live-backend verification remains pending. Source review is not runtime approval.

## Findings

### 1. High: uncertain bookmark toggles can reverse a committed change

[Android question-detail handler](../../../app/src/main/java/com/mukhtabir/android/ui/questions/QuestionLibraryScreen.kt#L672) clears its busy guard after an exception and permits another toggle without reading membership. [Django toggle](../../../../backend/apps/feedback/services.py) creates or deletes membership: losing a successful response and tapping again can undo the intended change. [Vue bookmark store](../../../../frontend/src/stores/bookmarkStore.js) also restores optimistic state on failure and permits another toggle. Native bookmark-list removal already has stronger reconciliation.

Required: block another uncertain toggle until membership is read and the user can choose the next action. Do not automatically replay the existing toggle.

### 2. High: uncertain duplication can create another copy

[Android detail mutation](../../../app/src/main/java/com/mukhtabir/android/ui/questions/QuestionLibraryScreen.kt#L692) stores an error and clears `mutationBusy` after a lost duplicate response. Another duplicate is allowed. [Django duplication](../../../../backend/apps/questions/services/question_service.py) creates a new question each time, without an operation receipt for exact recovery.

Required: retain an uncertain-create marker and require read/manual candidate review before another creation. Busy protection alone does not cover response loss.

Related older handlers need a consistent uncertainty-policy pass: [detail ratings/reports](../../../app/src/main/java/com/mukhtabir/android/ui/questions/QuestionFeedback.kt), [category management](../../../app/src/main/java/com/mukhtabir/android/ui/categories/CategoriesScreen.kt) and [admin settings](../../../app/src/main/java/com/mukhtabir/android/ui/settings/SettingsScreen.kt). Their exception paths re-enable writes without the recovery handling used by newer flows. Absolute-value updates do not inherently create duplicates, but still need accurate outcome messaging and protection against overwriting intervening edits.

### 3. Medium: structured backend errors are discarded

[Android transport](../../../app/src/main/java/com/mukhtabir/android/data/remote/ApiClient.kt#L362) retains only status/message in `ApiException`, although [the envelope](../../../app/src/main/java/com/mukhtabir/android/data/remote/ApiModels.kt) supports details. Django field-validation details and semantic reasons consequently do not reach screens. [Vue transport](../../../../frontend/src/services/api/client.js#L176) retains message/code/details.

Required: preserve safe structured errors across request/download paths, render known field errors and handle known session/password reasons. Do not expose arbitrary diagnostics. This is contract loss, not proof of a particular device authentication failure.

### 4. Medium: authenticated profile/capabilities become stale

[AuthViewModel](../../../app/src/main/java/com/mukhtabir/android/ui/auth/AuthViewModel.kt#L99) has a refresh method, but its explicit caller is forced-password completion; ordinary signed-in navigation has no refresh integration. [App navigation/actions](../../../app/src/main/java/com/mukhtabir/android/ui/MukhtabirApp.kt) use the retained user. Role/override, rank and expiry changes can therefore remain absent until authentication restoration/login. Revoked actions can remain visible and receive backend denials; newly granted actions can stay hidden. Django remains authoritative.

Required: safe foreground/explicit profile refresh and refresh after relevant self-administration changes. Preserve drafts/active sessions and handle destinations whose capability changed.

### 5. Medium: management writes leave dependent selectors stale

[Tag management](../../../app/src/main/java/com/mukhtabir/android/ui/admin/TagManagementScreen.kt#L134) increments the question-bank revision, but [library refresh](../../../app/src/main/java/com/mukhtabir/android/ui/questions/QuestionLibraryScreen.kt#L247) reloads questions without cached tag/category options. Renamed/merged/deleted tags can leave old filter labels or identifiers selected. Category and group/blueprint routes lack dependent quiz-metadata invalidation; [quiz setup](../../../app/src/main/java/com/mukhtabir/android/ui/quiz/QuizScreen.kt#L144) initializes only when not already loaded. Returning to retained setup can show old choices until explicit reload.

Required: invalidate affected metadata after confirmed writes or refresh selectors on re-entry. Reconcile removed selections, preserve valid drafts/selections and avoid resetting an active exam.

### 6. Low: category icon selection is missing

[Vue category form](../../../../frontend/src/features/categories/components/CategoryForm.vue#L44) and [Django endpoints](../../../../backend/apps/questions/views/category_views.py) support icon selection/update. [Android form](../../../app/src/main/java/com/mukhtabir/android/ui/categories/CategoriesScreen.kt#L83) exposes name, description and color only.

Required: a localized picker using the existing icon registry, retaining the current icon. This does not mean Android resets icons: the encoder omits default request fields and Django preserves an omitted update icon.

## Coverage and limits

Inventory covered 132 Django app URL declarations across 11 modules, 54 Vue route declarations and 142 Android production Kotlin files. Selected contract, ownership, state and navigation tracing followed; inventory is not exhaustive verification of every function/failure path.

| Traced area | Source conclusion |
| --- | --- |
| Auth transport | Current CSRF/session-cookie conventions align; writes have no automatic transport retry. Phone origin/TLS/session behavior is pending. |
| Ordinary study/exam/recall | Expected index/slot, frozen context and caller-owned exact Finish/result recovery are represented. |
| Offline completion/own sessions | Reviewed DTO/endpoint identities align; deployed schema/session engine and interruption behavior remain pending. |
| Question/knowledge/case ownership | Native gates follow backend ownership fields and linked-question case policy; Django is authoritative. |
| Master authoring/results | Reviewed expected versions, supported draft fields and result keys align. |
| Analytics | Independent active-user gate, report fields/windows and duration totals match current contracts. |
| Groups/blueprints | Member fields/category weights align; backend has no persisted difficulty allocation. |
| Transfer | Ordered question IDs and current formats are represented; PDF interaction needs viewer/device verification. |
| Learner library/planner | New rating/analytics/planner paths have stronger recovery or independent reads; this does not cover older handlers above. |

Backend query caps, legacy results without source identity and database-specific operator restrictions are platform limits. Local SQLite readiness does not establish readiness of the phone-selected backend.

Review only: no production changes, builds/compilation, suites, migrations, versions, deployment or destructive actions. See [remaining work](../../workPlan.md) and [runtime matrix](../../verification/manualVerification.md). Findings 1–2 need response-loss verification after implementation; 3–5 need account/capability/retained-navigation validation.

## 2026-10-09 follow-up for findings 3–5

- Transport keeps bounded allowlisted serializer details/recognized auth reasons on ordinary and download exceptions. Shared error presentation labels common fields in both languages; handlers using exception.message retain the additional messages without raw diagnostics. No uncertain mutation replay was added.
- Profile refresh runs on foreground/navigation, once per minute while resumed, and throttled confirmed-write signals, with explicit Profile refresh and immediate refresh signals after 403. Refresh reads check the returned account ID without rebinding private storage; failure retains the current profile. Navigation-changing user updates defer during protected editor/write/ordinary/master workflows and apply after leaving them; backend request gates remain authoritative.
- Library options refresh after bank revisions and on re-entry, reconciling removed category/tag filters and filter-dialog drafts while retaining valid selection and readable options on failure. Quiz refreshes metadata independently of session initialization on re-entry, with separate retained options/error handling and reconciliation of selected categories/tags/active blueprints. Active sessions/recovery are not reset. Fresh blueprint permission is applied without recreating the Quiz ViewModel.

These changes address the bounded source findings, not a guarantee of all selector/profile UI behavior. Source/AST/XML/resource/lexical/whitespace checks only; no builds, suites, migrations or runtime verification. Findings 1–2, the older-handler uncertainty pass and category icons remain unfinished.

## 2026-10-09 learning-review recovery follow-up

Findings 1–2 are addressed in source: explicit bookmark GET/PUT and retained desired state; question create/duplicate receipts, read reconciliation and explicitly initiated same-identity retries. Android new-question identity is encrypted with the existing draft; detail duplication and Vue pending state remain memory-scoped. See [backend implementation and limits](../../../../backend/docs/contracts/learningConsistency.md). Other older feedback/category/settings uncertainty handlers and category icons remain separate work. No runtime/device/build verification is claimed.

## 2026-10-09 requested follow-up closure

Category icon selection, encrypted standalone editor/detail-operation restoration, the older native feedback/category/settings uncertainty pass and category/case/tag/settings revision checks are implemented in source. This supersedes the historical missing/memory-only descriptions above. Vue pending question identities now have account/server-scoped restart checks without persisted draft text/images. See [work log](../../workDone.md) for scope and limits; no compilation/device/HTTP/server-database concurrency verification is claimed.
