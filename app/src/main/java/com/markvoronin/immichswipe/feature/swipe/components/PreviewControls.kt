package com.markvoronin.immichswipe.feature.swipe.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButtonColors
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.feature.settings.components.rememberJiggleGroup

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

/** Height of the bottom floating toolbar; VideoControls sits above it in fullscreen. */
internal val PreviewToolbarHeight = 52.dp

/** Translucent tile color shared by the fullscreen toolbar buttons, close button and seekbar track. */
@Composable
internal fun previewTileColor(): Color =
    FloatingToolbarDefaults.vibrantFloatingToolbarColors().toolbarContainerColor.copy(alpha = 0.7f)

/** Colors of a tile in the fullscreen toolbar. */
@Composable
internal fun previewButtonColors(): IconButtonColors = IconButtonDefaults.filledTonalIconButtonColors(
    containerColor = previewTileColor(),
    contentColor = FloatingToolbarDefaults.vibrantFloatingToolbarColors().toolbarContentColor
)

/** Like [previewButtonColors], but a checked tile inverts to the content color. */
@Composable
internal fun previewToggleColors(): IconToggleButtonColors {
    val toolbarColors = FloatingToolbarDefaults.vibrantFloatingToolbarColors()
    return IconButtonDefaults.filledTonalIconToggleButtonColors(
        containerColor = previewTileColor(),
        contentColor = toolbarColors.toolbarContentColor,
        checkedContainerColor = toolbarColors.toolbarContentColor,
        checkedContentColor = toolbarColors.toolbarContainerColor
    )
}

/** Space between the bottom toolbar and the seekbar or size pill above it. */
internal val PreviewToolbarGap = 12.dp

/** Bottom inset of the floating toolbar, the same for photos and videos. */
internal fun previewToolbarBottom(isLandscape: Boolean): Dp = if (isLandscape) 12.dp else 24.dp

/** Shape of tile [index] in a connected group of [count]: fully rounded outer ends, nearly square inner corners. */
internal fun connectedTileShape(index: Int, count: Int, outer: Dp = 26.dp): RoundedCornerShape {
    val inner = 6.dp
    val start = if (index == 0) outer else inner
    val end = if (index == count - 1) outer else inner
    return RoundedCornerShape(topStart = start, bottomStart = start, topEnd = end, bottomEnd = end)
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
    assetType: String
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(MaterialTheme.motionScheme.slowEffectsSpec()),
        exit = fadeOut(MaterialTheme.motionScheme.slowEffectsSpec())
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // The toolbar sits at the very bottom; the seekbar and size pill sit above it.
            val isVideo = assetType == "VIDEO"

            // A connected button group: separate tiles with a gap showing the media between them.
            // Only the outer ends are fully rounded; the inner corners stay nearly square.
            val buttonColors = previewButtonColors()
            val toggleColors = previewToggleColors()
            val showMute = showMuteButton && isVideo
            val buttonCount = (if (onUndo != null) 1 else 0) + 1 + (if (showMute) 1 else 0)
            // The tapped tile grows and its neighbours give way; the widths always sum to the same total.
            val jiggle = rememberJiggleGroup(buttonCount, grow = 0.14f)
            val pressedShape = RoundedCornerShape(10.dp)
            val undoIndex = if (onUndo != null) 0 else -1
            val favoriteIndex = undoIndex + 1
            val muteIndex = favoriteIndex + 1
            // A checked tile widens and the others narrow; the toolbar keeps its total width.
            val undoWeight = if (onUndo != null) jiggle.factor(undoIndex) else 0f
            val favoriteWeight = jiggle.factor(favoriteIndex) * checkedWidthFactor(isFavorite)
            val muteWeight = if (showMute) jiggle.factor(muteIndex) * checkedWidthFactor(isMuted) else 0f
            val unitWidth = 56.dp * buttonCount / (undoWeight + favoriteWeight + muteWeight)

            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = previewToolbarBottom(isLandscape))
            ) {
                if (onUndo != null) {
                    val shape = connectedTileShape(undoIndex, buttonCount)
                    FilledTonalIconButton(
                        onClick = { jiggle.tap(undoIndex); onUndo() },
                        shapes = IconButtonDefaults.shapes(shape = shape, pressedShape = pressedShape),
                        colors = buttonColors,
                        modifier = Modifier.size(width = unitWidth * undoWeight, height = PreviewToolbarHeight)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = stringResource(R.string.swipe_undo)
                        )
                    }
                }

                val favoriteShape = connectedTileShape(favoriteIndex, buttonCount)
                FilledTonalIconToggleButton(
                    checked = isFavorite,
                    onCheckedChange = { jiggle.tap(favoriteIndex); onToggleFavorite() },
                    shapes = IconButtonDefaults.toggleableShapes(
                        shape = favoriteShape,
                        pressedShape = pressedShape,
                        checkedShape = CircleShape
                    ),
                    colors = toggleColors,
                    modifier = Modifier.size(width = unitWidth * favoriteWeight, height = PreviewToolbarHeight)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null
                    )
                }

                if (showMute) {
                    val muteShape = connectedTileShape(muteIndex, buttonCount)
                    FilledTonalIconToggleButton(
                        checked = isMuted,
                        onCheckedChange = { jiggle.tap(muteIndex); onToggleMute() },
                        shapes = IconButtonDefaults.toggleableShapes(
                            shape = muteShape,
                            pressedShape = pressedShape,
                            checkedShape = CircleShape
                        ),
                        colors = toggleColors,
                        modifier = Modifier.size(width = unitWidth * muteWeight, height = PreviewToolbarHeight)
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = stringResource(R.string.cd_mute)
                        )
                    }
                }
            }

            FilledTonalIconButton(
                onClick = onClose,
                shapes = IconButtonDefaults.shapes(),
                colors = buttonColors,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(vertical = 50.dp, horizontal = 20.dp)
                    .size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.common_close)
                )
            }
        }
    }
}
