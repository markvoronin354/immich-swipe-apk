package com.markvoronin.immichswipe.feature.swipe.components

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.markvoronin.immichswipe.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// Library-free glass look: dark scrim plus a rim highlight that is brightest on opposite corners.
// `backdrop` is unused; kept so call sites don't change.
@Suppress("UNUSED_PARAMETER")
internal fun Modifier.glassCircle(backdrop: Backdrop): Modifier = this
    .background(Color.Black.copy(alpha = 0.25f), CircleShape)
    .border(
        width = 1.dp,
        brush = Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.5f), Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.35f))
        ),
        shape = CircleShape
    )

/** Height of the bottom action bar; VideoControls sits above it in fullscreen. */
internal val PreviewToolbarHeight = 64.dp

/** Space between the bottom toolbar and the seekbar or size pill above it. */
internal val PreviewToolbarGap = 12.dp

/** Bottom inset of the floating toolbar, the same for photos and videos. */
internal fun previewToolbarBottom(isLandscape: Boolean): Dp = if (isLandscape) 12.dp else 24.dp

/** Top inset of the viewer's top bar; it spans this to 64dp below it. */
internal fun previewTopBarTop(isLandscape: Boolean): Dp = if (isLandscape) 8.dp else 40.dp

/** Shape of tile [index] in a connected group of [count]: fully rounded outer ends, nearly square inner corners. */
internal fun connectedTileShape(index: Int, count: Int, outer: Dp = 26.dp): RoundedCornerShape {
    val inner = 6.dp
    val start = if (index == 0) outer else inner
    val end = if (index == count - 1) outer else inner
    return RoundedCornerShape(topStart = start, bottomStart = start, topEnd = end, bottomEnd = end)
}

/** Gradients behind the containerless top and bottom controls, so they stay legible over bright media. */
@Composable
internal fun ViewerScrim(visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(MaterialTheme.motionScheme.slowEffectsSpec()),
        exit = fadeOut(MaterialTheme.motionScheme.slowEffectsSpec())
    ) {
        val scrim = MaterialTheme.colorScheme.scrim
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(Brush.verticalGradient(listOf(scrim.copy(alpha = 0.5f), Color.Transparent)))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, scrim.copy(alpha = 0.6f))))
            )
        }
    }
}

/** The asset's date ("Nov 11", with the year if it isn't this year) and time, in the device's zone and clock format. */
@Composable
private fun rememberViewerDateTime(isoDate: String): Pair<String, String>? {
    val context = LocalContext.current
    val is24Hour = DateFormat.is24HourFormat(context)
    return remember(isoDate, is24Hour) {
        val dateTime = runCatching {
            OffsetDateTime.parse(isoDate).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
        }.recoverCatching { LocalDateTime.parse(isoDate) }.getOrNull() ?: return@remember null
        val locale = Locale.getDefault()
        val datePattern = if (dateTime.year == LocalDate.now().year) "MMMd" else "MMMdyyyy"
        val date = dateTime.format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, datePattern), locale))
        val time = dateTime.format(
            DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, if (is24Hour) "Hm" else "hm"), locale)
        )
        date to time
    }
}

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

/** A containerless bottom-bar action: icon over its label, or the icon alone when [showLabel] is off. */
@Composable
private fun LabeledAction(
    label: String,
    showLabel: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    icon: @Composable (contentDescription: String?) -> Unit
) {
    val contentColor by animateColorAsState(
        targetValue = LocalContentColor.current.copy(alpha = if (enabled) 1f else 0.38f),
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "ActionEnabled"
    )
    TextButton(
        onClick = onClick,
        enabled = enabled,
        shapes = ButtonDefaults.shapes(),
        colors = ButtonDefaults.textButtonColors(contentColor = contentColor, disabledContentColor = contentColor),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            icon(if (showLabel) null else label)
            AnimatedVisibility(
                visible = showLabel,
                enter = fadeIn(MaterialTheme.motionScheme.slowEffectsSpec()) + expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
                exit = fadeOut(MaterialTheme.motionScheme.slowEffectsSpec()) + shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec())
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

// `backdrop` is unused since the Material 3 Expressive redesign; kept so call sites don't change.
@Suppress("UNUSED_PARAMETER")
@Composable
fun PreviewControls(
    backdrop: Backdrop,
    visible: Boolean,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClose: () -> Unit,
    onUndo: (() -> Unit)? = null,
    isLandscape: Boolean,
    showMuteButton: Boolean,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    assetType: String,
    assetDate: String,
    showButtonLabels: Boolean = true,
    onShowInfo: () -> Unit = {},
    canUndo: Boolean = true
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(MaterialTheme.motionScheme.slowEffectsSpec()),
        exit = fadeOut(MaterialTheme.motionScheme.slowEffectsSpec())
    ) {
        // The viewer is always black behind its scrims, so the controls are always light.
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            // Driven by taps rather than state, so moving to an already-favourite asset doesn't celebrate.
            val scope = rememberCoroutineScope()
            val motionScheme = MaterialTheme.motionScheme
            Box(modifier = Modifier.fillMaxSize()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(top = previewTopBarTop(isLandscape))
                        .padding(horizontal = 4.dp)
                        .height(64.dp)
                ) {
                    IconButton(onClick = onClose, shapes = IconButtonDefaults.shapes()) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back)
                        )
                    }

                    val dateTime = rememberViewerDateTime(assetDate)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (dateTime != null) {
                            Text(
                                text = dateTime.first,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = dateTime.second,
                                style = MaterialTheme.typography.bodySmall,
                                color = LocalContentColor.current.copy(alpha = 0.8f),
                                maxLines = 1
                            )
                        }
                    }

                    IconButton(onClick = onShowInfo, shapes = IconButtonDefaults.shapes()) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = stringResource(R.string.cd_asset_info)
                        )
                    }
                }

                // Three fixed slots, so the buttons stay put when a photo gives way to a video; the mute slot is kept empty
                // on photos.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(bottom = previewToolbarBottom(isLandscape))
                        .height(PreviewToolbarHeight)
                ) {
                    val slot = Modifier.weight(1f)
                    Box(slot, contentAlignment = Alignment.Center) {
                        if (onUndo != null) {
                            // The arrow winds back a full turn, like rewinding, against the way it points in RTL too.
                            val undoScale = remember { Animatable(1f) }
                            val undoSpin = remember { Animatable(0f) }
                            val undoDirection = if (LocalLayoutDirection.current == LayoutDirection.Rtl) 1f else -1f
                            LabeledAction(
                                label = stringResource(R.string.swipe_undo),
                                showLabel = showButtonLabels,
                                enabled = canUndo,
                                onClick = {
                                    scope.bounce(undoScale, motionScheme)
                                    scope.launch {
                                        undoSpin.snapTo(0f)
                                        undoSpin.animateTo(360f, motionScheme.defaultSpatialSpec())
                                    }
                                    onUndo()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Undo,
                                    contentDescription = it,
                                    modifier = Modifier.graphicsLayer {
                                        scaleX = undoScale.value
                                        scaleY = undoScale.value
                                        rotationZ = undoSpin.value * undoDirection
                                    }
                                )
                            }
                        }
                    }

                    val favoriteScale = remember { Animatable(1f) }
                    val favoriteBurst = remember { Animatable(1f) }
                    Box(slot, contentAlignment = Alignment.Center) {
                        LabeledAction(
                            label = stringResource(R.string.swipe_label_favorite),
                            showLabel = showButtonLabels,
                            onClick = {
                                scope.bounce(favoriteScale, motionScheme)
                                if (!isFavorite) scope.launch {
                                    favoriteBurst.snapTo(0f)
                                    favoriteBurst.animateTo(1f, motionScheme.slowSpatialSpec())
                                }
                                onToggleFavorite()
                            }
                        ) { FavoriteIcon(isFavorite, { favoriteScale.value }, { favoriteBurst.value }, it) }
                    }

                    Box(slot, contentAlignment = Alignment.Center) {
                        val muteScale = remember { Animatable(1f) }
                        androidx.compose.animation.AnimatedVisibility(
                            visible = showMuteButton && assetType == "VIDEO",
                            enter = fadeIn(motionScheme.defaultEffectsSpec()) + scaleIn(motionScheme.defaultSpatialSpec(), initialScale = 0.6f),
                            exit = fadeOut(motionScheme.defaultEffectsSpec()) + scaleOut(motionScheme.defaultSpatialSpec(), targetScale = 0.6f)
                        ) {
                            LabeledAction(
                                label = stringResource(if (isMuted) R.string.cd_unmute else R.string.cd_mute),
                                showLabel = showButtonLabels,
                                onClick = {
                                    scope.bounce(muteScale, motionScheme)
                                    onToggleMute()
                                }
                            ) { VolumeIcon(isMuted, { muteScale.value }, it) }
                        }
                    }
                }
            }
        }
    }
}
