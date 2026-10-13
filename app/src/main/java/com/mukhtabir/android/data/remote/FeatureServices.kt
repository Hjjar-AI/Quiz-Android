package com.mukhtabir.android.data.remote

import okhttp3.MultipartBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ConfigService(private val api: ApiClient) {
    suspend fun getConfig(): PublicConfigDto =
        api.decode(PublicConfigDto.serializer(), api.get(ApiEndpoints.CONFIG))
}

/**
 * Tips for the dashboard "Did you know?" card.
 *
 * The backend accepts `?locale=ar|en`, returns the requested locale's
 * tips, and echoes the resolved locale in the payload. When the
 * requested locale has no active tips the backend falls back to the
 * default locale and reports that in the `locale` field — the client
 * trusts the returned list rather than re-checking the locale.
 */
class TipsService(private val api: ApiClient) {
    suspend fun forLocale(locale: String): TipsResponseDto = api.decode(
        TipsResponseDto.serializer(),
        api.get(
            ApiEndpoints.TIPS,
            mapOf("locale" to locale),
        ),
    )
}

class QuestionService(private val api: ApiClient) {
    suspend fun batch(ids: List<Long>): QuestionBatchDto {
        require(ids.size <= 500)
        return api.decode(QuestionBatchDto.serializer(), api.post(ApiEndpoints.Questions.BATCH,
            buildJsonObject { put("ids", JsonArray(ids.map { JsonPrimitive(it) })) }))
    }

    suspend fun get(questionId: Long): QuestionDto = api.decode(
        QuestionDto.serializer(),
        api.get(ApiEndpoints.Questions.item(questionId)),
    )

    suspend fun uploadImage(id: Long, bytes: ByteArray, mime: String, filename: String): QuestionImageResultDto =
        api.decode(QuestionImageResultDto.serializer(),api.postMultipart(ApiEndpoints.Questions.image(id),
            MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("image",filename,bytes.toRequestBody(mime.toMediaType())).build()))

    suspend fun removeImage(id: Long): QuestionImageResultDto =
        api.decode(QuestionImageResultDto.serializer(),api.postMultipart(ApiEndpoints.Questions.image(id),
            MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart("delete","true").build()))

    suspend fun sourceBookFilters(name: String): SourceBookFiltersDto = api.decode(
        SourceBookFiltersDto.serializer(), api.get("questions/source-book-filters/", mapOf("source_document" to name)))

    suspend fun sourceBooks(): SourceBookListDto =
        api.decode(SourceBookListDto.serializer(), api.get("questions/source-books/"))

    suspend fun tags(): TagListDto =
        api.decode(TagListDto.serializer(), api.get(ApiEndpoints.Questions.TAGS))

    suspend fun list(
        page: Int = 1,
        perPage: Int = 20,
        search: String? = null,
        categoryId: Long? = null,
        difficulty: String? = null,
        verifiedOnly: Boolean = false,
        tag: String? = null,
    ): QuestionPageDto = api.decode(
        QuestionPageDto.serializer(),
        api.get(
            ApiEndpoints.Questions.BASE,
            mapOf(
                "page" to page.toString(),
                "per_page" to perPage.toString(),
                "search" to search,
                "category" to categoryId?.toString(),
                "difficulty" to difficulty,
                "verified" to "yes".takeIf { verifiedOnly },
                "tag" to tag,
            ),
        ),
    )

    suspend fun unverified(page: Int = 1, perPage: Int = 20): QuestionPageDto = api.decode(
        QuestionPageDto.serializer(),
        api.get(
            ApiEndpoints.Questions.UNVERIFIED,
            mapOf("page" to page.toString(), "per_page" to perPage.toString()),
        ),
    )

    suspend fun availableCount(
        categoryIds: List<Long> = emptyList(),
        difficulty: String? = null,
        verifiedOnly: Boolean = false,
        useBookmarks: Boolean = false,
        useSrs: Boolean = false,
        tags: List<String> = emptyList(),
        blueprintId: Long? = null,
        sourceDocument: String? = null,
    ): Int = api.decode(
        CountDto.serializer(),
        api.get(
            ApiEndpoints.Questions.AVAILABLE_COUNT,
            mapOf(
                "category_ids" to categoryIds.takeIf { it.isNotEmpty() }?.joinToString(","),
                "difficulty" to difficulty,
                "verified_only" to verifiedOnly.takeIf { it }?.toString(),
                "use_bookmarks" to useBookmarks.takeIf { it }?.toString(),
                "use_srs" to useSrs.takeIf { it }?.toString(),
                "tags_filter" to tags.takeIf { it.isNotEmpty() }?.joinToString(","),
                "blueprint_id" to blueprintId?.toString(),
                "source_document" to sourceDocument,
            ),
        ),
    ).count

    suspend fun attemptSummary(): AttemptSummaryDto =
        api.decode(AttemptSummaryDto.serializer(), api.get(ApiEndpoints.Questions.ATTEMPT_SUMMARY))

    suspend fun srsDueCount(): Int =
        api.decode(CountDto.serializer(), api.get(ApiEndpoints.Questions.SRS_DUE_COUNT)).count

    suspend fun mistakes(page: Int = 1, perPage: Int = 20): QuestionPageDto = api.decode(
        QuestionPageDto.serializer(),
        api.get(
            ApiEndpoints.Questions.MISTAKES,
            mapOf("page" to page.toString(), "per_page" to perPage.toString()),
        ),
    )

    suspend fun fragile(page: Int = 1, perPage: Int = 20): QuestionPageDto = api.decode(
        QuestionPageDto.serializer(),
        api.get(
            ApiEndpoints.Questions.FRAGILE,
            mapOf("page" to page.toString(), "per_page" to perPage.toString()),
        ),
    )

    suspend fun studyNow(limit: Int = 20): StudyNowDto = api.decode(
        StudyNowDto.serializer(),
        api.get(ApiEndpoints.Questions.STUDY_NOW, mapOf("limit" to limit.toString())),
    )

    suspend fun cases(search: String, page: Int = 1, size: Int = 20): ClinicalCaseListDto = api.decode(
        ClinicalCaseListDto.serializer(),api.get(ApiEndpoints.Questions.CASES,mapOf("search" to search,"page" to page.toString(),"per_page" to size.toString())),
    )

    suspend fun knowledgeAssociations(search: String): KnowledgeAssociationListDto = api.decode(
        KnowledgeAssociationListDto.serializer(),api.get(ApiEndpoints.Questions.KNOWLEDGE_OBJECTS,mapOf("search" to search)),
    )

    suspend fun createKnowledge(title: String, objective: String, answer: String,
        facts: List<String>, category: Long?): KnowledgeDetailDto = api.decode(
        KnowledgeDetailDto.serializer(),api.post(ApiEndpoints.Questions.KNOWLEDGE_OBJECTS,buildJsonObject {
            put("title",title)
            put("learning_objective",objective)
            put("canonical_answer",answer)
            put("key_facts",JsonArray(facts.map(::JsonPrimitive)))
            put("category",category?.let(::JsonPrimitive) ?: JsonNull)
            put("status","active")
        }),
    )

    suspend fun knowledgeDetail(id: Long): KnowledgeDetailDto = api.decode(
        KnowledgeDetailDto.serializer(),api.get(ApiEndpoints.Questions.knowledgeObject(id)),
    )

    suspend fun updateCaseStem(key: String, stem: String?, version: Int): CaseStemResultDto = api.decode(
        CaseStemResultDto.serializer(),api.post(ApiEndpoints.Questions.caseStem(key),buildJsonObject {
            put("expected_version",version)
        put("case_stem",stem?.let(::JsonPrimitive) ?: JsonNull)
        }),
    )

    suspend fun caseDetail(key: String): ClinicalCaseDetailDto = api.decode(
        ClinicalCaseDetailDto.serializer(),api.get(ApiEndpoints.Questions.clinicalCase(key)),
    )

    suspend fun knowledgeMap(): KnowledgeMapDto = api.decode(
        KnowledgeMapDto.serializer(), api.get(ApiEndpoints.Questions.KNOWLEDGE_MAP),
    )

    // Explicitly encode edited clears/defaults. An untouched tag field is omitted
    // on update to avoid rewriting unchanged memberships. Writes use exact-name arrays.
    private fun questionBody(request: QuestionWriteRequest): JsonObject = buildJsonObject {
        put("question", request.question)
        put("choices", JsonArray(request.choices.map(::JsonPrimitive)))
        put("correct_answer", request.correctAnswer)
        put("explanation", request.explanation?.let(::JsonPrimitive) ?: JsonNull)
        if(request.writeTags) put("tags", JsonArray(request.tags.map(::JsonPrimitive)))
        put("difficulty", request.difficulty)
        put("category", request.category?.let(::JsonPrimitive) ?: JsonNull)
        put("source", request.source)
        request.lastRevisedAt?.let { put("last_revised_at", it) }
        put("source_document", request.sourceDocument)
        put("source_page", request.sourcePage?.let(::JsonPrimitive) ?: JsonNull)
        put("translations", request.translations)
        put("knowledge_object", request.knowledgeObject?.let(::JsonPrimitive) ?: JsonNull)
        put("case_key", request.caseKey?.let(::JsonPrimitive) ?: JsonNull)
        put("case_stem", request.caseStem?.let(::JsonPrimitive) ?: JsonNull)
        put("case_order", request.caseOrder?.let(::JsonPrimitive) ?: JsonNull)
        request.expectedVersion?.let { put("expected_version", it) }
    }

    suspend fun create(request: QuestionWriteRequest, operationId: String? = null): QuestionDto = api.decode(
        QuestionDto.serializer(),
        api.post(ApiEndpoints.Questions.BASE, buildJsonObject {
            questionBody(request).forEach { (key, value) -> put(key, value) }
            operationId?.let { put("operation_id", it) }
        }),
    )

    suspend fun createReceipt(operationId: String): QuestionDto = api.decode(
        QuestionDto.serializer(), api.get(ApiEndpoints.Questions.BASE, mapOf("operation_id" to operationId)),
    )

    suspend fun update(questionId: Long, request: QuestionWriteRequest): QuestionDto = api.decode(
        QuestionDto.serializer(),
        api.put(ApiEndpoints.Questions.item(questionId), questionBody(request)),
    )

    suspend fun duplicate(questionId: Long, operationId: String): QuestionDto = api.decode(
        QuestionDto.serializer(), api.post(ApiEndpoints.Questions.duplicate(questionId), buildJsonObject { put("operation_id", operationId) }),
    )
    suspend fun duplicateReceipt(questionId: Long, operationId: String): QuestionDto = api.decode(
        QuestionDto.serializer(), api.get(ApiEndpoints.Questions.duplicate(questionId), mapOf("operation_id" to operationId)),
    )

    suspend fun delete(questionId: Long, version: Int) {
        api.delete(ApiEndpoints.Questions.item(questionId)+"?expected_version=$version")
    }

    suspend fun verify(questionId: Long): Boolean = api.decode(
        VerifyQuestionDto.serializer(), api.post(ApiEndpoints.Questions.verify(questionId)),
    ).verified

    /** One bounded read per visible page; never launch one request per card. */
    suspend fun ratings(ids: List<Long>): Map<Long, QuestionRatingRowDto> {
        val requested = ids.filter { it > 0 }.distinct()
        return requested.chunked(500).flatMap { chunk ->
            api.decode(QuestionRatingsPageDto.serializer(), api.get("questions/ratings/",
                mapOf("ids" to chunk.joinToString(",")))).items
        }.filter { it.questionId in requested }.associateBy { it.questionId }
    }

    suspend fun rating(questionId: Long): QuestionRatingDto = api.decode(
        QuestionRatingDto.serializer(), api.get(ApiEndpoints.Questions.rating(questionId)),
    )

    suspend fun rate(questionId: Long, rating: Int) {
        require(rating in 1..5)
        api.post(ApiEndpoints.Questions.rate(questionId), api.encode(
            QuestionRatingRequest.serializer(), QuestionRatingRequest(rating),
        ))
    }

    suspend fun reportState(questionId: Long): ReportStateDto = api.decode(ReportStateDto.serializer(), api.get(ApiEndpoints.Questions.flag(questionId)))

    suspend fun report(questionId: Long, reason: String) {
        api.post(ApiEndpoints.Questions.flag(questionId), api.encode(
            QuestionReportRequest.serializer(), QuestionReportRequest(reason.trim()),
        ))
    }
}

class CategoryService(private val api: ApiClient) {
    suspend fun list(): CategoryListDto =
        api.decode(CategoryListDto.serializer(), api.get(ApiEndpoints.Categories.LIST))

    suspend fun create(request: CategoryWriteRequest): CategoryDto = api.decode(
        CategoryDto.serializer(),
        api.post(ApiEndpoints.Categories.CREATE, categoryBody(request)),
    )

    suspend fun update(id: Long, request: CategoryWriteRequest): CategoryDto = api.decode(
        CategoryDto.serializer(),
        api.put(ApiEndpoints.Categories.update(id), categoryBody(request)),
    )

    suspend fun delete(id: Long, version: Int) { api.delete(ApiEndpoints.Categories.delete(id) + "?expected_version=$version") }
    suspend fun merge(source: CategoryDto, target: CategoryDto, operation: String): CategoryDto = api.decode(
        CategoryDto.serializer(), api.post(ApiEndpoints.Categories.MERGE, buildJsonObject {
            put("source_id", source.id); put("target_id", target.id)
            put("expected_version", source.version); put("target_version", target.version)
            put("operation_id", operation)
        }))
    suspend fun receipt(operation: String, action: String = "category.create"): Long {
        val receipt = api.contentReceipt(operation)
        check(receipt.action == action)
        if(!receipt.targetExists) throw ContentTargetGoneException()
        return receipt.targetId
    }
    private fun categoryBody(request: CategoryWriteRequest) = buildJsonObject {
        put("name", request.name); put("description", request.description); put("color", request.color); put("icon", request.icon)
        request.expectedVersion?.let { put("expected_version", it) }
        request.operationId?.let { put("operation_id", it) }
    }

}

class BlueprintService(private val api: ApiClient) {
    suspend fun list(): BlueprintListDto =
        api.decode(BlueprintListDto.serializer(), api.get(ApiEndpoints.Blueprints.LIST))
}

class BookmarkService(private val api: ApiClient) {
    suspend fun list(): BookmarkListDto =
        api.decode(BookmarkListDto.serializer(), api.get(ApiEndpoints.Bookmarks.LIST))

    suspend fun page(page: Int = 1, perPage: Int = 10): QuestionPageDto = api.decode(
        QuestionPageDto.serializer(),
        api.get(
            ApiEndpoints.Bookmarks.LIST,
            mapOf("page" to page.toString(), "per_page" to perPage.toString()),
        ),
    )

    suspend fun count(): Int =
        api.decode(CountDto.serializer(), api.get(ApiEndpoints.Bookmarks.COUNT)).count

    suspend fun state(questionId: Long): Boolean = api.decode(
        BookmarkToggleDto.serializer(), api.get(ApiEndpoints.Questions.bookmark(questionId)),
    ).added

    suspend fun set(questionId: Long, added: Boolean): Boolean = api.decode(
        BookmarkToggleDto.serializer(), api.put(ApiEndpoints.Questions.bookmark(questionId), buildJsonObject { put("added", added) }),
    ).added

    suspend fun toggle(questionId: Long): Boolean = api.decode(
        BookmarkToggleDto.serializer(),
        api.post(ApiEndpoints.Questions.bookmark(questionId)),
    ).added
}

/**
 * Analytics endpoints.
 *
 * Param corrections from the previous revision:
 *   • `categoryMastery` used to send `page` / `per_page`. The backend
 *     reads `min_attempts` (1-50) and `top` (1-50); the old params
 *     were silently ignored.
 *   • `streakHistory` and `categoryMastery` returned `JsonObject`.
 *     Both now return typed DTOs.
 */
class AnalyticsService(private val api: ApiClient) {
    suspend fun verificationStats(): VerificationStatsDto = api.decode(
        VerificationStatsDto.serializer(), api.get(ApiEndpoints.Analytics.VERIFICATION_STATS),
    )

    suspend fun summary(days: Int = 30): AnalyticsSummaryDto = api.decode(
        AnalyticsSummaryDto.serializer(),
        api.get(
            ApiEndpoints.Analytics.SUMMARY,
            mapOf("days" to days.coerceIn(1, 90).toString()),
        ),
    )

    suspend fun activeUsers(days: Int = 30): List<ActiveUsersPointDto> = api.decode(
        ListSerializer(ActiveUsersPointDto.serializer()),
        api.get(
            ApiEndpoints.Analytics.ACTIVE_USERS,
            mapOf("days" to days.coerceIn(1, 90).toString()),
        ),
    )

    suspend fun categoryMastery(
        minAttempts: Int? = null,
        top: Int? = null,
    ): CategoryMasteryDto = api.decode(
        CategoryMasteryDto.serializer(),
        api.get(
            ApiEndpoints.Analytics.CATEGORY_MASTERY,
            buildMap {
                minAttempts?.let { put("min_attempts", it.coerceIn(1, 50).toString()) }
                top?.let { put("top", it.coerceIn(1, 50).toString()) }
            },
        ),
    )

    suspend fun streakHistory(days: Int = 30): StreakHistoryDto = api.decode(
        StreakHistoryDto.serializer(),
        api.get(
            ApiEndpoints.Analytics.STREAK_HISTORY,
            mapOf("days" to days.coerceIn(7, 365).toString()),
        ),
    )

    suspend fun difficultyCalibration(): ReportRowsDto = report(ApiEndpoints.Analytics.DIFFICULTY_CALIBRATION)
    suspend fun authorFlagRate(): ReportRowsDto = report(ApiEndpoints.Analytics.AUTHOR_FLAG_RATE)
    suspend fun cohortComparison(): ReportRowsDto = report(ApiEndpoints.Analytics.COHORT_COMPARISON)
    suspend fun retention(): ReportRowsDto = report(ApiEndpoints.Analytics.RETENTION)
    suspend fun examDuration(): ExamDurationReportDto = api.decode(
        ExamDurationReportDto.serializer(), api.get(ApiEndpoints.Analytics.EXAM_DURATION),
    )

    private suspend fun report(path: String): ReportRowsDto =
        api.decode(ReportRowsDto.serializer(), api.get(path))
}

class HistoryService(private val api: ApiClient) {
    suspend fun list(
        page: Int = 1,
        perPage: Int = 20,
        userId: Long? = null,
    ): HistoryPageDto = api.decode(
        HistoryPageDto.serializer(),
        api.get(
            ApiEndpoints.History.LIST,
            mapOf(
                "page" to page.toString(),
                "per_page" to perPage.toString(),
                "user_id" to userId?.toString(),
            ),
        ),
    )

    suspend fun detail(id: Long): HistoryDetailDto = api.decode(
        HistoryDetailDto.serializer(), api.get(ApiEndpoints.History.detail(id)),
    )
}

class TestSessionService(private val api: ApiClient) {
    suspend fun start(mode: TestMode, request: StartSessionRequest): ExamSessionDto = api.decode(
        ExamSessionDto.serializer(),
        api.post(
            ApiEndpoints.test(mode, "start/${mode.path}"),
            api.encode(StartSessionRequest.serializer(), request),
        ),
    )

    suspend fun question(mode: TestMode, sessionId: String): SessionQuestionPageDto = api.decode(
        SessionQuestionPageDto.serializer(),
        api.get(ApiEndpoints.test(mode, "question"), mapOf("session_id" to sessionId)),
    )

    suspend fun submit(mode: TestMode, request: SubmitAnswerRequest): AnswerSubmissionDto = api.decode(
        AnswerSubmissionDto.serializer(),
        api.post(
            ApiEndpoints.test(mode, "answer"),
            api.encode(SubmitAnswerRequest.serializer(), request),
        ),
    )

    suspend fun questions(mode: TestMode, sessionId: String): SessionQuestionBundleDto = api.decode(
        SessionQuestionBundleDto.serializer(),
        api.get(ApiEndpoints.test(mode, "questions"), mapOf("session_id" to sessionId)),
    )

    suspend fun finish(mode: TestMode, sessionId: String): SessionResultsDto = api.decode(
        SessionResultsDto.serializer(),
        api.post(
            ApiEndpoints.test(mode, "results"),
            buildJsonObject { put("session_id", sessionId) },
        ),
    )

    suspend fun completedResult(mode: TestMode, sessionId: String): SessionResultsDto {
        val result = api.decode(SessionResultsDto.serializer(), api.get(
            ApiEndpoints.test(mode, "results"), mapOf("session_id" to sessionId, "mode" to mode.path)))
        check(result.sourceSessionId == sessionId && result.mode == mode.path && (result.historyId ?: 0) > 0)
        return result
    }

    suspend fun pause(mode: TestMode, sessionId: String) {
        api.post(
            ApiEndpoints.test(mode, "pause"),
            buildJsonObject { put("session_id", sessionId) },
        )
    }

    suspend fun resume(mode: TestMode, sessionId: String? = null): SessionProgressDto = api.decode(
        SessionProgressDto.serializer(),
        api.post(
            ApiEndpoints.test(mode, "resume"),
            buildJsonObject {
                sessionId?.let { put("session_id", it) }
                put("mode", mode.path)
            },
        ),
    )

    suspend fun status(mode: TestMode, sessionId: String? = null): SessionProgressDto = api.decode(
        SessionProgressDto.serializer(),
        api.get(
            ApiEndpoints.test(mode, "status"),
            mapOf(
                "session_id" to sessionId,
                "mode" to mode.path.takeIf { sessionId == null },
            ),
        ),
    )

    suspend fun discard(mode: TestMode, sessionId: String) {
        api.delete(
            ApiEndpoints.test(mode, "discard"),
            buildJsonObject { put("session_id", sessionId) },
        )
    }
}
