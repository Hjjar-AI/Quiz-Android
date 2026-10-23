package com.mukhtabir.android.testing

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.runtime.Composable
import com.mukhtabir.android.core.DisplayPreferences
import com.mukhtabir.android.ui.theme.MukhtabirTheme
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.assertIsDisplayed

internal val answerChoices = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)

/** Markwon uses real Android TextViews; Compose's text matcher does not inspect their text. */
internal fun readingViews(root: View): List<TextView> = buildList {
    fun visit(view: View) {
        if (view is TextView && view.isShown) add(view)
        if (view is ViewGroup) for (index in 0 until view.childCount) visit(view.getChildAt(index))
    }
    visit(root)
}

/** Production theme with the supported device font option; no font fetching in JVM tests. */
@Composable
internal fun ContractTestTheme(content: @Composable () -> Unit) {
    MukhtabirTheme(darkTheme = false, display = DisplayPreferences(font = "device"), content = content)
}

/** Closed Material drawers keep offscreen semantic nodes with the same labels as visible tabs. */
internal fun SemanticsNodeInteractionCollection.singleDisplayed(): SemanticsNodeInteraction {
    val visible = fetchSemanticsNodes().indices.map { get(it) }.filter {
        runCatching { it.assertIsDisplayed() }.isSuccess
    }
    check(visible.size == 1) { "Expected one displayed control, found ${visible.size}" }
    return visible.single()
}
