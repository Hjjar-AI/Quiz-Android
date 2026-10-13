# Android / Vue / Django feature map

Current summary 2026-10-10, incorporating source fixes after the 2026-10-09 comparison. “Represented” means reachable source exists, not verified runtime parity. [Status](../workDone.md) · [Manual checks](../verification/manualVerification.md).

| Function | Android source coverage / intentional boundary |
| --- | --- |
| Authentication/account/server | Login, profile/password/expiry/logout, manual/file server selection and three hidden imported slots; actual cookies/CSRF/TLS remain integration checks. |
| Ordinary learning | Study/exam/recall, filters/strategies, answers/reveal/timers, pause/resume, exact results/History and encrypted uncertain-write recovery. Broad History needs its separate permission. |
| Library/review/content | Questions, bookmarks, mistakes/fragile/smart review, translations/cases/images, knowledge map, rating/rank cards and case browsing. Learner/management lists now use full server pagination; autocomplete still has intentional bounds. |
| Planner/groups/activity | Exact native tag lists, encrypted recovery, revision/id conflicts, historical scope attribution, member groups/leaderboards, streak/activity and richer metrics. |
| Learner master exams | Acknowledgement, fresh start access, normal/makeup resume, timing/grace, frozen questions, flags/results, answer/navigation preconditions and explicit draft recovery. |
| Authoring/moderation | Rich questions/translations, exact tags/images/camera/Markdown, cases/knowledge, own/any gates, verification/notes/reports/ratings and statistics. |
| Administration | Users/roles/individual overrides/expiry/password reset, categories/icons/tags/groups/blueprints, standalone content, settings and analytics. Revision/conflict and receipt-backed recovery are represented; runtime races need checks. |
| Master management | Composition/audience/co-authors, private/shared drafts, interactive preview/lifecycle, foreground live results/matrix/CSV. Blueprint weights are category weights; difficulty allocation is not persisted by backend. |
| Imports/exports/operators | Simple/Telegram/state import; ordered JSON/CSV/XLSX/PDF/state exports; backup/restore/clear and quality/rank/seed tools. Destructive tools were not executed. |
| Native additions | Encrypted offline practice/full bank, local reminders and own-session controls. Signed-cookie sessions cannot be individually revoked. |
| Presentation | Bilingual/RTL themes/fonts/density, drawer/help/preferences and accessibility controls; device approval pending. |

Full-bank download is accessible study material, not an admin backup. Selected downloads without its capability have a server cumulative distinct-ID budget. Imported-file hiding does not encrypt its JSON or authenticate its decorative identities. Same-server profile sharing requires explicit user choice.

The [2026-10-10 detailed parity review](../archive/reviews/2026-10-10-androidParityReview.md) recorded seven further subfeature/behavior gaps and smaller convenience differences, now fixed in source. The six earlier parity/cross-cutting findings, seven rescan findings and eight deeper findings are source-complete; see [dated evidence](../archive/README.md). Backend PostgreSQL configuration exists, but native PostgreSQL provisioning/backup/restore administration remains unsupported. Source review is bounded; trace current routes, handlers, DTOs and Django contracts for new work.


Source-book selection (2026-10-10): ordinary exam/study/recall setup in Android and Vue offers a dedicated book picker from public question source-document metadata. Counts and start share exact-title filtering and existing refinements. Source-complete; matching backend rollout/imported content and manual verification remain required.

Optional case translations (2026-10-10): collapsed case-authoring fields feed the existing question-language preference; available translated title/stem render independently with original fallback. Ordinary/master/offline case snapshots carry translations without live substitution into historical content. [Contract and rollout limits](../../../backend/docs/contracts/caseTranslations.md).
