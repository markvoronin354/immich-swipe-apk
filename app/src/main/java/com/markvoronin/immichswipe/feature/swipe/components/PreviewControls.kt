package com.markvoronin.immichswipe.feature.swipe.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.markvoronin.immichswipe.R

private fun Modifier.glassCircle(backdrop: Backdrop): Modifier = drawBackdrop(
    backdrop = backdrop,
    shape = { CircleShape },
    shadow = null,
    effects = {
        vibrancy()
        blur(2.dp.toPx())
        lens(8.dp.toPx(), 16.dp.toPx(), true)
    },
    onDrawSurface = { drawRect(Color.Black.copy(alpha = 0.25f)) }
)

@Composable
fun PreviewControls(
    backdrop: Backdrop,
    visible: Boolean,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClose: () -> Unit,
    onUndo: (() -> Unit)? = null,
    isLandscape: Boolean,
    controlsOffset: Dp,
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
            val heartScale = animateFloatAsState(
                if (isFavorite) 1.2f else 1f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "HeartScale"
            ).value

            // Favorite Button
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = (if (isLandscape) 20.dp else 106.dp) + controlsOffset)
                    .graphicsLayer {
                        scaleX = heartScale
                        scaleY = heartScale
                    }
                    .glassCircle(backdrop)
                    .clip(CircleShape)
                    .clickable { onToggleFavorite() }
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = if (isFavorite) Color.Red else Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                )
            }

            // Close Button
            // Plain boxes rather than IconButton: its own container drew a second ring inside the glass.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(vertical = 50.dp, horizontal = 20.dp)
                    .glassCircle(backdrop)
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onClose)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.common_close),
                    tint = Color.White
                )
            }
            
            // Undo Button
            if (onUndo != null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 24.dp, bottom = (if (isLandscape) 20.dp else 100.dp) + controlsOffset)
                        .glassCircle(backdrop)
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onUndo)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = stringResource(R.string.swipe_undo),
                        tint = Color.White
                    )
                }
            }

            // Mute Button
            if (showMuteButton && assetType == "VIDEO") {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = (if (isLandscape) 20.dp else 100.dp) + controlsOffset, end = 24.dp)
                        .glassCircle(backdrop)
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onToggleMute)
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = stringResource(R.string.cd_mute),
                        tint = Color.White
                    )
                }
            }
        }
    }
}
