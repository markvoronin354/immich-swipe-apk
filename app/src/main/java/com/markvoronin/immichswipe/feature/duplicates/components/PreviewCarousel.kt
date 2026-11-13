package com.markvoronin.immichswipe.feature.duplicates.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.domain.model.Asset
import com.markvoronin.immichswipe.feature.duplicates.DuplicateDecision
import com.markvoronin.immichswipe.feature.settings.components.horizontalFadingEdges
import kotlin.math.abs

private val ThumbShape = RoundedCornerShape(12.dp)
// A 16dp badge inset 4dp shares its centre with the 12dp corner arc, so it never touches the corner or the 3dp frame.
private val ThumbBadgeSize = 16.dp
private val ThumbBadgeInset = 4.dp

/**
 * The cluster's thumbnails in a strip that keeps the selected one centred. A new [clusterId] gets a fresh strip, opened at
 * [initialIndex]; dragging the strip selects whichever thumbnail it settles on.
 */
@Composable
internal fun PreviewCarousel(
    assets: List<Asset>,
    clusterId: String,
    initialIndex: Int,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    decisions: Map<String, DuplicateDecision>,
    isFavorite: (Asset) -> Boolean,
    baseUrl: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val motionScheme = MaterialTheme.motionScheme
    // Read by the long-running effects below, which would otherwise keep the values they started with.
    val currentSelectedIndex by rememberUpdatedState(selectedIndex)
    val currentOnSelect by rememberUpdatedState(onSelect)

    val carouselState = remember(clusterId) {
        LazyListState(firstVisibleItemIndex = initialIndex)
    }
    val isUserDragging by carouselState.interactionSource.collectIsDraggedAsState()

    var carouselWidthPx by remember { mutableIntStateOf(0) }
    val itemWidthDp = 54.dp
    val itemWidthPx = with(density) { itemWidthDp.roundToPx() }

    val sidePaddingDp = if (carouselWidthPx > 0) {
        val paddingPx = ((carouselWidthPx - itemWidthPx) / 2).coerceAtLeast(0)
        with(density) { paddingPx.toDp() }
    } else {
        16.dp
    }

    LaunchedEffect(carouselState, isUserDragging, carouselWidthPx) {
        if (!isUserDragging || carouselWidthPx <= 0) return@LaunchedEffect

        snapshotFlow {
            @Suppress("UNUSED_EXPRESSION")
            carouselState.firstVisibleItemIndex
            @Suppress("UNUSED_EXPRESSION")
            carouselState.firstVisibleItemScrollOffset

            val layoutInfo = carouselState.layoutInfo
            val contentStart = layoutInfo.beforeContentPadding
            val viewportCenter = layoutInfo.viewportSize.width / 2

            if (layoutInfo.viewportSize.width > 0) {
                layoutInfo.visibleItemsInfo.minByOrNull { item ->
                    val itemCenter = contentStart + item.offset + item.size / 2
                    abs(itemCenter - viewportCenter)
                }?.index
            } else null
        }.collect { closestIndex ->
            if (closestIndex != null && closestIndex in assets.indices && closestIndex != currentSelectedIndex) {
                currentOnSelect(closestIndex)
            }
        }
    }

    val glideSpec = motionScheme.defaultSpatialSpec<Float>()
    // Slides the strip until [index] sits in the centre (offset 0 after the side padding). Jumps instead when that
    // thumbnail isn't laid out yet, as when the strip first appears or a new cluster opens.
    suspend fun LazyListState.glideTo(index: Int) {
        val target = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
        if (target != null) animateScrollBy(target.offset.toFloat(), glideSpec) else scrollToItem(index, 0)
    }

    LaunchedEffect(carouselState, carouselWidthPx) {
        if (carouselWidthPx > 0 && !isUserDragging && currentSelectedIndex in assets.indices) {
            carouselState.scrollToItem(currentSelectedIndex, 0)
        }
    }

    // A swipe or a tap moves the selection; the strip follows. Another swipe mid-glide restarts this and retargets
    // from where the strip is. Stays out of the way while a finger is on the strip.
    LaunchedEffect(carouselState, selectedIndex) {
        if (carouselWidthPx > 0 && !isUserDragging && selectedIndex in assets.indices) {
            carouselState.glideTo(selectedIndex)
        }
    }

    var wasCarouselDragged by remember { mutableStateOf(false) }
    LaunchedEffect(isUserDragging) {
        if (isUserDragging) {
            wasCarouselDragged = true
        } else if (wasCarouselDragged) {
            wasCarouselDragged = false
            if (carouselWidthPx > 0) {
                val layoutInfo = carouselState.layoutInfo
                val contentStart = layoutInfo.beforeContentPadding
                val viewportCenter = layoutInfo.viewportSize.width / 2
                if (layoutInfo.viewportSize.width > 0) {
                    val closest = layoutInfo.visibleItemsInfo.minByOrNull { item ->
                        val itemCenter = contentStart + item.offset + item.size / 2
                        abs(itemCenter - viewportCenter)
                    }?.index
                    if (closest != null && closest in assets.indices) {
                        currentOnSelect(closest)
                        carouselState.glideTo(closest)
                    }
                }
            }
        }
    }

    LazyRow(
        state = carouselState,
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .horizontalFadingEdges(carouselState, length = 24.dp)
            .onSizeChanged { size ->
                carouselWidthPx = size.width
            },
        contentPadding = PaddingValues(horizontal = sidePaddingDp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        itemsIndexed(assets, key = { _, asset -> asset.id }) { index, asset ->
            val isSelected = index == selectedIndex
            val assetDecision = decisions[asset.id] ?: DuplicateDecision.NONE
            val assetFav = isFavorite(asset)
            // The current photo sits centered at full size; the others shrink and dim.
            val selectedProgress by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0f,
                animationSpec = motionScheme.defaultSpatialSpec(),
                label = "thumbSelected"
            )

            Box(
                modifier = Modifier
                    .size(width = itemWidthDp, height = 68.dp)
                    .graphicsLayer {
                        val scale = 0.86f + 0.14f * selectedProgress
                        scaleX = scale
                        scaleY = scale
                        shape = ThumbShape
                        clip = true
                    }
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .then(
                        if (assetDecision != DuplicateDecision.NONE) {
                            Modifier.border(3.dp, decisionColor(assetDecision), ThumbShape)
                        } else Modifier
                    )
                    .clickable {
                        onSelect(index)
                    }
            ) {
                val thumbRequest = remember(asset.id, baseUrl, asset.isGif) {
                    ImageRequest.Builder(context)
                        .data(
                            if (asset.isGif) "$baseUrl/api/assets/${asset.id}/original"
                            else "$baseUrl/api/assets/${asset.id}/thumbnail?format=WEBP&size=preview"
                        )
                        .crossfade(false)
                        .build()
                }

                AsyncImage(
                    model = thumbRequest,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = 0.6f + 0.4f * selectedProgress
                        }
                )

                // Favourite and video share one pill.
                if (assetFav || asset.type == "VIDEO") {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(ThumbBadgeInset)
                            .height(ThumbBadgeSize)
                            .widthIn(min = ThumbBadgeSize)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .padding(horizontal = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (assetFav) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Color.Red,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                        if (asset.type == "VIDEO") {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = stringResource(R.string.cd_video),
                                tint = Color.White,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
