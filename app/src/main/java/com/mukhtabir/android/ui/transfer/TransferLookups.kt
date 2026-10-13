package com.mukhtabir.android.ui.transfer

import com.mukhtabir.android.ui.components.AppLoadingIndicator

import com.mukhtabir.android.ui.components.AppOutlinedButton as OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import com.mukhtabir.android.ui.components.AppTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.AdminService
import com.mukhtabir.android.data.remote.UserDto
import com.mukhtabir.android.ui.components.ErrorState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

@Composable
internal fun AuthorUserLookup(admin: AdminService, enabled: Boolean, pageSize: Int, pick: (UserDto) -> Unit) {
    var query by remember { mutableStateOf("") }
    var rows by remember { mutableStateOf(emptyList<UserDto>()) }
    var page by remember { mutableStateOf(1) }
    var totalPages by remember { mutableStateOf(1) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Throwable?>(null) }
    var job by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    DisposableEffect(Unit) { onDispose { job?.cancel() } }
    fun search(requested: Int) {
        if (!enabled || busy) return
        busy = true; error = null
        job = scope.launch {
            try {
                val found = admin.users(requested, pageSize, query.trim())
                ensureActive(); rows = found.items; page = found.page; totalPages = found.totalPages
            } catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) { ensureActive(); error = failure }
            finally { busy = false }
        }
    }
    OutlinedTextField(query, { query = it }, enabled = enabled && !busy, label = { Text(stringResource(R.string.search)) })
    OutlinedButton({ search(1) }, enabled = enabled && !busy) { Text(stringResource(R.string.search)) }
    if (busy) AppLoadingIndicator()
    error?.let { ErrorState(throwable = it, onRetry = if (busy || !enabled) null else ({ search(page) })) }
    if (!busy && error == null) rows.forEach { person ->
        TextButton({ if (enabled) pick(person) }, enabled = enabled) { Text(person.displayName + " (@" + person.username + ")") }
    }
    OutlinedButton({ search(page - 1) }, enabled = enabled && !busy && error == null && page > 1) { Text(stringResource(R.string.previous)) }
    OutlinedButton({ search(page + 1) }, enabled = enabled && !busy && error == null && page < totalPages) { Text(stringResource(R.string.next)) }
}
