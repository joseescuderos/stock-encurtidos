package com.photosdbrowser.app.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import coil.compose.AsyncImage
import kotlin.math.abs

private const val MIN_SCALE = 1f
private const val MAX_SCALE = 5f

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

    fun applyTransform(pan: Offset, zoom: Float) {
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

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var prevCount = 1
                    var prevCentroid = Offset.Zero

                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type != PointerEventType.Move) {
                            if (event.changes.none { it.pressed }) break
                            continue
                        }
                        val pressed = event.changes.filter { it.pressed }
                        val count = pressed.size
                        val centroid = pressed.fold(Offset.Zero) { acc, c -> acc + c.position } / count.toFloat()

                        if (count >= 2) {
                            // Pinch: always handle regardless of zoom state
                            val prevPositions = pressed.map { it.previousPosition }
                            val curPositions = pressed.map { it.position }
                            val prevSpan = (prevPositions[0] - prevPositions[1]).getDistance()
                            val curSpan = (curPositions[0] - curPositions[1]).getDistance()
                            val zoom = if (prevSpan > 0.001f) curSpan / prevSpan else 1f
                            val pan = centroid - prevCentroid
                            applyTransform(pan, zoom)
                            pressed.fastForEach { it.consume() }
                        } else if (scale > MIN_SCALE + 0.01f) {
                            // Single finger pan only when zoomed in
                            val pan = centroid - prevCentroid
                            if (prevCount == count && pan != Offset.Zero) {
                                applyTransform(pan, 1f)
                                pressed.fastForEach { it.consume() }
                            }
                        }
                        // When NOT zoomed AND single finger → don't consume → pager gets the swipe

                        prevCount = count
                        prevCentroid = centroid
                    }
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
