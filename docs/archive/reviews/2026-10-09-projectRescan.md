> Historical source review; findings may have subsequent fixes. Use [current status](../../workDone.md) and [remaining work](../../workPlan.md) before acting on them.

# Project source rescan — 2026-10-09

This pass inventories production source across Android/Django/Vue and traces reachable content CRUD, pagination, recovery, transport/account resets, conflict UI and ownership/import boundaries. It excludes suites/migrations, builds, database actions and runtime verification. It is not an exhaustive correctness certificate for every module. Production code is unchanged by this review.

## P1 — web responses and shared requests survive account resets

[Shared request keys](../../../../frontend/src/services/api/client.js#L79) contain method/URL/body/options but no authenticated account/session generation. [Pending mutation sharing](../../../../frontend/src/services/api/client.js#L265) outlives [auth store resets](../../../../frontend/src/stores/authStore.js#L92). Question create/duplicate [callbacks](../../../../frontend/src/stores/questionStore.js#L174) do not supply the supported useCrudActions isCurrent guard; its default is true. A delayed account A response can repopulate account B's reset store and checkpoint through the newly rebound browser recovery key. An identical ordinary mutation from B can also join A's outstanding promise. Server authorization remains separate; the defect is stale client state/response sharing across accounts.

Use an account/session generation for request sharing and completion ownership, invalidate/cancel the former scope on reset, and ignore old callbacks before touching stores/recovery/navigation. Verify with delayed responses across logout/login and account switches.

## P1 — a new web form can finish an old pending question save

[hydrateWriteRecovery](../../../../frontend/src/stores/questionStore.js#L94) returns early once recoveryLoaded is true. After an uncertain create, leaving the form through its allowed discard confirmation and reopening AddView keeps pendingCreate in the store, but creates a fresh [memory-only save flow](../../../../frontend/src/features/questions/composables/useQuestionSave.js#L91). The [new form save](../../../../frontend/src/stores/questionStore.js#L181) reads the old receipt and returns its question. The flow then runs image completion using the new form's sentinels. Result: newly selected imagery can be attached to the previous question, while the new text was never saved. Full browser restart uses the dedicated restoredCreate flow and avoids this path; ordinary route re-entry does not.

Bind pending intent to its originating form/body. On re-entry treat every orphan pending create as check/open/acknowledge-only, independent of whether storage was already hydrated. Never attach current text/images to a recovered old result. Preserve same-form explicit retry of its frozen body.

## P1 — question authorization is checked before the transactional row

[Question update/delete views](../../../../backend/apps/questions/views/question_views.py#L235) authorize ownership against an earlier fetched row. [update_question](../../../../backend/apps/questions/services/question_service.py#L165) locks current content but does not recheck the acting user's current ownership; [delete_question](../../../../backend/apps/questions/services/question_service.py#L282) accepts no acting user and fetches/deletes separately. [State import](../../../../backend/apps/questions/services/importing/state_import/apply.py#L599) can replace owned_by and an explicitly supplied version. An intervening import/ownership change after the view check can leave the former owner authorized by stale data; deletion has no version comparison at all. Version protection alone does not replace ownership validation.

Recheck actual ownership/override against the locked row inside the service transaction. Pass acting user explicitly for deletion, preserve visible 403/404 behavior, and enforce a retained delete revision where appropriate. Runtime interleaving has not been exercised.

## P2 — knowledge deletion lacks a revision precondition

[Knowledge DELETE](../../../../backend/apps/questions/views/knowledge_object_views.py#L100) deletes by ID/management permission without expected_version. Android's [preflight and delete](../../../app/src/main/java/com/mukhtabir/android/ui/admin/ContentManagementViewModel.kt#L215) are separate requests; [transport](../../../app/src/main/java/com/mukhtabir/android/data/remote/ContentManagementService.kt#L48) and [Vue](../../../../frontend/src/services/knowledgeService.js#L17) send no revision. Another editor's update after confirmation/preflight can therefore be deleted silently. Knowledge updates and case/category deletes already have stronger preconditions.

Compare a retained expected_version under the deletion transaction/row lock, and align clients with explicit conflict review. Linked-question invariants must remain intact.

## P2 — deleted receipt targets can trap Android create recovery

[Content receipts](../../../../backend/apps/core/write_receipts.py#L24) retain a target ID after that target is deleted; create retry returns 404 rather than creating again. Android [knowledge reconciliation](../../../app/src/main/java/com/mukhtabir/android/ui/admin/ContentManagementViewModel.kt#L295) treats both missing receipt and missing target as receiptMissing, leaves reviewed false and offers another identical retry. That retry cannot succeed for a deleted committed target. Category [acknowledgement](../../../app/src/main/java/com/mukhtabir/android/ui/categories/CategoriesScreen.kt#L125) clears uncertainty/action but retains the old operation ID, so another save can reuse the exhausted identity. Restart/discard or cancel can escape some paths, but sacrifices the draft rather than providing a clear terminal outcome.

Distinguish absent operation, committed-but-deleted target, hidden target and read failure. Provide explicit acknowledgement/abandonment that preserves draft content while retiring the old identity; allocate a new identity only for an explicitly authorized new creation.

## P2 — web conflict resolution stops at the rejection

[Settings](../../../../frontend/src/features/admin/views/Settings.vue#L234), [category editor](../../../../frontend/src/features/categories/components/CategoryForm.vue#L140), and [tag dialogs](../../../../frontend/src/features/admin/views/Tags.vue#L186) send retained revisions correctly. But a 409 leaves the old revision, without latest-data comparison and Keep draft/Use server controls equivalent to Android. Settings [loadSettings](../../../../frontend/src/features/admin/views/Settings.vue#L208) replaces the draft and marks it clean; closing/reopening category/tag editors also resets their input. Retrying the stale revision repeats the rejection; refreshing/reopening loses unsaved edits unless manually copied.

Read current values separately, preserve draft/selection, recheck permissions and rebase only after explicit conflict resolution. Keep confirmed write outcomes independent of follow-up reads.

## P2 — learner case browsing is still capped and failed search clears results

Management pagination is implemented. The separate learner [case service](../../../app/src/main/java/com/mukhtabir/android/data/remote/FeatureServices.kt#L145) still sends limit=50; [learner screen](../../../app/src/main/java/com/mukhtabir/android/ui/knowledge/ClinicalCasesScreen.kt#L63) has no page traversal. Its [search start](../../../app/src/main/java/com/mukhtabir/android/ui/knowledge/ClinicalCasesScreen.kt#L60) clears prior data before the read succeeds, so a failed new search loses the readable previous list. This is independent of intentionally bounded autocomplete choices.

Use the existing page/per_page contract for learner browsing and commit applied query/page/results only after a successful read; retain typed search and failed-request retry separately.

## Remaining verification and intentional limits

The recent management pagination, native category controls, encrypted restoration and revision contracts are represented in source; this review does not reopen them as wholly missing features. Device/build/HTTP/schema checks, true server-database concurrency, large-bank measurements, RTL/TalkBack and browser accessibility remain pending. Autocomplete result caps, manual exact-ID selection semantics, offline access policy and unsupported PostgreSQL backup/restore administration are distinct existing policies/limits, not new defects proven by this pass.

## 2026-10-09 seven rescan findings fixed in source

- Web transport uses an independent authenticated-session generation for shared requests, cancellation, success/error handling and delayed expiry redirects. Old account requests are aborted/ignored; CRUD completion guards prevent old callbacks from repopulating reset stores. Login/initial restore can publish only their own matching one-step account transition. CSRF rotation remains separate.
- Web AddView marks an orphan pending create as review-only on route departure/re-entry even when storage was already hydrated. Confirmed late results keep their receipt identity for the new view. Save/image flows also check component lifetime/account generation, so an old response cannot consume a new form's images or navigate an unmounted form. Browser draft text/images remain memory-only.
- Question update/delete services recheck current ownership/override against locked rows. Question and knowledge DELETE now require expected_version in query parameters; native detail confirmation and all reachable Vue question-delete handlers capture the displayed version before confirmation. Knowledge native deletion uses its retained editor baseline. Protected relationships/reputation behavior remain, and stale/rejected deletes retain readable items.
- Caller-scoped content receipts report whether their committed target still exists. Native category/knowledge reconciliation distinguishes deleted targets from absent operations/read failures, offers a terminal acknowledgement, preserves the typed draft and retires the exhausted UUID. Only a subsequent explicit new save creates a new identity.
- Shared bilingual Vue revision-review controls load current data independently and expose Keep draft/Use server choices for categories, tags and Settings. Drafts stay intact during read failures; tag source/target identities rebase by ID only after explicit review. Removed categories can prepare a new creation without discarding draft content. Settings dirty-state baselines adopt the reviewed server values without replacing kept inputs. Permissions/account scope gate reads and resolution; no automatic replay.
- Learner case browsing now uses server page/per_page and the configured page size, with Next/Previous. Applied query/results commit only after successful reads; failed search/page requests retain readable results and retry the requested query/page. Management pagination and bounded autocomplete contracts remain intact.

Validation: production Python AST (242 files), JavaScript/extracted Vue script syntax (299), locale JSON, native lexical delimiters (152 files), bilingual XML/resources/positional placeholders (1,382 strings per locale) and CRLF-aware whitespace checks passed. These are source checks, not compilation/runtime evidence. No builds, suites, migration inspection/work, database/setup/data generation, deployment or version changes. Matching backend/native/web rollout is required for the new DELETE preconditions and receipt status; this follow-up introduces no model/schema changes. Verify delayed responses/account switches, orphan creates, deleted receipts, stale ownership/revisions, read failures and large learner case lists over actual HTTP/device/server-database concurrency.
