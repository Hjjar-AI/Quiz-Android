package com.mukhtabir.android.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/** Subtle persistent cue only when there is additional vertical content. */
@Composable
fun Modifier.appVerticalScrollIndicator(state: ScrollState): Modifier {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val track = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=AppControlTokens.scrollTrackAlpha)
    val thumb = MaterialTheme.colorScheme.primary.copy(alpha=AppControlTokens.scrollThumbAlpha)
    return drawWithContent {
        drawContent()
        val maximum = state.maxValue
        if(maximum > 0 && maximum != Int.MAX_VALUE && size.height > 8.dp.toPx()) {
            val inset = 4.dp.toPx()
            val width = 3.dp.toPx()
            val height = size.height-2*inset
            val length = (height * size.height/(size.height+maximum)).coerceIn(16.dp.toPx().coerceAtMost(height),height)
            val offset = (state.value.toFloat()/maximum).coerceIn(0f,1f)*(height-length)
            val x = if(rtl) 2.dp.toPx() else size.width-width-2.dp.toPx()
            drawRoundRect(track,Offset(x,inset),Size(width,height),CornerRadius(width,width))
            drawRoundRect(thumb,Offset(x,inset+offset),Size(width,length),CornerRadius(width,width))
        }
    }
}
