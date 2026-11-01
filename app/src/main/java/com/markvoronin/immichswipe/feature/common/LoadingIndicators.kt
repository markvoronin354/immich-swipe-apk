package com.markvoronin.immichswipe.feature.common

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Wavy ring for screen/section/dialog loads (replaces the plain circular spinner). */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WavyLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp? = null,
    strokeWidth: Dp = 4.dp,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val strokePx = with(LocalDensity.current) { strokeWidth.toPx() }
    CircularWavyProgressIndicator(
        modifier = if (size != null) modifier.size(size) else modifier,
        color = color,
        stroke = Stroke(width = strokePx, cap = StrokeCap.Round),
        trackStroke = Stroke(width = strokePx, cap = StrokeCap.Round),
    )
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

/** Small solid morphing-shape spinner for inline status rows. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SolidShapeLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    LoadingIndicator(modifier = modifier.size(size), color = color)
}

/**
 * Morphing shape loader for spinners over photos/videos. Uses its own shape set so it reads
 * differently from the default one used by pull-to-refresh.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MediaLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
) {
    ContainedLoadingIndicator(
        modifier = modifier.size(size),
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        indicatorColor = MaterialTheme.colorScheme.onPrimaryContainer,
        polygons = listOf(
            MaterialShapes.Cookie6Sided,
            MaterialShapes.Clover4Leaf,
            MaterialShapes.Flower,
            MaterialShapes.Gem,
        ),
    )
}
