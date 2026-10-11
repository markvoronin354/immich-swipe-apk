package com.markvoronin.immichswipe.feature.swipe.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.markvoronin.immichswipe.core.IconPosition

@Composable
fun PreviewControls(
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
    assetType: String,
    showDownloadButton: Boolean,
    downloadButtonPosition: IconPosition,
    onDownload: () -> Unit,
    showShareButton: Boolean,
    shareButtonPosition: IconPosition,
    onShare: () -> Unit
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
                    .background(Color.Black.copy(alpha = 0.3f), CircleShape)
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
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(vertical = 50.dp, horizontal = 20.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
            
            // Undo Button
            if (onUndo != null) {
                IconButton(
                    onClick = onUndo,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 24.dp, bottom = (if (isLandscape) 20.dp else 100.dp) + controlsOffset)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo",
                        tint = Color.White
                    )
                }
            }

            // Mute Button
            if (showMuteButton && assetType == "VIDEO") {
                IconButton(
                    onClick = onToggleMute,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = (if (isLandscape) 20.dp else 100.dp) + controlsOffset, end = 24.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Mute",
                        tint = Color.White
                    )
                }
            }

            // Download Button
            if (showDownloadButton) {
                val dlAlign = when (downloadButtonPosition) {
                    IconPosition.TOP_LEFT -> Alignment.TopStart
                    IconPosition.TOP_RIGHT -> Alignment.TopEnd
                    IconPosition.BOTTOM_LEFT -> Alignment.BottomStart
                    IconPosition.BOTTOM_RIGHT -> Alignment.BottomEnd
                }

                val dlPadding = when (downloadButtonPosition) {
                    IconPosition.TOP_LEFT -> Modifier.padding(top = 50.dp, start = 20.dp)
                    IconPosition.TOP_RIGHT -> Modifier.padding(top = 110.dp, end = 20.dp)
                    IconPosition.BOTTOM_LEFT -> Modifier.padding(bottom = (if (isLandscape) 80.dp else 160.dp) + controlsOffset, start = 24.dp)
                    IconPosition.BOTTOM_RIGHT -> Modifier.padding(bottom = (if (isLandscape) 20.dp else 100.dp) + controlsOffset, end = 24.dp)
                }

                IconButton(
                    onClick = onDownload,
                    modifier = Modifier
                        .align(dlAlign)
                        .then(dlPadding)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "Download",
                        tint = Color.White
                    )
                }
            }

            // Share Button
            if (showShareButton) {
                val shareAlign = when (shareButtonPosition) {
                    IconPosition.TOP_LEFT -> Alignment.TopStart
                    IconPosition.TOP_RIGHT -> Alignment.TopEnd
                    IconPosition.BOTTOM_LEFT -> Alignment.BottomStart
                    IconPosition.BOTTOM_RIGHT -> Alignment.BottomEnd
                }

                val sharePadding = when (shareButtonPosition) {
                    IconPosition.TOP_LEFT -> Modifier.padding(top = 50.dp, start = 20.dp)
                    IconPosition.TOP_RIGHT -> Modifier.padding(top = 110.dp, end = 20.dp)
                    IconPosition.BOTTOM_LEFT -> Modifier.padding(bottom = (if (isLandscape) 80.dp else 160.dp) + controlsOffset, start = 24.dp)
                    IconPosition.BOTTOM_RIGHT -> Modifier.padding(bottom = (if (isLandscape) 20.dp else 100.dp) + controlsOffset, end = 24.dp)
                }

                IconButton(
                    onClick = onShare,
                    modifier = Modifier
                        .align(shareAlign)
                        .then(sharePadding)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
