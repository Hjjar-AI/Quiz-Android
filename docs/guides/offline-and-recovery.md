# Offline access and recovery

- **Downloaded ordinary sessions:** encrypted owned snapshots/images and ordered local answers. Study feedback follows a saved answer; exam feedback remains hidden. Explicit Sync reconciles current index/slot and acknowledgement before removing queued data. Pause/server Finish require confirmation; uncertain writes are never automatically replayed.
- **Deadlines:** exam time continues offline; answers must reach the server before its deadline. Only downloaded study can finish on device for later explicit synchronization. Local completion does not establish server History/learning credit.
- **Offline collections:** selected downloads/sync require `tests.start`; full-bank/focused catalogue download additionally requires `tests.download_full_bank`. Without that grant, the server enforces a cumulative selected-question budget (currently 200 distinct IDs); deleting local copies does not reset it. Admin Permissions controls the grant. Interrupted downloads retain checkpoints; text-only copies and explicit update/review are supported.
- **Storage:** immutable encrypted pack files and an encrypted account/server index; no authenticated HTTP disk cache. Freeze completion UUIDs, bodies, answers and occurrence times across retries. Rename/flag/display metadata must not alter signed packs or upload identities. Failed reads/commits retain recovery and block unsafe changes.
- **Offline launch:** eligible retained downloads can open during startup connection checks, after a saved-login read fails, and from sign-in. Recovery keeps Retry visible and groups sign-out and server changes under More options.
- **Access:** ordinary login expiry permits retained local-only downloaded practice; uploads require fresh authentication. Explicit logout, another account or confirmed account expiry revoke/clear private storage. Changing server address retains isolated files but requires reauthentication. Revoking download permission cannot remotely erase an offline device.
- **Drafts:** retain readable content, selections and confirmed results on failure. Compare current server state before explicit retry/discard. Task saved state holds account-checked navigation metadata, not private answer/question bodies; sensitive drafts use memory or existing encrypted recovery. Never log secrets/private responses.

Server contracts: [learning](../../../backend/docs/contracts/learningConsistency.md), [writes/recovery](../../../backend/docs/contracts/writeRecovery.md), [schema readiness](../../../backend/docs/contracts/schemaReadiness.md). Matching deployment/schema must be verified; this guide does not authorize setup or migrations.

## Standalone demo

Mukhtabir Demo opens on the shared Home screen from its own launcher. Sample activity and downloads stay in demo-private storage. Use Demo Connection to simulate going offline or losing a response. [Demo behavior and source ownership](native-ui.md#learner-demo).

## Recovery decisions

| Situation | Required behavior |
| --- | --- |
| Write response is lost | Keep the original intent/body and inspect authoritative state or supported receipts. Offer an explicit decision; never automatically replay. |
| Review/read fails | Keep readable content, drafts and uncertainty. A failed read does not mean empty, deleted or saved. |
| Follow-up refresh fails after confirmed success | Preserve the confirmed outcome; show refresh failure independently. |
| Account/server changes | Cancel stale reads and reject late responses; follow the account/profile storage policy above. Imported metadata is not identity evidence. |
| Pause/Finish/sync | Keep guards and confirmation; failed pause must not exit. Freeze completion identity and answer timestamps across allowed retries. |
| Draft storage fails | Retain in-memory content, surface recovery and block unsafe navigation/mutation until the existing explicit recovery path resolves it. |

Exercise these paths using the [verification checklist](../verification/checklist.md); source review cannot establish server concurrency or device storage behavior.
