# Remaining repository work

Application/device/API work remains in the [verification checklist](verification/checklist.md#remaining-checks). This queue records the Git/GitHub transfer only; it grants no build, suite or remote execution authorization.

- Review/commit/push remaining source updates when requested. Optional hook activation and all eight manual hosted workflows remain unperformed; GitHub dispatch requires the workflows on the default branch. Added workflows default to skipped unless the operator selects `confirm=true` for their documented scope.
- Verify live repository visibility/default branch, action availability, private vulnerability reporting, CODEOWNERS eligibility and desired server-side rules before enabling protections. No remote controls were changed.
- Keep backend/Vue work deferred; the [Git review](guides/git-and-github.md#deferred-backend-and-vue-work) records concrete ignore/link gaps and proposed adaptations.
- Workflow definitions for compilation/JVM, production lint, debug APK, unsigned release APK, OSV dependencies, CodeQL and actionlint/policy are source-complete. Hosted downloads/setup/cache/task/report behavior remains unverified; run only an explicitly selected activity, preserving versions. No current execution is requested by this record.
- Keep signing/publication, history scanning, emulator campaigns, destructive cleanup and automatic actionlint pin proposals deferred as detailed in the [adaptation matrix](guides/git-and-github.md#current-setup). Preserve existing version counters, signing identity and explicit activity approvals; Aqaqeer-only crypto/flavor/ABI/reservation behavior is inapplicable.
