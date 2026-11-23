package com.mukhtabir.android.data.remote

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class BookmarkService(private val api: ApiClient) : LearnerBookmarks {
    override suspend fun list(): BookmarkListDto =
        api.decode(BookmarkListDto.serializer(), api.get(ApiEndpoints.Bookmarks.LIST))

    override suspend fun page(page: Int, perPage: Int): QuestionPageDto = api.decode(
        QuestionPageDto.serializer(),
        api.get(
            ApiEndpoints.Bookmarks.LIST,
            mapOf("page" to page.toString(), "per_page" to perPage.toString()),
        ),
    )

    override suspend fun count(): Int =
        api.decode(CountDto.serializer(), api.get(ApiEndpoints.Bookmarks.COUNT)).count

    override suspend fun state(questionId: Long): Boolean = api.decode(
        BookmarkToggleDto.serializer(), api.get(ApiEndpoints.Questions.bookmark(questionId)),
    ).added

    override suspend fun set(questionId: Long, added: Boolean): Boolean = api.decode(
        BookmarkToggleDto.serializer(), api.put(ApiEndpoints.Questions.bookmark(questionId), buildJsonObject { put("added", added) }),
    ).added

    suspend fun toggle(questionId: Long): Boolean = api.decode(
        BookmarkToggleDto.serializer(),
        api.post(ApiEndpoints.Questions.bookmark(questionId)),
    ).added
}
