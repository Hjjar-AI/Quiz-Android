@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.mukhtabir.android.ui.masterexams

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mukhtabir.android.R
import com.mukhtabir.android.core.has
import com.mukhtabir.android.data.remote.MasterExamDto
import com.mukhtabir.android.data.remote.MasterExamService
import com.mukhtabir.android.data.remote.UserDto
import com.mukhtabir.android.ui.components.AppActionRow
import com.mukhtabir.android.ui.components.AppButton
import com.mukhtabir.android.ui.components.AppCard
import com.mukhtabir.android.ui.components.AppLayoutTokens
import com.mukhtabir.android.ui.components.AppLoadingIndicator
import com.mukhtabir.android.ui.components.AppOutlinedButton
import com.mukhtabir.android.ui.components.AppPageHeading
import com.mukhtabir.android.ui.components.EmptyState
import com.mukhtabir.android.ui.components.ErrorState
import com.mukhtabir.android.ui.components.RefreshOnResume
import com.mukhtabir.android.ui.theme.AppTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

@Composable
fun MasterExamManagementRoute(user: UserDto, service: MasterExamService, onCreate: () -> Unit, onEdit: (Long) -> Unit) {
    var exams by remember(user.id) { mutableStateOf(emptyList<MasterExamDto>()) }
    var loading by remember(user.id) { mutableStateOf(false) }
    var error by remember(user.id) { mutableStateOf<Throwable?>(null) }
    val scope = rememberCoroutineScope()
    val refresh: () -> Unit = {
        if (!loading) {
            loading = true
            error = null
            scope.launch {
                try {
                    val response = service.list()
                    ensureActive()
                    exams = response.items.filter { user.has("master_exams.manage_any") || it.isOwner }
                } catch (cancel: CancellationException) { throw cancel }
                catch (failure: Exception) { ensureActive(); error = failure }
                finally { loading = false }
            }
        }
    }
    RefreshOnResume(refresh)
    LazyColumn(Modifier.fillMaxSize().padding(AppLayoutTokens.pageInsets), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
        item {
            AppPageHeading(stringResource(R.string.exam_author_manage))
            AppActionRow() {
                if (user.has("master_exams.create")) AppButton(onCreate, enabled = !loading) { Text(stringResource(R.string.exam_author_create)) }
                AppOutlinedButton(refresh, enabled = !loading) { Text(stringResource(R.string.refresh)) }
            }
            if (loading) AppLoadingIndicator()
            error?.let { ErrorState(throwable = it, onRetry = if (loading) null else refresh) }
            if (!loading && error == null && exams.isEmpty()) EmptyState(stringResource(R.string.master_empty))
        }
        items(exams, key = { it.id }) { exam ->
            AppCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(AppLayoutTokens.cardInsets), verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
                    MasterExamSummary(exam, actions = {
                        if (user.has("master_exams.manage_any") || user.has("master_exams.manage_own")) {
                            AppOutlinedButton({ onEdit(exam.id) }, enabled = !loading) { Text(stringResource(R.string.exam_author_edit)) }
                        }
                    })
                }
            }
        }
    }
}
