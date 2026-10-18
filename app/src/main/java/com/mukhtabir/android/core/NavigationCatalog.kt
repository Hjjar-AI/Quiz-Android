package com.mukhtabir.android.core

import androidx.annotation.StringRes
import com.mukhtabir.android.R

/** Capability-filtered destinations shared by the categorized side drawer and restored routes. */
enum class MoreSection(@StringRes val titleRes: Int) {
    LEARN(R.string.more_learn), REVIEW(R.string.more_review), EXAMS(R.string.drawer_exams),
    ACCOUNT(R.string.more_account), SUPPORT(R.string.drawer_help_information),
    AUTHORING(R.string.drawer_authoring), MODERATE(R.string.drawer_moderation), ADMIN(R.string.more_admin),
}

data class MoreDestination(
    val route: String,
    @StringRes val labelRes: Int,
    val iconName: String,
    val capability: String? = null,
    val implemented: Boolean = true,
    val section: MoreSection = when {
        route in setOf("more/admin/flags", "more/admin/tags", "more/admin/verification", "more/admin/verification_stats") -> MoreSection.MODERATE
        route in setOf("more/authoring_drafts", "more/master_exams/manage", "more/master_drafts") -> MoreSection.AUTHORING
        route in setOf("more/help", "more/about", "more/privacy") -> MoreSection.SUPPORT
        route.startsWith("more/master") -> MoreSection.EXAMS
        route.startsWith("more/admin/") -> MoreSection.ADMIN
        route in setOf("history", "more/mistakes", "more/fragile", "more/analytics") -> MoreSection.REVIEW
        route in setOf("more/profile", "more/settings", "more/change_password") -> MoreSection.ACCOUNT
        else -> MoreSection.LEARN
    },
)

/**
 * Material Symbols / Icons names.
 *
 * We only reference names that exist in `material-icons-extended`. The
 * strings are looked up by [MaterialIcons] at render time.
 */
object MaterialIcons {
    const val HOME = "Home"
    const val QUIZ = "Quiz"
    const val BOOKMARK = "Bookmark"
    const val HISTORY = "History"
    const val MORE_HORIZ = "MoreHoriz"
    const val CATEGORIES = "Category"
    const val QUESTIONS = "ListAlt"
    const val MISTAKES = "ErrorOutline"
    const val FRAGILE = "WarningAmber"
    const val ANALYTICS = "Insights"
    const val PROFILE = "Person"
    const val SETTINGS = "Settings"
    const val LOCK = "Lock"
    const val INFO = "Info"
    const val MAP = "Map"
    const val PLANNER = "CalendarMonth"
    const val GROUPS = "Groups"
    const val TAGS = "Label"
    const val VERIFIED = "Verified"
}

object NavigationCatalog {

    /** Canonical destinations; empty and unauthorized drawer sections are omitted. */
    val moreDestinations: List<MoreDestination> = listOf(
        MoreDestination("more/questions", R.string.question_library, MaterialIcons.QUESTIONS),
        MoreDestination("quizzes", R.string.quizzes, MaterialIcons.QUIZ, "tests.start"),
        MoreDestination("more/offline", R.string.offline_title, MaterialIcons.QUIZ),
        MoreDestination("more/planner", R.string.nav_planner, MaterialIcons.PLANNER),
        MoreDestination("more/knowledge_map", R.string.nav_knowledge_map, MaterialIcons.MAP),
        MoreDestination("more/cases", R.string.explore_cases, MaterialIcons.QUESTIONS),
        MoreDestination("more/categories", R.string.nav_categories, MaterialIcons.CATEGORIES),
        MoreDestination("more/groups", R.string.nav_groups, MaterialIcons.GROUPS),
        MoreDestination("bookmarks", R.string.bookmarks, MaterialIcons.BOOKMARK, section = MoreSection.REVIEW),
        MoreDestination("history", R.string.history, MaterialIcons.HISTORY, "tests.view_own_history"),
        MoreDestination("more/mistakes", R.string.nav_mistakes, MaterialIcons.MISTAKES),
        MoreDestination("more/fragile", R.string.nav_fragile, MaterialIcons.FRAGILE),
        MoreDestination("more/activity", R.string.activity_title, MaterialIcons.ANALYTICS, section = MoreSection.REVIEW),
        MoreDestination("more/analytics", R.string.nav_analytics, MaterialIcons.ANALYTICS),
        MoreDestination("more/master_exams", R.string.master_exams_title, MaterialIcons.QUIZ),
        MoreDestination("more/master_results", R.string.exam_results_title, MaterialIcons.ANALYTICS),
        MoreDestination("more/master_csv", R.string.transfer_exam_csv, MaterialIcons.HISTORY),
        MoreDestination("more/profile", R.string.nav_profile, MaterialIcons.PROFILE),
        MoreDestination("more/settings", R.string.nav_settings, MaterialIcons.SETTINGS),
        MoreDestination("more/notices", R.string.notices_title, MaterialIcons.INFO, section = MoreSection.ACCOUNT),
        MoreDestination("more/sessions", R.string.device_sessions_title, MaterialIcons.LOCK, section = MoreSection.ACCOUNT),
        MoreDestination("more/change_password", R.string.nav_change_password, MaterialIcons.LOCK),
        MoreDestination("more/help", R.string.help_title, MaterialIcons.INFO),
        MoreDestination("more/about", R.string.nav_about, MaterialIcons.INFO),
        MoreDestination("more/privacy", R.string.privacy_title, MaterialIcons.LOCK),
        MoreDestination("more/authoring_drafts", R.string.author_drafts_title, MaterialIcons.QUESTIONS),
        MoreDestination("more/master_exams/manage", R.string.exam_manage_title, MaterialIcons.QUIZ),
        MoreDestination("more/master_drafts", R.string.exam_library_title, MaterialIcons.QUESTIONS, "master_exams.drafts_library"),
        MoreDestination("more/admin/flags", R.string.moderation_flags_title, MaterialIcons.MISTAKES, "admin.flags"),
        MoreDestination("more/admin/tags", R.string.moderation_tags_title, MaterialIcons.TAGS, "questions.bulk_tag"),
        MoreDestination("more/admin/verification", R.string.moderation_verification_title, MaterialIcons.VERIFIED, "questions.bulk_verify"),
        MoreDestination("more/admin/verification_stats", R.string.verification_stats_title, MaterialIcons.ANALYTICS, "admin.verification_stats"),
        MoreDestination("more/admin/users", R.string.nav_admin_users, MaterialIcons.GROUPS, "admin.users"),
        MoreDestination("more/admin/active_users", R.string.active_accounts_title, MaterialIcons.GROUPS, "admin.active_users"),
        MoreDestination("more/admin/permissions", R.string.nav_admin_permissions, MaterialIcons.LOCK, "admin.permissions"),
        MoreDestination("more/admin/groups", R.string.manage_groups, MaterialIcons.GROUPS, "groups.admin"),
        MoreDestination("more/admin/tag_management", R.string.taxonomy_title, MaterialIcons.TAGS, "questions.manage_tags"),
        MoreDestination("more/admin/knowledge", R.string.content_knowledge_title, MaterialIcons.MAP),
        MoreDestination("more/admin/cases", R.string.content_cases_title, MaterialIcons.QUESTIONS),
        MoreDestination("more/admin/blueprints", R.string.manage_blueprints, MaterialIcons.QUIZ, "admin.blueprints"),
        MoreDestination("more/admin/history", R.string.all_history, MaterialIcons.HISTORY, "tests.view_all_history"),
        MoreDestination("more/admin/transfers", R.string.transfer_title, MaterialIcons.SETTINGS, "admin.database"),
        MoreDestination("more/admin/tools", R.string.tools_title, MaterialIcons.SETTINGS),
        MoreDestination("more/admin/settings", R.string.nav_admin_settings, MaterialIcons.SETTINGS, "admin.settings"),
    )

    /** Filter the catalog by the user's capabilities. */
    fun visibleFor(user: com.mukhtabir.android.data.remote.UserDto): List<MoreDestination> =
        moreDestinations.filter { destination ->
            (destination.capability?.let(user::has) ?: true) &&
                (destination.route !in setOf("more/master_csv", "more/master_results") || com.mukhtabir.android.data.remote.canExportExamResults(user)) &&
                (destination.route != "more/authoring_drafts" || com.mukhtabir.android.ui.questions.canUseAuthoringDrafts(user)) &&
                (destination.route != "more/admin/knowledge" || com.mukhtabir.android.ui.admin.mayManageContent(user, com.mukhtabir.android.ui.admin.ContentKind.KNOWLEDGE)) &&
                (destination.route != "more/admin/cases" || com.mukhtabir.android.ui.admin.mayManageContent(user, com.mukhtabir.android.ui.admin.ContentKind.CASE)) &&
                (destination.route != "more/admin/tools" || user.has("admin.database") || user.has("admin.seed")) &&
                (destination.route != "more/master_exams/manage" || user.has("master_exams.create") || user.has("master_exams.manage_own") || user.has("master_exams.manage_any"))
        }
}
