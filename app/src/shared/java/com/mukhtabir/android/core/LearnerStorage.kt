package com.mukhtabir.android.core
import com.mukhtabir.android.data.remote.ExamSessionDto
import com.mukhtabir.android.data.remote.SessionQuestionPageDto
import com.mukhtabir.android.data.remote.MasterExamAnswerRequest
import kotlinx.serialization.Serializable

@Serializable
enum class SessionDraftKind { RECALL_TEXT, REVEAL, ANSWER, REFLECTION }

@Serializable
data class PendingSessionFinish(val sessionId: String, val mode: String)

/** Downloaded ordinary session and ordered, unconfirmed answer changes. */
@Serializable
data class DownloadedSession(
    val session: ExamSessionDto,
    val pages: List<SessionQuestionPageDto>,
    val answers: List<SessionDraft> = emptyList(),
    val timerDisabled: Boolean = false,
    val images: Map<String, String> = emptyMap(),
    val locallyFinished: Boolean = false,
    val finishedTimerSeconds: Int = 0,
    val finishedTimerOvertime: Boolean = false,
)

/** Capacity rejection happens before touching encrypted preferences. */
class OfflineCacheCapacityException : IllegalStateException()

/** Last learner destination and at most one unresolved answer, in the existing encrypted account store. */
@Serializable
data class MasterExamRecovery(
    val examId: Long,
    val sessionId: String? = null,
    val draft: MasterExamAnswerRequest? = null,
    val page: SessionQuestionPageDto? = null,
    val finishPending: Boolean = false,
)

/** One unresolved question only, with the last confirmed slot for conflict detection. */
@Serializable
data class SessionDraft(
    val sessionId: String,
    val mode: String,
    val questionId: Long,
    val index: Int,
    val kind: SessionDraftKind,
    val answer: Int? = null,
    val confidence: Int? = null,
    val preAnswer: String = "",
    val errorReason: String? = null,
    val baseAnswer: Int? = null,
    val baseConfidence: Int? = null,
    val basePreAnswer: String? = null,
    val baseErrorReason: String? = null,
) {
    fun matches(page: SessionQuestionPageDto): Boolean = index == page.index && questionId == page.question.id

    fun acknowledged(page: SessionQuestionPageDto): Boolean = when (kind) {
        SessionDraftKind.RECALL_TEXT, SessionDraftKind.REVEAL ->
            page.savedPreAnswer == preAnswer.trim() && !page.question.choicesHidden
        SessionDraftKind.ANSWER -> page.savedAnswer == answer && page.savedConfidence == confidence &&
            (mode != "recall" || page.savedPreAnswer == preAnswer.trim())
        SessionDraftKind.REFLECTION -> page.savedAnswer == answer && page.savedConfidence == confidence &&
            page.savedErrorReason == errorReason
    }

    fun baselineUnchanged(page: SessionQuestionPageDto): Boolean =
        page.savedAnswer == baseAnswer && page.savedConfidence == baseConfidence &&
            page.savedPreAnswer == basePreAnswer && page.savedErrorReason == baseErrorReason
}

interface LearnerStorage {
    fun retryBinding(userId: Long): Result<Unit>
    fun authorNavigation(userId: Long): Result<AuthorNavigation>
    fun recordLastQuestion(userId: Long, id: Long?): Result<Unit>
    fun readEditor(userId: Long, key: String): Result<String?>
    fun writeEditor(userId: Long, key: String, value: String): Result<Unit>
    fun clearEditor(userId: Long, key: String): Result<Unit>
    fun read(userId: Long): Result<SessionDraft?>
    fun write(userId: Long, draft: SessionDraft): Result<Unit>
    fun clearDraft(userId: Long): Result<Unit>
    fun readDownloadedSession(userId: Long): Result<DownloadedSession?>
    fun writeDownloadedSession(userId: Long, session: DownloadedSession?): Result<Unit>
    fun readFlags(userId: Long, sessionId: String): Result<Set<Int>>
    fun writeFlags(userId: Long, sessionId: String, indices: Set<Int>): Result<Unit>
    fun readFinish(userId: Long): Result<PendingSessionFinish?>
    fun writeFinish(userId: Long, recovery: PendingSessionFinish): Result<Unit>
    fun clearFinish(userId: Long): Result<Unit>
    fun readMaster(userId: Long): Result<MasterExamRecovery?>
    fun readOffline(userId: Long, localOnly: Boolean): Result<List<com.mukhtabir.android.data.remote.OfflineMaterial>>
    fun readOfflinePack(userId: Long, localOnly: Boolean, id: String): Result<com.mukhtabir.android.data.remote.OfflineMaterial>
    fun readOfflineBankProgress(userId: Long, localOnly: Boolean): Result<com.mukhtabir.android.data.remote.OfflineBankProgress?>
    fun writeOffline(userId: Long, localOnly: Boolean, materials: List<com.mukhtabir.android.data.remote.OfflineMaterial>,
        progress: com.mukhtabir.android.data.remote.OfflineBankProgress?): Result<Unit>
}
