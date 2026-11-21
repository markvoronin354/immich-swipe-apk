package com.markvoronin.immichswipe.feature.swipe.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.markvoronin.immichswipe.R

private const val StarCount = 5

/** The toast text for the result of rating a photo. */
@Composable
fun rememberRatingMessage(): (success: Boolean, rating: Int, missingPermission: String?) -> String {
    val successTemplate = stringResource(R.string.rating_success)
    val errorMessage = stringResource(R.string.rating_error)
    val permissionErrorTemplate = stringResource(R.string.rating_permission_error)
    return { success, rating, missingPermission ->
        when {
            success -> successTemplate.format(rating)
            missingPermission != null -> permissionErrorTemplate.format(missingPermission)
            else -> errorMessage
        }
    }
}

/**
 * Five stars to drop the dragged card on, rising over the toolbar while a card is pulled down.
 * The stars up to the one under the finger fill in; each reports its place to [state] under its number.
 * Until the finger is over one, the stars fill to the photo's [currentRating] (0 when unrated) in a quieter color.
 */
@Composable
fun RatingDropStrip(
    state: AlbumDropState,
    currentRating: Int,
    modifier: Modifier = Modifier,
) {
    val motionScheme = MaterialTheme.motionScheme
    val haptic = LocalHapticFeedback.current
    val rating = state.hoveredId?.toIntOrNull() ?: 0
    // Held once the drag ends, so the next photo's rating doesn't flash in while the strip slides away.
    var shownRating by remember { mutableIntStateOf(currentRating) }
    LaunchedEffect(currentRating, state.isActive) {
        if (state.isActive) shownRating = currentRating
    }
    val lit = if (rating > 0) rating else shownRating
    LaunchedEffect(rating) {
        if (rating > 0) haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
    }
    LaunchedEffect(state.isActive) {
        if (!state.isActive) state.tileRects.clear()
    }

    AnimatedVisibility(
        visible = state.isActive,
        enter = slideInVertically(motionScheme.defaultSpatialSpec()) { it } + fadeIn(motionScheme.defaultEffectsSpec()),
        exit = slideOutVertically(motionScheme.fastSpatialSpec()) { it } + fadeOut(motionScheme.fastEffectsSpec()),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (star in 1..StarCount) {
                    DisposableEffect(star) { onDispose { state.tileRects.remove(star.toString()) } }
                    StarTile(
                        filled = star <= lit,
                        hovered = rating > 0,
                        selected = star == rating,
                        onPositioned = { state.tileRects[star.toString()] = it }
                    )
                }
            }
        }
    }
}

@Composable
private fun StarTile(
    filled: Boolean,
    hovered: Boolean,
    selected: Boolean,
    onPositioned: (Rect) -> Unit,
) {
    val motionScheme = MaterialTheme.motionScheme
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.25f else 1f,
        animationSpec = motionScheme.fastSpatialSpec(),
        label = "StarTileScale"
    )
    val tint by animateColorAsState(
        targetValue = when {
            !filled -> MaterialTheme.colorScheme.onSurfaceVariant
            hovered -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.secondary
        },
        animationSpec = motionScheme.fastEffectsSpec(),
        label = "StarTileTint"
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(56.dp)
            // Measured before the scale, so a star growing under the finger can't push itself away from it.
            .onGloballyPositioned { onPositioned(Rect(it.positionOnScreen(), it.size.toSize())) }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Icon(
            imageVector = if (filled) Icons.Default.Star else Icons.Default.StarBorder,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(44.dp)
        )
    }
}

/** Five stars to tap, for rating where there is no strip to drop the card on. */
@Composable
fun RatingDialog(
    onRate: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rating_dialog_title)) },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (star in 1..StarCount) {
                    IconButton(onClick = { onRate(star) }) {
                        Icon(
                            imageVector = Icons.Default.StarBorder,
                            contentDescription = stringResource(R.string.rating_star_description, star),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}
