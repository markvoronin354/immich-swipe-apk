package com.markvoronin.immichswipe.feature.swipe.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
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

/** Bottom inset of the floating toolbar, the same for photos and videos. */
internal fun previewToolbarBottom(isLandscape: Boolean): Dp = if (isLandscape) 12.dp else 24.dp

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
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // The toolbar sits at the very bottom; the seekbar and size pill sit above it.
            val isVideo = assetType == "VIDEO"

            // Translucent so the media shows through; no shadow, since it would show through the container too.
            val toolbarColors = FloatingToolbarDefaults.vibrantFloatingToolbarColors()
            HorizontalFloatingToolbar(
                expanded = true,
                colors = toolbarColors.copy(
                    toolbarContainerColor = toolbarColors.toolbarContainerColor.copy(alpha = 0.7f)
                ),
                expandedShadowElevation = 0.dp,
                // Shorter than the 64dp default: 6dp above and below the 40dp buttons.
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = previewToolbarBottom(isLandscape))
                    .height(PreviewToolbarHeight)
            ) {
                if (onUndo != null) {
                    IconButton(onClick = onUndo, shapes = IconButtonDefaults.shapes()) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = stringResource(R.string.swipe_undo)
                        )
                    }
                }

                IconToggleButton(
                    checked = isFavorite,
                    onCheckedChange = { onToggleFavorite() },
                    shapes = IconButtonDefaults.toggleableShapes()
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null
                    )
                }

                if (showMuteButton && isVideo) {
                    IconToggleButton(
                        checked = isMuted,
                        onCheckedChange = { onToggleMute() },
                        shapes = IconButtonDefaults.toggleableShapes()
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
