package com.markvoronin.immichswipe.feature.duplicates.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.domain.model.Asset
import com.markvoronin.immichswipe.feature.duplicates.DuplicateDecision

@Composable
fun DuplicateAssetItem(
    asset: Asset,
    decision: DuplicateDecision,
    isBeingZoomed: Boolean,
    onToggle: () -> Unit,
    onLongPress: () -> Unit,
    // Reports this thumbnail's on-screen bounds (null once it leaves composition); the screen runs the pinch-to-peek gesture.
    onBoundsChanged: (Rect?) -> Unit,
    modifier: Modifier = Modifier,
    baseUrl: String = "",
    isFavorite: Boolean = false,
) {
    val context = LocalContext.current
    val baseUrlClean = baseUrl.removeSuffix("/")

    val currentOnToggle by rememberUpdatedState(onToggle)
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    val currentOnBoundsChanged by rememberUpdatedState(onBoundsChanged)

    val borderColor = when (decision) {
        DuplicateDecision.DELETE -> MaterialTheme.colorScheme.error
        DuplicateDecision.KEEP -> Color(0xFF4CAF50)
        DuplicateDecision.NONE -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    }

    DisposableEffect(asset.id) {
        onDispose { currentOnBoundsChanged(null) }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(0.75f)
                .onGloballyPositioned { coords -> currentOnBoundsChanged(coords.boundsInWindow()) }
                .graphicsLayer {
                    alpha = if (isBeingZoomed) 0f else 1f
                }
                .clip(RoundedCornerShape(14.dp))
                .border(2.dp, borderColor, RoundedCornerShape(14.dp))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { currentOnToggle() },
                        onLongPress = { currentOnLongPress() }
                    )
                }
        ) {
            val imageRequest = remember(asset.id, baseUrlClean, asset.isGif) {
                ImageRequest.Builder(context)
                    .data(
                        if (asset.isGif) "$baseUrlClean/api/assets/${asset.id}/original"
                        else "$baseUrlClean/api/assets/${asset.id}/thumbnail?format=WEBP&size=preview"
                    )
                    .crossfade(true)
                    .build()
            }

            SubcomposeAsyncImage(
                model = imageRequest,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                error = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.BrokenImage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            )

            if (isFavorite) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = stringResource(R.string.cd_favorite),
                    tint = Color.Red,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .size(20.dp)
                )
            }

            if (asset.type == "VIDEO") {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape)
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.cd_video),
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else if (asset.isGif) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "GIF",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (decision != DuplicateDecision.NONE) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = if (decision == DuplicateDecision.DELETE) Icons.Default.Delete else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (decision == DuplicateDecision.DELETE) MaterialTheme.colorScheme.error else Color(0xFF4CAF50),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(32.dp)
                            .background(Color.White, shape = CircleShape)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val sizeStr = asset.exifInfo?.fileSizeInBytes?.let { formatSizeStr(it) } ?: stringResource(R.string.duplicates_unknown_size)
        Text(text = sizeStr, style = MaterialTheme.typography.bodySmall)

        val decisionText = when (decision) {
            DuplicateDecision.DELETE -> stringResource(R.string.swipe_delete_upper)
            DuplicateDecision.KEEP -> stringResource(R.string.swipe_keep_upper)
            DuplicateDecision.NONE -> "-"
        }

        Text(
            text = decisionText,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (decision == DuplicateDecision.NONE) MaterialTheme.colorScheme.outline else borderColor,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
