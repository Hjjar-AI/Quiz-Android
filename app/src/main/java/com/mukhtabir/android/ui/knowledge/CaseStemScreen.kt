package com.mukhtabir.android.ui.knowledge

import com.mukhtabir.android.ui.components.editorRecoveryBlocked
import com.mukhtabir.android.ui.components.EditorRecoveryControls
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.QuestionService
import com.mukhtabir.android.ui.components.AppConfirmationDialog
import com.mukhtabir.android.ui.components.AppDangerTextButton
import com.mukhtabir.android.ui.components.AppLayoutTokens
import com.mukhtabir.android.ui.components.AppLoadingIndicator
import com.mukhtabir.android.ui.components.AppOutlinedButton
import com.mukhtabir.android.ui.components.AppPageHeading
import com.mukhtabir.android.ui.components.AppStatus
import com.mukhtabir.android.ui.components.AppStatusKind
import com.mukhtabir.android.ui.components.AppTextButton
import com.mukhtabir.android.ui.components.ErrorState
import com.mukhtabir.android.ui.components.MarkdownField
import com.mukhtabir.android.ui.components.MarkdownText
import com.mukhtabir.android.ui.components.RefreshOnResume
import com.mukhtabir.android.ui.components.collectAsLifecycleState
import com.mukhtabir.android.ui.theme.AppTheme

@Composable
fun CaseStemRoute(key: String,userId: Long,service: QuestionService,canEditAny: Boolean,canEditOwn: Boolean, store: com.mukhtabir.android.core.SessionDraftStore,
    onBack: () -> Unit,onChanged: () -> Unit,onBusyChanged: (Boolean) -> Unit) {
    val vm: CaseStemViewModel=viewModel(key="case-stem-$userId-$key-$canEditAny-$canEditOwn",
        factory=CaseStemViewModel.Factory(key,userId,service,canEditAny,canEditOwn,store))
    vm.permissions(canEditAny,canEditOwn)
    val s by vm.state.collectAsLifecycleState()
    val recoveryBlocked=editorRecoveryBlocked(vm.recovery)
    var leaving by rememberSaveable { mutableStateOf(false) }
    var confirming by rememberSaveable { mutableStateOf(false) }
    val back={if(s.dirty) leaving=true else onBack()}
    BackHandler { if(!s.busy) back() }
    DisposableEffect(s.busy) { onBusyChanged(s.busy);onDispose { onBusyChanged(false) } }
    LaunchedEffect(s.changed) { if(s.changed) { onChanged();vm.consumeChange() } }
    RefreshOnResume(vm::refresh)
    val permitted=vm.mayEdit(s.data)
    val enabled=!recoveryBlocked && permitted && !s.busy && !s.loading
    if(leaving) AppConfirmationDialog(onDismissRequest={leaving=false},
        title={Text(stringResource(R.string.unsaved_question_title))},text={Text(stringResource(R.string.case_stem_leave))},
        confirmButton={AppDangerTextButton({leaving=false;onBack()},enabled=!s.busy){Text(stringResource(R.string.ux_discard_case_changes))}},
        dismissButton={AppTextButton({leaving=false}){Text(stringResource(R.string.keep_editing))}})
    if(confirming) AppConfirmationDialog(onDismissRequest={confirming=false},
        title={Text(stringResource(R.string.case_stem_save_title))},
        text={Text(stringResource(if(s.draft.isBlank()) R.string.case_stem_clear_confirm else R.string.case_stem_save_confirm,key))},
        confirmButton={AppTextButton({confirming=false;vm.save()},enabled=enabled && s.dirty && !s.conflict && !s.needsReview) {
            Text(stringResource(R.string.confirm))
        }},dismissButton={AppTextButton({confirming=false}){Text(stringResource(R.string.cancel))}})
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(AppLayoutTokens.pageInsets),verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
        EditorRecoveryControls(vm.recovery,s.busy || s.loading,vm::restore)
        AppPageHeading(stringResource(R.string.case_stem_edit))
        Text(key)
        AppTextButton(back,enabled=!s.busy){Text(stringResource(R.string.back))}
        Text(stringResource(R.string.case_stem_scope_hint))
        AppOutlinedButton(vm::refresh,enabled=!s.busy && !s.loading){Text(stringResource(R.string.refresh))}
        if(s.loading || s.busy) AppLoadingIndicator()
        s.error?.let { ErrorState(throwable=it,onRetry=if(!s.busy && !s.loading) vm::refresh else null) }
        s.actionError?.let { ErrorState(throwable=it) }
        s.validation?.let { AppStatus(stringResource(it)) }
        s.savedCount?.let { Text(stringResource(R.string.case_stem_saved, com.mukhtabir.android.util.formatNumber(it)),modifier=Modifier.semantics { liveRegion=LiveRegionMode.Polite }) }
        if(s.needsReview) AppStatus(stringResource(R.string.case_stem_review_required), kind = AppStatusKind.WARNING)
        if((s.loaded && !permitted) || (!canEditAny && !canEditOwn)) AppStatus(stringResource(R.string.error_forbidden))
        if(s.loaded) {
            if(s.conflict) {
                ContentHeading(stringResource(R.string.case_stem_conflict))
                Text(stringResource(R.string.case_stem_server))
                if(s.data?.stem.isNullOrBlank()) Text(stringResource(R.string.none)) else MarkdownText(s.data?.stem.orEmpty())
                AppOutlinedButton({vm.resolve(false)},enabled=enabled && !s.needsReview){Text(stringResource(R.string.case_stem_use_server))}
                AppOutlinedButton({vm.resolve(true)},enabled=enabled && !s.needsReview){Text(stringResource(R.string.case_stem_keep_draft))}
            }
            MarkdownField(s.draft,vm::field,enabled=enabled,label=stringResource(R.string.editor_case_stem),
                minLines=4,modifier=Modifier.fillMaxWidth())
            Text(stringResource(R.string.case_stem_clear_hint))
            if(s.draft.isNotBlank()) MarkdownText(s.draft)
            AppOutlinedButton({confirming=true},enabled=enabled && s.dirty && !s.conflict && !s.needsReview) {
                Text(stringResource(R.string.case_stem_save_title))
            }
        }
    }
}
