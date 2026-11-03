package com.mukhtabir.android.ui.transfer

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.MasterExamDto
import com.mukhtabir.android.data.remote.MasterExamService
import com.mukhtabir.android.data.remote.QuestionService
import com.mukhtabir.android.data.remote.TransferService
import com.mukhtabir.android.data.remote.UserDto
import com.mukhtabir.android.data.remote.canExportExamResults
import com.mukhtabir.android.ui.components.AppLayoutTokens
import com.mukhtabir.android.ui.components.AppLoadingIndicator
import com.mukhtabir.android.ui.components.AppOutlinedButton
import com.mukhtabir.android.ui.components.AppPageHeading
import com.mukhtabir.android.ui.components.AppTextField
import com.mukhtabir.android.ui.components.BlockedBackHandler
import com.mukhtabir.android.ui.components.ErrorState
import com.mukhtabir.android.ui.components.ExportArtifactActions
import com.mukhtabir.android.ui.components.ScreenBusyEffect
import com.mukhtabir.android.ui.components.collectAsLifecycleState
import com.mukhtabir.android.ui.theme.AppTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive

@Composable
fun ExamCsvRoute(service: TransferService, questions: QuestionService, exams: MasterExamService, user: UserDto,
    onBusyChanged: (Boolean) -> Unit, initialExamId: Long? = null) {
    if (!canExportExamResults(user)) { Text(stringResource(R.string.error_forbidden)); return }
    val context = LocalContext.current
    val vm: TransferViewModel = viewModel(key = "exam-csv-${user.id}", factory = TransferViewModel.Factory(service, questions, user, context))
    val s by vm.state.collectAsLifecycleState()
    var id by remember { mutableStateOf(initialExamId?.toString().orEmpty()) }
    var rows by remember { mutableStateOf(emptyList<MasterExamDto>()) }
    var error by remember { mutableStateOf<Throwable?>(null) }
    var reload by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(false) }
    var shareFailed by remember { mutableStateOf(false) }
    LaunchedEffect(user.id, reload) {
        loading = true
        try { rows = exams.list().items; ensureActive(); error = null }
        catch (cancel: CancellationException) { throw cancel }
        catch (failure: Exception) { ensureActive(); error = failure }
        finally { loading = false }
    }
    BlockedBackHandler(enabled = s.busy)
    ScreenBusyEffect(s.busy, onBusyChanged)
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { if (it != null) vm.save(it) }
    LazyColumn(Modifier.padding(AppLayoutTokens.pageInsets), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
        item {
            AppPageHeading(stringResource(R.string.transfer_exam_csv))
            if (loading || s.busy) AppLoadingIndicator()
            error?.let { ErrorState(throwable = it, onRetry = if (loading || s.busy) null else ({ reload += 1 })) }
            s.error?.let { ErrorState(throwable = it) }
            s.message?.let { Text(stringResource(it)) }
            Text(stringResource(R.string.transfer_exam_scope))
            AppTextField(id, { id = it }, enabled = !s.busy, label = { Text(stringResource(R.string.transfer_exam_id)) })
            rows.forEach { exam -> AppOutlinedButton({ id = exam.id.toString() }, enabled = !s.busy) { Text(exam.name) } }
            val chosen = id.trim().toLongOrNull()?.takeIf { it > 0 }
            AppOutlinedButton({ if (chosen != null) vm.exportExam(chosen, "summary", exams) }, enabled = chosen != null && !s.busy) { Text(stringResource(R.string.transfer_summary_csv)) }
            AppOutlinedButton({ if (chosen != null) vm.exportExam(chosen, "matrix", exams) }, enabled = chosen != null && !s.busy) { Text(stringResource(R.string.transfer_matrix_csv)) }
            s.download?.let { info ->
                ExportArtifactActions(info.filename, "text/csv", s.savedUri, enabled = !s.busy,
                    onSave = { save.launch(info.filename) }, onShareFailed = { shareFailed = true })
            }
            if (shareFailed) Text(stringResource(R.string.transfer_share_error))
        }
    }
}
