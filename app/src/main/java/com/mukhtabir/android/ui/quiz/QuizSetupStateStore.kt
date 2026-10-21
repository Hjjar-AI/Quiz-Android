package com.mukhtabir.android.ui.quiz

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import com.mukhtabir.android.data.remote.TestMode

/** Only lightweight, account-checked navigation/setup metadata enters saved state. */
internal class QuizSetupStateStore(
    private val savedStateHandle: SavedStateHandle,
    private val userId: Long,
    private val preferences: com.mukhtabir.android.core.AppPreferences,
) {
    fun restore(canUseBlueprint: Boolean): QuizUiState {
        val saved = savedStateHandle.get<Bundle>("quiz_setup")
            ?.takeIf { it.getLong("user_id", -1L) == userId }
            ?: return QuizUiState(difficulty = preferences.display.value.difficulty.takeIf { it.isNotBlank() }).also { savedStateHandle.remove<Bundle>("quiz_setup") }
        val source = saved.getString("source")?.let { name ->
            QuizSource.values().firstOrNull { it.name == name }
        }?.takeUnless { it == QuizSource.BLUEPRINT && !canUseBlueprint }
        val pool = when (source) { null, QuizSource.TAG, QuizSource.CATEGORY -> QuizSource.BOOK; else -> source }
        return QuizUiState(
            mode = TestMode.values().firstOrNull { it.name == saved.getString("mode") } ?: TestMode.EXAM,
            source = pool,
            selectedCategoryIds = saved.getLongArray("categories")?.toSet().orEmpty(),
            selectedTags = saved.getStringArrayList("tags")?.toSet().orEmpty(),
            selectedBook = saved.getString("source_book").takeIf { pool == QuizSource.BOOK },
            selectedBlueprintId = saved.getLong("blueprint", -1L).takeIf { it > 0 && canUseBlueprint },
            additionalTags = saved.getString("additional_tags").orEmpty(),
            difficulty = saved.getString("difficulty")?.takeIf { it in setOf("easy", "medium", "hard") },
            verifiedOnly = saved.getBoolean("verified"),
            requestedCount = saved.getString("count") ?: "10",
            selectionStrategy = saved.getString("selection_strategy")?.takeIf { it in setOf("coverage", "balanced", "review") } ?: "coverage",
            finishPending = saved.getBoolean("finish_pending") && !saved.getString("session_id").isNullOrBlank(),
            recoveryRequired = saved.getBoolean("finish_pending") && !saved.getString("session_id").isNullOrBlank(),
        )
    }

    fun persist(
        current: QuizUiState,
        sessionId: String? = savedStateHandle.get<Bundle>("quiz_setup")?.getString("session_id"),
    ) {
        val previous = savedStateHandle.get<Bundle>("quiz_setup")
        val sessionMode = current.session?.let { current.mode.name }
            ?: current.resumable?.takeIf { it.progress.sessionId == sessionId }?.mode?.name
            ?: previous?.takeIf { it.getString("session_id") == sessionId }?.getString("session_mode")
        savedStateHandle["quiz_setup"] = Bundle().apply {
            putLong("user_id", userId)
            putString("mode", current.mode.name)
            putString("source", current.source?.name)
            putLongArray("categories", current.selectedCategoryIds.toLongArray())
            putStringArrayList("tags", ArrayList(current.selectedTags))
            putString("source_book", current.selectedBook)
            current.selectedBlueprintId?.let { putLong("blueprint", it) }
            putString("additional_tags", current.additionalTags)
            putString("difficulty", current.difficulty)
            putBoolean("verified", current.verifiedOnly)
            putString("count", current.requestedCount)
            putString("selection_strategy", current.selectionStrategy)
            putString("session_id", sessionId)
            putString("session_mode", sessionMode)
            putBoolean("finish_pending",current.finishPending)
        }
    }

}
