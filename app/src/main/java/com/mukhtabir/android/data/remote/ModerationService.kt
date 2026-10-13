package com.mukhtabir.android.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class ModerationFlagDto(
    val id: Long,
    @SerialName("question_id") val questionId: Long,
    val reason: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("question_text") val questionText: String = "",
    @SerialName("question_author") val questionAuthor: String = "",
    @SerialName("flagger_username") val flaggerUsername: String = "",
)

@Serializable
data class ModerationFlagPageDto(
    val items: List<ModerationFlagDto> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    @SerialName("total_pages") val totalPages: Int = 1,
)

@Serializable
data class ModerationCountDto(val count: Int)

class ModerationService(private val api: ApiClient) {
    suspend fun flags(page: Int, size: Int): ModerationFlagPageDto = api.decode(
        ModerationFlagPageDto.serializer(),
        api.get(ApiEndpoints.Questions.FLAGS,mapOf("page" to page.toString(),"per_page" to size.toString())),
    )

    suspend fun ratings(ids: List<Long>): Map<Long, QuestionRatingRowDto> = QuestionService(api).ratings(ids)

    suspend fun resolveFlag(id: Long) { api.post(ApiEndpoints.Questions.resolveFlag(id)) }

    suspend fun questions(page: Int, size: Int, search: String, unverified: Boolean): QuestionPageDto = api.decode(
        QuestionPageDto.serializer(),
        api.get(ApiEndpoints.Questions.BASE,mapOf("page" to page.toString(),"per_page" to size.toString(),
            "search" to search.takeIf(String::isNotBlank),"verified" to "no".takeIf { unverified })),
    )

    suspend fun tags(ids: List<Long>, add: List<String>, remove: List<String>): ModerationCountDto = api.decode(
        ModerationCountDto.serializer(),
        api.post(ApiEndpoints.Questions.BULK_TAGS,buildJsonObject {
            put("question_ids",JsonArray(ids.map(::JsonPrimitive)))
            put("add_tags",JsonArray(add.map(::JsonPrimitive)))
            put("remove_tags",JsonArray(remove.map(::JsonPrimitive)))
        }),
    )

    // Explicit desired state, unlike the existing single-question toggle endpoint.
    suspend fun verify(ids: List<Long>, verified: Boolean, notes: String): ModerationCountDto = api.decode(
        ModerationCountDto.serializer(),
        api.post(ApiEndpoints.Questions.BULK_VERIFY,buildJsonObject {
            put("question_ids",JsonArray(ids.map(::JsonPrimitive)))
            put("action",if(verified) "verify" else "unverify")
            put("verification_notes",notes)
        }),
    )
}
