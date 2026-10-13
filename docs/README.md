# Quiz project documentation

Updated 2026-10-10. Start here for humans and agents working across Android, Django and Vue.

## Read first

1. Read the repository's [agent rules](../AGENTS.md) and [detailed policies](../Agents.md).
2. Read [current completed scope](workDone.md) and [remaining work](workPlan.md).
3. Open the relevant functional reference below, then trace current production source.
4. For runtime work, use [manual verification](verification/manualVerification.md); record actual outcomes separately from source completion.

| Need | Canonical document |
| --- | --- |
| Android / web / backend coverage and intentional limits | [Feature map](reference/functionalityReview.md) · [Latest parity review (source-complete)](archive/reviews/2026-10-10-androidParityReview.md) |
| Server entry, hidden imported files, three slots, same-server retained files | [Server configuration](contracts/serverConfiguration.md) |
| Downloaded ordinary session questions, local answers and deadline limits | [Downloaded sessions](contracts/downloadedSessions.md) |
| Offline bank, per-user permission, storage and sync | [Offline practice](contracts/offlineQuestionBank.md) |
| Learning evidence, SRS chronology, selection and planner attribution | [Learning contracts](../../backend/docs/contracts/learningConsistency.md) |
| Saved writes, receipts, revisions, master attempts and polling | [Recovery/concurrency contracts](../../backend/docs/contracts/writeRecovery.md) |
| Database setup and coordinated rollout | [Schema readiness](../../backend/docs/contracts/schemaReadiness.md) · [Startup](../../backend/docs/guides/START_HERE.md) |
| First phone run / debugging | [Learner launch](guides/learnerLaunch.md) · [ADB](guides/Debugging.md) · [Historical SDK notes](archive/reviews/2026-10-09-GradleShare.md) |
| Native component and layout standards | [Android UI conventions](guides/uiConventions.md) · [Launcher artwork](guides/launcherArtwork.md) |
| Web components/themes | [Frontend index](../../frontend/docs/README.md) |
| Current native shell/layout inconsistencies | [Android UI review (source-complete)](reference/androidUiReview.md) · [Posted audits reviewed](reference/2026-10-10-postedAuditReview.md) |
| Production hosting | [Deployment](../../backend/docs/guides/DEPLOYMENT.md) |
| Optional ideas | [Later work](reference/laterWork.md) |
| Why a change happened | [Dated archive](archive/README.md) |

## How these records stay usable

- `workPlan.md`: only current unresolved implementation and release gates. Completed findings move out of the queue.
- `workDone.md`: compact current source status by function; link dated evidence rather than copying the same change into all three repositories.
- `contracts/`, `guides/`, `reference/`: maintained functional behavior, operational instructions and coverage. Update these when behavior changes.
- `verification/`: pending runtime scenarios and recorded outcomes. A checkbox changes only with actual evidence.
- `archive/`: dated snapshots/reviews. Preserve their context; they are not the current backlog and their old build counts do not approve current source.

Markdown links are document-relative. Shell commands and unlinked source paths are repository-root-relative unless stated otherwise. When moving a document, repair inbound and outbound links. Do not put private logs, credentials or user data in documentation.
