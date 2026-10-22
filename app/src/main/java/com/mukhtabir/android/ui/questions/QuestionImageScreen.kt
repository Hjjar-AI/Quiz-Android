package com.mukhtabir.android.ui.questions

import com.mukhtabir.android.ui.components.AppSectionHeading
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.ImageRepository
import com.mukhtabir.android.data.remote.QuestionService
import com.mukhtabir.android.ui.components.AppConfirmationDialog
import com.mukhtabir.android.ui.components.AppDangerTextButton
import com.mukhtabir.android.ui.components.AppLayoutTokens
import com.mukhtabir.android.ui.components.AppLoadingIndicator
import com.mukhtabir.android.ui.components.AppOutlinedButton
import com.mukhtabir.android.ui.components.AppPageHeading
import com.mukhtabir.android.ui.components.AppStatus
import com.mukhtabir.android.ui.components.AppTextButton
import com.mukhtabir.android.ui.components.ErrorState
import com.mukhtabir.android.ui.components.QuestionImage
import com.mukhtabir.android.ui.components.collectAsLifecycleState
import com.mukhtabir.android.ui.theme.AppTheme

@Composable
fun QuestionImageRoute(questionId: Long, userId: Long, questions: QuestionService, images: ImageRepository,
    canEditAny: Boolean, canUploadOwn: Boolean, draftStore: com.mukhtabir.android.core.SessionDraftStore, onBack: () -> Unit, onChanged: () -> Unit, onBusyChanged: (Boolean) -> Unit) {
    val context=LocalContext.current
    val vm: QuestionImageViewModel=viewModel(key="question-image-$userId-$questionId",
        factory=QuestionImageViewModel.Factory(context,questionId,questions,userId,canEditAny,canUploadOwn,draftStore))
    val s by vm.state.collectAsLifecycleState()
    val recovery by vm.recovery.collectAsLifecycleState()
    var cameraFile by rememberSaveable { mutableStateOf<String?>(null) }
    var cameraResult by rememberSaveable { mutableStateOf<String?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        if (saved) cameraResult = cameraFile else {
            cameraFile?.let { java.io.File(it).delete() }; cameraFile = null
        }
    }
    LaunchedEffect(cameraResult, s.question, s.busy, recovery) {
        val path = cameraResult
        if (path != null && s.question != null && !s.busy && recovery == null) {
            val file = java.io.File(path)
            val directory = java.io.File(context.cacheDir, "question-camera")
            if (file.canonicalFile.parentFile == directory.canonicalFile && file.isFile) {
                val uri = androidx.core.content.FileProvider.getUriForFile(context, context.packageName + ".camera", file)
                cameraResult = null
                vm.select(uri) { file.delete(); cameraFile = null }
            } else { cameraResult = null; cameraFile = null }
        }
    }
    if (recovery != null && s.question != null) AppConfirmationDialog(onDismissRequest = {}, title = { Text(stringResource(R.string.author_recover_title)) },
        text = { Text(stringResource(R.string.author_recover_body)) },
        confirmButton = { AppTextButton(vm::restoreDraft, enabled = !s.busy && !s.denied) { Text(stringResource(R.string.author_restore)) } },
        dismissButton = { AppDangerTextButton(vm::discardRecovery, enabled = !s.busy) { Text(stringResource(R.string.ux_discard_local_recovery)) } })
    var action by rememberSaveable { mutableStateOf<String?>(null) }
    var leave by rememberSaveable { mutableStateOf(false) }
    val back={ if(s.selected!=null) leave=true else onBack() }
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { vm.select(it) } }
    DisposableEffect(s.busy) {
        onBusyChanged(s.busy)
        onDispose { onBusyChanged(false) }
    }
    var discarding by androidx.compose.runtime.remember { mutableStateOf(false) }
    LaunchedEffect(discarding, s.busy, s.selected) { if (discarding && !s.busy && s.selected == null) { discarding = false; onBack() } }
    LaunchedEffect(s.changed) { if(s.changed) { vm.consumeChange();onChanged() } }
    BackHandler { if(!s.busy) back() }
    if(leave) AppConfirmationDialog(
        onDismissRequest={leave=false},title={Text(stringResource(R.string.unsaved_question_title))},
        text={Text(stringResource(R.string.question_image_leave))},
        confirmButton={Column {
            AppTextButton({leave=false;vm.keepDraft(onBack)},enabled=!s.busy){Text(stringResource(R.string.author_save_later))}
            AppDangerTextButton({leave=false;discarding=true;vm.clearSelection()},enabled=!s.busy){Text(stringResource(R.string.ux_discard_image_changes))}
        }},
        dismissButton={AppTextButton({leave=false}){Text(stringResource(R.string.keep_editing))}},
    )
    if(action!=null) AppConfirmationDialog(
        onDismissRequest={action=null},title={Text(stringResource(R.string.question_manage_image))},
        text={Text(stringResource(if(action=="remove") R.string.question_image_remove_confirm else R.string.question_image_upload_confirm))},
        confirmButton={AppTextButton({if(action=="remove") vm.remove() else vm.upload();action=null},enabled=!s.busy && !s.denied && !s.needsReview){Text(stringResource(R.string.confirm))}},
        dismissButton={AppTextButton({action=null}){Text(stringResource(R.string.cancel))}},
    )
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(AppLayoutTokens.pageInsets),verticalArrangement=Arrangement.spacedBy(AppTheme.spacing.section)) {
        AppPageHeading(stringResource(R.string.question_manage_image))
        AppTextButton(back,enabled=!s.busy){Text(stringResource(R.string.cancel))}
        Text(stringResource(R.string.question_image_policy))
        if(s.busy) AppLoadingIndicator()
        s.storageError?.let { Text(stringResource(R.string.author_storage_failed)); if (s.error !== it) ErrorState(throwable = it) }
        s.error?.let { ErrorState(throwable=it,onRetry=if(!s.busy && (s.question==null || s.needsReview)) vm::refresh else null) }
        if(s.needsReview) {
            Text(stringResource(R.string.question_image_review_required))
            AppOutlinedButton(vm::refresh,enabled=!s.busy){Text(stringResource(R.string.question_image_refresh))}
        }
        if(s.denied) AppStatus(stringResource(R.string.error_forbidden))
        s.question?.let { q ->
            AppSectionHeading(stringResource(R.string.question_image_current))
            if(q.imageUrl.isNullOrBlank()) Text(stringResource(R.string.none)) else key(s.imageRevision) { QuestionImage(q.imageUrl,images) }
            if(s.success) Text(stringResource(R.string.question_image_saved), modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            val enabled=!s.busy && !s.denied
            AppOutlinedButton({
                try { picker.launch(arrayOf("image/jpeg","image/png","image/gif","image/webp")) }
                catch(failure: Exception) { vm.selectionError(failure) }
            },enabled=enabled){Text(stringResource(R.string.question_image_choose))}
            AppOutlinedButton({
                try {
                    val directory = java.io.File(context.cacheDir, "question-camera").apply { mkdirs() }
                    directory.listFiles()?.filter { it.isFile && System.currentTimeMillis() - it.lastModified() > 24 * 60 * 60 * 1000L }?.forEach { it.delete() }
                    val file = java.io.File.createTempFile("capture-", ".jpg", directory)
                    cameraFile = file.absolutePath
                    val uri = androidx.core.content.FileProvider.getUriForFile(context, context.packageName + ".camera", file)
                    camera.launch(uri)
                } catch (failure: Exception) {
                    cameraFile?.let { java.io.File(it).delete() }; cameraFile = null
                    vm.selectionError(failure)
                }
            }, enabled = enabled && recovery == null && cameraFile == null) { Text(stringResource(R.string.author_camera)) }
            s.selected?.let { selected ->
                AppSectionHeading(stringResource(R.string.question_image_selected))
                Image(selected.preview.asImageBitmap(),contentDescription=stringResource(R.string.question_image_selected),
                    modifier=Modifier.fillMaxWidth().heightIn(max=AppLayoutTokens.imageMaxHeight),contentScale=ContentScale.Fit)
                AppOutlinedButton({action="upload"},enabled=enabled && !s.needsReview){Text(stringResource(R.string.question_image_upload))}
                AppTextButton(vm::clearSelection,enabled=enabled){Text(stringResource(R.string.question_image_clear_selection))}
            }
            AppOutlinedButton({action="remove"},enabled=enabled && !s.needsReview && !q.imageUrl.isNullOrBlank()) {
                Text(stringResource(R.string.question_image_remove))
            }
        }
    }
}
