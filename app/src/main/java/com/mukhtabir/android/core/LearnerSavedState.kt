package com.mukhtabir.android.core

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle

/** Account-bound navigation metadata only; never store learner content here. */
class LearnerSavedState(
    private val handle: SavedStateHandle,
    private val userId: Long,
    private val key: String,
) {
    init {
        if (handle.get<Bundle>(key)?.getLong("user_id", -1L) != userId) {
            handle.remove<Bundle>(key)
        }
    }

    fun page(): Int = handle.get<Bundle>(key)?.getInt("page", 1)?.coerceAtLeast(1) ?: 1
    fun filter(): String? = handle.get<Bundle>(key)?.getString("filter")
    fun draftFilter(): String? = handle.get<Bundle>(key)?.getString("draft_filter")
    fun expandedId(): Long? = handle.get<Bundle>(key)?.getLong("expanded_id", -1L)?.takeIf { it > 0 }

    fun savePage(page: Int) = update { putInt("page", page.coerceAtLeast(1)) }
    fun saveFilter(filter: String) = update { putString("filter", filter) }
    fun saveDraftFilter(filter: String) = update { putString("draft_filter", filter) }
    fun saveExpandedId(id: Long?) = update {
        if (id == null) remove("expanded_id") else putLong("expanded_id", id)
    }

    private fun update(change: Bundle.() -> Unit) {
        // Replace the Bundle so SavedStateHandle sees every update.
        handle[key] = (handle.get<Bundle>(key)?.let { Bundle(it) } ?: Bundle()).apply {
            putLong("user_id", userId)
            change()
        }
    }
}
