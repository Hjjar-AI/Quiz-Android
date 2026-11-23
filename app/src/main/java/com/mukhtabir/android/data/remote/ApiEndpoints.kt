package com.mukhtabir.android.data.remote

/** All Android API paths. Keep builders here so backend contract checks see every route. */
object ApiEndpoints {
    object Auth {
        const val LOGIN = "auth/login/"
        const val LOGOUT = "auth/logout/"
        const val ME = "auth/me/"
        const val CHANGE_PASSWORD = "auth/change-password/"
        const val CSRF = "auth/csrf/"
        const val SESSIONS = "auth/sessions/"
        fun revokeSession(id: String) = "auth/sessions/$id/revoke/"
    }

    const val CONFIG = "config/"
    const val TIPS = "tips/"

    object Questions {
        const val BASE = "questions/"
        const val SOURCE_BOOKS = "questions/source-books/"
        const val SOURCE_BOOK_FILTERS = "questions/source-book-filters/"
        const val RATINGS = "questions/ratings/"
        fun linkedKnowledge(id: Long) = "questions/knowledge-objects/$id/questions/"
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

    object Tags {
        const val TREE = "questions/admin/tags/tree/"
        const val MERGE = "questions/admin/tags/merge/"
        fun rename(name: String) = "questions/admin/tags/${android.net.Uri.encode(name)}/rename/"
        fun delete(name: String) = "questions/admin/tags/${android.net.Uri.encode(name)}/delete/"
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
        fun item(id: Long) = "exam/blueprints/$id/"
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
        fun item(id: Long) = "study/admin/groups/$id/"
        fun members(id: Long) = "study/admin/groups/$id/members/"
        fun member(id: Long, user: Long) = "study/admin/groups/$id/members/$user/"
        fun leaderboard(id: Long) = "study/groups/$id/leaderboard/"
        fun visibility(id: Long) = "study/groups/$id/visibility/"
    }

    object MasterExams {
        const val BASE = "exam/master/"
        const val NOTICES = "exam/master/needs-acknowledgement/"
        fun item(id: Long) = "exam/master/$id/"
        const val DRAFTS = "exam/master/drafts/"
        fun draft(id: Long) = "exam/master/drafts/$id/"
        fun action(id: Long, action: String): String = when (action) {
            "questions/add" -> "exam/master/$id/questions/add/"
            "questions/remove" -> "exam/master/$id/questions/remove/"
            "questions/reorder" -> "exam/master/$id/questions/reorder/"
            "drafts" -> "exam/master/$id/drafts/"
            "publish" -> "exam/master/$id/publish/"
            "cancel" -> "exam/master/$id/cancel/"
            "publish-to-bank" -> "exam/master/$id/publish-to-bank/"
            "start" -> "exam/master/$id/start/"
            "results" -> "exam/master/$id/results/"
            "acknowledge" -> "exam/master/$id/acknowledge/"
            "results/summary.csv" -> "exam/master/$id/results/summary.csv/"
            "results/matrix.csv" -> "exam/master/$id/results/matrix.csv/"
            else -> error("Unknown master exam action")
        }
        fun attempt(id: Long, action: String): String = when (action) {
            "status" -> "exam/master/$id/attempt/status/"
            "question" -> "exam/master/$id/attempt/question/"
            "answer" -> "exam/master/$id/attempt/answer/"
            "goto" -> "exam/master/$id/attempt/goto/"
            "finish" -> "exam/master/$id/attempt/finish/"
            "flag" -> "exam/master/$id/attempt/flag/"
            else -> error("Unknown attempt action")
        }
    }

    object Admin {
        const val USERS = "auth/admin/users/"
        const val ACTIVE_USERS = "auth/admin/active-users/"
        fun resetUserPassword(id: Long) = "auth/admin/users/$id/reset-password/"
        fun user(id: Long) = "auth/admin/users/$id/"
        fun deleteUser(id: Long) = "auth/admin/users/$id/delete/"
        fun setUserActive(id: Long) = "auth/admin/users/$id/active/"
        fun toggleUser(id: Long) = "auth/admin/users/$id/toggle/"
        const val PERMISSIONS = "auth/admin/permissions/"
        const val PERMISSION_USERS = "auth/admin/permissions/users/"
        fun userPermissions(id: Long) = "auth/admin/permissions/users/$id/"
        const val SETTINGS = "admin/settings/"
    }

    object Offline {
        const val CATALOG = "exam/offline/catalog/"
        const val PACKS = "exam/offline/packs/"
        const val COMPLETIONS = "exam/offline/completions/"
    }

    object Database {
        const val INFO = "database/info/"
        const val QUALITY = "database/data-quality/"
        const val BACKUPS = "database/backups/"
        const val BACKUP = "database/backup/"
        const val RESTORE = "database/restore/"
        const val CLEAR = "database/clear/"
        const val REFRESH_RANKS = "admin/refresh-author-ranks/"
        const val SEED = "admin/seed-sample-questions/"
        const val IMPORT = "database/import/"
        const val IMPORT_TELEGRAM = "database/import/telegram/"
        const val IMPORT_STATE = "database/import/state/"
        const val IMPORT_UNLOCK = "database/import/unlock/"
        const val EXPORT_STATE = "database/export/state/"
        fun export(format: String, verified: Boolean): String {
            require(format in setOf("json", "csv", "xlsx", "pdf"))
            return if (verified) "database/export/$format/verified/" else "database/export/$format/"
        }
    }

    fun contentReceipt(operation: String) = "recovery/operations/$operation/"

    fun managedItem(kind: ManagedKind, id: Long): String = when (kind) {
        ManagedKind.GROUP -> Groups.item(id)
        ManagedKind.BLUEPRINT -> Blueprints.item(id)
    }

    fun test(mode: TestMode, action: String): String = when (action) {
        "start/${mode.path}" -> "${mode.path}/start/${mode.path}/"
        "question" -> "${mode.path}/question/"
        "answer" -> "${mode.path}/answer/"
        "questions" -> "${mode.path}/questions/"
        "results" -> "${mode.path}/results/"
        "pause" -> "${mode.path}/pause/"
        "resume" -> "${mode.path}/resume/"
        "status" -> "${mode.path}/status/"
        "discard" -> "${mode.path}/discard/"
        else -> error("Unknown session action")
    }
}
