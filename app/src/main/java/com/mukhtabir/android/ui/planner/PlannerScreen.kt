package com.mukhtabir.android.ui.planner

import com.mukhtabir.android.ui.theme.AppTheme
import com.mukhtabir.android.ui.components.AppSection

import com.mukhtabir.android.ui.components.AppPageHeading

import com.mukhtabir.android.ui.components.AppLoadingIndicator

import com.mukhtabir.android.ui.components.AppLayoutTokens

import com.mukhtabir.android.ui.components.AppDangerTextButton

import com.mukhtabir.android.ui.components.AppDangerOutlinedButton

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import com.mukhtabir.android.ui.components.AppConfirmationDialog as AlertDialog
import com.mukhtabir.android.ui.components.AppTextButton as TextButton
import com.mukhtabir.android.ui.components.AppButton as Button
import com.mukhtabir.android.ui.components.AppCard as Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import com.mukhtabir.android.ui.components.AppOutlinedButton as OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.error
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.CategoryDto
import com.mukhtabir.android.data.remote.CategoryService
import com.mukhtabir.android.data.remote.QuestionService
import com.mukhtabir.android.data.remote.PlannerDto
import com.mukhtabir.android.data.remote.PlannerService
import com.mukhtabir.android.data.remote.PlannerWriteRequest
import com.mukhtabir.android.ui.components.ErrorState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.FormatStyle
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@kotlinx.serialization.Serializable
private data class StoredPlannerDraft(val target: String, val categories: Set<Long>, val tags: List<String>, val tagInput: String,
    val start: String, val end: String, val pending: Boolean, val version: Int? = null, val plannerId: Long? = null)

enum class PlannerValidationError { TARGET, CATEGORIES, DATE, DATE_ORDER }
enum class PlannerOutcome { SAVED, DELETED }

data class PlannerUiState(
    val planner: PlannerDto? = null,
    val baselineVersion: Int? = null, val baselineId: Long? = null,
    val target: String = "10",
    val selectedCategories: Set<Long> = emptySet(),
    val categoryOptions: List<CategoryDto> = emptyList(),
    val tagOptions: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val tagInput: String = "",
    val tagValidation: Int? = null,
    val startDate: String = "",
    val endDate: String = "",
    val loaded: Boolean = false,
    val serverToday: String = "",
    val validationError: PlannerValidationError? = null,
    val validationAttempt: Int = 0,
    val loading: Boolean = false,
    val optionsLoading: Boolean = false,
    val recoveryRequired: Boolean = false,
    val error: Throwable? = null,
    val actionError: Throwable? = null,
    val optionsError: Throwable? = null,
    val outcome: PlannerOutcome? = null,
) { val conflict: Boolean get() = loaded && planner!=null && (baselineVersion!=planner.version || baselineId!=planner.id) }

class PlannerViewModel(
    private val service: PlannerService,
    private val categoryService: CategoryService,
    private val questionService: QuestionService,
    private val store: com.mukhtabir.android.core.SessionDraftStore,
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
        val start = parseDate(s.startDate.trim().ifBlank { s.serverToday })
        val endText = s.endDate.trim()
        val end = endText.takeIf(String::isNotEmpty)?.let(::parseDate)
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
        private val service: PlannerService,
        private val categoryService: CategoryService,
        private val questionService: QuestionService,
        private val store: com.mukhtabir.android.core.SessionDraftStore,
        private val userId: Long,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PlannerViewModel(service, categoryService, questionService, store, userId) as T
    }
}

private fun parseDate(value: String): LocalDate? {
    if (!Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}").matches(value)) return null
    return try { LocalDate.parse(value).takeIf { it.year in 1..9999 } }
    catch (_: DateTimeParseException) { null }
}

@Composable
fun PlannerRoute(service: PlannerService, categoryService: CategoryService, questionService: QuestionService, userId: Long,
    store: com.mukhtabir.android.core.SessionDraftStore, onBack: () -> Unit, onBusy: (Boolean) -> Unit) {
    val vm: PlannerViewModel = viewModel(key = "planner-$userId",
        factory = PlannerViewModel.Factory(service, categoryService, questionService, store, userId))
    val state by vm.state.collectAsState()
    val pending by vm.hasRecovery.collectAsState()
    val storageError by vm.recoveryError.collectAsState()
    com.mukhtabir.android.ui.components.BlockedBackHandler(enabled = state.loading)
    androidx.compose.runtime.DisposableEffect(state.loading) { onBusy(state.loading); onDispose { onBusy(false) } }
    var confirmDiscard by rememberSaveable(userId) { mutableStateOf(false) }
    var confirmDelete by rememberSaveable(userId) { mutableStateOf(false) }
    var showCategories by rememberSaveable(userId) { mutableStateOf(false) }
    val enabled = state.loaded && !state.loading && !pending
    val canWrite = enabled && !state.conflict && !state.recoveryRequired && storageError == null
    val progress = state.planner?.let { (it.dailyProgress[it.today] ?: 0) to it.targetQuestionsPerDay }
    val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault())
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(AppLayoutTokens.pageInsets),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
        AppPageHeading(stringResource(R.string.nav_planner))
        if (state.loading) AppLoadingIndicator()
        storageError?.let { ErrorState(throwable = it, onRetry = if(!state.loading) vm::retryStorage else null) }
        if(pending || storageError != null) {
            Text(stringResource(R.string.planner_draft_found))
            if(pending) OutlinedButton(vm::restoreDraft, enabled = state.loaded && !state.loading) { Text(stringResource(R.string.planner_draft_restore)) }
            OutlinedButton({confirmDiscard=true}, enabled = state.loaded && !state.loading) { Text(stringResource(R.string.planner_draft_discard)) }
        }
        OutlinedButton({ vm.keepDraft(onBack) }, enabled = enabled && storageError == null) { Text(stringResource(R.string.planner_keep_draft)) }
        state.error?.let { ErrorState(throwable = it, onRetry = if (state.loading) null else vm::load,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
        state.actionError?.let { ErrorState(throwable = it, onRetry = if (state.loading) null else vm::load,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
        if(state.conflict) {
            Text(stringResource(R.string.content_conflict))
            OutlinedButton(vm::load,enabled=!state.loading) {Text(stringResource(R.string.refresh))}
            OutlinedButton({vm.resolvePlan(true)},enabled=enabled && !state.recoveryRequired && storageError==null) {Text(stringResource(R.string.content_keep_draft))}
            OutlinedButton({vm.resolvePlan(false)},enabled=enabled && !state.recoveryRequired && storageError==null) {Text(stringResource(R.string.content_use_server))}
        }
        if (state.recoveryRequired) Text(stringResource(R.string.planner_recovery_required),
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        if(state.loaded) state.planner?.let { plan ->
            AppSection(stringResource(R.string.planner_server_plan)) {
                Text(stringResource(R.string.planner_server_target, plan.targetQuestionsPerDay))
                Text(plan.startDate + " · " + plan.endDate.orEmpty())
                Text(plan.targetTags.joinToString(" · "))
                Text(plan.targetCategories.map { id -> state.categoryOptions.find { it.id==id }?.name ?: id.toString() }.joinToString(" · "))
            }
        }
        state.outcome?.let { Text(stringResource(if (it == PlannerOutcome.SAVED) R.string.saved else R.string.planner_deleted),
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
        state.validationError?.let {
            Text(stringResource(when (it) {
                PlannerValidationError.TARGET -> R.string.planner_invalid_target
                PlannerValidationError.CATEGORIES -> R.string.planner_invalid_categories
                PlannerValidationError.DATE -> R.string.planner_invalid_date
                PlannerValidationError.DATE_ORDER -> R.string.planner_invalid_date_order
            }), color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
        if (state.loaded) {
            val plan = state.planner
            val today = parseDate(state.serverToday)
            val start = plan?.startDate?.let(::parseDate)
            val end = plan?.endDate?.let(::parseDate)
            Text(stringResource(when {
                plan == null -> R.string.planner_empty
                today != null && start != null && today < start -> R.string.planner_scheduled
                today != null && end != null && today > end -> R.string.planner_ended
                else -> R.string.planner_active
            }))
        }
        progress?.let { (done, target) ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(com.mukhtabir.android.ui.components.AppLayoutTokens.denseCardInsets), verticalArrangement = Arrangement.spacedBy(com.mukhtabir.android.ui.components.AppLayoutTokens.controlGap)) {
                    Text(stringResource(R.string.today_progress, done, target))
                    LinearProgressIndicator((done.toFloat() / target.coerceAtLeast(1)).coerceIn(0f, 1f), Modifier.fillMaxWidth())
                }
            }
        }
        val categoryError = if (state.validationError == PlannerValidationError.CATEGORIES) stringResource(R.string.planner_invalid_categories) else null
        AppSection(stringResource(R.string.section_planner_goal), initiallyExpanded = true, revealRequest = state.validationAttempt, revealError = state.validationError in setOf(PlannerValidationError.TARGET, PlannerValidationError.CATEGORIES)) {
            OutlinedTextField(state.target, { vm.update("target", it) }, enabled = enabled,
                label = { Text(stringResource(R.string.daily_target)) }, isError = state.validationError == PlannerValidationError.TARGET,
                supportingText = if (state.validationError == PlannerValidationError.TARGET) { { Text(stringResource(R.string.planner_invalid_target)) } } else null, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(),
                textStyle = com.mukhtabir.android.util.ltrInputStyle(),
            )
            Text(stringResource(R.string.source_categories), modifier = Modifier.semantics { heading(); if (categoryError != null) error(categoryError) })
            if (state.selectedCategories.isEmpty()) Text(stringResource(R.string.planner_all_categories))
            state.selectedCategories.forEach { id ->
                Text(state.categoryOptions.firstOrNull { it.id == id }?.name ?: stringResource(R.string.planner_category_unavailable, id))
            }
            OutlinedButton({ showCategories = true }, enabled = enabled) { Text(stringResource(R.string.select_categories)) }
            if (state.optionsLoading) AppLoadingIndicator()
            state.optionsError?.let { ErrorState(throwable = it, onRetry = if (state.optionsLoading) null else vm::loadOptions) }
        }
        AppSection(stringResource(R.string.additional_tags)) {
        Text(stringResource(R.string.additional_tags), modifier = Modifier.semantics { heading() })
        Text(stringResource(R.string.editor_tags_exact_hint))
        state.tags.forEach { name ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(com.mukhtabir.android.ui.components.AppLayoutTokens.controlGap)) {
                Text(name, Modifier.weight(1f))
                TextButton({ vm.removeTag(name) }, enabled = enabled) { Text(stringResource(R.string.editor_tag_remove, name)) }
            }
        }
        if (state.tags.isEmpty()) Text(stringResource(R.string.none))
        OutlinedTextField(state.tagInput, { vm.update("tagInput", it) }, enabled = enabled, singleLine = true,
            label = { Text(stringResource(R.string.editor_tag_name)) }, modifier = Modifier.fillMaxWidth())
        state.tagValidation?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
        OutlinedButton({ vm.addTag() }, enabled = enabled) { Text(stringResource(R.string.editor_tag_add)) }
        val suggestions = state.tagOptions.filter { it !in state.tags && it.contains(state.tagInput.trim(), ignoreCase = true) }.take(10)
        if (suggestions.isNotEmpty()) {
            Text(stringResource(R.string.planner_tag_suggestions))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(com.mukhtabir.android.ui.components.AppLayoutTokens.controlGap)) {
                suggestions.forEach { tag -> OutlinedButton({ vm.addTag(tag) }, enabled = enabled) { Text(tag) } }
            }
        }
        }
        PlannerDateField(R.string.start_date, state.startDate, state.serverToday, enabled, formatter, fieldError = state.validationError?.takeIf { it == PlannerValidationError.DATE || it == PlannerValidationError.DATE_ORDER }?.let { stringResource(if (it == PlannerValidationError.DATE_ORDER) R.string.planner_invalid_date_order else R.string.planner_invalid_date) }) { vm.update("start", it) }
        PlannerDateField(R.string.end_date, state.endDate, state.startDate.ifBlank { state.serverToday }, enabled, formatter, fieldError = state.validationError?.takeIf { it == PlannerValidationError.DATE || it == PlannerValidationError.DATE_ORDER }?.let { stringResource(if (it == PlannerValidationError.DATE_ORDER) R.string.planner_invalid_date_order else R.string.planner_invalid_date) }) { vm.update("end", it) }
        if (state.endDate.isNotBlank()) TextButton({ vm.update("end", "") }, enabled = enabled) { Text(stringResource(R.string.planner_clear_end)) }
        Row(horizontalArrangement = Arrangement.spacedBy(com.mukhtabir.android.ui.components.AppLayoutTokens.controlGap)) {
            Button(vm::save, enabled = canWrite) { Text(stringResource(R.string.save)) }
            AppDangerOutlinedButton({ confirmDelete = true }, enabled = state.planner != null && canWrite) { Text(stringResource(R.string.ux_delete_plan)) }
        }
        state.planner?.let { plan ->
            parseDate(plan.today)?.let { today ->
                (6 downTo 0).forEach { offset ->
                    val day = today.minusDays(offset.toLong())
                    com.mukhtabir.android.ui.components.AppMetadataRow(listOf(stringResource(R.string.planner_day_progress,day.format(formatter),plan.dailyProgress[day.toString()] ?: 0)))
                }
            }
        }
    }
    if (showCategories) AlertDialog(
        onDismissRequest = { showCategories = false },
        title = { Text(stringResource(R.string.select_categories)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                val options = state.categoryOptions.map { it.id to it.name } +
                    state.selectedCategories.filter { id -> state.categoryOptions.none { it.id == id } }
                        .map { it to stringResource(R.string.planner_category_unavailable, it) }
                if (options.isEmpty() && !state.optionsLoading && state.optionsError==null && state.loaded) com.mukhtabir.android.ui.components.EmptyState(stringResource(R.string.no_items))
                options.forEach { (id, name) ->
                    com.mukhtabir.android.ui.components.AppToggleRow(Modifier.fillMaxWidth().toggleable(
                        value = id in state.selectedCategories, enabled = enabled, role = Role.Checkbox,
                        onValueChange = { vm.toggleCategory(id) }), horizontalArrangement = Arrangement.spacedBy(com.mukhtabir.android.ui.components.AppLayoutTokens.controlGap)) {
                        Checkbox(checked = id in state.selectedCategories, onCheckedChange = null)
                        Text(name, Modifier.weight(1f))
                    }
                }
            }
        },
        confirmButton = { TextButton({ showCategories = false }) { Text(stringResource(R.string.done)) } },
    )
    if (confirmDiscard) AlertDialog(onDismissRequest={confirmDiscard=false},
        title={Text(stringResource(R.string.planner_draft_discard))},
        text={Text(stringResource(R.string.planner_draft_discard_confirm))},
        confirmButton={AppDangerTextButton({confirmDiscard=false;vm.discardStoredDraft()},enabled=state.loaded && !state.loading){Text(stringResource(R.string.planner_draft_discard))}},
        dismissButton={TextButton({confirmDiscard=false}){Text(stringResource(R.string.cancel))}})
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false }, title = { Text(stringResource(R.string.ux_delete_plan)) },
        text = { Text(stringResource(R.string.planner_delete_confirmation)) },
        confirmButton = { AppDangerTextButton({ confirmDelete = false; vm.delete() }, enabled = canWrite) { Text(stringResource(R.string.ux_delete_plan)) } },
        dismissButton = { TextButton({ confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun PlannerDateField(label: Int, value: String, fallback: String, enabled: Boolean,
    formatter: DateTimeFormatter, fieldError: String? = null, onPicked: (String) -> Unit) {
    val context = LocalContext.current
    val selected = parseDate(value)
    Text(stringResource(label), modifier = Modifier.semantics { heading() })
    fieldError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    OutlinedButton(onClick = {
        val initial = selected ?: parseDate(fallback) ?: LocalDate.now()
        DatePickerDialog(context, { _, year, month, day -> onPicked(LocalDate.of(year, month + 1, day).toString()) },
            initial.year, initial.monthValue - 1, initial.dayOfMonth).show()
    }, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = com.mukhtabir.android.ui.components.AppLayoutTokens.touchTarget).semantics { if (fieldError != null) error(fieldError) }) {
        Text(selected?.format(formatter) ?: stringResource(R.string.planner_choose_date))
    }
}
