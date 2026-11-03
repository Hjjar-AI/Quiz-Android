package com.mukhtabir.android.ui.masterexams

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.MasterExamDto
import com.mukhtabir.android.ui.components.AppLayoutTokens
import com.mukhtabir.android.util.formatLocalDateTime
import com.mukhtabir.android.util.formatNumber

/** Shared summary body; clickable containers, capabilities and learner notices are caller-owned. */
@Composable
internal fun MasterExamSummary(exam: MasterExamDto,
    notices: @Composable ColumnScope.() -> Unit = {},
    actions: @Composable ColumnScope.() -> Unit = {}) {
    Column(verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
        Text(exam.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        Text(masterStatus(exam.status))
        notices()
        Text(stringResource(R.string.master_summary, formatNumber(exam.questionCount), formatNumber(exam.durationMinutes)))
        Text(stringResource(R.string.master_window, formatLocalDateTime(exam.opensAt), formatLocalDateTime(exam.closesAt)))
        actions()
    }
}
