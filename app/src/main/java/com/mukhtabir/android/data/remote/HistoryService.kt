package com.mukhtabir.android.data.remote

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
