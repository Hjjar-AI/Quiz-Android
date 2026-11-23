package com.mukhtabir.android.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mukhtabir.android.shared.R
import com.mukhtabir.android.data.remote.caseTranslationLocales
import com.mukhtabir.android.data.remote.completeQuestionLocales
import com.mukhtabir.android.data.remote.questionTranslationApplied
import com.mukhtabir.android.data.remote.translationLocale
import com.mukhtabir.android.ui.components.AppFilterChip
import com.mukhtabir.android.util.bidi
import java.util.Locale
import kotlinx.serialization.json.JsonObject

/** Text preference only: never changes app locale, answer order, timers, or write state. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun QuestionLanguageSelector(translations: JsonObject, choiceCount: Int, selectedLocale: String?,
    onSelected: (String) -> Unit, enabled: Boolean = true, hasCase: Boolean = false,
    explanationAvailable: Boolean = false, explanationTranslated: Boolean = true,
    caseTranslations: JsonObject = JsonObject(emptyMap()), action: (@Composable () -> Unit)? = null) {
    val caseOptions = caseTranslationLocales(caseTranslations)
    val options = (completeQuestionLocales(translations, choiceCount) + caseOptions)
        .distinctBy { it.replace('_', '-').lowercase(Locale.ROOT) }.sorted()
    if (options.isEmpty()) {
        if(action!=null) androidx.compose.foundation.layout.Row(androidx.compose.ui.Modifier.fillMaxWidth(),
            horizontalArrangement=androidx.compose.foundation.layout.Arrangement.End) { action() }
        return
    }
    val requested = selectedLocale ?: Locale.getDefault().toLanguageTag()
    val applied = questionTranslationApplied(translations, choiceCount, requested)
    val caseTag = translationLocale(caseTranslations, requested)?.takeIf { it in caseOptions }
    val selectedTag = if (applied) translationLocale(translations, requested) else caseTag
    val anyApplied = applied || caseTag != null
    androidx.compose.foundation.layout.Row(androidx.compose.ui.Modifier.fillMaxWidth(),
        verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement=androidx.compose.foundation.layout.Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
        Text(stringResource(R.string.question_reading_language),modifier=androidx.compose.ui.Modifier.weight(1f),
            style=MaterialTheme.typography.labelLarge)
        action?.invoke()
    }
    AppActionRow {
        AppFilterChip(selected = !anyApplied, onClick = { onSelected("") }, enabled = enabled,
            label = { Text(stringResource(R.string.question_original)) })
        options.forEach { tag ->
            AppFilterChip(selected = anyApplied && tag.replace('_', '-').equals(selectedTag?.replace('_', '-'), ignoreCase = true), onClick = { onSelected(tag) },
                enabled = enabled, label = { Text(bidi(Locale.forLanguageTag(tag.replace('_', '-')).getDisplayName(Locale.getDefault()).ifBlank { tag })) })
        }
    }
    if (!applied && requested.isNotBlank()) Text(stringResource(R.string.question_showing_original), style = MaterialTheme.typography.bodySmall)
    if (anyApplied && hasCase && caseTag == null) Text(stringResource(R.string.question_case_original), style = MaterialTheme.typography.bodySmall)
    if (applied && explanationAvailable && !explanationTranslated) Text(stringResource(R.string.question_explanation_unavailable), style = MaterialTheme.typography.bodySmall)
}
