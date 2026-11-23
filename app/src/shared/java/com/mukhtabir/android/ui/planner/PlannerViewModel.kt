package com.mukhtabir.android.ui.planner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mukhtabir.android.shared.R
import com.mukhtabir.android.data.remote.LearnerCategories
import com.mukhtabir.android.data.remote.LearnerQuestions
import com.mukhtabir.android.data.remote.PlannerDto
import com.mukhtabir.android.data.remote.LearnerPlanner
import com.mukhtabir.android.data.remote.PlannerWriteRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PlannerViewModel(
    private val service: LearnerPlanner,
    private val categoryService: LearnerCategories,
    private val questionService: LearnerQuestions,
    private val store: com.mukhtabir.android.core.LearnerStorage,
    private val userId: Long,
) : ViewModel() {
    private val _state = MutableStateFlow(PlannerUiState())
    val state = _state.asStateFlow()
    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val storageKey = "planner-new"
    private val storageMutex = kotlinx.coroutines.sync.Mutex()
    private val _pending = MutableStateFlow<StoredPlannerDraft?>(null)
    val hasRecovery = MutableStateFlow(false)
    val recoveryError = MutableStateFlow<Throwable?>(null)
    private var unreadable = false
    init {
        viewModelScope.launch { readRecovery(); load(); loadOptions() }
        viewModelScope.launch {
            state.collect { value -> if(value.loaded && !value.loading && !hasRecovery.value && !unreadable) persist(value, automatic = true) }
        }
    }
    private suspend fun readRecovery() {
        try {
            val raw = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { store.readEditor(userId, storageKey).getOrThrow() }
            kotlin.coroutines.coroutineContext.ensureActive()
            _pending.value = raw?.let { json.decodeFromString(StoredPlannerDraft.serializer(), it) }
            hasRecovery.value = _pending.value != null; unreadable = false; recoveryError.value = null
        } catch(cancel: CancellationException) { throw cancel }
        catch(failure: Exception) { unreadable = true; recoveryError.value = failure }
    }
    private fun dirty(s: PlannerUiState): Boolean = s.target != (s.planner?.targetQuestionsPerDay ?: 10).toString() ||
        s.selectedCategories != s.planner?.targetCategories.orEmpty().toSet() || s.tags != s.planner?.targetTags.orEmpty().distinct() ||
        s.tagInput.isNotEmpty() || s.startDate != (s.planner?.startDate ?: s.serverToday) || s.endDate != s.planner?.endDate.orEmpty()
    private suspend fun persist(s: PlannerUiState, pending: Boolean = s.recoveryRequired, automatic: Boolean = false): Boolean {
        storageMutex.lock()
        try {
            // A queued idle snapshot must never overwrite a marker committed before dispatch.
            if(automatic && (_state.value != s || _state.value.loading || hasRecovery.value || unreadable)) return true
            val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                if (!dirty(s) && !pending) store.clearEditor(userId, storageKey)
                else store.writeEditor(userId, storageKey, json.encodeToString(StoredPlannerDraft.serializer(),
                    StoredPlannerDraft(s.target, s.selectedCategories, s.tags, s.tagInput, s.startDate, s.endDate, pending,s.baselineVersion,s.baselineId)))
            }
            kotlin.coroutines.coroutineContext.ensureActive(); recoveryError.value = result.exceptionOrNull()
            if(result.isSuccess) unreadable = false
            return result.isSuccess
        } finally { storageMutex.unlock() }
    }
    fun retryStorage() {
        if (_state.value.loading) return
        viewModelScope.launch { if (unreadable) readRecovery() else persist(_state.value) }
    }
    fun restoreDraft() {
        val saved = _pending.value ?: return
        if (!_state.value.loaded || _state.value.loading) return
        _state.update { it.copy(target = saved.target, selectedCategories = saved.categories, tags = saved.tags,
            tagInput = saved.tagInput, startDate = saved.start, endDate = saved.end, recoveryRequired = saved.pending, baselineVersion=saved.version,baselineId=saved.plannerId,outcome = null) }
        _pending.value = null; hasRecovery.value = false
        if(saved.pending) load() // Read-only reconciliation; never replay a planner save/delete.
    }
    fun discardStoredDraft() {
        if (!_state.value.loaded || _state.value.loading) return
        viewModelScope.launch {
            storageMutex.lock()
            try {
                val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { store.clearEditor(userId, storageKey) }
                kotlin.coroutines.coroutineContext.ensureActive(); recoveryError.value = result.exceptionOrNull()
                if(result.isSuccess) { _pending.value = null; hasRecovery.value = false; unreadable = false }
            } finally { storageMutex.unlock() }
        }
    }
    fun keepDraft(onKept: () -> Unit) {
        if (!_state.value.loaded || _state.value.loading || hasRecovery.value || unreadable) return
        val snapshot = _state.value
        _state.update { it.copy(loading = true) }
        viewModelScope.launch {
            val kept = persist(snapshot)
            _state.update { it.copy(loading = false) }
            if(kept) onKept()
        }
    }

    fun load() {
        if (_state.value.loading) return
        val keepDraft = _state.value.loaded
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching {
                // Initialize the daily ledger only for the initial load.
                if (!keepDraft && !hasRecovery.value && !unreadable) service.recordProgress()
                service.get()
            }.onSuccess { planner ->
                kotlin.coroutines.coroutineContext.ensureActive()
                applyPlanner(planner, keepDraft)
                _state.update { it.copy(loading = false, recoveryRequired = false, actionError = null) }
            }.onFailure { failure ->
                if (failure is CancellationException) throw failure
                kotlin.coroutines.coroutineContext.ensureActive()
                _state.update { it.copy(loading = false, error = failure) }
            }
        }
    }

    fun loadOptions() {
        if (_state.value.optionsLoading) return
        _state.update { it.copy(optionsLoading = true, optionsError = null) }
        viewModelScope.launch {
            val (categories, tags) = coroutineScope {
                val categories = async { lookup { categoryService.list().items } }
                val tags = async { lookup { questionService.tags().items.map { it.name } } }
                categories.await() to tags.await()
            }
            kotlin.coroutines.coroutineContext.ensureActive()
            _state.update {
                it.copy(categoryOptions = categories.getOrDefault(it.categoryOptions),
                    tagOptions = tags.getOrDefault(it.tagOptions), optionsLoading = false,
                    optionsError = categories.exceptionOrNull() ?: tags.exceptionOrNull())
            }
        }
    }

    private suspend fun <T> lookup(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (cancel: CancellationException) {
        throw cancel
    } catch (failure: Exception) {
        Result.failure(failure)
    }

    private fun applyPlanner(planner: PlannerDto, keepDraft: Boolean) = _state.update {
        val matches=it.target.trim().toIntOrNull()==planner.targetQuestionsPerDay && it.selectedCategories==planner.targetCategories.toSet() && it.tags.toSet()==planner.targetTags.toSet() && it.startDate==planner.startDate && it.endDate==planner.endDate.orEmpty()
        val confirmed=keepDraft && matches && (it.recoveryRequired || it.outcome==PlannerOutcome.SAVED)
        val base = it.copy(planner = planner, serverToday = planner.today, loaded = true,baselineVersion=if(confirmed) planner.version else it.baselineVersion,baselineId=if(confirmed) planner.id else it.baselineId,outcome=if(confirmed) PlannerOutcome.SAVED else it.outcome)
        if (keepDraft) base else base.copy(baselineVersion=planner.version,baselineId=planner.id,
            target = planner.targetQuestionsPerDay.toString(), selectedCategories = planner.targetCategories.toSet(),
            tags = planner.targetTags.distinct(), tagInput = "", tagValidation = null, startDate = planner.startDate,
            endDate = planner.endDate.orEmpty(), validationError = null,
        )
    }

    fun update(field: String, value: String) = _state.update {
        if (!it.loaded || it.loading) it else when (field) {
            "target" -> it.copy(target = value, validationError = null, outcome = null)
            "tagInput" -> it.copy(tagInput = value, tagValidation = null, outcome = null)
            "start" -> it.copy(startDate = value, validationError = null, outcome = null)
            "end" -> it.copy(endDate = value, validationError = null, outcome = null)
            else -> it
        }
    }

    fun toggleCategory(id: Long) = _state.update {
        if (!it.loaded || it.loading || (id !in it.selectedCategories && it.categoryOptions.none { category -> category.id == id })) it
        else it.copy(selectedCategories = if (id in it.selectedCategories) it.selectedCategories - id else it.selectedCategories + id,
            validationError = null, outcome = null)
    }

    fun addTag(value: String = _state.value.tagInput) {
        val current = _state.value
        if (!current.loaded || current.loading) return
        val name = value.trim()
        val error = when {
            name.isEmpty() -> R.string.editor_tag_required
            name.codePointCount(0, name.length) > 50 -> R.string.editor_tag_too_long
            else -> null
        }
        if (error != null) _state.update { it.copy(tagValidation = error) }
        else _state.update { it.copy(tags = (it.tags + name).distinct(), tagInput = "", tagValidation = null, outcome = null) }
    }

    fun removeTag(name: String) {
        if (_state.value.loaded && !_state.value.loading) {
            _state.update { it.copy(tags = it.tags - name, tagValidation = null, outcome = null) }
        }
    }

    private fun invalid(error: PlannerValidationError) {
        _state.update { it.copy(validationError = error, validationAttempt = it.validationAttempt + 1) }
    }

    fun resolvePlan(keep: Boolean) {
        val s=_state.value;val plan=s.planner ?: return
        if(s.loading || s.recoveryRequired || hasRecovery.value || recoveryError.value!=null) return
        if(keep) _state.update {it.copy(baselineVersion=plan.version,baselineId=plan.id,actionError=null)}
        else applyPlanner(plan,keepDraft=false)
    }

    fun save() {
        val s = _state.value
        if (!s.loaded || s.conflict || s.loading || s.recoveryRequired || hasRecovery.value || recoveryError.value != null) return
        if (s.tagInput.isNotBlank()) {
            _state.update { it.copy(tagValidation = R.string.editor_tag_pending) }
            return
        }
        val target = s.target.trim().toIntOrNull()
        if (target == null || target !in 1..1000) { invalid(PlannerValidationError.TARGET); return }
        if (s.selectedCategories.any { it <= 0 }) { invalid(PlannerValidationError.CATEGORIES); return }
        val start = parsePlannerDate(s.startDate.trim().ifBlank { s.serverToday })
        val endText = s.endDate.trim()
        val end = endText.takeIf(String::isNotEmpty)?.let(::parsePlannerDate)
        if (start == null || (endText.isNotEmpty() && end == null)) { invalid(PlannerValidationError.DATE); return }
        if (end != null && end < start) { invalid(PlannerValidationError.DATE_ORDER); return }
        val request = PlannerWriteRequest(s.baselineVersion ?: run {invalid(PlannerValidationError.TARGET);return},s.baselineId ?: run {invalid(PlannerValidationError.TARGET);return},target, s.selectedCategories.toList(),
            s.tags, start.toString(), end?.toString())
        beginWrite()
        viewModelScope.launch {
            if(!persist(s, pending = true)) { _state.update { it.copy(loading = false) }; return@launch }
            runCatching { service.update(request) }
                .onSuccess { accepted ->
                    applyPlanner(accepted,keepDraft=false)
                    kotlin.coroutines.coroutineContext.ensureActive()
                    _state.update { it.copy(outcome = PlannerOutcome.SAVED) }
                    persist(_state.value, pending = false)
                    // Record the accepted save separately from any subsequent read failure.
                    runCatching { service.get() }.onSuccess { planner ->
                        kotlin.coroutines.coroutineContext.ensureActive()
                        applyPlanner(planner, keepDraft = false)
                        _state.update { it.copy(loading = false) }
                    }.onFailure { failure -> kotlin.coroutines.coroutineContext.ensureActive(); readAfterWriteFailed(failure) }
                }.onFailure { failure -> kotlin.coroutines.coroutineContext.ensureActive(); writeFailed(failure) }
        }
    }

    fun delete() {
        val s = _state.value
        if (!s.loaded || s.conflict || s.loading || s.recoveryRequired || s.planner == null || hasRecovery.value || recoveryError.value != null) return
        beginWrite()
        viewModelScope.launch {
            if(!persist(s, pending = true)) { _state.update { it.copy(loading = false) }; return@launch }
            runCatching { service.delete(requireNotNull(s.baselineId),requireNotNull(s.baselineVersion)) }.onSuccess {
                kotlin.coroutines.coroutineContext.ensureActive()
                _state.update { it.copy(planner = null,loaded=false,baselineVersion=null,baselineId=null, target = "10", selectedCategories = emptySet(),
                    tags = emptyList(), tagInput = "", tagValidation = null, startDate = it.serverToday, endDate = "", loading = false,
                    outcome = PlannerOutcome.DELETED) }
            }.onFailure { failure -> kotlin.coroutines.coroutineContext.ensureActive(); writeFailed(failure) }
        }
    }

    private fun beginWrite() {
        _state.update { it.copy(loading = true, error = null, actionError = null, outcome = null, validationError = null) }
    }

    private fun writeFailed(failure: Throwable) {
        if (failure is CancellationException) throw failure
        _state.update { it.copy(loading = false, actionError = failure, recoveryRequired = true) }
    }

    private fun readAfterWriteFailed(failure: Throwable) {
        if (failure is CancellationException) throw failure
        _state.update { it.copy(loading = false, error = failure, recoveryRequired = true) }
    }

    class Factory(
        private val service: LearnerPlanner,
        private val categoryService: LearnerCategories,
        private val questionService: LearnerQuestions,
        private val store: com.mukhtabir.android.core.LearnerStorage,
        private val userId: Long,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PlannerViewModel(service, categoryService, questionService, store, userId) as T
    }
}
