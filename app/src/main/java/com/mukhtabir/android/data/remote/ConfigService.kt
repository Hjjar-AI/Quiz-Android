package com.mukhtabir.android.data.remote

class ConfigService(private val api: ApiClient) {
    suspend fun getConfig(): PublicConfigDto =
        api.decode(PublicConfigDto.serializer(), api.get(ApiEndpoints.CONFIG))
}
