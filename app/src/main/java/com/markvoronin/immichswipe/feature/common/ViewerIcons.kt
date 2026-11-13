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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
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

// Material's file_download arrow, drawn apart from its tray so it can fall in on its own.
private val DownloadArrowPath = Path().apply {
    moveTo(19f, 9f); lineTo(15f, 9f); lineTo(15f, 3f); lineTo(9f, 3f); lineTo(9f, 9f); lineTo(5f, 9f); lineTo(12f, 16f); close()
}

/** The download icon, its arrow moved down by [drop]: gone into the tray at 1, and dropping back in from above between -1 and 0. */
@Composable
internal fun DownloadIcon(drop: () -> Float, scale: () -> Float, contentDescription: String?) {
    val color = LocalContentColor.current
    Canvas(
        modifier = Modifier
            .size(24.dp)
            .graphicsLayer {
                scaleX = scale()
                scaleY = scale()
            }
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier)
    ) {
        scale(size.width / 24f, pivot = Offset.Zero) {
            clipRect(left = 0f, top = 0f, right = 24f, bottom = 17f) {
                translate(top = drop() * 16f) { drawPath(DownloadArrowPath, color) }
            }
            drawRect(color, topLeft = Offset(5f, 18f), size = Size(14f, 2f))
        }
    }
}

// Material's lock shackle as a centre line, stroked 1.9 wide; its legs reach into the body.
private val LockShacklePath = Path().apply {
    moveTo(7.95f, 9f)
    lineTo(7.95f, 6f)
    arcTo(Rect(7.95f, 1.95f, 16.05f, 10.05f), 180f, 180f, false)
    lineTo(16.05f, 9f)
}

// The body with its keyhole cut out by the even-odd fill, so no offscreen layer clips the raised shackle.
private val LockBodyPath = Path().apply {
    fillType = PathFillType.EvenOdd
    addRoundRect(RoundRect(4f, 8f, 20f, 22f, CornerRadius(2f)))
    addOval(Rect(center = Offset(12f, 15f), radius = 2f))
}

/** Material's lock with its shackle raised by [lift]: shut at 0, open at 1. */
@Composable
internal fun LockIcon(lift: () -> Float, scale: () -> Float, contentDescription: String?) {
    val color = LocalContentColor.current
    Canvas(
        modifier = Modifier
            .size(24.dp)
            .graphicsLayer {
                scaleX = scale()
                scaleY = scale()
            }
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier)
    ) {
        scale(size.width / 24f, pivot = Offset.Zero) {
            drawPath(LockBodyPath, color)
            translate(top = -lift() * 3f) { drawPath(LockShacklePath, color, style = Stroke(width = 1.9f)) }
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
