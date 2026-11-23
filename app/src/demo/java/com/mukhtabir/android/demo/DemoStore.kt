package com.mukhtabir.android.demo

import android.content.Context
import android.util.AtomicFile
import com.mukhtabir.android.core.AuthorNavigation
import com.mukhtabir.android.core.DownloadedSession
import com.mukhtabir.android.core.LearnerStorage
import com.mukhtabir.android.core.MasterExamRecovery
import com.mukhtabir.android.core.PendingSessionFinish
import com.mukhtabir.android.core.SessionDraft
import com.mukhtabir.android.data.remote.ExamSessionDto
import com.mukhtabir.android.data.remote.OfflineAnswer
import com.mukhtabir.android.data.remote.OfflineBankProgress
import com.mukhtabir.android.data.remote.OfflineMaterial
import com.mukhtabir.android.data.remote.PlannerDto
import com.mukhtabir.android.data.remote.SessionAnswerBaseline
import com.mukhtabir.android.data.remote.SessionResultsDto
import com.mukhtabir.android.data.remote.summaryOnly
import java.io.File
import java.io.FileNotFoundException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal const val SAMPLE_USER_ID = 1L
internal const val SAMPLE_LIMIT = 8

internal enum class DemoFailure { READ, ANSWER, FINISH, UPLOAD }

@Serializable
internal data class DemoSession(
    val session: ExamSessionDto,
    val answers: Map<Int, SessionAnswerBaseline> = emptyMap(),
    val createdAt: String? = session.startedAt,
)

@Serializable
internal data class DemoUpload(
    val packId: String,
    val answers: List<OfflineAnswer>,
    val time: Int,
    val completedAt: String?,
)

@Serializable
internal data class DemoCompletion(
    val result: SessionResultsDto,
    val startedAt: String?,
    val completedAt: String,
    val tag: String?,
    val upload: DemoUpload? = null,
)

@Serializable
internal data class DemoData(
    val bookmarks: Set<Long> = setOf(1L, 3L, 5L),
    val ratings: Map<Long, Int> = emptyMap(),
    val reports: Map<Long, String> = emptyMap(),
    val sessions: Map<String, DemoSession> = emptyMap(),
    val completions: Map<String, DemoCompletion> = emptyMap(),
    val nextHistoryId: Long = 1,
    val planner: PlannerDto? = null,
    val nextPlannerId: Long = 1,
    val draft: SessionDraft? = null,
    val downloaded: DownloadedSession? = null,
    val flags: Map<String, Set<Int>> = emptyMap(),
    val finish: PendingSessionFinish? = null,
    val editor: Map<String, String> = emptyMap(),
    val lastQuestion: Long? = null,
    val offline: List<OfflineMaterial> = emptyList(),
    val offlineProgress: OfflineBankProgress? = null,
)

internal data class DemoConnection(val connected: Boolean = true, val armed: Set<DemoFailure> = emptySet())

/** A single transaction boundary for public fixtures, practice state and recovery markers. */
internal class DemoStore(context: Context, seed: () -> DemoData) : LearnerStorage {
    private val connectionMessage = context.getString(R.string.demo_connection_failed)
    private val file = AtomicFile(File(context.filesDir, "demo-progress.json"))
    private val json = Json { ignoreUnknownKeys = true }
    private var current = runCatching {
        val bytes = try { file.readFully() } catch (missing: FileNotFoundException) {
            if (file.baseFile.exists()) throw missing
            null
        }
        if (bytes == null) seed() else json.decodeFromString(DemoData.serializer(), bytes.decodeToString())
    }
    private var retired = false
    private val _connection = MutableStateFlow(DemoConnection())
    val connection = _connection.asStateFlow()

    @Synchronized
    fun setConnected(connected: Boolean) {
        _connection.value = _connection.value.copy(connected = connected)
    }

    @Synchronized
    fun arm(failure: DemoFailure, armed: Boolean) {
        val values = _connection.value.armed
        _connection.value = _connection.value.copy(armed = if (armed) values + failure else values - failure)
    }

    @Synchronized
    fun <T> readLocal(block: (DemoData) -> T): T {
        check(!retired)
        return block(current.getOrThrow())
    }

    @Synchronized
    fun <T> readOnline(block: (DemoData) -> T): T {
        requireConnection()
        if (consume(DemoFailure.READ)) throw DemoConnectionException(connectionMessage)
        return readLocal(block)
    }

    @Synchronized
    fun changeLocal(block: (DemoData) -> DemoData) {
        val before = readLocal { it }
        val after = block(before)
        if (before != after) save(after)
    }

    @Synchronized
    fun <T> changeOnline(failure: DemoFailure? = null, block: (DemoData) -> Pair<DemoData, T>): T {
        requireConnection()
        val before = readLocal { it }
        val (after, result) = block(before)
        if (before != after) {
            save(after)
            if (failure != null && consume(failure)) throw DemoConnectionException(connectionMessage)
        }
        return result
    }

    @Synchronized
    fun reset(seed: DemoData) {
        check(!retired)
        save(seed)
        _connection.value = DemoConnection()
        retire()
    }

    @Synchronized
    fun retire() { retired = true }

    private fun requireConnection() {
        if (!_connection.value.connected) throw DemoConnectionException(connectionMessage)
    }

    private fun consume(failure: DemoFailure): Boolean {
        if (failure !in _connection.value.armed) return false
        arm(failure, false)
        return true
    }

    private fun save(data: DemoData) {
        val stream = file.startWrite()
        try {
            stream.write(json.encodeToString(DemoData.serializer(), data).encodeToByteArray())
            stream.fd.sync()
            file.finishWrite(stream)
            current = Result.success(data)
        } catch (failure: Exception) {
            file.failWrite(stream)
            throw failure
        }
    }

    private fun <T> read(userId: Long, block: (DemoData) -> T): Result<T> = runCatching {
        check(userId == SAMPLE_USER_ID)
        readLocal(block)
    }

    private fun write(userId: Long, block: (DemoData) -> DemoData): Result<Unit> = runCatching {
        check(userId == SAMPLE_USER_ID)
        changeLocal(block)
    }

    override fun retryBinding(userId: Long) = read(userId) { }
    override fun authorNavigation(userId: Long) = read(userId) { AuthorNavigation(lastQuestionId = it.lastQuestion) }
    override fun recordLastQuestion(userId: Long, id: Long?) = write(userId) { it.copy(lastQuestion = id) }
    override fun readEditor(userId: Long, key: String) = read(userId) { it.editor[key] }
    override fun writeEditor(userId: Long, key: String, value: String) = write(userId) { it.copy(editor = it.editor + (key to value)) }
    override fun clearEditor(userId: Long, key: String) = write(userId) { it.copy(editor = it.editor - key) }
    override fun read(userId: Long) = read(userId) { it.draft }
    override fun write(userId: Long, draft: SessionDraft) = write(userId) { it.copy(draft = draft) }
    override fun clearDraft(userId: Long) = write(userId) { it.copy(draft = null) }
    override fun readDownloadedSession(userId: Long) = read(userId) { it.downloaded }
    override fun writeDownloadedSession(userId: Long, session: DownloadedSession?) = write(userId) { it.copy(downloaded = session) }
    override fun readFlags(userId: Long, sessionId: String) = read(userId) { it.flags[sessionId].orEmpty() }
    override fun writeFlags(userId: Long, sessionId: String, indices: Set<Int>) = write(userId) {
        it.copy(flags = if (indices.isEmpty()) it.flags - sessionId else it.flags + (sessionId to indices))
    }
    override fun readFinish(userId: Long) = read(userId) { it.finish }
    override fun writeFinish(userId: Long, recovery: PendingSessionFinish) = write(userId) { it.copy(finish = recovery) }
    override fun clearFinish(userId: Long) = write(userId) { it.copy(finish = null) }
    override fun readMaster(userId: Long): Result<MasterExamRecovery?> = read(userId) { null }
    override fun readOffline(userId: Long, localOnly: Boolean) = read(userId) { it.offline.map(OfflineMaterial::summaryOnly) }
    override fun readOfflinePack(userId: Long, localOnly: Boolean, id: String) = read(userId) { data ->
        data.offline.first { it.pack.id == id }
    }
    override fun readOfflineBankProgress(userId: Long, localOnly: Boolean) = read(userId) { it.offlineProgress }
    override fun writeOffline(userId: Long, localOnly: Boolean, materials: List<OfflineMaterial>, progress: OfflineBankProgress?) =
        write(userId) { data ->
            val previous = data.offline.associateBy { it.pack.id }
            val retained = materials.map { if (it.summary == null) it else previous.getValue(it.pack.id) }
            data.copy(offline = retained, offlineProgress = progress)
        }
}

internal class DemoConnectionException(message: String) : java.io.IOException(message)
