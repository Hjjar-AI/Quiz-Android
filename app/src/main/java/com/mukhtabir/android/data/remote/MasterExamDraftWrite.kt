package com.mukhtabir.android.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Only fields accepted by MasterExamDraftCreateSerializer. */
@Serializable
data class MasterExamDraftWrite(
    val question: String,
    val choices: List<String>,
    @SerialName("correct_answer") val correctAnswer: Int,
    val explanation: String = "",
    val source: String = "",
    val difficulty: String = "medium",
    val category: Long? = null,
    @SerialName("case_key") val caseKey: String? = null,
    @SerialName("case_stem") val caseStem: String? = null,
    @SerialName("case_order") val caseOrder: Int? = null,
)
