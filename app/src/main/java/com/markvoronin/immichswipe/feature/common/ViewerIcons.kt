package com.markvoronin.immichswipe.feature.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Squashes an icon and lets it spring back past full size. */
internal fun CoroutineScope.bounce(scale: Animatable<Float, AnimationVector1D>, motionScheme: MotionScheme) = launch {
    scale.animateTo(0.7f, motionScheme.fastEffectsSpec())
    scale.animateTo(1f, motionScheme.fastSpatialSpec(), initialVelocity = 6f)
}

/** The heart, scaled by [scale], with a ring of dots flying out from behind it while [burst] runs from 0 to 1. */
@Composable
internal fun FavoriteIcon(isFavorite: Boolean, scale: () -> Float, burst: () -> Float, contentDescription: String?) {
    val color = LocalContentColor.current
    Icon(
        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
        contentDescription = contentDescription,
        modifier = Modifier
            .drawBehind {
                val p = burst()
                if (p >= 1f) return@drawBehind
                // Stays inside the button's 6dp padding, which clips its content.
                val distance = lerp(10.dp.toPx(), 16.dp.toPx(), p)
                val radius = 3.dp.toPx() * (1f - p).coerceAtLeast(0f)
                repeat(6) { i ->
                    val angle = i * PI / 3 - PI / 2
                    drawCircle(color, radius, center + Offset(cos(angle).toFloat(), sin(angle).toFloat()) * distance)
                }
            }
            .graphicsLayer {
                scaleX = scale()
                scaleY = scale()
            }
    )
}

// Material's volume_up speaker in a 24-unit box; the waves are drawn as arcs so they can shrink away one by one.
private val SpeakerPath = Path().apply {
    moveTo(3f, 9f); lineTo(3f, 15f); lineTo(7f, 15f); lineTo(12f, 20f); lineTo(12f, 4f); lineTo(7f, 9f); close()
}

private fun DrawScope.soundWave(radius: Float, sweep: Float, visible: Float, color: Color) {
    if (visible <= 0f) return
    scale(lerp(0.4f, 1f, visible), pivot = Offset(12f, 12f)) {
        drawArc(
            color = color,
            startAngle = -sweep / 2,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = Offset(12f - radius, 12f - radius),
            size = Size(radius * 2, radius * 2),
            alpha = visible,
            style = Stroke(width = 2f, cap = StrokeCap.Round)
        )
    }
}

/** A speaker whose waves shrink away, outer first, before a slash cuts across it; reversed when unmuting. */
@Composable
internal fun VolumeIcon(isMuted: Boolean, scale: () -> Float, contentDescription: String?) {
    val color = LocalContentColor.current
    val progress = animateFloatAsState(
        targetValue = if (isMuted) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "MuteMorph"
    )
    Canvas(
        modifier = Modifier
            .size(24.dp)
            .graphicsLayer {
                scaleX = scale()
                scaleY = scale()
                // The slash clears a gap around itself, which needs its own layer.
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier)
    ) {
        val p = progress.value
        scale(size.width / 24f, pivot = Offset.Zero) {
            drawPath(SpeakerPath, color)
            soundWave(radius = 7.5f, sweep = 110f, visible = 1f - (p / 0.5f).coerceIn(0f, 1f), color = color)
            soundWave(radius = 4f, sweep = 100f, visible = 1f - ((p - 0.2f) / 0.5f).coerceIn(0f, 1f), color = color)
            val slash = ((p - 0.3f) / 0.7f).coerceIn(0f, 1f)
            if (slash > 0f) {
                val start = Offset(3.5f, 3.5f)
                val end = start + (Offset(20.5f, 20.5f) - start) * slash
                drawLine(Color.Black, start, end, strokeWidth = 5f, cap = StrokeCap.Round, blendMode = BlendMode.Clear)
                drawLine(color, start, end, strokeWidth = 2f, cap = StrokeCap.Round)
            }
        }
    }
}

// Material's play_arrow split down the middle into two quads, each of which morphs into one of pause's bars.
// Points run top-start, top-end, bottom-end, bottom-start in a 24-unit box.
private val PlayStart = floatArrayOf(8f, 5f, 13.5f, 8.5f, 13.5f, 15.5f, 8f, 19f)
private val PlayEnd = floatArrayOf(13.5f, 8.5f, 19f, 12f, 19f, 12f, 13.5f, 15.5f)
private val PauseStart = floatArrayOf(6f, 5f, 10f, 5f, 10f, 19f, 6f, 19f)
private val PauseEnd = floatArrayOf(14f, 5f, 18f, 5f, 18f, 19f, 14f, 19f)

/** Play icon at [progress] 0, pause icon at 1, morphing and turning half a turn in between (pause looks the same upside down). */
@Composable
internal fun PlayPauseIcon(progress: () -> Float, contentDescription: String, color: Color) {
    val path = remember { Path() }
    Canvas(modifier = Modifier.size(24.dp).semantics { this.contentDescription = contentDescription }) {
        val t = progress()
        val unit = size.width / 24f
        path.reset()
        for ((from, to) in listOf(PlayStart to PauseStart, PlayEnd to PauseEnd)) {
            for (i in 0 until 4) {
                val x = lerp(from[i * 2], to[i * 2], t) * unit
                val y = lerp(from[i * 2 + 1], to[i * 2 + 1], t) * unit
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
        }
        rotate(180f * t) { drawPath(path, color) }
    }
}
