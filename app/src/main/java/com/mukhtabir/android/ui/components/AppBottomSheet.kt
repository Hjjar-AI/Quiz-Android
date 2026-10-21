package com.mukhtabir.android.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mukhtabir.android.R

/** One bounded scroll owner; header and explicit actions stay reachable at large text/IME sizes. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomSheet(title: String, onDismiss: () -> Unit,
    actions: (@Composable () -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val viewport = LocalConfiguration.current.screenHeightDp.dp * AppLayoutTokens.sheetViewportFraction
    ModalBottomSheet(onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        val header: @Composable () -> Unit = {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(title, Modifier.weight(1f).semantics { heading() }, style = MaterialTheme.typography.titleLarge)
                AppIconButton(onDismiss) { Icon(Icons.Filled.Close, stringResource(R.string.close)) }
            }
        }
        val constrainedHeight = viewport < AppLayoutTokens.sessionPinnedMinHeight * LocalDensity.current.fontScale.coerceAtLeast(1f)
        if (constrainedHeight) {
            Column(Modifier.fillMaxWidth().heightIn(max = viewport).navigationBarsPadding()
                .verticalScroll(rememberScrollState()).padding(AppLayoutTokens.pageInsets),
                verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
                header(); content(); actions?.invoke()
            }
        } else Column(Modifier.fillMaxWidth().heightIn(max = viewport).navigationBarsPadding()
            .padding(AppLayoutTokens.pageInsets),
            verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
            header()
            Column(Modifier.weight(1f, fill = false).fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap), content = content)
            actions?.invoke()
        }
    }
}
