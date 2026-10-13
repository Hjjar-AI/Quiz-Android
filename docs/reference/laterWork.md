# Optional future work

[Current queue](../workPlan.md) · [Pending verification](../verification/manualVerification.md).

## Selected UI consolidations — source-complete 2026-10-13

The six saved selections (shared choices, sync status, recovery, confirmations, visible pending cards with smaller centered Home grids, and conventions) and the subsequent six selections (filter chips, metadata, empty states, metric tiles, section headings and size tokens) are implemented in production source. See [UI conventions](../guides/uiConventions.md#shared-learner-presentation--2026-10-13), [completed scope](../workDone.md) and [pending device gates](../workPlan.md). This is presentation reuse; it does not prove every UI in the app uses these helpers or establish runtime/compilation correctness. The previously suggested separate compact-navigation consolidation was not selected.

- Refine selection, scrolling and accessibility from concrete device findings.
- Native push or richer scheduling would be new scope; local reminders/in-app notices already exist.
- Measure large-bank/import performance before choosing further optimization.

Management pagination, editor restoration, master preview/deep links, activity/groups/content tools and planner recovery already exist in source. Do not recreate them from archived unchecked lists. PostgreSQL administration requires separate backend scope. Earlier ideas: [dated archive](../archive/README.md).
