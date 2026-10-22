package com.mukhtabir.android.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.ModerationService
import com.mukhtabir.android.data.remote.QuestionRatingRowDto
import com.mukhtabir.android.data.remote.localizedForDisplay
import com.mukhtabir.android.ui.components.AppButton
import com.mukhtabir.android.ui.components.AppCard
import com.mukhtabir.android.ui.components.AppConfirmationDialog
import com.mukhtabir.android.ui.components.AppFilterChip
import com.mukhtabir.android.ui.components.AppLayoutTokens
import com.mukhtabir.android.ui.components.AppLoadingIndicator
import com.mukhtabir.android.ui.components.AppMetadataRow
import com.mukhtabir.android.ui.components.AppOutlinedButton
import com.mukhtabir.android.ui.components.AppPageHeading
import com.mukhtabir.android.ui.components.AppPagination
import com.mukhtabir.android.ui.components.AppSection
import com.mukhtabir.android.ui.components.AppStatus
import com.mukhtabir.android.ui.components.AppStatusKind
import com.mukhtabir.android.ui.components.AppTextButton
import com.mukhtabir.android.ui.components.AppTextField
import com.mukhtabir.android.ui.components.BlockedBackHandler
import com.mukhtabir.android.ui.components.ClinicalCaseContext
import com.mukhtabir.android.ui.components.EmptyState
import com.mukhtabir.android.ui.components.ErrorState
import com.mukhtabir.android.ui.components.MarkdownText
import com.mukhtabir.android.ui.components.RefreshOnResume
import com.mukhtabir.android.ui.components.collectAsLifecycleState
import com.mukhtabir.android.ui.theme.AppTheme

@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
fun ModerationRoute(mode: ModerationMode, service: ModerationService, pageSize: Int, userId: Long,
    canManage: Boolean, onBack: () -> Unit, onOpenQuestion: (Long) -> Unit,
    onQuestionsChanged: () -> Unit, onBusyChanged: (Boolean) -> Unit) {
    val vm: ModerationViewModel=viewModel(key="moderation-$mode-$userId-$pageSize-$canManage",
        factory=ModerationViewModel.Factory(mode,service,pageSize,canManage))
    val s by vm.state.collectAsLifecycleState()
    val flags=mode==ModerationMode.FLAGS
    val tags=mode==ModerationMode.TAGS
    val tagValidation=moderationTagError(moderationTagNames(s.addTags),moderationTagNames(s.removeTags))
    var action by rememberSaveable { mutableStateOf<String?>(null) }
    DisposableEffect(s.busy) {
        onBusyChanged(s.busy)
        onDispose { onBusyChanged(false) }
    }
    RefreshOnResume(vm::refresh)
    LaunchedEffect(s.invalidated) { if(s.invalidated) { vm.consumeInvalidation();onQuestionsChanged() } }
    BlockedBackHandler(enabled = s.busy)
    if(!canManage) {
        Text(stringResource(R.string.error_forbidden),modifier=Modifier.padding(AppLayoutTokens.pageInsets))
        return
    }
    val enabled=!s.busy && !s.loading
    if(action!=null) AppConfirmationDialog(
        onDismissRequest={action=null},
        title={Text(stringResource(when(action) {
            "tags" -> R.string.moderation_tags_apply
            "resolve" -> R.string.moderation_resolve
            "unverify" -> R.string.moderation_unverify
            else -> R.string.moderation_verify
        }))},
        text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
            Text(stringResource(R.string.moderation_confirm_count, com.mukhtabir.android.util.formatNumber(s.selected.size)))
            Text(stringResource(when(action) {
                "tags" -> R.string.moderation_tags_warning
                "resolve" -> R.string.moderation_resolve_warning
                "unverify" -> R.string.moderation_unverify_warning
                else -> R.string.moderation_verify_warning
            }))
            if(action=="verify" && s.notes.isNotBlank()) Text(s.notes)
            if(action=="tags") {
                Text(stringResource(R.string.moderation_tags_add)+"\n"+moderationTagNames(s.addTags).joinToString("\n"))
                Text(stringResource(R.string.moderation_tags_remove)+"\n"+moderationTagNames(s.removeTags).joinToString("\n"))
            }
        }},
        confirmButton={AppTextButton({
            when(action) { "tags" -> vm.updateTags();"resolve" -> vm.resolve();"unverify" -> vm.verify(false);else -> vm.verify(true) }
            action=null
        },enabled=enabled && !s.needsReview && s.selected.isNotEmpty() && (action!="tags" || tagValidation==null)){Text(stringResource(R.string.confirm))}},
        dismissButton={AppTextButton({action=null}){Text(stringResource(R.string.cancel))}},
    )
    LazyColumn(Modifier.fillMaxSize().padding(AppLayoutTokens.pageInsets),verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
        item {
            AppPageHeading(stringResource(when(mode) { ModerationMode.FLAGS -> R.string.moderation_flags_title; ModerationMode.TAGS -> R.string.moderation_tags_title; ModerationMode.VERIFICATION -> R.string.moderation_verification_title }))
            AppTextButton(onBack,enabled=!s.busy){Text(stringResource(R.string.cancel))}
            AppOutlinedButton(vm::refresh,enabled=enabled){Text(stringResource(R.string.refresh))}
            Text(stringResource(R.string.moderation_page_selection))
        }
        if(!flags) item {
            AppSection(stringResource(R.string.section_moderation_controls), initiallyExpanded = true) {
                AppTextField(s.query,vm::query,enabled=enabled,label={Text(stringResource(R.string.search))},modifier=Modifier.fillMaxWidth())
                AppFilterChip(selected=s.unverifiedOnly,onClick={vm.unverified(!s.unverifiedOnly)},enabled=enabled,
                    label={Text(stringResource(R.string.moderation_unverified_only))})
                AppOutlinedButton(vm::applySearch,enabled=enabled){Text(stringResource(R.string.search))}
                if(s.loaded) Text(stringResource(R.string.moderation_applied_filter,
                    s.appliedQuery.ifBlank { stringResource(R.string.none) },
                    stringResource(if(s.appliedUnverifiedOnly) R.string.moderation_unverified_only else R.string.moderation_all_questions)))
                if(tags) {
                    Text(stringResource(R.string.moderation_tags_hint))
                    AppTextField(s.addTags,{vm.tagField(true,it)},enabled=enabled,
                        label={Text(stringResource(R.string.moderation_tags_add))},minLines=2,modifier=Modifier.fillMaxWidth())
                    AppTextField(s.removeTags,{vm.tagField(false,it)},enabled=enabled,
                        label={Text(stringResource(R.string.moderation_tags_remove))},minLines=2,modifier=Modifier.fillMaxWidth())
                    val error=s.tagError ?: tagValidation?.takeUnless { it==ModerationTagError.EMPTY }
                    error?.let { AppStatus(stringResource(when(it) {
                        ModerationTagError.EMPTY -> R.string.moderation_tags_empty
                        ModerationTagError.TOO_LONG -> R.string.moderation_tags_length
                        ModerationTagError.OVERLAP -> R.string.moderation_tags_overlap
                    })) }
                } else AppTextField(s.notes,vm::notes,enabled=enabled,label={Text(stringResource(R.string.moderation_notes))},
                    minLines=2,modifier=Modifier.fillMaxWidth())
            }
        }
        if(s.loading || s.busy) item { AppLoadingIndicator() }
        s.error?.let { failure -> item { ErrorState(modifier=Modifier.semantics { liveRegion=LiveRegionMode.Polite },throwable=failure,onRetry=if(enabled) vm::retry else null) } }
        if(s.ratingsLoading) item { Text(stringResource(R.string.moderation_ratings_loading)) }
        s.ratingsError?.let { failure -> item {
            Text(stringResource(R.string.moderation_ratings_failed))
            ErrorState(throwable=failure,onRetry=if(enabled && !s.ratingsLoading) vm::refreshRatings else null)
        } }
        s.actionError?.let { failure -> item { ErrorState(modifier=Modifier.semantics { liveRegion=LiveRegionMode.Polite },throwable=failure) } }
        s.processed?.takeIf { !s.busy }?.let { count -> item { Text(stringResource(R.string.moderation_summary, com.mukhtabir.android.util.formatNumber(count), com.mukhtabir.android.util.formatNumber(s.submitted)),
            modifier=Modifier.semantics { liveRegion=LiveRegionMode.Polite }) } }
        if(s.needsReview) item {
            AppStatus(stringResource(R.string.moderation_review_required), kind = AppStatusKind.WARNING)
            AppOutlinedButton(vm::refresh,enabled=enabled){Text(stringResource(R.string.refresh))}
        }
        item {
            Text(stringResource(R.string.moderation_selected, com.mukhtabir.android.util.formatNumber(s.selected.size)))
            AppOutlinedButton(vm::selectPage,enabled=enabled && !s.needsReview && s.loaded){Text(stringResource(R.string.moderation_select_page))}
            AppTextButton(vm::clearSelection,enabled=enabled && s.selected.isNotEmpty()){Text(stringResource(R.string.moderation_clear_selection))}
            if(flags) AppButton({action="resolve"},enabled=enabled && !s.needsReview && s.selected.isNotEmpty()) {
                Text(stringResource(R.string.moderation_resolve))
            } else if(tags) {
                AppButton({action="tags"},enabled=enabled && !s.needsReview && s.selected.isNotEmpty() && tagValidation==null) {
                    Text(stringResource(R.string.moderation_tags_apply))
                }
            } else {
                AppButton({action="verify"},enabled=enabled && !s.needsReview && s.selected.isNotEmpty()){Text(stringResource(R.string.moderation_verify))}
                AppOutlinedButton({action="unverify"},enabled=enabled && !s.needsReview && s.selected.isNotEmpty()){Text(stringResource(R.string.moderation_unverify))}
            }
        }
        if(s.loaded && !s.loading && s.error==null && (if(flags) s.flags.isEmpty() else s.questions.isEmpty())) item { EmptyState(stringResource(R.string.moderation_empty)) }
        if(flags) items(s.flags,key={it.id}) { flag ->
            AppCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(AppLayoutTokens.denseCardInsets),verticalArrangement=Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
                    ModerationSelection(s.selected.contains(flag.id),enabled && !s.needsReview,
                        stringResource(R.string.moderation_report_id,flag.id),{vm.toggle(flag.id)})
                    Text(flag.questionText)
                    RatingSummary(s.ratings[flag.questionId])
                    Text(stringResource(R.string.moderation_reporter,flag.flaggerUsername))
                    Text(stringResource(R.string.moderation_author,flag.questionAuthor.ifBlank { stringResource(R.string.none) }))
                    flag.createdAt?.let { AppMetadataRow(listOf(it)) }
                    if(!flag.reason.isNullOrBlank()) Text(stringResource(R.string.moderation_reason,flag.reason.orEmpty()))
                    AppOutlinedButton({onOpenQuestion(flag.questionId)},enabled=!s.busy){Text(stringResource(R.string.question_details))}
                }
            }
        } else items(s.questions,key={it.id}) { question ->
            val display=question.localizedForDisplay()
            AppCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(AppLayoutTokens.denseCardInsets),verticalArrangement=Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
                    ModerationSelection(question.id in s.selected,enabled && !s.needsReview,
                        stringResource(R.string.moderation_question_id,question.id),{vm.toggle(question.id)})
                    Text(stringResource(if(question.verified) R.string.verified else R.string.unverified))
                    RatingSummary(s.ratings[question.id])
                    if(tags) Text(stringResource(R.string.moderation_tags_current,question.tags.joinToString(" · ").ifBlank { stringResource(R.string.none) }))
                    ClinicalCaseContext(display.case)
                    MarkdownText(display.question)
                    if(!question.answersHidden && !question.verificationNotes.isNullOrBlank()) Text(stringResource(R.string.moderation_existing_notes,question.verificationNotes.orEmpty()))
                    AppOutlinedButton({onOpenQuestion(question.id)},enabled=!s.busy){Text(stringResource(R.string.question_details))}
                }
            }
        }
        item {
            AppPagination(s.page, s.totalPages,
                onPrevious = { vm.page(s.page - 1) }, onNext = { vm.page(s.page + 1) },
                previousEnabled = enabled, nextEnabled = enabled,
                statusLabel = stringResource(R.string.moderation_page, com.mukhtabir.android.util.formatNumber(s.page), com.mukhtabir.android.util.formatNumber(s.totalPages), com.mukhtabir.android.util.formatNumber(s.total)))
        }
    }
}

@Composable
private fun ModerationSelection(selected: Boolean, enabled: Boolean, label: String, toggle: () -> Unit) {
    Row(modifier=Modifier.fillMaxWidth().toggleable(value=selected,enabled=enabled,role=Role.Checkbox,onValueChange={toggle()}),
        verticalAlignment=Alignment.CenterVertically) {
        Checkbox(checked=selected,onCheckedChange=null,enabled=enabled)
        Text(label,modifier=Modifier.weight(1f))
    }
}


@Composable
private fun RatingSummary(rating: QuestionRatingRowDto?) {
    Text(when {
        rating == null -> stringResource(R.string.moderation_rating_unavailable)
        rating.count == 0 -> stringResource(R.string.moderation_no_ratings)
        else -> stringResource(R.string.moderation_rating_summary, rating.average, com.mukhtabir.android.util.formatNumber(rating.count), if(rating.userRating in 1..5) "${rating.userRating}/5" else stringResource(R.string.none))
    }, style=MaterialTheme.typography.bodySmall, color=MaterialTheme.colorScheme.onSurfaceVariant)
}
