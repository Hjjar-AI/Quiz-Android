package com.mukhtabir.android.ui.questions

import com.mukhtabir.android.util.QuestionContentLimits
import com.mukhtabir.android.util.contentLength

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.CategoryService
import com.mukhtabir.android.data.remote.ClinicalCaseContextDto
import com.mukhtabir.android.data.remote.QuestionDto
import com.mukhtabir.android.data.remote.QuestionService
import com.mukhtabir.android.ui.components.AppActionRow
import com.mukhtabir.android.ui.components.AppButton
import com.mukhtabir.android.ui.components.AppCategoryField
import com.mukhtabir.android.ui.components.AppConfirmationDialog
import com.mukhtabir.android.ui.components.AppDangerOutlinedButton
import com.mukhtabir.android.ui.components.AppDangerTextButton
import com.mukhtabir.android.ui.components.AppDateField
import com.mukhtabir.android.ui.components.AppFilterChip
import com.mukhtabir.android.ui.components.AppLayoutTokens
import com.mukhtabir.android.ui.components.AppLoadingIndicator
import com.mukhtabir.android.ui.components.AppOutlinedButton
import com.mukhtabir.android.ui.components.AppPageHeading
import com.mukhtabir.android.ui.components.AppSection
import com.mukhtabir.android.ui.components.AppSectionHeading
import com.mukhtabir.android.ui.components.AppStatus
import com.mukhtabir.android.ui.components.AppStatusKind
import com.mukhtabir.android.ui.components.AppTextButton
import com.mukhtabir.android.ui.components.AppTextField
import com.mukhtabir.android.ui.components.AuthoringChoiceEditor
import com.mukhtabir.android.ui.components.AuthoringRecoveryPrompt
import com.mukhtabir.android.ui.components.ErrorState
import com.mukhtabir.android.ui.components.MarkdownField
import com.mukhtabir.android.ui.components.MarkdownText
import com.mukhtabir.android.ui.components.QuestionDraftPreview
import com.mukhtabir.android.ui.components.collectAsLifecycleState
import com.mukhtabir.android.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun QuestionEditorRoute(
    questionId: Long?, questionService: QuestionService, categoryService: CategoryService,
    maxChoices: Int, userId: Long, canCreate: Boolean, canEditOwn: Boolean,
    canEditAny: Boolean, draftStore: com.mukhtabir.android.core.SessionDraftStore, onBack: () -> Unit, onSaved: (Long) -> Unit,
) {
    val vm: QuestionEditorViewModel = viewModel(
        key = "editor-$questionId-$userId",
        factory = QuestionEditorViewModel.Factory(questionId, questionService, categoryService,
            userId, canCreate, canEditOwn, canEditAny, draftStore),
    )
    val s by vm.state.collectAsLifecycleState()
    val recovery by vm.recovery.collectAsLifecycleState()
    val storageError by vm.recoveryError.collectAsLifecycleState()
    if (recovery != null && s.loaded) AuthoringRecoveryPrompt(vm::restoreDraft, vm::discardRecovery,
        restoreEnabled = !s.loading && !s.accessDenied, discardEnabled = !s.loading)
    LaunchedEffect(maxChoices) { vm.applyMaxChoices(maxChoices) }
    LaunchedEffect(s.loaded) { if(s.loaded) vm.refreshTags() }
    LaunchedEffect(s.savedId) { s.savedId?.let(onSaved) }
    var showLeave by rememberSaveable { mutableStateOf(false) }
    var preview by rememberSaveable { mutableStateOf(false) }
    val leave = { if (s.dirty) showLeave = true else onBack() }
    BackHandler(enabled = s.savedId == null) { if (!s.loading) leave() }
    if (showLeave) AppConfirmationDialog(
        onDismissRequest = { showLeave = false },
        title = { Text(stringResource(R.string.unsaved_question_title)) },
        text = { Text(stringResource(if (s.needsReview) R.string.author_leave_unknown else R.string.unsaved_question_message)) },
        confirmButton = { Column {
            AppTextButton({ showLeave = false; vm.keepDraft(onBack) }, enabled = !s.loading) { Text(stringResource(R.string.author_save_later)) }
            AppDangerTextButton({ showLeave = false; vm.discardDraft(onBack) }, enabled = !s.loading) { Text(stringResource(R.string.ux_discard_question_draft)) }
        } },
        dismissButton = { AppTextButton({ showLeave = false }) { Text(stringResource(R.string.keep_editing)) } },
    )
    if (!s.loading && s.accessDenied) {
        Column(Modifier.padding(AppLayoutTokens.cardInsets)) {
            AppStatus(stringResource(R.string.error_forbidden))
            AppTextButton(leave) { Text(stringResource(R.string.cancel)) }
        }
        return
    }
    val enabled = s.loaded && !s.loading && s.savedId == null && !s.conflict && !s.needsReview && recovery == null
    QuestionAssociationPicker(s,vm)
    QuestionKnowledgeCreator(s,vm)
    var localeInput by rememberSaveable { mutableStateOf("") }
    var removeTranslation by rememberSaveable { mutableStateOf<String?>(null) }
    var alignTranslation by rememberSaveable { mutableStateOf<String?>(null) }
    val pendingLocale=removeTranslation ?: alignTranslation
    if(pendingLocale!=null) AppConfirmationDialog(
        onDismissRequest={if(!s.loading) { removeTranslation=null;alignTranslation=null }},
        title={Text(stringResource(R.string.editor_translation_action,pendingLocale))},
        text={Text(stringResource(if(removeTranslation!=null) R.string.editor_translation_remove_confirm else R.string.editor_translation_align_confirm))},
        confirmButton={AppTextButton({
            if(removeTranslation!=null) vm.removeTranslation(pendingLocale) else vm.alignTranslation(pendingLocale)
            removeTranslation=null;alignTranslation=null
        },enabled=enabled){Text(stringResource(R.string.confirm))}},
        dismissButton={AppTextButton({removeTranslation=null;alignTranslation=null},enabled=!s.loading){Text(stringResource(R.string.cancel))}},
    )
    val validationMessage = s.validationError?.let {
        when (it) {
                QuestionEditorValidationError.INVALID_ASSOCIATION -> stringResource(R.string.editor_association_invalid)
                QuestionEditorValidationError.INVALID_FORM -> stringResource(R.string.invalid_question_form)
                QuestionEditorValidationError.TOO_MANY_CHOICES -> stringResource(R.string.too_many_choices, com.mukhtabir.android.util.formatNumber(s.maxChoices))
                QuestionEditorValidationError.TEXT_TOO_LONG -> stringResource(R.string.question_text_too_long)
                QuestionEditorValidationError.CHOICE_TOO_LONG -> stringResource(R.string.choice_text_too_long)
                QuestionEditorValidationError.DUPLICATE_CHOICES -> stringResource(R.string.duplicate_choices)
                QuestionEditorValidationError.INVALID_SOURCE_PAGE -> stringResource(R.string.editor_source_page_invalid)
                QuestionEditorValidationError.SOURCE_TOO_LONG -> stringResource(R.string.editor_source_length)
                QuestionEditorValidationError.INVALID_LOCALE -> stringResource(R.string.editor_translation_locale_invalid)
                QuestionEditorValidationError.TRANSLATION_REQUIRED -> stringResource(R.string.editor_translation_question_required)
                QuestionEditorValidationError.TRANSLATION_CHOICES -> stringResource(R.string.editor_translation_choices_required)
            }
    }
    val fieldError: (Boolean) -> String? = { invalid -> validationMessage.takeIf { invalid } }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(AppLayoutTokens.pageInsets),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
        AppPageHeading(stringResource(if (questionId == null) R.string.create_question else R.string.edit_question))
        AppTextButton(leave, enabled = !s.loading) { Text(stringResource(R.string.cancel)) }
        if (s.loading) AppLoadingIndicator()
        if (s.conflict) {
            AppStatus(stringResource(R.string.question_conflict_message), kind = AppStatusKind.WARNING)
            s.latest?.let { latest ->
                Text(stringResource(R.string.server_question_version, latest.version), style = MaterialTheme.typography.titleMedium)
                QuestionDraftPreview(latest.question, latest.choices, latest.correctAnswer, latest.explanation.orEmpty(), latest.case)
                Text(stringResource(R.string.question_metadata_comparison,
                    latest.tags.joinToString(" · "), latest.difficulty,
                    s.categories.firstOrNull { it.id == latest.category }?.name ?: latest.category?.toString().orEmpty()))
                QuestionExtendedPreview(latest)
                AppSectionHeading(stringResource(R.string.your_question_draft))
                QuestionDraftPreview(s.text, s.choices.map { it.text }, s.choices.indexOfFirst { it.id == s.correctChoiceId } + 1, s.explanation,
                    ClinicalCaseContextDto(key = s.caseKey, stem = s.caseStem))
                Text(stringResource(R.string.question_metadata_comparison, s.tags.joinToString(" · "), s.difficulty,
                    s.categories.firstOrNull { it.id == s.category }?.name ?: s.category?.toString().orEmpty()))
                QuestionExtendedPreview(QuestionDto(id=0,question=s.text,choices=s.choices.map { it.text },
                    source=s.source,sourceDocument=s.sourceDocument,sourcePage=s.sourcePage.toIntOrNull(),
                    knowledgeObject=s.knowledgeObject,knowledgeObjectTitle=s.knowledgeTitle,
                    knowledgeObjectObjective=s.knowledgeObjective,
                    case=ClinicalCaseContextDto(key=s.caseKey,stem=s.caseStem),caseOrder=s.caseOrder.toIntOrNull(),
                    translations=translationPayload(s.translations,s.choices)))
                Text(stringResource(R.string.question_conflict_resolution))
                AppOutlinedButton({ vm.resolveConflict(false) }, enabled = !s.loading) { Text(stringResource(R.string.use_server_question)) }
                AppOutlinedButton({ vm.resolveConflict(true) }, enabled = !s.loading) { Text(stringResource(R.string.keep_question_draft)) }
            } ?: AppOutlinedButton(vm::fetchLatest, enabled = !s.loading) { Text(stringResource(R.string.retry)) }
        }
        storageError?.let { Text(stringResource(R.string.author_storage_failed)); ErrorState(throwable = it) }
        if (s.needsReview) {
            Text(stringResource(R.string.author_write_unknown))
            AppOutlinedButton(vm::reviewWrite, enabled = !s.loading) { Text(stringResource(R.string.refresh)) }
            if (s.receiptMissing) AppOutlinedButton(vm::retryCreateReceipt, enabled = !s.loading) { Text(stringResource(R.string.retry_same_request)) }
            s.reviewRows.forEach { row ->
                QuestionDraftPreview(row.question, row.choices, row.correctAnswer ?: 0, row.explanation.orEmpty(), row.case)
                AppOutlinedButton({ vm.useExisting(row.id) }, enabled = !s.loading) { Text(stringResource(R.string.author_use_existing)) }
            }
            if (s.reviewReady && questionId == null) AppOutlinedButton(vm::acknowledgeWrite, enabled = !s.loading) { Text(stringResource(R.string.tools_reviewed)) }
        }
        RevisedDateControl(s.revisedDate, enabled = s.loaded && !s.loading && !s.accessDenied && !s.conflict && !s.needsReview) { vm.field("revisedDate", it) }
        AppFilterChip(selected = preview, onClick = { preview = !preview }, enabled = s.loaded && !s.loading,
            label = { Text(stringResource(R.string.question_markdown_preview)) })
        if (preview) QuestionDraftPreview(s.text, s.choices.map { it.text },
            s.choices.indexOfFirst { it.id == s.correctChoiceId } + 1, s.explanation,
            ClinicalCaseContextDto(key = s.caseKey, stem = s.caseStem))
        validationMessage?.let { message ->
            AppStatus(message,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
        s.error?.let {
            ErrorState(throwable=it,onRetry=if(!s.loaded && !s.loading) vm::load else null)
        }
        AppSection(stringResource(R.string.section_question_content), initiallyExpanded = true, revealRequest = s.validationAttempt, revealError = s.validationError in setOf(QuestionEditorValidationError.INVALID_FORM, QuestionEditorValidationError.TOO_MANY_CHOICES, QuestionEditorValidationError.TEXT_TOO_LONG, QuestionEditorValidationError.CHOICE_TOO_LONG, QuestionEditorValidationError.DUPLICATE_CHOICES)) {
            MarkdownField(s.text, { vm.field("text", it) }, enabled = enabled,
                label = stringResource(R.string.question_text), error = fieldError(s.text.isBlank() || s.text.contentLength() > QuestionContentLimits.QUESTION), minLines = 3, modifier = Modifier.fillMaxWidth())
            Text(stringResource(R.string.maximum_choices, com.mukhtabir.android.util.formatNumber(s.maxChoices)))
            s.choices.forEachIndexed { index, choice ->
                key(choice.id) {
                    AuthoringChoiceEditor(index + 1, choice.text, s.correctChoiceId == choice.id, enabled,
                        onTextChange = { vm.choice(choice.id, it) }, onSelectCorrect = { vm.selectCorrect(choice.id) },
                        onMoveUp = { vm.moveChoice(choice.id, -1) }, onMoveDown = { vm.moveChoice(choice.id, 1) },
                        onRemove = { vm.removeChoice(choice.id) }, canMoveUp = index > 0,
                        canMoveDown = index < s.choices.lastIndex, canRemove = s.choices.size > 2,
                        error = fieldError(choice.text.isBlank() || choice.text.contentLength() > QuestionContentLimits.CHOICE || s.choices.count { it.text.trim().equals(choice.text.trim(), ignoreCase = true) } > 1),
                        correctModifier = Modifier.semantics { if (validationMessage != null && s.correctChoiceId == null) error(validationMessage) })
                }
            }
            AppOutlinedButton(vm::addChoice, enabled = enabled && s.choices.size < s.maxChoices) { Text(stringResource(R.string.add_choice)) }
        }
        AppSection(stringResource(R.string.explanation), initiallyExpanded = true, revealRequest = s.validationAttempt, revealError = validationMessage != null && s.explanation.contentLength() > QuestionContentLimits.EXPLANATION) {
            MarkdownField(s.explanation, { vm.field("explanation", it) }, enabled = enabled,
                label = stringResource(R.string.explanation), error = fieldError(s.explanation.contentLength() > QuestionContentLimits.EXPLANATION), minLines = 2, modifier = Modifier.fillMaxWidth())
        }
        AppSection(stringResource(R.string.section_reference), revealRequest = s.validationAttempt, revealError = s.validationError in setOf(QuestionEditorValidationError.INVALID_SOURCE_PAGE, QuestionEditorValidationError.SOURCE_TOO_LONG)) {
            AppTextField(s.source,{vm.field("source",it)},enabled=enabled,
                label={Text(stringResource(R.string.question_source))},isError=fieldError(s.source.length > 200)!=null, supportingText=fieldError(s.source.length > 200)?.let { message -> { Text(message) } },modifier=Modifier.fillMaxWidth())
            AppTextField(s.sourceDocument,{vm.field("sourceDocument",it)},enabled=enabled,
                label={Text(stringResource(R.string.editor_source_document))},isError=fieldError(s.sourceDocument.length > 500)!=null, supportingText=fieldError(s.sourceDocument.length > 500)?.let { message -> { Text(message) } },modifier=Modifier.fillMaxWidth())
            AppTextField(s.sourcePage,{vm.field("sourcePage",it)},enabled=enabled,
                label={Text(stringResource(R.string.editor_source_page))},isError=fieldError(s.validationError == QuestionEditorValidationError.INVALID_SOURCE_PAGE)!=null, supportingText=fieldError(s.validationError == QuestionEditorValidationError.INVALID_SOURCE_PAGE)?.let { message -> { Text(message) } },modifier=Modifier.fillMaxWidth())
        }
        AppSection(stringResource(R.string.section_associations), revealRequest = s.validationAttempt, revealError = s.validationError == QuestionEditorValidationError.INVALID_ASSOCIATION) {
            Text(stringResource(R.string.editor_image_after_save))
            QuestionAssociationFields(s,vm,enabled)
        }
        AppSection(stringResource(R.string.editor_translations), revealRequest = s.validationAttempt, revealError = s.validationError in setOf(QuestionEditorValidationError.INVALID_LOCALE, QuestionEditorValidationError.TRANSLATION_REQUIRED, QuestionEditorValidationError.TRANSLATION_CHOICES) || (validationMessage != null && s.translations.values.any { it.question.contentLength() > QuestionContentLimits.QUESTION || it.explanation.contentLength() > QuestionContentLimits.EXPLANATION || it.choices.values.any { text -> text.contentLength() > QuestionContentLimits.CHOICE } })) {
            Text(stringResource(R.string.editor_translation_hint))
            AppTextField(localeInput,{localeInput=it},enabled=enabled,
                label={Text(stringResource(R.string.editor_translation_locale))},isError=s.validationError==QuestionEditorValidationError.INVALID_LOCALE, supportingText=fieldError(s.validationError==QuestionEditorValidationError.INVALID_LOCALE)?.let { message -> { Text(message) } },modifier=Modifier.fillMaxWidth(),singleLine=true)
            AppOutlinedButton({vm.addTranslation(localeInput)},enabled=enabled && localeInput.isNotBlank()) { Text(stringResource(R.string.editor_translation_add)) }
            s.translations.forEach { (locale,translation) ->
                key(locale) {
                    Text(locale,style=MaterialTheme.typography.titleMedium)
                    MarkdownField(translation.question,{vm.translationField(locale,"question",it)},enabled=enabled,
                        label=stringResource(R.string.question_text),error=fieldError((s.validationError==QuestionEditorValidationError.TRANSLATION_REQUIRED && translation.question.isBlank()) || translation.question.contentLength() > QuestionContentLimits.QUESTION),minLines=2,modifier=Modifier.fillMaxWidth())
                    s.choices.forEachIndexed { index,choice ->
                        MarkdownField(translation.choices[choice.id].orEmpty(),{vm.translationChoice(locale,choice.id,it)},enabled=enabled,
                            label=stringResource(R.string.choice_number, com.mukhtabir.android.util.formatNumber(index+1)),error=fieldError((s.validationError==QuestionEditorValidationError.TRANSLATION_CHOICES && translation.choices[choice.id].isNullOrBlank()) || translation.choices[choice.id].orEmpty().contentLength() > QuestionContentLimits.CHOICE),modifier=Modifier.fillMaxWidth())
                    }
                    if(translation.extraChoices.isNotEmpty()) {
                        AppStatus(stringResource(R.string.editor_translation_extra))
                        translation.extraChoices.forEach { MarkdownText(it) }
                        AppOutlinedButton({alignTranslation=locale},enabled=enabled) { Text(stringResource(R.string.editor_translation_align)) }
                    }
                    MarkdownField(translation.explanation,{vm.translationField(locale,"explanation",it)},enabled=enabled,
                        label=stringResource(R.string.explanation),error=fieldError(translation.explanation.contentLength() > QuestionContentLimits.EXPLANATION),minLines=2,modifier=Modifier.fillMaxWidth())
                    if(preview) QuestionDraftPreview(translation.question,translatedChoices(translation,s.choices),
                        s.choices.indexOfFirst { it.id==s.correctChoiceId }+1,translation.explanation,
                        ClinicalCaseContextDto(key = s.caseKey, stem = s.caseStem))
                    AppDangerOutlinedButton({removeTranslation=locale},enabled=enabled) { Text(stringResource(R.string.editor_translation_remove)) }
                }
            }
        }
        AppSection(stringResource(R.string.section_organization), revealRequest = s.validationAttempt, revealError = s.validationError==QuestionEditorValidationError.INVALID_FORM && s.difficulty !in listOf("easy", "medium", "hard")) {
            QuestionTagFields(s,vm,enabled)
            Text(stringResource(R.string.difficulty))
            AppActionRow() {
                listOf("easy" to R.string.difficulty_easy, "medium" to R.string.difficulty_medium, "hard" to R.string.difficulty_hard).forEach { (value, label) ->
                    AppFilterChip(selected = s.difficulty == value, onClick = { vm.field("difficulty", value) },
                        enabled = enabled, label = { Text(stringResource(label)) })
                }
            }
            AppCategoryField(s.categories, s.category, vm::category, enabled = enabled)
        }
        AppButton(vm::save, enabled = enabled, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.save)) }
    }
}

@Composable
private fun RevisedDateControl(value: String, enabled: Boolean, onChange: (String) -> Unit) {
    AppDateField(stringResource(R.string.author_revised_date) + ": " + value.ifBlank { stringResource(R.string.author_default_date) },
        initial = { runCatching { java.time.LocalDate.parse(value) }.getOrElse { java.time.LocalDate.now() } },
        enabled = enabled, onPicked = { onChange(it.toString()) })
}
