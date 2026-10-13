package com.mukhtabir.android.util

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Format an ISO 8601 timestamp as a locale-aware long date.
 *
 * Returns the input verbatim when it cannot be parsed. That is
 * deliberately a "show something" fallback rather than an empty
 * string — a broken timestamp is a bug on the server side and it is
 * more useful to see it than to hide it.
 *
 * The three callers today are the Profile screen's "joined" and
 * "last login" rows and the subscription expiry row. None of them
 * accept a null input; the null case is handled at the call site so
 * the row is simply omitted.
 */
fun formatLongDate(iso: String, locale: Locale = Locale.getDefault()): String {
    return try {
        OffsetDateTime.parse(iso)
            .toLocalDate()
            .format(
                DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale),
            )
    } catch (_: Exception) {
        iso
    }
}

/** Exam windows need the time as well as the date, in the device time zone. */
fun formatLocalDateTime(iso: String): String = try {
    OffsetDateTime.parse(iso).atZoneSameInstant(java.time.ZoneId.systemDefault())
        .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale.getDefault()))
} catch (_: Exception) { iso }

fun formatCalendarDate(iso: String): String = try {
    java.time.LocalDate.parse(iso).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(Locale.getDefault()).withDecimalStyle(java.time.format.DecimalStyle.of(Locale.getDefault())))
} catch (_: Exception) { iso }
