package com.mukhtabir.android.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class ManagedMemberDto(
    @SerialName("user_id") val userId: Long,
    val username: String = "",
    @SerialName("full_name") val fullName: String? = null,
    val role: String = "",
    @SerialName("is_active") val active: Boolean = true,
    @SerialName("show_in_leaderboard") val visible: Boolean = true,
)

@Serializable
data class ManagedContentDto(
    val id: Long,
    val version: Int = 1,
    val name: String,
    val description: String? = null,
    @SerialName("is_active") val active: Boolean = true,
    val weights: Map<String,Double> = emptyMap(),
    val members: List<ManagedMemberDto> = emptyList(),
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class ManagedContentList(val items: List<ManagedContentDto> = emptyList())

@Serializable
data class MemberAddResult(val added: Int = 0, val requested: Int = 0)

enum class ManagedKind(val capability: String, val path: String) {
    GROUP("groups.admin",ApiEndpoints.Groups.ADMIN_LIST),
    BLUEPRINT("admin.blueprints",ApiEndpoints.Blueprints.LIST),
}

class ManagedContentService(private val api: ApiClient) {
    suspend fun list(kind: ManagedKind): List<ManagedContentDto> = api.decode(
        ManagedContentList.serializer(),api.get(kind.path,
            if(kind==ManagedKind.GROUP) mapOf("include_inactive" to "true") else emptyMap())).items

    suspend fun detail(kind: ManagedKind,id: Long): ManagedContentDto {
        require(id>0)
        return api.decode(ManagedContentDto.serializer(),api.get(ApiEndpoints.managedItem(kind, id)))
    }

    suspend fun save(kind: ManagedKind,id: Long?,name: String,description: String,active: Boolean,
        weights: Map<Long,Double>, expectedVersion: Int? = null): ManagedContentDto {
        val payload=buildJsonObject {
            put("name",name); put("description",description)
            if(kind==ManagedKind.GROUP && id!=null) put("expected_version", requireNotNull(expectedVersion))
            // Group creation does not accept is_active; new groups are active by contract.
            if(kind==ManagedKind.BLUEPRINT || id!=null) put("is_active",active)
            if(kind==ManagedKind.BLUEPRINT) put("weights",buildJsonObject {
                weights.forEach { (category,weight) -> put(category.toString(),weight) }
            })
        }
        return api.decode(ManagedContentDto.serializer(),if(id==null) api.post(kind.path,payload)
            else api.put(ApiEndpoints.managedItem(kind, id),payload))
    }

    suspend fun delete(kind: ManagedKind,id: Long) { require(id>0);api.delete(ApiEndpoints.managedItem(kind, id)) }

    suspend fun addMembers(id: Long,users: List<Long>): MemberAddResult {
        require(id>0 && users.isNotEmpty() && users.all { it>0 })
        return api.decode(MemberAddResult.serializer(),api.post(ApiEndpoints.Groups.members(id),
            buildJsonObject { put("user_ids",JsonArray(users.map { JsonPrimitive(it) })) }))
    }

    suspend fun removeMember(id: Long,user: Long) {
        require(id>0 && user>0)
        api.delete(ApiEndpoints.Groups.member(id, user))
    }
}
