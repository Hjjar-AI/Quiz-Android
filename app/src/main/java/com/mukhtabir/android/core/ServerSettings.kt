package com.mukhtabir.android.core

import android.content.Context
import com.mukhtabir.android.data.remote.PersistentCookieJar
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** Public connection settings; private storage uses explicit local server profiles. */
class ServerSettings(context: Context) {
    private val context = context.applicationContext
    private val preferences = this.context.getSharedPreferences("server_configuration", Context.MODE_PRIVATE)

    fun read(): String? = preferences.getString("api_url", null)?.let { normalize(it) }

    /** Presentation preference only: the unencrypted connection file remains readable. */
    fun isFileConfigured(): Boolean = preferences.getBoolean("configured_from_file", false)

    /** Three public connection slots; never retain original URIs/names or account data. */
    fun importedFiles(): List<ImportedServerFile> {
        if (!preferences.getBoolean("imported_registry_initialized", false)) {
            return if (isFileConfigured()) read()?.let { listOf(ImportedServerFile(1, it)) } ?: emptyList()
            else emptyList()
        }
        return (1..3).mapNotNull { slot ->
            preferences.getString("imported_api_$slot", null)?.let { normalize(it) }
                ?.let { ImportedServerFile(slot, it) }
        }
    }

    fun activeImportedSlot(): Int? {
        if (!isFileConfigured()) return null
        val saved = importedFiles()
        val selected = preferences.getInt("active_imported_slot", 0)
        return saved.firstOrNull { it.slot == selected && it.address == read() }?.slot
            ?: saved.firstOrNull { it.address == read() }?.slot
    }

    @Synchronized
    fun saveImportedFile(slot: Int, address: String): Result<Unit> = runCatching {
        require(slot in 1..3)
        val url = requireNotNull(normalize(address))
        val retained = importedFiles().associate { it.slot to it.address }.toMutableMap()
        retained[slot] = url
        val editor = preferences.edit().putBoolean("imported_registry_initialized", true)
        for (position in 1..3) {
            retained[position]?.let { editor.putString("imported_api_$position", it) }
                ?: editor.remove("imported_api_$position")
        }
        // Storing/replacing a public slot never changes the active server or private profile.
        check(editor.commit())
    }

    /** Stable local storage namespace; imported metadata never grants private-data access. */
    fun profile(): String = preferences.getString("profile", null) ?: "legacy"

    fun requiresProfileAuthentication(): Boolean = preferences.getBoolean("profile_requires_auth", false)

    fun markProfileAuthenticated(expectedProfile: String, expectedAddress: String?): Result<Unit> = runCatching {
        check(profile() == expectedProfile && read() == expectedAddress)
        check(preferences.edit().putBoolean("profile_requires_auth", false).commit())
    }

    fun revokeProfileAuthentication(expectedProfile: String, expectedAddress: String?): Result<Unit> = runCatching {
        check(profile() == expectedProfile && read() == expectedAddress)
        check(preferences.edit().putBoolean("profile_requires_auth", true).commit())
    }

    private fun addressKey(url: String): String = "address:" + java.security.MessageDigest.getInstance("SHA-256")
        .digest(url.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

    fun save(input: String, sameServer: Boolean = false, fromFile: Boolean = false, importedSlot: Int? = null): Result<Unit> = runCatching {
        val url = requireNotNull(normalize(input))
        if (fromFile && importedSlot != null) {
            require(importedFiles().any { it.slot == importedSlot && it.address == url })
        }
        val previous = read()
        val oldProfile = profile()
        val knownProfile = preferences.getString(addressKey(url), null)
        // Never merge two already distinct profiles by silently orphaning their files.
        require(!sameServer || knownProfile == null || knownProfile == oldProfile)
        val nextProfile = when {
            previous == url || (sameServer && previous != null) -> oldProfile
            knownProfile != null -> knownProfile
            else -> java.util.UUID.randomUUID().toString()
        }
        if (previous != url) {
            StudyReminders(context).clear()
            PersistentCookieJar(context).clearForServerChange().getOrThrow()
            SessionDraftStore(context).revokeForServerChange()
        }
        val editor = preferences.edit().putString("api_url", url)
            .putString("profile", nextProfile).putString(addressKey(url), nextProfile)
            .putBoolean("configured_from_file", fromFile)
        if (fromFile && importedSlot != null) editor.putInt("active_imported_slot", importedSlot)
        else editor.remove("active_imported_slot")
        if (previous != url) editor.putBoolean("profile_requires_auth", true)
        if (previous != null) editor.putString(addressKey(previous), oldProfile)
        check(editor.commit())
    }

    companion object {
        fun normalize(input: String): String? {
            val value = input.trim()
            if (value.length !in 1..2048 || value.any { it.isWhitespace() || it.isISOControl() } ||
                !(value.startsWith("https://", true) || value.startsWith("http://", true))) return null
            val url = value.toHttpUrlOrNull() ?: return null
            if (url.username.isNotEmpty() || url.password.isNotEmpty() || url.query != null || url.fragment != null) return null
            val path = url.encodedPath
            val apiPath = when {
                path == "/" -> "/api/v1/"
                path.endsWith("/api/v1/") -> path
                path.endsWith("/api/v1") -> "$path/"
                else -> return null
            }
            return url.newBuilder().encodedPath(apiPath).build().toString()
        }
    }
}

/** Address is transport data, never a display label. */
data class ImportedServerFile(val slot: Int, val address: String)
