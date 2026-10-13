> Historical snapshot before the 2026-10-10 documentation reorganization. Current status: [documentation index](../../README.md). Do not use unchecked items here as the current queue.

# Optional Android scope

Updated 2026-10-09. Confirmed implementation gaps belong in [workPlan.md](../../workPlan.md), runtime checks in [manualVerification.md](../../verification/manualVerification.md). Earlier detailed ideas and completed scopes are preserved in [the archive](../2026-10-09-laterWork.md).

- Validate the new encrypted standalone knowledge/case/tag/stem recovery on-device, including persistence failures and process death; see [manual verification](../../verification/manualVerification.md).
- Full case/knowledge management server pagination is implemented; validate large datasets and failed page requests on-device. Autocomplete picker limits remain intentional.
- Refine secondary selection, scroll, visual and accessibility behavior from concrete device findings.
- Native push or richer scheduling would be new scope; existing local reminders/in-app notices already exist.

Master-exam preview, deep links, activity/streak, preferences, group/blueprint/tag/content management, camera and encrypted planner recovery are already represented. Their archived unchecked lists are historical, not new implementation requests. Backend-only PostgreSQL backup/restore administration is unsupported and must not be presented as a mobile feature.
