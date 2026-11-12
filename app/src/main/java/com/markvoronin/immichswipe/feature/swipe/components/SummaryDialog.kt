package com.markvoronin.immichswipe.feature.swipe.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Precision
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.core.PlaybackBehavior
import com.markvoronin.immichswipe.domain.model.Asset
import com.markvoronin.immichswipe.feature.common.SolidShapeLoadingIndicator
import com.markvoronin.immichswipe.feature.common.WavyLoadingBar
import com.markvoronin.immichswipe.feature.common.WavyLoadingIndicator
import com.markvoronin.immichswipe.feature.duplicates.components.DuplicateVideoPlayer
import com.markvoronin.immichswipe.feature.settings.components.verticalFadingEdges
import com.markvoronin.immichswipe.feature.swipe.SwipeUiState
import com.markvoronin.immichswipe.feature.swipe.utils.formatSize
import kotlinx.coroutines.launch


@Composable
fun SummaryDialog(
    uiState: SwipeUiState,
    onDismiss: () -> Unit,
    onApply: () -> Unit,
    onUndoDecision: (String) -> Unit
) {
    var previewAsset by remember { mutableStateOf<Asset?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // Same container, header and tile treatment as the usage statistics popup.
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column {
                // Header: sits outside the scrolling content so it stays pinned
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.common_close), modifier = Modifier.size(28.dp))
                    }
                    Text(
                        text = stringResource(R.string.swipe_summary_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Spacer(Modifier.width(48.dp))
                }

                // Pinned stats and header; only the photo grid below scrolls, so there's no nested scrolling.
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    Spacer(Modifier.height(12.dp))

                    // Connected 2x2 group: tiles sit close with a small gap; a corner is fully rounded only when nothing touches it.
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            StatSummaryBox(
                                label = stringResource(R.string.swipe_keep),
                                count = uiState.keptCount,
                                size = uiState.keptSize,
                                badgeColor = MaterialTheme.colorScheme.primaryContainer,
                                badgeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                icon = Icons.Default.Check,
                                shape = summaryTileShape(column = 0, columns = 2, isFirstRow = true, isLastRow = false),
                                modifier = Modifier.weight(1f),
                                isEstimated = uiState.isRemainingEstimated
                            )
                            StatSummaryBox(
                                label = stringResource(R.string.swipe_delete),
                                count = uiState.deletedCount,
                                size = uiState.deletedSize,
                                badgeColor = MaterialTheme.colorScheme.errorContainer,
                                badgeContentColor = MaterialTheme.colorScheme.onErrorContainer,
                                icon = Icons.Default.Delete,
                                shape = summaryTileShape(column = 1, columns = 2, isFirstRow = true, isLastRow = false),
                                modifier = Modifier.weight(1f),
                                isEstimated = uiState.isRemainingEstimated
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            StatSummaryBox(
                                label = stringResource(R.string.swipe_archive),
                                count = uiState.archiveCount,
                                size = uiState.archiveSize,
                                badgeColor = MaterialTheme.colorScheme.secondaryContainer,
                                badgeContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                icon = Icons.Default.Archive,
                                shape = summaryTileShape(column = 0, columns = 2, isFirstRow = false, isLastRow = true),
                                modifier = Modifier.weight(1f),
                                isEstimated = uiState.isRemainingEstimated
                            )
                            StatSummaryBox(
                                label = stringResource(R.string.swipe_locked),
                                count = uiState.lockedCount,
                                size = uiState.lockedSize,
                                badgeColor = MaterialTheme.colorScheme.tertiaryContainer,
                                badgeContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                icon = Icons.Default.Lock,
                                shape = summaryTileShape(column = 1, columns = 2, isFirstRow = false, isLastRow = true),
                                modifier = Modifier.weight(1f),
                                isEstimated = uiState.isRemainingEstimated
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Spacer(
                                Modifier
                                    .size(8.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.swipe_check_before_delete).uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        val isLoadingDeletedAssets = uiState.deletedCount > uiState.deletedAssets.size
                        if (isLoadingDeletedAssets) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                WavyLoadingIndicator(size = 12.dp, strokeWidth = 2.dp, delayed = true)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "${uiState.deletedAssets.size} / ${uiState.deletedCount}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else if (uiState.deletedAssets.isNotEmpty()) {
                            Text(
                                pluralStringResource(R.plurals.swipe_summary_photos_count, uiState.deletedAssets.size, uiState.deletedAssets.size),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (uiState.deletedAssets.isNotEmpty()) {
                        val isLoadingDeletedAssets = uiState.deletedCount > uiState.deletedAssets.size
                        val gridState = rememberLazyGridState()
                        // Wraps short lists; otherwise fills the space left in the popup.
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.weight(1f, fill = false).fillMaxWidth()
                        ) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                state = gridState,
                                contentPadding = PaddingValues(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth().verticalFadingEdges(gridState, length = 32.dp)
                            ) {
                                items(
                                    items = uiState.deletedAssets,
                                    key = { it.id }
                                ) { asset ->
                                    DeletedAssetThumbnail(
                                        asset = asset,
                                        uiState = uiState,
                                        onClick = { previewAsset = asset },
                                        modifier = Modifier.animateItem()
                                    )
                                }
                                if (isLoadingDeletedAssets) {
                                    item(key = "loading_indicator") {
                                        Surface(
                                            modifier = Modifier
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(12.dp)),
                                            color = MaterialTheme.colorScheme.surfaceContainerHighest
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                SolidShapeLoadingIndicator(size = 32.dp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else if (uiState.deletedCount > 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth().height(100.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                SolidShapeLoadingIndicator(size = 40.dp)
                            }
                        }
                    } else {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth().height(100.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    stringResource(R.string.swipe_no_deletions),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }

                    if (uiState.isSyncing) {
                        Spacer(Modifier.height(16.dp))
                        WavyLoadingBar(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                }

                Box(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp)) {
                    val hasChanges = uiState.processedCount > 0 || uiState.localFavorites.isNotEmpty()

                    Button(
                        onClick = onApply,
                        enabled = !uiState.isSyncing && hasChanges,
                        colors = if (uiState.deletedCount > 0) {
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        } else {
                            ButtonDefaults.buttonColors()
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        val totalSize = formatSize(uiState.deletedSize)
                        Text(
                            text = if (uiState.isSyncing) stringResource(R.string.swipe_syncing) else stringResource(R.string.swipe_liberate_button, totalSize, uiState.deletedCount),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (previewAsset != null) {
        SummaryFullscreenPreviewDialog(
            initialAsset = previewAsset!!,
            assets = uiState.deletedAssets.ifEmpty { listOf(previewAsset!!) },
            uiState = uiState,
            onDismiss = { previewAsset = null },
            onRevert = { assetToRevert ->
                onUndoDecision(assetToRevert.id)
            }
        )
    }
}

/**
 * Shape of a tile in a group of up to two rows. Only corners on the group's outside edge are fully rounded;
 * a tile spanning the whole row ([columns] == 1) is rounded on both sides.
 */
private fun summaryTileShape(
    column: Int,
    columns: Int,
    isFirstRow: Boolean,
    isLastRow: Boolean,
    outer: Dp = 24.dp,
    inner: Dp = 6.dp
): RoundedCornerShape {
    val isFirstColumn = column == 0
    val isLastColumn = column == columns - 1
    return RoundedCornerShape(
        topStart = if (isFirstRow && isFirstColumn) outer else inner,
        topEnd = if (isFirstRow && isLastColumn) outer else inner,
        bottomStart = if (isLastRow && isFirstColumn) outer else inner,
        bottomEnd = if (isLastRow && isLastColumn) outer else inner
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StatSummaryBox(
    label: String,
    count: Int,
    size: Long,
    badgeColor: Color,
    badgeContentColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    isEstimated: Boolean = false,
    shape: Shape = RoundedCornerShape(16.dp),
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        // Compact two-row layout: badge + count on top, size centered below.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Neutral tile; the expressive cookie badge carries the only accent color.
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(badgeColor, MaterialShapes.Cookie9Sided.toShape()),
                    contentAlignment = Alignment.Center
                ) {
                    // No visible label; the badge announces it for accessibility.
                    Icon(icon, contentDescription = label, tint = badgeContentColor, modifier = Modifier.size(22.dp))
                }
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
            Text(
                text = if (isEstimated) "~ ${formatSize(size)}" else formatSize(size),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
fun DeletedAssetThumbnail(
    asset: Asset,
    uiState: SwipeUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val baseUrl = uiState.baseUrl.removeSuffix("/")

    val hasHeart = uiState.isFavorite(asset.id)
    val hasArchive = asset.isArchived
    val hasLock = asset.isLocked

    val imageRequest = remember(asset.id, baseUrl) {
        ImageRequest.Builder(context)
            .data("$baseUrl/api/assets/${asset.id}/thumbnail?format=WEBP")
            .crossfade(true)
            .precision(Precision.INEXACT)
            .build()
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (hasHeart) TimelineMiniBadge(Icons.Default.Favorite, Color.Red)
            if (hasArchive) TimelineMiniBadge(Icons.Default.Archive, Color.Black)
            if (hasLock) TimelineMiniBadge(Icons.Default.Lock, Color.Black)
        }

        if (asset.type == "VIDEO") {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
                    .size(16.dp)
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
            )
        } else if (asset.isGif) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.65f), shape = RoundedCornerShape(3.dp))
                    .padding(horizontal = 3.dp, vertical = 1.dp)
            ) {
                Text(
                    text = "GIF",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 10.sp
                )
            }
        }

        val assetSize = asset.exifInfo?.fileSizeInBytes ?: 0L
        if (assetSize > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))))
                    .padding(4.dp)
            ) {
                Text(
                    text = formatSize(assetSize),
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
fun SummaryFullscreenPreviewDialog(
    initialAsset: Asset,
    assets: List<Asset>,
    uiState: SwipeUiState,
    onDismiss: () -> Unit,
    onRevert: (Asset) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val dialogView = LocalView.current

        SideEffect {
            val window = (dialogView.parent as? DialogWindowProvider)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, dialogView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }

        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val baseUrlClean = uiState.baseUrl.removeSuffix("/")

        val initialIndex = remember(initialAsset.id) {
            assets.indexOfFirst { it.id == initialAsset.id }.coerceAtLeast(0)
        }
        val pagerState = rememberPagerState(initialPage = initialIndex) { assets.size }
        val currentAsset = assets.getOrNull(pagerState.currentPage) ?: initialAsset

        var isZoomedIn by remember(currentAsset.id) { mutableStateOf(false) }
        var dismissOffsetY by remember(currentAsset.id) { mutableFloatStateOf(0f) }

        val animOffsetY by animateFloatAsState(
            targetValue = dismissOffsetY,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "DismissOffsetY"
        )

        val bgAlpha = (1f - (animOffsetY / 600f)).coerceIn(0f, 1f)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = bgAlpha))
        ) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = !isZoomedIn,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationY = animOffsetY
                    }
                    .pointerInput(currentAsset.id, isZoomedIn) {
                        if (isZoomedIn) return@pointerInput
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (dismissOffsetY > 150f) {
                                    onDismiss()
                                } else {
                                    dismissOffsetY = 0f
                                }
                            },
                            onDragCancel = {
                                dismissOffsetY = 0f
                            },
                            onVerticalDrag = { change, dragAmount ->
                                if (dragAmount > 0f || dismissOffsetY > 0f) {
                                    dismissOffsetY = (dismissOffsetY + dragAmount).coerceAtLeast(0f)
                                    change.consume()
                                }
                            }
                        )
                    }
            ) { page ->
                val pageAsset = assets.getOrNull(page) ?: currentAsset
                ZoomableBox(
                    modifier = Modifier.fillMaxSize(),
                    resetOnRelease = false,
                    onIsZoomedChanged = { isZoomedIn = it }
                ) {
                    if (pageAsset.type == "VIDEO") {
                        DuplicateVideoPlayer(
                            asset = pageAsset,
                            baseUrl = baseUrlClean,
                            handleAudioFocus = uiState.playbackBehavior != PlaybackBehavior.IGNORE,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        val photoRequest =
                            remember(pageAsset.id, baseUrlClean, pageAsset.isGif) {
                                ImageRequest.Builder(context)
                                    .data(
                                        if (pageAsset.isGif) "$baseUrlClean/api/assets/${pageAsset.id}/original"
                                        else "$baseUrlClean/api/assets/${pageAsset.id}/thumbnail?format=WEBP&size=preview"
                                    )
                                    .crossfade(true)
                                    .build()
                            }

                        AsyncImage(
                            model = photoRequest,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val fileSize = currentAsset.exifInfo?.fileSizeInBytes ?: 0L
                    if (fileSize > 0) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = formatSize(fileSize),
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                    }

                    if (assets.size > 1) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "${pagerState.currentPage + 1} / ${assets.size}",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.common_close),
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Bottom Action Bar
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = if (currentAsset.type == "VIDEO") 88.dp else 24.dp)
            ) {
                Button(
                    onClick = {
                        onRevert(currentAsset)
                        if (assets.size <= 1) {
                            onDismiss()
                        } else {
                            val nextTargetPage = (pagerState.currentPage).coerceAtMost(assets.size - 2)
                            if (nextTargetPage >= 0) {
                                scope.launch {
                                    pagerState.scrollToPage(nextTargetPage)
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(24.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .padding(horizontal = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.swipe_revert_delete),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}
