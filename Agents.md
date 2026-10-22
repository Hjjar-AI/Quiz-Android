# Android working rules

## Start and scope

- Read this file, the [documentation map](README.md), relevant current status and affected production source. Inspect the working tree; preserve unrelated edits. Do not commit unless requested.
- Finish the authorized task, reuse existing authorization and continue independent work while clarification is pending. Ask only for missing decisions or genuinely blocked actions. Do not expand a documentation edit into a build or implementation project.
- Verify reported defects against reachable callers, current state and contracts. Trace affected DTOs/services and Django/Vue behavior before contract changes; do not invent endpoints, fields, capabilities, idempotency or runtime evidence.

## Efficient verification

- Relevant source/XML/resource checks, existing focused tests, compilation and debug builds are permitted without asking again when useful to the implementation task. Choose the smallest check that can expose the relevant defect; add/update regression coverage when behavior warrants it. Full suites, release/minified builds or packaging variants are appropriate when the task or an evidenced integration risk needs them, not as a routine multiplier.
- For documentation-only edits, check links, accuracy and consistency. Do not inspect/run suites or build just because they are available. Production study/exam code is application code, not an automated test suite.
- Use installed system tools, the existing project wrapper and persistent account-owned dependency caches. Follow the [toolchain preflight](docs/guides/build-and-setup.md#verification-workflow): select a compatible installed JDK/SDK; verify the pinned Gradle distribution is cached before invoking the wrapper. `--offline` alone does not stop a wrapper-distribution download.
- Before every build or other memory-heavy verification run, check system-wide active builds (including other projects/IDE builds) and available RAM/memory pressure using installed tools. Distinguish idle Gradle daemons and reclaimable cache from active builds or genuine pressure. If another build is running or RAM usage is high/headroom insufficient, do not start another build. Continue independent work or wait briefly and recheck without busy polling. If the machine remains busy, or safe headroom cannot be established, finish the authorized work and tell the user the build was skipped, why, and the exact version-preserving command to run manually. Never stop unrelated builds/processes or clear caches to make room.
- If a required tool or dependency is missing, identify it precisely and ask the user to install or approve a one-time persistent installation/cache fill. Do not silently install software, download disposable toolchains/environments repeatedly, upgrade dependencies to bypass a cache miss, delete caches or force refreshes. Continue unaffected work while waiting.
- Verification packaging must pass `-Pmukhtabir.autoVersion=false`; no version changes unless requested. Device installation, signing/distribution, deployment, destructive operations and migrations/setup require explicit task scope. Supplied logs authorize troubleshooting; they do not establish results for changed source.
- Keep expensive command output in one local log; read the exit status and relevant error context. Avoid full log dumps, busy polling, repeated successful checks and broad searches/unrelated file reads. Recheck only after relevant changes, failures or new uncertainty. Reuse useful evidence; summarize it accurately.
- Report separately: source checks, compilation/build, automated tests, device/accessibility and HTTP/concurrency verification. Never claim a category passed from another category's evidence. If a visible five-hour allowance reaches 20% remaining, finish the current step and stop; never infer usage figures.

## Architecture, authority and recovery

- Preserve native architecture/DI/Compose patterns. Reuse working controls/controllers and focused helpers; avoid speculative rewrites or abstractions based only on line counts. Handle nullable/legacy/fractional responses, accessible serialization companions and scoped experimental opt-ins; check actual installed API signatures.
- Gate destinations and handlers independently; Django is authoritative. Own-question permissions use current `owned_by`; personal History is user-scoped and all-user History separately gated. Refresh affected profiles/selectors after confirmed writes without discarding drafts; use runtime limits/roles/pagination.
- Set busy guards before launching writes; cancel stale reads and propagate cancellation. Preserve drafts, selections, readable content and confirmed success. Reconcile uncertain writes against state/receipts before explicit retry/discard; never automatically replay answers, toggles, creates, navigation, Finish or sync. Failed reads are not empty/saved outcomes; failed pause must not exit. Consume practice intent after initialization/permission checks.
- Scope private state to account/server; preserve frozen completion identities/bodies and existing encrypted recovery. Keep other sensitive drafts memory-only, authenticated HTTP uncached, temporary files cleaned and secrets out of logs. Follow [offline/access policy](docs/guides/offline-and-recovery.md); imported metadata never authenticates or merges servers.

## UI and connection invariants

- Preserve snapshots, case-before-question order and exam/recall reveal guards in every reader, translation and result view. Reuse bilingual resources/shared tokens/controls; preserve RTL, 48dp targets, large text, focus, reduced motion and theme/font choices. Announce outcomes, never ticking timers. Coordinate shared palette/key changes with web/PDF exports; distinguish native derived contrast from base palette changes.
- Preserve configurable HTTP/private HTTPS: selected-origin HTTP only, valid chain/hostname with system/user CA trust, no trust-all validation. Use actual SDK/property configuration, never guessed phone hosts or committed machine paths. See [setup](docs/guides/build-and-setup.md).
- Fresh SQLite setup through the user's normal `python start.py sqlite` generates initial migrations only when app migration folders are absent. This is a setup caveat, not permission to inspect/run migrations. Follow the [backend startup guide](../backend/docs/guides/START_HERE.md) within explicitly authorized setup work.

## Documentation maintenance

- Keep this policy short and put procedures in focused guides. Read only what the task needs. Maintain one canonical location per topic, with README as the entry point.
- Shrink resolved review/history into a compact disposition; remove superseded transcripts, duplicate lists and obsolete examples. Preserve current setup, troubleshooting, contracts, safety boundaries and verification caveats. Do not create historical archives merely to move the same bulk elsewhere.
- Keep only current evidence and remaining gates in the [verification checklist](docs/verification/checklist.md). Record command/scope, result and relevant revision/environment when actually known; do not invent them. Source completion does not imply runtime verification. Fix document links when moving content.
