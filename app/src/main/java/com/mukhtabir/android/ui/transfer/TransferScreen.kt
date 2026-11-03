@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.mukhtabir.android.ui.transfer

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mukhtabir.android.R
import com.mukhtabir.android.core.has
import com.mukhtabir.android.data.remote.CategoryService
import com.mukhtabir.android.data.remote.QuestionDto
import com.mukhtabir.android.data.remote.QuestionService
import com.mukhtabir.android.data.remote.TransferService
import com.mukhtabir.android.data.remote.UserDto
import com.mukhtabir.android.ui.components.AppActionRow
import com.mukhtabir.android.ui.components.AppCheckboxRow
import com.mukhtabir.android.ui.components.AppConfirmationDialog
import com.mukhtabir.android.ui.components.AppDangerTextButton
import com.mukhtabir.android.ui.components.AppFilterChip
import com.mukhtabir.android.ui.components.AppLayoutTokens
import com.mukhtabir.android.ui.components.AppLoadingIndicator
import com.mukhtabir.android.ui.components.AppOutlinedButton
import com.mukhtabir.android.ui.components.AppPageHeading
import com.mukhtabir.android.ui.components.AppPagination
import com.mukhtabir.android.ui.components.AppPasswordField
import com.mukhtabir.android.ui.components.AppSection
import com.mukhtabir.android.ui.components.AppTextButton
import com.mukhtabir.android.ui.components.AppTextField
import com.mukhtabir.android.ui.components.ErrorState
import com.mukhtabir.android.ui.components.ExportArtifactActions
import com.mukhtabir.android.ui.components.MarkdownText
import com.mukhtabir.android.ui.components.ScreenBusyEffect
import com.mukhtabir.android.ui.components.collectAsLifecycleState
import com.mukhtabir.android.ui.theme.AppTheme
import com.mukhtabir.android.util.formatNumber
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
fun TransferRoute(service: TransferService, admin: com.mukhtabir.android.data.remote.AdminService, questions: QuestionService, categories: CategoryService,
    user: UserDto, pageSize: Int, onBack: () -> Unit, onBusyChanged: (Boolean) -> Unit, initialVerified: Boolean = false) {
    val context = LocalContext.current
    val vm: TransferViewModel = viewModel(key = "transfer-${user.id}", factory = TransferViewModel.Factory(service, questions, user, context))
    val s by vm.state.collectAsLifecycleState()
    if (!user.has("admin.database")) { Text(stringResource(R.string.error_forbidden)); return }
    var kind by vm.inputs.kind
    var mode by vm.inputs.mode
    var strategy by vm.inputs.strategy
    var mapping by vm.inputs.mapping
    var resolutions by vm.inputs.resolutions
    var confirming by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var importPassword by remember { mutableStateOf("") }
    LaunchedEffect(vm) { vm.refreshImportLimit() }
    var format by vm.inputs.format
    var statePackage by vm.inputs.statePackage
    var images by vm.inputs.images
    var verified by vm.inputs.verified
    LaunchedEffect(initialVerified) {if(initialVerified) verified=true}
    var search by vm.inputs.search
    var categoryIds by vm.inputs.categoryIds
    var tags by vm.inputs.tags
    var difficulty by vm.inputs.difficulty
    var manual by vm.inputs.manual
    var selected by vm.inputs.selected
    var questionQuery by vm.inputs.questionQuery
    var questionPage by remember { mutableStateOf(1) }
    var questionPages by remember { mutableStateOf(1) }
    var rows by remember { mutableStateOf(emptyList<QuestionDto>()) }
    var finding by remember { mutableStateOf(false) }
    var findError by remember { mutableStateOf<Throwable?>(null) }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    var title by vm.inputs.title
    var pdfMode by vm.inputs.pdfMode
    var answers by vm.inputs.answers
    var pdfTheme by vm.inputs.pdfTheme
    var locale by vm.inputs.locale
    var about by vm.inputs.about
    var aboutTitle by vm.inputs.aboutTitle
    var aboutBody by vm.inputs.aboutBody
    var fields by vm.inputs.fields
    var validation by remember { mutableStateOf(false) }
    var categoryRows by remember { mutableStateOf(emptyList<com.mukhtabir.android.data.remote.CategoryDto>()) }
    var categoryError by remember { mutableStateOf<Throwable?>(null) }
    var categoryReload by remember { mutableStateOf(0) }
    var shareError by remember { mutableStateOf(false) }
    LaunchedEffect(user.id, categoryReload) {
        try { categoryRows = categories.list().items; categoryError = null }
        catch (cancel: CancellationException) { throw cancel }
        catch (error: Exception) { ensureActive(); categoryError = error }
    }
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) vm.select(uri, kind) }
    val multiPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris -> vm.selectMany(uris) }
    val saver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(s.download?.mimeType ?: "application/octet-stream")) { uri -> if (uri != null) vm.save(uri) }
    var leaving by remember { mutableStateOf(false) }
    BackHandler {
        if (!s.busy) {
            if (s.file != null || s.queue.any { it.status != ImportStatus.Imported } || s.uncertain || (s.artifact != null && s.savedUri == null)) leaving = true else onBack()
        }
    }
    if (leaving) AppConfirmationDialog(onDismissRequest = { leaving = false }, title = { Text(stringResource(R.string.transfer_leave)) },
        text = { Text(stringResource(R.string.transfer_leave_body)) },
        confirmButton = { AppDangerTextButton({ leaving = false; onBack() }, enabled = !s.busy) { Text(stringResource(R.string.ux_leave_transfer)) } },
        dismissButton = { AppTextButton({ leaving = false }) { Text(stringResource(R.string.cancel)) } })
    ScreenBusyEffect(s.busy, onBusyChanged)
    DisposableEffect(Unit) { onDispose { searchJob?.cancel() } }
    fun runImport(preview: Boolean) = vm.importFile(kind, mode, strategy, preview, JsonObject(mapping), JsonObject(resolutions), password)
    val unknown = s.analysis?.get("unknown_authors") as? JsonArray ?: JsonArray(emptyList())
    val conflicts = s.analysis?.get("conflicts") as? JsonArray ?: JsonArray(emptyList())
    val ready = kind != "state" || (s.analysis != null && unknown.all { mapping[it.jsonObject["name"]?.jsonPrimitive?.content.orEmpty()] != null } &&
        (strategy != "review" || conflicts.all { resolutions[it.jsonObject["uuid"]?.jsonPrimitive?.content.orEmpty()] != null }))
    if (confirming) AppConfirmationDialog(onDismissRequest = { confirming = false; password = "" },
        title = { Text(stringResource(R.string.transfer_confirm)) },
        text = { Column {
            if (kind == "simple") {
                Text(stringResource(R.string.transfer_batch_confirm, formatNumber(s.queue.count { it.status in setOf(ImportStatus.Pending, ImportStatus.Limited) })))
            } else Text(s.name)
            Text(stringResource(if (kind == "state" && mode == "replace") R.string.transfer_replace_warning else R.string.transfer_import_warning))
            if (kind == "state" && mode == "replace") AppPasswordField(password, { password = it },
                label = { Text(stringResource(R.string.transfer_admin_password)) }, textStyle = com.mukhtabir.android.util.ltrInputStyle(),
            )
        } }, confirmButton = { AppTextButton({ confirming = false; runImport(false); password = "" },
            enabled = !s.busy && ready && (kind != "state" || mode != "replace" || password.isNotBlank())) { Text(stringResource(R.string.confirm)) } },
        dismissButton = { AppTextButton({ confirming = false; password = "" }) { Text(stringResource(R.string.cancel)) } })
    LazyColumn(Modifier.padding(AppLayoutTokens.pageInsets), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
        item {
            AppPageHeading(stringResource(R.string.transfer_title))
            if (s.busy) { AppLoadingIndicator(); Text(stringResource(R.string.transfer_working)) }
            s.error?.takeUnless { error -> s.queue.any { it.error === error } }?.let { ErrorState(throwable = it) }
            s.message?.let { Text(stringResource(it)) }
            if (validation) Text(stringResource(R.string.transfer_invalid))
            if (shareError) Text(stringResource(R.string.transfer_share_error))
            Text(stringResource(R.string.transfer_import))
            if (s.importUnlockedUntil > 0) Text(stringResource(R.string.transfer_unlock_active))
            AppSection(stringResource(R.string.transfer_unlock_title)) {
                Text(stringResource(R.string.transfer_unlock_hint))
                AppPasswordField(importPassword, { importPassword = it }, enabled = !s.busy,
                    label = { Text(stringResource(R.string.transfer_admin_password)) },
                    textStyle = com.mukhtabir.android.util.ltrInputStyle(),
                )
                AppOutlinedButton({ vm.unlockImports(importPassword); importPassword = "" }, enabled = !s.busy && importPassword.isNotEmpty()) {
                    Text(stringResource(R.string.transfer_unlock_button))
                }
            }
            if (s.file == null && s.queue.isEmpty()) Choice(kind, listOf("simple" to R.string.transfer_simple, "telegram" to R.string.transfer_telegram, "state" to R.string.transfer_state), !s.busy && !s.uncertain) { kind = it; vm.invalidateAnalysis() }
            Text(stringResource(R.string.transfer_formats))
            if (s.file != null || s.queue.isNotEmpty()) AppTextButton(vm::clearFile, enabled = !s.busy && !s.uncertain) {
                Text(stringResource(if (s.queue.isNotEmpty()) R.string.ux_clear_import_queue else R.string.ux_remove_import_file))
            }
            if (kind == "simple") Text(stringResource(R.string.transfer_multiple_hint))
            AppOutlinedButton({
                if (kind == "simple") multiPicker.launch(arrayOf("*/*")) else picker.launch(arrayOf("*/*"))
            }, enabled = !s.busy && !s.uncertain) { Text(stringResource(if (kind == "simple") R.string.transfer_choose_multiple else R.string.transfer_choose)) }
            if (s.queue.isNotEmpty()) Text(stringResource(R.string.ux_import_queue_summary,
                formatNumber(s.queue.size), formatNumber(s.queue.count { it.status == ImportStatus.Imported }),
                formatNumber(s.queue.count { it.status in setOf(ImportStatus.Failed, ImportStatus.Uncertain, ImportStatus.Limited) })))
            if (s.queue.any { it.status == ImportStatus.Limited }) Text(stringResource(R.string.transfer_unlock_limited))
            if (kind == "simple" && s.queue.any { it.status == ImportStatus.Failed || it.status == ImportStatus.Uncertain }) {
                Text(stringResource(R.string.transfer_batch_stopped))
            }
            if (kind == "state") {
                Choice(mode, listOf("merge" to R.string.transfer_merge, "replace" to R.string.transfer_replace), !s.busy && !s.uncertain) { mode = it; strategy = if (it == "replace") "use_imported" else "keep_local"; vm.invalidateAnalysis() }
                Choice(strategy, listOf("keep_local" to R.string.transfer_keep, "use_imported" to R.string.transfer_use, "review" to R.string.transfer_review), !s.busy && !s.uncertain) { strategy = it; vm.invalidateAnalysis() }
                AppOutlinedButton({ runImport(true) }, enabled = !s.busy && s.file != null) { Text(stringResource(R.string.transfer_preview)) }
            }
            if (s.uncertain) {
                Text(stringResource(R.string.transfer_uncertain))
                if (kind != "state" && s.queue.isEmpty()) {
                    Text(stringResource(R.string.transfer_simple_review))
                    AppOutlinedButton(vm::reviewSimple, enabled = !s.busy) { Text(stringResource(R.string.transfer_review)) }
                    s.reviewQuestions.forEach { MarkdownText(it.question) }
                }
                if (s.queue.isEmpty()) AppOutlinedButton(vm::acknowledgeReview, enabled = !s.busy && s.reviewed) { Text(stringResource(R.string.transfer_reviewed)) }
            }
            if (s.queue.isEmpty() && s.name.isNotBlank() && (s.file != null || s.analysis != null || s.result != null)) {
                AppSection(s.name, subtitle = stringResource(when {
                    s.uncertain -> R.string.ux_import_uncertain_status
                    s.result != null -> R.string.transfer_queue_imported
                    s.analysis != null -> R.string.ux_import_preview_ready
                    else -> R.string.ux_import_selected
                }), initiallyExpanded = false, composeCollapsedContent = false) {
                    TransferReport(s.analysis)
                    TransferReport(s.result)
                }
            }
        }
        s.queue.forEach { entry -> item(key = "import-${entry.uri}") {
            AppSection(title = entry.name, modifier = Modifier.fillMaxWidth(),
                initiallyExpanded = false, composeCollapsedContent = false,
                subtitle = stringResource(when (entry.status) {
                        ImportStatus.Pending -> R.string.transfer_queue_pending
                        ImportStatus.Limited -> R.string.transfer_queue_limited
                        ImportStatus.Uploading -> R.string.transfer_queue_uploading
                        ImportStatus.Imported -> R.string.transfer_queue_imported
                        ImportStatus.Failed -> R.string.transfer_queue_failed
                        ImportStatus.Uncertain -> R.string.ux_import_uncertain_status
                    })) {
                    TransferReport(entry.result)
                    entry.error?.let { error ->
                        AppSection(stringResource(R.string.ux_import_error_details), composeCollapsedContent = false) {
                            ErrorState(throwable = error)
                        }
                    }
                    if (entry.status == ImportStatus.Failed) Text(stringResource(R.string.ux_import_failed_next))
                    if (entry.status == ImportStatus.Uncertain) {
                        Text(stringResource(R.string.transfer_uncertain))
                        Text(stringResource(R.string.transfer_simple_review))
                        AppOutlinedButton(vm::reviewSimple, enabled = !s.busy) { Text(stringResource(R.string.ux_review_import_outcome)) }
                        s.reviewQuestions.forEach { MarkdownText(it.question) }
                        AppOutlinedButton(vm::acknowledgeReview, enabled = !s.busy && s.reviewed) {
                            Text(stringResource(R.string.transfer_reviewed))
                        }
                    }
                    Text(stringResource(R.string.ux_import_remove_hint), style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                    AppTextButton({ vm.removeQueued(entry.uri) }, enabled = !s.busy && !s.uncertain) {
                        Text(stringResource(R.string.transfer_remove_file, entry.name))
                    }
            }
        } }
        unknown.forEach { author -> item(key = "author-${author.jsonObject["name"]}") {
            val name = author.jsonObject.getValue("name").jsonPrimitive.content
            Text(stringResource(R.string.transfer_author, name))
            val current = (mapping[name] as? JsonObject)?.get("action")?.jsonPrimitive?.content.orEmpty()
            Choice(current, listOf("stub" to R.string.transfer_stub, "null" to R.string.transfer_unassigned), !s.busy && !s.uncertain) { action ->
                mapping = mapping + (name to buildJsonObject { put("action", action) })
            }
            if (user.has("admin.users")) AuthorUserLookup(admin, !s.busy && !s.uncertain, pageSize) { person ->
                mapping = mapping + (name to buildJsonObject { put("action", "user"); put("user_id", person.id) })
            }
            (mapping[name] as? JsonObject)?.get("user_id")?.jsonPrimitive?.content?.let { Text(stringResource(R.string.transfer_local_user) + ": " + it) }
            var id by remember(name, s.analysis) { mutableStateOf((mapping[name] as? JsonObject)?.get("user_id")?.jsonPrimitive?.content.orEmpty()) }
            AppTextField(id, { value ->
                id = value
                val parsed = value.trim().toLongOrNull()?.takeIf { it > 0 }
                mapping = if (parsed == null) mapping - name else mapping + (name to buildJsonObject { put("action", "user"); put("user_id", parsed) })
            }, enabled = !s.busy && !s.uncertain, label = { Text(stringResource(R.string.transfer_local_user)) })
        } }
        if (strategy == "review") conflicts.forEach { conflict -> item(key = "conflict-${conflict.jsonObject["uuid"]}") {
            val c = conflict.jsonObject
            val uuid = c.getValue("uuid").jsonPrimitive.content
            Text(stringResource(R.string.transfer_conflict))
            Text(stringResource(R.string.transfer_keep)); MarkdownText(c["local_question"]?.jsonPrimitive?.content.orEmpty())
            Text(stringResource(R.string.transfer_use)); MarkdownText(c["imported_question"]?.jsonPrimitive?.content.orEmpty())
            Text((c["changed_fields"] as? JsonArray).orEmpty().joinToString { it.jsonPrimitive.content.replace('_', ' ') })
            Choice(resolutions[uuid]?.jsonPrimitive?.content.orEmpty(), listOf("keep_local" to R.string.transfer_keep, "use_imported" to R.string.transfer_use), !s.busy && !s.uncertain) { resolutions = resolutions + (uuid to JsonPrimitive(it)) }
        } }
        item {
            AppOutlinedButton({ confirming = true }, enabled = !s.busy && !s.uncertain && (if (kind == "simple") s.queue.any { it.status in setOf(ImportStatus.Pending, ImportStatus.Limited) } else s.file != null) && ready) { Text(stringResource(R.string.transfer_import)) }
            AppSection(stringResource(R.string.transfer_export), initiallyExpanded = true) {
            Text(stringResource(R.string.transfer_export))
            Check(statePackage, R.string.transfer_state, !s.busy) { statePackage = it; if (it && format !in setOf("json", "xlsx")) format = "json" }
            Choice(format, (if (statePackage) listOf("json", "xlsx") else listOf("json", "csv", "xlsx", "pdf")).map { it to 0 }, !s.busy) { format = it }
            Check(verified, R.string.transfer_verified, !s.busy) { verified = it }
            if (statePackage) Check(images, R.string.transfer_images, !s.busy) { images = it }
            if (!statePackage) Check(manual, R.string.transfer_manual, !s.busy) { manual = it }
            if (!manual || statePackage) {
                AppTextField(search, { search = it }, enabled = !s.busy, label = { Text(stringResource(R.string.search)) })
                categoryError?.let { ErrorState(throwable = it, onRetry = if (s.busy) null else ({ categoryReload += 1 })) }
                categoryRows.forEach { category ->
                    val ids = parseTransferIds(categoryIds)
                    AppFilterChip(category.id in ids.orEmpty(), { ids?.let { current -> categoryIds = (if (category.id in current) current - category.id else current + category.id).joinToString(",") } },
                        enabled = !s.busy && ids != null, label = { Text(category.name) })
                }
                AppTextField(categoryIds, { categoryIds = it }, enabled = !s.busy, label = { Text(stringResource(R.string.transfer_categories)) })
                AppTextField(tags, { tags = it }, enabled = !s.busy, label = { Text(stringResource(R.string.ux_tags_any)) })
                Text(stringResource(R.string.quiz_filter_matching), style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                FlowRow { listOf("easy" to R.string.difficulty_easy, "medium" to R.string.difficulty_medium, "hard" to R.string.difficulty_hard).forEach { (d, label) ->
                    AppFilterChip(d in difficulty, { difficulty = if (d in difficulty) difficulty - d else difficulty + d }, enabled = !s.busy, label = { Text(stringResource(label)) })
                } }
            } else {
                Text(stringResource(R.string.exam_author_selected, formatNumber(selected.size)))
                selected.forEachIndexed { index, id ->
                    Text(stringResource(R.string.export_selection_position, formatNumber(index + 1), formatNumber(id)))
                    AppActionRow() {
                        fun move(delta: Int) {
                            if (vm.state.value.busy) return
                            // Resolve the current ID rather than relying on an index from an older composition.
                            val current = selected.indexOf(id)
                            val target = current + delta
                            if (current < 0 || target !in selected.indices) return
                            selected = selected.toMutableList().apply { add(target, removeAt(current)) }.toList()
                        }
                        AppTextButton({ move(-1) }, enabled = !s.busy && index > 0) {
                            Text(stringResource(R.string.export_move_up, formatNumber(id)))
                        }
                        AppTextButton({ move(1) }, enabled = !s.busy && index < selected.lastIndex) {
                            Text(stringResource(R.string.export_move_down, formatNumber(id)))
                        }
                        AppTextButton({ if (!vm.state.value.busy) selected = selected - id }, enabled = !s.busy) {
                            Text(stringResource(R.string.exam_target_remove, formatNumber(id)))
                        }
                    }
                }
                AppTextField(questionQuery, { questionQuery = it }, label = { Text(stringResource(R.string.search)) }, enabled = !s.busy)
                fun find(page: Int) {
                    searchJob?.cancel(); finding = true; findError = null
                    searchJob = scope.launch {
                        try { val found = questions.list(page = page, perPage = pageSize, search = questionQuery, verifiedOnly = verified)
                            ensureActive(); rows = found.items; questionPage = found.page; questionPages = found.totalPages
                        } catch (cancel: CancellationException) { throw cancel }
                        catch (error: Exception) { ensureActive(); findError = error }
                        finally { finding = false }
                    }
                }
                AppOutlinedButton({ find(1) }, enabled = !finding && !s.busy) { Text(stringResource(R.string.search)) }
                if (finding) AppLoadingIndicator()
                findError?.let { ErrorState(throwable = it) }
                rows.forEach { row ->
                    MarkdownText(row.question)
                    AppFilterChip(row.id in selected, { selected = if (row.id in selected) selected - row.id else selected + row.id },
                        enabled = !s.busy && !finding && findError == null, label = { Text(formatNumber(row.id)) })
                }
                AppPagination(questionPage, questionPages,
                    onPrevious = { find(questionPage - 1) }, onNext = { find(questionPage + 1) },
                    previousEnabled = !s.busy && !finding && findError == null,
                    nextEnabled = !s.busy && !finding && findError == null)
            }
            if (format == "pdf") {
                AppTextField(title, { title = it }, enabled = !s.busy, label = { Text(stringResource(R.string.transfer_pdf_title)) })
                Choice(pdfMode, listOf("study" to R.string.study, "quiz" to R.string.exam), !s.busy) { pdfMode = it }
                Choice(answers, listOf("inline" to R.string.transfer_inline, "end" to R.string.transfer_end, "after_25" to R.string.transfer_after25, "none" to R.string.none), !s.busy && pdfMode == "study") { answers = it }
                Text(stringResource(R.string.theme))
                Choice(pdfTheme, listOf("stone", "iris", "blossom", "lagoon", "slate", "ink", "amber", "ruby", "dark", "midnight", "onyx", "contrast").map { it to 0 }, !s.busy) { pdfTheme = it }
                Choice(locale, listOf("ar" to 0, "en" to 0), !s.busy) { locale = it }
                Check(about, R.string.transfer_cover, !s.busy) { about = it }
                if (about) {
                    AppTextField(aboutTitle, { aboutTitle = it }, enabled = !s.busy, label = { Text(stringResource(R.string.transfer_pdf_title)) })
                    AppTextField(aboutBody, { aboutBody = it }, enabled = !s.busy, label = { Text(stringResource(R.string.transfer_cover_body)) })
                    fields.forEachIndexed { index, pair ->
                        AppTextField(pair.first, { value -> fields = fields.mapIndexed { i, p -> if (i == index) p.copy(first = value) else p } }, enabled = !s.busy, label = { Text(stringResource(R.string.transfer_label)) })
                        AppTextField(pair.second, { value -> fields = fields.mapIndexed { i, p -> if (i == index) p.copy(second = value) else p } }, enabled = !s.busy, label = { Text(stringResource(R.string.transfer_value)) })
                        AppDangerTextButton({ fields = fields.filterIndexed { i, _ -> i != index } }, enabled = !s.busy) { Text(stringResource(R.string.remove_choice)) }
                    }
                    AppOutlinedButton({ fields = fields + ("" to "") }, enabled = !s.busy && fields.size < 10) { Text(stringResource(R.string.transfer_add_field)) }
                }
            }
            }
            AppOutlinedButton({
                val ids = parseTransferIds(categoryIds)
                val categoryArray = JsonArray(ids.orEmpty().map { JsonPrimitive(it) })
                val tagArray = JsonArray(tags.lineSequence().map(String::trim).filter(String::isNotEmpty).map { JsonPrimitive(it) }.toList())
                val filters = mapOf("search" to search, "category_ids" to categoryArray.toString(), "difficulty" to difficulty.joinToString(","), "tags_filter" to tagArray.toString())
                val filtered = !manual || statePackage
                val invalidPdf = format == "pdf" && (title.codePointsCount() > 150 || (about &&
                    (aboutTitle.codePointsCount() > 150 || aboutBody.codePointsCount() > 3000 || fields.any {
                        it.first.isBlank() || it.first.codePointsCount() > 60 || it.second.isBlank() || it.second.codePointsCount() > 500 })))
                if ((filtered && (ids == null || filters.values.any { it.codePointsCount() > 500 })) ||
                    (!filtered && selected.isEmpty()) || invalidPdf) { validation = true; return@AppOutlinedButton }
                validation = false
                val body = buildJsonObject {
                    if (manual && !statePackage) put("question_ids", JsonArray(selected.map { JsonPrimitive(it) })) else put("filters", JsonObject(filters.mapValues { (key, value) ->
                        when (key) {
                            "category_ids" -> categoryArray
                            "tags_filter" -> tagArray
                            else -> JsonPrimitive(value)
                        }
                    }))
                    if (format == "pdf") put("title", title)
                    if (format == "pdf") {
                        put("pdf_mode", pdfMode); put("answer_layout", if (pdfMode == "quiz") "none" else answers); put("theme", pdfTheme); put("locale", locale)
                        put("front_matter", buildJsonObject { put("enabled", about); put("heading", if (about) aboutTitle else ""); put("body", if (about) aboutBody else "")
                            put("fields", JsonArray((if (about) fields else emptyList()).map { (label, value) -> buildJsonObject { put("label", label); put("value", value) } })) })
                    }
                }
                vm.export(format, verified, statePackage, images, filters, body)
            }, enabled = !s.busy) { Text(stringResource(R.string.transfer_export)) }
            s.download?.let { info ->
                ExportArtifactActions(info.filename, info.mimeType, s.savedUri, enabled = !s.busy,
                    onSave = { saver.launch(info.filename) }, onShareFailed = { shareError = true })
            }
        }
    }
}

@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
private fun Choice(value: String, options: List<Pair<String, Int>>, enabled: Boolean, change: (String) -> Unit) {
    AppActionRow() { options.forEach { (key, label) ->
        AppFilterChip(value == key, { change(key) }, enabled = enabled, label = { Text(if (label == 0) key.uppercase() else stringResource(label)) })
    } }
}

@Composable
private fun Check(value: Boolean, label: Int, enabled: Boolean, change: (Boolean) -> Unit) {
    AppCheckboxRow(stringResource(label), value, change, enabled = enabled)
}

@Composable
private fun TransferReport(report: JsonObject?) {
    if (report == null) return
    val counts = report["counts"] as? JsonObject ?: report
    val labels = mapOf(
        "categories_created" to R.string.transfer_count_categories_created,
        "categories_updated" to R.string.transfer_count_categories_updated,
        "tags_created" to R.string.transfer_count_tags_created,
        "cases_created" to R.string.transfer_count_cases_created,
        "knowledge_objects_created" to R.string.transfer_count_knowledge_objects_created,
        "knowledge_objects_updated" to R.string.transfer_count_knowledge_objects_updated,
        "questions_created" to R.string.transfer_count_questions_created,
        "questions_updated" to R.string.transfer_count_questions_updated,
        "questions_skipped" to R.string.transfer_count_questions_skipped,
        "questions_deleted" to R.string.transfer_count_questions_deleted,
        "images_imported" to R.string.transfer_count_images_imported,
        "drafts_created" to R.string.transfer_count_drafts_created,
        "drafts_updated" to R.string.transfer_count_drafts_updated,
        "drafts_skipped" to R.string.transfer_count_drafts_skipped,
        "imported" to R.string.transfer_count_imported,
        "skipped" to R.string.transfer_count_skipped,
        "quality_flags_created" to R.string.transfer_count_quality_flags_created,
    )
    counts.forEach { (key, value) ->
        val label = labels[key]
        if (label != null && value is JsonPrimitive) Text(stringResource(label) + ": " + (value.content.toLongOrNull()?.let { formatNumber(it) } ?: value.content))
    }
    report["message"]?.let { if (it is JsonPrimitive) Text(it.content) }
    (report["errors"] as? JsonArray)?.forEach { Text(it.toString()) }
}

private fun String.codePointsCount(): Int = codePointCount(0, length)

/** Invalid input stays visible until corrected; picker actions never drop unknown tokens. */
private fun parseTransferIds(value: String): List<Long>? {
    if (value.isBlank()) return emptyList()
    val normalized = value.map { char -> if (char.isDigit()) Character.digit(char, 10).digitToChar() else char }.joinToString("")
    val tokens = normalized.trim().split(Regex("[\\s,،]+"))
    val ids = tokens.map { it.toLongOrNull()?.takeIf { id -> id > 0 } }
    if (ids.any { it == null }) return null
    return ids.filterNotNull().distinct()
}
