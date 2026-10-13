package com.mukhtabir.android.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime

@Serializable
data class ApiEnvelope<T>(
    val code: Int,
    val data: T? = null,
    val message: String = "",
    val details: JsonElement? = null,
)

@Serializable
data class LoginRequest(
    val username: String,
    val password: String,
)

@Serializable
data class ChangePasswordRequest(
    @SerialName("current_password") val currentPassword: String,
    @SerialName("new_password") val newPassword: String,
)

@Serializable
data class LoginPayload(
    val user: UserDto,
)

@Serializable
data class CsrfPayload(
    val token: String,
)

/**
 * Authenticated user payload.
 *
 * Field-by-field mapping to `apps/users/serializers.py::UserSerializer`.
 * Every server field with a stable default is declared with the same
 * default so a partial payload (e.g. from a login response that omits
 * `trust_score`) deserializes without throwing.
 */
@Serializable
data class UserDto(
    val id: Long,
    val username: String,
    @SerialName("full_name") val fullName: String = "",
    val email: String = "",
    val role: String = "",
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("must_change_password") val mustChangePassword: Boolean = false,
    val capabilities: List<String> = emptyList(),

    // ── Extended (Phase 0) ─────────────────────────────────────────
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("auto_renew_days") val autoRenewDays: Int = 0,
    @SerialName("trust_score") val trustScore: Double = 0.0,
    @SerialName("questions_count") val questionsCount: Int = 0,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("last_login") val lastLogin: String? = null,
    @SerialName("latest_exam_accuracy") val latestExamAccuracy: Double? = null,
    @SerialName("latest_exam_tag") val latestExamTag: String? = null,

    // ── Extended (Phase 1) ─────────────────────────────────────────
    // Read-only computed fields the server exposes. `uuid` and
    // `is_stub` are also carried here so a future screen that needs
    // them does not require another DTO churn. The Streak fields
    // feed the Profile streak row.
    @SerialName("author_rank") val authorRank: String? = null,
    @SerialName("author_rank_label_ar") val authorRankLabelAr: String? = null,
    @SerialName("current_streak") val currentStreak: Int = 0,
    @SerialName("longest_streak") val longestStreak: Int = 0,
    @SerialName("last_study_date") val lastStudyDate: String? = null,
    val uuid: String? = null,
    @SerialName("is_stub") val isStub: Boolean = false,
) {
    val displayName: String
        get() = fullName.ifBlank { username }

    /**
     * Days remaining until the account expires, or null when:
     *   • the account has no expiry (admin, or `expires_at` absent), or
     *   • the account holds `system.bypass_expiry`, or
     *   • `expires_at` is not parseable.
     *
     * Rounding matches the web's `daysUntilExpiry` (see
     * `frontend/src/utils/formatters.js`): the count is CEILINGED,
     * so a partial day counts as a full day. An already-expired
     * account rounds up to 0, which is also what the web reports.
     *
     * Using `Duration.toDays()` here would truncate, producing a
     * value one lower than the web's for any non-integral remaining
     * span — the banner would then fire a day late.
     */
    fun remainingDays(now: Instant = Instant.now()): Long? {
        if (expiresAt.isNullOrBlank()) return null
        if (capabilities.contains("system.bypass_expiry")) return null
        return try {
            val parsed = OffsetDateTime.parse(expiresAt)
            val millis = Duration.between(now, parsed.toInstant()).toMillis()
            Math.ceil(millis.toDouble() / MILLIS_PER_DAY).toLong()
        } catch (_: Exception) {
            null
        }
    }

    /** True when `system.bypass_expiry` is held. */
    val bypassesExpiry: Boolean
        get() = capabilities.contains("system.bypass_expiry")

    companion object {
        private const val MILLIS_PER_DAY = 86_400_000.0
    }
}
