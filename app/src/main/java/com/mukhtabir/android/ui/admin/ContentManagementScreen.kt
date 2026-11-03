@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.mukhtabir.android.ui.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mukhtabir.android.R
import com.mukhtabir.android.core.has
import com.mukhtabir.android.data.remote.*
import com.mukhtabir.android.ui.components.*
import com.mukhtabir.android.ui.components.AppActionRow
import com.mukhtabir.android.ui.components.AppConfirmationDialog
import com.mukhtabir.android.ui.components.AppDangerOutlinedButton
import com.mukhtabir.android.ui.components.AppFilterChip
import com.mukhtabir.android.ui.components.AppLayoutTokens
import com.mukhtabir.android.ui.components.AppLoadingIndicator
import com.mukhtabir.android.ui.components.AppPageHeading
import com.mukhtabir.android.ui.components.AppPagination
import com.mukhtabir.android.ui.components.AppStatus
import com.mukhtabir.android.ui.components.AppSuccessText
import com.mukhtabir.android.ui.components.AppTextButton
import com.mukhtabir.android.ui.components.AppTextField
import com.mukhtabir.android.ui.components.ExactTagEditor
import com.mukhtabir.android.ui.components.ScreenBusyEffect
import com.mukhtabir.android.ui.components.collectAsLifecycleState
import com.mukhtabir.android.ui.theme.AppTheme

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ContentManagementRoute(kind: ContentKind, service: ContentManagementService, categories: CategoryService, user: UserDto,
    pageSize: Int, store: com.mukhtabir.android.core.SessionDraftStore, onBack: () -> Unit, onBusy: (Boolean) -> Unit, onChanged: () -> Unit,
    onQuestion: (Long) -> Unit, onReadKnowledge: (Long) -> Unit, onStem: (String) -> Unit) {
    val vm: ContentManagementViewModel = viewModel(key = "content-management-$kind-${user.id}",
        factory = ContentManagementViewModel.Factory(kind, service, categories, user, pageSize, store))
    vm.permissions(user)
    val s by vm.state.collectAsLifecycleState()
    val recoveryBlocked=editorRecoveryBlocked(vm.recovery)
    RefreshOnResume { if(s.editing) vm.refreshDetail() else vm.refresh() }
    var confirmation by remember { mutableStateOf<String?>(null) }
    var caseLinksPage by remember(s.selectedCase?.id) { mutableStateOf(1) }
    val busy = s.busy || s.loading
    val back: () -> Unit = { if (!busy) {
        if (s.dirty || s.uncertain) confirmation = "leave"
        else if (s.editing) vm.close() else onBack()
    } }
    BackHandler(onBack = back)
    ScreenBusyEffect(busy, onBusy)
    LaunchedEffect(s.saved) { if (s.saved) onChanged() }
    if (!mayManageContent(user, kind)) { Text(stringResource(R.string.error_forbidden)); return }
    val knowledge = kind == ContentKind.KNOWLEDGE
    val editable = !recoveryBlocked && !busy && !s.uncertain && !s.conflict &&
        if (knowledge) vm.mayWriteKnowledge(s.selectedKnowledge) else vm.mayWriteCase(s.selectedCase)
    if (confirmation != null) AppConfirmationDialog(onDismissRequest = { if (!busy) confirmation = null },
        title = { Text(stringResource(if (confirmation == "delete") {
            if (knowledge) R.string.ux_delete_knowledge else R.string.ux_delete_case
        } else R.string.confirm)) },
        text = { Text(stringResource(when (confirmation) {
            "delete" -> if (knowledge) R.string.content_delete_knowledge else R.string.content_delete_case
            else -> R.string.manage_leave_confirm
        }) + if(confirmation == "delete") "\n" + stringResource(R.string.content_linked_count, com.mukhtabir.android.util.formatNumber(if (knowledge) s.selectedKnowledge?.questionCount ?: 0 else s.selectedCase?.questionCount ?: 0)) else "") },
        confirmButton = { AppConfirmationButton(destructive = confirmation == "delete", onClick = { val action = confirmation; confirmation = null
            if (action == "delete") vm.delete() else if (s.uncertain) onBack() else vm.close()
        }, enabled = !busy) { Text(stringResource(if (confirmation == "delete") {
            if (knowledge) R.string.ux_delete_knowledge else R.string.ux_delete_case
        } else R.string.confirm)) } },
        dismissButton = { AppTextButton({ confirmation = null }, enabled = !busy) { Text(stringResource(R.string.cancel)) } })
    LazyColumn(Modifier.fillMaxSize().padding(AppLayoutTokens.pageInsets), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
        item { EditorRecoveryControls(vm.recovery,busy,vm::restore) }
        item {
            AppPageHeading(stringResource(if (knowledge) R.string.content_knowledge_title else R.string.content_cases_title))
            AppOutlinedButton(back, enabled = !busy) { Text(stringResource(R.string.back)) }
            if (busy) AppLoadingIndicator()
            s.error?.let { ErrorState(throwable = it, onRetry = if (!busy) vm::retryList else null) }
            s.actionError?.let { ErrorState(throwable = it) }
            if (s.saved) AppSuccessText(stringResource(R.string.saved))
            s.validation?.let { AppStatus(stringResource(it)) }
            if (s.uncertain) {
                Text(stringResource(R.string.content_unknown))
                if(s.uncertainCreate && s.receiptMissing) AppOutlinedButton(vm::retryCreate, enabled = !busy && !recoveryBlocked) { Text(stringResource(R.string.retry_same_request)) }
                AppOutlinedButton(vm::reconcile, enabled = !busy) { Text(stringResource(R.string.refresh)) }
                if (s.noticeMissing) Text(stringResource(if(s.uncertainCreate) R.string.recovery_target_gone else R.string.content_missing))
                if (s.uncertainCreate && s.reviewed) s.knowledge.forEach { match -> Text(com.mukhtabir.android.util.contentIdentity(match.title, match.id)) }
                AppOutlinedButton(vm::acknowledge, enabled = !busy && s.reviewed) { Text(stringResource(R.string.content_reviewed)) }
            }
            if (s.conflict) {
                Text(stringResource(R.string.content_conflict))
                s.selectedKnowledge?.let {Text("${com.mukhtabir.android.util.bidi(it.title)} · ${com.mukhtabir.android.util.bidi(it.learningObjective)}")}
                s.selectedCase?.let {Text("${com.mukhtabir.android.util.bidi(it.title.orEmpty())} · ${com.mukhtabir.android.util.bidi(it.stem.orEmpty())}")}
                AppOutlinedButton(vm::refreshDetail, enabled=!busy && !recoveryBlocked) {Text(stringResource(R.string.refresh))}
                AppOutlinedButton({ vm.resolve(true) }, enabled = !busy && !recoveryBlocked && s.reviewed) { Text(stringResource(R.string.content_keep_draft)) }
                AppOutlinedButton({ vm.resolve(false) }, enabled = !busy && !recoveryBlocked && s.reviewed) { Text(stringResource(R.string.content_use_server)) }
            }
        }
        if (!s.editing) {
            item { AppSection(stringResource(R.string.search), initiallyExpanded = true) {
                AppTextField(s.query, vm::query, enabled = !recoveryBlocked && !busy && !s.uncertain, label = { Text(stringResource(R.string.search)) }, modifier = Modifier.fillMaxWidth())
                if (knowledge) {
                    AppActionRow() {
                        listOf("", "draft", "active", "retired").forEach { status -> AppFilterChip(s.status == status,
                            { vm.filter(status, s.category) }, { Text(contentStatus(status)) }, enabled = !recoveryBlocked && !busy && !s.uncertain) }
                    }
                    AppSection(stringResource(R.string.select_categories)) {
                        AppFilterChip(s.category == null, { vm.filter(s.status, null) }, { Text(stringResource(R.string.none)) }, enabled = !busy)
                        s.categories.forEach { category -> AppFilterChip(s.category == category.id, { vm.filter(s.status, category.id) }, { Text(category.name) }, enabled = !busy) }
                    }
                }
                AppOutlinedButton(vm::search, enabled = !recoveryBlocked && !busy && !s.uncertain) { Text(stringResource(R.string.search)) }
                Text(stringResource(R.string.content_applied_search, s.appliedQuery.ifBlank { stringResource(R.string.none) }))
                if(knowledge) {
                    Text(contentStatus(s.appliedStatus))
                    Text(stringResource(R.string.content_applied_category, s.categories.find { it.id==s.appliedCategory }?.name
                        ?: s.appliedCategory?.toString() ?: stringResource(R.string.none)))
                }
                Text(stringResource(R.string.content_page_count, com.mukhtabir.android.util.formatNumber(if (knowledge) s.knowledge.size else s.cases.size), com.mukhtabir.android.util.formatNumber(s.total)))
            } }
            if (knowledge && vm.mayWriteKnowledge(null)) item { AppButton(vm::create, enabled = !recoveryBlocked && !busy && !s.uncertain) { Text(stringResource(R.string.create)) } }
            if (knowledge) items(s.knowledge, key = { it.id }) { row -> AppCard {
                Column(Modifier.padding(AppLayoutTokens.cardInsets), verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
                    Text(row.title, style = MaterialTheme.typography.titleMedium)
                    AppMetadataRow(listOf(contentStatus(row.status),stringResource(R.string.content_linked_count, com.mukhtabir.android.util.formatNumber(row.questionCount))))
                    AppOutlinedButton({ vm.open(row.id, null) }, enabled = !recoveryBlocked && !busy && !s.uncertain) { Text(stringResource(R.string.content_open)) }
                }
            } } else items(s.cases, key = { it.id }) { row -> AppCard {
                Column(Modifier.padding(AppLayoutTokens.cardInsets), verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
                    Text(row.title ?: row.key, style = MaterialTheme.typography.titleMedium)
                    AppMetadataRow(listOf(row.key,stringResource(R.string.content_linked_count, com.mukhtabir.android.util.formatNumber(row.questionCount))))
                    AppOutlinedButton({ vm.open(null, row.key) }, enabled = !recoveryBlocked && !busy && !s.uncertain) { Text(stringResource(R.string.content_open)) }
                }
            } }
            item { ContentPager(s.page, vm.pages(), !busy && !recoveryBlocked && !s.uncertain, vm::page) }
        } else {
            if (knowledge) item { KnowledgeFields(s, vm, editable) }
            else item { AppSection(stringResource(R.string.content_case_title), initiallyExpanded = true) {
                AppTextField(s.caseTitle, { vm.field("caseTitle", it) }, enabled = editable,
                    label = { Text(stringResource(R.string.content_case_title)) }, modifier = Modifier.fillMaxWidth())
                s.selectedCase?.let { row ->
                    Text(row.key); MarkdownText(row.stem.orEmpty())
                    AppOutlinedButton({ onStem(row.key) }, enabled = editable && !s.dirty) { Text(stringResource(R.string.content_edit_stem)) }
                    if (s.dirty) Text(stringResource(R.string.content_save_title_first))
                }
            } }
            if (!knowledge) item { CaseTranslationFields(s, vm, editable) }
            item { AppActionRow() {
                AppButton(vm::save, enabled = editable && (s.dirty || s.selectedKnowledge == null && knowledge)) { Text(stringResource(R.string.save)) }
                if (s.selectedKnowledge != null || s.selectedCase != null) AppDangerOutlinedButton({ confirmation = "delete" },
                    enabled = editable && (knowledge || user.has("questions.edit_case_stem_any"))) { Text(stringResource(if (knowledge) R.string.ux_delete_knowledge else R.string.ux_delete_case)) }
            } }
            item { AppSection(stringResource(R.string.content_linked_questions), initiallyExpanded = true) {
                if (knowledge) {
                    s.selectedKnowledge?.let { row ->
                        AppOutlinedButton({ onReadKnowledge(row.id) }, enabled = !busy && !s.dirty && !s.uncertain) { Text(stringResource(R.string.content_read)) }
                        AppOutlinedButton({ vm.linked() }, enabled = !busy) { Text(stringResource(R.string.refresh)) }
                    }
                    s.linkedError?.let { ErrorState(throwable = it, onRetry = if (!busy) ({ vm.linked(s.linkedRequestedPage) }) else null) }
                    s.linked?.let { page ->
                        if (page.items.isEmpty() && !busy && s.linkedError==null) EmptyState(stringResource(R.string.no_items))
                        page.items.forEach { question -> AppOutlinedButton({ onQuestion(question.id) }, enabled = !busy && !s.dirty && !s.uncertain) {
                            Text(question.localizedForDisplay().question.take(180))
                        } }
                        ContentPager(page.page, page.totalPages, !busy, vm::linked)
                    }
                } else s.selectedCase?.questions?.let { questions ->
                    val pages=((questions.size+vm.size-1)/vm.size).coerceAtLeast(1)
                    val page=caseLinksPage.coerceIn(1,pages)
                    if(questions.isEmpty() && !busy && s.error==null) EmptyState(stringResource(R.string.no_items))
                    questions.drop((page-1)*vm.size).take(vm.size).forEach { question -> AppOutlinedButton({ onQuestion(question.id) }, enabled = !busy && !s.dirty && !s.uncertain) {
                        Text(question.localizedForDisplay().question.take(180))
                    } }
                    ContentPager(page,pages,!busy) {caseLinksPage=it}
                }
            } }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun KnowledgeFields(s: ContentManagementState, vm: ContentManagementViewModel, enabled: Boolean) {
    val f = s.form; var locale by remember { mutableStateOf("ar") }
    AppSection(stringResource(R.string.content_knowledge_title), initiallyExpanded = true) {
        AppTextField(f.title, { vm.field("title", it) }, enabled = enabled, label = { Text(stringResource(R.string.knowledge_create_name)) }, modifier = Modifier.fillMaxWidth())
        MarkdownField(f.objective, { vm.field("objective", it) }, stringResource(R.string.explore_objective), enabled, modifier = Modifier.fillMaxWidth())
        MarkdownField(f.answer, { vm.field("answer", it) }, stringResource(R.string.explore_reference_answer), enabled, modifier = Modifier.fillMaxWidth())
        AppTextField(f.facts, { vm.field("facts", it) }, enabled = enabled, label = { Text(stringResource(R.string.knowledge_create_facts)) }, minLines = 2, modifier = Modifier.fillMaxWidth())
        AppTextField(f.misconceptions, { vm.field("misconceptions", it) }, enabled = enabled, label = { Text(stringResource(R.string.explore_misconceptions)) }, minLines = 2, modifier = Modifier.fillMaxWidth())
        Text(stringResource(R.string.content_one_per_line))
        AppActionRow() { listOf("draft", "active", "retired").forEach { status ->
            AppFilterChip(f.status == status, { vm.field("status", status) }, { Text(contentStatus(status)) }, enabled = enabled)
        } }
    }
    AppSection(stringResource(R.string.select_categories)) {
        s.lookupError?.let { ErrorState(throwable = it, onRetry = vm::loadCategories) }
        AppFilterChip(f.category == null, { vm.category(null) }, { Text(stringResource(R.string.none)) }, enabled = enabled)
        s.categories.forEach { category -> AppFilterChip(f.category == category.id, { vm.category(category.id) }, { Text(category.name) }, enabled = enabled) }
        if(f.category != null && s.categories.none { it.id == f.category }) Text(stringResource(R.string.planner_category_unavailable, com.mukhtabir.android.util.formatNumber(f.category)))
    }
    AppSection(stringResource(R.string.content_tags)) {
        ExactTagEditor(f.tags, f.tagInput, { vm.field("tagInput", it) },
            onAdd = { vm.tag(true, it) }, onRemove = { vm.tag(false, it) }, enabled = enabled)
    }
    AppSection(stringResource(R.string.content_source)) {
        AppTextField(f.document, { vm.field("document", it) }, enabled = enabled, label = { Text(stringResource(R.string.editor_source_document)) }, modifier = Modifier.fillMaxWidth())
        AppTextField(f.page, { vm.field("page", it) }, enabled = enabled, label = { Text(stringResource(R.string.editor_source_page)) }, modifier = Modifier.fillMaxWidth())
        AppTextField(f.revised, { vm.field("revised", it) }, enabled = enabled, label = { Text(stringResource(R.string.content_review_date)) }, modifier = Modifier.fillMaxWidth())
    }
    AppSection(stringResource(R.string.content_translations)) {
        TranslationLocaleChoices(locale, f.translations.keys, { locale = it }, enabled)
        val fields = f.translations[locale] as? kotlinx.serialization.json.JsonObject
        listOf("title" to R.string.knowledge_create_name, "learning_objective" to R.string.explore_objective, "canonical_answer" to R.string.explore_reference_answer).forEach { (field, label) ->
            val text = (fields?.get(field) as? kotlinx.serialization.json.JsonPrimitive)?.content.orEmpty()
            MarkdownField(text, { vm.translation(locale, field, it) }, stringResource(label), enabled, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun contentStatus(status: String): String = stringResource(when (status) {
    "draft" -> R.string.content_status_draft; "active" -> R.string.active; "retired" -> R.string.content_status_retired; else -> R.string.content_all
})
@Composable
private fun ContentPager(page: Int, total: Int, enabled: Boolean, onPage: (Int) -> Unit) {
    if(total <= 1) return
    AppPagination(page, total, onPrevious = { onPage(page - 1) }, onNext = { onPage(page + 1) }, previousEnabled = enabled, nextEnabled = enabled)
}

/** Optional authoring fields only; uses the existing reader language preference. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun CaseTranslationFields(s: ContentManagementState, vm: ContentManagementViewModel, enabled: Boolean) {
    var locale by rememberSaveable(s.selectedCase?.key) { mutableStateOf("ar") }
    AppSection(stringResource(R.string.content_translations)) {
        Text(stringResource(R.string.case_translation_hint))
        TranslationLocaleChoices(locale, s.form.translations.keys, { locale = it }, enabled)
        val fields = s.form.translations[locale] as? kotlinx.serialization.json.JsonObject
        listOf("title" to R.string.content_case_title, "stem" to R.string.case_translation_stem).forEach { (field, label) ->
            val text = (fields?.get(field) as? kotlinx.serialization.json.JsonPrimitive)?.content.orEmpty()
            MarkdownField(text, { vm.translation(locale, field, it) }, stringResource(label), enabled, modifier = Modifier.fillMaxWidth())
        }
    }
}
