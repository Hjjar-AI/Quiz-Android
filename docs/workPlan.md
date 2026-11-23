# Remaining repository work

## Standalone demo

- Verify both debug APKs side by side and check the merged demo manifest. Confirm that the standalone demo opens on Home and that the main app still reaches server setup, sign-in, and saved downloads.
- Verify Arabic/English, large text, restoration, failure recovery and upload deduplication. After explicit approval, verify workflow 06 with `application=both` and workflow 07 with a single API selection plus the default two-API matrix.

## Other Android work

Application/device/API work remains in the [verification checklist](verification/checklist.md#remaining-checks). The [complete workflow audit](guides/workflow-audit.md) accounts for Aqaqeer 01–15 and Mukhtabir 00; source preparation grants no execution authorization.

- Review/commit/push remaining source updates when requested. The optional hook is not activated. None of the 16 hosted definitions was dispatched by the agent; default-branch availability, setup/downloads/cache reuse/tasks/reports/media/API behavior still require selected runtime verification.
- User will add keys later: configure the separate signing environments, four secrets and fingerprint in each, then their repository readiness variables using the [signing setup](guides/git-and-github.md#signing-setup-for-later). Verify workflow 10 with `application=both` and `publication=artifact` after signing setup, including separate package/checksum receipts and installed-update continuity. Publication defaults to artifacts; draft/prerelease/release are explicit later choices. Stored versions stay unchanged; authorize any intended production version increase separately.
- Verify hosted demo dependency resolution (02), production compilation and per-module JVM reports (03), lint XML summaries (04), and CodeQL coverage (05). Confirm APK/compile retry diagnostics if a qualifying dependency HTTP 429 occurs. Instrumentation/coverage expansion and workflow 07's missing behavioral regression selection require separately explicit test-file scope; existing JVM cases are not emulator instrumentation. The [07 scope](guides/git-and-github.md#workflow-07-scope) distinguishes capture from functional acceptance.
- Workflow 08's release inspection and throwaway-signed API 35 main startup capture are source-complete but unexecuted. Verify actual merged manifest/resources/permissions/assets/alignment, minified UI/process survival and owned cleanup before claiming a pass.
- Workflow 09 remains manual, preview-first; automatic seven-day deletion is not enabled. Workflow 14 separately previews/cleans marker-owned published debug Releases/tags with age/keep guards. Neither was queried or executed; review the [cleanup scope](guides/git-and-github.md#workflow-09-scope) and retain needed evidence before authorizing deletion.
- Workflow 12's configuration-cache/dry-run checks, 13's production-history scan and 15's newer-pin patch proposal remain unexecuted. Applying proposed pin/version changes is separate from preparing the proposal workflow.
- Verify live repository visibility/default branch, Actions/code-scanning eligibility, private vulnerability reporting, CODEOWNERS eligibility and desired server-side controls; no settings were changed.
- Keep backend/Vue work deferred; the [Git review](guides/git-and-github.md#deferred-backend-and-vue-work) records their evidenced ignore/link gaps and proposed adaptations.
