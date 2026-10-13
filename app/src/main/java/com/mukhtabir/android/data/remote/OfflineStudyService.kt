package com.mukhtabir.android.data.remote

import java.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class OfflinePack(val id: String,val token: String,val questions: List<QuestionDto>,val images: Map<String,String> = emptyMap(),
    @SerialName("issued_at") val issuedAt: String? = null,
    @SerialName("images_included") val imagesIncluded: Boolean = true,
    @SerialName("image_question_ids") val imageQuestionIds: List<Long> = emptyList(),
    @SerialName("content_revision") val contentRevision: String? = null)
@Serializable
data class OfflineAnswer(val questionId: Long,val answer: Int,val confidence: Int,val answeredAt: String? = null)
@Serializable
data class OfflineMaterial(val pack: OfflinePack,val completionId: String? = null,
    val answers: List<OfflineAnswer> = emptyList(),val index: Int = 0,val startedAtMillis: Long = 0,
    val frozenTime: Int? = null,val result: SessionResultsDto? = null,
    val summary: OfflinePackSummary? = null, val clockOffsetMillis: Long = 0,
    val completedAt: String? = null)

fun OfflinePack.newMaterial(): OfflineMaterial {
    val serverTime = issuedAt?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
    return OfflineMaterial(this, clockOffsetMillis = serverTime?.minus(System.currentTimeMillis()) ?: 0)
}

@Serializable
data class OfflinePackSummary(val id: String, val title: String, val questionCount: Int,
    val answeredCount: Int, val finished: Boolean, val synced: Boolean)

fun OfflineMaterial.summaryOnly(): OfflineMaterial {
    if (summary != null) return this
    return OfflineMaterial(OfflinePack(pack.id, "", emptyList()), summary = OfflinePackSummary(
        pack.id, pack.questions.firstOrNull()?.localizedForDisplay()?.question.orEmpty().take(160),
        pack.questions.size, answers.size, frozenTime != null, result != null))
}

@Serializable
data class OfflineBankProgress(val id: String, val upperBound: Long? = null, val after: Long = 0,
    val pendingIds: List<Long> = emptyList(), val total: Int = 0, val downloaded: Int = 0,
    val excluded: Int = 0, val catalogFinished: Boolean = false, val complete: Boolean = false,
    val selection: OfflineSelection = OfflineSelection(), val includeImages: Boolean = true)

@Serializable
data class OfflineSelection(val book: String? = null, val categories: List<Long> = emptyList(), val tags: List<String> = emptyList())

data class OfflineFilterOptions(val books: List<SourceBookDto>, val categories: List<CategoryDto>, val tags: List<TagDto>)

@Serializable
data class OfflineCatalogPage(@SerialName("question_ids") val questionIds: List<Long>,
    @SerialName("upper_bound") val upperBound: Long,
    @SerialName("next_after") val nextAfter: Long? = null, val total: Int, val excluded: Int,
    val selection: OfflineSelection = OfflineSelection())

class OfflineStudyService(private val api: ApiClient) {
    suspend fun filterOptions(): OfflineFilterOptions = OfflineFilterOptions(
        QuestionService(api).sourceBooks().items.filter { it.name.isNotBlank() }, CategoryService(api).list().items,
        QuestionService(api).tags().items.filter { it.name.isNotBlank() })
    suspend fun catalog(after: Long, upperBound: Long?, selection: OfflineSelection = OfflineSelection()): OfflineCatalogPage = api.decode(
        OfflineCatalogPage.serializer(), api.get("exam/offline/catalog/", mapOf(
            "after" to after.toString(), "upper_bound" to upperBound?.toString(),
            "source_document" to selection.book, "category_ids" to selection.categories.joinToString(","),
            "tags_filter" to JsonArray(selection.tags.map { JsonPrimitive(it) }).toString())))
    suspend fun downloadPermission(): UserDto = api.currentUser()
    suspend fun download(ids: List<Long>, fullBank: Boolean = false, includeImages: Boolean = true): OfflinePack = api.decode(OfflinePack.serializer(),
        api.post("exam/offline/packs/",buildJsonObject {
            put("question_ids",JsonArray(ids.map {JsonPrimitive(it)}))
            if(fullBank) put("full_bank",true)
            put("include_images", includeImages)
        }))
    suspend fun upload(material: OfflineMaterial): SessionResultsDto {
        val identity=requireNotNull(material.completionId)
        val time=requireNotNull(material.frozenTime)
        return api.decode(SessionResultsDto.serializer(),api.post("exam/offline/completions/",buildJsonObject {
            put("completion_id",identity);put("token",material.pack.token);put("time_spent",time)
            material.completedAt?.let { put("occurred_at", it) }
            put("answers",JsonArray(material.answers.map {row -> buildJsonObject {
                put("question_id",row.questionId);put("answer",row.answer);put("confidence",row.confidence)
                if(material.completedAt != null) row.answeredAt?.let { put("answered_at", it) }
            }}))
        })).also { check(it.sourceSessionId==identity && it.mode=="study" && (it.historyId ?: 0)>0) }
    }
}
