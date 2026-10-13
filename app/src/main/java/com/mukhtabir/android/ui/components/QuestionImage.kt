package com.mukhtabir.android.ui.components

import com.mukhtabir.android.ui.components.AppLoadingIndicator

import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import com.mukhtabir.android.ui.components.AppTextButton as TextButton
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import com.mukhtabir.android.R
import kotlinx.coroutines.CancellationException
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.mukhtabir.android.data.remote.ImageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.mukhtabir.android.ui.theme.AppTheme

@Composable
fun QuestionImage(
    url: String?,
    repository: ImageRepository,
    modifier: Modifier = Modifier,
    downloadedImage: String? = null,
) {
    if (url.isNullOrBlank()) return

    var state by remember(url, repository) { mutableStateOf<ImageState>(ImageState.Loading) }
    var retry by remember(url, repository) { mutableStateOf(0) }
    LaunchedEffect(url, repository, retry, downloadedImage) {
        state = ImageState.Loading
        state = try {
            val bytes = downloadedImage?.let { android.util.Base64.decode(it, android.util.Base64.NO_WRAP) }
                ?: repository.load(url)
            val bitmap = withContext(Dispatchers.Default) {
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    ?: error("Unsupported question image")
            }
            ImageState.Success(bitmap.asImageBitmap())
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            ImageState.Error
        }
    }

    // Loading/display use the fitted area; failures keep their Retry in a compact panel.
    // Questions without a URL returned above and reserve no image space.
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val panelHeight = if (state is ImageState.Error) 96.dp else
            (maxWidth / AppLayoutTokens.questionImageAspectRatio).coerceAtMost(AppLayoutTokens.imageMaxHeight)
        Surface(Modifier.fillMaxWidth().height(panelHeight), color = AppTheme.questionSurfaces.choice,
            shape = MaterialTheme.shapes.medium, border = BorderStroke(1.dp, AppTheme.questionSurfaces.border)) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                when (val current = state) {
                    ImageState.Loading -> AppLoadingIndicator()
                    ImageState.Error -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                        .padding(AppLayoutTokens.cardInsets), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.content)) {
                        Text(stringResource(R.string.question_image_unavailable), color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                        TextButton(onClick = { retry += 1 }) { Text(stringResource(R.string.retry)) }
                    }
                    is ImageState.Success -> Image(bitmap = current.bitmap,
                        contentDescription = stringResource(R.string.question_image_description),
                        modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                }
            }
        }
    }
}

private sealed interface ImageState {
    data object Loading : ImageState
    data object Error : ImageState
    data class Success(val bitmap: androidx.compose.ui.graphics.ImageBitmap) : ImageState
}
