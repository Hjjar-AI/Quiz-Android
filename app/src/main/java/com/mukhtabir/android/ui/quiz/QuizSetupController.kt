package com.mukhtabir.android.ui.quiz

import com.mukhtabir.android.data.remote.QuestionService
import com.mukhtabir.android.data.remote.TestMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import kotlinx.coroutines.CoroutineScope

/** Setup selections and cancellable filter/count reads; no session mutations. */
internal class QuizSetupController(
    private val _state: MutableStateFlow<QuizUiState>,
    private val scope: CoroutineScope,
    private val questionService: QuestionService,
    private val blueprintPermission: () -> Boolean,
    private val persistSetup: () -> Unit,
) {
    private val canUseBlueprint: Boolean get() = blueprintPermission()
    private var countJob: Job? = null
    private var bookFiltersJob: Job? = null

    fun setMode(mode: TestMode) {
        if (_state.value.session != null || _state.value.isLoading || _state.value.recoveryRequired) return
        updateSetup { it.copy(mode = mode) }
    }

    fun setSource(source: QuizSource) {
        val current = _state.value
        val pool = if (source == QuizSource.TAG || source == QuizSource.CATEGORY) QuizSource.BOOK else source
        if (current.isLoading || current.recoveryRequired || current.session != null || current.source == pool) return
        bookFiltersJob?.cancel()
        updateSetup { it.copy(source = pool, selectedBook = null, selectedBlueprintId = null,
            bookFiltersLoading = false, bookFiltersFor = null, bookFiltersError = null) }
        queueAvailableCount()
    }

    /** Remove only the book restriction, keeping optional category/tag filters. */
    fun useWholeBank() {
        val current = _state.value
        if (current.isLoading || current.recoveryRequired || current.session != null) return
        if (current.source == QuizSource.BOOK && current.selectedBook == null) return
        bookFiltersJob?.cancel()
        updateSetup { it.copy(source = QuizSource.BOOK, selectedBook = null, selectedBlueprintId = null,
            bookFiltersFor = null, bookFiltersLoading = false, bookFiltersError = null) }
        queueAvailableCount()
    }

    fun selectBook(name: String) {
        val current = _state.value
        if (current.isLoading || current.recoveryRequired || current.session != null || current.source != QuizSource.BOOK ||
            current.selectedBook == name || current.books.none { it.name == name }) return
        updateSetup { it.copy(selectedBook = name, bookFiltersFor = null, bookFiltersError = null) }
        refreshBookFilters()
        queueAvailableCount()
    }

    fun clearActiveFilters() {
        val current = _state.value
        if (current.isLoading || current.recoveryRequired || current.session != null) return
        bookFiltersJob?.cancel()
        updateSetup { it.copy(selectedBook = null, selectedCategoryIds = emptySet(), selectedTags = emptySet(), additionalTags = "",
            bookFiltersFor = null, bookFiltersLoading = false, bookFiltersError = null) }
        queueAvailableCount()
    }

    fun clearFilters() {
        val current = _state.value
        if (current.isLoading || current.recoveryRequired || current.session != null) return
        updateSetup { it.copy(selectedCategoryIds = emptySet(), selectedTags = emptySet(), additionalTags = "") }
        queueAvailableCount()
    }

    fun removeCategoryFilter(id: Long) {
        val current = _state.value
        if (current.isLoading || current.recoveryRequired || current.session != null || id !in current.selectedCategoryIds) return
        updateSetup { it.copy(selectedCategoryIds = it.selectedCategoryIds - id) }
        queueAvailableCount()
    }

    fun removeTagFilter(name: String) {
        val current = _state.value
        if (current.isLoading || current.recoveryRequired || current.session != null) return
        updateSetup { it.copy(selectedTags = it.selectedTags - name,
            additionalTags = it.additionalTags.split(',').map(String::trim)
                .filter { tag -> tag.isNotEmpty() && tag != name }.joinToString(", ")) }
        queueAvailableCount()
    }

    fun refreshBookFilters() {
        if (_state.value.session != null || _state.value.isLoading || _state.value.recoveryRequired) return
        bookFiltersJob?.cancel()
        val snapshot = _state.value
        val name = snapshot.selectedBook?.takeIf { snapshot.source == QuizSource.BOOK } ?: run {
            _state.update { it.copy(bookFiltersLoading = false) }
            return
        }
        _state.update { it.copy(bookFiltersLoading = true, bookFiltersError = null) }
        bookFiltersJob = scope.launch {
            try {
                val data = questionService.sourceBookFilters(name)
                kotlin.coroutines.coroutineContext.ensureActive()
                if (_state.value.selectedBook != name || _state.value.source != QuizSource.BOOK) return@launch
                if (_state.value.session != null) {
                    _state.update { it.copy(bookFiltersLoading = false) }
                    return@launch
                }
                check(data.sourceDocument == name)
                updateSetup { it.copy(bookCategories = data.categories, bookTags = data.tags,
                    selectedCategoryIds = it.selectedCategoryIds.intersect(data.categories.map { c -> c.id }.toSet()),
                    selectedTags = it.selectedTags.intersect(data.tags.map { tag -> tag.name }.toSet()),
                    bookFiltersFor = name, bookFiltersLoading = false, bookFiltersError = null) }
                if (!_state.value.isLoading && !_state.value.recoveryRequired) queueAvailableCount()
            } catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) {
                if (_state.value.selectedBook == name && _state.value.source == QuizSource.BOOK)
                    _state.update { it.copy(bookFiltersLoading = false, bookFiltersError = failure) }
            }
        }
    }

    fun toggleTag(name: String) {
        val snapshot = _state.value
        if (snapshot.isLoading || snapshot.recoveryRequired || snapshot.session != null) return
        if (snapshot.source == QuizSource.BOOK && snapshot.selectedBook != null && (snapshot.bookFiltersLoading || snapshot.bookFiltersFor != snapshot.selectedBook || snapshot.bookTags.none { it.name == name })) return
        updateSetup { current ->
            val selected = current.selectedTags.toMutableSet().apply {
                if (!add(name)) remove(name)
            }
            current.copy(selectedTags = selected)
        }
        queueAvailableCount()
    }

    fun toggleCategory(id: Long) {
        val snapshot = _state.value
        if (snapshot.isLoading || snapshot.recoveryRequired || snapshot.session != null) return
        if (snapshot.source == QuizSource.BOOK && snapshot.selectedBook != null && (snapshot.bookFiltersLoading || snapshot.bookFiltersFor != snapshot.selectedBook || snapshot.bookCategories.none { it.id == id })) return
        updateSetup { current ->
            val selected = current.selectedCategoryIds.toMutableSet().apply {
                if (!add(id)) remove(id)
            }
            current.copy(selectedCategoryIds = selected)
        }
        queueAvailableCount()
    }

    fun selectBlueprint(id: Long) {
        val current = _state.value
        if (current.isLoading || current.recoveryRequired || current.session != null || !canUseBlueprint ||
            current.blueprints.none { it.id == id && it.isActive }) return
        updateSetup { it.copy(selectedBlueprintId = id) }
        queueAvailableCount()
    }

    fun setSelectionStrategy(value: String) {
        if (_state.value.isLoading || _state.value.recoveryRequired || _state.value.session != null) return
        if (value in setOf("coverage", "balanced", "review")) updateSetup { it.copy(selectionStrategy = value) }
    }

    fun setDifficulty(value: String?) {
        if (_state.value.isLoading || _state.value.recoveryRequired || _state.value.session != null ||
            value !in setOf(null, "easy", "medium", "hard")) return
        updateSetup { it.copy(difficulty = value) }
        queueAvailableCount()
    }

    fun setVerifiedOnly(value: Boolean) {
        if (_state.value.isLoading || _state.value.recoveryRequired || _state.value.session != null) return
        updateSetup { it.copy(verifiedOnly = value) }
        queueAvailableCount()
    }

    fun setAdditionalTags(value: String) {
        if (_state.value.isLoading || _state.value.recoveryRequired || _state.value.session != null) return
        updateSetup { it.copy(additionalTags = value) }
        queueAvailableCount()
    }

    fun setRequestedCount(value: String) {
        if (_state.value.isLoading || _state.value.recoveryRequired || _state.value.session != null) return
        if (value.isEmpty() || value.all(Char::isDigit)) {
            updateSetup { it.copy(requestedCount = value) }
        }
    }

    fun applyMaxQuizQuestions(value: Int) {
        val maximum = value.coerceAtLeast(1)
        updateSetup { it.copy(maxQuizQuestions = maximum) }
    }

    fun queueAvailableCount() {
        countJob?.cancel()
        val snapshot = _state.value
        if (snapshot.source == QuizSource.BOOK && snapshot.selectedBook != null && snapshot.bookFiltersFor != snapshot.selectedBook &&
                !snapshot.bookFiltersLoading && snapshot.bookFiltersError == null) refreshBookFilters()
        val selection = selectionFrom(_state.value)
        if (selection == null) {
            _state.update { it.copy(availableCount = 0, isCountLoading = false, countError = null) }
            return
        }
        _state.update { it.copy(availableCount = 0, isCountLoading = true, countError = null) }
        countJob = scope.launch {
            delay(250)
            try {
                val count = questionService.availableCount(
                    categoryIds = selection.categoryIds,
                    difficulty = snapshot.difficulty,
                    verifiedOnly = snapshot.verifiedOnly,
                    useBookmarks = selection.useBookmarks,
                    useSrs = selection.useSrs,
                    tags = selection.tags,
                    sourceDocument = selection.sourceDocument,
                    blueprintId = selection.blueprintId,
                )
                kotlin.coroutines.coroutineContext.ensureActive()
                updateSetup { it.copy(availableCount = count, isCountLoading = false, countError = null) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                kotlin.coroutines.coroutineContext.ensureActive()
                _state.update {
                    it.copy(
                        isCountLoading = false,
                        availableCount = 0,
                        countError = error,
                    )
                }
            }
        }
    }

    fun selectionFrom(state: QuizUiState): SetupSelection? {
        val categories = state.selectedCategoryIds.toList()
        val tags = (state.selectedTags + state.additionalTags.split(',').map { it.trim() }.filter { it.isNotEmpty() }).distinct()
        return when (state.source) {
            QuizSource.BOOK, QuizSource.TAG, QuizSource.CATEGORY -> {
                val book = state.selectedBook
                if (book != null && (state.books.none { it.name == book } ||
                    ((categories.isNotEmpty() || state.selectedTags.isNotEmpty()) &&
                        (state.bookFiltersFor != book || state.bookFiltersLoading || state.bookFiltersError != null)))) null
                else SetupSelection(sourceDocument = book, categoryIds = categories, tags = tags, label = book)
            }
            QuizSource.BOOKMARKS -> if (state.bookmarkCount > 0)
                SetupSelection(useBookmarks = true, categoryIds = categories, tags = tags, label = null) else null
            QuizSource.SRS -> if (state.srsDueCount > 0)
                SetupSelection(useSrs = true, categoryIds = categories, tags = tags, label = null) else null
            QuizSource.BLUEPRINT -> state.selectedBlueprintId?.takeIf { id ->
                canUseBlueprint && state.blueprints.any { it.id == id && it.isActive }
            }?.let { id -> SetupSelection(blueprintId = id, categoryIds = categories, tags = tags,
                label = state.blueprints.firstOrNull { it.id == id }?.name) }
            null -> null
        }
    }

    fun updateSetup(transform: (QuizUiState) -> QuizUiState) {
        _state.update(transform)
        persistSetup()
    }

    fun cancelCount() { countJob?.cancel() }
}

internal data class SetupSelection(
    val sourceDocument: String? = null,
    val categoryIds: List<Long> = emptyList(),
    val tags: List<String> = emptyList(),
    val useBookmarks: Boolean = false,
    val useSrs: Boolean = false,
    val blueprintId: Long? = null,
    val label: String?,
)
