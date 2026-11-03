package com.markvoronin.immichswipe.feature.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

// Loads shorter than this never show a spinner, avoiding a flash on fast loads.
private const val MediaLoaderDelayMs = 300L

/** Fades [content] in only after [MediaLoaderDelayMs], so loads shorter than that show nothing. */
@Composable
fun DelayedVisibility(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(MediaLoaderDelayMs.milliseconds)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(150)),
        exit = ExitTransition.None,
    ) {
        content()
    }
}

/**
 * Wavy ring for screen/section/dialog loads (replaces the plain circular spinner).
 * With [delayed] the ring appears only after a short delay; its space is reserved so layout doesn't jump.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WavyLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp? = null,
    strokeWidth: Dp = 4.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    delayed: Boolean = false,
) {
    val strokePx = with(LocalDensity.current) { strokeWidth.toPx() }
    val ring: @Composable (Modifier) -> Unit = { ringModifier ->
        CircularWavyProgressIndicator(
            modifier = ringModifier,
            color = color,
            stroke = Stroke(width = strokePx, cap = StrokeCap.Round),
            trackStroke = Stroke(width = strokePx, cap = StrokeCap.Round),
        )
    }
    if (delayed) {
        Box(modifier = modifier.size(size ?: 48.dp)) {
            DelayedVisibility { ring(Modifier.fillMaxSize()) }
        }
    } else {
        ring(if (size != null) modifier.size(size) else modifier)
    }
}

/** Wavy bar for loading/sync progress; indeterminate when [progress] is null. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WavyLoadingBar(
    modifier: Modifier = Modifier,
    progress: (() -> Float)? = null,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    if (progress != null) {
        LinearWavyProgressIndicator(progress = progress, modifier = modifier, color = color)
    } else {
        LinearWavyProgressIndicator(modifier = modifier, color = color)
    }
}

/** Small solid morphing-shape spinner for inline status rows; appears after a short delay. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SolidShapeLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Box(modifier = modifier.size(size)) {
        DelayedVisibility {
            LoadingIndicator(modifier = Modifier.fillMaxSize(), color = color)
        }
    }
}

/**
 * Morphing shape loader for spinners over photos/videos. Uses its own shape set so it reads
 * differently from the default one used by pull-to-refresh. Appears only after [MediaLoaderDelayMs]
 * and fades in, so quick loads show nothing.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MediaLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
) {
    DelayedVisibility(modifier) {
        ContainedLoadingIndicator(
            modifier = Modifier.size(size),
            containerColor = containerColor,
            indicatorColor = MaterialTheme.colorScheme.onPrimaryContainer,
            polygons = listOf(
                MaterialShapes.Cookie6Sided,
                MaterialShapes.Clover4Leaf,
                MaterialShapes.Flower,
                MaterialShapes.Gem,
            ),
        )
    }
}
