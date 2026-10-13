package com.mukhtabir.android.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class CategoryWriteRequest(
    val name: String,
    val description: String = "",
    val color: String = "#667eea",
    val icon: String = "bi-folder",
    @SerialName("expected_version") val expectedVersion: Int? = null,
    @SerialName("operation_id") val operationId: String? = null,
)

@Serializable
data class KnowledgeMapSummaryDto(
    @SerialName("total_objects") val totalObjects: Int = 0,
    @SerialName("started_objects") val startedObjects: Int = 0,
    @SerialName("mastered_objects") val masteredObjects: Int = 0,
    @SerialName("developing_objects") val developingObjects: Int = 0,
    @SerialName("needs_work_objects") val needsWorkObjects: Int = 0,
    @SerialName("unstarted_objects") val unstartedObjects: Int = 0,
    @SerialName("average_mastery") val averageMastery: Int = 0,
)

@Serializable
data class KnowledgeMapItemDto(
    val id: Long,
    val title: String,
    @SerialName("learning_objective") val learningObjective: String = "",
    @SerialName("category_name") val categoryName: String? = null,
    @SerialName("question_ids") val questionIds: List<Long> = emptyList(),
    @SerialName("questions_count") val questionsCount: Int = 0,
    @SerialName("attempted_questions") val attemptedQuestions: Int = 0,
    val coverage: Int = 0,
    @SerialName("total_attempts") val totalAttempts: Int = 0,
    @SerialName("average_confidence") val averageConfidence: Double? = null,
    @SerialName("mastery_score") val masteryScore: Double = 0.0,
    val status: String = "unstarted",
)

@Serializable
data class KnowledgeMapDto(
    val summary: KnowledgeMapSummaryDto = KnowledgeMapSummaryDto(),
    val items: List<KnowledgeMapItemDto> = emptyList(),
)

@Serializable
data class PlannerDto(
    val id: Long,
    val version: Int = 1,
    @SerialName("target_questions_per_day") val targetQuestionsPerDay: Int = 10,
    @SerialName("target_categories") val targetCategories: List<Long> = emptyList(),
    @SerialName("target_tags") val targetTags: List<String> = emptyList(),
    @SerialName("start_date") val startDate: String,
    @SerialName("end_date") val endDate: String? = null,
    @SerialName("daily_progress") val dailyProgress: Map<String, Int> = emptyMap(),
    val today: String,
)

@Serializable
data class PlannerWriteRequest(
    @SerialName("expected_version") val expectedVersion: Int,
    @SerialName("expected_id") val expectedId: Long,
    @SerialName("target_questions_per_day") val targetQuestionsPerDay: Int,
    @SerialName("target_categories") val targetCategories: List<Long> = emptyList(),
    @SerialName("target_tags") val targetTags: List<String> = emptyList(),
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("end_date") val endDate: String? = null,
)

@Serializable
data class GroupDto(
    val id: Long,
    val name: String,
    val description: String = "",
    @SerialName("member_count") val memberCount: Int = 0,
    @SerialName("show_in_leaderboard") val showInLeaderboard: Boolean = true,
)

@Serializable
data class GroupListDto(val items: List<GroupDto> = emptyList())

@Serializable
data class LeaderboardRowDto(
    @SerialName("user_id") val userId: Long,
    val username: String,
    @SerialName("full_name") val fullName: String = "",
    @SerialName("questions_answered") val questionsAnswered: Int = 0,
    val accuracy: Double = 0.0,
    @SerialName("current_streak") val currentStreak: Int = 0,
    val rank: Int = 0,
)

@Serializable
data class GroupLeaderboardDto(
    val group: GroupDto,
    val days: Int = 7,
    val rows: List<LeaderboardRowDto> = emptyList(),
)

@Serializable
data class GroupVisibilityRequest(
    @SerialName("show_in_leaderboard") val showInLeaderboard: Boolean,
)

@Serializable
data class UserPageDto(
    val items: List<UserDto> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    @SerialName("per_page") val perPage: Int = 20,
    @SerialName("total_pages") val totalPages: Int = 1,
)

@Serializable
data class AdminUserCreateRequest(
    val username: String,
    val password: String,
    @SerialName("full_name") val fullName: String = "",
    val role: String = "member",
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("expiry_days") val expiryDays: Int = 60,
    @SerialName("auto_renew_days") val autoRenewDays: Int = 0,
    @SerialName("admin_password") val adminPassword: String,
)

@Serializable
data class AdminUserUpdateRequest(
    @SerialName("full_name") val fullName: String? = null,
    val role: String? = null,
    @SerialName("is_active") val isActive: Boolean? = null,
    @SerialName("expiry_days") val expiryDays: Int? = null,
    @SerialName("auto_renew_days") val autoRenewDays: Int? = null,
    @SerialName("new_password") val newPassword: String? = null,
    @SerialName("admin_password") val adminPassword: String,
)

@Serializable
data class CapabilityGroupDto(
    val label: String,
    val capabilities: List<String> = emptyList(),
)

@Serializable
data class RoleCapabilitiesDto(
    val capabilities: List<String> = emptyList(),
    val groups: List<CapabilityGroupDto> = emptyList(),
    val roles: Map<String, List<String>> = emptyMap(),
)

@Serializable
data class RoleCapabilitiesUpdateRequest(
    val role: String,
    val capabilities: List<String>,
)

@Serializable
data class UserCapabilitiesDto(
    @SerialName("user_id") val userId: Long,
    val username: String,
    val role: String,
    @SerialName("role_capabilities") val roleCapabilities: List<String> = emptyList(),
    val overrides: Map<String, Boolean> = emptyMap(),
    val resolved: List<String> = emptyList(),
)

@Serializable
data class UserCapabilitiesUpdateRequest(
    val capabilities: Map<String, Boolean?>,
)

@Serializable
data class UserCapabilitiesSavedDto(
    @SerialName("user_id") val userId: Long,
    val overrides: Map<String, Boolean> = emptyMap(),
    val resolved: List<String> = emptyList(),
)

@Serializable
data class RuntimeSettingsDto(
    val version: Int = 1,
    @SerialName("default_expiry_days") val defaultExpiryDays: String = "60",
    @SerialName("default_renewal_days") val defaultRenewalDays: String = "30",
    @SerialName("exam_duration_minutes") val examDurationMinutes: String = "60",
)

@Serializable
data class RuntimeSettingsSavedDto(val version: Int, val updated: List<String> = emptyList())

@Serializable
data class RuntimeSettingsUpdateRequest(
    @SerialName("expected_version") val expectedVersion: Int,
    @SerialName("default_expiry_days") val defaultExpiryDays: Int,
    @SerialName("default_renewal_days") val defaultRenewalDays: Int,
    @SerialName("exam_duration_minutes") val examDurationMinutes: Int,
)

@Serializable
data class QuestionWriteRequest(
    val question: String,
    val choices: List<String>,
    @SerialName("correct_answer") val correctAnswer: Int,
    val explanation: String? = null,
    val tags: List<String> = emptyList(),
    @kotlinx.serialization.Transient val writeTags: Boolean = true,
    val difficulty: String = "medium",
    val category: Long? = null,
    @SerialName("expected_version") val expectedVersion: Int? = null,
    @SerialName("knowledge_object") val knowledgeObject: Long? = null,
    @SerialName("case_key") val caseKey: String? = null,
    @SerialName("case_stem") val caseStem: String? = null,
    @SerialName("case_order") val caseOrder: Int? = null,
    val source: String = "",
    @SerialName("source_document") val sourceDocument: String = "",
    @SerialName("source_page") val sourcePage: Int? = null,
    @SerialName("last_revised_at") val lastRevisedAt: String? = null,
    val translations: JsonObject = JsonObject(emptyMap()),
)

@Serializable
data class VerifyQuestionDto(val verified: Boolean)

@Serializable
data class QuestionRatingDto(
    @SerialName("user_rating") val userRating: Int = 0,
    val average: Double = 0.0,
    val count: Int = 0,
)

@Serializable
data class QuestionRatingRequest(val rating: Int)

@Serializable
data class QuestionReportRequest(val reason: String = "")

@Serializable
data class HistoryDetailDto(
    val id: Long,
    val mode: String,
    val tag: String? = null,
    @SerialName("total_questions") val totalQuestions: Int,
    @SerialName("answered_count") val answeredCount: Int,
    @SerialName("correct_count") val correctCount: Int,
    val accuracy: Double,
    @SerialName("time_spent") val timeSpent: Int,
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("completed_at") val completedAt: String,
    val results: List<SessionResultItemDto> = emptyList(),
    @SerialName("master_metadata") val masterMetadata: ArchivedMasterMetadataDto = ArchivedMasterMetadataDto(),
)

@Serializable
data class ReportRowsDto(val rows: List<JsonObject> = emptyList())

@Serializable
data class ExamDurationReportDto(
    val buckets: List<JsonObject> = emptyList(),
    @SerialName("total_sessions") val totalSessions: Int = 0,
    @SerialName("avg_seconds") val averageSeconds: Double = 0.0,
)


@Serializable
data class PermissionUserDto(
    val id: Long, val username: String,
    @SerialName("full_name") val fullName: String? = null,
    val role: String,
    @SerialName("is_active") val isActive: Boolean = true,
) {
    val displayName: String get() = fullName?.takeIf(String::isNotBlank) ?: username
}

@Serializable
data class PermissionUserPageDto(
    val items: List<PermissionUserDto> = emptyList(), val page: Int = 1,
    @SerialName("total_pages") val totalPages: Int = 1,
)

@Serializable
data class QuestionRatingRowDto(
    @SerialName("question_id") val questionId: Long,
    @SerialName("user_rating") val userRating: Int = 0,
    val average: Double = 0.0, val count: Int = 0,
)

@Serializable
data class QuestionRatingsPageDto(val items: List<QuestionRatingRowDto> = emptyList())
