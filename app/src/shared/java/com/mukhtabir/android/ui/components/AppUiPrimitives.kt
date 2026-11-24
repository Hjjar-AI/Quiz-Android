package com.mukhtabir.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mukhtabir.android.shared.R
import com.mukhtabir.android.ui.theme.AppTheme

object AppControlTokens {
    const val disabledContentAlpha = .62f
    const val disabledContainerAlpha = .08f
    const val disabledBorderAlpha = .25f
    const val cardBorderAlpha = .35f
    const val explanationBorderAlpha = .55f
    const val scrollTrackAlpha = .22f
    const val scrollThumbAlpha = .72f
    const val correctChoiceTint = .075f
    const val incorrectChoiceTint = .06f
    const val selectedChoiceTint = .08f
    const val bannerTint = .08f
}

/** Icon sizes are visual only; interactive controls retain the shared 48dp target. */
object AppIconSize {
    val inline = 16.dp
    val small = 20.dp
    val standard = 24.dp
}

/** Width roles differ by content; font-scale thresholds remain shared. */
object AppAdaptiveTokens {
    const val largeText = 1.3f
    const val extraLargeText = 1.5f

    fun stack(width: androidx.compose.ui.unit.Dp, breakpoint: androidx.compose.ui.unit.Dp,
        fontScale: Float, fontScaleThreshold: Float = largeText): Boolean =
        width < breakpoint || fontScale >= fontScaleThreshold
}

/** Page gutters and control spacing stay independent of density and minimum hit targets. */
object AppLayoutTokens {
    val pagePadding = 8.dp
    val pageInsets = PaddingValues(horizontal = pagePadding, vertical = 6.dp)
    val feedbackVerticalPadding = 10.dp
    val controlGap: androidx.compose.ui.unit.Dp @Composable get() = AppTheme.spacing.content
    val authMaxWidth = 520.dp
    val authPadding = 16.dp
    val authInsets = PaddingValues(horizontal = authPadding, vertical = 6.dp)
    val navigationRailBreakpoint = 600.dp
    val navigationRailWidth = 104.dp
    val detailPaneBreakpoint = 900.dp
    val detailListFraction = .42f
    val homeColumnMinWidth = 360.dp
    val reviewListMaxHeight = 480.dp
    val sheetViewportFraction = AppSheetSize.STANDARD.viewportFraction
    val heroElevation = 2.dp
    val comparisonBreakpoint = 600.dp
    val headingBreakpoint = 360.dp
    val sessionHeaderBreakpoint = 300.dp
    val sessionPinnedMinHeight = 420.dp
    val drawerPinnedMinHeight = 360.dp
    val drawerHorizontalPadding = 12.dp
    val imageMaxHeight = 360.dp
    val menuMaxHeight = 360.dp
    val filterMenuMaxHeight = 280.dp
    val shellHorizontalPadding = pagePadding
    val primaryContentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
    val pickerMaxHeight = 420.dp
    val cardPadding: androidx.compose.ui.unit.Dp @Composable get() = AppTheme.spacing.card
    val denseCardPadding: androidx.compose.ui.unit.Dp @Composable get() = AppTheme.spacing.denseCard
    val cardVerticalPadding: androidx.compose.ui.unit.Dp @Composable get() = AppTheme.spacing.cardVertical
    val denseCardVerticalPadding: androidx.compose.ui.unit.Dp @Composable get() = AppTheme.spacing.denseCardVertical
    val cardInsets: PaddingValues @Composable get() = PaddingValues(horizontal = cardPadding, vertical = cardVerticalPadding)
    val denseCardInsets: PaddingValues @Composable get() = PaddingValues(horizontal = denseCardPadding, vertical = denseCardVerticalPadding)
    val tightGap: androidx.compose.ui.unit.Dp @Composable get() = AppTheme.spacing.content
    val shellVerticalPadding = 0.dp
    val bottomNavigationHeight = 48.dp
    val bottomNavigationHorizontalPadding = 4.dp
    val navigationItemInsets = PaddingValues(horizontal = 4.dp, vertical = 1.dp)
    val libraryPadding = 12.dp
    val libraryCardPadding: androidx.compose.ui.unit.Dp @Composable get() = AppTheme.spacing.denseCard
    val libraryGap: androidx.compose.ui.unit.Dp
        @Composable get() = AppTheme.spacing.content
    val touchTarget = 48.dp
    val secondaryIconSize = AppIconSize.small
    val cardBorderWidth = 1.dp
    val dividerThickness = 1.dp
    val actionPanelElevation = 2.dp
    val selectedBorderWidth = 2.dp
    val answerFeedbackBorderWidth = 1.dp
    val questionBorderWidth = 1.5.dp
    val secondaryVisualHeight = 36.dp
    val secondaryFrameInset = 6.dp
    val secondaryVerticalPadding: androidx.compose.ui.unit.Dp @Composable get() =
        (AppTheme.spacing.denseCardVertical - 1.dp).coerceAtLeast(1.dp)
    val secondaryContentPadding: PaddingValues @Composable get() =
        PaddingValues(horizontal = 10.dp, vertical = secondaryVerticalPadding)
}

@Composable
fun AppIconButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    content: @Composable () -> Unit) {
    IconButton(onClick, modifier.sizeIn(minWidth = AppLayoutTokens.touchTarget, minHeight = AppLayoutTokens.touchTarget),
        enabled = enabled, colors = IconButtonDefaults.iconButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = AppControlTokens.disabledContentAlpha))) {
        Box(Modifier.size(AppLayoutTokens.secondaryIconSize), contentAlignment = Alignment.Center) { content() }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppActionRow(modifier: Modifier = Modifier, fillWidth: Boolean = true, content: @Composable FlowRowScope.() -> Unit) {
    FlowRow(if (fillWidth) modifier.fillMaxWidth() else modifier, horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.section),
        verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap), content = content)
}

@Composable
fun AppSuccessText(message: String, modifier: Modifier = Modifier) {
    AppStatus(message, modifier, kind = AppStatusKind.SUCCESS)
}


/** The screen retains paging state, retries and authority; this component only presents controls. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppPagination(page: Int, totalPages: Int, onPrevious: () -> Unit, onNext: () -> Unit,
    previousEnabled: Boolean = true, nextEnabled: Boolean = true, modifier: Modifier = Modifier, statusLabel: String? = null) {
    AppActionRow(modifier) {
        AppOutlinedButton(onPrevious, enabled = previousEnabled && page > 1) { Text(stringResource(R.string.previous)) }
        Text(statusLabel ?: stringResource(R.string.page_status, com.mukhtabir.android.util.formatNumber(page),
            com.mukhtabir.android.util.formatNumber(totalPages)),
            modifier = Modifier.heightIn(min = AppLayoutTokens.touchTarget).wrapContentHeight(),
            style = MaterialTheme.typography.bodyMedium)
        AppOutlinedButton(onNext, enabled = nextEnabled && page < totalPages) { Text(stringResource(R.string.next)) }
    }
}

/** Use the same action role in confirmations as in the initiating destructive control. */
@Composable
fun AppConfirmationButton(onClick: () -> Unit, destructive: Boolean, enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit) {
    if (destructive) AppDangerTextButton(onClick, enabled = enabled, content = content)
    else AppTextButton(onClick, enabled = enabled, content = content)
}
