# Remaining repository work

Application/device/API work remains in the [verification checklist](verification/checklist.md#remaining-checks). This queue records the Git/GitHub transfer only; it grants no build, suite or remote execution authorization.

- Review/commit/push the prepared Git tooling when requested. Optional hook activation and the manual hosted repository workflow remain unperformed; GitHub dispatch requires the workflow on the default branch.
- Verify live repository visibility/default branch, action availability, private vulnerability reporting, CODEOWNERS eligibility and desired server-side rules before enabling protections. No remote controls were changed.
- Keep backend/Vue work deferred; the [Git review](guides/git-and-github.md#deferred-backend-and-vue-work) records concrete ignore/link gaps and proposed adaptations.
- Build/test/lint/dependency/history scanning, signing/publication and cleanup workflows remain deferred as detailed in the [adaptation matrix](guides/git-and-github.md#current-setup). Preserve existing version counters, signing identity and explicit activity approvals.
