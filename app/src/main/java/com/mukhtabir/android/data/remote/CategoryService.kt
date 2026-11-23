package com.mukhtabir.android.data.remote

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class CategoryService(private val api: ApiClient) : LearnerCategories {
    override suspend fun list(): CategoryListDto =
        api.decode(CategoryListDto.serializer(), api.get(ApiEndpoints.Categories.LIST))

    suspend fun create(request: CategoryWriteRequest): CategoryDto = api.decode(
        CategoryDto.serializer(),
        api.post(ApiEndpoints.Categories.CREATE, categoryBody(request)),
    )

    suspend fun update(id: Long, request: CategoryWriteRequest): CategoryDto = api.decode(
        CategoryDto.serializer(),
        api.put(ApiEndpoints.Categories.update(id), categoryBody(request)),
    )

    suspend fun delete(id: Long, version: Int) { api.delete(ApiEndpoints.Categories.delete(id) + "?expected_version=$version") }
    suspend fun merge(source: CategoryDto, target: CategoryDto, operation: String): CategoryDto = api.decode(
        CategoryDto.serializer(), api.post(ApiEndpoints.Categories.MERGE, buildJsonObject {
            put("source_id", source.id); put("target_id", target.id)
            put("expected_version", source.version); put("target_version", target.version)
            put("operation_id", operation)
        }))
    suspend fun receipt(operation: String, action: String = "category.create"): Long {
        val receipt = api.contentReceipt(operation)
        check(receipt.action == action)
        if(!receipt.targetExists) throw ContentTargetGoneException()
        return receipt.targetId
    }
    private fun categoryBody(request: CategoryWriteRequest) = buildJsonObject {
        put("name", request.name); put("description", request.description); put("color", request.color); put("icon", request.icon)
        request.expectedVersion?.let { put("expected_version", it) }
        request.operationId?.let { put("operation_id", it) }
    }

}
