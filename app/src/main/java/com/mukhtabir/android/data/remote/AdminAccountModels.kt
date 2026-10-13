package com.mukhtabir.android.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ActiveAccountDto(
    val name: String = "",
    val ip: String? = null,
    val role: String = "",
    @SerialName("last_seen") val lastSeen: String? = null,
)

@Serializable
data class ActiveAccountsDto(val users: Map<Long,ActiveAccountDto> = emptyMap())

@Serializable
data class GeneratedPasswordDto(@SerialName("temp_password") val tempPassword: String)
