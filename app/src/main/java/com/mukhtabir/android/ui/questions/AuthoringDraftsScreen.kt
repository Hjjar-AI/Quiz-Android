package com.mukhtabir.android.ui.questions

import com.mukhtabir.android.ui.theme.AppTheme
import com.mukhtabir.android.ui.components.AppPageHeading

import com.mukhtabir.android.ui.components.AppLoadingIndicator

import com.mukhtabir.android.ui.components.AppLayoutTokens

import com.mukhtabir.android.ui.components.AppDangerTextButton

import com.mukhtabir.android.ui.components.AppDangerOutlinedButton

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import com.mukhtabir.android.ui.components.AppConfirmationDialog as AlertDialog
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.mukhtabir.android.ui.components.AppTextButton as TextButton
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mukhtabir.android.R
import com.mukhtabir.android.core.SessionDraftStore
import com.mukhtabir.android.core.has
import com.mukhtabir.android.data.remote.UserDto
import com.mukhtabir.android.ui.components.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun canUseAuthoringDrafts(user: UserDto) = listOf("questions.create", "questions.edit_own", "questions.edit_any", "questions.upload_image",
    "master_exams.create", "master_exams.manage_own", "master_exams.manage_any", "master_exams.drafts_library").any(user::has)

@Composable
fun AuthoringDraftsRoute(user: UserDto, store: SessionDraftStore, onOpen: (String) -> Unit) {
    if (!canUseAuthoringDrafts(user)) { Text(stringResource(R.string.error_forbidden)); return }
    var keys by remember(user.id) { mutableStateOf(emptyList<String>()) }
    var busy by remember(user.id) { mutableStateOf(false) }
    var error by remember(user.id) { mutableStateOf<Throwable?>(null) }
    var deleting by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    fun permitted(key: String): Boolean {
        val kind = key.substringBefore('-')
        val new = key.substringAfter('-') == "new"
        return when (kind) {
            "question" -> if (new) user.has("questions.create") else user.has("questions.edit_any") || user.has("questions.edit_own")
            "exam" -> if (new) user.has("master_exams.create") else user.has("master_exams.manage_any") || user.has("master_exams.manage_own")
            "image" -> user.has("questions.edit_any") || user.has("questions.upload_image")
            "draft" -> user.has("master_exams.drafts_library")
            else -> false
        }
    }
    val refresh: () -> Unit = { if (!busy) {
        busy = true
        scope.launch {
            try { val result = withContext(Dispatchers.IO) { store.editorKeys(user.id).getOrThrow() }; ensureActive(); keys = result; error = null }
            catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) { ensureActive(); error = failure }
            finally { busy = false }
        }
    } }
    LaunchedEffect(user.id) { refresh() }
    deleting?.let { key -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text(stringResource(R.string.author_drafts_title)) },
        text = { Text(stringResource(R.string.author_drafts_delete)) }, confirmButton = { AppDangerTextButton({
            deleting = null
            if (!busy) { busy = true; scope.launch {
                try { withContext(Dispatchers.IO) { store.clearEditor(user.id, key).getOrThrow() }; ensureActive(); keys = keys - key; error = null }
                catch (cancel: CancellationException) { throw cancel }
                catch (failure: Exception) { ensureActive(); error = failure }
                finally { busy = false }
            } }
        }) { Text(stringResource(R.string.ux_discard_saved_authoring_draft)) } }, dismissButton = { TextButton({ deleting = null }) { Text(stringResource(R.string.cancel)) } }) }
    LazyColumn(Modifier.fillMaxSize().padding(AppLayoutTokens.pageInsets), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
        item {
            AppPageHeading(stringResource(R.string.author_drafts_title))
            Text(stringResource(R.string.author_drafts_policy))
            AppOutlinedButton(refresh, enabled = !busy) { Text(stringResource(R.string.refresh)) }
            if (busy) AppLoadingIndicator()
            error?.let { ErrorState(throwable = it, onRetry = if (busy) null else refresh) }
            if (!busy && keys.isEmpty()) Text(stringResource(R.string.author_drafts_empty))
        }
        items(keys, key = { it }) { key -> AppCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(com.mukhtabir.android.ui.components.AppLayoutTokens.denseCardInsets)) {
            val label = when (key.substringBefore('-')) {
                "question" -> R.string.question_text; "image" -> R.string.question_manage_image
                "exam" -> R.string.exam_author_name; else -> R.string.exam_library_title
            }
            Text(stringResource(label) + " · " + if (key.endsWith("-new")) stringResource(R.string.author_new_draft) else key.substringAfter('-'))
            AppOutlinedButton({ onOpen(key) }, enabled = !busy && permitted(key)) { Text(stringResource(R.string.author_restore)) }
            AppDangerOutlinedButton({ deleting = key }, enabled = !busy) { Text(stringResource(R.string.ux_discard_saved_authoring_draft)) }
        } } }
    }
}
