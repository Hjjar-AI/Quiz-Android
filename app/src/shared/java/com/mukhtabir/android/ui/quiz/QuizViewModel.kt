package com.mukhtabir.android.ui.quiz

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.mukhtabir.android.core.DownloadedSession
import com.mukhtabir.android.core.PendingSessionFinish
import com.mukhtabir.android.core.QuizPreset
import com.mukhtabir.android.core.SessionDraft
import com.mukhtabir.android.core.SessionDraftKind
import com.mukhtabir.android.core.LearnerStorage
import com.mukhtabir.android.data.remote.AnswerSubmissionDto
import com.mukhtabir.android.data.remote.ApiException
import com.mukhtabir.android.data.remote.BlueprintDto
import com.mukhtabir.android.data.remote.LearnerBlueprints
import com.mukhtabir.android.data.remote.LearnerBookmarks
import com.mukhtabir.android.data.remote.CategoryDto
import com.mukhtabir.android.data.remote.LearnerCategories
import com.mukhtabir.android.data.remote.ExamSessionDto
import com.mukhtabir.android.data.remote.LearnerQuestions
import com.mukhtabir.android.data.remote.SessionAnswerBaseline
import com.mukhtabir.android.data.remote.SessionProgressDto
import com.mukhtabir.android.data.remote.SessionQuestionPageDto
import com.mukhtabir.android.data.remote.SessionResultsDto
import com.mukhtabir.android.data.remote.SourceBookDto
import com.mukhtabir.android.data.remote.StartSessionRequest
import com.mukhtabir.android.data.remote.SubmitAnswerRequest
import com.mukhtabir.android.data.remote.TagDto
import com.mukhtabir.android.data.remote.TestMode
import com.mukhtabir.android.data.remote.LearnerSessions
import java.time.Duration
import java.time.OffsetDateTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

class QuizViewModel(
    private val questionService: LearnerQuestions,
    private val sessionService: LearnerSessions,
    private val bookmarkService: LearnerBookmarks,
    private val categoryService: LearnerCategories,
    private val blueprintService: LearnerBlueprints,
    private var canUseBlueprint: Boolean,
    private val userId: Long,
    private val savedStateHandle: SavedStateHandle,
    private val draftStore: LearnerStorage,
    private val preferences: com.mukhtabir.android.core.AppPreferences,
    private val imageRepository: com.mukhtabir.android.data.remote.LearnerImages,
) : ViewModel() {
    private val setupState = QuizSetupStateStore(savedStateHandle, userId, preferences)
    private val _state = MutableStateFlow(setupState.restore(canUseBlueprint))
    private val setup = QuizSetupController(_state, viewModelScope, questionService,
        blueprintPermission = { canUseBlueprint }, persistSetup = { persistSetup() })
    val state = _state.asStateFlow()
    private val _clock = MutableStateFlow(QuizClock())
    internal val clock = _clock.asStateFlow()
    private var timerJob: Job? = null
    private var initializationJob: Job? = null
    private var metadataJob: Job? = null
    private var downloadJob: Job? = null
    private var pendingFinishWrite: PendingSessionFinish? = null
    private var downloadedSession: DownloadedSession? = null
    private var downloadedTimerDisabled = false
    private var pendingFinishClear = false
    private var bookmarkCountGeneration = 0L

    init {
        initialize()
    }

    fun configureBlueprintPermission(allowed: Boolean) {
        val changed=canUseBlueprint!=allowed
        if(changed) metadataJob?.cancel()
        canUseBlueprint=allowed
        if(!allowed && _state.value.session==null) updateSetup {it.copy(selectedBlueprintId=null,blueprints=emptyList(),metadataLoading=false)}
        if(changed && allowed) refreshMetadata()
    }

    fun refreshMetadata() {
        if(!_state.value.setupLoaded || _state.value.session!=null || _state.value.isLoading || _state.value.recoveryRequired || initializationJob?.isActive==true) return
        metadataJob?.cancel()
        _state.update {it.copy(metadataLoading=true,metadataError=null)}
        metadataJob=viewModelScope.launch {
            try {
                val loaded=coroutineScope {
                    suspend fun <T> read(block: suspend () -> T): Result<T> = try {Result.success(block())}
                    catch(cancel: CancellationException) {throw cancel}
                    catch(failure: Exception) {Result.failure(failure)}
                    val categories=async {read {categoryService.list().items}}
                    val tags=async {read {questionService.tags().items}}
                    val books=async {read {questionService.sourceBooks().items}}
                    val blueprints=async {read {if(canUseBlueprint) blueprintService.list().items.filter {it.isActive} else emptyList()}}
                    val bookmarks=async {read {bookmarkService.count()}}
                    val due=async {read {questionService.srsDueCount()}}
                    SetupMetadata(categories.await(), tags.await(), blueprints.await(), books.await(), bookmarks.await(), due.await())
                }
                kotlin.coroutines.coroutineContext.ensureActive()
                if(_state.value.session!=null || _state.value.isLoading || _state.value.recoveryRequired) return@launch
                updateSetup {current ->
                    val categories=loaded.categories.getOrNull() ?: current.categories
                    val tags=loaded.tags.getOrNull() ?: current.tags
                    val blueprints=loaded.blueprints.getOrNull() ?: current.blueprints
                    val books=loaded.books.getOrNull() ?: current.books
                    current.copy(books=books,
                        bookmarkCount=loaded.bookmarks.getOrNull() ?: current.bookmarkCount,
                        srsDueCount=loaded.due.getOrNull() ?: current.srsDueCount,
                        selectedBook=current.selectedBook,
                        categories=categories,tags=tags,blueprints=blueprints,
                        selectedCategoryIds=if(loaded.categories.isSuccess) current.selectedCategoryIds.intersect(categories.map {it.id}.toSet()) else current.selectedCategoryIds,
                        selectedTags=if(loaded.tags.isSuccess) current.selectedTags.intersect(tags.map {it.name}.toSet()) else current.selectedTags,
                        selectedBlueprintId=if(loaded.blueprints.isSuccess) current.selectedBlueprintId?.takeIf {id -> blueprints.any {it.id==id}} else current.selectedBlueprintId,
                        metadataLoading=false,metadataError=loaded.categories.exceptionOrNull() ?: loaded.tags.exceptionOrNull() ?: loaded.blueprints.exceptionOrNull() ?: loaded.books.exceptionOrNull() ?: loaded.bookmarks.exceptionOrNull() ?: loaded.due.exceptionOrNull())
                }
                queueAvailableCount()
            } finally {
                if(kotlin.coroutines.coroutineContext.isActive) _state.update {it.copy(metadataLoading=false)}
            }
        }
    }

    fun initialize() {
        if (_state.value.syncing) return
        if (initializationJob?.isActive == true) return
        metadataJob?.cancel()
        downloadJob?.cancel()
        _state.update {it.copy(metadataLoading=false)}
        timerJob?.cancel()
        setup.cancelCount()
        val localDraft = _state.value.pendingDraft
        val localStorageFailed = _state.value.draftStorageFailed && localDraft != null
        val finishRead=draftStore.readFinish(userId)
        val finish=pendingFinishWrite ?: finishRead.getOrNull() ?: _state.value.finishRecovery.takeIf { finishRead.isFailure }
        _state.update { it.copy(finishRecovery=finish,finishPending=it.finishPending || finish!=null,
            finishStorageFailed=finishRead.isFailure || pendingFinishWrite!=null || pendingFinishClear,
            recoveryRequired=it.recoveryRequired || finishRead.isFailure || finish!=null) }
        val storedDraft = draftStore.read(userId)
        _state.update { it.copy(pendingDraft = if (localStorageFailed) localDraft else
            storedDraft.getOrNull() ?: localDraft.takeIf { storedDraft.isFailure }, draftCanRetry = false,
            draftStorageFailed = storedDraft.isFailure || localStorageFailed) }
        _state.update { it.copy(isLoading = true, setupLoaded = false, error = null, hasUnknownError = false, metadataError = null,finishStatusChecked=false) }
        val downloadedRead = draftStore.readDownloadedSession(userId)
        if (downloadedRead.isFailure) {
            _state.update { it.copy(draftStorageFailed = true, recoveryRequired = true, isLoading = false) }
            return
        }
        val retained = downloadedSession ?: downloadedRead.getOrNull()
        if (retained != null && finish == null && _state.value.pendingDraft == null && retained.session.isActive) {
            downloadedSession = retained
            loadFlags(retained.session.sessionId, retained.session.questionIds.size)
            _state.update { it.copy(mode = TestMode.values().first { mode -> mode.path == retained.session.mode },
                session = retained.session, setupLoaded = true, downloaded = true,
                queuedAnswers = retained.answers.size, syncRequired = retained.answers.isNotEmpty() || retained.locallyFinished,
                locallyFinished = retained.locallyFinished,
                answeredIndices = downloadedAnsweredIndices(retained),
                recoveryRequired = false, isLoading = false) }
            applyDownloadedPage(retained.session.currentIndex)
            if (retained.locallyFinished) _clock.value = QuizClock(retained.finishedTimerSeconds, retained.finishedTimerOvertime)
            else startTimer(if (retained.timerDisabled) retained.session.copy(durationMinutes = null) else retained.session)
            return
        }
        initializationJob = viewModelScope.launch {
            runCatching {
                coroutineScope {
                    val categories = async {
                        readSetupMetadata(_state.value.categories) { categoryService.list().items }
                    }
                    val tags = async {
                        readSetupMetadata(_state.value.tags) { questionService.tags().items }
                    }
                    val books = async {
                        readSetupMetadata(_state.value.books) { questionService.sourceBooks().items }
                    }
                    val bookmarks = async {
                        readSetupMetadata(_state.value.bookmarkCount) { bookmarkService.count() }
                    }
                    val srsDue = async {
                        readSetupMetadata(_state.value.srsDueCount) { questionService.srsDueCount() }
                    }
                    val blueprints = async {
                        if (canUseBlueprint) {
                            readSetupMetadata(_state.value.blueprints) { blueprintService.list().items.filter { it.isActive } }
                        } else {
                            emptyList()
                        }
                    }
                    val preferredId = _state.value.finishRecovery?.sessionId ?: _state.value.pendingDraft?.sessionId ?: savedStateHandle.get<Bundle>("quiz_setup")
                        ?.getString("session_id")
                    val preferredMode = TestMode.values().firstOrNull { it.path == _state.value.finishRecovery?.mode }
                        ?: TestMode.values().firstOrNull { it.path == _state.value.pendingDraft?.mode }
                        ?: savedStateHandle.get<Bundle>("quiz_setup")
                        ?.getString("session_mode")?.let { name ->
                            TestMode.values().firstOrNull { it.name == name }
                        } ?: TestMode.values().firstOrNull { it.path == _state.value.pendingDraft?.mode }
                        ?: _state.value.mode
                    val preferred = preferredId?.let {
                        sessionService.status(preferredMode, it)
                    }?.takeIf { it.sessionId != null }
                    val completed = if (_state.value.finishPending && preferredId != null && preferred == null) {
                        try {
                            sessionService.completedResult(preferredMode, preferredId)
                        } catch (error: ApiException) {
                            // Old backend or historical/discarded session: retain the explicit History fallback.
                            if (error.statusCode in setOf(404,405)) null else throw error
                        }
                    } else null
                    val statuses = if (completed != null) emptyList() else listOf(
                        TestMode.EXAM,
                        TestMode.STUDY,
                        TestMode.RECALL,
                    ).map { mode ->
                        async { mode to sessionService.status(mode) }
                    }.map { it.await() } + if (preferred != null) {
                        listOf(preferredMode to preferred)
                    } else {
                        emptyList()
                    }
                    Initialization(
                        categories = categories.await(),
                        tags = tags.await(),
                        books = books.await(),
                        bookmarkCount = bookmarks.await(),
                        srsDueCount = srsDue.await(),
                        blueprints = blueprints.await(),
                        statuses = statuses,
                        completed = completed,
                    )
                }
            }.onSuccess { initialization ->
                initialization.completed?.let { completed ->
                    if (_state.value.pendingDraft?.sessionId == completed.sourceSessionId) forgetDraft()
                    if (retainDownloaded(null)) clearFinishRecord()
                    _state.update { it.copy(results=completed,session=null,questionPage=null,resumable=null,
                        setupLoaded=true,isLoading=false,recoveryRequired=it.finishStorageFailed,finishPending=false,
                        finishStatusChecked=false,finishedSessionMissing=false,finishRecovery=null,error=null,hasUnknownError=false,
                        categories=initialization.categories,tags=initialization.tags,books=initialization.books,
                        bookmarkCount=initialization.bookmarkCount,srsDueCount=initialization.srsDueCount,
                        blueprints=initialization.blueprints) }
                    persistSetup(sessionId=null)
                    return@onSuccess
                }
                val saved = initialization.statuses.mapNotNull { (mode, progress) ->
                    progress?.sessionId?.let { SavedSession(mode, progress) }
                }.distinctBy { it.progress.sessionId }
                val preferredId = _state.value.finishRecovery?.sessionId ?: _state.value.pendingDraft?.sessionId ?: savedStateHandle.get<Bundle>("quiz_setup")?.getString("session_id")
                val paused = saved.filter { !it.progress.isActive }
                val pendingFinish=_state.value.finishPending
                _state.update {
                    it.copy(
                        setupLoaded = true,
                        recoveryRequired = it.finishPending || it.finishStorageFailed,
                        finishStatusChecked = it.finishPending,
                        finishedSessionMissing = it.finishPending && saved.none { session -> session.progress.sessionId==preferredId },
                        session = null,
                        questionPage = null,
                        categories = initialization.categories,
                        tags = initialization.tags,
                        books = initialization.books,
                        bookmarkCount = initialization.bookmarkCount,
                        srsDueCount = initialization.srsDueCount,
                        blueprints = initialization.blueprints,
                        resumable = paused.firstOrNull { it.progress.sessionId == preferredId }
                            ?: paused.firstOrNull().takeUnless { pendingFinish },
                        isLoading = false,
                    )
                }
                val activeSessions = saved.filter { it.progress.isActive }
                val active = activeSessions.firstOrNull { it.progress.sessionId == preferredId }
                    ?: activeSessions.firstOrNull().takeUnless { _state.value.finishPending }
                if (active != null) {
                    restoreActive(active)
                } else {
                    persistSetup(sessionId = if(_state.value.finishPending) preferredId else _state.value.resumable?.progress?.sessionId)
                    queueAvailableCount()
                }
            }.onFailure(::handleSessionFailure)
        }
    }

    fun setMode(mode: TestMode) { setup.setMode(mode) }
    fun setSource(source: QuizSource) { setup.setSource(source) }
    fun useWholeBank() { setup.useWholeBank() }
    fun selectBook(name: String) { setup.selectBook(name) }
    fun clearActiveFilters() { setup.clearActiveFilters() }
    fun clearFilters() { setup.clearFilters() }
    fun removeCategoryFilter(id: Long) { setup.removeCategoryFilter(id) }
    fun removeTagFilter(name: String) { setup.removeTagFilter(name) }
    fun refreshBookFilters() { setup.refreshBookFilters() }
    fun toggleTag(name: String) { setup.toggleTag(name) }
    fun toggleCategory(id: Long) { setup.toggleCategory(id) }
    fun selectBlueprint(id: Long) { setup.selectBlueprint(id) }
    fun setSelectionStrategy(value: String) { setup.setSelectionStrategy(value) }
    fun setDifficulty(value: String?) { setup.setDifficulty(value) }
    fun setVerifiedOnly(value: Boolean) { setup.setVerifiedOnly(value) }
    fun setAdditionalTags(value: String) { setup.setAdditionalTags(value) }
    fun setRequestedCount(value: String) { setup.setRequestedCount(value) }
    fun applyMaxQuizQuestions(value: Int) { setup.applyMaxQuizQuestions(value) }

    fun start() {
        val snapshot = _state.value
        val selection = selectionFrom(snapshot) ?: return
        val effectiveMaximum = minOf(
            snapshot.availableCount.coerceAtLeast(1),
            snapshot.maxQuizQuestions,
        )
        val limit = snapshot.requestedCount.toIntOrNull()?.takeIf { it in 1..effectiveMaximum } ?: return
        if (!snapshot.setupLoaded || snapshot.session != null || snapshot.availableCount <= 0 || snapshot.isCountLoading || snapshot.isLoading || snapshot.recoveryRequired || snapshot.pendingDraft != null || snapshot.draftStorageFailed) return

        setBusy()
        viewModelScope.launch {
            runCatching {
                val session = sessionService.start(
                    snapshot.mode,
                    StartSessionRequest(
                        limit = limit,
                        selectionStrategy = snapshot.selectionStrategy,
                        categoryIds = selection.categoryIds,
                        difficulty = snapshot.difficulty,
                        verifiedOnly = snapshot.verifiedOnly,
                        useBookmarks = selection.useBookmarks,
                        useSrs = selection.useSrs,
                        tagsFilter = selection.tags,
                        sourceDocument = selection.sourceDocument,
                        sessionLabel = selection.label,
                        blueprintId = selection.blueprintId,
                    ),
                )
                session to sessionService.question(snapshot.mode, session.sessionId)
            }.onSuccess { (session, question) ->
                showSession(
                    mode = snapshot.mode,
                    session = session,
                    question = question,
                    timerDisabled = selection.useSrs && snapshot.mode != TestMode.EXAM,
                )
            }.onFailure(::handleSessionFailure)
        }
    }

    /**
     * Start a session from an external preset (Phase 1).
     *
     * Called by [QuizRoute] after consuming a pending [QuizPreset]
     * dropped by the Home / Mistakes / Fragile screens. The preset
     * bypasses the setup screen and starts the session with the exact
     * ordered question-id list the caller supplied.
     *
     * The backend re-validates every id against the caller's
     * visibility rules and returns 400 if any is unavailable — a
     * stale preset (e.g. the user's draft was deleted between the
     * list render and the tap) surfaces as an error in the quiz
     * screen, which is the honest outcome.
     *
     * Guards:
     *   • An empty id list is a no-op. The caller filters empty lists
     *     before setting the preset, but a defensive guard here keeps
     *     a future caller from tripping the backend's "at least one
     *     id" rule.
     *   • A running session blocks the preset. The user is looking at
     *     an in-flight session; a queued start would replace it
     *     silently.
     */
    fun startPreset(preset: QuizPreset) {
        if (preset.questionIds.isEmpty()) return
        if (!_state.value.setupLoaded || _state.value.isLoading || _state.value.recoveryRequired || _state.value.session != null || _state.value.pendingDraft != null || _state.value.draftStorageFailed) return
        _state.update { it.copy(launchPreset = preset) }
        if (preset.questionIds.size > _state.value.maxQuizQuestions || preset.questionIds.any { it <= 0 } ||
            preset.questionIds.distinct().size != preset.questionIds.size) return
        setBusy()
        viewModelScope.launch {
            runCatching {
                val questionIds = preset.questionIds
                val session = sessionService.start(
                    preset.mode,
                    StartSessionRequest(
                        limit = questionIds.size,
                        questionIds = questionIds,
                        sessionLabel = preset.sessionLabel,
                    ),
                )
                session to sessionService.question(preset.mode, session.sessionId)
            }.onSuccess { (session, question) ->
                showSession(
                    mode = preset.mode,
                    session = session,
                    question = question,
                )
            }.onFailure(::handleSessionFailure)
        }
    }

    fun discardLaunchPreset() { if (!_state.value.isLoading) _state.update { it.copy(launchPreset = null) } }

    fun practiceResults(preset: QuizPreset) {
        val result = _state.value.results ?: return
        if (result.reviewWithheld || preset.questionIds.isEmpty() ||
            preset.questionIds.any { id -> result.results.none { it.questionId == id } }) return
        returnToSetup()
        if (_state.value.results == null) startPreset(preset)
    }

    fun studyResultCategory(categoryId: Long) {
        val current = _state.value
        val result = current.results ?: return
        if (result.reviewWithheld || result.results.none { it.categoryId == categoryId } ||
            current.categories.none { it.id == categoryId }) return
        returnToSetup()
        if (_state.value.results != null) return
        updateSetup { it.copy(mode = TestMode.STUDY, source = QuizSource.BOOK, selectedBook = null,
            selectedCategoryIds = setOf(categoryId), selectedTags = emptySet(), additionalTags = "",
            selectedBlueprintId = null, difficulty = null, verifiedOnly = false) }
        queueAvailableCount()
    }

    private fun loadFlags(sessionId: String, total: Int) {
        val read = draftStore.readFlags(userId, sessionId)
        _state.update { it.copy(flaggedIndices = if (read.isSuccess) read.getOrThrow().filter { i -> i in 0 until total }.toSet()
            else if (it.session?.sessionId == sessionId) it.flaggedIndices else emptySet(), flagError = read.isFailure, flagReadFailed = read.isFailure) }
    }

    fun toggleFlag() {
        val current = _state.value
        val session = current.session ?: return
        val index = current.questionPage?.index ?: return
        if (current.isLoading || current.results != null || current.flagReadFailed) return
        val flags = if (index in current.flaggedIndices) current.flaggedIndices - index else current.flaggedIndices + index
        _state.update { it.copy(flaggedIndices = flags) }
        val saved = draftStore.writeFlags(userId, session.sessionId, flags)
        _state.update { it.copy(flagError = saved.isFailure) }
    }

    fun retryFlags() {
        val current = _state.value
        val session = current.session ?: return
        if (current.isLoading) return
        if (current.flagReadFailed) { loadFlags(session.sessionId, session.questionIds.size); return }
        val saved = draftStore.writeFlags(userId, session.sessionId, current.flaggedIndices)
        _state.update { it.copy(flagError = saved.isFailure) }
    }

    fun resumeSaved() {
        val saved = _state.value.resumable ?: return
        if (_state.value.isLoading || _state.value.recoveryRequired || _state.value.draftStorageFailed ||
            (_state.value.pendingDraft != null && _state.value.pendingDraft?.sessionId != saved.progress.sessionId)
        ) return
        setBusy()
        viewModelScope.launch {
            runCatching {
                val progress = if (saved.progress.isActive) {
                    saved.progress
                } else {
                    sessionService.resume(saved.mode, saved.progress.sessionId)
                }
                val session = progress.toSession(saved.mode)
                val question = if (session.currentIndex < session.questionIds.size) {
                    sessionService.question(saved.mode, session.sessionId)
                } else {
                    null
                }
                Triple(session, question, progress.answers)
            }.onSuccess { (session, question, answers) ->
                if (question == null) {
                    prepareCompletedSession(saved.mode, session)
                } else {
                    showSession(saved.mode, session, question, restoredAnswers = answers)
                }
            }.onFailure(::handleSessionFailure)
        }
    }

    fun discardSaved() {
        val saved = _state.value.resumable ?: return
        val id = saved.progress.sessionId ?: return
        if (_state.value.isLoading || _state.value.recoveryRequired || _state.value.draftStorageFailed) return
        setBusy()
        viewModelScope.launch {
            runCatching { sessionService.discard(saved.mode, id) }
                .onSuccess {
                    if (draftStore.read(userId).getOrNull()?.sessionId == id) forgetDraft()
                    persistSetup(sessionId = null)
                    _state.update { it.copy(resumable = null, isLoading = false) }
                }
                .onFailure(::handleSessionFailure)
        }
    }

    fun updatePreAnswer(value: String) {
        if (!_state.value.isLoading && !_state.value.recoveryRequired && value.length <= 1_000) {
            if (_state.value.pendingDraft != null || _state.value.draftStorageFailed || _state.value.choicesRevealed) return
            _state.update { it.copy(preAnswer = value) }
            if (value.isBlank()) forgetDraft() else rememberDraft(SessionDraftKind.RECALL_TEXT)
        }
    }

    fun revealRecallChoices() {
        val snapshot = _state.value
        val session = snapshot.session ?: return
        if (
            snapshot.mode != TestMode.RECALL || snapshot.preAnswer.isBlank() ||
            snapshot.choicesRevealed || snapshot.isLoading || snapshot.recoveryRequired || snapshot.pendingDraft != null || snapshot.draftStorageFailed
        ) return
        if (!rememberDraft(SessionDraftKind.REVEAL)) return
        setBusy()
        viewModelScope.launch {
            runCatching {
                sessionService.submit(
                    TestMode.RECALL,
                    SubmitAnswerRequest(
                        sessionId = session.sessionId,
                        action = "same",
                        expectedIndex = snapshot.questionPage?.index,
                        preAnswer = snapshot.preAnswer.trim(),
                    ),
                )
                sessionService.question(TestMode.RECALL, session.sessionId)
            }.onSuccess(::applyQuestion)
                .onFailure(::handleSessionFailure)
        }
    }

    /** Bookmark reads/writes are independent of answer storage and never replay an uncertain write. */
    fun refreshQuestionBookmark(questionId: Long, force: Boolean = false) {
        val snapshot = _state.value
        if (snapshot.questionPage?.question?.id != questionId || questionId in snapshot.bookmarkBusyIds ||
            (!force && questionId in snapshot.questionBookmarks)) return
        _state.update { it.copy(bookmarkBusyIds = it.bookmarkBusyIds + questionId,
            bookmarkErrors = it.bookmarkErrors - questionId) }
        viewModelScope.launch {
            try {
                val added = bookmarkService.state(questionId)
                kotlin.coroutines.coroutineContext.ensureActive()
                _state.update { it.copy(questionBookmarks = it.questionBookmarks + (questionId to added),
                    bookmarkUncertainIds = it.bookmarkUncertainIds - questionId) }
            } catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) {
                _state.update { it.copy(bookmarkErrors = it.bookmarkErrors + (questionId to failure.message.orEmpty())) }
            } finally {
                _state.update { it.copy(bookmarkBusyIds = it.bookmarkBusyIds - questionId) }
            }
        }
    }

    fun changeQuestionBookmark(questionId: Long) {
        val snapshot = _state.value
        val previous = snapshot.questionBookmarks[questionId] ?: return
        if (snapshot.questionPage?.question?.id != questionId || snapshot.recoveryRequired ||
            questionId in snapshot.bookmarkBusyIds || questionId in snapshot.bookmarkUncertainIds) return
        val desired = !previous
        _state.update { it.copy(bookmarkBusyIds = it.bookmarkBusyIds + questionId,
            bookmarkErrors = it.bookmarkErrors - questionId) }
        viewModelScope.launch {
            try {
                val added = bookmarkService.set(questionId, desired)
                kotlin.coroutines.coroutineContext.ensureActive()
                _state.update { it.copy(questionBookmarks = it.questionBookmarks + (questionId to added)) }
                // Retain confirmed success if the optional count read fails.
                try {
                    val generation = ++bookmarkCountGeneration
                    val count = bookmarkService.count()
                    kotlin.coroutines.coroutineContext.ensureActive()
                    if (generation == bookmarkCountGeneration) _state.update { it.copy(bookmarkCount = count) }
                } catch (cancel: CancellationException) { throw cancel }
                catch (_: Exception) { }
            } catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) {
                _state.update { it.copy(bookmarkUncertainIds = it.bookmarkUncertainIds + questionId,
                    bookmarkErrors = it.bookmarkErrors + (questionId to failure.message.orEmpty())) }
                // Read once to reconcile a lost response, without resending the write.
                try {
                    val actual = bookmarkService.state(questionId)
                    kotlin.coroutines.coroutineContext.ensureActive()
                    _state.update { it.copy(questionBookmarks = it.questionBookmarks + (questionId to actual),
                        bookmarkUncertainIds = it.bookmarkUncertainIds - questionId,
                        bookmarkErrors = if (actual == desired) it.bookmarkErrors - questionId else it.bookmarkErrors) }
                } catch (cancel: CancellationException) { throw cancel }
                catch (_: Exception) { }
            } finally {
                _state.update { it.copy(bookmarkBusyIds = it.bookmarkBusyIds - questionId) }
            }
        }
    }

    fun selectAnswer(answer: Int): Boolean {
        val snapshot = _state.value
        if (snapshot.mode != TestMode.RECALL && !snapshot.downloaded) return false
        if (snapshot.syncConflict != null && snapshot.syncConflict.index == snapshot.questionPage?.index) return false
        if (snapshot.mode == TestMode.EXAM && snapshot.session?.durationMinutes != null && _clock.value.seconds <= 0) return false
        if (snapshot.isLoading || snapshot.locallyFinished || snapshot.recoveryRequired || snapshot.questionPage == null || snapshot.pendingDraft != null || snapshot.draftStorageFailed) return false
        if (answer !in 1..snapshot.questionPage.question.choices.size || answer == snapshot.selectedAnswer) return false
        if (snapshot.mode == TestMode.RECALL && !snapshot.choicesRevealed) return false
        if (snapshot.mode.isLearningMode() && snapshot.showFeedback) return false
        _state.update {
            it.copy(
                selectedAnswer = answer,
                showFeedback = false,
                explanation = null,
                feedbackTranslations = JsonObject(emptyMap()),
                isCorrect = null,
                errorReason = null,
            )
        }
        saveCurrentAnswer()
        return true
    }

    fun selectConfidence(confidence: Int) {
        val snapshot = _state.value
        if (snapshot.syncConflict != null && snapshot.syncConflict.index == snapshot.questionPage?.index) return
        if (snapshot.mode == TestMode.EXAM && snapshot.session?.durationMinutes != null && _clock.value.seconds <= 0) return
        if (snapshot.selectedAnswer == null || snapshot.isLoading || snapshot.locallyFinished || snapshot.recoveryRequired || snapshot.pendingDraft != null || snapshot.draftStorageFailed || confidence !in 1..3) return
        _state.update { it.copy(confidence = confidence) }
        saveCurrentAnswer()
    }

    fun recordReflection(reason: String) {
        val snapshot = _state.value
        if (snapshot.selectedAnswer == null || snapshot.isLoading || snapshot.locallyFinished || snapshot.recoveryRequired ||
            !snapshot.mode.isLearningMode() || snapshot.isCorrect != false ||
            snapshot.errorReason != null || snapshot.pendingDraft != null || snapshot.draftStorageFailed || reason !in setOf("unknown", "misread", "confused", "guessed")
        ) return
        if (downloadedSession != null) {
            _state.update { it.copy(errorReason = reason) }
            stageDownloadedAnswer(reflection = true)
            return
        }
        if (!rememberDraft(SessionDraftKind.REFLECTION, reason)) return
        setBusy()
        viewModelScope.launch {
            runCatching { submitCurrent(action = "same", errorReason = reason) }
                .onSuccess { feedback ->
                    _state.update {
                        it.copy(
                            autoAdvanceToken = it.autoAdvanceToken + 1,
                            confirmedSaveToken = it.confirmedSaveToken + 1,
                            errorReason = reason,
                            questionPage = it.questionPage?.copy(savedErrorReason = reason),
                            explanation = feedback.explanation ?: it.explanation,
                            feedbackTranslations = feedback.feedbackTranslations.takeIf {
                                translations -> translations.isNotEmpty()
                            } ?: it.feedbackTranslations,
                            isCorrect = feedback.isCorrect ?: it.isCorrect,
                            isLoading = false,
                        )
                    }
                    forgetDraft()
                }.onFailure(::handleSessionFailure)
        }
    }

    val soundEnabled: Boolean get() = preferences.display.value.sound
    val hapticsEnabled: Boolean get() = preferences.display.value.haptics

    fun autoAdvance(index: Int, answer: Int) {
        val s = _state.value
        val page = s.questionPage ?: return
        if (!preferences.display.value.autoAdvance || s.results != null || s.session == null ||
            page.index != index || s.selectedAnswer != answer || page.savedAnswer != answer ||
            page.index + 1 >= page.total || !canNavigate(s)) return
        next()
    }

    fun next() {
        val snapshot = _state.value
        val page = snapshot.questionPage ?: return
        if (!canNavigate(snapshot)) return
        if (page.index + 1 >= page.total) {
            finish()
        } else {
            navigate("next")
        }
    }

    fun previous() {
        val snapshot = _state.value
        val page = snapshot.questionPage ?: return
        if (page.index > 0 && canNavigate(snapshot)) navigate("previous")
    }

    fun goTo(index: Int) {
        val snapshot = _state.value
        val page = snapshot.questionPage ?: return
        if (index == page.index || index !in 0 until page.total || !canNavigate(snapshot)) return
        navigate("goto", targetIndex = index)
    }

    fun finishStudyLocally() {
        val snapshot = _state.value
        val cached = downloadedSession ?: return
        if (snapshot.mode != TestMode.STUDY || snapshot.isLoading || snapshot.locallyFinished ||
            snapshot.recoveryRequired || snapshot.pendingDraft != null || snapshot.draftStorageFailed || snapshot.finishPending) return
        if (retainDownloaded(cached.copy(locallyFinished = true, finishedTimerSeconds = _clock.value.seconds,
                finishedTimerOvertime = _clock.value.overtime))) {
            timerJob?.cancel()
            _state.update { it.copy(syncRequired = true) }
        }
    }

    fun finish(forced: Boolean = false) {
        val snapshot = _state.value
        val session = snapshot.session ?: return
        if (snapshot.isLoading || snapshot.syncing || snapshot.recoveryRequired || snapshot.finishStorageFailed || snapshot.results != null || snapshot.draftStorageFailed || (!forced && snapshot.pendingDraft != null)) return
        if (downloadedSession?.answers?.isNotEmpty() == true) {
            _state.update { it.copy(syncRequired = true) }
            return
        }
        timerJob?.cancel()
        setBusy()
        viewModelScope.launch {
            runCatching {
                if (downloadedSession == null && !forced && snapshot.selectedAnswer != null) submitCurrent(action = "same", useDefaultReflection = true)
                val marker=PendingSessionFinish(session.sessionId,snapshot.mode.path)
                pendingFinishWrite=marker
                _state.update { it.copy(finishPending=true,finishRecovery=marker,finishStatusChecked=false,finishedSessionMissing=false) }
                persistSetup(sessionId=session.sessionId)
                val stored=draftStore.writeFinish(userId,marker)
                _state.update { it.copy(finishStorageFailed=stored.isFailure) }
                stored.getOrThrow()
                pendingFinishWrite=null
                sessionService.finish(snapshot.mode, session.sessionId)
            }.onSuccess { results ->
                val cacheCleared = retainDownloaded(null)
                forgetDraft()
                val cleared = cacheCleared && clearFinishRecord()
                _state.update { it.copy(finishPending=false,finishRecovery=it.finishRecovery.takeUnless { cleared },
                    finishStatusChecked=false,finishedSessionMissing=false,
                    finishStorageFailed = !cleared, recoveryRequired = !cleared) }
                persistSetup(sessionId = null)
                _state.update {
                    it.copy(
                        results = results,
                        questionPage = null,
                        session = null,
                        resumable = null,
                        isLoading = false,
                    )
                }
            }.onFailure(::handleSessionFailure)
        }
    }

    private fun clearFinishRecord(): Boolean {
        val result=draftStore.clearFinish(userId)
        pendingFinishClear=result.isFailure
        if(result.isSuccess) pendingFinishWrite=null
        _state.update { it.copy(finishStorageFailed=result.isFailure,recoveryRequired=it.recoveryRequired || result.isFailure) }
        return result.isSuccess
    }

    fun retryFinishStorage() {
        if(_state.value.isLoading || draftStore.retryBinding(userId).isFailure) return
        if (_state.value.results != null && downloadedSession != null) {
            if (!retainDownloaded(null)) return
            if (!clearFinishRecord()) return
            _state.update { it.copy(finishStorageFailed = false, recoveryRequired = false, draftStorageFailed = false) }
            return
        }
        if(pendingFinishClear) {
            if(!clearFinishRecord()) return
            if(_state.value.results!=null) {
                _state.update { it.copy(recoveryRequired=false) }
                return // Keep a confirmed result visible; never Finish again.
            }
        }
        pendingFinishWrite?.let { pending ->
            if(draftStore.writeFinish(userId,pending).isFailure) return
            pendingFinishWrite=null
        }
        _state.update { it.copy(finishStorageFailed=false) }
        initialize()
    }

    fun discardUnreadableFinishRecovery() {
        val s=_state.value
        if(s.isLoading || !s.finishStorageFailed || s.finishRecovery!=null || !clearFinishRecord()) return
        if(s.results!=null) {
            _state.update { it.copy(recoveryRequired=false) }
            return // Removing unreadable local data must not erase confirmed results.
        }
        _state.update { it.copy(finishPending=true,recoveryRequired=true,finishStatusChecked=false,finishedSessionMissing=false) }
        persistSetup(sessionId=s.session?.sessionId ?: s.resumable?.progress?.sessionId
            ?: savedStateHandle.get<Bundle>("quiz_setup")?.getString("session_id"))
        initialize() // Still requires a successful read and explicit review before further writes.
    }

    fun acknowledgeFinishRecovery() {
        val s=_state.value
        if(s.isLoading || s.finishStorageFailed || !s.finishPending || !s.finishStatusChecked || !clearFinishRecord()) return
        _state.update { it.copy(finishRecovery=null,finishPending=false,finishStatusChecked=false,finishedSessionMissing=false,
            recoveryRequired=false,error=null,hasUnknownError=false) }
        persistSetup(sessionId=if(s.finishedSessionMissing) null else s.session?.sessionId ?: s.resumable?.progress?.sessionId)
        if(s.finishedSessionMissing) queueAvailableCount()
    }

    fun refreshCompletedReview() {
        val snapshot = _state.value
        val result = snapshot.results ?: return
        val identity = result.sourceSessionId ?: return
        if (snapshot.reviewLoading || snapshot.isLoading || !result.reviewWithheld) return
        val mode = TestMode.values().firstOrNull { it.path == result.mode } ?: return
        _state.update { it.copy(reviewLoading = true, reviewFailed = false, reviewError = null) }
        viewModelScope.launch {
            fun owns(): Boolean = _state.value.results?.sourceSessionId == identity &&
                _state.value.results?.historyId == result.historyId && _state.value.mode == snapshot.mode
            try {
                val fresh = sessionService.completedResult(mode, identity)
                check(fresh.historyId == result.historyId)
                if (owns()) _state.update { it.copy(results = fresh) }
            } catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) {
                if (owns()) _state.update { it.copy(reviewFailed = true, reviewError = failure.message) }
            } finally {
                if (owns()) _state.update { it.copy(reviewLoading = false) }
            }
        }
    }

    fun returnToSetup() {
        if (_state.value.syncing || _state.value.locallyFinished) return
        if (downloadedSession?.answers?.isNotEmpty() == true) return
        if(_state.value.finishPending || _state.value.finishStorageFailed || _state.value.recoveryRequired) return
        timerJob?.cancel()
        persistSetup(sessionId = null)
        _state.update {
            it.copy(
                session = null,
                answeredIndices = emptySet(),
                questionPage = null,
                results = null,
                reviewLoading = false,
                reviewFailed = false,
                reviewError = null,
                selectedAnswer = null,
                confidence = 3,
                preAnswer = "",
                explanation = null,
                feedbackTranslations = JsonObject(emptyMap()),
                isCorrect = null,
                errorReason = null,
                showFeedback = false,
                isLoading = false,
                error = null,
                hasUnknownError = false,
            )
        }
        queueAvailableCount()
        refreshMetadata()
    }

    fun pauseAndExit(onComplete: () -> Unit) {
        val snapshot = _state.value
        if (snapshot.isLoading || snapshot.syncing) return
        val session = snapshot.session
        if (session == null || snapshot.results != null) {
            onComplete()
            return
        }
        if (snapshot.recoveryRequired || snapshot.draftStorageFailed || snapshot.pendingDraft != null ||
            snapshot.finishPending || snapshot.finishStorageFailed || snapshot.syncConflict != null) return
        if (snapshot.locallyFinished && snapshot.mode == TestMode.STUDY) { onComplete(); return }
        if (downloadedSession?.answers?.isNotEmpty() == true) {
            syncDownloadedAnswersThen { pauseAndExit(onComplete) }
            return
        }
        setBusy()
        viewModelScope.launch {
            runCatching {
                if (downloadedSession != null) {
                    val progress = sessionService.status(snapshot.mode, session.sessionId)
                    check(progress.sessionId == session.sessionId && progress.isActive)
                    val target = snapshot.questionPage?.index ?: session.currentIndex
                    if (progress.currentIndex != target) sessionService.submit(snapshot.mode, SubmitAnswerRequest(
                        sessionId = session.sessionId, action = "goto", expectedIndex = progress.currentIndex,
                        targetIndex = target))
                }
                if (downloadedSession == null && snapshot.mode.isLearningMode() && snapshot.isCorrect == false && snapshot.errorReason == null) {
                    submitCurrent(action = "same", useDefaultReflection = true)
                }
                sessionService.pause(snapshot.mode, session.sessionId)
            }
                .onSuccess {
                    if (!retainDownloaded(null)) return@onSuccess
                    timerJob?.cancel()
                    // Re-enter through status discovery; never reuse a paused local timer.
                    _state.update { it.copy(session = null, questionPage = null,
                        setupLoaded = false, isLoading = false) }
                    onComplete()
                }.onFailure(::handleSessionFailure)
        }
    }

    fun refreshAvailableCount() {
        val snapshot = _state.value
        if (snapshot.isLoading || snapshot.isCountLoading || snapshot.session != null ||
            snapshot.recoveryRequired || !snapshot.setupLoaded) return
        queueAvailableCount()
    }

    private fun queueAvailableCount() { setup.queueAvailableCount() }

    private fun selectionFrom(state: QuizUiState): SetupSelection? = setup.selectionFrom(state)

    private fun downloadedAnsweredIndices(value: DownloadedSession): Set<Int> =
        (value.pages.filter { it.savedAnswer != null }.map { it.index } + value.answers.map { it.index }).toSet()

    private fun retainDownloaded(value: DownloadedSession?): Boolean {
        val written = draftStore.writeDownloadedSession(userId, value)
        if (written.isFailure) {
            _state.update { it.copy(draftStorageFailed = true, isLoading = false) }
            return false
        }
        downloadedSession = value
        _state.update { it.copy(downloaded = value != null, queuedAnswers = value?.answers?.size ?: 0,
            locallyFinished = value?.locallyFinished == true,
            answeredIndices = value?.let(::downloadedAnsweredIndices) ?: it.answeredIndices) }
        return true
    }

    fun downloadSession(timerDisabled: Boolean = downloadedSession?.timerDisabled ?: downloadedTimerDisabled) {
        val snapshot = _state.value
        val session = snapshot.session ?: return
        if (snapshot.mode == TestMode.RECALL || downloadedSession != null || snapshot.draftStorageFailed || downloadJob?.isActive == true) return
        setBusy()
        downloadJob = viewModelScope.launch {
            try {
                val bundle = sessionService.questions(snapshot.mode, session.sessionId)
                check(bundle.sessionId == session.sessionId && bundle.questions.size == session.questionIds.size)
                check(bundle.questions.withIndex().all { (index, page) ->
                    page.index == index && page.question.id == session.questionIds[index]
                })
                val images = mutableMapOf<String, String>()
                var imageBytes = 0L
                for (url in bundle.questions.mapNotNull { it.question.imageUrl?.takeIf(String::isNotBlank) }.distinct()) {
                    val bytes = imageRepository.load(url)
                    imageBytes += bytes.size
                    check(imageBytes <= 10L * 1024 * 1024) { "Session images exceed local storage capacity." }
                    images[url] = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                }
                if (!retainDownloaded(DownloadedSession(session, bundle.questions,
                        timerDisabled = timerDisabled, images = images))) return@launch
                applyDownloadedPage(session.currentIndex)
            } catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) { fail(failure) }
        }
    }

    fun retryDownloadedStorage() {
        if (_state.value.isLoading || draftStore.retryBinding(userId).isFailure) return
        val cached = downloadedSession
        if (cached != null && retainDownloaded(cached)) {
            _state.update { it.copy(draftStorageFailed = false) }
            stageDownloadedAnswer()
        } else if (cached == null) {
            _state.update { it.copy(draftStorageFailed = false) }
            downloadSession()
        }
    }

    fun downloadedImage(url: String?): String? = downloadedSession?.images?.get(url)

    private fun applyDownloadedPage(index: Int) {
        val cached = downloadedSession ?: return
        var page = cached.pages.getOrNull(index) ?: return
        val correctAnswer = page.question.correctAnswer?.takeIf { it in 1..page.question.choices.size }
        cached.answers.filter { it.index == index }.forEach { draft ->
            page = page.copy(savedAnswer = draft.answer, savedConfidence = draft.confidence,
                savedIsCorrect = correctAnswer?.let { it == draft.answer }.takeIf { _state.value.mode == TestMode.STUDY },
                savedErrorReason = draft.errorReason)
        }
        applyQuestion(page)
        if (cached.answers.any { it.index == index } && correctAnswer == null) {
            _state.update { it.copy(showFeedback = false) }
        }
    }

    private fun stageDownloadedAnswer(reflection: Boolean = false) {
        val snapshot = _state.value
        if (snapshot.mode == TestMode.EXAM && snapshot.session?.durationMinutes != null && _clock.value.seconds <= 0) return
        val cached = downloadedSession ?: return
        val page = snapshot.questionPage ?: return
        val answer = snapshot.selectedAnswer ?: return
        if (snapshot.isLoading || snapshot.draftStorageFailed) return
        val draft = SessionDraft(cached.session.sessionId, snapshot.mode.path, page.question.id,
            page.index, if (reflection) SessionDraftKind.REFLECTION else SessionDraftKind.ANSWER,
            answer, snapshot.confidence ?: 3, errorReason = snapshot.errorReason,
            baseAnswer = page.savedAnswer, baseConfidence = page.savedConfidence,
            basePreAnswer = page.savedPreAnswer, baseErrorReason = page.savedErrorReason)
        if (draft.acknowledged(page)) return
        val wasPending = cached.answers.isNotEmpty() || snapshot.syncRequired
        // Keep ordered revisions so first-answer evidence survives later edits.
        val next = cached.copy(answers = cached.answers + draft)
        if (!retainDownloaded(next)) return
        applyDownloadedPage(page.index)
        _state.update { it.copy(syncRequired = true) }
        val hasLocalFeedback = snapshot.mode == TestMode.EXAM ||
            page.question.correctAnswer?.let { it in 1..page.question.choices.size } == true
        if (hasLocalFeedback) {
            _state.update { it.copy(autoAdvanceToken = it.autoAdvanceToken + 1) }
        } else if (!wasPending) {
            // Older downloaded study sessions need server feedback until downloaded again.
            syncDownloadedAnswers()
        }
    }

    /** Every explicit retry reads confirmed slots first; uncertain writes are never blindly replayed. */
    fun syncDownloadedAnswers() = syncDownloadedAnswersThen(null)

    private fun syncDownloadedAnswersThen(onSynced: (() -> Unit)?) {
        if (downloadedSession == null) return
        val snapshot = _state.value
        if (snapshot.isLoading || snapshot.syncing || snapshot.draftStorageFailed || snapshot.finishStorageFailed || snapshot.recoveryRequired || snapshot.finishPending || snapshot.syncConflict != null) return
        _state.update { it.copy(syncing = true, syncError = null, error = null, hasUnknownError = false) }
        viewModelScope.launch {
            var finalizeStudy = false
            try {
                val initial = downloadedSession ?: return@launch
                val bundle = sessionService.questions(snapshot.mode, initial.session.sessionId)
                check(bundle.sessionId == initial.session.sessionId && bundle.questions.size == initial.pages.size)
                check(bundle.questions.withIndex().all { (index, page) ->
                    page.index == index && page.question.id == initial.session.questionIds[index]
                })
                var confirmedPages = bundle.questions
                var remainingBatch = initial.answers.size
                while (remainingBatch > 0) {
                    val current = downloadedSession ?: break
                    val draft = current.answers.firstOrNull() ?: break
                    check(current.session.sessionId == initial.session.sessionId)
                    var page = confirmedPages[draft.index]
                    check(draft.matches(page))
                    if (!draft.acknowledged(page)) {
                        val progress = sessionService.status(snapshot.mode, current.session.sessionId)
                        check(progress.sessionId == current.session.sessionId && progress.isActive)
                        if (progress.currentIndex != draft.index) {
                            sessionService.submit(snapshot.mode, SubmitAnswerRequest(
                                sessionId = current.session.sessionId, action = "goto",
                                expectedIndex = progress.currentIndex, targetIndex = draft.index))
                        }
                        val presented = sessionService.question(snapshot.mode, current.session.sessionId)
                        check(draft.matches(presented))
                        if (!draft.acknowledged(presented)) {
                            if (!draft.baselineUnchanged(presented)) {
                                _state.update { it.copy(syncConflict = presented,
                                    syncConflictAnswer = downloadedSession?.answers?.lastOrNull { it.index == draft.index }?.answer,
                                    syncRequired = true, isLoading = false) }
                                return@launch
                            }
                            sessionService.submit(snapshot.mode, SubmitAnswerRequest(
                                sessionId = current.session.sessionId, action = "same", expectedIndex = draft.index,
                                expectedSlot = SessionAnswerBaseline(draft.baseAnswer, draft.baseConfidence,
                                    draft.basePreAnswer, draft.baseErrorReason), answer = draft.answer,
                                confidence = draft.confidence, errorReason = draft.errorReason))
                            // A fresh owned question read confirms the write without
                            // downloading every frozen page again. A moved index or
                            // lost response keeps the queued change for explicit retry.
                            page = sessionService.question(snapshot.mode, current.session.sessionId)
                        } else page = presented
                    } else {
                        // Cached acknowledgement can become stale while earlier
                        // changes sync. Re-read without moving an expired or
                        // already-finished session just to confirm saved data.
                        val latest = try { sessionService.question(snapshot.mode, current.session.sessionId) }
                            catch (cancel: CancellationException) { throw cancel }
                            catch (failure: ApiException) { if (failure.statusCode == 404) null else throw failure }
                        if (latest != null && draft.matches(latest)) page = latest else {
                            val refreshed = sessionService.questions(snapshot.mode, current.session.sessionId)
                            check(refreshed.sessionId == current.session.sessionId && refreshed.questions.size == current.pages.size)
                            check(refreshed.questions.withIndex().all { (index, saved) ->
                                saved.index == index && saved.question.id == current.session.questionIds[index]
                            })
                            confirmedPages = refreshed.questions
                            page = confirmedPages[draft.index]
                        }
                        if (!draft.acknowledged(page)) {
                            _state.update { it.copy(syncConflict = page,
                                syncConflictAnswer = downloadedSession?.answers?.lastOrNull { it.index == draft.index }?.answer,
                                syncRequired = true, isLoading = false) }
                            return@launch
                        }
                    }
                    check(draft.matches(page) && draft.acknowledged(page))
                    kotlin.coroutines.coroutineContext.ensureActive()
                    confirmedPages = confirmedPages.toMutableList().also { it[draft.index] = page }
                    // Local answers/navigation may have changed during the network awaits.
                    val latest = requireNotNull(downloadedSession)
                    check(latest.session.sessionId == initial.session.sessionId && latest.answers.firstOrNull() == draft)
                    val remaining = latest.answers.drop(1).toMutableList()
                    val following = remaining.firstOrNull()
                    if (following?.index == draft.index && following.baseAnswer == draft.answer &&
                            following.baseConfidence == draft.confidence) {
                        val saved = page
                        remaining[0] = following.copy(baseErrorReason = saved.savedErrorReason,
                            basePreAnswer = saved.savedPreAnswer)
                    }
                    if (!retainDownloaded(latest.copy(pages = confirmedPages,
                            answers = remaining))) return@launch
                    remainingBatch -= 1
                }
                val latest = requireNotNull(downloadedSession)
                if (latest.pages != confirmedPages && !retainDownloaded(latest.copy(pages = confirmedPages))) return@launch
                _state.update { it.copy(syncRequired = downloadedSession?.let { saved -> saved.answers.isNotEmpty() || saved.locallyFinished } == true,
                    error = null, hasUnknownError = false) }
                finalizeStudy = initial.locallyFinished && downloadedSession?.answers?.isEmpty() == true
                if (snapshot.queuedAnswers > 0) _state.update { it.copy(confirmedSaveToken = it.confirmedSaveToken + 1) }
                val index = downloadedSession?.session?.currentIndex ?: return@launch
                applyDownloadedPage(index)
                if (snapshot.mode.isLearningMode() && snapshot.questionPage?.question?.correctAnswer == null &&
                        snapshot.queuedAnswers > 0 && snapshot.selectedAnswer != null &&
                        downloadedSession?.pages?.getOrNull(index)?.savedAnswer == snapshot.selectedAnswer) {
                    _state.update { it.copy(autoAdvanceToken = it.autoAdvanceToken + 1) }
                }
            } catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) {
                _state.update { it.copy(syncRequired = true, syncError = failure) }
                fail(failure) // Keep the running timer, downloaded pages and remaining answers.
            } finally {
                _state.update { it.copy(syncing = false) }
            }
            if (finalizeStudy && _state.value.mode == TestMode.STUDY) finish()
            else if (onSynced != null && downloadedSession?.session?.sessionId == snapshot.session?.sessionId &&
                downloadedSession?.answers?.isEmpty() == true && _state.value.syncError == null &&
                !_state.value.recoveryRequired && !_state.value.draftStorageFailed && !_state.value.syncRequired) {
                onSynced()
            }
        }
    }

    /** Explicit per-question conflict choice; the next sync still uses a locked slot precondition. */
    fun resolveDownloadedConflict(keepDevice: Boolean) {
        val snapshot = _state.value
        val server = snapshot.syncConflict ?: return
        val cached = downloadedSession ?: return
        if (snapshot.isLoading || snapshot.syncing || snapshot.draftStorageFailed) return
        val pending = cached.answers.filter { it.index == server.index }
        val rest = cached.answers.filter { it.index != server.index }
        val replacements = if (keepDevice && pending.isNotEmpty()) listOf(pending.first().copy(
            baseAnswer = server.savedAnswer, baseConfidence = server.savedConfidence,
            basePreAnswer = server.savedPreAnswer, baseErrorReason = server.savedErrorReason
        )) + pending.drop(1) else emptyList()
        val pages = cached.pages.toMutableList().also { it[server.index] = server }
        if (!retainDownloaded(cached.copy(pages = pages, answers = replacements + rest))) return
        _state.update { it.copy(syncConflict = null, syncConflictAnswer = null,
            syncRequired = true, error = null, hasUnknownError = false) }
        applyDownloadedPage(cached.session.currentIndex)
    }

    private fun saveCurrentAnswer() {
        if (downloadedSession != null) {
            stageDownloadedAnswer()
            return
        }
        if (_state.value.isLoading || _state.value.recoveryRequired || _state.value.pendingDraft != null || _state.value.draftStorageFailed) return
        if (!rememberDraft(SessionDraftKind.ANSWER)) return
        setBusy()
        viewModelScope.launch {
            runCatching { submitCurrent(action = "same") }
                .onSuccess(::applyFeedback)
                .onFailure(::handleSessionFailure)
        }
    }

    private fun navigate(action: String, targetIndex: Int? = null) {
        val snapshot = _state.value
        val session = snapshot.session ?: return
        if (!canNavigate(snapshot)) return
        downloadedSession?.let { cached ->
            val index = snapshot.questionPage?.index ?: return
            val target = when (action) {
                "previous" -> index - 1
                "goto" -> targetIndex ?: return
                else -> index + 1
            }
            if (target !in cached.pages.indices) return
            val next = cached.copy(session = cached.session.copy(currentIndex = target))
            if (retainDownloaded(next)) {
                _state.update { it.copy(session = next.session) }
                applyDownloadedPage(target)
            }
            return
        }
        setBusy()
        viewModelScope.launch {
            runCatching {
                sessionService.submit(
                    snapshot.mode,
                    SubmitAnswerRequest(
                        sessionId = session.sessionId,
                        answer = snapshot.selectedAnswer.takeIf { action != "previous" || snapshot.mode.isLearningMode() },
                        action = action,
                        expectedIndex = snapshot.questionPage?.index,
                        targetIndex = targetIndex,
                        confidence = snapshot.confidence.takeIf { action != "previous" || snapshot.mode.isLearningMode() },
                        errorReason = (snapshot.errorReason ?: "unknown").takeIf { snapshot.mode.isLearningMode() && snapshot.isCorrect == false },
                        preAnswer = snapshot.preAnswer.takeIf {
                            snapshot.mode == TestMode.RECALL && it.isNotBlank()
                        },
                    ),
                )
                sessionService.question(snapshot.mode, session.sessionId)
            }.onSuccess(::applyQuestion)
                .onFailure(::handleSessionFailure)
        }
    }

    private suspend fun submitCurrent(
        action: String,
        errorReason: String? = _state.value.errorReason,
        useDefaultReflection: Boolean = false,
    ): AnswerSubmissionDto {
        val snapshot = _state.value
        val session = requireNotNull(snapshot.session)
        return sessionService.submit(
            snapshot.mode,
            SubmitAnswerRequest(
                sessionId = session.sessionId,
                answer = snapshot.selectedAnswer,
                action = action,
                expectedIndex = snapshot.questionPage?.index,
                confidence = snapshot.confidence,
                errorReason = errorReason ?: "unknown".takeIf { useDefaultReflection && snapshot.mode.isLearningMode() && snapshot.isCorrect == false },
                preAnswer = snapshot.preAnswer.takeIf {
                    snapshot.mode == TestMode.RECALL && it.isNotBlank()
                },
            ),
        )
    }

    private fun restoreActive(saved: SavedSession) {
        setBusy()
        viewModelScope.launch {
            runCatching {
                val session = saved.progress.toSession(saved.mode)
                val question = if (session.currentIndex < session.questionIds.size) {
                    sessionService.question(saved.mode, session.sessionId)
                } else {
                    null
                }
                session to question
            }.onSuccess { (session, question) ->
                if (question == null) {
                    prepareCompletedSession(saved.mode, session)
                } else {
                    showSession(saved.mode, session, question, restoredAnswers = saved.progress.answers)
                }
            }.onFailure(::handleSessionFailure)
        }
    }

    private fun prepareCompletedSession(mode: TestMode, session: ExamSessionDto) {
        _state.update {
            it.copy(
                mode = mode,
                session = session,
                resumable = null,
                questionPage = null,
                isLoading = false,
                error = null,
                hasUnknownError = false,
            )
        }
        finish(forced = true)
    }

    private fun showSession(
        mode: TestMode,
        session: ExamSessionDto,
        question: SessionQuestionPageDto,
        timerDisabled: Boolean = false,
        restoredAnswers: JsonObject = JsonObject(emptyMap()),
    ) {
        loadFlags(session.sessionId, session.questionIds.size)
        _state.update {
            it.copy(
                mode = mode,
                launchPreset = null,
                session = session,
                answeredIndices = restoredAnswers.entries.mapNotNull { (index, slot) ->
                    val value = if (slot is JsonObject) slot["answer"] else slot
                    index.toIntOrNull()?.takeIf { position ->
                        position in session.questionIds.indices &&
                            (value as? JsonPrimitive)?.contentOrNull?.toIntOrNull()?.let { it > 0 } == true
                    }
                }.toSet(),
                questionPage = question,
                isLoading = true,
                selectedAnswer = question.savedAnswer,
                confidence = question.savedConfidence ?: 3,
                preAnswer = question.savedPreAnswer.orEmpty(),
                choicesRevealed = !question.question.choicesHidden,
                showFeedback = false,
                explanation = null,
                feedbackTranslations = JsonObject(emptyMap()),
                isCorrect = null,
                errorReason = question.savedErrorReason,
                syncing = false,
                syncError = null,
                resumable = null,
                results = null,
                error = null,
                hasUnknownError = false,
            )
        }
        persistSetup(sessionId = session.sessionId)
        startTimer(if (timerDisabled) session.copy(durationMinutes = null) else session)
        if (mode == TestMode.RECALL) {
            applyQuestion(question)
            return
        }
        downloadedTimerDisabled = timerDisabled
        downloadSession(timerDisabled)
    }

    private fun applyQuestion(page: SessionQuestionPageDto) {
        val learningMode = _state.value.mode.isLearningMode()
        val hasSavedLearningAnswer = learningMode && page.savedAnswer != null &&
            !page.question.choicesHidden
        _state.update {
            it.copy(
                questionPage = page,
                answeredIndices = if (page.savedAnswer != null) it.answeredIndices + page.index
                    else it.answeredIndices - page.index,
                selectedAnswer = page.savedAnswer,
                confidence = page.savedConfidence ?: 3,
                preAnswer = page.savedPreAnswer.orEmpty(),
                choicesRevealed = !page.question.choicesHidden,
                explanation = page.question.explanation.takeIf { hasSavedLearningAnswer },
                feedbackTranslations = JsonObject(emptyMap()),
                isCorrect = page.savedIsCorrect.takeIf { hasSavedLearningAnswer },
                errorReason = page.savedErrorReason.takeIf {
                    hasSavedLearningAnswer && page.savedIsCorrect == false &&
                        it in setOf("unknown", "misread", "confused", "guessed")
                },
                showFeedback = hasSavedLearningAnswer,
                isLoading = false,
                error = null,
                hasUnknownError = false,
            )
        }
        reconcileDraft(page)
    }

    private fun applyFeedback(feedback: AnswerSubmissionDto) {
        val learningMode = _state.value.mode.isLearningMode()
        _state.update {
            it.copy(
                autoAdvanceToken = it.autoAdvanceToken + 1,
                confirmedSaveToken = it.confirmedSaveToken + 1,
                answeredIndices = if (it.selectedAnswer != null && it.questionPage != null)
                    it.answeredIndices + it.questionPage.index else it.answeredIndices,
                questionPage = it.questionPage?.copy(savedAnswer = it.selectedAnswer,
                    savedConfidence = it.confidence, savedPreAnswer = it.preAnswer.takeIf(String::isNotBlank),
                    savedErrorReason = it.errorReason, savedIsCorrect = feedback.isCorrect),
                explanation = feedback.explanation ?: it.explanation,
                feedbackTranslations = feedback.feedbackTranslations,
                isCorrect = feedback.isCorrect,
                showFeedback = learningMode && feedback.isCorrect != null,
                isLoading = false,
                error = null,
                hasUnknownError = false,
            )
        }
        forgetDraft()
    }

    private fun startTimer(session: ExamSessionDto) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                val runningSeconds = session.startedAt?.let { startedAt ->
                    runCatching {
                        Duration.between(
                            OffsetDateTime.parse(startedAt).toInstant(),
                            OffsetDateTime.now().toInstant(),
                        ).seconds.coerceAtLeast(0)
                    }.getOrDefault(0)
                } ?: 0
                val elapsed = (session.accumulatedTime.toLong() + runningSeconds)
                    .coerceAtMost(Int.MAX_VALUE.toLong())
                    .toInt()
                val total = session.durationMinutes?.times(60)
                val remaining = total?.minus(elapsed)
                val overtime = session.mode != TestMode.EXAM.path && remaining != null && remaining < 0
                _clock.value = QuizClock(
                    seconds = when {
                        remaining == null -> elapsed
                        overtime -> -remaining
                        else -> remaining.coerceAtLeast(0)
                    },
                    overtime = overtime,
                )
                if (
                    session.mode == TestMode.EXAM.path && remaining != null && remaining <= 0 &&
                    !_state.value.isLoading
                ) {
                    finish(forced = true)
                    break
                }
                delay(1_000)
            }
        }
    }

    private fun canNavigate(snapshot: QuizUiState): Boolean {
        if (snapshot.mode != TestMode.RECALL && !snapshot.downloaded) return false
        if (snapshot.isLoading || snapshot.recoveryRequired || snapshot.pendingDraft != null || snapshot.draftStorageFailed) return false
        if (snapshot.mode == TestMode.RECALL && (!snapshot.choicesRevealed || snapshot.selectedAnswer == null)) return false
        if (snapshot.mode.isLearningMode()) {
            if (snapshot.selectedAnswer != null && snapshot.confidence == null) return false
        }
        return true
    }

    // Only lightweight navigation/setup values enter Android's saved-state Bundle.
    // Answers, recall text, question snapshots and results remain outside it.
    private fun updateSetup(transform: (QuizUiState) -> QuizUiState) { setup.updateSetup(transform) }

    private fun persistSetup(
        sessionId: String? = savedStateHandle.get<Bundle>("quiz_setup")?.getString("session_id"),
    ) { setupState.persist(_state.value, sessionId) }

    private fun rememberDraft(kind: SessionDraftKind, reason: String? = _state.value.errorReason): Boolean {
        val current = _state.value
        val session = current.session ?: return false
        val page = current.questionPage ?: return false
        val draft = SessionDraft(session.sessionId, current.mode.path, page.question.id, page.index, kind,
            current.selectedAnswer, current.confidence, current.preAnswer, reason,
            page.savedAnswer, page.savedConfidence, page.savedPreAnswer, page.savedErrorReason)
        val result = draftStore.write(userId, draft)
        _state.update { it.copy(draftStorageFailed = result.isFailure,
            pendingDraft = draft.takeIf { kind != SessionDraftKind.RECALL_TEXT || result.isFailure }, draftCanRetry = false) }
        return result.isSuccess
    }

    private fun forgetDraft(): Boolean {
        val result = draftStore.clearDraft(userId)
        _state.update { it.copy(draftStorageFailed = result.isFailure,
            pendingDraft = if (result.isSuccess) null else it.pendingDraft, draftCanRetry = false) }
        return result.isSuccess
    }

    private fun reconcileDraft(page: SessionQuestionPageDto) {
        if (_state.value.draftStorageFailed) {
            _state.update { it.copy(draftCanRetry = false) }
            return
        }
        val result = draftStore.read(userId)
        if (result.isFailure) {
            _state.update { it.copy(draftStorageFailed = true, draftCanRetry = false) }
            return
        }
        val draft = result.getOrNull()
        _state.update { it.copy(pendingDraft = draft, draftCanRetry = false) }
        if (draft == null || draft.sessionId != _state.value.session?.sessionId ||
            draft.mode != _state.value.mode.path || !draft.matches(page)
        ) return
        if (draft.acknowledged(page)) {
            forgetDraft()
        } else if (draft.kind == SessionDraftKind.RECALL_TEXT && page.question.choicesHidden && draft.baselineUnchanged(page)) {
            _state.update { it.copy(preAnswer = draft.preAnswer, pendingDraft = null) }
        } else {
            _state.update { it.copy(draftCanRetry = draft.baselineUnchanged(page) &&
                (draft.kind == SessionDraftKind.REVEAL || draft.kind == SessionDraftKind.RECALL_TEXT || !page.question.choicesHidden)) }
        }
    }

    fun retryDraftStorage() {
        if (_state.value.isLoading) return
        if (draftStore.retryBinding(userId).isFailure) return
        val local = _state.value.pendingDraft
        if (local != null && draftStore.write(userId, local).isFailure) return
        _state.update { it.copy(draftStorageFailed = false) }
        initialize()
    }

    fun discardLocalDraft() {
        if (_state.value.isLoading || !forgetDraft()) return
        initialize()
    }

    fun retryPendingDraft() {
        val snapshot = _state.value
        val draft = snapshot.pendingDraft ?: return
        val session = snapshot.session ?: return
        if (snapshot.isLoading || snapshot.recoveryRequired || snapshot.draftStorageFailed || !snapshot.draftCanRetry ||
            session.sessionId != draft.sessionId || !draft.matches(snapshot.questionPage ?: return)
        ) return
        setBusy()
        viewModelScope.launch {
            runCatching {
                // Re-read immediately before retry; no answer/navigation/finish replay.
                val page = sessionService.question(snapshot.mode, session.sessionId)
                if (!draft.matches(page) || !draft.baselineUnchanged(page)) {
                    applyQuestion(page)
                } else {
                    sessionService.submit(snapshot.mode, SubmitAnswerRequest(
                        sessionId = session.sessionId, action = "same", expectedIndex = page.index,
                        expectedSlot = SessionAnswerBaseline(page.savedAnswer, page.savedConfidence,
                            page.savedPreAnswer, page.savedErrorReason),
                        answer = draft.answer.takeIf { draft.kind == SessionDraftKind.ANSWER || draft.kind == SessionDraftKind.REFLECTION },
                        confidence = draft.confidence,
                        preAnswer = draft.preAnswer.takeIf { snapshot.mode == TestMode.RECALL && it.isNotBlank() },
                        errorReason = draft.errorReason.takeIf { draft.kind == SessionDraftKind.REFLECTION },
                    ))
                    applyQuestion(sessionService.question(snapshot.mode, session.sessionId))
                }
            }.onFailure(::handleSessionFailure)
        }
    }

    private fun setBusy() {
        _state.update { it.copy(isLoading = true, error = null, hasUnknownError = false) }
    }

    private suspend fun <T> readSetupMetadata(fallback: T, block: suspend () -> T): T = try {
        block()
    } catch (error: CancellationException) {
        throw error
    } catch (failure: Exception) {
        kotlin.coroutines.coroutineContext.ensureActive()
        _state.update { it.copy(metadataError = it.metadataError ?: failure) }
        fallback
    }

    private fun handleSessionFailure(error: Throwable) {
        if (error is CancellationException) throw error
        timerJob?.cancel()
        _state.update { it.copy(recoveryRequired = true,finishStatusChecked=false) }
        fail(error)
    }

    private fun fail(error: Throwable) {
        if (error is CancellationException) throw error
        _state.update {
            it.copy(
                isLoading = false,
                error = error.message,
                hasUnknownError = error.message == null,
            )
        }
    }

    override fun onCleared() {
        timerJob?.cancel()
        setup.cancelCount()
        super.onCleared()
    }

    private data class SetupMetadata(
        val categories: Result<List<CategoryDto>>,
        val tags: Result<List<TagDto>>,
        val blueprints: Result<List<BlueprintDto>>,
        val books: Result<List<SourceBookDto>>,
        val bookmarks: Result<Int>,
        val due: Result<Int>,
    )

    private data class Initialization(
        val categories: List<CategoryDto>,
        val tags: List<TagDto>,
        val books: List<SourceBookDto>,
        val bookmarkCount: Int,
        val srsDueCount: Int,
        val blueprints: List<BlueprintDto>,
        val statuses: List<Pair<TestMode, SessionProgressDto?>>,
        val completed: SessionResultsDto? = null,
    )

    class Factory(
        private val questionService: LearnerQuestions,
        private val sessionService: LearnerSessions,
        private val bookmarkService: LearnerBookmarks,
        private val categoryService: LearnerCategories,
        private val blueprintService: LearnerBlueprints,
        private val canUseBlueprint: Boolean,
        private val userId: Long,
        private val draftStore: LearnerStorage,
        private val preferences: com.mukhtabir.android.core.AppPreferences,
        private val imageRepository: com.mukhtabir.android.data.remote.LearnerImages,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T = QuizViewModel(
            questionService,
            sessionService,
            bookmarkService,
            categoryService,
            blueprintService,
            canUseBlueprint,
            userId,
            extras.createSavedStateHandle(),
            draftStore,
            preferences,
            imageRepository,
        ) as T
    }
}

internal fun TestMode.isLearningMode(): Boolean = this == TestMode.STUDY || this == TestMode.RECALL

private fun SessionProgressDto.toSession(mode: TestMode): ExamSessionDto = ExamSessionDto(
    sessionId = requireNotNull(sessionId),
    mode = mode.path,
    questionIds = questionIds,
    currentIndex = currentIndex,
    tag = tag,
    startedAt = startedAt,
    accumulatedTime = accumulatedTime,
    durationMinutes = durationMinutes,
    isActive = isActive,
)
