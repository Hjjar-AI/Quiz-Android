package com.mukhtabir.android.util

import java.util.Locale

/** Isolate the clock from Arabic labels so minutes/seconds cannot change visual order. */
fun formatSessionDuration(seconds: Long): String {
    val safe = seconds.coerceAtLeast(0)
    val clock = if (safe >= 3600) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", safe / 3600, safe / 60 % 60, safe % 60)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", safe / 60, safe % 60)
    }
    return "\u2066$clock\u2069"
}
