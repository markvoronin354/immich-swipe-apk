package com.markvoronin.immichswipe.feature.duplicates.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.markvoronin.immichswipe.R
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Composable
fun InstagramZoomOverlay(
    zoomData: ZoomData?,
    rootWindowOffset: Offset,
    baseUrl: String = "",
    isFavorite: Boolean = false,
    onDismiss: () -> Unit
) {
    if (zoomData == null) return

    val context = LocalContext.current
    val density = LocalDensity.current
    val baseUrlClean = baseUrl.removeSuffix("/")

    val scaleAnim = remember { Animatable(zoomData.scale) }
    val offsetXAnim = remember { Animatable(zoomData.offset.x) }
    val offsetYAnim = remember { Animatable(zoomData.offset.y) }
    // The photo springs back into its thumbnail with the theme's spatial spring.
    val settleSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()

    LaunchedEffect(zoomData.asset.id, zoomData.scale, zoomData.offset, zoomData.isGestureActive) {
        if (zoomData.isGestureActive) {
            scaleAnim.snapTo(zoomData.scale)
            offsetXAnim.snapTo(zoomData.offset.x)
            offsetYAnim.snapTo(zoomData.offset.y)
        } else {
            coroutineScope {
                launch { scaleAnim.animateTo(targetValue = 1f, animationSpec = settleSpec) }
                launch { offsetXAnim.animateTo(targetValue = 0f, animationSpec = settleSpec) }
                launch { offsetYAnim.animateTo(targetValue = 0f, animationSpec = settleSpec) }
            }
            onDismiss()
        }
    }

    val currentScale = scaleAnim.value.coerceAtLeast(1f)
    val inverseScale = 1f / currentScale
    val badgeAlpha = (1f - (currentScale - 1f) * 1.5f).coerceIn(0f, 1f)
    val backdropAlpha = ((currentScale - 1f) / 1.5f).coerceIn(0f, 0.7f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(1000f)
            .background(Color.Black.copy(alpha = backdropAlpha))
    ) {
        val bounds = zoomData.initialBounds
        val leftPx = bounds.left - rootWindowOffset.x
        val topPx = bounds.top - rootWindowOffset.y

        val widthDp = with(density) { bounds.width.toDp() }
        val heightDp = with(density) { bounds.height.toDp() }
        val leftDp = with(density) { leftPx.toDp() }
        val topDp = with(density) { topPx.toDp() }

        Box(
            modifier = Modifier
                .offset(x = leftDp, y = topDp)
                .size(width = widthDp, height = heightDp)
                .graphicsLayer {
                    scaleX = currentScale
                    scaleY = currentScale
                    translationX = offsetXAnim.value
                    translationY = offsetYAnim.value
                }
                .clip(RoundedCornerShape(16.dp))
        ) {
            val imageRequest = remember(zoomData.asset.id, baseUrlClean, zoomData.asset.isGif) {
                ImageRequest.Builder(context)
                    .data(
                        if (zoomData.asset.isGif) "$baseUrlClean/api/assets/${zoomData.asset.id}/original"
                        else "$baseUrlClean/api/assets/${zoomData.asset.id}/thumbnail?format=WEBP&size=preview"
                    )
                    .crossfade(false)
                    .build()
            }

            AsyncImage(
                model = imageRequest,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            if (isFavorite) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = stringResource(R.string.cd_favorite),
                    tint = Color.Red,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .graphicsLayer {
                            scaleX = inverseScale
                            scaleY = inverseScale
                            transformOrigin = TransformOrigin(0f, 0f)
                            alpha = badgeAlpha
                        }
                        .size(20.dp)
                )
            }
        }
    }
}
