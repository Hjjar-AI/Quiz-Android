package com.mukhtabir.android.ui.masterexams

import com.mukhtabir.android.ui.components.ExplanationCard

import com.mukhtabir.android.ui.theme.AppTheme
import com.mukhtabir.android.ui.components.AppDangerTextButton

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import com.mukhtabir.android.ui.components.AppFilterChip as FilterChip
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.mukhtabir.android.ui.components.AppTextButton as TextButton
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.CategoryDto
import com.mukhtabir.android.data.remote.QuestionDto
import com.mukhtabir.android.ui.components.AppSection
import com.mukhtabir.android.ui.components.AppOutlinedButton
import com.mukhtabir.android.ui.components.MarkdownField
import com.mukhtabir.android.ui.components.MarkdownText
import com.mukhtabir.android.ui.questions.QuestionChoiceDraft

internal fun QuestionDto.examDraftForm(): EmbeddedExamDraftForm {
    val rows = choices.map { QuestionChoiceDraft(text = it) }
    return EmbeddedExamDraftForm(question, rows, rows.getOrNull((correctAnswer ?: 0) - 1)?.id,
        explanation.orEmpty(), source.orEmpty(), difficulty, category, case?.key.orEmpty(),
        case?.stem.orEmpty(), caseOrder?.toString().orEmpty())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExamDraftFields(form: EmbeddedExamDraftForm, enabled: Boolean, maxChoices: Int,
    categories: List<CategoryDto>, caseStemLocked: Boolean, change: ((EmbeddedExamDraftForm) -> EmbeddedExamDraftForm) -> Unit) {
    var preview by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
        FilterChip(preview, { preview = !preview }, label = { Text(stringResource(R.string.question_markdown_preview)) })
        if (preview) {
            MarkdownText(form.caseStem)
            com.mukhtabir.android.ui.components.QuestionTextCard(form.question)
            form.choices.forEachIndexed { index, choice ->
                com.mukhtabir.android.ui.components.QuestionChoiceCard {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.choice_number, index + 1))
                        MarkdownText(choice.text)
                        if (choice.id == form.correctId) Text(stringResource(R.string.correct_answer))
                    }
                }
            }
            ExplanationCard(form.explanation)
        }
        MarkdownField(form.question, { value -> change { it.copy(question = value) } }, stringResource(R.string.question_text), enabled, minLines = 3)
        form.choices.forEachIndexed { index, choice ->
            Column {
                MarkdownField(choice.text, { value -> change { f -> f.copy(choices = f.choices.map { if (it.id == choice.id) it.copy(text = value) else it }) } },
                    stringResource(R.string.choice_number, index + 1), enabled)
                FilterChip(choice.id == form.correctId, { change { it.copy(correctId = choice.id) } }, enabled = enabled,
                    label = { Text(stringResource(R.string.correct_answer)) })
                TextButton({ change { it.moveChoice(choice.id, -1) } }, enabled = enabled && index > 0) { Text(stringResource(R.string.move_choice_up)) }
                TextButton({ change { it.moveChoice(choice.id, 1) } }, enabled = enabled && index < form.choices.lastIndex) { Text(stringResource(R.string.move_choice_down)) }
                AppDangerTextButton({ change { it.copy(choices = it.choices.filterNot { row -> row.id == choice.id }, correctId = it.correctId.takeUnless { selected -> selected == choice.id }) } },
                    enabled = enabled && form.choices.size > 2) { Text(stringResource(R.string.remove_choice)) }
            }
        }
        AppOutlinedButton({ change { it.copy(choices = it.choices + QuestionChoiceDraft()) } }, enabled = enabled && form.choices.size < maxChoices.coerceAtLeast(2)) { Text(stringResource(R.string.add_choice)) }
        MarkdownField(form.explanation, { value -> change { it.copy(explanation = value) } }, stringResource(R.string.explanation), enabled, minLines = 2)
        AppSection(stringResource(R.string.section_organization)) {
            OutlinedTextField(form.source, { value -> change { it.copy(source = value) } }, enabled = enabled,
                label = { Text(stringResource(R.string.question_source)) }, modifier = Modifier.fillMaxWidth())
            listOf("easy" to R.string.difficulty_easy, "medium" to R.string.difficulty_medium, "hard" to R.string.difficulty_hard).forEach { (value, label) ->
                FilterChip(form.difficulty == value, { change { it.copy(difficulty = value) } }, enabled = enabled, label = { Text(stringResource(label)) })
            }
            Box {
                AppOutlinedButton({ menu = true }, enabled = enabled) { Text(stringResource(R.string.nav_categories) + ": " + (categories.firstOrNull { it.id == form.category }?.name ?: form.category?.let { com.mukhtabir.android.util.formatNumber(it) } ?: stringResource(R.string.none))) }
                DropdownMenu(menu && enabled, { menu = false }) {
                    DropdownMenuItem({ Text(stringResource(R.string.none)) }, { change { it.copy(category = null) }; menu = false })
                    categories.forEach { row -> DropdownMenuItem({ Text(row.name) }, { change { it.copy(category = row.id) }; menu = false }) }
                }
            }
        }
        AppSection(stringResource(R.string.section_case_context)) {
            Text(stringResource(R.string.editor_case_stem_hint))
            OutlinedTextField(form.caseKey, { value -> change { it.copy(caseKey = value, caseStem = "", caseOrder = "") } }, enabled = enabled,
                label = { Text(stringResource(R.string.editor_case_key)) }, modifier = Modifier.fillMaxWidth())
            MarkdownField(form.caseStem, { value -> change { it.copy(caseStem = value) } }, stringResource(R.string.editor_case_stem), enabled && !caseStemLocked, minLines = 2)
            OutlinedTextField(form.caseOrder, { value -> change { it.copy(caseOrder = value) } }, enabled = enabled,
                label = { Text(stringResource(R.string.editor_case_order)) }, modifier = Modifier.fillMaxWidth())
        }
    }
}
