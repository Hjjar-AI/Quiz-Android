package com.mukhtabir.android.ui.planner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mukhtabir.android.shared.R
import com.mukhtabir.android.data.remote.LearnerCategories
import com.mukhtabir.android.data.remote.LearnerPlanner
import com.mukhtabir.android.data.remote.LearnerQuestions
import com.mukhtabir.android.ui.components.AppButton
import com.mukhtabir.android.ui.components.AppCard
import com.mukhtabir.android.ui.components.AppCheckboxRow
import com.mukhtabir.android.ui.components.AppConfirmationDialog
import com.mukhtabir.android.ui.components.AppDangerOutlinedButton
import com.mukhtabir.android.ui.components.AppDangerTextButton
import com.mukhtabir.android.ui.components.AppLayoutTokens
import com.mukhtabir.android.ui.components.AppLoadingIndicator
import com.mukhtabir.android.ui.components.AppMetadataChips
import com.mukhtabir.android.ui.components.AppMetadataRow
import com.mukhtabir.android.ui.components.AppOutlinedButton
import com.mukhtabir.android.ui.components.AppProgressBar
import com.mukhtabir.android.ui.components.AppSection
import com.mukhtabir.android.ui.components.AppStatus
import com.mukhtabir.android.ui.components.AppTextButton
import com.mukhtabir.android.ui.components.AppTextField
import com.mukhtabir.android.ui.components.BlockedBackHandler
import com.mukhtabir.android.ui.components.EmptyState
import com.mukhtabir.android.ui.components.ErrorState
import com.mukhtabir.android.ui.components.ExactTagEditor
import com.mukhtabir.android.ui.components.ScreenBusyEffect
import com.mukhtabir.android.ui.components.collectAsLifecycleState
import com.mukhtabir.android.ui.theme.AppTheme
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlinx.coroutines.flow.update

@Composable
fun PlannerRoute(service: LearnerPlanner, categoryService: LearnerCategories, questionService: LearnerQuestions, userId: Long,
    store: com.mukhtabir.android.core.LearnerStorage, onBack: () -> Unit, onBusy: (Boolean) -> Unit) {
    val vm: PlannerViewModel = viewModel(key = "planner-$userId",
        factory = PlannerViewModel.Factory(service, categoryService, questionService, store, userId))
    val state by vm.state.collectAsLifecycleState()
    val pending by vm.hasRecovery.collectAsLifecycleState()
    val storageError by vm.recoveryError.collectAsLifecycleState()
    BlockedBackHandler(enabled = state.loading)
    ScreenBusyEffect(state.loading, onBusy)
    var confirmDiscard by rememberSaveable(userId) { mutableStateOf(false) }
    var confirmDelete by rememberSaveable(userId) { mutableStateOf(false) }
    var showCategories by rememberSaveable(userId) { mutableStateOf(false) }
    val enabled = state.loaded && !state.loading && !pending
    val canWrite = enabled && !state.conflict && !state.recoveryRequired && storageError == null
    val progress = state.planner?.let { (it.dailyProgress[it.today] ?: 0) to it.targetQuestionsPerDay }
    val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault())
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(AppLayoutTokens.pageInsets),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
        if (state.loading) AppLoadingIndicator()
        storageError?.let { ErrorState(throwable = it, onRetry = if(!state.loading) vm::retryStorage else null) }
        if(pending || storageError != null) {
            Text(stringResource(R.string.planner_draft_found))
            if(pending) AppOutlinedButton(vm::restoreDraft, enabled = state.loaded && !state.loading) { Text(stringResource(R.string.planner_draft_restore)) }
            AppOutlinedButton({confirmDiscard=true}, enabled = state.loaded && !state.loading) { Text(stringResource(R.string.planner_draft_discard)) }
        }
        AppOutlinedButton({ vm.keepDraft(onBack) }, enabled = enabled && storageError == null) { Text(stringResource(R.string.planner_keep_draft)) }
        state.error?.let { ErrorState(throwable = it, onRetry = if (state.loading) null else vm::load,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
        state.actionError?.let { ErrorState(throwable = it, onRetry = if (state.loading) null else vm::load,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
        if(state.conflict) {
            Text(stringResource(R.string.content_conflict))
            AppOutlinedButton(vm::load,enabled=!state.loading) {Text(stringResource(R.string.refresh))}
            AppOutlinedButton({vm.resolvePlan(true)},enabled=enabled && !state.recoveryRequired && storageError==null) {Text(stringResource(R.string.content_keep_draft))}
            AppOutlinedButton({vm.resolvePlan(false)},enabled=enabled && !state.recoveryRequired && storageError==null) {Text(stringResource(R.string.content_use_server))}
        }
        if (state.recoveryRequired) Text(stringResource(R.string.planner_recovery_required),
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        if(state.loaded) state.planner?.let { plan ->
            AppSection(stringResource(R.string.planner_server_plan)) {
                Text(stringResource(R.string.planner_server_target, com.mukhtabir.android.util.formatNumber(plan.targetQuestionsPerDay)))
                Text(listOfNotNull(plan.startDate, plan.endDate).joinToString(" · ") {
                    com.mukhtabir.android.util.formatCalendarDate(it)
                })
                AppMetadataChips(plan.targetTags)
                AppMetadataChips(plan.targetCategories.map { id ->
                    state.categoryOptions.find { it.id == id }?.name ?: com.mukhtabir.android.util.formatNumber(id)
                })
            }
        }
        state.outcome?.let { Text(stringResource(if (it == PlannerOutcome.SAVED) R.string.saved else R.string.planner_deleted),
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
        state.validationError?.let {
            AppStatus(stringResource(when (it) {
                PlannerValidationError.TARGET -> R.string.planner_invalid_target
                PlannerValidationError.CATEGORIES -> R.string.planner_invalid_categories
                PlannerValidationError.DATE -> R.string.planner_invalid_date
                PlannerValidationError.DATE_ORDER -> R.string.planner_invalid_date_order
            }), modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
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
            AppCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(AppLayoutTokens.denseCardInsets), verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
                    Text(stringResource(R.string.today_progress, com.mukhtabir.android.util.formatNumber(done), com.mukhtabir.android.util.formatNumber(target)))
                    AppProgressBar((done.toFloat() / target.coerceAtLeast(1)).coerceIn(0f, 1f), Modifier.fillMaxWidth())
                }
            }
        }
        val categoryError = if (state.validationError == PlannerValidationError.CATEGORIES) stringResource(R.string.planner_invalid_categories) else null
        AppSection(stringResource(R.string.section_planner_goal), initiallyExpanded = true, revealRequest = state.validationAttempt, revealError = state.validationError in setOf(PlannerValidationError.TARGET, PlannerValidationError.CATEGORIES)) {
            AppTextField(state.target, { vm.update("target", it) }, enabled = enabled,
                label = { Text(stringResource(R.string.daily_target)) }, isError = state.validationError == PlannerValidationError.TARGET,
                supportingText = if (state.validationError == PlannerValidationError.TARGET) { { Text(stringResource(R.string.planner_invalid_target)) } } else null, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(),
                textStyle = com.mukhtabir.android.util.ltrInputStyle(),
            )
            Text(stringResource(R.string.source_categories), modifier = Modifier.semantics { heading(); if (categoryError != null) error(categoryError) })
            if (state.selectedCategories.isEmpty()) Text(stringResource(R.string.planner_all_categories))
            state.selectedCategories.forEach { id ->
                Text(state.categoryOptions.firstOrNull { it.id == id }?.name ?: stringResource(R.string.planner_category_unavailable, com.mukhtabir.android.util.formatNumber(id)))
            }
            AppOutlinedButton({ showCategories = true }, enabled = enabled) { Text(stringResource(R.string.select_categories)) }
            if (state.optionsLoading) AppLoadingIndicator()
            state.optionsError?.let { ErrorState(throwable = it, onRetry = if (state.optionsLoading) null else vm::loadOptions) }
        }
        AppSection(stringResource(R.string.additional_tags)) {
        Text(stringResource(R.string.additional_tags), modifier = Modifier.semantics { heading() })
        ExactTagEditor(state.tags, state.tagInput, { vm.update("tagInput", it) }, vm::addTag, vm::removeTag, enabled,
            validation = state.tagValidation?.let { stringResource(it) },
            suggestions = state.tagOptions.filter { it !in state.tags && it.contains(state.tagInput.trim(), ignoreCase = true) }.take(10),
            suggestionsTitle = stringResource(R.string.planner_tag_suggestions))
        }
        PlannerDateField(R.string.start_date, state.startDate, state.serverToday, enabled, formatter, fieldError = state.validationError?.takeIf { it == PlannerValidationError.DATE || it == PlannerValidationError.DATE_ORDER }?.let { stringResource(if (it == PlannerValidationError.DATE_ORDER) R.string.planner_invalid_date_order else R.string.planner_invalid_date) }) { vm.update("start", it) }
        PlannerDateField(R.string.end_date, state.endDate, state.startDate.ifBlank { state.serverToday }, enabled, formatter, fieldError = state.validationError?.takeIf { it == PlannerValidationError.DATE || it == PlannerValidationError.DATE_ORDER }?.let { stringResource(if (it == PlannerValidationError.DATE_ORDER) R.string.planner_invalid_date_order else R.string.planner_invalid_date) }) { vm.update("end", it) }
        if (state.endDate.isNotBlank()) AppTextButton({ vm.update("end", "") }, enabled = enabled) { Text(stringResource(R.string.planner_clear_end)) }
        Row(horizontalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
            AppButton(vm::save, enabled = canWrite) { Text(stringResource(R.string.save)) }
            AppDangerOutlinedButton({ confirmDelete = true }, enabled = state.planner != null && canWrite) { Text(stringResource(R.string.ux_delete_plan)) }
        }
        state.planner?.let { plan ->
            parsePlannerDate(plan.today)?.let { today ->
                (6 downTo 0).forEach { offset ->
                    val day = today.minusDays(offset.toLong())
                    AppMetadataRow(listOf(stringResource(R.string.planner_day_progress,day.format(formatter), com.mukhtabir.android.util.formatNumber(plan.dailyProgress[day.toString()] ?: 0))))
                }
            }
        }
    }
    if (showCategories) AppConfirmationDialog(
        onDismissRequest = { showCategories = false },
        title = { Text(stringResource(R.string.select_categories)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                val options = state.categoryOptions.map { it.id to it.name } +
                    state.selectedCategories.filter { id -> state.categoryOptions.none { it.id == id } }
                        .map { it to stringResource(R.string.planner_category_unavailable, com.mukhtabir.android.util.formatNumber(it)) }
                if (options.isEmpty() && !state.optionsLoading && state.optionsError==null && state.loaded) EmptyState(stringResource(R.string.no_items))
                options.forEach { (id, name) ->
                    AppCheckboxRow(name, id in state.selectedCategories,
                        onCheckedChange = { vm.toggleCategory(id) }, enabled = enabled)
                }
            }
        },
        confirmButton = { AppTextButton({ showCategories = false }) { Text(stringResource(R.string.done)) } },
    )
    if (confirmDiscard) AppConfirmationDialog(onDismissRequest={confirmDiscard=false},
        title={Text(stringResource(R.string.planner_draft_discard))},
        text={Text(stringResource(R.string.planner_draft_discard_confirm))},
        confirmButton={AppDangerTextButton({confirmDiscard=false;vm.discardStoredDraft()},enabled=state.loaded && !state.loading){Text(stringResource(R.string.planner_draft_discard))}},
        dismissButton={AppTextButton({confirmDiscard=false}){Text(stringResource(R.string.cancel))}})
    if (confirmDelete) AppConfirmationDialog(
        onDismissRequest = { confirmDelete = false }, title = { Text(stringResource(R.string.ux_delete_plan)) },
        text = { Text(stringResource(R.string.planner_delete_confirmation)) },
        confirmButton = { AppDangerTextButton({ confirmDelete = false; vm.delete() }, enabled = canWrite) { Text(stringResource(R.string.ux_delete_plan)) } },
        dismissButton = { AppTextButton({ confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
    )
}
