package com.mukhtabir.android.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import java.time.LocalDate

/** Date-only picker mechanics. Parsing, fallback authority and display formatting stay with callers. */
@Composable
internal fun AppDateField(display: String, initial: () -> LocalDate, enabled: Boolean,
    onPicked: (LocalDate) -> Unit, modifier: Modifier = Modifier, fieldError: String? = null) {
    val context = LocalContext.current
    AppOutlinedButton({
        val date = initial()
        DatePickerDialog(context, { _, year, month, day -> onPicked(LocalDate.of(year, month + 1, day)) },
            date.year, date.monthValue - 1, date.dayOfMonth).show()
    }, enabled = enabled, modifier = modifier.fillMaxWidth().semantics {
        if (fieldError != null) error(fieldError)
    }) { Text(display) }
}
