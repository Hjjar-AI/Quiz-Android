package com.mukhtabir.android.ui.planner

import com.mukhtabir.android.ui.components.AppLayoutTokens

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import com.mukhtabir.android.ui.components.AppOutlinedButton as OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.error
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.mukhtabir.android.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

internal fun parsePlannerDate(value: String): LocalDate? {
    if (!Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}").matches(value)) return null
    return try { LocalDate.parse(value).takeIf { it.year in 1..9999 } }
    catch (_: DateTimeParseException) { null }
}

@Composable
internal fun PlannerDateField(label: Int, value: String, fallback: String, enabled: Boolean,
    formatter: DateTimeFormatter, fieldError: String? = null, onPicked: (String) -> Unit) {
    val context = LocalContext.current
    val selected = parsePlannerDate(value)
    Text(stringResource(label), modifier = Modifier.semantics { heading() })
    fieldError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    OutlinedButton(onClick = {
        val initial = selected ?: parsePlannerDate(fallback) ?: LocalDate.now()
        DatePickerDialog(context, { _, year, month, day -> onPicked(LocalDate.of(year, month + 1, day).toString()) },
            initial.year, initial.monthValue - 1, initial.dayOfMonth).show()
    }, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = com.mukhtabir.android.ui.components.AppLayoutTokens.touchTarget).semantics { if (fieldError != null) error(fieldError) }) {
        Text(selected?.format(formatter) ?: stringResource(R.string.planner_choose_date))
    }
}
