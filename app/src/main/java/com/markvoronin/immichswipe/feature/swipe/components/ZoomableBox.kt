package com.markvoronin.immichswipe.feature.swipe.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animate
import androidx.compose.foundation.gestures.PressGestureScope
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.launch

@Composable
fun ZoomableBox(
    modifier: Modifier = Modifier,
    resetOnRelease: Boolean = false,
    enabled: Boolean = true,
    aspectRatio: Float? = null,
    isFillMode: Boolean = false,
    enableDoubleTapZoom: Boolean = true,
    onTap: ((Offset, IntSize) -> Unit)? = null,
    onDoubleTap: (() -> Unit)? = null,
    onPress: (suspend PressGestureScope.(Offset, IntSize) -> Unit)? = null,
    onIsZoomedChanged: ((Boolean) -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    if (!enabled) {
        Box(modifier = modifier, content = content)
        return
    }

    val scope = rememberCoroutineScope()
    val motionScheme = MaterialTheme.motionScheme
    var fillScale by remember(aspectRatio) { mutableFloatStateOf(1f) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }

    val animatedScale = remember { Animatable(1f) }
    val animatedOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }

    val currentScale = if (resetOnRelease) animatedScale.value else scale
    val isZoomedIn = currentScale > 1.05f
    val currentIsZoomedIn by rememberUpdatedState(isZoomedIn)

    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnDoubleTap by rememberUpdatedState(onDoubleTap)
    val currentOnPress by rememberUpdatedState(onPress)

    LaunchedEffect(isZoomedIn) {
        onIsZoomedChanged?.invoke(isZoomedIn)
    }

    // Limits the offset so the scaled image (fitted by aspectRatio) never pans past its own edges.
    fun clampOffset(o: Offset, s: Float): Offset {
        val w = boxSize.width.toFloat()
        val h = boxSize.height.toFloat()
        if (w <= 0f || h <= 0f) return o
        var contentW = w
        var contentH = h
        if (aspectRatio != null) {
            if (aspectRatio > w / h) contentH = w / aspectRatio else contentW = h * aspectRatio
        }
        val maxX = ((contentW * s - w) / 2f).coerceAtLeast(0f)
        val maxY = ((contentH * s - h) / 2f).coerceAtLeast(0f)
        return Offset(o.x.coerceIn(-maxX, maxX), o.y.coerceIn(-maxY, maxY))
    }

    fun resetZoom() {
        if (resetOnRelease) {
            scope.launch {
                launch { animatedScale.animateTo(if (isFillMode) fillScale else 1f, motionScheme.fastEffectsSpec()) }
                launch { animatedOffset.animateTo(Offset.Zero, motionScheme.fastEffectsSpec()) }
            }
        } else {
            scope.launch {
                launch { animate(scale, 1f, animationSpec = motionScheme.fastEffectsSpec()) { v, _ -> scale = v } }
                launch { animate(typeConverter = Offset.VectorConverter, initialValue = offset, targetValue = Offset.Zero, animationSpec = motionScheme.fastEffectsSpec()) { v, _ -> offset = v } }
            }
        }
    }

    LaunchedEffect(isFillMode, fillScale) {
        val target = if (isFillMode) fillScale else 1f
        if (resetOnRelease) {
            launch { animatedScale.animateTo(target, motionScheme.fastSpatialSpec()) }
            launch { animatedOffset.animateTo(Offset.Zero, motionScheme.fastSpatialSpec()) }
        } else {
            scale = target
            offset = Offset.Zero
        }
    }

    Box(
        modifier = modifier
            .clipToBounds()
            .onSizeChanged { intSize ->
                boxSize = intSize
                if (aspectRatio != null && intSize.width > 0 && intSize.height > 0) {
                    val boxRatio = intSize.width.toFloat() / intSize.height.toFloat()
                    fillScale = if (aspectRatio > boxRatio) {
                        aspectRatio / boxRatio
                    } else {
                        boxRatio / aspectRatio
                    }
                }
            }
            .pointerInput(resetOnRelease, isFillMode, fillScale) {
                awaitEachGesture {
                    do {
                        val event = awaitPointerEvent()
                        val zoomChange = event.calculateZoom()
                        val panChange = event.calculatePan()
                        val centroid = event.calculateCentroid(useCurrent = false)
                        val pressedCount = event.changes.count { it.pressed }

                        if (pressedCount >= 2) {
                            if (zoomChange != 1f || panChange != Offset.Zero) {
                                val oldScale = if (resetOnRelease) animatedScale.value else scale
                                val newScale = (oldScale * zoomChange).coerceIn(0.7f, 10f)
                                val effectiveZoomChange = if (oldScale > 0.0001f) newScale / oldScale else 1f

                                val oldOffset = if (resetOnRelease) animatedOffset.value else offset
                                val rawOffset = (centroid - size.toSize().center) * (1f - effectiveZoomChange) + (oldOffset * effectiveZoomChange) + panChange
                                // Keep the content within bounds so zooming out never leaves a large offset to snap back from.
                                val newOffset = clampOffset(rawOffset, newScale)

                                if (resetOnRelease) {
                                    scope.launch {
                                        animatedScale.snapTo(newScale)
                                        animatedOffset.snapTo(newOffset)
                                    }
                                } else {
                                    scale = newScale
                                    offset = newOffset
                                }
                                event.changes.forEach { it.consume() }
                            }
                        } else if (pressedCount == 1 && (if (resetOnRelease) animatedScale.value else scale) > 1.05f) {
                            if (panChange != Offset.Zero) {
                                if (resetOnRelease) {
                                    scope.launch {
                                        animatedOffset.snapTo(clampOffset(animatedOffset.value + panChange, animatedScale.value))
                                    }
                                } else {
                                    offset = clampOffset(offset + panChange, scale)
                                }
                                event.changes.forEach { it.consume() }
                            }
                        }
                    } while (event.changes.any { it.pressed })

                    if (resetOnRelease || scale < 1.01f) {
                        resetZoom()
                    }
                }
            }
            .pointerInput(enableDoubleTapZoom, onDoubleTap != null) {
                val shouldHandleDoubleTap = enableDoubleTapZoom || onDoubleTap != null
                detectTapGestures(
                    onTap = { tapOffset -> currentOnTap?.invoke(tapOffset, boxSize) },
                    onDoubleTap = if (shouldHandleDoubleTap) {
                        { tapOffset ->
                            if (currentIsZoomedIn) {
                                resetZoom()
                            } else if (currentOnDoubleTap != null) {
                                currentOnDoubleTap?.invoke()
                            } else if (!resetOnRelease) {
                                val targetScale = 3f
                                val zoomChange = targetScale / scale
                                val targetOffset = clampOffset((tapOffset - boxSize.toSize().center) * (1f - zoomChange) + offset * zoomChange, targetScale)
                                scope.launch {
                                    launch { animate(scale, targetScale, animationSpec = motionScheme.fastSpatialSpec()) { v, _ -> scale = v } }
                                    launch { animate(typeConverter = Offset.VectorConverter, initialValue = offset, targetValue = targetOffset, animationSpec = motionScheme.fastSpatialSpec()) { v, _ -> offset = v } }
                                }
                            }
                        }
                    } else null,
                    onPress = { tapOffset ->
                        currentOnPress?.invoke(this, tapOffset, boxSize)
                    }
                )
            }
            .graphicsLayer {
                val s = if (resetOnRelease) animatedScale.value else scale
                val o = if (resetOnRelease) animatedOffset.value else offset
                scaleX = s
                scaleY = s
                translationX = o.x
                translationY = o.y
            },
        content = content
    )
}
