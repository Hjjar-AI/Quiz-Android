package com.mukhtabir.android.ui.planner

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.mukhtabir.android.shared.R
import com.mukhtabir.android.ui.components.AppDateField
import com.mukhtabir.android.ui.components.AppStatus
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

fun parsePlannerDate(value: String): LocalDate? {
    if (!Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}").matches(value)) return null
    return try { LocalDate.parse(value).takeIf { it.year in 1..9999 } }
    catch (_: DateTimeParseException) { null }
}

@Composable
fun PlannerDateField(label: Int, value: String, fallback: String, enabled: Boolean,
    formatter: DateTimeFormatter, fieldError: String? = null, onPicked: (String) -> Unit) {
    val selected = parsePlannerDate(value)
    Text(stringResource(label), modifier = Modifier.semantics { heading() })
    fieldError?.let { AppStatus(it) }
    AppDateField(selected?.format(formatter) ?: stringResource(R.string.planner_choose_date),
        initial = { selected ?: parsePlannerDate(fallback) ?: LocalDate.now() }, enabled = enabled,
        onPicked = { onPicked(it.toString()) }, fieldError = fieldError)
}
