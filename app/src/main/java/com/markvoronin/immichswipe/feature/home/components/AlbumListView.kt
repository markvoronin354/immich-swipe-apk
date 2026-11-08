package com.markvoronin.immichswipe.feature.home.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.RoundedPolygon
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Precision
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.domain.model.Album
import com.markvoronin.immichswipe.feature.home.AlbumStatus
import com.markvoronin.immichswipe.feature.settings.components.verticalFadingEdges
import com.markvoronin.immichswipe.ui.theme.VirtualGold

@Composable
fun AlbumList(
    groupedAlbums: Map<AlbumStatus, List<Album>>,
    treatedCounts: Map<String, Int>,
    unsyncedChanges: Map<String, Int>,
    collapsedCategories: Set<AlbumStatus>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onAlbumClick: (Album) -> Unit,
    onToggleCategory: (AlbumStatus) -> Unit,
    modifier: Modifier = Modifier,
    baseUrl: String = "",
) {
    val state = rememberLazyListState()
    val sectionExpansions = rememberSectionExpansions(collapsedCategories)
    val animatedRowLimit = rememberAnimatedRowLimit()

    Box(modifier = modifier.fillMaxSize()) {
        ExpressivePullToRefresh(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                state = state,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalFadingEdges(state, length = 32.dp),
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 85.dp)
            ) {
                val statusOrder = listOf(
                    AlbumStatus.IN_PROGRESS,
                    AlbumStatus.NOT_STARTED,
                    AlbumStatus.COMPLETED,
                    AlbumStatus.VIRTUAL
                )
                val firstStatus = statusOrder.firstOrNull { !groupedAlbums[it].isNullOrEmpty() }

                statusOrder.forEach { status ->
                    val albumsInStatus = groupedAlbums[status]
                    if (!albumsInStatus.isNullOrEmpty()) {
                        val isCollapsed = collapsedCategories.contains(status)

                        item(key = "header_${status.name}") {
                            val statusLabel = when(status) {
                                AlbumStatus.IN_PROGRESS -> stringResource(R.string.home_status_in_progress)
                                AlbumStatus.NOT_STARTED -> stringResource(R.string.home_status_not_started)
                                AlbumStatus.COMPLETED -> stringResource(R.string.home_status_completed)
                                AlbumStatus.VIRTUAL -> stringResource(R.string.home_status_virtual)
                            }
                            SectionHeader(
                                label = statusLabel,
                                count = albumsInStatus.size,
                                isCollapsed = isCollapsed,
                                attachedBelow = !isCollapsed,
                                labelColor = if (status == AlbumStatus.VIRTUAL)
                                    VirtualGold
                                else
                                    MaterialTheme.colorScheme.primary,
                                onClick = { onToggleCategory(status) },
                                modifier = Modifier
                                    .animateItem(placementSpec = null)
                                    .padding(top = if (status == firstStatus) 0.dp else 18.dp)
                            )
                        }

                        // Rows are emitted only while expanded or animating. All rows of a section
                        // shrink/grow together, so content below follows in lockstep.
                        val expansion = sectionExpansions.getValue(status)
                        if (expansion.rowsPresent) {
                            itemsIndexed(
                                items = if (expansion.settled) albumsInStatus else albumsInStatus.take(animatedRowLimit),
                                key = { _, album -> "album_${status.name}_${album.id}" }
                            ) { index, album ->
                                AlbumItem(
                                    album = album,
                                    treatedCount = treatedCounts[album.id] ?: 0,
                                    unsyncedCount = unsyncedChanges[album.id] ?: 0,
                                    baseUrl = baseUrl,
                                    onClick = { onAlbumClick(album) },
                                    shapes = albumRowShapes(index = index + 1, count = albumsInStatus.size + 1),
                                    modifier = Modifier
                                        .animateItem(placementSpec = null)
                                        .expandFraction { expansion.progress.value }
                                        .padding(top = ListItemDefaults.SegmentedGap)
                                )
                            }
                        }
                    }
                }
            }
        }

        val scrollFraction by remember {
            derivedStateOf {
                val layoutInfo = state.layoutInfo
                if (layoutInfo.totalItemsCount > 0 && layoutInfo.visibleItemsInfo.isNotEmpty()) {
                    val firstItem = layoutInfo.visibleItemsInfo.first()
                    val totalItems = layoutInfo.totalItemsCount
                    (firstItem.index + (-firstItem.offset.toFloat() / firstItem.size.coerceAtLeast(1).toFloat())) / totalItems.toFloat()
                } else 0f
            }
        }
        val visibleFraction by remember {
            derivedStateOf {
                val layoutInfo = state.layoutInfo
                if (layoutInfo.totalItemsCount > 0 && layoutInfo.visibleItemsInfo.isNotEmpty()) {
                    val viewportHeight = layoutInfo.viewportSize.height.toFloat()
                    val averageItemSize = layoutInfo.visibleItemsInfo.sumOf { it.size }.toFloat() / layoutInfo.visibleItemsInfo.size
                    (viewportHeight / averageItemSize) / layoutInfo.totalItemsCount
                } else 1.0f
            }
        }
        
        val animatedOffset by animateFloatAsState(targetValue = scrollFraction, animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(), label = "scrollbarOffset")
        val animatedHeight by animateFloatAsState(targetValue = visibleFraction.coerceIn(0.05f, 1.0f), animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(), label = "scrollbarHeight")

        if (visibleFraction < 1.0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp, top = 16.dp, bottom = 16.dp)
                    .fillMaxHeight()
                    .width(4.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), CircleShape)
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val height = maxHeight * animatedHeight
                    val offset = maxHeight * animatedOffset
                    Box(
                        modifier = Modifier
                            .offset(y = offset.coerceAtMost(maxHeight - height))
                            .fillMaxWidth()
                            .height(height)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AlbumItem(
    album: Album,
    treatedCount: Int,
    unsyncedCount: Int,
    onClick: () -> Unit,
    shapes: ListItemShapes,
    modifier: Modifier = Modifier,
    baseUrl: String = "",
) {
    val context = LocalContext.current
    val baseUrlClean = baseUrl.removeSuffix("/")
    val effectiveTreatedCount = if (album.assetCount > 0) treatedCount.coerceAtMost(album.assetCount) else treatedCount
    val progress = if (album.assetCount > 0) effectiveTreatedCount.toFloat() / album.assetCount else 0f
    val isCompleted = album.assetCount in 1..effectiveTreatedCount
    val isNotStarted = effectiveTreatedCount == 0
    val hasUnsyncedChanges = unsyncedCount > 0


    SegmentedListItem(
        onClick = onClick,
        shapes = shapes,
        modifier = modifier
            .fillMaxWidth(),
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        verticalAlignment = Alignment.CenterVertically,
        leadingContent = {
            Box(modifier = Modifier.size(60.dp)) {
                val isVirtualCollection = album.id == Album.VIRTUAL_ALL_ID ||
                        album.id == Album.VIRTUAL_ORPHANS_ID ||
                        album.id == Album.VIRTUAL_DUPLICATES_ID ||
                        album.albumName.equals("SKIP", ignoreCase = true)

                // Special collections each get their own expressive shape; real albums keep a rounded photo tile
                val tilePolygon = if (isVirtualCollection) virtualCollectionPolygon(album.id) else null
                val badgeAnchor = remember(tilePolygon) { tilePolygon?.let(::topEndBadgeAnchor) }

                // Centers a status badge on the shape's outline, or on the photo tile's corner
                fun BoxScope.badgePosition(badgeSize: Dp, cornerOffset: Dp): Modifier =
                    if (badgeAnchor != null) {
                        Modifier
                            .align(AbsoluteAlignment.TopLeft)
                            .absoluteOffset(x = 60.dp * badgeAnchor.x - badgeSize / 2, y = 60.dp * badgeAnchor.y - badgeSize / 2)
                    } else {
                        Modifier.align(Alignment.TopEnd).offset(x = cornerOffset, y = -cornerOffset)
                    }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = tilePolygon?.toShape() ?: RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    if (!isVirtualCollection && album.albumThumbnailAssetId != null && baseUrlClean.isNotEmpty()) {
                        val imageRequest = remember(album.albumThumbnailAssetId, baseUrlClean) {
                            ImageRequest.Builder(context)
                                .data("$baseUrlClean/api/assets/${album.albumThumbnailAssetId}/thumbnail?format=WEBP&size=thumbnail")
                                .crossfade(true)
                                .precision(Precision.INEXACT)
                                .memoryCachePolicy(CachePolicy.ENABLED)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .build()
                        }

                        AsyncImage(
                            model = imageRequest,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            placeholder = rememberVectorPainter(Icons.Default.PhotoLibrary),
                            error = rememberVectorPainter(Icons.Default.PhotoLibrary)
                        )
                    } else {
                        val (icon, brush, tint) = getVirtualCollectionStyle(album.id, album.albumName)
                        Box(
                            modifier = Modifier.fillMaxSize().background(brush),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = tint
                            )
                        }
                    }
                }
                
                if (isCompleted) {
                    Surface(
                        modifier = badgePosition(badgeSize = 16.dp, cornerOffset = 6.dp),
                        color = Color(0xFF388E3C),
                        shape = CircleShape,
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp).padding(2.dp))
                    }
                } else if (isNotStarted) {
                    Box(
                        modifier = badgePosition(badgeSize = 14.dp, cornerOffset = 4.dp)
                            .size(14.dp)
                            .background(MaterialTheme.colorScheme.tertiary, CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                    )
                }
            }
        },
        trailingContent = {
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant)
        },
        supportingContent = {
            Column {
                if (hasUnsyncedChanges) {
                    Text(
                        text = pluralStringResource(R.plurals.home_unsynced_changes, unsyncedCount, unsyncedCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
                val displayDescription = if (album.albumName.equals("SKIP", ignoreCase = true) && album.description.isNullOrBlank()) {
                    stringResource(R.string.home_virtual_skip_desc)
                } else {
                    album.description
                }
                if (!displayDescription.isNullOrBlank()) {
                    Text(text = displayDescription, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, maxLines = 3)
                }
                Text(
                    text = "$effectiveTreatedCount / ${album.assetCount}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isCompleted) Color(0xFF388E3C) else MaterialTheme.colorScheme.primary
                )
                if (progress > 0 && !isCompleted) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f),
                        drawStopIndicator = {}
                    )
                }
            }
        }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = album.albumName, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
            if (isCompleted) {
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.home_album_completed), style = MaterialTheme.typography.labelSmall, color = Color(0xFF388E3C), fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Corner radii matching the app's other grouped surfaces (Settings groups, popups, dialogs)
private val GroupOuterCorner = 28.dp
private val GroupInnerCorner = 6.dp
private val PressedInnerCorner = 16.dp

/**
 * Segment shapes for an album row at [index] of a group of [count] (header included): outer
 * group corners stay at [GroupOuterCorner], inner corners round up from [GroupInnerCorner] on press.
 */
@Composable
private fun albumRowShapes(index: Int, count: Int): ListItemShapes {
    val isFirst = index == 0
    val isLast = index == count - 1
    fun corners(inner: Dp): RoundedCornerShape {
        val top = if (isFirst) GroupOuterCorner else inner
        val bottom = if (isLast) GroupOuterCorner else inner
        return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
    }
    val shape = corners(GroupInnerCorner)
    val pressed = corners(PressedInnerCorner)
    return ListItemDefaults.shapes(
        shape = shape,
        selectedShape = shape,
        pressedShape = pressed,
        focusedShape = pressed,
        hoveredShape = pressed,
        draggedShape = pressed
    )
}

/**
 * Point on [polygon]'s outline that reaches furthest toward the top-end corner, as a fraction of
 * the tile size. A status badge centered here sits on the shape's edge whatever the shape.
 * Matches [toShape], which scales the normalized polygon to the tile and centers its bounds.
 */
private fun topEndBadgeAnchor(polygon: RoundedPolygon): Offset {
    var best = Offset(1f, 0f)
    var bestScore = Float.NEGATIVE_INFINITY
    for (c in polygon.cubics) {
        for (i in 0..16) {
            val t = i / 16f
            val u = 1f - t
            val x = u * u * u * c.anchor0X + 3 * u * u * t * c.control0X + 3 * u * t * t * c.control1X + t * t * t * c.anchor1X
            val y = u * u * u * c.anchor0Y + 3 * u * u * t * c.control0Y + 3 * u * t * t * c.control1Y + t * t * t * c.anchor1Y
            if (x - y > bestScore) {
                bestScore = x - y
                best = Offset(x, y)
            }
        }
    }
    val bounds = polygon.calculateBounds()
    return best + Offset(0.5f - (bounds[0] + bounds[2]) / 2, 0.5f - (bounds[1] + bounds[3]) / 2)
}

/**
 * Collapsible section header styled as the top segment of its album group.
 * Bottom corners morph to fully rounded when collapsed or pressed, so the
 * press ripple always has a shape to live in.
 */
@Composable
fun SectionHeader(
    label: String,
    count: Int,
    isCollapsed: Boolean,
    attachedBelow: Boolean,
    labelColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val motionScheme = MaterialTheme.motionScheme
    val bottomCorner by animateDpAsState(
        targetValue = if (attachedBelow && !isPressed) GroupInnerCorner else GroupOuterCorner,
        animationSpec = motionScheme.fastSpatialSpec(),
        label = "headerBottomCorner"
    )
    val shape = RoundedCornerShape(topStart = GroupOuterCorner, topEnd = GroupOuterCorner, bottomStart = bottomCorner, bottomEnd = bottomCorner)
    val rotation by animateFloatAsState(
        targetValue = if (isCollapsed) -90f else 0f,
        animationSpec = motionScheme.defaultSpatialSpec(),
        label = "chevronRotation"
    )

    SegmentedListItem(
        onClick = onClick,
        shapes = ListItemDefaults.shapes(
            shape = shape,
            selectedShape = shape,
            pressedShape = shape,
            focusedShape = shape,
            hoveredShape = shape,
            draggedShape = shape
        ),
        modifier = modifier.fillMaxWidth(),
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        verticalAlignment = Alignment.CenterVertically,
        interactionSource = interactionSource,
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.graphicsLayer { rotationZ = rotation }
                )
            }
        }
    ) {
        // Leading dot matches the settings/stats section headers.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(
                Modifier
                    .size(8.dp)
                    .background(labelColor, CircleShape)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = labelColor
            )
        }
    }
}
