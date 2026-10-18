package com.mukhtabir.android.ui.admin

import com.mukhtabir.android.ui.theme.AppTheme
import com.mukhtabir.android.ui.components.AppSection

import com.mukhtabir.android.ui.components.AppPageHeading

import com.mukhtabir.android.ui.components.AppLoadingIndicator

import com.mukhtabir.android.ui.components.AppLayoutTokens

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.mukhtabir.android.ui.components.AppConfirmationDialog as AlertDialog
import com.mukhtabir.android.ui.components.AppButton as Button
import com.mukhtabir.android.ui.components.AppCard as Card
import androidx.compose.material3.Checkbox
import com.mukhtabir.android.ui.components.AppFilterChip as FilterChip
import androidx.compose.material3.MaterialTheme
import com.mukhtabir.android.ui.components.AppOutlinedButton as OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import com.mukhtabir.android.ui.components.AppTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.QuestionRatingRowDto
import com.mukhtabir.android.data.remote.ModerationService
import com.mukhtabir.android.data.remote.localizedForDisplay
import com.mukhtabir.android.ui.components.ClinicalCaseContext
import com.mukhtabir.android.ui.components.ErrorState
import com.mukhtabir.android.ui.components.MarkdownText
import com.mukhtabir.android.ui.components.RefreshOnResume

@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
fun ModerationRoute(mode: ModerationMode, service: ModerationService, pageSize: Int, userId: Long,
    canManage: Boolean, onBack: () -> Unit, onOpenQuestion: (Long) -> Unit,
    onQuestionsChanged: () -> Unit, onBusyChanged: (Boolean) -> Unit) {
    val vm: ModerationViewModel=viewModel(key="moderation-$mode-$userId-$pageSize-$canManage",
        factory=ModerationViewModel.Factory(mode,service,pageSize,canManage))
    val s by vm.state.collectAsState()
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
    com.mukhtabir.android.ui.components.BlockedBackHandler(enabled = s.busy)
    if(!canManage) {
        Text(stringResource(R.string.error_forbidden),modifier=Modifier.padding(com.mukhtabir.android.ui.components.AppLayoutTokens.pageInsets))
        return
    }
    val enabled=!s.busy && !s.loading
    if(action!=null) AlertDialog(
        onDismissRequest={action=null},
        title={Text(stringResource(when(action) {
            "tags" -> R.string.moderation_tags_apply
            "resolve" -> R.string.moderation_resolve
            "unverify" -> R.string.moderation_unverify
            else -> R.string.moderation_verify
        }))},
        text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(com.mukhtabir.android.ui.components.AppLayoutTokens.controlGap)) {
            Text(stringResource(R.string.moderation_confirm_count,s.selected.size))
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
        confirmButton={TextButton({
            when(action) { "tags" -> vm.updateTags();"resolve" -> vm.resolve();"unverify" -> vm.verify(false);else -> vm.verify(true) }
            action=null
        },enabled=enabled && !s.needsReview && s.selected.isNotEmpty() && (action!="tags" || tagValidation==null)){Text(stringResource(R.string.confirm))}},
        dismissButton={TextButton({action=null}){Text(stringResource(R.string.cancel))}},
    )
    LazyColumn(Modifier.fillMaxSize().padding(AppLayoutTokens.pageInsets),verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
        item {
            AppPageHeading(stringResource(when(mode) { ModerationMode.FLAGS -> R.string.moderation_flags_title; ModerationMode.TAGS -> R.string.moderation_tags_title; ModerationMode.VERIFICATION -> R.string.moderation_verification_title }))
            TextButton(onBack,enabled=!s.busy){Text(stringResource(R.string.cancel))}
            OutlinedButton(vm::refresh,enabled=enabled){Text(stringResource(R.string.refresh))}
            Text(stringResource(R.string.moderation_page_selection))
        }
        if(!flags) item {
            AppSection(stringResource(R.string.section_moderation_controls), initiallyExpanded = true) {
                OutlinedTextField(s.query,vm::query,enabled=enabled,label={Text(stringResource(R.string.search))},modifier=Modifier.fillMaxWidth())
                FilterChip(selected=s.unverifiedOnly,onClick={vm.unverified(!s.unverifiedOnly)},enabled=enabled,
                    label={Text(stringResource(R.string.moderation_unverified_only))})
                OutlinedButton(vm::applySearch,enabled=enabled){Text(stringResource(R.string.search))}
                if(s.loaded) Text(stringResource(R.string.moderation_applied_filter,
                    s.appliedQuery.ifBlank { stringResource(R.string.none) },
                    stringResource(if(s.appliedUnverifiedOnly) R.string.moderation_unverified_only else R.string.moderation_all_questions)))
                if(tags) {
                    Text(stringResource(R.string.moderation_tags_hint))
                    OutlinedTextField(s.addTags,{vm.tagField(true,it)},enabled=enabled,
                        label={Text(stringResource(R.string.moderation_tags_add))},minLines=2,modifier=Modifier.fillMaxWidth())
                    OutlinedTextField(s.removeTags,{vm.tagField(false,it)},enabled=enabled,
                        label={Text(stringResource(R.string.moderation_tags_remove))},minLines=2,modifier=Modifier.fillMaxWidth())
                    val error=s.tagError ?: tagValidation?.takeUnless { it==ModerationTagError.EMPTY }
                    error?.let { Text(stringResource(when(it) {
                        ModerationTagError.EMPTY -> R.string.moderation_tags_empty
                        ModerationTagError.TOO_LONG -> R.string.moderation_tags_length
                        ModerationTagError.OVERLAP -> R.string.moderation_tags_overlap
                    }),color=MaterialTheme.colorScheme.error) }
                } else OutlinedTextField(s.notes,vm::notes,enabled=enabled,label={Text(stringResource(R.string.moderation_notes))},
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
        s.processed?.takeIf { !s.busy }?.let { count -> item { Text(stringResource(R.string.moderation_summary,count,s.submitted),
            modifier=Modifier.semantics { liveRegion=LiveRegionMode.Polite }) } }
        if(s.needsReview) item {
            Text(stringResource(R.string.moderation_review_required),color=MaterialTheme.colorScheme.error)
            OutlinedButton(vm::refresh,enabled=enabled){Text(stringResource(R.string.refresh))}
        }
        item {
            Text(stringResource(R.string.moderation_selected,s.selected.size))
            OutlinedButton(vm::selectPage,enabled=enabled && !s.needsReview && s.loaded){Text(stringResource(R.string.moderation_select_page))}
            TextButton(vm::clearSelection,enabled=enabled && s.selected.isNotEmpty()){Text(stringResource(R.string.moderation_clear_selection))}
            if(flags) Button({action="resolve"},enabled=enabled && !s.needsReview && s.selected.isNotEmpty()) {
                Text(stringResource(R.string.moderation_resolve))
            } else if(tags) {
                Button({action="tags"},enabled=enabled && !s.needsReview && s.selected.isNotEmpty() && tagValidation==null) {
                    Text(stringResource(R.string.moderation_tags_apply))
                }
            } else {
                Button({action="verify"},enabled=enabled && !s.needsReview && s.selected.isNotEmpty()){Text(stringResource(R.string.moderation_verify))}
                OutlinedButton({action="unverify"},enabled=enabled && !s.needsReview && s.selected.isNotEmpty()){Text(stringResource(R.string.moderation_unverify))}
            }
        }
        if(s.loaded && !s.loading && s.error==null && (if(flags) s.flags.isEmpty() else s.questions.isEmpty())) item { com.mukhtabir.android.ui.components.EmptyState(stringResource(R.string.moderation_empty)) }
        if(flags) items(s.flags,key={it.id}) { flag ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(com.mukhtabir.android.ui.components.AppLayoutTokens.denseCardInsets),verticalArrangement=Arrangement.spacedBy(com.mukhtabir.android.ui.components.AppLayoutTokens.controlGap)) {
                    ModerationSelection(s.selected.contains(flag.id),enabled && !s.needsReview,
                        stringResource(R.string.moderation_report_id,flag.id),{vm.toggle(flag.id)})
                    Text(flag.questionText)
                    RatingSummary(s.ratings[flag.questionId])
                    Text(stringResource(R.string.moderation_reporter,flag.flaggerUsername))
                    Text(stringResource(R.string.moderation_author,flag.questionAuthor.ifBlank { stringResource(R.string.none) }))
                    flag.createdAt?.let { com.mukhtabir.android.ui.components.AppMetadataRow(listOf(it)) }
                    if(!flag.reason.isNullOrBlank()) Text(stringResource(R.string.moderation_reason,flag.reason.orEmpty()))
                    OutlinedButton({onOpenQuestion(flag.questionId)},enabled=!s.busy){Text(stringResource(R.string.question_details))}
                }
            }
        } else items(s.questions,key={it.id}) { question ->
            val display=question.localizedForDisplay()
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(com.mukhtabir.android.ui.components.AppLayoutTokens.denseCardInsets),verticalArrangement=Arrangement.spacedBy(com.mukhtabir.android.ui.components.AppLayoutTokens.controlGap)) {
                    ModerationSelection(question.id in s.selected,enabled && !s.needsReview,
                        stringResource(R.string.moderation_question_id,question.id),{vm.toggle(question.id)})
                    Text(stringResource(if(question.verified) R.string.verified else R.string.unverified))
                    RatingSummary(s.ratings[question.id])
                    if(tags) Text(stringResource(R.string.moderation_tags_current,question.tags.joinToString(" · ").ifBlank { stringResource(R.string.none) }))
                    ClinicalCaseContext(display.case)
                    MarkdownText(display.question)
                    if(!question.answersHidden && !question.verificationNotes.isNullOrBlank()) Text(stringResource(R.string.moderation_existing_notes,question.verificationNotes.orEmpty()))
                    OutlinedButton({onOpenQuestion(question.id)},enabled=!s.busy){Text(stringResource(R.string.question_details))}
                }
            }
        }
        item {
            com.mukhtabir.android.ui.components.AppPagination(s.page, s.totalPages,
                onPrevious = { vm.page(s.page - 1) }, onNext = { vm.page(s.page + 1) },
                previousEnabled = enabled, nextEnabled = enabled,
                statusLabel = stringResource(R.string.moderation_page, s.page, s.totalPages, s.total))
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
        else -> stringResource(R.string.moderation_rating_summary, rating.average, rating.count, if(rating.userRating in 1..5) "${rating.userRating}/5" else stringResource(R.string.none))
    }, style=MaterialTheme.typography.bodySmall, color=MaterialTheme.colorScheme.onSurfaceVariant)
}
