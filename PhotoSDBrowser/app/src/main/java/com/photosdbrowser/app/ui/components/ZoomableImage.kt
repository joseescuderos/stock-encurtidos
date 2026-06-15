package com.photosdbrowser.app.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import coil.compose.AsyncImage

private const val MIN_SCALE = 1f
private const val MAX_SCALE = 5f

/**
 * Full-screen image with pinch-to-zoom and drag-to-pan once zoomed in. Double-tap resets to fit.
 * Reports the current scale via [onScaleChanged] so the caller can disable pager swipes while
 * the user is panning around a zoomed-in image.
 */
@Composable
fun ZoomableImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    onScaleChanged: (Float) -> Unit = {}
) {
    var scale by remember { mutableFloatStateOf(MIN_SCALE) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(Offset.Zero) }

    fun updateScale(newScale: Float) {
        scale = newScale
        onScaleChanged(newScale)
    }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
                    offset = if (newScale <= MIN_SCALE) {
                        Offset.Zero
                    } else {
                        val maxX = (containerSize.x * (newScale - 1f)) / 2f
                        val maxY = (containerSize.y * (newScale - 1f)) / 2f
                        Offset(
                            x = (offset.x + pan.x * scale).coerceIn(-maxX, maxX),
                            y = (offset.y + pan.y * scale).coerceIn(-maxY, maxY)
                        )
                    }
                    updateScale(newScale)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        offset = Offset.Zero
                        updateScale(MIN_SCALE)
                    }
                )
            }
            .onSizeChanged { containerSize = Offset(it.width.toFloat(), it.height.toFloat()) }
    ) {
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
        )
    }
}
