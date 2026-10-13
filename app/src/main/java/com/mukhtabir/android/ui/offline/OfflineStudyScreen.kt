@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.mukhtabir.android.ui.offline

import com.mukhtabir.android.ui.components.ExplanationCard
import com.mukhtabir.android.ui.theme.AppTheme

import com.mukhtabir.android.ui.components.AppPageHeading

import com.mukhtabir.android.ui.components.AppLoadingIndicator

import com.mukhtabir.android.ui.components.AppLayoutTokens

import com.mukhtabir.android.ui.components.AppDangerTextButton

import com.mukhtabir.android.ui.components.AppDangerOutlinedButton

import com.mukhtabir.android.ui.components.AppActionRow

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import com.mukhtabir.android.ui.components.AppConfirmationDialog as AlertDialog
import com.mukhtabir.android.ui.components.AppButton as Button
import com.mukhtabir.android.ui.components.AppCard as Card
import com.mukhtabir.android.ui.components.AppFilterChip as FilterChip
import androidx.compose.material3.MaterialTheme
import com.mukhtabir.android.ui.components.AppOutlinedButton as OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import com.mukhtabir.android.ui.components.AppTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mukhtabir.android.R
import com.mukhtabir.android.core.SessionDraftStore
import com.mukhtabir.android.data.remote.OfflineStudyService
import com.mukhtabir.android.data.remote.localizedForDisplay
import com.mukhtabir.android.ui.components.ClinicalCaseContext
import com.mukhtabir.android.ui.components.ErrorState
import com.mukhtabir.android.ui.components.MarkdownText
import com.mukhtabir.android.util.formatNumber

@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
fun OfflineStudyRoute(service: OfflineStudyService,store: SessionDraftStore,userId: Long,
    localOnly: Boolean,canSync: Boolean,maxQuestions: Int,initialIds: String = "",
    onBack: () -> Unit,onBusy: (Boolean) -> Unit = {}) {
    val vm: OfflineStudyViewModel=viewModel(key="offline-$userId-$localOnly-$canSync",
        factory=OfflineStudyViewModel.Factory(service,store,userId,localOnly,canSync,maxQuestions,initialIds))
    val s by vm.state.collectAsState()
    var deleting by remember {mutableStateOf<String?>(null)}
    var leaving by remember {mutableStateOf(false)}
    var confirmBank by remember {mutableStateOf(false)}
    val row=s.current
    val activeSelection = s.bank?.takeIf { !it.complete }?.selection ?: s.selection
    val focused = activeSelection != com.mukhtabir.android.data.remote.OfflineSelection()
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    var reviewToken by remember(s.selected) { mutableStateOf(0) }
    androidx.compose.runtime.LaunchedEffect(s.selected, row?.index, reviewToken) { listState.scrollToItem(0) }
    val q=row?.pack?.questions?.getOrNull(row.index)
    var readingLocale by androidx.compose.runtime.saveable.rememberSaveable(userId, row?.completionId) { mutableStateOf<String?>(null) }
    var confidence by remember(row?.completionId,q?.id) {mutableStateOf(3)}
    val back: () -> Unit={if(!s.busy) {if(s.selected!=null) vm.back() else if(s.pendingStorage) leaving=true else onBack()}}
    BackHandler(onBack=back)
    DisposableEffect(s.busy,s.pendingStorage) {onBusy(s.busy || s.pendingStorage);onDispose {onBusy(false)}}
    if(leaving) AlertDialog(onDismissRequest={leaving=false},title={Text(stringResource(R.string.confirm))},
        text={Text(stringResource(R.string.offline_leave_uncommitted))},
        confirmButton={TextButton({leaving=false;onBack()}) {Text(stringResource(R.string.confirm))}},
        dismissButton={TextButton({leaving=false}) {Text(stringResource(R.string.cancel))}})
    if(confirmBank) AlertDialog(onDismissRequest={confirmBank=false},
        title={Text(stringResource(if(focused) R.string.offline_scoped_download else R.string.offline_bank_download))},
        text={Text(stringResource(if(focused) R.string.offline_scoped_confirm else R.string.offline_bank_confirm))},
        confirmButton={TextButton({confirmBank=false;vm.downloadBank()},enabled=!s.busy && !s.storageFailed && s.bankAllowed) {Text(stringResource(R.string.confirm))}},
        dismissButton={TextButton({confirmBank=false}) {Text(stringResource(R.string.cancel))}})
    deleting?.let {id -> AlertDialog(onDismissRequest={deleting=null},
        title={Text(stringResource(R.string.delete))},text={Text(stringResource(R.string.offline_delete_confirm))},
        confirmButton={AppDangerTextButton({vm.delete(id);deleting=null},enabled=s.canDelete) {Text(stringResource(R.string.delete))}},
        dismissButton={TextButton({deleting=null}) {Text(stringResource(R.string.cancel))}})}
    LazyColumn(Modifier.fillMaxSize().padding(AppLayoutTokens.pageInsets),state=listState,verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
        item {AppPageHeading(stringResource(R.string.offline_title))}
        item {Text(stringResource(R.string.offline_hint))}
        item {OutlinedButton(back,enabled=!s.busy) {Text(stringResource(R.string.back))}}
        if(s.busy) item {AppLoadingIndicator()}
        s.error?.let {item {ErrorState(throwable=it)}}
        s.message?.let {item {Text(stringResource(it))}}
        s.validation?.let {item {Text(stringResource(it),color=MaterialTheme.colorScheme.error)}}
        if(s.storageFailed) item {
            com.mukhtabir.android.ui.components.AppRecoveryPanel(
                message=stringResource(R.string.offline_storage_error),onRetry=vm::retryStorage,retryEnabled=!s.busy)
        }
        if(row==null) {
            s.bank?.let {progress -> item {
                Text(stringResource(R.string.offline_bank_progress,progress.downloaded,progress.total,progress.excluded))
                Text(stringResource(if(progress.complete) R.string.offline_bank_completed else if(s.bankDownloading) R.string.offline_bank_running else R.string.offline_bank_paused))
                if(progress.complete && progress.downloaded+progress.excluded!=progress.total) Text(stringResource(R.string.offline_bank_changed))
                if(s.bankDownloading) OutlinedButton(vm::pauseBank) {Text(stringResource(R.string.offline_bank_pause))}
                else if(!localOnly && s.bankAllowed && !progress.complete) OutlinedButton(vm::downloadBank,enabled=!s.busy && !s.storageFailed) {
                    Text(stringResource(R.string.offline_bank_resume))
                }
                if(!progress.complete && !s.bankDownloading) OutlinedButton(vm::resetBank,
                    enabled=!s.busy && !s.pendingStorage && !s.storageFailed && s.syncingIds.isEmpty()) {
                    Text(stringResource(R.string.offline_reset_download))
                }
            } }
            if(!localOnly) item {
                if(!s.bankAllowed) Text(stringResource(R.string.offline_bank_permission_required))
                OutlinedButton(vm::refreshDownloadPermission,enabled=!s.busy && !s.pendingStorage) {
                    Text(stringResource(R.string.offline_bank_refresh_permission))
                }
            }
            if(!localOnly && s.bankAllowed) item {
                val selection = s.bank?.takeIf { !it.complete }?.selection ?: s.selection
                val editable = !s.busy && s.bank?.complete != false && s.syncingIds.isEmpty()
                s.filterError?.let { ErrorState(throwable=it, onRetry=if(s.busy) null else vm::refreshDownloadPermission) }
                s.filterOptions?.takeIf { s.filterError == null }?.let { options ->
                    com.mukhtabir.android.ui.components.AppSection(stringResource(R.string.offline_focused_download)) {
                        Text(stringResource(R.string.offline_filter_hint))
                        AppActionRow {
                            FilterChip(selection.book==null,{vm.selectBook(null)},enabled=editable,
                                label={Text(stringResource(R.string.quiz_no_book_restriction))})
                            options.books.forEach { book -> FilterChip(selection.book==book.name,{vm.selectBook(book.name)},enabled=editable,
                                label={Text(book.name)}) }
                        }
                        Text(stringResource(R.string.select_categories))
                        AppActionRow { options.categories.forEach { category -> FilterChip(category.id in selection.categories,
                            {vm.toggleCategory(category.id)},enabled=editable,label={Text(category.name)}) } }
                        Text(stringResource(R.string.select_tags))
                        AppActionRow { options.tags.forEach { tag -> FilterChip(tag.name in selection.tags,
                            {vm.toggleTag(tag.name)},enabled=editable,label={Text(tag.name)}) } }
                    }
                }
                Text(stringResource(if(focused) R.string.offline_scoped_hint else R.string.offline_bank_hint))
                Button({confirmBank=true},enabled=!s.busy && !s.storageFailed && s.syncingIds.isEmpty() && s.bank?.complete!=false) {
                    Text(stringResource(if(focused) R.string.offline_scoped_download else R.string.offline_bank_download))
                }
            }
            if(!localOnly && canSync) item {
                androidx.compose.foundation.layout.Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                    androidx.compose.material3.Checkbox(checked=s.bank?.takeIf { !it.complete }?.includeImages ?: s.includeImages,
                        onCheckedChange=vm::includeImages, enabled=!s.busy && s.bank?.complete!=false)
                    Text(stringResource(R.string.offline_include_images))
                }
                OutlinedTextField(s.ids,vm::ids,label={Text(stringResource(R.string.offline_ids))},enabled=!s.busy,modifier=Modifier.fillMaxWidth())
                Text(stringResource(R.string.offline_limits,maxQuestions.coerceAtMost(200)))
                Button(vm::download,enabled=!s.busy && !s.storageFailed && s.syncingIds.isEmpty()) {Text(stringResource(R.string.offline_download))}
            }
            if(s.materials.isEmpty() && !s.busy && !s.storageFailed && s.error==null) item {com.mukhtabir.android.ui.components.EmptyState(stringResource(R.string.offline_empty))}
            s.materials.forEach {material -> item(key=material.pack.id) {
                Card(Modifier.fillMaxWidth()) {Column(Modifier.padding(AppLayoutTokens.denseCardInsets),verticalArrangement=Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
                    Text(material.summary?.title ?: material.pack.questions.firstOrNull()?.localizedForDisplay()?.question.orEmpty().take(160))
                    com.mukhtabir.android.ui.components.AppMetadataRow(listOf(stringResource(R.string.offline_pack_count,
                        material.summary?.questionCount ?: material.pack.questions.size,
                        material.summary?.answeredCount ?: material.answers.size)))
                    com.mukhtabir.android.ui.components.AppSaveStatus(stringResource(
                        if(material.pack.id in s.syncingIds) R.string.quiz_syncing
                        else if(s.syncErrors[material.pack.id]!=null) R.string.quiz_sync_failed
                        else if(material.summary?.synced==true || material.result!=null) R.string.offline_synced
                        else if(material.summary?.finished==true || material.frozenTime!=null) R.string.offline_ready else R.string.offline_local),
                        failed=s.syncErrors[material.pack.id]!=null)
                    OutlinedButton({vm.open(material.pack.id)},enabled=!s.busy) {Text(stringResource(R.string.offline_open))}
                    s.syncErrors[material.pack.id]?.let { com.mukhtabir.android.ui.components.AppRecoveryPanel(throwable=it) }
                    AppDangerOutlinedButton({deleting=material.pack.id},enabled=s.canDelete && material.pack.id !in s.syncingIds) {Text(stringResource(R.string.delete))}
                } }
            } }
        } else {
            if(row.frozenTime!=null || row.pack.id in s.syncingIds || s.syncErrors[row.pack.id]!=null) item {
                com.mukhtabir.android.ui.components.AppSaveStatus(stringResource(when {
                    row.pack.id in s.syncingIds -> R.string.quiz_syncing
                    s.syncErrors[row.pack.id]!=null -> R.string.quiz_sync_failed
                    row.result!=null -> R.string.offline_synced
                    else -> R.string.offline_ready
                }),failed=s.syncErrors[row.pack.id]!=null)
            }
            s.syncErrors[row.pack.id]?.let { item { com.mukhtabir.android.ui.components.AppRecoveryPanel(throwable=it) } }
            if(!localOnly && canSync) item {
                OutlinedButton(vm::refreshPack,enabled=!s.busy && !s.storageFailed && s.syncingIds.isEmpty()) {
                    Text(stringResource(R.string.offline_check_updates))
                }
            }
            if(row.completionId==null) item {Button(vm::start,enabled=!s.busy && !s.storageFailed) {Text(stringResource(R.string.offline_start))}}
            if(q!=null && row.completionId!=null) {
                val display=q.localizedForDisplay(readingLocale ?: java.util.Locale.getDefault().toLanguageTag())
                val saved=row.answers.firstOrNull {it.questionId==q.id}
                item {Text(stringResource(R.string.offline_question_position,row.index+1,row.pack.questions.size))}
                item { com.mukhtabir.android.ui.components.QuestionLanguageSelector(q.translations, q.choices.size,
                    readingLocale, { readingLocale = it }, hasCase = q.case?.stem?.isNotBlank() == true, caseTranslations = q.case?.translations ?: kotlinx.serialization.json.JsonObject(emptyMap()),
                    explanationAvailable = saved != null && !q.explanation.isNullOrBlank(),
                    explanationTranslated = !display.explanation.isNullOrBlank()) }
                item {ClinicalCaseContext(display.case)}
                item {com.mukhtabir.android.ui.components.QuestionTextCard(display.question)}
                row.pack.images[q.id.toString()]?.let {encoded -> item {
                    val bitmap=remember(encoded) {runCatching {
                        val bytes=Base64.decode(encoded,Base64.DEFAULT)
                        val bounds=BitmapFactory.Options().apply {inJustDecodeBounds=true}
                        BitmapFactory.decodeByteArray(bytes,0,bytes.size,bounds)
                        var sample=1
                        while(bounds.outWidth/sample>1600 || bounds.outHeight/sample>1600) sample*=2
                        BitmapFactory.decodeByteArray(bytes,0,bytes.size,BitmapFactory.Options().apply {inSampleSize=sample})
                    }.getOrNull()}
                    if(bitmap!=null) Image(bitmap.asImageBitmap(),contentDescription=stringResource(R.string.offline_image),modifier=Modifier.fillMaxWidth())
                    else Text(stringResource(R.string.offline_image_error))
                } }
                if(q.id in row.pack.imageQuestionIds && row.pack.images[q.id.toString()]==null) item {
                    Text(stringResource(R.string.offline_image_omitted),style=MaterialTheme.typography.bodySmall)
                }
                if(saved==null && row.frozenTime==null) item {
                    androidx.compose.runtime.key(q.id) {
                        com.mukhtabir.android.ui.components.AppSection(stringResource(R.string.confidence_prompt),
                            subtitle = stringResource(when (confidence) {
                                1 -> R.string.confidence_guessing
                                2 -> R.string.confidence_uncertain
                                else -> R.string.confidence_certain
                            })) {
                            Text(stringResource(R.string.offline_answer_hint))
                            AppActionRow() {(1..3).forEach {level ->
                                FilterChip(confidence==level,{confidence=level},enabled=!s.busy && !s.storageFailed,
                                    label={Text(stringResource(when(level) {1 -> R.string.master_confidence_low;2 -> R.string.master_confidence_medium;else -> R.string.master_confidence_high}))})
                            }}
                        }
                    }
                }
                display.choices.forEachIndexed {index,choice -> item(key="choice-${q.id}-$index") {
                    com.mukhtabir.android.ui.components.AnswerChoice(number=index+1,text=choice,
                        selected=saved?.answer==index+1, onClick={vm.answer(index+1,confidence)},
                        answerCorrect=if(saved==null || q.correctAnswer==null) null else when(index+1) {
                            q.correctAnswer -> true
                            saved.answer -> false
                            else -> null
                        },
                        enabled=saved==null && row.frozenTime==null && !s.busy && !s.storageFailed)
                } }
                if(saved!=null) item {
                    display.explanation?.takeIf {it.isNotBlank()}?.let {ExplanationCard(it)}
                }
                item {AppActionRow() {
                    OutlinedButton({vm.go(-1)},enabled=row.index>0 && !s.busy && !s.storageFailed) {Text(stringResource(R.string.previous))}
                    OutlinedButton({vm.go(1)},enabled=row.index<row.pack.questions.lastIndex && !s.busy && !s.storageFailed) {Text(stringResource(R.string.next))}
                }}
            }
            if(row.frozenTime==null && row.answers.size==row.pack.questions.size) item {
                Button(vm::finish,enabled=!s.busy && !s.storageFailed) {Text(stringResource(R.string.offline_finish))}
            }
            if(row.frozenTime!=null) item {
                val correct=row.answers.count {answer -> row.pack.questions.any {it.id==answer.questionId && it.correctAnswer==answer.answer}}
                com.mukhtabir.android.ui.components.AppMetricTile(
                    com.mukhtabir.android.util.bidi("${formatNumber(correct)} / ${formatNumber(row.pack.questions.size)}"),
                    stringResource(R.string.offline_local_result),Modifier.fillMaxWidth())
                Text(stringResource(R.string.offline_review_hint))
                OutlinedButton({ vm.review(); reviewToken += 1 },enabled=!s.busy && !s.storageFailed) {Text(stringResource(R.string.offline_review))}
                if(!localOnly && canSync && row.result==null) {
                    Text(stringResource(R.string.offline_sync_hint))
                    Button(vm::sync,enabled=!s.busy && !s.storageFailed && row.pack.id !in s.syncingIds) {Text(stringResource(R.string.offline_sync))}
                }
                if(row.result!=null) OutlinedButton(vm::restart,enabled=!s.busy && !s.storageFailed) {Text(stringResource(R.string.offline_again))}
            }
            item {AppDangerOutlinedButton({deleting=row.pack.id},enabled=s.canDelete && row.pack.id !in s.syncingIds) {Text(stringResource(R.string.delete))}}
        }
    }
}
