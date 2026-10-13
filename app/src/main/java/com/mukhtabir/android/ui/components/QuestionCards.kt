package com.mukhtabir.android.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.lerp
import com.mukhtabir.android.ui.theme.AppTheme

@Composable
fun QuestionTextCard(text: String, modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge) {
    val colors = AppTheme.questionSurfaces
    Surface(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
        color = colors.question, contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(AppLayoutTokens.questionBorderWidth, colors.border)) {
        ProvideTextStyle(textStyle) {
            MarkdownText(text, Modifier.padding(horizontal = AppTheme.spacing.card,
                vertical = AppTheme.spacing.cardVertical).fillMaxWidth())
        }
    }
}

/** Explanation content keeps its existing reveal rules and selectable Markdown. */
@Composable
fun ExplanationCard(text: String, modifier: Modifier = Modifier, title: String? = null) {
    if (text.isBlank()) return
    val colors = AppTheme.questionSurfaces
    Surface(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
        color = colors.choice, contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(AppLayoutTokens.cardBorderWidth, colors.border.copy(alpha = .55f))) {
        Column(Modifier.padding(AppLayoutTokens.cardInsets),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.content)) {
            title?.let { Text(it, style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.semantics { heading() }) }
            ProvideTextStyle(MaterialTheme.typography.bodyLarge) {
                MarkdownText(text, Modifier.fillMaxWidth(), navigateHeadings = true, compactBlocks = true)
            }
        }
    }
}

/** Only presentation and whole-choice selection; the caller owns all write guards. */
@Composable
fun QuestionChoiceCard(modifier: Modifier = Modifier, selected: Boolean = false,
    enabled: Boolean = true, onClick: (() -> Unit)? = null,
    answerCorrect: Boolean? = null,
    neutralSelection: Boolean = false,
    content: @Composable RowScope.() -> Unit) {
    val colors = AppTheme.questionSurfaces
    val scheme = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val selection = if (onClick == null) Modifier else Modifier.selectable(
        selected = selected, enabled = enabled, role = Role.RadioButton,
        interactionSource = interactionSource, indication = rememberRipple(), onClick = onClick)
    Surface(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
        color = when (answerCorrect) {
            true -> lerp(colors.choice, AppTheme.feedback.success, .075f)
            false -> lerp(colors.choice, scheme.error, .06f)
            null -> if (selected) lerp(colors.choice, if (neutralSelection) scheme.onSurface else scheme.primary, .08f) else colors.choice
        },
        contentColor = scheme.onSurface,
        border = BorderStroke(if (selected) AppLayoutTokens.selectedBorderWidth else AppLayoutTokens.cardBorderWidth, when (answerCorrect) {
            true -> AppTheme.feedback.success
            false -> scheme.error
            null -> if (selected) { if (neutralSelection) scheme.onSurfaceVariant else scheme.primary } else colors.border
        })) {
        Row(selection.fillMaxWidth().heightIn(min = AppLayoutTokens.touchTarget)
            .padding(horizontal = AppTheme.spacing.card, vertical = AppLayoutTokens.secondaryVerticalPadding),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.content),
            verticalAlignment = Alignment.Top, content = content)
    }
}
