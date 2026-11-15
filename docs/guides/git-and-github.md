# Git and GitHub maintenance

## Current setup

The 2026-11-15 review adapted Aqaqeer's repository tooling to Mukhtabir Android. The local remote is `Hjjar-AI/Quiz-Android`, checked-out branch `main`; this does not verify GitHub's current default branch, visibility, billing, rulesets or enabled security features. Aqaqeer's application edits were left untouched. Backend and Vue suggestions below are deferred; neither repository was changed.

| Aqaqeer capability | Mukhtabir disposition |
| --- | --- |
| Signing/private-state/package ignore rules | Added missing private evidence, signing formats, generated packages and CI-output rules to the existing ignore file. Ignore rules do not untrack previously committed files. |
| Text/binary attributes and editor defaults | Added LF text defaults, Windows batch CRLF/whitespace handling, binary assets and editor conventions. No mass renormalization. |
| Repository integrity/credential helper | Adapted to current Mukhtabir wrappers and source layout, with test/migration/private-content exclusions always enabled. Removed Aqaqeer-only catalog/crypto/version assumptions. |
| Pre-push hook | Added optional hook; deletion-only pushes skip it. Checks working-tree source, not outgoing commit objects/history. |
| Manual repository workflow | Added one public-only Ubuntu job, ten-minute timeout, read-only token, full checkout commit pin, no persisted credentials and non-cancelling concurrency. No builds, suites, artifact uploads, caches or scheduled workflow runs. |
| Dependabot | Weekly grouped GitHub Actions proposals, seven-day cooldown, at most five open version-update PRs. No automatic merge; no current dependency version changed. Gradle updates remain deferred. |
| Review routing, issues and security guidance | Added CODEOWNERS for build/repository entry points, PR checklist, bilingual issue forms and a private-reporting policy. CODEOWNERS does not itself enforce review. |
| Build/JVM/lint/emulator/crypto/dependency scans | Deferred pending an explicit execution scope and adaptation to Mukhtabir's current source/toolchain. Aqaqeer flavors, crypto vectors and SDK pins do not apply. |
| Signed debug/production publication, reservations, APK inspection | Deferred. Preserve Mukhtabir's existing version counter/wrappers; production signer, release policy and server compatibility must be established before implementation. Aqaqeer's package, ABI splits, tag registry and signing assumptions cannot be copied. |
| History scanner and actionlint installation/update workflow | Deferred separately from the lightweight checker. Evaluate a pinned tool and exact scope before downloading/running it; source-only exclusions must remain explicit. No full-history secret-scan pass is claimed. |
| Rulesets, protected tags, security settings | Documented below for later owner configuration; no GitHub settings changed. |
| CI/debug-release cleanup | Deferred until this repository actually has retained CI evidence/releases and a defined retention policy. No destructive or scheduled cleanup added. |
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

After the files are committed/pushed and the workflow exists on the repository's default branch, an authorized operator can select **Actions → 01 - Checks - Repository integrity → Run workflow**. [GitHub's manual-workflow instructions](https://docs.github.com/en/actions/how-tos/manage-workflow-runs/manually-run-a-workflow) describe the default-branch requirement. The job skips private repositories to avoid assuming paid runner eligibility. Its summary records the limited source scope; no artifact upload or detailed secret report is produced. Hosted execution has not been performed for this change.

The checkout action uses the [same upstream v7.0.1 commit](https://github.com/actions/checkout/commit/3d3c42e5aac5ba805825da76410c181273ba90b1) reviewed in Aqaqeer. [Dependabot configuration](https://docs.github.com/en/code-security/reference/supply-chain-security/dependabot-options-reference) proposes later action-pin changes for review once the configuration is on the default branch. These bot proposals are distinct from manual Actions dispatch. No Gradle Dependabot job, build, test or auto-merge is configured here.

Before enabling server-side protections, inspect actual GitHub settings and repository eligibility. Useful candidates are secret scanning/push protection where available, restricted default-branch deletion/force-push, review requirements appropriate to the real maintainer team, read-only workflow defaults and protected production tags after a release naming policy exists. Do not require this manual workflow as a PR status check without separately designing its trigger/approval policy. Private vulnerability reporting and CODEOWNERS eligibility still need live verification. This review did not query or change those controls.

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
