> Historical snapshot before the 2026-10-10 documentation reorganization. Current status: [documentation index](../../README.md). Do not use unchecked items here as the current queue.

# Normal-user launch readiness

Updated: 2026-10-09. Production-source review plus user-reported successful debug build; later warning edits, device, deployment and schema verification remain pending.

## Source review for user testing

Reviewed current Android production code against Django routes, views, serializers and services. No further concrete missing normal-user implementation was identified in the reviewed paths after the fixes below. This is a bounded source review, not proof that every runtime path works.

| Learner area | Source findings |
| --- | --- |
| Server entry, authentication and account | Manual editable URL, CSRF/login, restoration, expiry, forced/self-service password change and logout are wired. Fixed saved-session recognition for isolated SQLite cookie suffixes and rejected late response cookies after session cleanup. |
| Ordinary study/exam/recall | Answer/reveal rules, navigation, pause/resume, encrypted answer/Finish recovery and exact caller-owned completed-result lookup are wired. Uncertain Finish recovery reads the original identity without automatically resubmitting Finish. |
| Library, bookmarks and personal History | Existing browsing, filtering, selection/restoration and result review are reachable. Ordinary History requests the signed-in user's ID; broad History remains a separately gated route. |
| Planner and groups | Existing learner scope/progress and group/leaderboard reads are wired. Planner now keeps exact tag names as a list: a comma inside one tag no longer changes its meaning when saving another field. Pending tag input must be explicitly added. |
| Assigned exams | Learner acknowledgement/start/resume, timing/grace, guarded answer/Finish recovery and personal results are implemented separately from exam authoring/admin tools. |
| Offline practice | Caller/account-bound download, encrypted local progress, explicit immutable completion upload and durable backend receipts are implemented. Capacity rejection preserves existing packs; storage recovery does not automatically upload. |
| Notices, reminders and own sessions | Existing notice reads, opt-in local reminders and password-confirmed own-session revocation are wired. Actual alarm delivery and supported backend session-engine behavior need runtime checks. |
| Presentation | Supplied SVG source is preserved at [artwork/app_icon.svg](../../../artwork/app_icon.svg); its paths are used by legacy/adaptive/themed launcher and notification vectors. English/Arabic resources match. Actual rendering, RTL, large text and TalkBack remain phone checks. |

Static checks passed: all 14 main XML files, 1008 matching bilingual keys/placeholders, 998 referenced string names, duplicate imports across 124 production Kotlin files, lexical structure in 10 changed Kotlin files, and whitespace. These checks do not compile Kotlin or exercise HTTP/database/device behavior. No test suites or migration files were inspected or changed.

## Connection

Users enter the server manually before sign-in and can edit it from signed-in Settings or Login. Settings displays the current API address. No phone host is hardcoded by this pass. The existing editor normalizes an HTTP/HTTPS origin to `/api/v1/`, confirms changes, and clears sign-in, encrypted downloads/recovery and reminders when switching servers. See [server configuration](../../contracts/serverConfiguration.md) for listener, host, CSRF, cookie and certificate requirements.

## Remaining release gates

1. User reported `assembleDebug` succeeded on 2026-10-09 with six Kotlin warnings. Those warnings now have source corrections; rebuild to verify that revision and install its exact APK for phone checks. The successful preceding build establishes compilation/packaging for that preceding source, not backend/schema/device readiness.
2. Confirm the selected backend runs matching source with a current schema containing `TestHistory.source_session_id` and `OfflineCompletion`. For a fresh SQLite environment with app migration folders removed, normal SQLite startup creates initial migrations/schema from current models. An existing schema needs its normal authorized upgrade path. Interactive startup/runserver alone does not initialize it. No schema readiness is established here; see [backend startup](../../../../backend/docs/guides/START_HERE.md).

   Schema verification itself can use read-only database metadata without creating/applying migrations. On 2026-10-09 neither `backend/SQLite/db.sqlite3` nor `backend/db.sqlite3` exists locally. A custom SQLite path or running MariaDB/deployed backend may still exist; the database actually used by the selected server must be identified before reporting its schema ready. Source models alone do not establish that state.
3. On the phone, enter the actual server address; verify login/restore, forced password change and the first authenticated write. Verify HTTP or private-CA HTTPS as selected, including image loading and account expiry.
4. Exercise ordinary study/exam/recall through answers, navigation, pause/resume, Finish and personal History. Interrupt an answer/Finish response and confirm read-only recovery without automatic replay. Check bookmarks, mistakes/fragile practice, planner, groups and assigned-exam acknowledgement/start/resume/grace/Finish/personal results.
5. Download selected library questions; practise with the network disabled, recreate the task, finish locally, reconnect and explicitly upload. Retry a lost upload response and confirm one server History/learning effect. Check cache-capacity rejection, retained existing progress, storage retry and confirmed deletion. Logout/server/account changes must revoke private offline access.
6. Check own-session listing and password-confirmed revocation, including local sign-out after current-session revocation. Signed-cookie backend engines intentionally cannot individually revoke sessions.
7. Check Arabic/English, RTL, large text, TalkBack and reminder permission/delivery on the actual phone. Keep concrete failures in the work plan and verification outcomes in the work log.

These gates do not depend on completing moderator/admin tools. [Manual verification](../../verification/manualVerification.md) contains the broader scenarios. Builds, test-suite work and migrations still need the explicit authorization required by [Agents.md](../../../Agents.md).
