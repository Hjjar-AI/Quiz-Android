package com.mukhtabir.android.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class DeviceSessionDto(val id: String, @SerialName("is_current") val current: Boolean,
    val ip: String? = null, @SerialName("user_agent") val userAgent: String = "",
    @SerialName("last_seen") val lastSeen: String)
@Serializable
data class DeviceSessionsDto(val items: List<DeviceSessionDto> = emptyList())
@Serializable
data class SessionRevokedDto(val revoked: Boolean, @SerialName("is_current") val current: Boolean)
class DeviceSessionsService(private val api: ApiClient) {
    suspend fun list(): List<DeviceSessionDto> = api.decode(DeviceSessionsDto.serializer(), api.get(ApiEndpoints.Auth.SESSIONS)).items
    suspend fun revoke(id: String, password: String): SessionRevokedDto {
        require(id.matches(Regex("[0-9a-f]{64}")))
        return api.decode(SessionRevokedDto.serializer(), api.post(ApiEndpoints.Auth.revokeSession(id), buildJsonObject { put("current_password", password) }))
    }
}
