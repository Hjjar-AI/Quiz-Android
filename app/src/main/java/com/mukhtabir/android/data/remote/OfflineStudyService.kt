package com.mukhtabir.android.data.remote

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class OfflineStudyService(private val api: ApiClient) : LearnerOffline {
    override suspend fun filterOptions(): OfflineFilterOptions = OfflineFilterOptions(
        QuestionService(api).sourceBooks().items.filter { it.name.isNotBlank() }, CategoryService(api).list().items,
        QuestionService(api).tags().items.filter { it.name.isNotBlank() })
    override suspend fun catalog(after: Long, upperBound: Long?, selection: OfflineSelection): OfflineCatalogPage = api.decode(
        OfflineCatalogPage.serializer(), api.get(ApiEndpoints.Offline.CATALOG, mapOf(
            "after" to after.toString(), "upper_bound" to upperBound?.toString(),
            "source_document" to selection.book, "category_ids" to JsonArray(selection.categories.map { JsonPrimitive(it) }).toString(),
            "tags_filter" to JsonArray(selection.tags.map { JsonPrimitive(it) }).toString())))
    override suspend fun downloadPermission(): UserDto = api.currentUser()
    override suspend fun download(ids: List<Long>, fullBank: Boolean, includeImages: Boolean): OfflinePack = api.decode(OfflinePack.serializer(),
        api.post(ApiEndpoints.Offline.PACKS,buildJsonObject {
            put("question_ids",JsonArray(ids.map {JsonPrimitive(it)}))
            if(fullBank) put("full_bank",true)
            put("include_images", includeImages)
        }))
    override suspend fun upload(material: OfflineMaterial): SessionResultsDto {
        val identity=requireNotNull(material.completionId)
        val time=requireNotNull(material.frozenTime)
        return api.decode(SessionResultsDto.serializer(),api.post(ApiEndpoints.Offline.COMPLETIONS,buildJsonObject {
            put("completion_id",identity);put("token",material.pack.token);put("time_spent",time)
            material.completedAt?.let { put("occurred_at", it) }
            put("answers",JsonArray(material.answers.map {row -> buildJsonObject {
                put("question_id",row.questionId);put("answer",row.answer);put("confidence",row.confidence)
                if(material.completedAt != null) row.answeredAt?.let { put("answered_at", it) }
            }}))
        })).also { check(it.sourceSessionId==identity && it.mode=="study" && (it.historyId ?: 0)>0) }
    }
}
