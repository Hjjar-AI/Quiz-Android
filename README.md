# Mukhtabir Android

Native Kotlin/Compose client for Mukhtabir Django: Arabic/English and RTL; Android 6+ (API 23), compile/target SDK 34 and JDK 17. Read [working rules](Agents.md) before editing.

## Documentation map

| Task | Read |
| --- | --- |
| Tools, efficient checks, builds, artifacts, versions and server connection | [Build and setup guide](docs/guides/build-and-setup.md) |
| Features, source ownership, controls, fonts, accessibility and adaptive layouts | [Native UI/source guide](docs/guides/native-ui.md) |
| Download permissions, account/server isolation, drafts and uncertain writes | [Offline and recovery guide](docs/guides/offline-and-recovery.md) |
| Review disposition, current evidence and remaining device/API checks | [Verification status](docs/verification/checklist.md) |
| Shared server contracts | [Learning](../backend/docs/contracts/learningConsistency.md) · [Writes/recovery](../backend/docs/contracts/writeRecovery.md) · [Schema readiness](../backend/docs/contracts/schemaReadiness.md) |
| Vue client documentation | [Frontend index](../frontend/docs/README.md) |

## Current capabilities

- Bundled offline demo; ordinary study/exam/recall; downloaded practice and explicit synchronization; master exams and frozen result review.
- Home/planner/activity, bookmarks, personal History, mistakes/fragile notebooks, analytics, groups, question/case/knowledge browsing.
- Capability-gated authoring, draft editors, imports/exports, moderation and administration; Django remains authoritative.
- Shared accessible controls, Arabic/English resources, themes/density/reading preferences, adaptive rail/panes, bounded images and recovery-aware forms.

## Verification

The supplied user-run debug build for **0.1.9.20261022 (code 10)** succeeded in **4m 57s**, confirming compilation/packaging after the opt-in and role corrections. Device, accessibility, automated-suite and live API verification remain pending; Gradle deprecations are still unspecified. See the [current evidence and remaining checks](docs/verification/checklist.md).

Relevant focused checks and debug builds are permitted under the [toolchain policy](Agents.md#efficient-verification). Reuse installed tools and persistent caches, preserve versions for verification and ask before installing missing requirements. Documentation-only changes need link/consistency checks, not a build.

Maintain one canonical guide per topic. Replace stale status and shrink resolved findings; do not append chat transcripts, dated archives or duplicate completed-work lists. Preserve useful setup/recovery procedures when compacting history.
