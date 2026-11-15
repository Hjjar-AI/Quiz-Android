# Git and GitHub maintenance

## Current setup

The 2026-11-15 review adapted Aqaqeer's repository tooling to Mukhtabir Android. The local remote is `Hjjar-AI/Quiz-Android`, checked-out branch `main`; this does not verify GitHub's current default branch, visibility, billing, rulesets or enabled security features. Aqaqeer's application edits were left untouched. Backend and Vue suggestions below are deferred; neither repository was changed. The [complete 01–15 audit](workflow-audit.md) records every source workflow and intentional difference.

| Aqaqeer capability | Mukhtabir disposition |
| --- | --- |
| Signing/private-state/package ignore rules | Added missing private evidence, signing formats, generated packages and CI-output rules to the existing ignore file. Ignore rules do not untrack previously committed files. |
| Text/binary attributes and editor defaults | Added LF text defaults, Windows batch CRLF/whitespace handling, binary assets and editor conventions. No mass renormalization. |
| Repository integrity/credential helper | Adapted to current Mukhtabir wrappers and source layout, with test/migration/private-content exclusions always enabled. Removed Aqaqeer-only catalog/crypto/version assumptions. |
| Pre-push hook | Added optional hook; deletion-only pushes skip it. Checks working-tree source, not outgoing commit objects/history. |
| Manual workflows | Sixteen manual definitions account for Aqaqeer 01–15 plus shared workflow syntax/policy 00. All source workflows have a disposition in the linked audit; no hosted execution is claimed. |
| Dependabot | Weekly grouped GitHub Actions proposals, seven-day cooldown, at most five open version-update PRs. No automatic merge; no current dependency version changed. Gradle updates remain deferred. |
| Review routing, issues and security guidance | Added CODEOWNERS for build/repository entry points, PR checklist, bilingual issue forms and a private-reporting policy. CODEOWNERS does not itself enforce review. |
| Build/JVM/lint/dependency/CodeQL checks | Adapted as manual definitions with opt-in confirmation, current Mukhtabir task names, production-only dependency/lint scopes and preserved versions. No application/test source changes or execution. |
| Emulator visual evidence | Workflow 07 is adapted for a serial API 29/35 matrix, owned hosted AVD, fresh install, startup/demo captures, recording, provenance, failure diagnostics and identity-checked cleanup. Earlier omission conflated transferable orchestration with Aqaqeer-only instrumentation methods. Mukhtabir-specific instrumentation coverage remains a separate authorized scope, not an implemented regression pass. |
| Public crypto-vector checks | Aqaqeer's file-crypto/transport contracts and vectors do not apply to this client; no fabricated matching suite was added. |
| Signed debug/production delivery, reservations, APK inspection | Artifact-only default, with explicit draft/prerelease/release choices and isolated signing. Release inspection follows current Mukhtabir package/manifest/assets/network policy; 08 adds throwaway-signed minified startup capture. Keys remain for later. Existing versions are preserved; no remote reservation/ABI/flavor mechanism was invented. |
| History scanner and actionlint updates | Checksum-pinned production-history scanning is defined with path exclusions/count-only output. Workflow 15 proposes newer stable actionlint pins as an artifact, without applying or executing them. Neither helper was run. |
| Build-tooling/cache-reuse verification | Workflow 12 defines two configuration-cache help invocations and a packaging dry-run with version-byte preservation. Cache reuse/performance and wrapper behavior remain unverified until an authorized run. |
| Rulesets, protected tags, security settings | Documented below for later owner configuration; no GitHub settings changed. |
| CI/debug-release cleanup | Workflow 09 defines manual completed-run preview/deletion, seven-day default; 14 separately defines marker-bound debug Release/tag preview/deletion, 30-day default and at least five retained. Automatic schedules are off and no cleanup ran. |
| License/NOTICE and release changelog | Do not copy Aqaqeer's product license, third-party notices or release history. Confirm this project's licensing and shipping dependency graph separately before distribution. |

## Local checks and optional hook

From the Android repository root, with Python 3.9+ and Git installed:

```sh
python3 .github/scripts/check-repository.py
```

The checker reads tracked and unignored working-tree text files, checks JSON/XML/Python syntax, repository-local Markdown links/anchors, selected credential patterns, wrapper distribution-checksum presence and existing version-preparation markers. It never invokes Gradle, reserves versions, downloads tools or reads private file contents. Suspected credentials report only rule/path/line, without values or source excerpts. This is a limited source guard, not a comprehensive secret scanner or wrapper-JAR authenticity check.

Tests/test fixtures, migrations, schemas and generated directories are excluded. Links into those paths are not followed. Sibling backend/Vue links and absolute workstation links are counted separately without reading their targets; CI can run from an Android-only checkout. Binary content, YAML semantics, arbitrary passwords, ignored files, Git history, outgoing commits and runtime behavior remain unverified. A locally deleted tracked file is skipped; committing an intended deletion remains the operator's responsibility.

To enable the hook explicitly for this checkout:

```sh
git config --get core.hooksPath
# If no existing hook setup needs to be retained:
git config core.hooksPath .githooks
```

The hook has not been activated automatically. Preserve any existing custom hook configuration before selecting it. Restore the prior value when disabling it; if there was no prior value, `git config --unset core.hooksPath` restores Git's default location. A passing working-tree check does not prove all pushed commits are safe; review the actual outgoing changes before pushing. Nothing here commits or pushes changes.

## Hosted controls

After the files are committed/pushed and workflows exist on the repository's default branch, an authorized operator can select **Actions → workflow → Run workflow**. [GitHub's manual-workflow instructions](https://docs.github.com/en/actions/how-tos/manage-workflow-runs/manually-run-a-workflow) describe the default-branch requirement. All jobs skip private repositories to avoid assuming paid runner eligibility. Workflow 01 runs source checks only; all added workflows require default-branch dispatch and `confirm=true`, with the input describing downloads and the selected activity. The default `false` skips execution. No workflow was dispatched by the agent.

| Workflow / file | Manual operation and retained output |
| --- | --- |
| **00 Workflow syntax** / `workflows.yml` | Download checksum-pinned actionlint and a hash-pinned YAML parser, enforce manual/public/pin/cache/retention policy, validate workflow YAML/shell. One-day diagnostics. |
| **01 Repository integrity** / `repository.yml` | Existing lightweight source/configuration/link and redacted credential-pattern checks. No Gradle or suite reads. Summary only. |
| **02 Dependency vulnerabilities** / `dependencies.yml` | Resolve `debugRuntimeClasspath`, `releaseRuntimeClasspath` and build-plugin classpaths through `:app:ciDependencyInventory`; query exact public Maven coordinates through [OSV's API](https://google.github.io/osv.dev/api/). No upgrades, test configurations, KSP assumptions or compilation. One-day inventory/findings/triage/logs. Empty/incomplete resolution fails; advisory exceptions require an exact coordinate, reason and future expiry. No exceptions are preapproved. |
| **03 Compilation and JVM** / `verification.yml` | Choose `compile-debug`, `compile-release`, `jvm` or `all` (both production variants plus the full existing JVM suite). Default debug compilation. JVM/all explicitly include app/test compilation, reports and JUnit results. No suite files were inspected/changed and no coverage/instrumentation task was fabricated. |
| **04 Production Android lint** / `quality.yml` | `:app:lintDebug` with an init script excluding test sources. One-day HTML/XML reports and log. |
| **05 Security analysis** / `security.yml` | Serial CodeQL Java/Kotlin, Python and Actions; extended queries for compiled/Python languages. Fresh native-language extraction, scoped security-result upload and one-day SARIF/diagnostics. Requires actual GitHub eligibility/settings. |
| **06 Debug APK** / `android.yml` | `:app:assembleDebug`; one universal APK, SHA-256 checksum and diagnostics retained one day. Ephemeral runner debug signer: do not use this to replace a differently signed existing installation. No installation or publication. |
| **07 Emulator with visual evidence** / `emulator.yml` | Build the current debug application with unchanged version; launch fresh API 29/35 hosted AVDs serially; verify emulator/user/package/signer/artifact/install provenance; capture startup, demo and 1.3× text screenshots plus up-to-45-second video. Retain one-day evidence and failure diagnostics. No permanent signing keys, account/server or instrumentation suite required. See the scope below. |
| **08 Release output and startup** / `release.yml` | Current shrunk release build, package/version/SDK/manifest/network-policy/permission/resource/asset/alignment inspection, then isolated API 35 minified startup/demo capture and 30-second process observation using a temporary signer. Shipping artifact remains unsigned; no permanent signing keys or production update acceptance. One-day APK/mapping/inspection/capture evidence. |
| **09 Clear workflow runs and logs** / `cleanup-evidence.yml` | Manual `confirm=true`, preview by default (`dry_run=true`), seven-day default cutoff. Select `all-workflows` including Dependabot or `emulator` for 07 only; integer days 0–365, with 0 selecting all otherwise-eligible completed runs. `dry_run=false` deletes selected completed runs and all attached logs/artifacts/APKs. No schedule; active runs, current cleanup run, Releases/assets, tags, source and caches are preserved. |
| **10 Signed debug APK** / `sign-debug.yml` | Fresh version-preserving build and permanent-debug signing. `artifact` default; explicit `draft`, `prerelease` or `release` GitHub creation also available, always `latest=false`. Requires signing setup. |
| **11 Signed production APK** / `sign-release.yml` | Fresh inspected release build and isolated production signing. `artifact` default; explicit `draft`, `prerelease` or regular `release` choices. Only regular production Release is marked latest. Stored version preserved; no existing tag/Release is overwritten. |
| **12 Build tooling** / `build-tooling.yml` | Two Gradle `help` invocations checking configuration-cache reuse, then a debug packaging dry-run; stored version bytes must not change. One-day logs. No actual packaging or dependency-cache performance claim. |
| **13 Production history secrets** / `secret-history.yml` | Fetch available branches/tags/history and run checksum-pinned Gitleaks on production paths, excluding suite/migration/schema paths. Only a finding count reaches logs/summary; raw reports/logs are private temporary files, removed when the helper exits and never uploaded. Findings fail the job and need a separately scoped private investigation. Not an all-files or unreachable-object scan. |
| **14 Debug Release cleanup** / `cleanup-debug.yml` | Manual preview-first cleanup of owned published debug Releases and matching tags. Default age 30 days, at least five newest retained, drafts/production/unmarked/immutable Releases preserved. No automatic mutation retries. |
| **15 Actionlint updates** / `actionlint-updates.yml` | Query upstream stable release metadata/digest and retain a proposed newer-pin patch for manual review. No tool execution, pin change, PR creation or merge. |

The shared composite action uses JDK 21, the existing checksum-pinned Gradle 8.7 wrapper, SDK 34 and Build Tools 34.0.0 ([AGP 8.5 defaults](https://developer.android.com/build/releases/agp-8-5-0-release-notes)). SDK/tool/dependency downloads occur only on a separately chosen hosted run. Gradle uses the basic GitHub cache provider; only default-branch runs can write caches. Full action commit pins come from Aqaqeer and were checked against upstream commit pages. Dependabot includes local composite-action dependencies.

All Android workflows share non-cancelling concurrency to avoid competing hosted operations. The helper checks available RAM and other launchers/workers before Gradle, limits it to one worker/in-process Kotlin and passes `-Pmukhtabir.autoVersion=false` for every operation. It checks stored-version bytes before/after, with no counter reset or reservation. Output is explicitly isolated under runner temp: the existing Gradle scripts derive module build output from the selected project-cache parent. Reports/APKs are collected from that actual module path, rather than assuming `app/build`. Gradle logs remain in the one-day artifact; failed commands fail the job without automatic retry. A setup failure can have no operation report. Hosted task execution, setup/cache reuse and artifact contents remain unverified until a run occurs.

The checkout action uses the [same upstream v7.0.1 commit](https://github.com/actions/checkout/commit/3d3c42e5aac5ba805825da76410c181273ba90b1) reviewed in Aqaqeer. [Dependabot configuration](https://docs.github.com/en/code-security/reference/supply-chain-security/dependabot-options-reference) proposes later action-pin changes for review once the configuration is on the default branch. These bot proposals are distinct from manual Actions dispatch. No Gradle Dependabot job, automatic build/test trigger or auto-merge is configured here.

## Signing setup for later

The user will add signing keys later; no keys were requested, copied, inspected, generated or uploaded during this work. Workflows 10 and 11 are already defined and refuse to start building until their corresponding repository readiness variable is exactly `true`. Creating workflow files does not create environments/secrets or prove their values agree.

In **Settings → Environments**, create `debug-signing` and `release-signing`. Restrict each to the actual default branch and add reviewers if desired. They use the same names but different values, scoped to their own environment:

| Type | Name | Value to add later |
| --- | --- | --- |
| Environment secret | `MUKHTABIR_SIGN_KEYSTORE_BASE64` | Base64 of the existing intended keystore; never commit or paste it into an issue/log. |
| Environment secret | `MUKHTABIR_SIGN_KEY_ALIAS` | Alias of the intended signing key. |
| Environment secret | `MUKHTABIR_SIGN_STORE_PASSWORD` | Keystore password. |
| Environment secret | `MUKHTABIR_SIGN_KEY_PASSWORD` | Key password. |
| Environment variable | `MUKHTABIR_SIGN_CERT_SHA256` | Public SHA-256 certificate fingerprint; 64 hex characters, optional colons. This is not the keystore or APK hash. |
| Repository variable | `MUKHTABIR_DEBUG_SIGNING_READY` | Set `true` only after the `debug-signing` configuration is ready. Missing/other values fail before building. |
| Repository variable | `MUKHTABIR_RELEASE_SIGNING_READY` | Set `true` only after the `release-signing` configuration is ready. Missing/other values fail before building. |

Preserve the signer already used for the installations you intend to update and retain a protected off-device backup. Debug and release currently share package `com.mukhtabir.android`; differently signed APKs cannot update one another. Do not uninstall or clear existing data to bypass that mismatch. Debug/production keystores remain separate identities unless an explicitly chosen continuity policy says otherwise. The earlier workflow 06 still produces an ephemeral-signer preview, while 10 produces a permanent-signer debug artifact after setup.

Each delivery workflow builds without signing secrets, retains its input artifact for one day, then starts a separate signing job with its dedicated environment. The signing job does not run Gradle or restore/save Gradle caches. It downloads only the current run/attempt's named build artifact, checks its checksum and source version, and exposes the four secrets only to the signing command. Temporary keystores use private permissions and are removed when signing exits. [Android apksigner](https://developer.android.com/tools/apksigner) verifies the output against the expected public certificate; successful output includes APK, certificate/version/source/run receipt and checksums, plus the release shrinker mapping. Private key/password/tool diagnostics are withheld and are never uploaded.

Select `publication=artifact` to retain the signed bundle for one day without a GitHub Release. Select `draft` for an unpublished Release, `prerelease` for a published prerelease, or `release` for a regular published Release. Artifact-only remains the default. Only the dependent publication job gets `contents: write`; it receives no signing secrets. It verifies signed-bundle checksums and source/run/attempt receipt, lists existing Releases/tags successfully before writing, and never edits or overwrites them. Debug modes always use `latest=false`; production regular Release uses `latest`, while other production modes use `latest=false`. See [GitHub Release creation](https://cli.github.com/manual/gh_release_create).

Production tags use `android-v<VERSION_NAME>-code-<VERSION_CODE>`; debug tags use `debug-<RUN_ID>-<ATTEMPT>`. Debug Release notes include the source/run/attempt ownership marker used by workflow 14; unmarked older Releases remain protected. An interrupted/failed GitHub write requires inspection before an explicit retry; no automatic replay occurs.

These definitions preserve stored versions for every publication mode. Repeated builds are not a reservation system. Before production distribution, separately authorize the intended version update and verify signer/update continuity and matching backend/client rollout. Keys/readiness/actual signing/publication remain unconfigured/unexecuted by the agent. Final publication is possible only through an explicitly selected non-artifact mode on a separately authorized dispatch; no Release was created here.

Before enabling server-side protections, inspect actual GitHub settings and repository eligibility. Useful candidates are secret scanning/push protection where available, restricted default-branch deletion/force-push, review requirements appropriate to the real maintainer team, read-only workflow defaults and protected production tags after a release naming policy exists. Do not require this manual workflow as a PR status check without separately designing its trigger/approval policy. Private vulnerability reporting and CODEOWNERS eligibility still need live verification. This review did not query or change those controls.

## Workflow 09 scope

Aqaqeer 09 automatically removes completed runs older than seven days daily at 03:23 UTC (06:23 Asia/Damascus), and also offers manual preview/deletion. Mukhtabir has the reusable **manual** path, with preview enabled by default. The destructive automatic schedule has not been enabled; creating this definition did not list or delete any actual GitHub runs.

For a later authorized run, choose Actions → **09 - Maintenance - Clear workflow runs and logs**, `confirm=true`, `dry_run=true`, scope and age. Review the preview. A separate dispatch with `dry_run=false` selects deletion; it is a new live selection, not a frozen copy of an earlier preview. Age uses the run's last-updated UTC timestamp so a recently rerun old build is retained. `older_than_days=0` removes the age restriction but still excludes active runs and the cleanup's own run. Deleting a run removes its attempts/logs/summaries and attached artifacts, including signed/unsigned APK downloads; export any needed evidence first. [GitHub's workflow-run REST API](https://docs.github.com/en/rest/actions/workflow-runs#delete-a-workflow-run) provides the deletion operation.

The helper verifies the actual public repository ID/name/default branch, snapshots all paginated runs without the filtered-search 1,000-result cap, and rereads each candidate immediately before mutation. Repository/workflow/run identity, completion, cutoff, attempt and last-updated timestamp must still match. Acknowledged deletion is followed only by bounded GET confirmation; no DELETE is automatically replayed. Unconfirmed absence or an API failure is an incomplete cleanup, not a deletion pass. The cleanup job alone receives `actions: write`, checks out default-branch tooling and uses dedicated non-cancelling concurrency. It never cancels runs or calls release/tag/cache deletion endpoints. These guards reduce stale-selection risk but cannot make the separate REST read/delete operations atomic with an external rerun; start cleanup while intended candidate runs are quiescent.

## Workflow 07 scope

Aqaqeer 07 combines reusable hosted-device/evidence infrastructure with its own application instrumentation. Its default API 29/35 jobs package demo app/instrumentation, run selected offline cases and retain recordings, screenshots, results and provenance. The optional standard-functional API 35 job waits for the demo matrix, can run despite its failures when not cancelled, and runs additional app-specific cases. Missing evidence or incomplete instrumentation fails the helper; emulator startup alone is not a functional pass. Permanent signing keys are not prerequisites for those ephemeral installations.

Mukhtabir's 07 now reuses the applicable infrastructure: manual confirmed default-branch/public-runner execution, full action pins, bounded KVM-backed AVDs, `fail-fast=false`, `max-parallel=1`, per-API evidence and one-day retention. It uses the current `MainActivity` → unconfigured launch screen → visible `demo_open` control → demo screen route, without adding an application shortcut or invented instrumentation class. The ADB helper matches current Arabic/English resource labels, taps the demo entry once, captures startup/demo and large-text views, and records only the first 45 seconds. UIAutomator is used to observe the visible app controls; screenshots are evidence for review, not rendered-layout or accessibility assertions.

Every ADB command is bound to `emulator-5554`. The helper refuses non-GitHub-hosted execution, checks the expected AVD name/API, QEMU identity and user 0, refuses an existing package, verifies the source APK signer/package, records UID/install identity, and compares installed APK bytes to the built artifact. Wi-Fi/mobile-data disablement and its readback are recorded; this is not packet-level isolation proof. Demo data is the existing local fake repository; no account or server is configured. Cleanup rechecks emulator/install identity before stopping/uninstalling, stops only its recorder with the unique remote filename, and reports ambiguous installation/cleanup rather than guessing ownership. Evidence/cleanup failures fail the helper. The emulator action owns AVD shutdown; no physical device is selected or touched.

This remains a **startup/demo visual-capture selection**, not Aqaqeer's full instrumentation logic. No Android test source was inspected or changed, no instrumentation APK is invented, and no arbitrary method input or empty success placeholder was added. Porting Mukhtabir's relevant behavioral assertions to real-device instrumentation—including any functional follow-on job, per-case outcomes, assertion-time screenshots and broader Arabic/English/theme/layout scenarios—needs a separately explicit test-file scope. Existing JVM/Robolectric cases are not automatically emulator tests. The workflow, emulator setup, actual UI reachability, image/video quality, ownership cleanup and all behavioral/device acceptance remain unexecuted/unverified.

## Deferred backend and Vue work

These are suggestions for a later authorized task, based on current Git metadata/configuration. No application, migration or test contents were reviewed for this comparison.

| Repository | Evidenced gap / later work |
| --- | --- |
| Vue | `.gitignore` currently contains `src/components.d.ts.env` rather than separate generated-declaration and `.env` entries; it has no standalone `.env`/`.env.*` protection. Correct those rules while retaining an explicit public `.env.example` exception, then inspect tracked paths privately before any cleanup. Ignore changes alone cannot remove historical secrets. |
| Vue | No tracked GitHub workflow/hook/attributes/editor setup was found. Adapt the repository checker to production JS/Vue/locale files and `pnpm-lock.yaml`, preserving the production `features/testCommon` modules. Reuse the actual Node/pnpm pins; do not copy Android wrapper checks or create build/test runs as part of a Git cleanup. |
| Vue documentation | `docs/README.md` links to `guides/Git/Git-GitHub-Start-Here.md`, which is absent in the current checkout. Repair the link or supply a focused guide when frontend documentation work is resumed. |
| Backend | `.gitignore` already covers `.env`, certificates, uploads, backups and migration directories, but lacks general `.env.*`/private signing-key protections. Review safe example exceptions and existing tracked paths without inspecting migrations or exposing contents. |
| Backend | No tracked GitHub workflow/hook/attributes/editor setup was found. Adapt source/config/link checks around current Python requirements and excluded migrations/private state. Database setup, Django startup/import side effects, suites and deployment remain separate authorized work. |
| Both | Add review templates, private-report guidance and ownership using each repository's actual remote/maintainer policy. Consider grouped Dependabot proposals against existing Python requirement files or Vue's pnpm lockfile; inspect compatibility before accepting changes. No dependency upgrade is implied. |
| Shared release process | Document backend-first rollout and matching-client versions using existing contracts. Keep credentials, database backups, browser bundles, APKs and private evidence out of Git; decide release/signing/storage policy before copying publishing or cleanup automation. |

Remaining activation and verification belong in the [current work queue](../workPlan.md); executed checks and their limits belong in [verification status](../verification/checklist.md).
