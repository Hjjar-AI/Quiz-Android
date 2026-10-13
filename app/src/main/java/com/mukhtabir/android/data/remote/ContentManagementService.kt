package com.mukhtabir.android.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class TagTreeNode(val id: Long, val name: String, val children: List<TagTreeNode> = emptyList())
@Serializable
data class TagTreeDto(val tree: List<TagTreeNode> = emptyList(), val version: Int = 1)
@Serializable
data class KnowledgeAdminListDto(val items: List<KnowledgeDetailDto> = emptyList(), val count: Int = 0,
    val page: Int = 1, @SerialName("per_page") val perPage: Int = 20,
    @SerialName("total_pages") val totalPages: Int = 1)
@Serializable
data class CaseAdminDto(val id: Long, val version: Int = 1, val key: String, val title: String? = null, val stem: String? = null,
    val translations: JsonObject = JsonObject(emptyMap()),
    @SerialName("authored_by") val authoredBy: Long? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("question_count") val questionCount: Int = 0,
    val questions: List<QuestionDto> = emptyList())
@Serializable
data class CaseAdminListDto(val items: List<CaseAdminDto> = emptyList(), val total: Int = 0,
    val page: Int = 1, @SerialName("per_page") val perPage: Int = 20,
    @SerialName("total_pages") val totalPages: Int = 1)

class ContentManagementService(private val api: ApiClient) {
    suspend fun tags(): TagTreeDto = api.decode(TagTreeDto.serializer(), api.get("questions/admin/tags/tree/"))
    suspend fun renameTag(old: String, name: String, version: Int) { api.post("questions/admin/tags/${android.net.Uri.encode(old)}/rename/", buildJsonObject { put("new_name", name); put("expected_version",version) }) }
    suspend fun deleteTag(name: String, version: Int) { api.delete("questions/admin/tags/${android.net.Uri.encode(name)}/delete/?expected_version=$version") }
    suspend fun mergeTags(sources: List<String>, target: String, version: Int) { api.post("questions/admin/tags/merge/", buildJsonObject {
        put("expected_version",version)
        put("source_tags", JsonArray(sources.map { JsonPrimitive(it) })); put("target_tag", target)
    }) }
    suspend fun knowledge(search: String, status: String, category: Long?, page: Int, size: Int): KnowledgeAdminListDto = api.decode(
        KnowledgeAdminListDto.serializer(), api.get(ApiEndpoints.Questions.KNOWLEDGE_OBJECTS,
            mapOf("search" to search, "status" to status.takeIf(String::isNotBlank), "category" to category?.toString(), "page" to page.toString(), "per_page" to size.toString())))
    suspend fun knowledgeDetail(id: Long): KnowledgeDetailDto = api.decode(KnowledgeDetailDto.serializer(), api.get(ApiEndpoints.Questions.knowledgeObject(id)))
    suspend fun saveKnowledge(id: Long?, body: JsonObject): KnowledgeDetailDto = api.decode(KnowledgeDetailDto.serializer(),
        if (id == null) api.post(ApiEndpoints.Questions.KNOWLEDGE_OBJECTS, body) else api.put(ApiEndpoints.Questions.knowledgeObject(id), body))
    suspend fun knowledgeReceipt(operation: String): KnowledgeDetailDto {
        val receipt=api.contentReceipt(operation);check(receipt.action=="knowledge.create")
        if(!receipt.targetExists) throw ContentTargetGoneException()
        return try {knowledgeDetail(receipt.targetId)} catch(failure: ApiException) {if(failure.statusCode==404) throw ContentTargetGoneException() else throw failure}
    }
    suspend fun deleteKnowledge(id: Long, version: Int) { api.delete(ApiEndpoints.Questions.knowledgeObject(id)+"?expected_version=$version") }
    suspend fun linkedKnowledge(id: Long, page: Int, size: Int): QuestionPageDto = api.decode(QuestionPageDto.serializer(),
        api.get(ApiEndpoints.Questions.knowledgeObject(id) + "questions/", mapOf("page" to page.toString(), "per_page" to size.toString())))
    suspend fun cases(search: String, page: Int, size: Int): CaseAdminListDto = api.decode(CaseAdminListDto.serializer(),
        api.get(ApiEndpoints.Questions.CASES, mapOf("search" to search, "page" to page.toString(), "per_page" to size.toString())))
    suspend fun caseDetail(key: String): CaseAdminDto = api.decode(CaseAdminDto.serializer(), api.get(ApiEndpoints.Questions.clinicalCase(key)))
    suspend fun saveCase(key: String, title: String, version: Int, translations: JsonObject): CaseAdminDto = api.decode(CaseAdminDto.serializer(),
        api.put(ApiEndpoints.Questions.clinicalCase(key), buildJsonObject { put("title", title); put("translations", translations); put("expected_version",version) }))
    suspend fun deleteCase(key: String, version: Int) { api.delete(ApiEndpoints.Questions.clinicalCase(key) + "?expected_version=$version") }
}
