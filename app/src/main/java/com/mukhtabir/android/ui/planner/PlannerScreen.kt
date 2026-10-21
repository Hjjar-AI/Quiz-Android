package com.mukhtabir.android.ui.planner

import com.mukhtabir.android.ui.theme.AppTheme
import com.mukhtabir.android.ui.components.AppSection

import com.mukhtabir.android.ui.components.AppPageHeading

import com.mukhtabir.android.ui.components.AppLoadingIndicator

import com.mukhtabir.android.ui.components.AppLayoutTokens

import com.mukhtabir.android.ui.components.AppDangerTextButton

import com.mukhtabir.android.ui.components.AppDangerOutlinedButton

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.CategoryService
import com.mukhtabir.android.data.remote.QuestionService
import com.mukhtabir.android.data.remote.PlannerService
import com.mukhtabir.android.ui.components.ErrorState
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlinx.coroutines.flow.update

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
            val today = parsePlannerDate(state.serverToday)
            val start = plan?.startDate?.let(::parsePlannerDate)
            val end = plan?.endDate?.let(::parsePlannerDate)
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
            parsePlannerDate(plan.today)?.let { today ->
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
