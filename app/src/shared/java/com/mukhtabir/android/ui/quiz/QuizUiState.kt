package com.mukhtabir.android.ui.quiz

import com.mukhtabir.android.core.PendingSessionFinish
import com.mukhtabir.android.core.QuizPreset
import com.mukhtabir.android.core.SessionDraft
import com.mukhtabir.android.data.remote.BlueprintDto
import com.mukhtabir.android.data.remote.CategoryDto
import com.mukhtabir.android.data.remote.ExamSessionDto
import com.mukhtabir.android.data.remote.SessionProgressDto
import com.mukhtabir.android.data.remote.SessionQuestionPageDto
import com.mukhtabir.android.data.remote.SessionResultsDto
import com.mukhtabir.android.data.remote.SourceBookDto
import com.mukhtabir.android.data.remote.TagDto
import com.mukhtabir.android.data.remote.TestMode
import kotlinx.serialization.json.JsonObject

enum class QuizSource {
    BOOK,
    TAG,
    CATEGORY,
    BOOKMARKS,
    SRS,
    BLUEPRINT,
}

data class SavedSession(
    val mode: TestMode,
    val progress: SessionProgressDto,
)

data class QuizUiState(
    val flaggedIndices: Set<Int> = emptySet(),
    val flagError: Boolean = false,
    val flagReadFailed: Boolean = false,
    val launchPreset: QuizPreset? = null,
    val downloaded: Boolean = false,
    val queuedAnswers: Int = 0,
    val syncRequired: Boolean = false,
    val syncing: Boolean = false,
    val syncError: Throwable? = null,
    val locallyFinished: Boolean = false,
    val syncConflict: SessionQuestionPageDto? = null,
    val syncConflictAnswer: Int? = null,
    val setupLoaded: Boolean = false,
    val recoveryRequired: Boolean = false,
    val pendingDraft: SessionDraft? = null,
    val draftCanRetry: Boolean = false,
    val draftStorageFailed: Boolean = false,
    val mode: TestMode = TestMode.EXAM,
    val source: QuizSource? = QuizSource.BOOK,
    val categories: List<CategoryDto> = emptyList(),
    val tags: List<TagDto> = emptyList(),
    val books: List<SourceBookDto> = emptyList(),
    val selectedBook: String? = null,
    val bookFiltersFor: String? = null,
    val bookCategories: List<CategoryDto> = emptyList(),
    val bookTags: List<TagDto> = emptyList(),
    val bookFiltersLoading: Boolean = false,
    val bookFiltersError: Throwable? = null,
    val blueprints: List<BlueprintDto> = emptyList(),
    val metadataLoading: Boolean = false,
    val metadataError: Throwable? = null,
    val bookmarkCount: Int = 0,
    val questionBookmarks: Map<Long, Boolean> = emptyMap(),
    val bookmarkBusyIds: Set<Long> = emptySet(),
    val bookmarkUncertainIds: Set<Long> = emptySet(),
    val bookmarkErrors: Map<Long, String> = emptyMap(),
    val srsDueCount: Int = 0,
    val selectedCategoryIds: Set<Long> = emptySet(),
    val selectedTags: Set<String> = emptySet(),
    val selectedBlueprintId: Long? = null,
    val additionalTags: String = "",
    val difficulty: String? = null,
    val verifiedOnly: Boolean = false,
    val requestedCount: String = "10",
    val selectionStrategy: String = "coverage",
    val availableCount: Int = 0,
    val maxQuizQuestions: Int = 200,
    val isCountLoading: Boolean = false,
    val countError: Throwable? = null,
    val session: ExamSessionDto? = null,
    val resumable: SavedSession? = null,
    val questionPage: SessionQuestionPageDto? = null,
    val selectedAnswer: Int? = null,
    val confidence: Int? = 3,
    val preAnswer: String = "",
    val choicesRevealed: Boolean = true,
    val explanation: String? = null,
    val feedbackTranslations: JsonObject = JsonObject(emptyMap()),
    val isCorrect: Boolean? = null,
    val errorReason: String? = null,
    val showFeedback: Boolean = false,
    val answeredIndices: Set<Int> = emptySet(),
    val autoAdvanceToken: Long = 0,
    val confirmedSaveToken: Long = 0,
    val results: SessionResultsDto? = null,
    val reviewLoading: Boolean = false,
    val reviewFailed: Boolean = false,
    val reviewError: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val hasUnknownError: Boolean = false,
    val finishPending: Boolean = false,
    val finishStatusChecked: Boolean = false,
    val finishedSessionMissing: Boolean = false,
    val finishRecovery: PendingSessionFinish? = null,
    val finishStorageFailed: Boolean = false,
)
