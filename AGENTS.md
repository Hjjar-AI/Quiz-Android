# Android

- Start at [docs/README.md](docs/README.md), then read current `docs/workPlan.md` and `docs/workDone.md`; use `docs/reference/functionalityReview.md` for coverage and `docs/verification/manualVerification.md` for pending checks. Dated reviews are historical evidence, not the current queue. “Continue” advances unfinished work, not completed features.
- Match current Django contracts and Vue behavior; gate routes and handlers independently, checking actual ownership. Preserve drafts, selection and confirmed success through read failures; never replay uncertain writes automatically.
- Scope private state to the account, keep sensitive drafts memory-only unless existing encrypted recovery applies, and clean temporary files. Reuse native file APIs, shared components and bilingual resources; preserve RTL, large text and accessibility. Source checks never establish compilation/device correctness.
- Follow [Agents.md](Agents.md) for the approved offline/server configuration policies, fresh SQLite setup distinctions and explicit migration/build restrictions.
