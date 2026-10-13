package com.mukhtabir.android.util

import java.text.NumberFormat
import java.util.Locale

/** UI digits follow the selected app locale; protocol IDs/input values remain unchanged. */
fun formatNumber(value: Int): String = NumberFormat.getIntegerInstance(Locale.getDefault()).format(value)

fun formatNumber(value: Long): String = NumberFormat.getIntegerInstance(Locale.getDefault()).format(value)

fun formatDecimal(value: Double): String = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
    maximumFractionDigits = 1
}.format(value)
