package com.mukhtabir.android.data.remote

class HistoryService(private val api: ApiClient) : LearnerHistory {
    override suspend fun list(
        page: Int,
        perPage: Int,
        userId: Long?,
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

    override suspend fun detail(id: Long): HistoryDetailDto = api.decode(
        HistoryDetailDto.serializer(), api.get(ApiEndpoints.History.detail(id)),
    )
}
