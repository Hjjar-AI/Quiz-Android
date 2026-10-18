package com.mukhtabir.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import com.mukhtabir.android.R
import com.mukhtabir.android.ui.theme.AppTheme

/** Page gutters and control spacing stay independent of density and minimum hit targets. */
object AppLayoutTokens {
    val pagePadding = 8.dp
    val pageInsets = PaddingValues(horizontal = pagePadding, vertical = 6.dp)
    val feedbackVerticalPadding = 10.dp
    val controlGap: androidx.compose.ui.unit.Dp @Composable get() = AppTheme.spacing.content
    val authPadding = 16.dp
    val authInsets = PaddingValues(horizontal = authPadding, vertical = 6.dp)
    val comparisonBreakpoint = 600.dp
    val headingBreakpoint = 360.dp
    val sessionHeaderBreakpoint = 300.dp
    val imageMaxHeight = 360.dp
    const val questionImageAspectRatio = 1.6f
    val menuMaxHeight = 360.dp
    val pickerMaxHeight = 420.dp
    val cardPadding: androidx.compose.ui.unit.Dp @Composable get() = AppTheme.spacing.card
    val denseCardPadding: androidx.compose.ui.unit.Dp @Composable get() = AppTheme.spacing.denseCard
    val cardVerticalPadding: androidx.compose.ui.unit.Dp @Composable get() = AppTheme.spacing.cardVertical
    val denseCardVerticalPadding: androidx.compose.ui.unit.Dp @Composable get() = AppTheme.spacing.denseCardVertical
    val cardInsets: PaddingValues @Composable get() = PaddingValues(horizontal = cardPadding, vertical = cardVerticalPadding)
    val denseCardInsets: PaddingValues @Composable get() = PaddingValues(horizontal = denseCardPadding, vertical = denseCardVerticalPadding)
    val tightGap: androidx.compose.ui.unit.Dp @Composable get() = AppTheme.spacing.tight
    val shellVerticalPadding = 0.dp
    val bottomNavigationHeight = 48.dp
    val libraryPadding = 12.dp
    val libraryCardPadding: androidx.compose.ui.unit.Dp @Composable get() = AppTheme.spacing.libraryCard
    val libraryGap: androidx.compose.ui.unit.Dp
        @Composable get() = AppTheme.spacing.content
    val touchTarget = 48.dp
    val secondaryIconSize = 20.dp
    val cardBorderWidth = 1.dp
    val selectedBorderWidth = 2.dp
    val answerFeedbackBorderWidth = 3.dp
    val questionBorderWidth = 1.5.dp
    val tileMinWidth = 112.dp
    const val tileMaxColumns = 3
    val tileWidthAllowance = .5.dp
    val secondaryVisualHeight = 36.dp
    val secondaryFrameInset = 6.dp
    val choiceNumberWidth = 24.dp
    val heatmapCellSize = 24.dp
    val secondaryVerticalPadding: androidx.compose.ui.unit.Dp @Composable get() =
        (AppTheme.spacing.denseCardVertical - 1.dp).coerceAtLeast(1.dp)
    val secondaryContentPadding: PaddingValues @Composable get() =
        PaddingValues(horizontal = 10.dp, vertical = secondaryVerticalPadding)
}

@Composable
fun AppTextButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit) {
    TextButton(onClick, modifier.heightIn(min = AppLayoutTokens.touchTarget), enabled = enabled,
        shape = MaterialTheme.shapes.medium, contentPadding = AppLayoutTokens.secondaryContentPadding,
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .62f)), content = content)
}

@Composable
fun AppIconButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    content: @Composable () -> Unit) {
    IconButton(onClick, modifier.sizeIn(minWidth = AppLayoutTokens.touchTarget, minHeight = AppLayoutTokens.touchTarget),
        enabled = enabled, colors = IconButtonDefaults.iconButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .62f))) {
        Box(Modifier.size(AppLayoutTokens.secondaryIconSize), contentAlignment = Alignment.Center) { content() }
    }
}

/** Destructive labels stay explicit; color is additional feedback, never the only cue. */
@Composable
fun AppDangerTextButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit) {
    TextButton(onClick, modifier.heightIn(min = AppLayoutTokens.touchTarget), enabled = enabled,
        shape = MaterialTheme.shapes.medium, contentPadding = AppLayoutTokens.secondaryContentPadding,
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .62f)), content = content)
}

@Composable
fun AppDangerOutlinedButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit) {
    AppSecondaryOutlinedButton(onClick, modifier, enabled, MaterialTheme.colorScheme.error, content)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppActionRow(modifier: Modifier = Modifier, fillWidth: Boolean = true, content: @Composable FlowRowScope.() -> Unit) {
    FlowRow(if (fillWidth) modifier.fillMaxWidth() else modifier, horizontalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap),
        verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap), content = content)
}

@Composable
fun AppSuccessText(message: String, modifier: Modifier = Modifier) {
    Text(message, modifier.semantics { liveRegion = LiveRegionMode.Polite },
        style = MaterialTheme.typography.bodyMedium, color = AppTheme.feedback.success)
}


/** The screen retains paging state, retries and authority; this component only presents controls. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppPagination(page: Int, totalPages: Int, onPrevious: () -> Unit, onNext: () -> Unit,
    previousEnabled: Boolean = true, nextEnabled: Boolean = true, modifier: Modifier = Modifier) {
    AppActionRow(modifier) {
        AppOutlinedButton(onPrevious, enabled = previousEnabled && page > 1) { Text(stringResource(R.string.previous)) }
        Text(stringResource(R.string.page_status, com.mukhtabir.android.util.formatNumber(page),
            com.mukhtabir.android.util.formatNumber(totalPages)),
            modifier = Modifier.padding(vertical = 14.dp), style = MaterialTheme.typography.bodyMedium)
        AppOutlinedButton(onNext, enabled = nextEnabled && page < totalPages) { Text(stringResource(R.string.next)) }
    }
}
