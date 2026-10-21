package com.mukhtabir.android.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.mukhtabir.android.R
import com.mukhtabir.android.ui.theme.MukhtabirTheme

/** Public localized sample content only; no services, private state or writes. */
@Preview(name = "Light", showBackground = true, widthDp = 360)
@Preview(name = "Dark", showBackground = true, widthDp = 360,
    uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Arabic RTL", showBackground = true, widthDp = 360, locale = "ar")
@Preview(name = "Large text", showBackground = true, widthDp = 360, fontScale = 2f)
@Composable
private fun SharedComponentPreviews() {
    MukhtabirTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(AppLayoutTokens.pageInsets),
                verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
                AppCard(Modifier.fillMaxWidth()) {
                    AppCardBody { Text(stringResource(R.string.reading_preview_hint)) }
                }
                AppMetricTile("20", stringResource(R.string.number_of_questions), onClick = {})
                AnswerChoice(1, stringResource(R.string.demo_q1_c1), selected = true,
                    onClick = {}, answerCorrect = true, markdown = false)
                AppSection(stringResource(R.string.confidence_prompt), initiallyExpanded = true) {
                    Text(stringResource(R.string.confidence_certain))
                }
            }
        }
    }
}
