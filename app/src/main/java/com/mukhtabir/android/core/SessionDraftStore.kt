package com.mukhtabir.android.core

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.mukhtabir.android.data.remote.ExamSessionDto
import com.mukhtabir.android.data.remote.SessionQuestionPageDto
import com.mukhtabir.android.data.remote.MasterExamAnswerRequest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
private data class SessionFlags(val sessionId: String, val indices: Set<Int>)

/** Private encrypted storage. Access starts only after authenticated account binding. */
class SessionDraftStore(context: Context) : LearnerStorage {
    private val applicationContext = context.applicationContext
    private val json = Json { ignoreUnknownKeys = true }
    // Capture before a setting change; old instances must never touch a new server's data.
    private val serverSettings = ServerSettings(applicationContext)
    private val serverProfile = serverSettings.profile()
    private val serverAddress = serverSettings.read()
    private val preferences by lazy {
        EncryptedSharedPreferences.create(
            if (serverProfile == "legacy") "mukhtabir_session_drafts" else "mukhtabir_session_drafts_$serverProfile",
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
            applicationContext,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }
    private val offlineFiles by lazy { OfflinePackStorage(applicationContext, serverProfile, preferences) }
    private var downloadedImages: Map<String, String>? = null
    private var activeUserId: Long? = null
    private var authenticatedUserId: Long? = null
    private var authenticatedUserUuid: String? = null
    private var offlineAccessAllowed = true

    @Synchronized
    fun bind(userId: Long, userUuid: String? = null): Result<Unit> {
        authenticatedUserId = userId
        authenticatedUserUuid = userUuid
        activeUserId = null
        offlineAccessAllowed = false
        return runCatching {
            check(serverSettings.profile() == serverProfile && serverSettings.read() == serverAddress)
            if (preferences.getLong("owner", -1L) != userId || (userUuid != null && preferences.getString("owner_uuid", null)?.let {it != userUuid} == true)) {
                clearCameraFiles()
                downloadedImages = null
                check(preferences.edit().clear().putLong("owner", userId).commit())
                offlineFiles.clearFiles()
            }
            if (userUuid != null) check(preferences.edit().putString("owner_uuid", userUuid).commit())
            serverSettings.markProfileAuthenticated(serverProfile, serverAddress).getOrThrow()
            activeUserId = userId
            offlineAccessAllowed = true
        }
    }

    @Synchronized
    override fun retryBinding(userId: Long): Result<Unit> {
        if (authenticatedUserId != userId) return Result.failure(IllegalStateException())
        return bind(userId, authenticatedUserUuid)
    }

    @Synchronized
    override fun authorNavigation(userId: Long): Result<AuthorNavigation> = runCatching {
        check(activeUserId==userId && preferences.getLong("owner",-1L)==userId)
        check(serverSettings.profile()==serverProfile && serverSettings.read()==serverAddress)
        preferences.getString("author_navigation",null)?.let {json.decodeFromString(AuthorNavigation.serializer(),it)} ?: AuthorNavigation()
    }
    @Synchronized
    fun recordAuthorChoices(userId: Long, category: Long?, tags: List<String>): Result<Unit> = runCatching {
        val previous=authorNavigation(userId).getOrThrow()
        val value=previous.copy(categories=(listOfNotNull(category)+previous.categories).distinct().take(5),
            tags=(tags+previous.tags).distinct().take(5))
        check(preferences.edit().putString("author_navigation",json.encodeToString(AuthorNavigation.serializer(),value)).commit())
    }
    @Synchronized
    override fun recordLastQuestion(userId: Long, id: Long?): Result<Unit> = runCatching {
        require(id==null || id>0)
        val value=authorNavigation(userId).getOrThrow().copy(lastQuestionId=id)
        check(preferences.edit().putString("author_navigation",json.encodeToString(AuthorNavigation.serializer(),value)).commit())
    }

    /** Authoring uses the same authenticated account binding and encrypted cleanup policy. */
    @Synchronized
    override fun readEditor(userId: Long, key: String): Result<String?> = runCatching {
        check(activeUserId == userId && preferences.getLong("owner", -1L) == userId)
        check(key.matches(Regex("[a-z_]+-(?:new|[0-9]+|[a-f0-9]{64})")))
        preferences.getString("editor:$key", null)
    }

    @Synchronized
    override fun writeEditor(userId: Long, key: String, value: String): Result<Unit> = runCatching {
        check(activeUserId == userId && preferences.getLong("owner", -1L) == userId)
        check(key.matches(Regex("[a-z_]+-(?:new|[0-9]+|[a-f0-9]{64})")))
        check(value.length <= if (key.startsWith("image-")) 8 * 1024 * 1024 else 512 * 1024)
        val other = preferences.all.filterKeys { it.startsWith("editor:") && it != "editor:$key" }
        check(other.size < 40 && other.values.sumOf { (it as? String)?.length ?: 0 } + value.length <= 24 * 1024 * 1024)
        check(preferences.edit().putString("editor:$key", value).commit())
    }

    @Synchronized
    override fun clearEditor(userId: Long, key: String): Result<Unit> = runCatching {
        check(activeUserId == userId && preferences.getLong("owner", -1L) == userId)
        check(preferences.edit().remove("editor:$key").commit())
    }

    @Synchronized
    fun editorKeys(userId: Long): Result<List<String>> = runCatching {
        check(activeUserId == userId && preferences.getLong("owner", -1L) == userId)
        preferences.all.keys.filter { it.startsWith("editor:") }.map { it.removePrefix("editor:") }.sorted()
    }

    @Synchronized
    override fun read(userId: Long): Result<SessionDraft?> = runCatching {
        check(activeUserId == userId && preferences.getLong("owner", -1L) == userId)
        preferences.getString("draft", null)?.let { json.decodeFromString(SessionDraft.serializer(), it) }
    }

    @Synchronized
    override fun write(userId: Long, draft: SessionDraft): Result<Unit> = runCatching {
        check(activeUserId == userId && preferences.getLong("owner", -1L) == userId)
        check(draft.preAnswer.length <= 1_000 && draft.index >= 0 && draft.questionId > 0)
        // A synchronous commit must succeed before any answer/reveal write starts.
        check(preferences.edit().putString("draft", json.encodeToString(SessionDraft.serializer(), draft)).commit())
    }

    @Synchronized
    override fun clearDraft(userId: Long): Result<Unit> = runCatching {
        check(activeUserId == userId && preferences.getLong("owner", -1L) == userId)
        check(preferences.edit().remove("draft").commit())
    }

    @Synchronized
    override fun readDownloadedSession(userId: Long): Result<DownloadedSession?> = runCatching {
        check(activeUserId == userId && preferences.getLong("owner", -1L) == userId)
        check(serverSettings.profile() == serverProfile && serverSettings.read() == serverAddress)
        preferences.getString("downloaded_session", null)?.let {
            val session = json.decodeFromString(DownloadedSession.serializer(), it)
            val images = preferences.getString("downloaded_images", null)?.let { encoded ->
                json.parseToJsonElement(encoded).jsonObject.mapValues { (_, value) -> value.jsonPrimitive.content }
            } ?: session.images
            downloadedImages = images
            session.copy(images = images)
        }
    }

    @Synchronized
    override fun writeDownloadedSession(userId: Long, session: DownloadedSession?): Result<Unit> = runCatching {
        check(activeUserId == userId && preferences.getLong("owner", -1L) == userId)
        check(serverSettings.profile() == serverProfile && serverSettings.read() == serverAddress)
        val editor = preferences.edit()
        if (session == null) {
            editor.remove("downloaded_session").remove("downloaded_images")
        } else {
            // Immutable image data is written once, rather than re-encrypted for every answer.
            val encoded = json.encodeToString(DownloadedSession.serializer(), session.copy(images = emptyMap()))
            check(encoded.length + session.images.values.sumOf { it.length } <= 16 * 1024 * 1024)
            editor.putString("downloaded_session", encoded)
            if (downloadedImages != session.images) editor.putString("downloaded_images",
                JsonObject(session.images.mapValues { JsonPrimitive(it.value) }).toString())
        }
        check(editor.commit())
        downloadedImages = session?.images
    }

    @Synchronized
    override fun readFlags(userId: Long, sessionId: String): Result<Set<Int>> = runCatching {
        check(activeUserId == userId && preferences.getLong("owner", -1L) == userId)
        check(serverSettings.profile() == serverProfile && serverSettings.read() == serverAddress)
        val stored = preferences.getString("session_flags_$sessionId", null)?.let { json.decodeFromString(SessionFlags.serializer(), it) }
        if (stored?.sessionId == sessionId) stored.indices.filter { it >= 0 }.toSet() else emptySet()
    }

    @Synchronized
    override fun writeFlags(userId: Long, sessionId: String, indices: Set<Int>): Result<Unit> = runCatching {
        check(activeUserId == userId && preferences.getLong("owner", -1L) == userId)
        check(serverSettings.profile() == serverProfile && serverSettings.read() == serverAddress)
        check(sessionId.isNotBlank() && indices.all { it >= 0 })
        check(preferences.edit().putString("session_flags_$sessionId", json.encodeToString(SessionFlags.serializer(),
            SessionFlags(sessionId, indices))).commit())
    }

    @Synchronized
    override fun readFinish(userId: Long): Result<PendingSessionFinish?> = runCatching {
        check(activeUserId == userId && preferences.getLong("owner", -1L) == userId)
        preferences.getString("finish_recovery", null)?.let {
            json.decodeFromString(PendingSessionFinish.serializer(), it).also(::validateFinish)
        }
    }

    @Synchronized
    override fun writeFinish(userId: Long, recovery: PendingSessionFinish): Result<Unit> = runCatching {
        check(activeUserId == userId && preferences.getLong("owner", -1L) == userId)
        validateFinish(recovery)
        check(preferences.edit().putString("finish_recovery",
            json.encodeToString(PendingSessionFinish.serializer(), recovery)).commit())
    }

    @Synchronized
    override fun clearFinish(userId: Long): Result<Unit> = runCatching {
        check(activeUserId == userId && preferences.getLong("owner", -1L) == userId)
        check(preferences.edit().remove("finish_recovery").commit())
    }

    private fun validateFinish(recovery: PendingSessionFinish) {
        check(recovery.sessionId.isNotBlank() && recovery.sessionId.length<=36 &&
            recovery.mode in setOf("exam","study","recall"))
    }

    @Synchronized
    override fun readMaster(userId: Long): Result<MasterExamRecovery?> = runCatching {
        check(activeUserId == userId && preferences.getLong("owner", -1L) == userId)
        preferences.getString("master_recovery", null)?.let {
            json.decodeFromString(MasterExamRecovery.serializer(), it).also(::validateMaster)
        }
    }

    @Synchronized
    fun writeMaster(userId: Long, recovery: MasterExamRecovery): Result<Unit> = runCatching {
        check(activeUserId == userId && preferences.getLong("owner", -1L) == userId)
        validateMaster(recovery)
        check(preferences.edit().putString("master_recovery",
            json.encodeToString(MasterExamRecovery.serializer(), recovery)).commit())
    }

    @Synchronized
    fun clearMaster(userId: Long): Result<Unit> = runCatching {
        check(activeUserId == userId && preferences.getLong("owner", -1L) == userId)
        check(preferences.edit().remove("master_recovery").commit())
    }

    private fun validateMaster(recovery: MasterExamRecovery) {
        check(recovery.examId>0)
        recovery.draft?.let { draft ->
            val page=requireNotNull(recovery.page)
            check(!recovery.sessionId.isNullOrBlank() && draft.questionId>0 && page.index>=0 &&
                page.question.id==draft.questionId && draft.answer in 1..page.question.choices.size && draft.confidence in 1..3)
        } ?: check(recovery.page==null &&
            (if(recovery.finishPending) !recovery.sessionId.isNullOrBlank() else recovery.sessionId==null))
    }

    /** Last bound account's downloaded study remains usable after online expiry. */
    @Synchronized
    fun cachedOfflineOwner(): Result<Long?> = runCatching {
        if (serverSettings.profile() != serverProfile || serverSettings.read() != serverAddress ||
            serverSettings.requiresProfileAuthentication() || !offlineAccessAllowed) return@runCatching null
        preferences.getLong("owner", -1).takeIf { it > 0 && (preferences.contains("offline_material") || preferences.contains("offline_index")) }
    }

    @Synchronized
    override fun readOffline(userId: Long, localOnly: Boolean): Result<List<com.mukhtabir.android.data.remote.OfflineMaterial>> = runCatching {
        checkOfflineOwner(userId, localOnly)
        offlineFiles.read()
    }

    @Synchronized
    override fun readOfflinePack(userId: Long, localOnly: Boolean, id: String): Result<com.mukhtabir.android.data.remote.OfflineMaterial> = runCatching {
        checkOfflineOwner(userId, localOnly)
        offlineFiles.readPack(id)
    }

    @Synchronized
    override fun readOfflineBankProgress(userId: Long, localOnly: Boolean): Result<com.mukhtabir.android.data.remote.OfflineBankProgress?> = runCatching {
        checkOfflineOwner(userId, localOnly)
        offlineFiles.progress()
    }

    @Synchronized
    override fun writeOffline(userId: Long, localOnly: Boolean, materials: List<com.mukhtabir.android.data.remote.OfflineMaterial>,
        progress: com.mukhtabir.android.data.remote.OfflineBankProgress?): Result<Unit> = runCatching {
        checkOfflineOwner(userId, localOnly)
        offlineFiles.write(materials, progress)
    }

    private fun checkOfflineOwner(userId: Long, localOnly: Boolean) {
        check(userId > 0 && preferences.getLong("owner", -1) == userId)
        if (localOnly) {
            check(serverSettings.profile() == serverProfile && serverSettings.read() == serverAddress)
            check(!serverSettings.requiresProfileAuthentication() && offlineAccessAllowed && (authenticatedUserId == null || authenticatedUserId == userId))
        }
        else check(activeUserId == userId && authenticatedUserId == userId)
    }

    private fun clearCameraFiles() {
        runCatching {
            java.io.File(applicationContext.cacheDir, "question-camera").listFiles()?.forEach { it.delete() }
            applicationContext.cacheDir.listFiles()?.filter { it.name.startsWith("exam-matrix-") && it.isFile }?.forEach { it.delete() }
        }
    }

    /** Lock online drafts/actions while keeping encrypted offline work and ownership. */
    @Synchronized
    fun expireOnlineSession() {
        activeUserId = null
        authenticatedUserId = null
        authenticatedUserUuid = null
        clearCameraFiles()
    }

    @Synchronized
    fun revokeForServerChange() {
        activeUserId = null
        authenticatedUserId = null
        authenticatedUserUuid = null
        offlineAccessAllowed = false
        clearCameraFiles()
    }

    @Synchronized
    fun clear() {
        downloadedImages = null
        activeUserId = null
        authenticatedUserId = null
        authenticatedUserUuid = null
        offlineAccessAllowed = false
        // Keep logout/account-expiry revocation across process restart even if
        // encrypted-file cleanup fails. A later matching login clears this gate.
        serverSettings.revokeProfileAuthentication(serverProfile, serverAddress)
        // Account binding still checks persisted ownership if cleanup is unavailable.
        runCatching { check(preferences.edit().clear().commit()); offlineFiles.clearFiles() }
        clearCameraFiles()
    }
}
