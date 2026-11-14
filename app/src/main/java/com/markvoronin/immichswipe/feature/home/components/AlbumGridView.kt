package com.markvoronin.immichswipe.feature.home.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Precision
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.domain.model.Album
import com.markvoronin.immichswipe.feature.home.AlbumStatus
import com.markvoronin.immichswipe.feature.settings.components.verticalFadingEdges
import com.markvoronin.immichswipe.ui.theme.VirtualGold
import kotlinx.coroutines.launch

@Composable
fun AlbumGrid(
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
    val state = rememberLazyGridState()
    val sectionExpansions = rememberSectionExpansions(collapsedCategories)
    val animatedRowLimit = rememberAnimatedRowLimit(itemsPerRow = 3)

    Box(modifier = modifier.fillMaxSize()) {
        ExpressivePullToRefresh(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            LazyVerticalGrid(
                state = state,
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .verticalFadingEdges(state, length = 32.dp),
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 85.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val statusOrder = listOf(
                    AlbumStatus.VIRTUAL,
                    AlbumStatus.IN_PROGRESS,
                    AlbumStatus.NOT_STARTED,
                    AlbumStatus.COMPLETED
                )
                val firstStatus = statusOrder.firstOrNull { !groupedAlbums[it].isNullOrEmpty() }

                statusOrder.forEach { status ->
                    val albumsInStatus = groupedAlbums[status]
                    if (!albumsInStatus.isNullOrEmpty()) {
                        val isCollapsed = collapsedCategories.contains(status)

                        item(
                            span = { GridItemSpan(maxLineSpan) },
                            key = "header_${status.name}"
                        ) {
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
                                attachedBelow = false,
                                labelColor = if (status == AlbumStatus.VIRTUAL)
                                    VirtualGold
                                else
                                    MaterialTheme.colorScheme.primary,
                                onClick = { onToggleCategory(status) },
                                modifier = Modifier
                                    .animateItem(placementSpec = null)
                                    .padding(top = if (status == firstStatus) 0.dp else 16.dp)
                            )
                        }

                        // Cells are emitted only while expanded or animating. All cells of a section
                        // shrink/grow together, so content below follows in lockstep.
                        val expansion = sectionExpansions.getValue(status)
                        if (expansion.rowsPresent) {
                            itemsIndexed(
                                items = if (expansion.settled) albumsInStatus else albumsInStatus.take(animatedRowLimit),
                                key = { _, album -> "album_grid_${status.name}_${album.id}" }
                            ) { _, album ->
                                AlbumGridItem(
                                    album = album,
                                    treatedCount = treatedCounts[album.id] ?: 0,
                                    unsyncedCount = unsyncedChanges[album.id] ?: 0,
                                    baseUrl = baseUrl,
                                    onClick = { onAlbumClick(album) },
                                    modifier = Modifier
                                        .animateItem(placementSpec = null)
                                        .expandFraction { expansion.progress.value }
                                        .padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        val showScrollToTop by remember { derivedStateOf { state.firstVisibleItemIndex >= 15 } }
        val scope = rememberCoroutineScope()
        ScrollToTopButton(
            visible = showScrollToTop,
            onClick = { scope.launch { state.animateScrollToItem(0) } },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        val scrollFraction by remember {
            derivedStateOf {
                val layoutInfo = state.layoutInfo
                if (layoutInfo.totalItemsCount > 0 && layoutInfo.visibleItemsInfo.isNotEmpty()) {
                    val firstItem = layoutInfo.visibleItemsInfo.first()
                    val totalItems = layoutInfo.totalItemsCount
                    (firstItem.index / 3f + (-firstItem.offset.y.toFloat() / firstItem.size.height.coerceAtLeast(1).toFloat())) / (totalItems / 3f)
                } else 0f
            }
        }
        val visibleFraction by remember {
            derivedStateOf {
                val layoutInfo = state.layoutInfo
                if (layoutInfo.totalItemsCount > 0 && layoutInfo.visibleItemsInfo.isNotEmpty()) {
                    val viewportHeight = layoutInfo.viewportSize.height.toFloat()
                    val averageItemHeight = layoutInfo.visibleItemsInfo.sumOf { it.size.height }.toFloat() / layoutInfo.visibleItemsInfo.size
                    (viewportHeight / averageItemHeight) / (layoutInfo.totalItemsCount / 3f)
                } else 1.0f
            }
        }

        val animatedOffset by animateFloatAsState(targetValue = scrollFraction.coerceIn(0f, 1f), animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(), label = "scrollbarOffset")
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
fun AlbumGridItem(
    album: Album,
    treatedCount: Int,
    unsyncedCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    baseUrl: String = "",
) {
    val context = LocalContext.current
    val baseUrlClean = baseUrl.removeSuffix("/")
    val effectiveTreatedCount = if (album.assetCount > 0) treatedCount.coerceAtMost(album.assetCount) else treatedCount
    val progress = if (album.assetCount > 0) effectiveTreatedCount.toFloat() / album.assetCount else 0f
    val isCompleted = album.assetCount in 1..effectiveTreatedCount
    val hasUnsyncedChanges = unsyncedCount > 0

    val motionScheme = MaterialTheme.motionScheme

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val corner by animateDpAsState(
        targetValue = if (isPressed) 24.dp else 16.dp,
        animationSpec = motionScheme.fastSpatialSpec(),
        label = "cardCorner"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.8f),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = RoundedCornerShape(corner),
        interactionSource = interactionSource
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val isVirtualCollection = album.id == Album.VIRTUAL_ALL_ID ||
                    album.id == Album.VIRTUAL_ORPHANS_ID ||
                    album.id == Album.VIRTUAL_DUPLICATES_ID ||
                    album.albumName.equals("SKIP", ignoreCase = true)

            if (!isVirtualCollection && album.albumThumbnailAssetId != null && baseUrlClean.isNotEmpty()) {
                val imageRequest = remember(album.albumThumbnailAssetId, baseUrlClean) {
                    ImageRequest.Builder(context)
                        .data("$baseUrlClean/api/assets/${album.albumThumbnailAssetId}/thumbnail?format=WEBP")
                        .crossfade(true)
                        .precision(Precision.INEXACT)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build()
                }

                SubcomposeAsyncImage(
                    model = imageRequest,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (isCompleted) Modifier.alpha(0.8f) else Modifier),
                    error = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.BrokenImage,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                )
            } else {
                val (icon, brush, tint) = getVirtualCollectionStyle(album.id, album.albumName)
                // Icon sits top-left so it never collides with the name at the bottom.
                Box(
                    modifier = Modifier.fillMaxSize().background(brush),
                    contentAlignment = Alignment.TopStart
                ) {
                    // Same expressive shape as in list view, sized like the Settings item icons
                    Box(
                        modifier = Modifier
                            .padding(10.dp)
                            .size(40.dp)
                            .background(Color.Black.copy(alpha = 0.12f), virtualCollectionPolygon(album.id).toShape()),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Special collections use light pastel gradients: skip the dark scrim and use dark text.
            if (!isVirtualCollection) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .fillMaxHeight(0.6f)
                        .background(
                            // Covers only the text area so the photo stays bright; tall enough for a two-line name.
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                0.3f to Color.Black.copy(alpha = 0.5f),
                                1f to Color.Black.copy(alpha = 0.85f)
                            )
                        )
                )
            }
            val labelColor = if (isVirtualCollection) Color.Black.copy(alpha = 0.87f) else Color.White

            if (isCompleted) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                    color = Color(0xFF388E3C),
                    shape = CircleShape,
                    shadowElevation = 4.dp
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.padding(4.dp).size(16.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                if (hasUnsyncedChanges) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        shape = CircleShape,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.home_unsynced_badge),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = album.albumName,
                    color = labelColor,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.sharedAlbumName(album.id)
                )
                Text(
                    text = "$effectiveTreatedCount / ${album.assetCount}",
                    color = labelColor.copy(alpha = if (isVirtualCollection) 0.7f else 0.8f),
                    style = MaterialTheme.typography.labelMedium
                )
            }

            if (progress > 0 && !isCompleted) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .align(Alignment.BottomCenter),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent,
                    gapSize = 0.dp,
                    drawStopIndicator = {}
                )
            }
        }
    }
}
