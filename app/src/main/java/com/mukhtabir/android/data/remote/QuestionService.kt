package com.mukhtabir.android.data.remote

import okhttp3.MultipartBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

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
