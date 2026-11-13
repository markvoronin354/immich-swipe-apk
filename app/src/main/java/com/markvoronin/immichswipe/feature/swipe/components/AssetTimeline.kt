package com.markvoronin.immichswipe.feature.swipe.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Precision
import com.markvoronin.immichswipe.domain.model.Asset
import com.markvoronin.immichswipe.feature.swipe.SwipeDecision
import com.markvoronin.immichswipe.feature.swipe.utils.MaterialGreen
import com.markvoronin.immichswipe.feature.swipe.utils.MaterialOrange
import com.markvoronin.immichswipe.feature.swipe.utils.MaterialRed
import com.markvoronin.immichswipe.feature.swipe.utils.rotateLayout
import kotlin.math.abs

private const val UNFOCUSED_ITEM_SCALE = 0.8f
private val FADING_EDGE_WIDTH = 76.dp

@Composable
fun AssetTimeline(
    assets: List<Asset>,
    decisions: Map<String, SwipeDecision>,
    currentIndex: Int,
    isFavorite: (String) -> Boolean,
    isArchived: (String) -> Boolean,
    isLocked: (String) -> Boolean,
    onAssetClick: (Int) -> Unit,
    baseUrl: String = "",
    isBulkMode: Boolean = false,
    bulkSelection: Set<String> = emptySet(),
    isBulkDelete: Boolean = false,
    getRotation: (String) -> Int = { 0 },
    swipeOffset: Float = 0f,
    nextIndex: Int = -1
) {
    val listState = rememberLazyListState()
    val baseUrlClean = baseUrl.removeSuffix("/")
    val density = LocalDensity.current
    val itemSizePx = with(density) { 64.dp.toPx() }

    var previousAssets by remember { mutableStateOf<List<Asset>?>(null) }

    val swipeProgress = (abs(swipeOffset) / 250f).coerceIn(0f, 1f)
    val hasValidNext = nextIndex in assets.indices && nextIndex != currentIndex
    val isActivelySwiping = swipeProgress > 0f && hasValidNext && !isBulkMode

    val targetIndex = when {
        isBulkMode && bulkSelection.isNotEmpty() -> {
            assets.indices.lastOrNull { i -> bulkSelection.contains(assets[i].id) } ?: currentIndex
        }
        else -> currentIndex
    }

    LaunchedEffect(targetIndex, nextIndex, swipeOffset, isBulkMode, bulkSelection, assets) {
        if (assets.isEmpty()) return@LaunchedEffect

        val assetsChanged = assets !== previousAssets
        previousAssets = assets

        if (isActivelySwiping) {
            val indexDelta = nextIndex - currentIndex
            val basePx = currentIndex * itemSizePx + (indexDelta * swipeProgress * itemSizePx)
            val scrollItemIndex = (basePx / itemSizePx).toInt().coerceIn(0, assets.lastIndex)
            val scrollOffsetPx = (basePx - (scrollItemIndex * itemSizePx)).toInt()

            listState.scrollToItem(scrollItemIndex, scrollOffsetPx)
        } else if (targetIndex in assets.indices) {
            val currentVisible = listState.firstVisibleItemIndex
            val distance = abs(targetIndex - currentVisible)
            if (assetsChanged || distance > 3) {
                listState.scrollToItem(targetIndex, scrollOffset = 0)
            } else {
                listState.animateScrollToItem(targetIndex, scrollOffset = 0)
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        // Pad both ends so that an item scrolled to offset 0 sits in the middle of the row.
        val sidePadding = ((maxWidth - 60.dp) / 2).coerceAtLeast(16.dp)

        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .height(75.dp)
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    val fade = (FADING_EDGE_WIDTH.toPx() / size.width).coerceAtMost(0.5f)
                    drawRect(
                        brush = Brush.horizontalGradient(
                            0f to Color.Transparent,
                            fade to Color.Black,
                            1f - fade to Color.Black,
                            1f to Color.Transparent
                        ),
                        blendMode = BlendMode.DstIn
                    )
                },
            // Top room inside the faded layer (which clips) for the badges that hang over the thumbnails' top edge.
            contentPadding = PaddingValues(start = sidePadding, top = 7.dp, end = sidePadding),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(assets, key = { _, asset -> asset.id }) { index, asset ->
                AssetTimelineItem(
                    asset = asset,
                    index = index,
                    currentIndex = currentIndex,
                    focusIndex = targetIndex,
                    nextIndex = nextIndex,
                    swipeProgress = if (isActivelySwiping) swipeProgress else 0f,
                    isSelected = bulkSelection.contains(asset.id),
                    decision = decisions[asset.id],
                    hasHeart = isFavorite(asset.id),
                    hasArchive = isArchived(asset.id),
                    hasLock = isLocked(asset.id),
                    isBulkDelete = isBulkDelete,
                    rotation = getRotation(asset.id),
                    baseUrl = baseUrlClean,
                    onAssetClick = onAssetClick
                )
            }
        }
    }
}

@Composable
private fun AssetTimelineItem(
    asset: Asset,
    index: Int,
    currentIndex: Int,
    focusIndex: Int,
    nextIndex: Int,
    swipeProgress: Float,
    isSelected: Boolean,
    decision: SwipeDecision?,
    hasHeart: Boolean,
    hasArchive: Boolean,
    hasLock: Boolean,
    isBulkDelete: Boolean,
    rotation: Int,
    baseUrl: String,
    onAssetClick: (Int) -> Unit
) {
    val context = LocalContext.current
    val animatedRotation by animateFloatAsState(
        targetValue = rotation.toFloat(),
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "TimelineRotation"
    )

    val isCurrent = index == currentIndex
    val isNext = index == nextIndex

    // The item in the middle of the row is full size; the rest shrink, easing over as a swipe hands focus to the next one.
    val targetFocus = when {
        index == focusIndex -> 1f - swipeProgress
        isNext -> swipeProgress
        else -> 0f
    }
    // Springy follow, so focus changes that aren't driven by a swipe (taps, undo) bounce into place instead of snapping.
    val focusProgress by animateFloatAsState(
        targetValue = targetFocus,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "TimelineFocus"
    )
    val itemScale = UNFOCUSED_ITEM_SCALE + ((1f - UNFOCUSED_ITEM_SCALE) * focusProgress)

    val borderProgress = when {
        isCurrent -> 1f - swipeProgress
        isNext -> swipeProgress
        else -> 0f
    }

    val itemAlpha = when {
        isCurrent -> 1f - (swipeProgress * 0.4f)
        isNext -> 0.6f + (swipeProgress * 0.4f)
        else -> 0.6f
    }

    val borderColor = when {
        isSelected -> if (isBulkDelete) MaterialRed else MaterialGreen
        borderProgress > 0.05f -> MaterialTheme.colorScheme.primary.copy(alpha = borderProgress)
        else -> Color.Transparent
    }

    val borderWidth = when {
        isSelected -> 3.dp
        borderProgress > 0.05f -> (1.dp + (1.dp * borderProgress))
        else -> 0.dp
    }

    Box(
        modifier = Modifier
            .size(60.dp)
            .graphicsLayer {
                scaleX = itemScale
                scaleY = itemScale
            }
    ) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = borderWidth,
                color = borderColor,
                shape = MaterialTheme.shapes.medium
            )
            .clickable { onAssetClick(index) }
    ) {
        if (baseUrl.isNotEmpty()) {
            val thumbnailRequest = remember(asset.id, baseUrl, rotation) {
                ImageRequest.Builder(context)
                    .data("$baseUrl/api/assets/${asset.id}/thumbnail?format=WEBP&size=thumbnail&edited=true")
                    .crossfade(true)
                    .precision(Precision.INEXACT)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build()
            }
            SubcomposeAsyncImage(
                model = thumbnailRequest,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationZ = animatedRotation }
                    .rotateLayout(rotation)
                    .alpha(itemAlpha),
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
        }

    }

        if (hasHeart) {
            InsetBadge(Modifier.align(Alignment.TopStart)) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color.Red,
                    modifier = Modifier.size(10.dp)
                )
            }
        }

        decision?.let { d ->
            EdgeBadge(Modifier.align(Alignment.TopEnd).onCorner(x = 1, y = -1)) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            when (d) {
                                SwipeDecision.KEEP -> MaterialGreen
                                SwipeDecision.DELETE -> MaterialRed
                                SwipeDecision.ARCHIVE -> MaterialTheme.colorScheme.primary
                                SwipeDecision.LOCK -> MaterialTheme.colorScheme.outline
                                SwipeDecision.SKIP -> MaterialOrange
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (d) {
                            SwipeDecision.KEEP -> Icons.Default.Check
                            SwipeDecision.DELETE -> Icons.Default.Delete
                            SwipeDecision.ARCHIVE -> Icons.Default.Archive
                            SwipeDecision.LOCK -> Icons.Default.Lock
                            SwipeDecision.SKIP -> Icons.Default.ArrowDownward
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }

        if (asset.type == "VIDEO") {
            InsetBadge(Modifier.align(Alignment.BottomStart)) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(12.dp)
                )
            }
        } else if (asset.isGif) {
            InsetBadge(Modifier.align(Alignment.BottomStart)) {
                Text(
                    text = "GIF",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 9.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }

        // Already archived or locked on the server, without a matching decision this session.
        val showArchive = hasArchive && decision != SwipeDecision.ARCHIVE
        val showLock = hasLock && decision != SwipeDecision.LOCK
        if (showArchive || showLock) {
            EdgeBadge(Modifier.align(Alignment.BottomEnd).onCorner(x = 1, y = 1)) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (showArchive) GreyBadge { GreyBadgeIcon(Icons.Default.Archive) }
                    if (showLock) GreyBadge { GreyBadgeIcon(Icons.Default.Lock) }
                }
            }
        }
    }
}

// Where a 12dp corner's curve crosses its diagonal: 12dp × (1 − 1/√2).
private val CornerEdgeInset = 3.5.dp

/** Centres a badge on the curve of the thumbnail corner it's aligned to; [x] and [y] are -1 for start/top, 1 for end/bottom. */
private fun Modifier.onCorner(x: Int, y: Int) = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val inset = CornerEdgeInset.roundToPx()
    layout(placeable.width, placeable.height) {
        placeable.placeRelative(x * (placeable.width / 2 - inset), y * (placeable.height / 2 - inset))
    }
}

/** A 16dp badge tucked 4dp into a thumbnail corner, so it shares the 12dp corner's centre. */
@Composable
private fun InsetBadge(modifier: Modifier, content: @Composable () -> Unit) {
    GreyBadge(modifier.padding(4.dp), content)
}

/** The timeline's 16dp grey badge: a disc, or a pill when its content is wider. */
@Composable
private fun GreyBadge(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .height(16.dp)
            .widthIn(min = 16.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun GreyBadgeIcon(icon: ImageVector) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.size(10.dp)
    )
}

/** Rings a badge in the background colour, cutting it out of the thumbnail like the avatar's status dot. */
@Composable
private fun EdgeBadge(modifier: Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier.background(MaterialTheme.colorScheme.surface, CircleShape).padding(2.dp)) {
        content()
    }
}

@Composable
fun TimelineMiniBadge(icon: ImageVector, color: Color) {
    Box(
        modifier = Modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(10.dp)
        )
    }
}
