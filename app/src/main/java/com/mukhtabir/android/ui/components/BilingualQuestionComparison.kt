@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.mukhtabir.android.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.QuestionDto
import com.mukhtabir.android.data.remote.caseTranslationLocales
import com.mukhtabir.android.data.remote.completeQuestionLocales
import com.mukhtabir.android.data.remote.localizedForDisplay
import com.mukhtabir.android.data.remote.questionTranslationApplied
import com.mukhtabir.android.data.remote.translationFor
import com.mukhtabir.android.data.remote.translationLocale
import com.mukhtabir.android.ui.theme.AppTheme
import com.mukhtabir.android.util.bidi
import java.util.Locale
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** No writes or grading. Callers own recall-choice and explanation reveal permissions. */
@Composable
fun BilingualQuestionComparison(question: QuestionDto, choicesVisible: Boolean, explanationVisible: Boolean,
    feedbackTranslations: JsonObject = JsonObject(emptyMap())) {
    var expanded by remember(question.id) { mutableStateOf(false) }
    val readable = question.copy(translations = JsonObject(question.translations.mapValues { (tag, raw) ->
        val explanation = translationFor(feedbackTranslations, tag)?.get("explanation")
        if (raw is JsonObject && explanation is JsonPrimitive && explanation.contentOrNull?.isNotBlank() == true)
            JsonObject(raw + ("explanation" to explanation)) else raw
    }))
    val locales = (completeQuestionLocales(readable.translations, readable.choices.size) +
        caseTranslationLocales(question.case?.translations ?: JsonObject(emptyMap()))).distinct()
    var locale by remember(question.id, locales) { mutableStateOf(
        translationLocale(question.translations, Locale.getDefault().toLanguageTag())?.takeIf { it in locales } ?: locales.firstOrNull().orEmpty()) }
    AppTextButton({ expanded = !expanded }) { Text(stringResource(R.string.flow_compare)) }
    if (!expanded) return
    if (locales.isEmpty()) { Text(stringResource(R.string.flow_no_translation)); return }
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.content)) {
        Text(stringResource(R.string.flow_compare_hint), style = MaterialTheme.typography.bodySmall)
        AppActionRow { locales.forEach { tag -> AppFilterChip(locale == tag, { locale = tag }, label = {
            Text(bidi(Locale.forLanguageTag(tag.replace('_', '-')).getDisplayName(Locale.getDefault())))
        }) } }
        val original = readable.localizedForDisplay("")
        val translated = readable.localizedForDisplay(locale)
        if (!questionTranslationApplied(readable.translations, readable.choices.size, locale))
            Text(stringResource(R.string.question_showing_original), style = MaterialTheme.typography.bodySmall)
        question.case?.let { context ->
            val fields = translationFor(context.translations, locale)
            val missingStem = !context.stem.isNullOrBlank() && (fields?.get("stem") as? JsonPrimitive)?.contentOrNull.isNullOrBlank()
            val missingTitle = !context.title.isNullOrBlank() && (fields?.get("title") as? JsonPrimitive)?.contentOrNull.isNullOrBlank()
            if (missingStem || missingTitle) Text(stringResource(R.string.question_case_original), style = MaterialTheme.typography.bodySmall)
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val readingScale = com.mukhtabir.android.ui.theme.LocalReadingComfort.current.readingSize / 100f
            if (!AppAdaptiveTokens.stack(maxWidth, AppLayoutTokens.comparisonBreakpoint, LocalDensity.current.fontScale * readingScale)) {
                Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.content)) {
                    ComparisonPane(original, stringResource(R.string.question_original), choicesVisible, explanationVisible, Modifier.weight(1f))
                    ComparisonPane(translated, stringResource(R.string.flow_translation, bidi(locale)), choicesVisible, explanationVisible, Modifier.weight(1f))
                }
            } else Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
                ComparisonPane(original, stringResource(R.string.question_original), choicesVisible, explanationVisible)
                ComparisonPane(translated, stringResource(R.string.flow_translation, bidi(locale)), choicesVisible, explanationVisible)
            }
        }
        if (explanationVisible && !question.answersHidden && !question.explanation.isNullOrBlank() && translated.explanation.isNullOrBlank())
            Text(stringResource(R.string.question_explanation_unavailable), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ComparisonPane(question: QuestionDto, title: String, choicesVisible: Boolean, explanationVisible: Boolean,
    modifier: Modifier = Modifier) {
    AppCard(modifier) { AppCardBody {
        Text(title, style = MaterialTheme.typography.titleSmall)
        ClinicalCaseContext(question.case)
        QuestionTextCard(question.question)
        if (choicesVisible) question.choices.forEachIndexed { i, text -> AnswerChoice(i + 1, text, interactiveText = true) }
        if (explanationVisible && !question.answersHidden) ExplanationCard(question.explanation.orEmpty(), title = stringResource(R.string.explanation))
    } }
}
