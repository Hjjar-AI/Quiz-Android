package com.mukhtabir.android.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VerificationStatsDto(
    @SerialName("total_verified") val totalVerified: Long = 0,
    @SerialName("total_unverified") val totalUnverified: Long = 0,
    @SerialName("verification_rate") val verificationRate: Double = 0.0,
    @SerialName("by_user") val byUser: List<VerifierCountDto> = emptyList(),
    @SerialName("by_category") val byCategory: List<CategoryVerificationDto> = emptyList(),
    val monthly: List<MonthlyVerificationDto> = emptyList(),
)

@Serializable
data class VerifierCountDto(
    @SerialName("verified_by") val verifiedBy: String? = null,
    val count: Long = 0,
)

@Serializable
data class CategoryVerificationDto(
    val name: String = "",
    val color: String? = null,
    val total: Long = 0,
    val verified: Long = 0,
)

@Serializable
data class MonthlyVerificationDto(val month: String = "", val count: Long = 0)
