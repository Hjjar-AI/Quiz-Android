package com.mukhtabir.android.data.remote

class BlueprintService(private val api: ApiClient) {
    suspend fun list(): BlueprintListDto =
        api.decode(BlueprintListDto.serializer(), api.get(ApiEndpoints.Blueprints.LIST))
}
