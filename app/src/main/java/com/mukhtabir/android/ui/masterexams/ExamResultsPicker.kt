package com.mukhtabir.android.ui.masterexams

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.*
import com.mukhtabir.android.ui.components.*
import com.mukhtabir.android.ui.components.AppLayoutTokens
import com.mukhtabir.android.ui.components.AppLoadingIndicator
import com.mukhtabir.android.ui.components.AppPageHeading
import com.mukhtabir.android.ui.components.AppTextField
import com.mukhtabir.android.ui.theme.AppTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

@Composable
fun ExamResultsPicker(user: UserDto, service: MasterExamService, onOpen: (Long) -> Unit) {
    if (!canExportExamResults(user)) { Text(stringResource(R.string.error_forbidden)); return }
    var rows by remember(user.id) { mutableStateOf(emptyList<MasterExamDto>()) }
    var busy by remember(user.id) { mutableStateOf(false) }
    var error by remember(user.id) { mutableStateOf<Throwable?>(null) }
    var id by remember(user.id) { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val refresh: () -> Unit = { if (!busy) {
        busy = true; error = null
        scope.launch {
            try { val response = service.list(); ensureActive(); rows = response.items }
            catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) { ensureActive(); error = failure }
            finally { busy = false }
        }
    } }
    LaunchedEffect(user.id) { refresh() }
    LazyColumn(Modifier.fillMaxSize().padding(AppLayoutTokens.pageInsets), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
        item {
            AppPageHeading(stringResource(R.string.exam_results_title))
            Text(stringResource(R.string.transfer_exam_scope))
            AppTextField(id, { id = it }, label = { Text(stringResource(R.string.transfer_exam_id)) }, modifier = Modifier.fillMaxWidth())
            val chosen = id.trim().map { char -> Character.digit(char, 10).takeIf { it >= 0 }?.let { ('0'.code + it).toChar() } ?: char }.joinToString("").toLongOrNull()?.takeIf { it > 0 }
            AppOutlinedButton({ chosen?.let(onOpen) }, enabled = chosen != null) { Text(stringResource(R.string.exam_results_title)) }
            AppOutlinedButton(refresh, enabled = !busy) { Text(stringResource(R.string.refresh)) }
            if (busy) AppLoadingIndicator()
            error?.let { ErrorState(throwable = it, onRetry = if (busy) null else refresh) }
        }
        items(rows, key = { it.id }) { exam -> AppCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(AppLayoutTokens.denseCardInsets)) {
                MasterExamSummary(exam, actions = {
                    AppOutlinedButton({ onOpen(exam.id) }, enabled = !busy) { Text(stringResource(R.string.exam_results_title)) }
                })
            }
        } }
    }
}
