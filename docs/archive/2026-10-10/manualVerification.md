> Historical snapshot before the 2026-10-10 documentation reorganization. Current status: [documentation index](../../README.md). Do not use unchecked items here as the current queue.

# Android pending manual verification

Updated 2026-10-09. None of these scenarios is marked passed by source presence. Build and test-suite work require explicit authorization. The user will perform debug builds. Detailed interruption cases and prior checklists remain in [the archived matrix](../2026-10-09-manualVerification.md).

| Area | Device / live-backend checks |
| --- | --- |
| Installed revision/server | Build the current revision manually; verify editable LAN address, selected-origin HTTP, private HTTPS/user CA, redirects, timeout and offline errors. |
| Auth/isolation | Login CSRF/rotation/first mutation, expiry/password change/logout, switching user/server, encrypted draft binding, unreadable/full storage and denied restoration. |
| Ordinary learning | Study/exam/recall selection/navigation/answers/reveal/timer/pause/resume/finish; lost answers/Finish responses, process death/cold launch, exact identity recovery and old-backend/legacy History fallback. |
| Browsing/context | Combined filters/pages/selections, bookmarks/mistakes/fragile/smart review, translations/choice indices, cases/images/source/knowledge, frozen History/results and closed-book answer hiding. |
| Master participation | Visibility/acknowledgement, audience/time/grace/makeup, frozen questions, flags, interrupted answers/navigation/Finish, restored attempt/result and explicit recovery review. |
| Planner/member groups | Scope/dates/progress/delete, exact comma-bearing tags, encrypted restore/discard/keep-and-leave, lost saves read first, pending-marker autosave ordering; 7/30/90 leaderboards/rank/visibility. |
| Authoring | Own versus any gates, fresh revision conflicts, explicit clears, tags/translations/source, case/knowledge associations, Markdown, camera/image upload/remove and encrypted drafts. |
| Moderation | Independent gates, stable-ID ratings, bulk verify/notes/tags/report resolution, stale selections, uncertain writes and confirmed-save refresh failure; verification statistics/exports. |
| Accounts/permissions | Permission-only search versus admin.users, inactive/non-stub paging, roles/overrides, revoked access, expiry inputs, reset secret retention and password-confirmed deletion restrictions. |
| Taxonomy/content/groups | Deep tag trees/slash/comma names, counts and merge/delete impacts/protected planners; knowledge/case caps/local paging/linked-question scope, owner/version/count conflicts and separate title/stem writes; group memberships/blueprint weights. |
| Master management | Private/shared draft editing/delete rejection, audience/co-authors/composition/version, preview isolation, publish/cancel/bank publication, results-only accounts and matrix/CSV agreement. |
| Files/PDF/operators | Import mapping/conflicts, selection/order/visibility, verified-only, save/share, fresh Android PDF forward/back links and Arabic layout. Backup/restore/clear/seed require deliberate safe operator execution, not routine review. |
| Offline/notices/sessions | Pack privacy/closed-book guards, immutable completion retries/receipt identity and logout cleanup; reminders/timezones/permission denial, own-session revocation and unsupported signed-cookie engines. |
| Accessibility/presentation | English/Arabic/RTL, narrow/large text/TalkBack/focus, drawer/back/collapsed controls, themes/density/reduced motion, long titles, charts, keyboard and active-session protections. |

The [source review](../../reference/functionalityReview.md) lists known UI gaps; exercising those does not turn absent controls into implemented features. Verify matching running backend and actual schema before attributing an integration failure to Android. Do not automatically replay uncertain ordinary/admin writes.

## Normal-user follow-up checks

- Switch analytics 7/30/90 rapidly, rotate/reopen, change account/server; verify request period and retained-data period label. Fail summary, mastery, streak or one privileged report independently: other successful reports remain readable and Retry targets only that report.
- Compare every daily trend/streak value with backend data; check folded sections, RTL, TalkBack and large-text wrapping.
- Browse/filter/page library while batch ratings load; confirm bounded page requests, author-rank labels, no fabricated zero ratings on failure and usable questions when ratings fail. Return from detail after a changed rating and refresh.
- Rating selection/save: repeated taps, confirmed POST plus failed GET, rejected POST, lost POST response, fresh read/explicit review before retry, inaccessible question and page/account changes. No automatic POST replay; new rating drafts are memory-only.

## Admin analytics and export follow-up checks

- Accounts with only admin.active_users, only analytics.view_all, both or neither: verify requests/visible trend gates separately from the active-account snapshot. Rapid period changes, failed reads and revoked capabilities must preserve correct periods and remove authoritatively denied content.
- All five reports in Arabic/English/RTL/large text/TalkBack: counts, percentages, week dates, duration buckets, total sessions/average and explicit default windows. More than 20 cohort rows and tag rows remain reachable; paging clamps when data shrinks and independent retries do not replace another report's state. Author top-20 is a server bound, not a claim to list every author.
- Manual question selections: first/last boundaries, rapid up/down taps, remove/reselect, search/page/format switches, failed exports and in-flight changes. Verify actual PDF/Excel/CSV/JSON file order matches displayed positions and backend numbering, including RTL.

## Connection-file and alternate-address checks

- Fresh install: manual URL reaches address confirmation; exported JSON reaches file-based confirmation without rendering its address; HTTP warning and private-CA/hostname rules remain in force. English/Arabic/RTL/large text/TalkBack, including labeled same-server checkbox and native picker/saver.
- Android and web admin.settings-only permission: export a current and an alternate phone-accessible address, cancel saver, denied/read-only/provider failure, rotate/process recreation while choosing a file. Confirm no backend mutation and matching imported address. Partial failed export may need manual removal from its chosen destination.
- Import canceled, unreadable, malformed/unsupported JSON, missing fields, >64 KiB, credentials/query/fragment/disallowed paths; entered address and all local downloads/recovery remain intact. Different decorative identities for the same URL do not change profile.
- With encrypted downloads, pending completions and drafts: change to an alternate address of the same backend with **Same server, different address**, sign in to the same account and verify retained content. No automatic write/synchronization replay. Failed login must retain files and deny cached offline access until authenticated binding; a different account must not access them.
- Change to a genuinely different backend without the checkbox: previous files stay encrypted and unavailable; sign in to matching account after returning to the prior address. Same user numeric ID on another backend must not expose the prior profile. Existing pre-profile installations keep legacy recovery on their original address. Two known distinct profiles reject silent merging.
- Cookies/reminders/old ViewModels must be revoked for any changed address; provoke late old-origin HTTP failures while saving and verify they do not delete archived state. Explicit logout/expiry/account change retain existing cleanup behavior. Validate task restart and storage-save failure recovery on-device.

## Hidden imported-address checks

- Import a file at first launch and through Change server: neither manual input nor confirmation, Settings or Android admin-export prefill shows the imported address/IP. TalkBack/selection must not expose it through hidden controls. HTTP warnings still appear when applicable.
- Save, rotate, kill/relaunch and reopen Change server/Settings: generic file-mode status persists. Upload another file updates the hidden selection; invalid/canceled/unreadable replacement retains it. Switching to manual entry starts blank and leaves the saved configuration/local files intact until a new address is confirmed.
- Confirm a manually entered replacement shows only the new user-entered address and saves manual presentation mode. Reimporting a file for the same normalized URL changes presentation mode without deleting recovery/downloads or signing out unnecessarily. Alternate-address profile selection remains explicit and non-destructive.

## Three saved connection-file checks

- Fill slots 1–3, restart/rotate and open Settings: numbered entries persist with no address or filename in visual/TalkBack text. Select each from Settings and confirm it through the shared setup screen. Current/selected statuses remain distinct, including identical URLs in different files.
- Existing single imported configuration appears in slot 1; add slots 2/3 and verify the original survives registry initialization. Export files with different decorative identities or alternate IPs for the same backend; verify normalized address/profile rules and explicit same-server selection.
- A fourth import requires choosing a slot to replace; other slots remain intact. Cancel picker, malformed/oversized/unreadable JSON and storage-commit failure preserve prior registry/selection/active server. Canceling setup after a successful import retains the stored connection but does not activate it.
- Replace the active slot without confirming: active endpoint/sign-in remain unchanged. Confirm a different endpoint: sign-in/reminders revoke and encrypted private files remain in their profile. Return to known endpoints and reauthenticate to recover matching-account data; no automatic uncertain-write replay.
- Native picker rotation/process recreation retains the requested slot; storage/read failure and large text/RTL/TalkBack preserve meaningful numbered controls and errors. Original JSON documents are never modified/deleted and can be removed after successful import without breaking stored connections.

## Full-bank download and encrypted-file storage checks

- Matching backend/Android: all caller-visible public and own-draft questions are enumerated once by keyset within the initial upper ID bound. `tests.start` denial/expiry and private drafts belonging to another account remain authoritative. Closed-book ordinary/master questions are excluded both at catalogue and pack generation; begin an exam between requests to exercise rechecks.
- Download a bank larger than five packs/8 MiB with supported question images, translations and clinical cases. Verify local reading/first-answer feedback/confidence/progress/finish and explicit synchronization, plus low-memory scrolling and payload release when switching batches. No accounts/admin data are downloaded.
- Oversized batches adapt to smaller read-only requests. A single oversized image/material or unready/unavailable question is counted as excluded without hiding other content. Verify downloaded/initial/skipped counters, empty bank, all-protected bank, 400/404/409 content rejection, 403/401/5xx/timeout and storage capacity stops. A finished partial pass must not claim all questions were downloaded.
- Pause during catalogue fetch, pack response, encrypted write and checkpoint commit; rotate/kill/relaunch and explicitly resume. Check no lost/duplicated committed batches, no automatic HTTP resume and no automatic answer/completion replay. Change/add/delete/restrict content mid-pass: existing snapshots remain readable; new IDs beyond the initial bound await a new pass.
- Legacy encrypted preferences migrate only after file/index commit succeeds, preserving answers, index, tokens, UUID/frozen completion body and confirmed sync results. Test unreadable/removed individual pack files, Keystore/storage errors, full disk and failed index commit; keep prior readable packs and uncertain changes, then exercise Storage Retry and deliberate deletion.
- Fresh full-bank download after a completed pass adds fresh packs and preserves prior answers; explicitly deleting a pack removes it regardless of historical download counters. Logout/expiry/different account clear private access/files; alternate-server profiles retain their own downloads/checkpoint behind reauthentication. Confirm no plaintext temp files and no Android cloud/device backup of pack payloads/index.

## Full-bank per-user capability checks

- A member (or explicitly denied moderator) with tests.start but no tests.download_full_bank: no bulk Start/Resume; direct catalogue/marked bulk pack requests return 403. Admin grants/denies an individual through native/web Permissions and saves; Refresh download permission applies the change without logout/data deletion. Test role grants, explicit per-user false overriding a granted role, Inherit and the immutable all-capabilities admin policy.
- Revoke during catalogue/pack download: the next authoritative request stops with 403 and blocks Resume. Retain all prior packs/answers/checkpoints. Regrant and refresh explicitly to resume from the saved checkpoint. Deny/lose the permission read: bulk actions stay unavailable but cached content remains readable; local-only cold launch makes no permission request.
- Missing tests.start must still deny catalogue/bulk download even if tests.download_full_bank is granted. Selected-question download and completion synchronization retain tests.start behavior. Verify both English/Arabic permission descriptions and independent handler/button guards, including a confirmation open while permission changes.

- Fresh/fallback moderator role grants full-bank download by default; explicit per-user false still denies it. Existing saved moderator role settings remain authoritative and can enable the capability through Permissions → Roles without resetting other grants.

## Metadata/profile/structured-error follow-up

- Rename/delete tags/categories or modify/deactivate blueprints, then return to library/quiz setup: options refresh, removed selections reconcile and valid filters/requested settings survive. Refresh failure keeps readable options and exposes retry. Switch selections during reads and start a session during refresh; no stale callback may reset session/questions/answers or recovery.
- Foreground/navigate/explicit Profile refresh and confirmed self-admin changes: user identity, rank, expiry and capabilities refresh. Fail profile reads, overlap account/login/logout and refresh requests, revoke permissions during editing or active exams: retain drafts/session, defer navigation changes, and rely on backend denial. Verify new grants after leaving protected workflows, without logout/private-file deletion.
- Return varied/nested field validation and ACCOUNT_EXPIRED/MUST_CHANGE_PASSWORD reasons from ordinary requests and file downloads. Common labels and all safe messages should render in English/Arabic/RTL/large text/TalkBack; unknown diagnostics and oversize/deep values must not appear. CSRF matching remains bounded; no failed mutation automatically replays.
- Repeated automatic category/tag/bookmark/blueprint starts: choose unseen eligible questions before repeats, then least frequent/oldest exposures; insufficient pools still supply the available count. Compare two users independently. Exact selected IDs and SRS order stay unchanged. Verify blueprint quotas, ties, sparse/legacy results, paused/master allocations, offline synchronized history and concurrent starts against matching backend.

## Learning consistency follow-up — 2026-10-09

- Prepare matching fresh schema, then rebuild/reload the coordinated sources before device checks.
- Check coverage/balanced/review selection, blueprint quotas and case grouping; manual IDs/SRS/fixed master sets retain order/contracts.
- Study offline before and after online practice; sync in reverse order/across midnight and with a changed device clock. Confirm current SRS state, occurrence-day credit and unchanged pending UUID/body on failure/retry.
- Deny full-bank permission and try more than 200 distinct selected IDs across batches; retry existing IDs and revoke a previous full-bank grant. Keep all encrypted local files.
- Lose bookmark/create/duplicate responses; read state/receipt and explicitly retry the same request. Confirm one creation, retained image/text intent, account isolation and encrypted new-question recovery. Detail duplicate/Vue pending UUIDs do not survive process/page destruction.
- Race question/knowledge/group edits; confirm 409 and retained drafts. Edit translated options and check structural errors/provenance.
- Answer then pause/discard: planner, streak and activity agree on answered learning; verify distinct/due/mastery metrics and archived master weighted results in History.

## Requested recovery/category/concurrent-edit follow-up

- [ ] Select every category icon, preserve unfamiliar imported values, reopen after save/restart; confirm visual/text selection with RTL, TalkBack and large text.
- [ ] Restart with unsaved category/knowledge/case/tag/stem/settings/report drafts, and with pending question copies/bookmarks. Restore explicitly; verify no mutation fires on startup/restore.
- [ ] Interrupt each mutation after server commit but before response; check receipts/current state, retain confirmed success if later reads fail, and use only explicit same-identity retries where offered. Cover deleted/missing results and revoked permissions.
- [ ] Fail encrypted reads/writes/clears (including capacity); verify requests do not dispatch without checkpoints, controls do not stay busy, memory drafts/success remain, and Retry preserves the latest state.
- [ ] Edit categories/cases/tags/settings in two clients; reject stale versions, preserve draft/selection, show latest data, and apply only the explicitly reviewed revision. Repeat on the actual server database.
- [ ] Switch accounts (including reused numeric IDs) and server profiles; validate private isolation. Switch alternate addresses/files of the explicitly selected same server and verify existing offline files survive.
- [ ] Restart Vue with unknown create/duplicate outcomes: retain identity only; check/open the old result without attaching new text/images; explicit missing-receipt acknowledgement enables new creation. Logout/account changes must isolate recovery.

## Full content-management browsing

- [ ] Browse beyond 100 cases and 500 knowledge objects with Next/Previous; verify totals and stable ordering for identical titles, and permission-scoped case visibility.
- [ ] Change search/status/category from a later page: successful search starts on page 1; failed search/page requests preserve previous items/page and typed filters. Retry uses the failed request's exact page/filters.
- [ ] Delete the last item on the final page and verify refresh moves to the remaining last page; repeat while another client deletes items.
- [ ] Open/edit/restore/save/close a draft from a later page: pagination/re-entry never discards it or replays uncertain writes. Check RTL/large text/TalkBack page controls.
- [ ] Verify existing Vue and native autocomplete requests without page/per_page retain their bounded legacy response shape.

## Seven rescan fixes

- [ ] Delay authenticated reads/writes and 401 responses across logout/login/account switches. Old completions, shared promises, redirects and recovery storage must not affect the new account.
- [ ] Lose a question-create response, leave the form, then reopen and choose new text/images. The old receipt must be check/open-only; late confirmation must not apply the new imagery or navigate the unmounted form. Explicit same-form retry still uses its frozen body/identity.
- [ ] Change/import question ownership between initial API lookup and locked mutation; reject the former owner. Modify questions/knowledge after delete confirmation/preflight; reject stale versions and retain displayed items/drafts. Protected master questions must still reject deletion.
- [ ] Commit a category/knowledge create, lose its response, then delete that result. Acknowledge its terminal receipt while retaining the draft; only an explicit new Save may issue a new UUID. Absent receipt, permission denial and transient read failure remain distinct.
- [ ] Produce category/tag/Settings conflicts in two web clients. Compare latest values, retain input through failed refresh, and exercise Keep draft/Use server, removed sources/targets, renamed merge targets and revoked permissions.
- [ ] Browse learner case results beyond 50 across pages; fail a later-page/new-search request, retry exact filters/page and preserve old results. Verify configured page sizes, Arabic/RTL, large text and TalkBack.

## 2026-10-10 deeper consistency release scenarios

- Lose master answer responses before/after commit; compare full saved slot, keep/rebase/discard draft explicitly, and verify no automatic resend. Change the same answer/confidence on two devices and verify 409 without overwriting either draft. Check legacy native draft recovery.
- Race answer/goto/current-question/finish and expiry; stale navigation must reject and GET must not change position. Resume normal attempts after closes_at and retry makeup starts: no second attempt or extended timer. Remove audience/co-attending/group membership during new start/preview; verify fresh authorization while existing owned work remains readable.
- Change exam routes as the same user while reads/writes complete; hide/show/unmount during polling backoff. Old responses must not replace the new attempt or restart orphan timers/navigation.
- Edit/delete/recreate planners on two clients; verify retained id/revision conflicts and explicit Keep draft/Use server. Lose accepted update responses and fail post-save reads; preserve confirmed configuration and drafts.
- Record offline answers before/after filter/date resets, then upload late; original scope gets credit, today's reset does not regain old activity. Rename/merge planner tags: preserve today's progress without copying counts. Cross midnight after a reset: historical daily totals include pre-reset activity exactly once. Preserve global heatmap/learning evidence.
- Import replacements carrying lower/equal source revisions, then save old editors: stale local revisions reject. Exercise parent edits, learning completion, tag merge and import on the deployed server database for lock cycles, rollback and large-import contention.
- Verify fresh planner schema and coordinated API rollout, bilingual/RTL/large-text/focus/TalkBack controls. These are pending manual checks, not completed runtime evidence.
