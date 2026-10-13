# Server address configuration

The app asks for a backend address before sign-in. The connection-file upload and saved-file selection controls appear first; manual address entry appears below them. Enter a phone-accessible HTTP or HTTPS origin, or the full API URL ending in `/api/v1/`, or choose a connection file. A bare origin receives `/api/v1/`; an explicit deployment-prefix API path is retained. Credentials, query parameters and fragments are rejected.

System/English/Arabic language controls appear at the beginning of this screen, before server selection. They use the same stored language preference as Settings. Iris is the initial theme when no theme preference is stored; users can change it in Settings.

The selected address is saved in app preferences, outside Kotlin source. Use **Change server** from sign-in or saved-login recovery to edit it. Changing addresses requires confirmation, durably clears stored cookies, disables reminders, and restarts the app task so old account ViewModels/navigation cannot move to the new address. Encrypted local downloads, pending offline answers and recovery are retained in their local server profile. Cancel keeps the current configuration. Re-saving the same normalized URL preserves private state. For an unfamiliar alternate hostname/IP of the **same backend and account database**, select **Same server, different address** to keep the current local profile. Otherwise an unfamiliar address gets a separate profile; returning to a known address reuses its profile. A known address belonging to another local profile cannot silently merge profiles. Sign in again to the same account before accessing retained data after an address change. Signing in to a different account, explicit logout and confirmed account expiry clear private storage; ordinary online-session expiry retains local-only downloaded study under the [offline policy](offlineQuestionBank.md). Export/import metadata does not authenticate a server or automatically merge profiles.

## Initial build-time value

Copy [server.properties.example](../../server.properties.example) to ignored `Android/server.properties`, then set:

```properties
api.base.url=https://YOUR_PC_ADDRESS:8443/api/v1/
```

For an intentionally HTTP development server:

```properties
api.base.url=http://YOUR_PC_ADDRESS:8000/api/v1/
```

These are placeholders, not a configured host or a claim about your PC's ports. Replace the address/port with your actual listener. Precedence for the initial field is `MUKHTABIR_API_BASE_URL` Gradle property, `server.properties`, `local.properties` (`api.base.url`), then the existing emulator fallback. A previously selected phone address takes precedence at runtime. No build is run by editing these properties; the initial build-time value changes only after an authorized rebuild.

## Phone connecting to a PC

Use the PC's LAN IP or a hostname resolvable from the phone, with both devices on a reachable network. `localhost` refers to the phone; `10.0.2.2` is for the Android emulator. The PC listener must accept LAN connections and its firewall must allow the chosen port. Keep the matching backend revision and required database schema in place before running it.

Django must permit the chosen hostname/IP in `ALLOWED_HOSTS`. The native client sends its configured origin for CSRF-protected writes. Existing normal settings derive CSRF trusted origins from `CORS_ORIGINS`; the SQLite profile also supports `SQLITE_CSRF_TRUSTED_ORIGINS`. Use actual scheme/host/port origins and verify the real login/CSRF/cookie flow. For a direct HTTP listener, secure-only cookies must not be enabled (`USE_HTTPS=False` in the appropriate development configuration); HTTPS needs secure cookies and correct proxy scheme forwarding when a proxy terminates TLS. Do not change the active profile or production cookies blindly.

## HTTP and private/self-signed HTTPS

HTTP is allowed only after the app displays its unencrypted-credentials warning and the user confirms the selected endpoint. Android's network policy allows cleartext, while ApiClient's per-exchange gate restricts HTTP to the explicitly selected HTTP origin, including redirects. A selected HTTPS endpoint cannot downgrade to HTTP, and external HTTP image hosts are blocked.

For private/self-signed HTTPS, install the trusted issuing CA certificate in the phone's Android certificate settings. The app opts into Android's user-installed CA store alongside system CAs. Use a valid server certificate whose Subject Alternative Name matches the PC hostname/IP entered in the app. Hostname/chain validation remain enabled; expired, untrusted or mismatched certificates are not accepted. For a self-signed setup, use an installable CA and issue the matching server certificate from it.

Official reference: [Android network security configuration](https://developer.android.com/privacy-and-security/security-config). Real API 23/newer-device certificate installation and HTTP/TLS behavior remain unverified until an authorized build/device check.

## Deployment status

The server is entered manually or imported at startup and editable through the same screen from Login or signed-in Settings. Settings displays the current address for manual configuration and a generic file-configured label for imported configuration; changing it reuses the confirmation/cleanup flow above.

No PC backend process, database, environment or certificate was changed by this Android implementation. Matching source needs a current schema for `TestHistory.source_session_id` and `OfflineCompletion`. Fresh SQLite setup with app migration folders removed generates the initial schema from current models; existing databases need their authorized upgrade path. Actual schema readiness remains unverified. See [backend startup](../../../backend/docs/guides/START_HERE.md) and [learner launch gates](../guides/learnerLaunch.md).

## Connection files

An administrator with `admin.settings` can use Android **Administration → Settings → Export connection file** or web **System settings → Android connection file → Download connection file**. Edit the export address if recipients need a different LAN IP, hostname or port, then use Android’s document saver or the browser download. Exporting does not change the administrator’s selected server or perform a backend write. Both exporters use the same v1 format. No new backend endpoint/schema is required.

The v1 file is UTF-8, unencrypted JSON. Its only functional value is the validated API URL at `bundle.connection.endpoint.api`; randomized identity strings and nested metadata are decorative, never credentials or a stable server identity. Nesting makes it less immediately visible, not secret. Hiding an imported address in the server controls is a presentation choice: the unencrypted file, stored configuration and network connections still contain the endpoint. Example (replace the placeholder address):

```json
{
  "format": "mukhtabir.connection",
  "version": 1,
  "bundle": {
    "identity": "decorative-id",
    "connection": {
      "identity": "decorative-id",
      "endpoint": { "api": "https://YOUR_SERVER:8443/api/v1/" }
    },
    "metadata": { "identity": "decorative-id", "purpose": "connection convenience" }
  }
}
```

First launch and **Change server** accept this file through the native document picker. Import reads at most 64 KiB, closes the stream, keeps no temporary copy, validates format/version and the existing URL restrictions, and selects a hidden file-based address without filling the manual-entry field. Import stores the validated public connection in the chosen numbered slot; it never connects, activates a server or merges private profiles automatically. The confirmation identifies file-based configuration without displaying its address; existing HTTP/TLS warnings remain in force. File mode offers three numbered connection slots, each with an upload/replace control and a selection control, plus **Enter another address manually**. Manual entry begins blank and never copies the imported address. A persisted configuration-source flag keeps the imported address hidden when reopening Change server, after restart/rotation, and in signed-in Settings; Android admin export also starts with a blank field instead of displaying the imported address. A canceled/invalid/unreadable replacement preserves the selected file/manual field and local data. Multiple exports of the same address and different decorative identities resolve to the same normalized address. For different addresses of one backend, use the explicit same-server choice; this is a user declaration, not automatic backend identity verification.

Local profile names are generated by the app, never accepted from file metadata. Existing installations retain their legacy encrypted store without copying/deleting it; new profiles use separate encrypted preference files excluded by the existing backup rules. Temporary camera/transfer files are still cleaned. A durable reauthentication flag blocks cached offline access after an address change, including if a failed login leaves a cookie before account binding. Archived private data is unavailable without reauthentication and matching account binding; uncertain writes are not replayed by changing an address.

## Three saved imported configurations

Settings retains up to three imported connections as **Connection file 1–3**. **Settings → Server address** lists saved numbered files with **Use connection file** actions; these open the shared setup screen with that file selected for the existing confirmation flow. **Change server** can fill an empty slot or replace one. First launch uses the same three-slot controls. Every slot label/button uses its number, without displaying the address or original filename. The confirmed slot number is remembered separately so identical addresses in different files do not confuse current/selected labels.

Successful import immediately saves only its validated normalized connection address in the selected public slot and selects it as the setup draft. Canceling the setup screen keeps that saved slot but leaves the active server unchanged. A canceled/invalid/unreadable import leaves the old slot and draft intact. A fourth configuration requires explicitly replacing one of the three slots; it cannot add a fourth slot or silently evict another. An existing single file-configured server is presented as slot 1 until the first registry write persists it alongside new entries.

Switching/replacing slots does not delete encrypted study data, recovery or the original documents. Slot replacement changes public connection metadata, not the active server or private profile; the new address becomes active only after confirmation. Known addresses reuse their existing local profiles, and the same-server checkbox still controls unfamiliar alternate addresses. Endpoint changes revoke sign-in/reminders and require account binding as before. The registry retains no file URI, filename, raw JSON or account data and does not need continued access to the original document.

First setup leaves manual input empty instead of copying the build development URL. An actual saved manual address still reopens for editing. The saved/import-file controls precede manual entry, which is labelled at the bottom of that selection area; imported addresses are never copied into the editable field.
