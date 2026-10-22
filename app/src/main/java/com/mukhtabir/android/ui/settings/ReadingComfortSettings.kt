package com.mukhtabir.android.ui.settings

import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mukhtabir.android.R
import com.mukhtabir.android.core.AppPreferences
import com.mukhtabir.android.core.DisplayPreferences
import com.mukhtabir.android.ui.components.AnswerChoice
import com.mukhtabir.android.ui.components.AppActionRow
import com.mukhtabir.android.ui.components.AppButton
import com.mukhtabir.android.ui.components.AppCard
import com.mukhtabir.android.ui.components.AppCardBody
import com.mukhtabir.android.ui.components.AppFilterChip
import com.mukhtabir.android.ui.components.AppLayoutTokens
import com.mukhtabir.android.ui.components.AppOutlinedButton
import com.mukhtabir.android.ui.components.AppPageHeading
import com.mukhtabir.android.ui.components.AppSection
import com.mukhtabir.android.ui.components.AppSectionHeading
import com.mukhtabir.android.ui.components.ExplanationCard
import com.mukhtabir.android.ui.components.QuestionTextCard
import com.mukhtabir.android.ui.theme.AppTheme
import com.mukhtabir.android.util.formatNumber
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun ReadingComfortSettings(preferences: AppPreferences, display: DisplayPreferences) {
    var preview by rememberSaveable { mutableStateOf(false) }
    AppCard {
        AppCardBody {
            AppSectionHeading(stringResource(R.string.reading_title))
            Text(stringResource(R.string.reading_intro), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            AppButton({ preview = true }) { Text(stringResource(R.string.reading_preview)) }
        }
    }
    AppSection(stringResource(R.string.reading_controls), subtitle = stringResource(R.string.reading_summary,
        formatNumber(display.readingSize), formatNumber(display.readingLeading))) {
        Text(stringResource(R.string.reading_scope), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        ReadingSlider(stringResource(R.string.reading_size), display.readingSize, 90f..160f, 6) {
            preferences.setDisplay(display.copy(readingSize = it))
        }
        ReadingSlider(stringResource(R.string.reading_leading), display.readingLeading, 100f..160f, 5) {
            preferences.setDisplay(display.copy(readingLeading = it))
        }
        AppSectionHeading(stringResource(R.string.reading_paragraphs))
        AppActionRow {
            listOf(100 to R.string.reading_standard, 150 to R.string.reading_relaxed, 200 to R.string.reading_spacious).forEach { (value, label) ->
                AppFilterChip(display.readingParagraphs == value,
                    { preferences.setDisplay(display.copy(readingParagraphs = value)) }, { Text(stringResource(label)) })
            }
        }
        AppSectionHeading(stringResource(R.string.reading_width))
        Text(stringResource(R.string.reading_width_hint), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        AppActionRow {
            listOf("full" to R.string.reading_full, "comfortable" to R.string.reading_comfortable,
                "focused" to R.string.reading_focused).forEach { (value, label) ->
                AppFilterChip(display.readingWidth == value,
                    { preferences.setDisplay(display.copy(readingWidth = value)) }, { Text(stringResource(label)) })
            }
        }
        AppSectionHeading(stringResource(R.string.preferences_font))
        Text(stringResource(R.string.reading_font_hint), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        AppActionRow {
            listOf("default", "system", "noto-arabic", "inter", "outfit", "tajawal", "cairo", "ibm-plex-arabic", "amiri").forEach { font ->
                AppFilterChip(display.font == font, { preferences.setDisplay(display.copy(font = font)) }, {
                    Text(when (font) {
                        "default" -> stringResource(R.string.font_default)
                        "system" -> stringResource(R.string.font_system)
                        "noto-arabic" -> "Noto Sans Arabic"
                        "ibm-plex-arabic" -> "IBM Plex Sans Arabic"
                        else -> font.replaceFirstChar { it.uppercase() }
                    })
                })
            }
        }
        QuestionTextCard(stringResource(R.string.settings_font_sample))
        AppOutlinedButton(preferences::resetReading) { Text(stringResource(R.string.reading_reset)) }
    }
    if (preview) StudyReadingPreview { preview = false }
}

@Composable
private fun ReadingSlider(label: String, value: Int, range: ClosedFloatingPointRange<Float>, steps: Int,
    onChange: (Int) -> Unit) {
    val percentage = stringResource(R.string.reading_percent, formatNumber(value))
    Column {
        AppSectionHeading("$label · $percentage")
        Slider(value.toFloat(), { onChange(it.roundToInt()) }, valueRange = range, steps = steps,
            modifier = Modifier.fillMaxWidth().semantics {
                contentDescription = label
                stateDescription = percentage
            })
    }
}

/** Public bundled material only; selecting an answer never starts a session or writes progress. */
@Composable
private fun StudyReadingPreview(onClose: () -> Unit) {
    var selected by rememberSaveable { mutableStateOf(-1) }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().safeDrawingPadding().padding(AppLayoutTokens.pageInsets),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
                AppOutlinedButton(onClose) { Text(stringResource(R.string.close)) }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.content)) {
                    AppPageHeading(stringResource(R.string.reading_preview), stringResource(R.string.reading_preview_hint))
                    QuestionTextCard(stringResource(R.string.demo_q1))
                    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
                    listOf(R.string.demo_q1_c1, R.string.demo_q1_c2, R.string.demo_q1_c3, R.string.demo_q1_c4).forEachIndexed { index, label ->
                        AnswerChoice(index + 1, stringResource(label), selected = selected == index,
                            onClick = { selected = index },
                            answerCorrect = if (selected < 0) null else if (index == 0) true else if (selected == index) false else null)
                    }
                    }
                    if (selected >= 0) ExplanationCard(stringResource(R.string.demo_q1_explanation))
                }
            }
        }
    }
}
