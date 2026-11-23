package com.mukhtabir.android.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mukhtabir.android.shared.R
import com.mukhtabir.android.data.remote.*
import com.mukhtabir.android.ui.theme.AppTheme
import com.mukhtabir.android.util.formatNumber
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Caller retains reveal authority; local images never cause a network request. */
@Composable
fun QuestionImage(url: String?, repository: LearnerImages? = null, modifier: Modifier = Modifier,
    downloadedImage: String? = null, maxHeight: androidx.compose.ui.unit.Dp = AppLayoutTokens.imageMaxHeight) {
    if (url.isNullOrBlank() && downloadedImage == null) return
    var state by remember(url, repository, downloadedImage) { mutableStateOf<ImageState>(ImageState.Loading) }
    var retry by remember(url, repository, downloadedImage) { mutableStateOf(0) }
    var inspect by remember(url, repository, downloadedImage) { mutableStateOf(false) }
    LaunchedEffect(url, repository, retry, downloadedImage) {
        state = ImageState.Loading
        state = try {
            val result = withContext(Dispatchers.Default) {
                val bytes = downloadedImage?.let(::decodeQuestionImageData)
                    ?: requireNotNull(repository).load(requireNotNull(url))
                ImageState.Success(bytes, decodeBoundedQuestionBitmap(bytes, QuestionImageLimits.previewEdge).asImageBitmap())
            }
            result
        } catch (cancel: CancellationException) { throw cancel }
        catch (_: Exception) { ImageState.Error }
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.tightGap)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val panelHeight = if (state is ImageState.Error) 96.dp else
                (maxWidth / QuestionImageGeometry.questionImageAspectRatio).coerceAtMost(maxHeight)
            Surface(Modifier.fillMaxWidth().height(panelHeight), color = AppTheme.questionSurfaces.choice,
                shape = MaterialTheme.shapes.medium, border = BorderStroke(AppLayoutTokens.cardBorderWidth, AppTheme.questionSurfaces.border)) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    when (val current = state) {
                        ImageState.Loading -> AppLoadingIndicator()
                        ImageState.Error -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                            .padding(AppLayoutTokens.cardInsets), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.content)) {
                            AppStatus(stringResource(R.string.question_image_unavailable),
                                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                            AppTextButton({ retry += 1 }) { Text(stringResource(R.string.retry)) }
                        }
                        is ImageState.Success -> Image(current.bitmap, stringResource(R.string.question_image_description),
                            Modifier.fillMaxSize().clickable(role = Role.Button) { inspect = true }, contentScale = ContentScale.Fit)
                    }
                }
            }
        }
        if (state is ImageState.Success) AppTextButton({ inspect = true }) { Text(stringResource(R.string.image_inspect)) }
    }
    val success = state as? ImageState.Success
    if (inspect && success != null) QuestionImageViewer(success.bytes) { inspect = false }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuestionImageViewer(bytes: ByteArray, onClose: () -> Unit) {
    var bitmap by remember(bytes) { mutableStateOf<ImageBitmap?>(null) }
    var failed by remember(bytes) { mutableStateOf(false) }
    var scale by remember(bytes) { mutableStateOf(1f) }
    var pan by remember(bytes) { mutableStateOf(Offset.Zero) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    LaunchedEffect(bytes) {
        try { bitmap = withContext(Dispatchers.Default) {
            decodeBoundedQuestionBitmap(bytes, QuestionImageLimits.detailEdge).asImageBitmap()
        } } catch (cancel: CancellationException) { throw cancel }
        catch (_: Exception) { failed = true }
    }
    fun limits(zoom: Float): Offset {
        val image = bitmap ?: return Offset.Zero
        val fit = minOf(viewport.width.toFloat() / image.width, viewport.height.toFloat() / image.height)
        return Offset(((image.width * fit * zoom - viewport.width) / 2).coerceAtLeast(0f),
            ((image.height * fit * zoom - viewport.height) / 2).coerceAtLeast(0f))
    }
    val transform = rememberTransformableState { zoom, delta, _ ->
        scale = (scale * zoom).coerceIn(1f, 5f)
        val bounds = limits(scale)
        pan = Offset(if (bounds.x > 0) (pan.x + delta.x / bounds.x).coerceIn(-1f, 1f) else 0f,
            if (bounds.y > 0) (pan.y + delta.y / bounds.y).coerceIn(-1f, 1f) else 0f)
    }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
            val viewportHeight = maxHeight
            val short = viewportHeight < AppLayoutTokens.sessionPinnedMinHeight * androidx.compose.ui.platform.LocalDensity.current.fontScale.coerceAtLeast(1f)
            Column(Modifier.fillMaxSize().then(if (short) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(AppLayoutTokens.pageInsets), verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
                AppActionRow {
                    AppOutlinedButton(onClose) { Text(stringResource(R.string.close)) }
                    AppTextButton({ scale = 1f; pan = Offset.Zero }) { Text(stringResource(R.string.image_reset_view)) }
                }
                val imageModifier = if (short) Modifier.fillMaxWidth().height((viewportHeight * .6f).coerceAtLeast(QuestionImageGeometry.imageControlsMaxHeight))
                    else Modifier.weight(1f).fillMaxWidth()
                Box(imageModifier.clipToBounds().onSizeChanged { viewport = it }
                    .transformable(transform), contentAlignment = Alignment.Center) {
                    bitmap?.let { image ->
                        val bounds = limits(scale)
                        Image(image, stringResource(R.string.question_image_description), Modifier.fillMaxSize()
                            .graphicsLayer(scaleX = scale, scaleY = scale,
                                translationX = pan.x * bounds.x, translationY = pan.y * bounds.y), contentScale = ContentScale.Fit)
                    } ?: if (failed) Text(stringResource(R.string.question_image_unavailable)) else AppLoadingIndicator()
                }
                // Sliders provide keyboard/TalkBack alternatives to every gesture, including panning.
                Column(Modifier.fillMaxWidth().then(if (short) Modifier else
                    Modifier.heightIn(max = QuestionImageGeometry.imageControlsMaxHeight).verticalScroll(rememberScrollState()))) {
                    ImageViewSlider(stringResource(R.string.image_zoom), scale, 1f..5f) { scale = it; if (it == 1f) pan = Offset.Zero }
                    val bounds = limits(scale)
                    if (bounds.x > 0) ImageViewSlider(stringResource(R.string.image_pan_horizontal), pan.x, -1f..1f) { pan = pan.copy(x = it) }
                    if (bounds.y > 0) ImageViewSlider(stringResource(R.string.image_pan_vertical), pan.y, -1f..1f) { pan = pan.copy(y = it) }
                }
            }
        }
    }
    }
}

@Composable
private fun ImageViewSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    val description = if (range.start == 1f) stringResource(R.string.reading_percent, formatNumber((value * 100).roundToInt()))
        else stringResource(R.string.reading_percent, formatNumber(((value + 1) * 50).roundToInt()))
    Text("$label · $description", style = MaterialTheme.typography.labelMedium)
    Slider(value, onChange, valueRange = range, modifier = Modifier.fillMaxWidth().semantics {
        contentDescription = label; stateDescription = description
    })
}

private sealed interface ImageState {
    data object Loading : ImageState
    data object Error : ImageState
    data class Success(val bytes: ByteArray, val bitmap: ImageBitmap) : ImageState
}

private object QuestionImageGeometry {
    val imageControlsMaxHeight = 160.dp
    const val questionImageAspectRatio = 1.6f
}
