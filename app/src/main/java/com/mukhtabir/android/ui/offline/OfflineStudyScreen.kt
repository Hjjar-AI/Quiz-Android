@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.mukhtabir.android.ui.offline

import com.mukhtabir.android.ui.components.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.material3.Scaffold
import com.mukhtabir.android.ui.theme.AppTheme

import com.mukhtabir.android.ui.components.AppPageHeading

import com.mukhtabir.android.ui.components.AppLoadingIndicator

import com.mukhtabir.android.ui.components.AppLayoutTokens

import com.mukhtabir.android.ui.components.AppDangerTextButton

import com.mukhtabir.android.ui.components.AppDangerOutlinedButton

import com.mukhtabir.android.ui.components.AppActionRow

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.Icon
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mukhtabir.android.R
import com.mukhtabir.android.core.SessionDraftStore
import com.mukhtabir.android.data.remote.OfflineStudyService
import com.mukhtabir.android.data.remote.localizedForDisplay
import com.mukhtabir.android.data.remote.summaryOnly
import com.mukhtabir.android.ui.components.ClinicalCaseContext
import com.mukhtabir.android.ui.components.ErrorState

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
    var renameOpen by remember(s.selected) { mutableStateOf(false) }
    var collectionTitle by remember(s.selected) { mutableStateOf("") }
    var renameSubmitted by remember(s.selected) { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(row?.displayTitle, s.busy, s.storageFailed, s.pendingStorage) {
        if (renameSubmitted && !s.busy) {
            if (!s.storageFailed && !s.pendingStorage && row?.displayTitle == collectionTitle.trim()) renameOpen = false
            if (s.storageFailed || s.pendingStorage || row?.displayTitle == collectionTitle.trim()) renameSubmitted = false
        }
    }
    if (renameOpen && row != null) AlertDialog(onDismissRequest = { if (!s.busy && !renameSubmitted) renameOpen = false },
        title = { Text(stringResource(R.string.flow_rename)) },
        text = { Column {
            OutlinedTextField(collectionTitle, { collectionTitle = it.take(120) }, label = { Text(stringResource(R.string.flow_local_title)) }, enabled = !s.busy)
            if (s.storageFailed || s.pendingStorage) {
                Text(stringResource(R.string.offline_storage_error), color = MaterialTheme.colorScheme.error)
                TextButton(vm::retryStorage, enabled = !s.busy) { Text(stringResource(R.string.retry)) }
            }
        } },
        confirmButton = { TextButton({ renameSubmitted = vm.rename(collectionTitle) }, enabled = !renameSubmitted && collectionTitle.isNotBlank() && !s.busy && !s.storageFailed && !s.pendingStorage && row.pack.id !in s.syncingIds) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton({ renameOpen = false }, enabled = !s.busy && !renameSubmitted) { Text(stringResource(R.string.cancel)) } })
    val activeSelection = s.bank?.takeIf { !it.complete }?.selection ?: s.selection
    val focused = activeSelection != com.mukhtabir.android.data.remote.OfflineSelection()
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    var reviewToken by remember(s.selected) { mutableStateOf(0) }
    var reviewing by rememberSaveable(s.selected, row?.completionId) { mutableStateOf(false) }
    var reviewFilter by rememberSaveable(s.selected, row?.completionId) { mutableStateOf(ReviewFilter.ALL) }
    var confirmFinish by remember(s.selected) { mutableStateOf(false) }
    var showMap by remember(s.selected) { mutableStateOf(false) }
    if (showMap && row != null) AlertDialog(onDismissRequest = { showMap = false },
        title = { Text(stringResource(R.string.session_question_navigation)) },
        text = { LearningQuestionMap(row.index, row.pack.questions.size,
            row.pack.questions.indices.filter { i -> row.answers.any { it.questionId == row.pack.questions[i].id } }.toSet(),
            row.flaggedIndices, !s.busy && !s.storageFailed) { target ->
                reviewFilter = ReviewFilter.ALL; vm.go(target - row.index); showMap = false
            } }, confirmButton = { TextButton({ showMap = false }) { Text(stringResource(R.string.done)) } })
    val visibleReview = row?.let { material -> material.pack.questions.indices.filter { i ->
        val question = material.pack.questions[i]
        val saved = material.answers.firstOrNull { it.questionId == question.id }
        if (reviewFilter == ReviewFilter.INCORRECT && question.correctAnswer == null) false
        else reviewFilter.includes(saved != null, saved?.answer == question.correctAnswer)
    } }.orEmpty()
    val shownIndex = if (row?.frozenTime != null && reviewing) row.index.takeIf { it in visibleReview } ?: visibleReview.firstOrNull()
        else row?.index
    val q = shownIndex?.let { row?.pack?.questions?.getOrNull(it) }
    androidx.compose.runtime.LaunchedEffect(s.selected, shownIndex, reviewToken, reviewing, reviewFilter) { listState.scrollToItem(0) }
    val showQuestion = row?.completionId != null && (row.frozenTime == null || reviewing)
    if (confirmFinish) AlertDialog(onDismissRequest = { if (!s.busy) confirmFinish = false },
        title = { Text(stringResource(R.string.flow_final_review)) }, text = { Column {
            Text(stringResource(R.string.ui_finish_offline))
            if (row != null) LearningQuestionMap(row.index, row.pack.questions.size,
                row.pack.questions.indices.filter { i -> row.answers.any { it.questionId == row.pack.questions[i].id } }.toSet(),
                row.flaggedIndices, !s.busy && !s.storageFailed) { target -> vm.go(target - row.index); confirmFinish = false }
        } },
        confirmButton = { TextButton({ confirmFinish = false; vm.finish() }, enabled = !s.busy && !s.storageFailed) { Text(stringResource(R.string.finish)) } },
        dismissButton = { TextButton({ confirmFinish = false }, enabled = !s.busy) { Text(stringResource(R.string.cancel)) } })
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
        title={Text(stringResource(R.string.ux_delete_download))},text={Text(stringResource(R.string.offline_delete_confirm))},
        confirmButton={AppDangerTextButton({vm.delete(id);deleting=null},enabled=s.canDelete) {Text(stringResource(R.string.ux_delete_download))}},
        dismissButton={TextButton({deleting=null}) {Text(stringResource(R.string.cancel))}})}
    Scaffold(topBar = {
        if (showQuestion && q != null && row != null) LearningSessionHeader(shownIndex, row.pack.questions.size, row.answers.size,
            tools = {
                if (row.frozenTime == null) AppIconButton({ vm.toggleFlag(shownIndex) }, enabled = !s.busy && !s.storageFailed && !s.pendingStorage) {
                    Icon(Icons.Filled.Flag, stringResource(if (shownIndex in row.flaggedIndices) R.string.flow_unflag else R.string.flow_flag))
                }
                if (row.frozenTime == null && row.answers.size == row.pack.questions.size) TextButton({ confirmFinish = true },
                enabled = !s.busy && !s.storageFailed) { Text(stringResource(R.string.finish)) } })
    }, bottomBar = {
        if (showQuestion && q != null && row != null) {
            val i = shownIndex
            val position = visibleReview.indexOf(i)
            val last = if (row.frozenTime != null) position == visibleReview.lastIndex else i == row.pack.questions.lastIndex
            LearningSessionNavigation(onQuestions = { showMap = true },
                onPrevious = { vm.go((if (row.frozenTime != null) visibleReview[position - 1] else i - 1) - row.index) },
                previousEnabled = !s.busy && !s.storageFailed && if (row.frozenTime != null) position > 0 else i > 0,
                nextEnabled = !s.busy && !s.storageFailed && (!last || row.frozenTime != null || row.answers.size == row.pack.questions.size),
                nextLabel = stringResource(if (!last) R.string.next else if (row.frozenTime != null) R.string.results else R.string.finish),
                onNext = { if (last) { if (row.frozenTime != null) reviewing = false else confirmFinish = true }
                    else vm.go((if (row.frozenTime != null) visibleReview[position + 1] else i + 1) - row.index) })
        }
    }) { insets ->
    LazyColumn(Modifier.fillMaxSize().padding(insets).consumeWindowInsets(insets).padding(AppLayoutTokens.pageInsets),state=listState,verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
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
                        Text(stringResource(R.string.quiz_filter_matching), style = MaterialTheme.typography.bodySmall)
                        AppActionRow {
                            FilterChip(selection.book==null,{vm.selectBook(null)},enabled=editable,
                                label={Text(stringResource(R.string.quiz_no_book_restriction))})
                            options.books.forEach { book -> FilterChip(selection.book==book.name,{vm.selectBook(book.name)},enabled=editable,
                                label={Text(book.name)}) }
                        }
                        Text(stringResource(R.string.select_categories))
                        AppActionRow { options.categories.forEach { category -> FilterChip(category.id in selection.categories,
                            {vm.toggleCategory(category.id)},enabled=editable,label={Text(category.name)}) } }
                        Text(stringResource(R.string.ux_tags_any))
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
                    val summary = material.summary ?: material.summaryOnly().summary
                    Text(summary?.title?.takeIf(String::isNotBlank) ?: stringResource(R.string.flow_collection), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.flow_offline_ready), style = MaterialTheme.typography.labelMedium)
                    if (summary?.sources.isNullOrEmpty()) Text(stringResource(R.string.flow_source_unknown), style = MaterialTheme.typography.bodySmall)
                    else AppMetadataRow(summary?.sources.orEmpty())
                    Text(summary?.lastStudiedAt?.let { stringResource(R.string.flow_last_studied,
                        com.mukhtabir.android.util.formatLocalDateTime(it)) } ?: stringResource(if ((summary?.answeredCount ?: material.answers.size) > 0 || material.completionId != null)
                        R.string.flow_studied_date_unknown else R.string.flow_not_studied), style = MaterialTheme.typography.bodySmall)
                    val total = summary?.questionCount ?: material.pack.questions.size
                    val done = summary?.answeredCount ?: material.answers.size
                    androidx.compose.material3.LinearProgressIndicator(if (total > 0) (done.toFloat() / total).coerceIn(0f,1f) else 0f, Modifier.fillMaxWidth())
                    com.mukhtabir.android.ui.components.AppMetadataRow(listOf(stringResource(R.string.offline_pack_count,
                        material.summary?.questionCount ?: material.pack.questions.size,
                        material.summary?.answeredCount ?: material.answers.size)))
                    LearningSaveStatus(when {
                        material.pack.id in s.syncingIds -> LearningSaveState.SYNCING
                        s.syncErrors[material.pack.id] != null -> LearningSaveState.REVIEW
                        material.summary?.synced == true || material.result != null -> LearningSaveState.SERVER
                        material.summary?.finished == true || material.frozenTime != null -> LearningSaveState.WAITING
                        else -> LearningSaveState.DEVICE
                    }, detail = stringResource(
                        if(material.pack.id in s.syncingIds) R.string.quiz_syncing
                        else if(s.syncErrors[material.pack.id]!=null) R.string.quiz_sync_failed
                        else if(material.summary?.synced==true || material.result!=null) R.string.offline_synced
                        else if(material.summary?.finished==true || material.frozenTime!=null) R.string.offline_ready else R.string.offline_local),
                        )
                    OutlinedButton({vm.open(material.pack.id)},enabled=!s.busy) {Text(stringResource(R.string.offline_open))}
                    s.syncErrors[material.pack.id]?.let { com.mukhtabir.android.ui.components.AppRecoveryPanel(throwable=it) }
                    AppDangerOutlinedButton({deleting=material.pack.id},enabled=s.canDelete && material.pack.id !in s.syncingIds) {Text(stringResource(R.string.ux_delete_download))}
                } }
            } }
        } else {
            item {
                Text(row.displayTitle?.takeIf(String::isNotBlank) ?: row.summaryOnly().summary?.title?.takeIf(String::isNotBlank)
                    ?: stringResource(R.string.flow_collection), style = MaterialTheme.typography.titleLarge)
                AppMetadataRow(row.summaryOnly().summary?.sources.orEmpty())
                TextButton({ collectionTitle = row.displayTitle.orEmpty(); renameOpen = true },
                    enabled = !s.busy && !s.storageFailed && !s.pendingStorage && row.pack.id !in s.syncingIds) { Text(stringResource(R.string.flow_rename)) }
            }
            if(row.frozenTime!=null || row.pack.id in s.syncingIds || s.syncErrors[row.pack.id]!=null) item {
                LearningSaveStatus(when {
                    row.pack.id in s.syncingIds -> LearningSaveState.SYNCING
                    s.syncErrors[row.pack.id] != null -> LearningSaveState.REVIEW
                    row.result != null -> LearningSaveState.SERVER
                    else -> LearningSaveState.WAITING
                }, detail = stringResource(when {
                    row.pack.id in s.syncingIds -> R.string.quiz_syncing
                    s.syncErrors[row.pack.id]!=null -> R.string.quiz_sync_failed
                    row.result!=null -> R.string.offline_synced
                    else -> R.string.offline_ready
                }))
            }
            s.syncErrors[row.pack.id]?.let { item { com.mukhtabir.android.ui.components.AppRecoveryPanel(throwable=it) } }
            if(!localOnly && canSync) item {
                OutlinedButton(vm::refreshPack,enabled=!s.busy && !s.storageFailed && s.syncingIds.isEmpty()) {
                    Text(stringResource(R.string.offline_check_updates))
                }
            }
            if(row.completionId==null) item {Button(vm::start,enabled=!s.busy && !s.storageFailed) {Text(stringResource(R.string.offline_start))}}
            if (row.frozenTime != null && reviewing) item {
                ResultReviewFilters(reviewFilter, { reviewFilter = it }, visibleReview.size, row.pack.questions.size)
                OutlinedButton({ reviewing = false }) { Text(stringResource(R.string.results)) }
            }
            if(q!=null && showQuestion) {
                val display=q.localizedForDisplay(readingLocale ?: java.util.Locale.getDefault().toLanguageTag())
                val saved=row.answers.firstOrNull {it.questionId==q.id}
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
                        enabled=saved==null && row.frozenTime==null && !s.busy && !s.storageFailed,
                        interactiveText=row.frozenTime!=null)
                } }
                item { BilingualQuestionComparison(q, choicesVisible = true, explanationVisible = saved != null || row.frozenTime != null) }
                if(saved!=null) item {
                    display.explanation?.takeIf {it.isNotBlank()}?.let {ExplanationCard(it, title=stringResource(R.string.explanation))}
                }
            }
            if(row.frozenTime!=null && !reviewing) item {
                val correct=row.answers.count {answer -> row.pack.questions.any {it.id==answer.questionId && it.correctAnswer==answer.answer}}
                LearningResultsSummary(row.result?.correctCount ?: correct, row.result?.totalQuestions ?: row.pack.questions.size,
                    row.result?.answeredCount ?: row.answers.size, row.result?.accuracy)
                if (row.pack.questions.any { it.correctAnswer == null }) Text(stringResource(R.string.ui_review_unknown))
                Text(stringResource(R.string.offline_review_hint))
                OutlinedButton({ reviewing = true; reviewFilter = ReviewFilter.ALL; vm.review(); reviewToken += 1 },enabled=!s.busy && !s.storageFailed) {Text(stringResource(R.string.offline_review))}
                if(!localOnly && canSync && row.result==null) {
                    Text(stringResource(R.string.offline_sync_hint))
                    Button(vm::sync,enabled=!s.busy && !s.storageFailed && row.pack.id !in s.syncingIds) {Text(stringResource(R.string.offline_sync))}
                }
                if(row.result!=null) OutlinedButton(vm::restart,enabled=!s.busy && !s.storageFailed) {Text(stringResource(R.string.offline_again))}
            }
            item {AppDangerOutlinedButton({deleting=row.pack.id},enabled=s.canDelete && row.pack.id !in s.syncingIds) {Text(stringResource(R.string.ux_delete_download))}}
        }
    }
    }
}
