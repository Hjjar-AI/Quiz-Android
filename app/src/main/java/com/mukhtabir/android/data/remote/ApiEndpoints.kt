package com.mukhtabir.android.data.remote

/** Paths mirror frontend/src/services/api/endpoints.js. */
object ApiEndpoints {
    object Auth {
        const val LOGIN = "auth/login/"
        const val LOGOUT = "auth/logout/"
        const val ME = "auth/me/"
        const val CHANGE_PASSWORD = "auth/change-password/"
        const val CSRF = "auth/csrf/"
    }

    const val CONFIG = "config/"
    const val TIPS = "tips/"

    object Questions {
        const val BASE = "questions/"
        const val BATCH = "questions/batch/"
        const val TAGS = "questions/tags/"
        const val FLAGS = "questions/admin/flags/"
        const val BULK_TAGS = "questions/bulk-tags/"
        const val BULK_VERIFY = "questions/bulk-verify/"
        fun resolveFlag(id: Long) = "questions/admin/flags/$id/resolve/"
        const val UNVERIFIED = "questions/unverified/"
        const val AVAILABLE_COUNT = "questions/available-count/"
        const val MISTAKES = "questions/mistakes/"
        const val FRAGILE = "questions/fragile/"
        const val ATTEMPT_SUMMARY = "questions/attempt-summary/"
        const val SRS_DUE_COUNT = "questions/srs-due-count/"
        const val STUDY_NOW = "questions/study-now/"
        const val KNOWLEDGE_MAP = "questions/knowledge-map/"
        const val KNOWLEDGE_OBJECTS = "questions/knowledge-objects/"
        const val CASES = "questions/cases/"
        fun knowledgeObject(id: Long) = "questions/knowledge-objects/$id/"
        fun caseStem(key: String) = "questions/case/${android.net.Uri.encode(key)}/stem/"
        fun clinicalCase(key: String) = "questions/cases/${android.net.Uri.encode(key)}/"

        fun image(id: Long) = "questions/$id/image/"

        fun item(id: Long) = "questions/$id/"
        fun duplicate(id: Long) = "questions/$id/duplicate/"
        fun bookmark(id: Long) = "questions/$id/bookmark/"
        fun verify(id: Long) = "questions/$id/verify/"
        fun rating(id: Long) = "questions/$id/rating/"
        fun rate(id: Long) = "questions/$id/rate/"
        fun flag(id: Long) = "questions/$id/flag/"
    }

    object Categories {
        const val LIST = "questions/categories/"
        const val CREATE = "questions/categories/create/"
        const val MERGE = "questions/categories/merge/"
        fun update(id: Long) = "questions/categories/$id/update/"
        fun delete(id: Long) = "questions/categories/$id/delete/"
    }

    object Blueprints {
        const val LIST = "exam/blueprints/"
    }

    object Bookmarks {
        const val LIST = "questions/bookmarks/"
        const val COUNT = "questions/bookmarks/count/"
    }

    object Analytics {
        const val VERIFICATION_STATS = "analytics/verification-stats/"
        const val SUMMARY = "analytics/summary/"
        const val ACTIVE_USERS = "analytics/active-users/"
        const val CATEGORY_MASTERY = "analytics/category-mastery/"
        const val STREAK_HISTORY = "analytics/streak-history/"
        const val DIFFICULTY_CALIBRATION = "analytics/admin/difficulty-calibration/"
        const val AUTHOR_FLAG_RATE = "analytics/admin/author-flag-rate/"
        const val EXAM_DURATION = "analytics/admin/exam-duration/"
        const val COHORT_COMPARISON = "analytics/admin/cohort-comparison/"
        const val RETENTION = "analytics/admin/retention/"
    }

    object History {
        const val LIST = "history/"
        fun detail(id: Long) = "history/$id/"
    }

    object Planner {
        const val GET = "study-planner/"
        const val PROGRESS = "study-planner/progress/"
        const val UPDATE = "study-planner/update/"
        const val DELETE = "study-planner/delete/"
    }

    object Groups {
        const val MINE = "study/groups/mine/"
        const val ADMIN_LIST = "study/admin/groups/"
        fun leaderboard(id: Long) = "study/groups/$id/leaderboard/"
        fun visibility(id: Long) = "study/groups/$id/visibility/"
    }

    object MasterExams {
        const val BASE = "exam/master/"
        const val NOTICES = "exam/master/needs-acknowledgement/"
        fun item(id: Long) = "exam/master/$id/"
        fun action(id: Long, action: String) = "exam/master/$id/$action/"
        fun attempt(id: Long, action: String) = "exam/master/$id/attempt/$action/"
    }

    object Admin {
        const val USERS = "auth/admin/users/"
        const val ACTIVE_USERS = "auth/admin/active-users/"
        fun resetUserPassword(id: Long) = "auth/admin/users/$id/reset-password/"
        fun user(id: Long) = "auth/admin/users/$id/"
        fun setUserActive(id: Long) = "auth/admin/users/$id/active/"
        fun toggleUser(id: Long) = "auth/admin/users/$id/toggle/"
        const val PERMISSIONS = "auth/admin/permissions/"
        fun userPermissions(id: Long) = "auth/admin/permissions/users/$id/"
        const val SETTINGS = "admin/settings/"
    }

    fun test(mode: TestMode, action: String): String = "${mode.path}/$action/"
}

enum class TestMode(val path: String) {
    EXAM("exam"),
    STUDY("study"),
    RECALL("recall"),
}
