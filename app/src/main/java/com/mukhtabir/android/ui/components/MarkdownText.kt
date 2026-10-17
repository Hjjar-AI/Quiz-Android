package com.mukhtabir.android.ui.components

import android.widget.TextView
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.content.Context
import kotlin.math.abs
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.mukhtabir.android.ui.theme.LocalReadingComfort
import com.mukhtabir.android.ui.theme.readingMaxWidth
import com.mukhtabir.android.ui.theme.readingTextStyle
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.toArgb
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.Markwon
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.html.tag.UnderlineHandler
import io.noties.markwon.html.tag.SubScriptHandler
import io.noties.markwon.html.tag.SuperScriptHandler
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tables.TableTheme
import io.noties.markwon.ext.tables.TableAwareMovementMethod
import io.noties.markwon.movement.MovementMethodPlugin
import io.noties.markwon.simple.ext.SimpleExtPlugin
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import io.noties.markwon.core.spans.HeadingSpan
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.Text
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.launch
import com.mukhtabir.android.R

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
    navigateHeadings: Boolean = false,
    compactBlocks: Boolean = false,
) {
    val context = LocalContext.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val paragraphScale = LocalReadingComfort.current.readingParagraphs / 100f
    val paragraphMargin = if (compactBlocks) com.mukhtabir.android.ui.theme.AppTheme.spacing.tight else 8.dp
    val blockMargin = with(density) {
        (paragraphMargin * fontScale.coerceAtLeast(1f) * paragraphScale).roundToPx()
    }
    val highlight = MaterialTheme.colorScheme.tertiaryContainer.toArgb()
    val highlightText = MaterialTheme.colorScheme.onTertiaryContainer.toArgb()
    val border = MaterialTheme.colorScheme.outline.toArgb()
    val markwon = remember(context, highlight, highlightText, border, compactBlocks, blockMargin, paragraphScale) {
        Markwon.builder(context)
            .usePlugin(HtmlPlugin.create { html ->
                html.excludeDefaults(true).addHandler(UnderlineHandler())
                    .addHandler(SubScriptHandler()).addHandler(SuperScriptHandler())
            })
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(SimpleExtPlugin.create { plugin ->
                plugin.addExtension(2, '=') { _, _ ->
                    arrayOf(BackgroundColorSpan(highlight), ForegroundColorSpan(highlightText))
                }
            })
            .usePlugin(TablePlugin.create(TableTheme.buildWithDefaults(context).tableBorderColor(border).build()))
            .usePlugin(MovementMethodPlugin.create(TableAwareMovementMethod.create()))
            .usePlugin(object : AbstractMarkwonPlugin() {
                override fun configureTheme(builder: MarkwonTheme.Builder) {
                    if (compactBlocks || paragraphScale != 1f) builder.blockMargin(blockMargin)
                    if (compactBlocks) builder.headingTextSizeMultipliers(floatArrayOf(1.35f, 1.25f, 1.15f, 1.1f, 1f, 1f))
                }
            })
            .build()
    }
    val rendered = remember(markdown) { scriptMarkdown(markdown) }
    val parsed = remember(markwon, rendered, navigateHeadings) {
        if (navigateHeadings) markwon.toMarkdown(rendered) else null
    }
    val headings = remember(parsed) {
        parsed?.let { content ->
            content.getSpans(0, content.length, HeadingSpan::class.java).mapNotNull { span ->
                val start = content.getSpanStart(span)
                val end = content.getSpanEnd(span)
                if (start < 0 || end <= start || end > content.length) null
                else content.subSequence(start, end).toString().trim().takeIf(String::isNotEmpty)
                    ?.let { MarkdownHeading(start, it) }
            }.sortedBy { it.offset }
        }.orEmpty()
    }
    val requester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    val nativeViewHolder = remember { arrayOfNulls<TextView>(1) }

    val typeface = com.mukhtabir.android.ui.theme.LocalNativeTypeface.current
    val color = LocalContentColor.current.toArgb()
    val linkColor = MaterialTheme.colorScheme.primary.toArgb()
    val style = readingTextStyle(LocalTextStyle.current)

    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
    Column(Modifier.widthIn(max = readingMaxWidth()).fillMaxWidth()) {
        if (headings.isNotEmpty()) AppSection(stringResource(R.string.explanation_sections), composeCollapsedContent = false) {
            AppActionRow {
                headings.forEach { target ->
                    val jumpLabel = stringResource(R.string.explanation_jump_to, target.title)
                    AppTextButton(onClick = {
                        scope.launch {
                            val view = nativeViewHolder[0] ?: return@launch
                            val layout = view.layout ?: return@launch
                            if (view.text.toString() != parsed?.toString()) return@launch
                            val line = layout.getLineForOffset(target.offset)
                            requester.bringIntoView(Rect(0f,
                                (layout.getLineTop(line) + view.totalPaddingTop).toFloat(), view.width.toFloat(),
                                (layout.getLineBottom(line) + view.totalPaddingTop).toFloat()))
                        }
                    }, modifier = Modifier.semantics { contentDescription = jumpLabel }) { Text(target.title) }
                }
            }
        }
        AndroidView(
            modifier = Modifier.fillMaxWidth().bringIntoViewRequester(requester),
            factory = { viewContext ->
                ReadingTextView(viewContext).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    setHorizontallyScrolling(false)
                    setTextIsSelectable(true)
                }
            },
            update = { textView ->
                nativeViewHolder[0] = textView
                val interactionChanged = textView.interactive != interactive
                textView.interactive = interactive
                textView.setTextIsSelectable(interactive)
                textView.typeface = typeface
                textView.setTextColor(color)
                textView.setLinkTextColor(linkColor)
                textView.applyReadingStyle(style, density)
                // Status-only updates must not reset native selection or paragraph layout.
                if (textView.renderer !== markwon || textView.renderedMarkdown != rendered || interactionChanged) {
                    if (parsed != null) markwon.setParsedMarkdown(textView, parsed)
                    else markwon.setMarkdown(textView, rendered)
                    textView.renderer = markwon
                    textView.renderedMarkdown = rendered
                }
                textView.alignReadingText()
                if (!interactive) {
                    textView.movementMethod = null
                    textView.isClickable = false
                    textView.isLongClickable = false
                    textView.isFocusable = false
                }
            },
        )
    }
    }
}

private data class MarkdownHeading(val offset: Int, val title: String)


/** Let ordinary vertical reading drags reach Compose while retaining native text selection. */
private class ReadingTextView(context: Context) : TextView(context) {
    var interactive = true
    var renderer: Markwon? = null
    var renderedMarkdown: String? = null
    private val slop = ViewConfiguration.get(context).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!interactive) return false
        if (event.actionMasked == MotionEvent.ACTION_DOWN) { downX = event.x; downY = event.y }
        val handled = super.onTouchEvent(event)
        if (event.actionMasked == MotionEvent.ACTION_MOVE && !hasSelection() &&
            abs(event.y - downY) > slop && abs(event.y - downY) > abs(event.x - downX)) {
            parent?.requestDisallowInterceptTouchEvent(false)
        }
        return handled
    }
}
