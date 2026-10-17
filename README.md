# Mukhtabir Android

Native Kotlin/Compose client for Mukhtabir Django. Arabic/English, RTL, themes and density settings; Android 6+ (API 23), SDK 34, Java 17. Contributor rules: [Agents.md](Agents.md).

## Features

- Offline first-launch demo: bundled science questions, quick study, timed exam, recall and result review; no account/server/download required. Demo progress is separate from private learning.
- Ordinary study/exam/recall, book/category/tag selection, shared practice launcher, targeted practice from results, confidence/reflection, session flags/maps and final review. Bilingual comparison respects answer/explanation reveal rules.
- Reading comfort: saved text size, line/paragraph spacing, reading width and app font; offline study preview and independent reading reset.
- Continue learning, daily planner progress, downloaded collections, bookmarks, personal History, mistakes, fragile knowledge, analytics, groups and knowledge/case browsing.
- Filter labels explain any-tag matching and combined restrictions. Import file cards start collapsed with status, reports/error details and explicit uncertain-outcome review; destructive actions name what they remove.
- Capability-gated authoring, private drafts, master exams, moderation and administration. Django is authoritative; personal History always uses the signed-in user, and all-user history requires its separate capability.

## Setup

Use JDK 17 and SDK 34; set `ANDROID_HOME` or ignored `local.properties` (`sdk.dir`). Use the included wrapper. With explicit build authorization:

```bash
./gradlew assembleDebug
./gradlew installDebug
```

First launch offers demo or server setup. Enter a reachable HTTP/HTTPS origin or full deployment-prefix `/api/v1/` URL. Manual input starts blank; saved manual addresses reopen. Emulator host access uses `10.0.2.2`; a phone needs a reachable LAN address or USB `adb reverse tcp:PORT tcp:PORT` with its actual backend port. Phone `localhost` means the phone.

A build-time URL can come from `MUKHTABIR_API_BASE_URL`, ignored `server.properties` (`api.base.url`), then `local.properties`; this does not replace the phone-selected address. See [server.properties.example](server.properties.example).

HTTP requires confirmation and is restricted to the selected origin; HTTPS cannot downgrade. Private HTTPS needs a trusted Android-installed CA and a certificate matching the host/IP. Never disable chain/hostname validation. Align Django hosts, CSRF origins, cookies and proxy scheme with the actual endpoint: [backend startup](../backend/docs/guides/START_HERE.md).

### Connection files

Admin Settings can export v1 JSON; the endpoint is `bundle.connection.endpoint.api`. Files are unencrypted convenience data, not credentials or trusted server identities. Import accepts at most 64 KiB and retains three numbered slots without original filenames/URIs. Imported addresses stay hidden in controls; manual entry never copies them. Saving/replacing a slot does not activate it; switching requires confirmation.

Address changes clear sign-in/reminders, restart navigation and require reauthentication. Encrypted files remain in their local server profile. Only explicit **Same server, different address** associates an unfamiliar alternate address with that profile; imported identities never merge profiles.

## Offline and recovery

- **Downloaded ordinary sessions:** encrypted owned snapshots/images and ordered local answers. Study feedback follows a saved answer; exam feedback remains hidden. Explicit Sync reconciles current index/slot and acknowledgement before removing queued data. Pause/server Finish require confirmation; uncertain writes are never automatically replayed.
- **Deadlines:** exam time continues offline; answers must reach the server before its deadline. Only downloaded study can finish on device for later explicit synchronization. Local completion does not establish server History/learning credit.
- **Offline collections:** selected downloads/sync require `tests.start`; full-bank/focused catalogue download additionally requires `tests.download_full_bank`. Without that grant, the server enforces a cumulative selected-question budget (currently 200 distinct IDs); deleting local copies does not reset it. Admin Permissions controls the grant. Interrupted downloads retain checkpoints; text-only copies and explicit update/review are supported.
- **Storage:** immutable encrypted pack files and an encrypted account/server index; no authenticated HTTP disk cache. Freeze completion UUIDs, bodies, answers and occurrence times across retries. Rename/flag/display metadata must not alter signed packs or upload identities. Failed reads/commits retain recovery and block unsafe changes.
- **Access:** ordinary login expiry permits retained local-only downloaded practice; uploads require fresh authentication. Explicit logout, another account or confirmed account expiry revoke/clear private storage. Changing server address retains isolated files but requires reauthentication. Revoking download permission cannot remotely erase an offline device.
- **Drafts:** retain readable content, selections and confirmed results on failure. Compare current server state before explicit retry/discard. Task saved state holds account-checked navigation metadata, not private answer/question bodies; sensitive drafts use memory or existing encrypted recovery. Never log secrets/private responses.

Server contracts: [learning](../backend/docs/contracts/learningConsistency.md), [writes/recovery](../backend/docs/contracts/writeRecovery.md), [schema readiness](../backend/docs/contracts/schemaReadiness.md). Matching deployment/schema must be verified; this README does not authorize setup or migrations.

## Development

`app/src/main/java/com/mukhtabir/android/`: `core` owns storage/preferences/navigation/container, `data` owns auth/HTTP/DTOs/services, `ui` owns Compose screens/shared controls, and `util` owns formatting. Keep endpoints aligned with current Django and applicable Vue behavior. Runtime `/config/` supplies limits, pagination and role options with safe fallbacks; optional reads must not block startup.

Reuse shared controls/tokens, session chrome, practice launcher, result filters, question maps/cards and locale resolver. Keep 48dp targets, selected/non-color states, keyboard focus, RTL, large text, reduced motion and existing fonts/themes. Announce errors/outcomes, never ticking clocks. Theme/palette changes must coordinate web/PDF exports. Frozen results and reveal restrictions remain authoritative; Bookmarks filters explicitly apply to the loaded page. Missing translations/source dates must be labelled rather than invented.

Launcher master: [artwork/app_icon.png](artwork/app_icon.png); monochrome icon: [ic_launcher_monochrome.xml](app/src/main/res/drawable/ic_launcher_monochrome.xml). Regenerate with `python3 artwork/generate_launcher_icons.py` (FFmpeg) or `python3 artwork/generate_startup_wordmark.py` (Fontconfig/Pango/Cairo). Labels remain **مُختبِر**; asset conversion does not verify an installed launcher/cache.

Useful diagnostics: `adb devices -l`, `adb shell pidof com.mukhtabir.android`, `adb logcat --pid=PID -v threadtime`. Release signing stays outside version control; verify minified serialization/navigation before distribution.

## Verification

User-run `assembleDebug` succeeded. Its seven Kotlin warnings are cleaned up in source, pending rebuild confirmation. Source/resource/whitespace checks pass; device behavior remains unverified:

1. Build/install current source; confirm matching backend/schema, HTTPS/HTTP/CSRF, account/capability freshness and server/profile isolation.
2. Cold-launch demo in airplane mode; exercise all modes, timers, restart/rotation/process restoration and exit/reset.
3. Exercise book filters, shared launchers, targeted results, flags/maps, bilingual reading and withheld exam/recall content; verify stale/failed reads and retained selections.
4. Interrupt answer/start/pause/Finish/sync, lose responses and fail storage; review server state explicitly, retain drafts/confirmed results, reject duplicate/late writes and preserve completion identities. Include master grace, private-draft revisions and overlapping exams.
5. Verify downloaded-bank permissions/checkpoints, legacy copies, titles/dates, rename recovery, local completion, explicit sync, logout/account/server switches and no duplicate learning/History credit.
6. Check planner day/midnight attribution and overlapping review counts; collapsed import cards/status/errors/review, filter matching, specific destructive labels, merges/editor revision conflicts and partial outcomes; exports/administration permissions.
7. Check reading controls/reset/persistence and study preview; English/Arabic/RTL, themes/density/fonts, narrow/tablet screens, 200% text, keyboard/system insets, TalkBack, reduced motion, Markdown/images, haptics and launcher rendering on API 23 and a current device. Measure large-bank/import performance.

Keep this README current: replace stale status instead of appending dated logs, review archives or duplicate checklists.
