package com.mukhtabir.android.ui.transfer

import androidx.compose.runtime.mutableStateOf
import kotlinx.serialization.json.JsonElement

/** Memory-only working state, scoped to the current account's transfer destination. */
internal class TransferInputs {
    val kind = mutableStateOf("simple")
    val mode = mutableStateOf("merge")
    val strategy = mutableStateOf("keep_local")
    val format = mutableStateOf("json")
    val statePackage = mutableStateOf(false)
    val images = mutableStateOf(true)
    val verified = mutableStateOf(false)
    val search = mutableStateOf("")
    val categoryIds = mutableStateOf("")
    val tags = mutableStateOf("")
    val difficulty = mutableStateOf(setOf<String>())
    val manual = mutableStateOf(false)
    val selected = mutableStateOf(emptyList<Long>())
    val questionQuery = mutableStateOf("")
    val title = mutableStateOf("")
    val pdfMode = mutableStateOf("study")
    val answers = mutableStateOf("inline")
    val pdfTheme = mutableStateOf("stone")
    val locale = mutableStateOf(java.util.Locale.getDefault().language.takeIf { it == "ar" } ?: "en")
    val about = mutableStateOf(false)
    val aboutTitle = mutableStateOf("")
    val aboutBody = mutableStateOf("")
    val fields = mutableStateOf(emptyList<Pair<String, String>>())
    val mapping = mutableStateOf(emptyMap<String, JsonElement>())
    val resolutions = mutableStateOf(emptyMap<String, JsonElement>())
}
