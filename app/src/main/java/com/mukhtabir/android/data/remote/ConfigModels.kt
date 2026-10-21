package com.mukhtabir.android.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PublicConfigDto(
    @SerialName("max_quiz_questions") val maxQuizQuestions: Int = 200,
    @SerialName("max_choices") val maxChoices: Int = 8,
    @SerialName("items_per_page") val itemsPerPage: Int = 20,
    val roles: Map<String, String> = mapOf(
        "admin" to "admin",
        "moderator" to "moderator",
        "member" to "member",
    ),
)
