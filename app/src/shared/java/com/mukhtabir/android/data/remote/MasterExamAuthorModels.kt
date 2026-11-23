package com.mukhtabir.android.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExamPersonDto(val id: Long, val username: String = "", @SerialName("full_name") val fullName: String = "")
@Serializable
data class ExamGroupDto(val id: Long, val name: String = "", @SerialName("member_count") val memberCount: Int = 0)

/** Author detail is separate from the learner attempt model. */
@Serializable
data class MasterExamAuthorDto(
    val id: Long,
    val name: String,
    val description: String? = null,
    val instructions: String? = null,
    @SerialName("primary_attending") val primaryAttending: Long,
    @SerialName("co_attendings") val coAttendings: List<ExamPersonDto> = emptyList(),
    @SerialName("audience_groups") val audienceGroups: List<ExamGroupDto> = emptyList(),
    @SerialName("audience_users") val audienceUsers: List<ExamPersonDto> = emptyList(),
    @SerialName("audience_all_doctors") val audienceAllDoctors: Boolean = false,
    @SerialName("opens_at") val opensAt: String,
    @SerialName("closes_at") val closesAt: String,
    @SerialName("duration_minutes") val durationMinutes: Int,
    @SerialName("weight_easy") val weightEasy: Double = 1.0,
    @SerialName("weight_medium") val weightMedium: Double = 1.0,
    @SerialName("weight_hard") val weightHard: Double = 1.0,
    @SerialName("shuffle_questions") val shuffleQuestions: Boolean = true,
    @SerialName("shuffle_choices") val shuffleChoices: Boolean = false,
    @SerialName("allow_makeup") val allowMakeup: Boolean = true,
    @SerialName("exam_topic_tag") val examTopicTag: String? = null,
    @SerialName("question_ids") val questionIds: List<Long> = emptyList(),
    @SerialName("can_edit_now") val canEditNow: Boolean = false,
    @SerialName("can_be_deleted") val canBeDeleted: Boolean = false,
    val version: Int,
    val status: String = "",
    @SerialName("stored_status") val storedStatus: String = "",
)

@Serializable
data class MasterExamMetadataWrite(
    val name: String,
    val description: String,
    val instructions: String,
    @SerialName("opens_at") val opensAt: String,
    @SerialName("closes_at") val closesAt: String,
    @SerialName("duration_minutes") val durationMinutes: Int,
    @SerialName("audience_all_doctors") val audienceAllDoctors: Boolean,
    @SerialName("audience_group_ids") val audienceGroupIds: List<Long>,
    @SerialName("audience_user_ids") val audienceUserIds: List<Long>,
    @SerialName("co_attending_ids") val coAttendingIds: List<Long>,
    @SerialName("weight_easy") val weightEasy: Double,
    @SerialName("weight_medium") val weightMedium: Double,
    @SerialName("weight_hard") val weightHard: Double,
    @SerialName("shuffle_questions") val shuffleQuestions: Boolean,
    @SerialName("shuffle_choices") val shuffleChoices: Boolean,
    @SerialName("allow_makeup") val allowMakeup: Boolean,
    @SerialName("exam_topic_tag") val examTopicTag: String,
)

@Serializable
data class QuestionBatchDto(val items: List<QuestionDto> = emptyList())
