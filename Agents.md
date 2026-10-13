# Android Agent Instructions

## Working rules

- Start at [documentation index](docs/README.md). Read applicable instructions and [workPlan](docs/workPlan.md)/[workDone](docs/workDone.md)
  first; inspect current production source and the working tree. Preserve unrelated
  edits and existing authorization; do not commit unless requested.
- Trace affected contracts and callers before making focused corrections. Do not
  invent endpoints, permissions, idempotency or verification evidence.
- No migration inspection/changes/setup, automated-suite inspection/work, or
  Gradle/build/compilation/packaging without an explicit request. Supplied logs and
  user-run builds authorize source troubleshooting, not agent-run builds. No version
  changes unless requested. Production study/exam modules are application source;
  exclude actual suites and migration directories from content searches.
- Lightweight production-source/XML/AST/whitespace checks are allowed. State their
  scope; source completion does not establish compilation, HTTP/device behavior,
  accessibility, deployed schema or database concurrency.
- Reviews must trace reachable controls, DTOs/serializers, gates/ownership, retained
  state and failure/recovery paths across affected clients. Endpoint inventories and
  old Markdown alone are insufficient. Distinguish shared client defects, native
  omissions, backend limits and deployment/runtime gates. Report prioritized findings
  with source evidence; completing a bounded list does not prove full parity.
- Keep remaining work in the plan and completed work/checks/limits in the log.
  Compact stale records with linked archives; keep supporting Markdown in `docs/`
  and root instructions discoverable. Preserve intentional policies when compacting.
- Continue authorized work; ask only for missing decisions. Communicate concise
  findings and limits in English unless requested otherwise. If a visible five-hour
  allowance reaches 20% remaining, finish the current step and stop; never infer usage.

## Scope and priority

- Preserve native architecture, naming, dependency injection and Compose patterns.
  Follow the current P0–P4 definitions in `docs/workPlan.md`; confirmed ordinary-use
  failures take priority. Learner participation/context differs from management.
- For "continue"/"one more", choose a useful bounded unfinished item after checking
  current source. Unchecked verification does not imply missing implementation.
- Keep remaining scope in the plan, feature/date/contracts/limits in the work log,
  device gates in [manual verification](docs/verification/manualVerification.md), and optional
  ideas in [later work](docs/reference/laterWork.md). Do not duplicate completed features.

## Contracts and learning

- Verify changed Django views/serializers in `../backend` and relevant Vue callers;
  update endpoints, DTOs, transport, ViewModels and reachable UI together. Do not
  invent request fields/filters; handle nullable, missing, fractional and legacy data.
  Keep serialization companions accessible for generated serializers.
- Django capabilities are authoritative: gate destinations and mutation handlers as
  well as buttons, and handle server 403/404/conflicts. Own-question edit/delete uses
  current `owned_by`, not historical `authored_by`.
- Profile/capability/rank/expiry freshness and dependent tag/category/blueprint
  selectors are part of integration. After relevant confirmed writes, invalidate
  affected reads without discarding drafts or resetting an active exam. Retain safe
  structured API validation details/reasons across transport and screen handlers.
- Ordinary History filters by signed-in `user_id`; unfiltered history requires the
  separate `tests.view_all_history` destination. Read choice/quiz/pagination/role
  settings from `GET /config/` with `PublicConfigDto` fallbacks; avoid fixed limits.
- Show case context before questions across detail/session/history/results. Preserve
  returned snapshots and exam/recall reveal rules, including previews and accessibility.

## Writes, recovery and private data

- Set busy state synchronously before launching coroutines; guard conflicting writes
  in ViewModels. Propagate cancellation and prevent stale loads overwriting new state.
- Preserve drafts/readable content on failure; distinguish initial-load, action and
  post-save refresh failures. Reconcile ambiguous writes before allowing more writes;
  never blindly replay answers/navigation/finish or other uncertain mutations.
- A busy guard does not resolve response loss: toggles can undo committed changes
  and repeated creates can duplicate them. Use current-state/receipt reconciliation
  where supported; otherwise retain uncertainty and require manual review. Compare
  older detail handlers with newer shared recovery flows, not only the main list.
- Distinguish status-read failure from no saved session. Consume practice presets only
  after initialization/capability checks. Failed pause must not silently exit.
- Confirm destructive actions, discarded progress and user-initiated finish; retain
  timer expiry. Resolve editor conflicts explicitly after comparing latest data and
  rechecking ownership; do not claim atomic protection absent a backend precondition.
- Keep authenticated transport uncached without an explicit account-scoped offline
  policy. Preserve session-clear cancellation/legacy-cache cleanup. Never commit
  credentials, tokens, device identifiers or private logs; show image errors with Retry.
- The approved offline policy is encrypted local practice plus explicit server
  synchronization. Preserve frozen completion identities/bodies and durable backend
  receipts; ordinary uncertain writes still must not be replayed automatically.
  Logout, account/server changes and session clearing revoke private offline access.
  Address changes retain encrypted local server profiles; alternate addresses share
  a profile only through the explicit same-server choice, never imported file identities.

## Backend schema readiness

- For a fresh SQLite database with app migration folders removed, the user's normal
  `python start.py sqlite` setup generates initial migrations from current models
  and applies them. Do not insist on separate additive migrations for that workflow.
- If app migration files remain, an empty database does not make the launcher generate
  migrations for later model changes. `manage.py runserver` and interactive
  `python start.py -i` do not perform schema setup. See
  [backend startup](../backend/docs/guides/START_HERE.md).
- Report schema readiness only after actual setup is confirmed. Describing the user's
  fresh setup does not authorize the agent to inspect/create/apply migrations, run
  setup/seeders or build Android; the explicit-request rules above remain in force.

## Presentation and troubleshooting

- Add matching English/default and Arabic resources; check XML, duplicates, references
  and positional placeholders. Preserve RTL using the configured dependency APIs.
- Expose selection/non-color state, labels, touch targets and error announcements.
  Do not make ticking timers live regions. Keep TalkBack, large-text and native
  Markdown focus checks pending until exercised on a device.
- Read supplied diagnostics first; use [ADB guidance](docs/guides/Debugging.md) for device
  issues. Keep tests out of production inputs; do not add runtime test dependencies
  or inspect tests to work around a misplaced test without explicit authorization.
- Use `ANDROID_HOME` or ignored `local.properties` for SDK paths; preserve property-based
  API configuration and do not guess phone hosts. The user approved configurable
  HTTP/private HTTPS in all variants: preserve startup server selection and external
  `server.properties`, restrict HTTP exchanges to the selected origin, and validate
  HTTPS chains/hostnames using system or user-installed CA trust. Never add trust-all
  TLS or a permissive hostname verifier. See [server configuration](docs/contracts/serverConfiguration.md).
- Fix source against current versions; prefer supported APIs/scoped opt-ins over
  upgrades. Compiler-log fixes remain uncompiled until an authorized build; source
  delimiter checks do not establish Kotlin correctness or device readiness.
- Check shared component signatures and name non-leading arguments (`ErrorState`
  takes `modifier` before `throwable`). Scope experimental Compose opt-ins to callers;
  align exposed property/type visibility. In suspend helpers without a CoroutineScope
  receiver, use the current coroutine context for cancellation checks. Trace equivalent
  callers when fixing a compiler error rather than patching only the first occurrence.
