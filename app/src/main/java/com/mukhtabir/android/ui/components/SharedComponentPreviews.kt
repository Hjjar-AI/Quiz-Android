@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.mukhtabir.android.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.mukhtabir.android.R
import com.mukhtabir.android.core.DisplayPreferences
import com.mukhtabir.android.ui.theme.MukhtabirTheme

/** Public localized sample content only; no services, private state or writes. */
@Preview(name = "Light", showBackground = true, widthDp = 360)
@Preview(name = "Dark", showBackground = true, widthDp = 360,
    uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Arabic RTL", showBackground = true, widthDp = 360, locale = "ar")
@Preview(name = "Large text", showBackground = true, widthDp = 360, fontScale = 2f)
@Preview(name = "Narrow Arabic large text", showBackground = true, widthDp = 300, locale = "ar", fontScale = 2f)
@Composable
private fun SharedComponentPreviews(compact: Boolean = false) {
    MukhtabirTheme(display = DisplayPreferences(compact = compact)) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(AppLayoutTokens.pageInsets),
                verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
                AppCard(Modifier.fillMaxWidth()) {
                    AppCardBody { Text(stringResource(R.string.reading_preview_hint)) }
                }
                Text(stringResource(R.string.reading_preview_hint), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.reading_preview_hint), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.reading_preview_hint), style = MaterialTheme.typography.titleSmall)
                for (size in AppButtonSize.values()) {
                    AppActionRow {
                        for (style in AppButtonStyle.values()) {
                            AppButton({}, size, style = style) { Text(stringResource(R.string.done)) }
                        }
                    }
                }
                AppActionRow {
                    AppButton({}, AppButtonSize.MEDIUM, enabled = false) { Text(stringResource(R.string.done)) }
                    AppButton({}, AppButtonSize.MEDIUM, style = AppButtonStyle.OUTLINED,
                        tone = AppButtonTone.DANGER) { Text(stringResource(R.string.sign_out)) }
                }
                AppMetricRow(stringResource(R.string.profile_status), stringResource(R.string.saved))
                AppBanner(stringResource(R.string.offline_banner), kind = AppStatusKind.WARNING)
                AppCheckboxRow(stringResource(R.string.offline_include_images), true, {}, enabled = false)
                Column(Modifier.selectableGroup()) {
                    AppRadioRow(stringResource(R.string.demo_q1_c1), true, {})
                }
                AppTextField("", {}, label = { Text(stringResource(R.string.username)) }, singleLine = true)
                AppPasswordField("", {}, label = { Text(stringResource(R.string.password)) })
                AppStatus(stringResource(R.string.quiz_saved_pending, com.mukhtabir.android.util.formatNumber(2)), kind = AppStatusKind.WARNING)
                AppStatus(stringResource(R.string.saved), kind = AppStatusKind.SUCCESS)
                AppProgressBar(.5f, Modifier.fillMaxWidth(), label = stringResource(R.string.session_question_navigation))
                AppMetricTile(com.mukhtabir.android.util.formatNumber(20), stringResource(R.string.number_of_questions), onClick = {})
                AnswerChoice(1, stringResource(R.string.demo_q1_c1), selected = true,
                    onClick = {}, answerCorrect = true, markdown = false)
                AppSection(stringResource(R.string.confidence_prompt), initiallyExpanded = true) {
                    Text(stringResource(R.string.confidence_certain))
                }
            }
        }
    }
}

@Preview(name = "Compact Arabic large text", showBackground = true, widthDp = 300, locale = "ar", fontScale = 2f)
@Composable
private fun CompactSharedComponentPreviews() {
    SharedComponentPreviews(compact = true)
}

/** Open each mode in Interactive Preview; includes enough content to inspect scrolling/actions. */
@Preview(name = "Sheet modes", showBackground = true, widthDp = 360, heightDp = 640)
@Preview(name = "Sheet modes Arabic large text", showBackground = true,
    widthDp = 300, heightDp = 480, locale = "ar", fontScale = 2f)
@Composable
private fun SheetModePreviews() {
    var selectedSize by remember { mutableStateOf<AppSheetSize?>(null) }
    MukhtabirTheme {
        Column(Modifier.padding(AppLayoutTokens.pageInsets)) {
            AppTopBar(stringResource(R.string.app_name), true, {})
            AppBackBar(stringResource(R.string.reading_preview_hint), true, onBack = {})
            for (size in AppSheetSize.values()) {
                AppOutlinedButton({ selectedSize = size }) { Text(size.name) }
            }
        }
        selectedSize?.let { size ->
            AppBottomSheet(stringResource(R.string.reading_preview_hint), { selectedSize = null }, size,
                actions = { AppButton({ selectedSize = null }) { Text(stringResource(R.string.done)) } }) {
                repeat(12) { Text(stringResource(R.string.reading_preview_hint)) }
            }
        }
    }
}
