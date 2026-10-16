package com.mukhtabir.android.data.remote

import com.mukhtabir.android.core.has
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class MasterExamDto(
    val id: Long,
    val name: String,
    @SerialName("is_owner") val isOwner: Boolean = false,
    val description: String = "",
    val instructions: String = "",
    val status: String = "",
    @SerialName("stored_status") val storedStatus: String? = null,
    @SerialName("opens_at") val opensAt: String,
    @SerialName("closes_at") val closesAt: String,
    @SerialName("duration_minutes") val durationMinutes: Int,
    @SerialName("question_count") val questionCount: Int = 0,
    @SerialName("allow_makeup") val allowMakeup: Boolean = false,
    @SerialName("has_acknowledged") val hasAcknowledged: Boolean = false,
    @SerialName("my_attempt") val myAttempt: MasterExamAttemptDto? = null,
)

@Serializable
data class MasterExamListDto(val items: List<MasterExamDto> = emptyList(), val total: Int = 0)

@Serializable
data class MasterExamNoticesDto(val items: List<MasterExamDto> = emptyList(), val count: Int = 0)

@Serializable
data class MasterExamAttemptDto(
    val id: Long,
    @SerialName("exam_name") val examName: String = "",
    @SerialName("session_id") val sessionId: String,
    @SerialName("is_complete") val isComplete: Boolean = false,
    @SerialName("is_makeup") val isMakeup: Boolean = false,
    @SerialName("forced_finish") val forcedFinish: Boolean = false,
    @SerialName("correct_count") val correctCount: Int = 0,
    @SerialName("total_questions") val totalQuestions: Int = 0,
    @SerialName("answered_count") val answeredCount: Int = 0,
    val accuracy: Double = 0.0,
    @SerialName("weighted_score") val weightedScore: Double = 0.0,
    @SerialName("started_at") val startedAt: String,
    @SerialName("finished_at") val finishedAt: String? = null,
)

@Serializable
data class MasterExamStatusDto(
    @SerialName("attempt_id") val attemptId: Long,
    @SerialName("session_id") val sessionId: String,
    @SerialName("is_complete") val isComplete: Boolean,
    @SerialName("is_makeup") val isMakeup: Boolean = false,
    @SerialName("question_ids") val questionIds: List<Long> = emptyList(),
    val answers: Map<String, JsonObject> = emptyMap(),
    @SerialName("current_question_id") val currentQuestionId: Long? = null,
    @SerialName("deadline_at") val deadlineAt: String,
    @SerialName("server_now") val serverNow: String,
    @SerialName("grace_seconds") val graceSeconds: Int = 180,
)

@Serializable
data class MasterExamAnswerRequest(
    @SerialName("question_id") val questionId: Long,
    val answer: Int,
    val confidence: Int = 3,
    @SerialName("expected_slot") val expectedSlot: JsonObject? = null,
    @SerialName("session_id") val sessionId: String? = null,
)

/** Learner participation and versioned author operations; learner results still come only from my_attempt. */
class MasterExamService(private val api: ApiClient) {
    suspend fun list(): MasterExamListDto = api.decode(MasterExamListDto.serializer(), api.get(ApiEndpoints.MasterExams.BASE))
    suspend fun authorDetail(id: Long): MasterExamAuthorDto = api.decode(
        MasterExamAuthorDto.serializer(), api.get(ApiEndpoints.MasterExams.item(id)))

    suspend fun createExam(request: MasterExamMetadataWrite): MasterExamAuthorDto = api.decode(
        MasterExamAuthorDto.serializer(), api.post(ApiEndpoints.MasterExams.BASE,
            api.encode(MasterExamMetadataWrite.serializer(), request)))

    suspend fun updateExam(id: Long, version: Int, request: MasterExamMetadataWrite): MasterExamAuthorDto {
        val data = api.encode(MasterExamMetadataWrite.serializer(), request) as JsonObject
        return api.decode(MasterExamAuthorDto.serializer(), api.put(ApiEndpoints.MasterExams.item(id),
            JsonObject(data + ("expected_version" to JsonPrimitive(version)))))
    }

    suspend fun deleteExam(id: Long, mode: String) {
        require(mode in setOf("keep_drafts", "delete_drafts", "publish_drafts"))
        api.delete(ApiEndpoints.MasterExams.item(id), buildJsonObject { put("delete_mode", mode) })
    }

    suspend fun compose(id: Long, version: Int, action: String, ids: List<Long>): MasterExamAuthorDto {
        require(action in setOf("add", "remove", "reorder"))
        return api.decode(MasterExamAuthorDto.serializer(), api.post(
            ApiEndpoints.MasterExams.action(id, "questions/$action"), buildJsonObject {
                put("expected_version", version)
                if (action == "remove") put("question_id", ids.single())
                else put("question_ids", JsonArray(ids.map { JsonPrimitive(it) }))
            }))
    }

    // The draft-create endpoint advances the exam revision but accepts no expected_version.
    suspend fun addDraft(id: Long, request: MasterExamDraftWrite): QuestionDto = api.decode(
        QuestionDto.serializer(), api.post(ApiEndpoints.MasterExams.action(id, "drafts"),
            api.encode(MasterExamDraftWrite.serializer(), request)))

    suspend fun drafts(search: String, usage: String): QuestionBatchDto = api.decode(
        QuestionBatchDto.serializer(), api.get(ApiEndpoints.MasterExams.BASE + "drafts/", mapOf("search" to search, "usage" to usage)))
    suspend fun draft(id: Long): QuestionDto = api.decode(QuestionDto.serializer(), api.get(ApiEndpoints.MasterExams.BASE + "drafts/$id/"))
    suspend fun updateDraft(id: Long, version: Int, request: MasterExamDraftWrite): QuestionDto {
        val data = api.encode(MasterExamDraftWrite.serializer(), request) as JsonObject
        return api.decode(QuestionDto.serializer(), api.put(ApiEndpoints.MasterExams.BASE + "drafts/$id/",
            JsonObject(data + ("expected_version" to JsonPrimitive(version)))))
    }
    suspend fun deleteDraft(id: Long, version: Int) { api.delete(ApiEndpoints.MasterExams.BASE + "drafts/$id/?expected_version=$version") }
    suspend fun lifecycle(id: Long, action: String): MasterExamAuthorDto {
        require(action in setOf("publish", "cancel", "publish-to-bank"))
        return api.decode(MasterExamAuthorDto.serializer(), api.post(ApiEndpoints.MasterExams.action(id, action)))
    }
    suspend fun preview(id: Long): ExamPreviewDto = api.decode(ExamPreviewDto.serializer(),
        api.post(ApiEndpoints.MasterExams.action(id, "start"), buildJsonObject { put("preview", true) }))
    fun resultsExam(result: JsonObject): MasterExamAuthorDto = api.decode(MasterExamAuthorDto.serializer(), requireNotNull(result["exam"]))
    suspend fun results(id: Long): JsonObject = api.get(ApiEndpoints.MasterExams.action(id, "results")) as JsonObject

    suspend fun notices(): MasterExamNoticesDto = api.decode(MasterExamNoticesDto.serializer(), api.get(ApiEndpoints.MasterExams.NOTICES))
    suspend fun detail(id: Long): MasterExamDto = api.decode(MasterExamDto.serializer(), api.get(ApiEndpoints.MasterExams.item(id)))
    suspend fun acknowledge(id: Long) { api.post(ApiEndpoints.MasterExams.action(id,"acknowledge")) }
    suspend fun start(id: Long) { api.post(ApiEndpoints.MasterExams.action(id,"start"),buildJsonObject { put("preview",false) }) }
    suspend fun status(id: Long): MasterExamStatusDto = api.decode(MasterExamStatusDto.serializer(), api.get(ApiEndpoints.MasterExams.attempt(id,"status")))
    suspend fun question(id: Long): SessionQuestionPageDto = api.decode(SessionQuestionPageDto.serializer(), api.get(ApiEndpoints.MasterExams.attempt(id,"question")))
    suspend fun answer(id: Long, request: MasterExamAnswerRequest) {
        api.post(ApiEndpoints.MasterExams.attempt(id,"answer"),JsonObject((api.encode(MasterExamAnswerRequest.serializer(),request) as JsonObject) + ("expected_slot" to (request.expectedSlot ?: kotlinx.serialization.json.JsonNull))))
    }
    suspend fun goto(id: Long, questionId: Long, sessionId: String, currentId: Long?) { api.post(ApiEndpoints.MasterExams.attempt(id,"goto"),buildJsonObject { put("question_id",questionId);put("session_id",sessionId);put("expected_current_question_id",currentId?.let(::JsonPrimitive) ?: kotlinx.serialization.json.JsonNull) }) }
    suspend fun finish(id: Long): MasterExamAttemptDto = api.decode(MasterExamAttemptDto.serializer(),api.post(ApiEndpoints.MasterExams.attempt(id,"finish")))
    suspend fun flag(id: Long, questionId: Long, reason: String) {
        api.post(ApiEndpoints.MasterExams.attempt(id,"flag"),buildJsonObject { put("question_id",questionId);put("reason",reason) })
    }
}

@Serializable
data class ExamPreviewDto(
    @SerialName("is_preview") val isPreview: Boolean,
    @SerialName("master_exam_id") val examId: Long,
    @SerialName("exam_name_snapshot") val name: String,
    val questions: List<ExamPreviewQuestionDto> = emptyList(),
)

fun canManageExam(user: UserDto, exam: MasterExamAuthorDto): Boolean = user.has("master_exams.manage_any") ||
    (user.has("master_exams.manage_own") && isExamAuthor(user, exam))
fun isExamAuthor(user: UserDto, exam: MasterExamAuthorDto): Boolean = exam.primaryAttending == user.id || exam.coAttendings.any { it.id == user.id }
fun canViewExamResults(user: UserDto, exam: MasterExamAuthorDto): Boolean = user.has("master_exams.view_results_any") ||
    canManageExam(user, exam) || (user.has("master_exams.view_results_own") && isExamAuthor(user, exam))

@Serializable
data class ExamPreviewQuestionDto(val id: Long, val text: String, val choices: List<String> = emptyList(),
    @SerialName("correct_answer") val correctAnswer: Int? = null, val explanation: String? = null,
    @SerialName("image_url") val imageUrl: String? = null, val case: ClinicalCaseContextDto? = null,
    val translations: JsonObject = JsonObject(emptyMap())) {
    fun question() = QuestionDto(id = id, question = text, choices = choices, correctAnswer = correctAnswer,
        explanation = explanation, imageUrl = imageUrl, case = case, translations = translations)
}
