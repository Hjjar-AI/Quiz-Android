package com.mukhtabir.android.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class ContentReceiptDto(val action: String, @SerialName("target_id") val targetId: Long, @SerialName("target_exists") val targetExists: Boolean = true)
@Serializable
data class ReportStateDto(val reported: Boolean, val reason: String? = null)

suspend fun ApiClient.contentReceipt(operation: String): ContentReceiptDto = decode(
    ContentReceiptDto.serializer(), get("recovery/operations/$operation/"))

class ContentTargetGoneException : Exception()
