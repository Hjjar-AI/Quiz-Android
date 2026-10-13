package com.mukhtabir.android.core

import androidx.annotation.StringRes
import com.mukhtabir.android.R

/** Destination templates and concrete navigation targets share the same chrome identity. */
object ShellNavigation {
    fun base(route: String?): String = route.orEmpty().substringBefore('?')
    fun matches(route: String?, parent: String): Boolean = base(route).let { it == parent || it.startsWith("$parent/") }
    fun parent(route: String?): MoreDestination? {
        val identity = if (matches(route, "more/knowledge")) "more/knowledge_map" else base(route)
        return NavigationCatalog.moreDestinations.filter { matches(identity, it.route) }.maxByOrNull { it.route.length }
    }

    private val focused = setOf(
        "more/questions/create", "more/questions/{questionId}/edit", "more/questions/{questionId}/image",
        "more/master_exams/manage/new", "more/master_exams/manage/{examId}", "more/cases/{caseKey}/stem",
        "more/change_password", "more/admin/users/{userId}/account", "more/admin/users",
        "more/admin/groups", "more/admin/blueprints", "more/admin/transfers", "more/master_preview/{examId}",
        "more/master_results/{examId}", "more/admin/tag_management", "more/admin/knowledge", "more/admin/cases",
        "more/master_drafts", "more/master_drafts/{draftId}",
    )
    // The exam list can restore an attempt; preserve its existing restriction on shell navigation.
    fun preservesDraft(route: String?): Boolean = base(route) in focused
    fun isFocused(route: String?): Boolean = preservesDraft(route) || base(route) == "more/master_exams" || matches(route, "more/master_exams/open")

    @StringRes fun title(route: String?): Int = when (base(route)) {
        "more/questions/create" -> R.string.create_question
        "more/questions/{questionId}/edit" -> R.string.edit_question
        "more/questions/{questionId}", "more/questions/{questionId}/image" -> R.string.question_details
        "more/master_exams/manage/new" -> R.string.exam_author_create
        "more/master_exams/manage/{examId}" -> R.string.exam_author_edit
        "more/master_preview/{examId}" -> R.string.exam_preview_title
        "more/master_results/{examId}" -> R.string.exam_results_title
        "more/master_exams/open/{examId}" -> R.string.master_exams_title
        "more/admin/users/{userId}/account" -> R.string.account_tools_title
        "more/knowledge/{knowledgeId}" -> R.string.explore_knowledge_detail
        "more/cases/{caseKey}", "more/cases/{caseKey}/stem" -> R.string.explore_cases
        else -> parent(route)?.labelRes ?: R.string.more_title
    }
}
