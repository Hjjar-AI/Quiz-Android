package com.mukhtabir.android.ui.admin

import com.mukhtabir.android.ui.theme.AppTheme
import com.mukhtabir.android.ui.components.AppPageHeading

import com.mukhtabir.android.ui.components.AppLoadingIndicator

import com.mukhtabir.android.ui.components.AppLayoutTokens

import com.mukhtabir.android.ui.components.AppDangerOutlinedButton

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import com.mukhtabir.android.ui.components.AppConfirmationDialog as AlertDialog
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.mukhtabir.android.ui.components.AppTextButton as TextButton
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mukhtabir.android.R
import com.mukhtabir.android.core.has
import com.mukhtabir.android.data.remote.*
import com.mukhtabir.android.ui.components.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*

internal data class OperatorState(val busy: Boolean = false, val info: JsonObject? = null, val quality: JsonObject? = null,
    val backups: List<JsonObject> = emptyList(), val errors: Map<String, Throwable> = emptyMap(), val error: Throwable? = null,
    val confirmed: Int? = null, val result: JsonObject? = null, val unknown: String? = null, val reviewed: Boolean = false)

internal class OperatorViewModel(private val user: UserDto, private val service: OperatorService) : ViewModel() {
    private val mutable = MutableStateFlow(OperatorState())
    val state = mutable.asStateFlow()
    init { if (user.has("admin.database")) refresh() }
    fun permitted(action: String) = user.has(if (action in setOf("ranks", "seed")) "admin.seed" else "admin.database") &&
        action in setOf("backup", "flag", "restore", "clear", "ranks", "seed")
    fun refresh() {
        if (!user.has("admin.database") || mutable.value.busy) return
        mutable.update { it.copy(busy = true, errors = emptyMap(), reviewed = false) }
        viewModelScope.launch {
            for (kind in listOf("info", "quality", "backups")) {
                try {
                    val data = when (kind) { "info" -> service.info(); "quality" -> service.quality(); else -> service.backups() }
                    ensureActive()
                    mutable.update { when (kind) {
                        "info" -> it.copy(info = data)
                        "quality" -> it.copy(quality = data)
                        else -> it.copy(backups = (data["items"] as? JsonArray)?.mapNotNull { row -> row as? JsonObject }.orEmpty())
                    } }
                } catch (cancel: CancellationException) { throw cancel }
                catch (failure: Exception) { ensureActive(); mutable.update { it.copy(errors = it.errors + (kind to failure)) } }
            }
            ensureActive()
            mutable.update { it.copy(busy = false, reviewed = it.unknown != null && it.errors.isEmpty()) }
        }
    }
    fun acknowledgeReview() {
        val s = mutable.value
        // Rank/seed have no outcome-read endpoint. Explicit acknowledgement follows inspection in the bank/ranks UI.
        if (!s.busy && s.unknown != null && (s.reviewed || s.unknown in setOf("ranks", "seed")))
            mutable.update { it.copy(unknown = null, reviewed = false, error = null) }
    }
    fun act(action: String, backup: String?, password: String) {
        val s = mutable.value
        if (!permitted(action) || s.busy || s.unknown != null || (action in setOf("restore", "clear") && password.isBlank())) return
        if (action == "restore" && s.backups.none { (it["name"] as? JsonPrimitive)?.content == backup }) return
        mutable.update { it.copy(busy = true, error = null, confirmed = null, result = null) }
        viewModelScope.launch {
            try {
                val result = service.act(action, backup, password)
                ensureActive()
                mutable.update { it.copy(busy = false, result = if (action == "ranks") result else null, confirmed = R.string.tools_done) }
                if (user.has("admin.database")) refresh()
            } catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) {
                ensureActive()
                val certain = failure is ApiException && failure.statusCode in setOf(400, 401, 403, 404, 405, 429)
                mutable.update { it.copy(busy = false, error = failure, unknown = if (certain) null else action, reviewed = false) }
            }
        }
    }
    class Factory(private val user: UserDto, private val service: OperatorService) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = OperatorViewModel(user, service) as T
    }
}

@Composable
fun OperatorToolsRoute(user: UserDto, service: OperatorService, onQuestion: (Long) -> Unit, onBank: () -> Unit,
    onBusyChanged: (Boolean) -> Unit) {
    if (!user.has("admin.database") && !user.has("admin.seed")) { Text(stringResource(R.string.error_forbidden)); return }
    val vm: OperatorViewModel = viewModel(key = "operator-${user.id}", factory = OperatorViewModel.Factory(user, service))
    val s by vm.state.collectAsState()
    var action by remember { mutableStateOf<String?>(null) }
    var backup by remember { mutableStateOf<String?>(null) }
    var password by remember { mutableStateOf("") }
    var qualityPage by remember { mutableStateOf(1) }
    com.mukhtabir.android.ui.components.BlockedBackHandler(enabled = s.busy)
    DisposableEffect(s.busy) { onBusyChanged(s.busy); onDispose { onBusyChanged(false) } }
    fun dismiss() { action = null; password = ""; backup = null }
    action?.let { selected -> AlertDialog(onDismissRequest = ::dismiss,
        title = { Text(stringResource(operatorLabel(selected))) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
            Text(stringResource(operatorWarning(selected)))
            backup?.let { Text(it) }
            if (selected in setOf("restore", "clear")) OutlinedTextField(password, { password = it }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(autoCorrect = false, keyboardType = androidx.compose.ui.text.input.KeyboardType.Password), label = { Text(stringResource(R.string.tools_password)) }, modifier = Modifier.fillMaxWidth(),
                textStyle = com.mukhtabir.android.util.ltrInputStyle(),
            )
        } }, confirmButton = { TextButton({
            val secret = password
            val name = backup
            dismiss()
            vm.act(selected, name, secret)
        }, enabled = !s.busy && s.unknown == null && vm.permitted(selected) &&
            (selected !in setOf("restore", "clear") || password.isNotBlank())) { Text(stringResource(R.string.confirm)) } },
        dismissButton = { TextButton(::dismiss) { Text(stringResource(R.string.cancel)) } }) }
    LazyColumn(Modifier.fillMaxSize().padding(AppLayoutTokens.pageInsets), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
        item {
            AppPageHeading(stringResource(R.string.tools_title))
            if (s.busy) AppLoadingIndicator()
            s.error?.let { ErrorState(throwable = it) }
            s.confirmed?.let { Text(stringResource(it)) }
            s.result?.let { OperatorReport(it, listOf("scanned", "updated")) }
            if (user.has("admin.database")) AppOutlinedButton(vm::refresh, enabled = !s.busy) { Text(stringResource(R.string.refresh)) }
            if (s.unknown != null) {
                Text(stringResource(R.string.tools_unknown))
                AppOutlinedButton(onBank, enabled = !s.busy) { Text(stringResource(R.string.question_library)) }
                AppOutlinedButton(vm::acknowledgeReview, enabled = !s.busy && (s.reviewed || s.unknown in setOf("ranks", "seed"))) { Text(stringResource(R.string.tools_reviewed)) }
            }
        }
        if (user.has("admin.database")) {
            item { AppSection(stringResource(R.string.tools_info), initiallyExpanded = true) {
                s.errors["info"]?.let { ErrorState(throwable = it) }
                s.info?.let { OperatorReport(it, listOf("database_type", "file_size", "file_modified", "total_questions", "verified_count", "total_users", "total_categories", "total_sessions")) }
            } }
            item { AppSection(stringResource(R.string.tools_quality)) {
                s.errors["quality"]?.let { ErrorState(throwable = it) }
                s.quality?.let { report ->
                    (report["summary"] as? JsonObject)?.let { summary ->
                        OperatorReport(summary, listOf("questions_scanned", "questions_with_issues"))
                        (summary["issue_counts"] as? JsonObject)?.let { OperatorReport(it, it.keys.toList()) }
                    }
                    OperatorReport(report, listOf("flags_created"))
                    AppOutlinedButton({ action = "flag" }, enabled = !s.busy && s.unknown == null) { Text(stringResource(R.string.tools_flag)) }
                    val rows = (report["items"] as? JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty()
                    val pages = ((rows.size - 1).coerceAtLeast(0) / 20) + 1
                    val page = qualityPage.coerceIn(1, pages)
                    rows.drop((page - 1) * 20).take(20).forEach { row ->
                        AppCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(com.mukhtabir.android.ui.components.AppLayoutTokens.denseCardInsets), verticalArrangement = Arrangement.spacedBy(com.mukhtabir.android.ui.components.AppLayoutTokens.controlGap)) {
                            MarkdownText((row["question"] as? JsonPrimitive)?.content.orEmpty())
                            OperatorReport(row, listOf("has_open_quality_flag"))
                            (row["issues"] as? JsonArray)?.forEach { issue ->
                                val objectValue = issue as? JsonObject
                                val code = (objectValue?.get("code") as? JsonPrimitive)?.content
                                val label = code?.let(::reportLabel)
                                if (label != null) Text(stringResource(label))
                                objectValue?.let { OperatorReport(it, listOf("related_question_ids", "positions", "locales")) }
                            }
                            val id = (row["id"] as? JsonPrimitive)?.longOrNull
                            if (id != null) AppOutlinedButton({ onQuestion(id) }, enabled = !s.busy) { Text(stringResource(R.string.tools_open_question)) }
                        } }
                    }
                    if (rows.isEmpty()) Text(stringResource(R.string.tools_quality_clean))
                    if (pages > 1) {
                        Text(stringResource(R.string.report_page, page, pages))
                        AppOutlinedButton({ qualityPage = page - 1 }, enabled = page > 1) { Text(stringResource(R.string.previous)) }
                        AppOutlinedButton({ qualityPage = page + 1 }, enabled = page < pages) { Text(stringResource(R.string.next)) }
                    }
                }
            } }
            item { AppSection(stringResource(R.string.tools_backups)) {
                s.errors["backups"]?.let { ErrorState(throwable = it) }
                AppOutlinedButton({ action = "backup" }, enabled = !s.busy && s.unknown == null) { Text(stringResource(R.string.tools_backup)) }
                if (s.backups.isEmpty()) Text(stringResource(R.string.tools_no_backups))
                s.backups.forEach { row ->
                    AppCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(com.mukhtabir.android.ui.components.AppLayoutTokens.denseCardInsets)) {
                        OperatorReport(row, listOf("name", "size", "modified"))
                        val name = (row["name"] as? JsonPrimitive)?.content
                        if (name != null) AppDangerOutlinedButton({ backup = name; action = "restore" }, enabled = !s.busy && s.unknown == null && s.errors["backups"] == null) { Text(stringResource(R.string.tools_restore)) }
                    } }
                }
            } }
            item { AppSection(stringResource(R.string.tools_clear)) {
                Text(stringResource(R.string.tools_clear_warning))
                AppDangerOutlinedButton({ action = "clear" }, enabled = !s.busy && s.unknown == null) { Text(stringResource(R.string.tools_clear)) }
            } }
        }
        if (user.has("admin.seed")) item { AppSection(stringResource(R.string.tools_utilities)) {
            AppOutlinedButton({ action = "ranks" }, enabled = !s.busy && s.unknown == null) { Text(stringResource(R.string.tools_ranks)) }
            AppOutlinedButton({ action = "seed" }, enabled = !s.busy && s.unknown == null) { Text(stringResource(R.string.tools_seed)) }
        } }
    }
}

private fun operatorLabel(action: String): Int = when (action) {
    "backup" -> R.string.tools_backup; "restore" -> R.string.tools_restore; "clear" -> R.string.tools_clear
    "flag" -> R.string.tools_flag; "ranks" -> R.string.tools_ranks; else -> R.string.tools_seed
}
private fun operatorWarning(action: String): Int = when (action) {
    "backup" -> R.string.tools_backup_warning; "restore" -> R.string.tools_restore_warning; "clear" -> R.string.tools_clear_warning
    "flag" -> R.string.tools_flag_warning; "ranks" -> R.string.tools_ranks_warning; else -> R.string.tools_seed_warning
}
