package com.markvoronin.immichswipe.feature.duplicates.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.core.ImmichLauncher
import com.markvoronin.immichswipe.core.cache.VideoPreloader
import com.markvoronin.immichswipe.domain.model.Asset
import com.markvoronin.immichswipe.feature.common.MediaLoadingIndicator
import com.markvoronin.immichswipe.feature.duplicates.DuplicateDecision
import com.markvoronin.immichswipe.feature.settings.components.horizontalFadingEdges
import com.markvoronin.immichswipe.feature.swipe.components.ZoomableBox
import com.markvoronin.immichswipe.feature.swipe.components.checkedWidthFactor
import com.markvoronin.immichswipe.feature.swipe.components.connectedTileShape
import com.markvoronin.immichswipe.feature.swipe.components.previewButtonColors
import com.markvoronin.immichswipe.feature.swipe.components.previewTileColor
import com.markvoronin.immichswipe.feature.swipe.components.previewToggleColors
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

// Small tiles keep the chrome quiet around the photo; the buttons still get a 48dp touch target.
private val ControlTileWidth = 48.dp
private val ControlTileHeight = 40.dp
private val ThumbShape = RoundedCornerShape(12.dp)

@Composable
fun FullScreenPreviewModal(
    previewData: FullScreenPreviewData?,
    decisions: Map<String, DuplicateDecision>,
    isFavorite: (Asset) -> Boolean,
    baseUrl: String = "",
    onDecisionToggle: (String) -> Unit,
    onFavoriteToggle: (Asset) -> Unit,
    onClusterChanged: (Int) -> Unit = {},
    onDismiss: () -> Unit
) {
    if (previewData == null) return

    val clusters = remember(previewData) {
        previewData.clusters.ifEmpty { listOf(previewData.cluster) }
    }
    var currentClusterIdx by remember(previewData) {
        mutableIntStateOf(previewData.clusterIndex.coerceIn(0, clusters.size - 1))
    }

    val currentOnClusterChanged by rememberUpdatedState(onClusterChanged)
    LaunchedEffect(currentClusterIdx) {
        currentOnClusterChanged(currentClusterIdx)
    }

    val currentCluster = clusters.getOrNull(currentClusterIdx) ?: previewData.cluster
    val assets = currentCluster.assets
    if (assets.isEmpty()) return

    val context = LocalContext.current
    val density = LocalDensity.current
    val baseUrlClean = baseUrl.removeSuffix("/")

    val currentOnDecisionToggle by rememberUpdatedState(onDecisionToggle)
    val currentOnFavoriteToggle by rememberUpdatedState(onFavoriteToggle)

    // Set when swiping back from the first asset, so the previous cluster opens on its last asset.
    var enterClusterAtLast by remember(previewData) { mutableStateOf(false) }
    val initialIndex = remember(currentCluster.clusterId, previewData) {
        if (enterClusterAtLast) {
            assets.size - 1
        } else if (currentCluster.clusterId == previewData.cluster.clusterId) {
            previewData.initialIndex.coerceIn(0, assets.size - 1)
        } else {
            0
        }
    }
    var selectedIndex by remember(currentCluster.clusterId) { mutableIntStateOf(initialIndex) }

    LaunchedEffect(selectedIndex, currentClusterIdx, baseUrlClean) {
        if (baseUrlClean.isEmpty()) return@LaunchedEffect

        // Small delay to allow the current full screen asset's main request to start first
        delay(150.milliseconds)

        val nextAsset = assets.getOrNull(selectedIndex + 1)
            ?: clusters.getOrNull(currentClusterIdx + 1)?.assets?.firstOrNull()
        val prevAsset = assets.getOrNull(selectedIndex - 1)
            ?: clusters.getOrNull(currentClusterIdx - 1)?.assets?.lastOrNull()

        val targetsToPreload = listOfNotNull(nextAsset, prevAsset).distinct()

        targetsToPreload.forEach { targetAsset ->
            if (targetAsset.type == "VIDEO") {
                val videoUrl = "$baseUrlClean/api/assets/${targetAsset.id}/video/playback"
                VideoPreloader.preload(
                    context = context.applicationContext,
                    assetId = targetAsset.id,
                    videoUrl = videoUrl,
                )
                val thumbUrl = "$baseUrlClean/api/assets/${targetAsset.id}/thumbnail?format=WEBP&size=preview"
                val thumbRequest = ImageRequest.Builder(context)
                    .data(thumbUrl)
                    .build()
                context.imageLoader.enqueue(thumbRequest)
            } else {
                val imageUrl = "$baseUrlClean/api/assets/${targetAsset.id}/original"
                val imageRequest = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .size(coil3.size.Size.ORIGINAL)
                    .build()
                context.imageLoader.enqueue(imageRequest)

                if (!targetAsset.isGif) {
                    val thumbUrl = "$baseUrlClean/api/assets/${targetAsset.id}/thumbnail?format=WEBP&size=preview"
                    val thumbRequest = ImageRequest.Builder(context)
                        .data(thumbUrl)
                        .build()
                    context.imageLoader.enqueue(thumbRequest)
                }
            }
        }
    }
    val carouselState = remember(currentCluster.clusterId) {
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
            if (closestIndex != null && closestIndex in assets.indices && closestIndex != selectedIndex) {
                selectedIndex = closestIndex
            }
        }
    }

    LaunchedEffect(selectedIndex, carouselWidthPx) {
        if (carouselWidthPx > 0 && !carouselState.isScrollInProgress && selectedIndex in assets.indices) {
            carouselState.scrollToItem(selectedIndex, 0)
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
                        selectedIndex = closest
                        carouselState.animateScrollToItem(closest, 0)
                    }
                }
            }
        }
    }

    var dismissOffsetY by remember { mutableFloatStateOf(0f) }
    var isDraggingDismiss by remember { mutableStateOf(false) }

    val motionScheme = MaterialTheme.motionScheme
    val animDismissOffsetY by animateFloatAsState(
        targetValue = dismissOffsetY,
        animationSpec = motionScheme.fastSpatialSpec(),
        label = "FullScreenDismiss"
    )

    val currentDismissOffsetY = if (isDraggingDismiss) dismissOffsetY else animDismissOffsetY
    val backdropAlpha = (1f - (currentDismissOffsetY / 800f)).coerceIn(0f, 1f)

    // Live horizontal drag (px) while pulling past the first/last asset towards a neighbouring cluster.
    var boundaryDragX by remember { mutableFloatStateOf(0f) }
    var isDraggingBoundary by remember { mutableStateOf(false) }
    val animBoundaryDragX by animateFloatAsState(
        targetValue = boundaryDragX,
        animationSpec = motionScheme.fastSpatialSpec(),
        label = "ClusterBoundaryDrag"
    )

    // Signed progress of the cluster transition: +1 / -1 right after moving to the next / previous
    // cluster, easing to 0. Swiping between assets of the same cluster never touches it.
    val clusterSlide = remember { Animatable(0f) }
    var lastClusterIdx by remember(previewData) { mutableIntStateOf(currentClusterIdx) }
    LaunchedEffect(currentClusterIdx) {
        enterClusterAtLast = false
        if (currentClusterIdx == lastClusterIdx) return@LaunchedEffect
        val direction = if (currentClusterIdx > lastClusterIdx) 1f else -1f
        lastClusterIdx = currentClusterIdx
        clusterSlide.snapTo(direction)
        clusterSlide.animateTo(0f, motionScheme.fastSpatialSpec())
    }

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
                insetsController.show(WindowInsetsCompat.Type.statusBars())
                insetsController.isAppearanceLightStatusBars = false
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = backdropAlpha))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationY = currentDismissOffsetY
                    }
            ) {
                val currentAsset = assets.getOrNull(selectedIndex) ?: assets.first()
                val currentDecision = decisions[currentAsset.id] ?: DuplicateDecision.NONE
                val currentIsFav = isFavorite(currentAsset)

                var isFileNameExpanded by remember(currentAsset.id) { mutableStateOf(false) }
                var showMetadataDialog by remember { mutableStateOf(false) }

                val buttonColors = previewButtonColors()
                val pressedShape = RoundedCornerShape(10.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                isFileNameExpanded = !isFileNameExpanded
                            }
                    ) {
                        val sizeStr = currentAsset.exifInfo?.fileSizeInBytes?.let { formatSizeStr(it) } ?: ""
                        val originalName = currentAsset.originalFileName ?: stringResource(R.string.duplicates_photo_fallback)

                        val displayedName = if (isFileNameExpanded) {
                            originalName
                        } else {
                            formatMiddleEllipsisFileName(originalName, maxLength = 22)
                        }

                        Text(
                            text = displayedName,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = if (isFileNameExpanded) Int.MAX_VALUE else 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (sizeStr.isNotEmpty()) {
                            Text(
                                text = sizeStr,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    // Same tiles as the swipe fullscreen toolbar: a connected pair, and a separate close button.
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        FilledTonalIconToggleButton(
                            checked = currentIsFav,
                            onCheckedChange = { currentOnFavoriteToggle(currentAsset) },
                            shapes = IconButtonDefaults.toggleableShapes(
                                shape = connectedTileShape(0, 2, outer = ControlTileHeight / 2),
                                pressedShape = pressedShape,
                                checkedShape = CircleShape
                            ),
                            colors = previewToggleColors(),
                            modifier = Modifier.size(width = ControlTileWidth * checkedWidthFactor(currentIsFav), height = ControlTileHeight)
                        ) {
                            Icon(
                                imageVector = if (currentIsFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = stringResource(R.string.cd_favorite)
                            )
                        }

                        FilledTonalIconButton(
                            onClick = { showMetadataDialog = true },
                            shapes = IconButtonDefaults.shapes(
                                shape = connectedTileShape(1, 2, outer = ControlTileHeight / 2),
                                pressedShape = pressedShape
                            ),
                            colors = buttonColors,
                            modifier = Modifier.size(width = ControlTileWidth, height = ControlTileHeight)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = stringResource(R.string.cd_asset_info)
                            )
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    FilledTonalIconButton(
                        onClick = onDismiss,
                        shapes = IconButtonDefaults.shapes(),
                        colors = buttonColors,
                        modifier = Modifier.size(ControlTileHeight)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.common_close)
                        )
                    }
                }

                if (showMetadataDialog) {
                    Dialog(onDismissRequest = { showMetadataDialog = false }) {
                        Surface(
                            shape = MaterialTheme.shapes.extraLarge,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(20.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Outlined.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        val titleRes = if (currentAsset.isVideo) R.string.swipe_metadata_title_video else R.string.swipe_metadata_title
                                        Text(
                                            text = stringResource(titleRes),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    IconButton(onClick = { showMetadataDialog = false }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = stringResource(R.string.common_close)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                val originalName = currentAsset.originalFileName ?: stringResource(R.string.diag_unknown)
                                val formatLabel = if (currentAsset.fileExtension != null) {
                                    "${currentAsset.type} (.${currentAsset.fileExtension.lowercase()})"
                                } else currentAsset.type
                                val sizeLabel = currentAsset.exifInfo?.fileSizeInBytes?.let { formatSizeStr(it) } ?: stringResource(R.string.duplicates_unknown)
                                val resolutionLabel = currentAsset.exifInfo?.let {
                                    "${it.imageWidth ?: "?"} × ${it.imageHeight ?: "?"}"
                                } ?: stringResource(R.string.duplicates_unknown)
                                val dateLabel = currentAsset.effectiveDate.substringBefore("T")

                                MetadataRow(
                                    icon = Icons.Default.Description,
                                    label = stringResource(R.string.swipe_metadata_file),
                                    value = originalName
                                )
                                MetadataRow(
                                    icon = Icons.Default.CalendarToday,
                                    label = stringResource(R.string.swipe_metadata_date),
                                    value = dateLabel
                                )
                                MetadataRow(
                                    icon = Icons.Default.Info,
                                    label = stringResource(R.string.swipe_metadata_format),
                                    value = formatLabel
                                )
                                MetadataRow(
                                    icon = Icons.Default.SdStorage,
                                    label = stringResource(R.string.swipe_metadata_size),
                                    value = sizeLabel
                                )
                                MetadataRow(
                                    icon = Icons.Default.AspectRatio,
                                    label = stringResource(R.string.swipe_metadata_resolution),
                                    value = resolutionLabel
                                )

                                Spacer(Modifier.height(20.dp))

                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(CircleShape)
                                        .combinedClickable(
                                            onClick = {
                                                ImmichLauncher.openAssetInImmich(context, baseUrlClean, currentAsset.id)
                                            },
                                            onLongClick = if (baseUrlClean.isNotBlank()) {
                                                {
                                                    ImmichLauncher.openInWeb(context, baseUrlClean, currentAsset.id)
                                                }
                                            } else null
                                        )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(R.string.settings_card_immich),
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }

                                if (baseUrlClean.isNotBlank()) {
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = stringResource(R.string.settings_immich_long_press_web),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )
                                }
                            }
                        }
                    }
                }

                var isZoomedIn by remember { mutableStateOf(false) }

                LaunchedEffect(currentAsset.id) {
                    dismissOffsetY = 0f
                    isDraggingDismiss = false
                    boundaryDragX = 0f
                    isDraggingBoundary = false
                }

                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .graphicsLayer {
                            val dragX = if (isDraggingBoundary) boundaryDragX else animBoundaryDragX
                            translationX = dragX + clusterSlide.value * size.width * 0.25f
                            alpha = (1f - abs(clusterSlide.value)) * (1f - abs(dragX) / size.width).coerceIn(0f, 1f)
                        }
                ) {
                    val containerHeightPx = with(density) { maxHeight.toPx() }

                    val swipeModifier = Modifier
                        .fillMaxSize()
                        .pointerInput(currentAsset.id, isZoomedIn) {
                            if (isZoomedIn) return@pointerInput
                            awaitEachGesture {
                                val down = awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)
                                val startY = down.position.y
                                val controlsHeightPx = with(density) { 70.dp.toPx() }
                                val isTouchInControls = currentAsset.type == "VIDEO" && startY > (containerHeightPx - controlsHeightPx)

                                if (isTouchInControls) {
                                    // Touch started in video controls bar (Play/Pause, Seekbar, Mute). Let child controls handle it.
                                    return@awaitEachGesture
                                }

                                var isDraggingHorizontal = false
                                var isDraggingVertical = false
                                var totalDragX = 0f
                                var totalDragY = 0f
                                val touchSlop = viewConfiguration.touchSlop

                                do {
                                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                    if (event.changes.size == 1) {
                                        val change = event.changes[0]
                                        val panChange = event.calculatePan()
                                        totalDragX += panChange.x
                                        totalDragY += panChange.y

                                        val absX = abs(totalDragX)
                                        val absY = abs(totalDragY)

                                        if (!isDraggingHorizontal && !isDraggingVertical) {
                                            if (absX > touchSlop && absX > absY) {
                                                isDraggingHorizontal = true
                                            } else if (absY > touchSlop && absY > absX && totalDragY > 0f) {
                                                isDraggingVertical = true
                                                isDraggingDismiss = true
                                            }
                                        }

                                        if (isDraggingHorizontal) {
                                            val pullingNext = totalDragX < 0f &&
                                                selectedIndex == assets.size - 1 && currentClusterIdx < clusters.size - 1
                                            val pullingPrevious = totalDragX > 0f &&
                                                selectedIndex == 0 && currentClusterIdx > 0
                                            isDraggingBoundary = pullingNext || pullingPrevious
                                            boundaryDragX = if (isDraggingBoundary) totalDragX else 0f
                                            change.consume()
                                        } else if (isDraggingVertical) {
                                            dismissOffsetY = (totalDragY - touchSlop).coerceAtLeast(0f)
                                            change.consume()
                                        }
                                    }
                                } while (event.changes.any { it.pressed })

                                if (isDraggingVertical) {
                                    isDraggingDismiss = false
                                    if (dismissOffsetY > 200f) {
                                        onDismiss()
                                    } else {
                                        dismissOffsetY = 0f
                                    }
                                } else if (isDraggingHorizontal) {
                                    isDraggingBoundary = false
                                    boundaryDragX = 0f
                                    if (totalDragX < -100f) {
                                        if (selectedIndex < assets.size - 1) {
                                            selectedIndex++
                                        } else if (currentClusterIdx < clusters.size - 1) {
                                            currentClusterIdx++
                                        }
                                    } else if (totalDragX > 100f) {
                                        if (selectedIndex > 0) {
                                            selectedIndex--
                                        } else if (currentClusterIdx > 0) {
                                            enterClusterAtLast = true
                                            currentClusterIdx--
                                        }
                                    }
                                }
                            }
                        }

                    // Lets ZoomableBox keep panning within the photo itself; null (box bounds) when EXIF has no size.
                    val zoomAspectRatio = currentAsset.exifInfo
                        ?.takeIf { (it.imageWidth ?: 0) > 0 && (it.imageHeight ?: 0) > 0 }
                        ?.displayAspectRatio

                    if (currentAsset.type == "VIDEO") {
                        DuplicateVideoPlayer(
                            asset = currentAsset,
                            baseUrl = baseUrlClean,
                            modifier = swipeModifier,
                            contentScale = ContentScale.Fit,
                            onTap = {
                                if (!isZoomedIn) {
                                    currentOnDecisionToggle(currentAsset.id)
                                }
                            },
                            videoSurfaceWrapper = { videoSurface ->
                                ZoomableBox(
                                    modifier = Modifier.fillMaxSize(),
                                    resetOnRelease = false,
                                    aspectRatio = zoomAspectRatio,
                                    enableDoubleTapZoom = isZoomedIn,
                                    onIsZoomedChanged = { zoomed ->
                                        isZoomedIn = zoomed
                                    },
                                    onTap = { _, _ ->
                                        if (!isZoomedIn) {
                                            currentOnDecisionToggle(currentAsset.id)
                                        }
                                    }
                                ) {
                                    videoSurface()
                                }
                            }
                        )
                    } else {
                        var isImageLoading by remember(currentAsset.id) { mutableStateOf(true) }

                        Box(modifier = swipeModifier) {
                            ZoomableBox(
                                modifier = Modifier.fillMaxSize(),
                                resetOnRelease = false,
                                aspectRatio = zoomAspectRatio,
                                enableDoubleTapZoom = isZoomedIn,
                                onIsZoomedChanged = { zoomed ->
                                    isZoomedIn = zoomed
                                },
                                onTap = { _, _ ->
                                    if (!isZoomedIn) {
                                        currentOnDecisionToggle(currentAsset.id)
                                    }
                                }
                            ) {
                                val imageRequest = remember(currentAsset.id, baseUrlClean) {
                                    ImageRequest.Builder(context)
                                        .data("$baseUrlClean/api/assets/${currentAsset.id}/original")
                                        .size(coil3.size.Size.ORIGINAL)
                                        .crossfade(false)
                                        .build()
                                }
                                AsyncImage(
                                    model = imageRequest,
                                    contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize(),
                                    onState = { state ->
                                        isImageLoading = state is coil3.compose.AsyncImagePainter.State.Loading
                                    }
                                )
                            }
                            
                            if (isImageLoading) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    MediaLoadingIndicator()
                                }
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .navigationBarsPadding()
                        .padding(top = 8.dp, bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // The current photo's decision; tapping it cycles like tapping the photo. Same green and red as the cluster card.
                    val toolbarContentColor = FloatingToolbarDefaults.vibrantFloatingToolbarColors().toolbarContentColor
                    val decisionContainer by animateColorAsState(
                        targetValue = if (currentDecision == DuplicateDecision.NONE) previewTileColor() else decisionColor(currentDecision),
                        animationSpec = motionScheme.defaultEffectsSpec(),
                        label = "decisionContainer"
                    )
                    val decisionContent by animateColorAsState(
                        targetValue = if (currentDecision == DuplicateDecision.NONE) toolbarContentColor else Color.White,
                        animationSpec = motionScheme.defaultEffectsSpec(),
                        label = "decisionContent"
                    )
                    Surface(
                        onClick = { currentOnDecisionToggle(currentAsset.id) },
                        shape = CircleShape,
                        color = decisionContainer,
                        contentColor = decisionContent,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        AnimatedContent(
                            targetState = currentDecision,
                            transitionSpec = {
                                fadeIn(motionScheme.defaultEffectsSpec()) togetherWith fadeOut(motionScheme.fastEffectsSpec()) using
                                    SizeTransform(clip = false) { _, _ -> motionScheme.defaultSpatialSpec() }
                            },
                            contentAlignment = Alignment.Center,
                            label = "decisionPill"
                        ) { decision ->
                            Row(
                                modifier = Modifier
                                    .height(40.dp)
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (decision != DuplicateDecision.NONE) {
                                    Icon(
                                        imageVector = if (decision == DuplicateDecision.DELETE) Icons.Default.Delete else Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text(
                                    text = when (decision) {
                                        DuplicateDecision.DELETE -> stringResource(R.string.swipe_delete_upper)
                                        DuplicateDecision.KEEP -> stringResource(R.string.swipe_keep_upper)
                                        DuplicateDecision.NONE -> stringResource(R.string.duplicates_decision_none)
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    LazyRow(
                        state = carouselState,
                        modifier = Modifier
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
                                            Modifier.border(2.dp, decisionColor(assetDecision), ThumbShape)
                                        } else Modifier
                                    )
                                    .clickable {
                                        selectedIndex = index
                                    }
                            ) {
                                val thumbRequest = remember(asset.id, baseUrlClean, asset.isGif) {
                                    ImageRequest.Builder(context)
                                        .data(
                                            if (asset.isGif) "$baseUrlClean/api/assets/${asset.id}/original"
                                            else "$baseUrlClean/api/assets/${asset.id}/thumbnail?format=WEBP&size=preview"
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

                                DecisionBadge(
                                    decision = assetDecision,
                                    size = 16.dp,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(3.dp)
                                )

                                if (assetFav || asset.type == "VIDEO") {
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(2.dp),
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (assetFav) {
                                            Icon(
                                                imageVector = Icons.Default.Favorite,
                                                contentDescription = null,
                                                tint = Color.Red,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                        if (asset.type == "VIDEO") {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = stringResource(R.string.cd_video),
                                                tint = Color.White,
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                            )
                                        }
                                    }
                                }

                                val thumbSizeStr = asset.exifInfo?.fileSizeInBytes?.let { formatSizeStr(it) }
                                if (thumbSizeStr != null) {
                                    Text(
                                        text = thumbSizeStr,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                                        color = Color.White,
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .padding(horizontal = 2.dp, vertical = 1.dp),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Previous group, position, next group as one connected group, like the swipe fullscreen toolbar.
                    val navColors = buttonColors.copy(
                        disabledContainerColor = previewTileColor().copy(alpha = 0.3f),
                        disabledContentColor = toolbarContentColor.copy(alpha = 0.38f)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val hasPreviousGroup = currentClusterIdx > 0
                        FilledTonalIconButton(
                            onClick = { if (hasPreviousGroup) currentClusterIdx-- },
                            enabled = hasPreviousGroup,
                            shapes = IconButtonDefaults.shapes(
                                shape = connectedTileShape(0, 3, outer = ControlTileHeight / 2),
                                pressedShape = pressedShape
                            ),
                            colors = navColors,
                            modifier = Modifier.size(width = ControlTileWidth, height = ControlTileHeight)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.NavigateBefore,
                                contentDescription = stringResource(R.string.cd_previous_group)
                            )
                        }

                        Surface(
                            shape = connectedTileShape(1, 3),
                            color = previewTileColor(),
                            contentColor = toolbarContentColor,
                            modifier = Modifier
                                .height(ControlTileHeight)
                                .widthIn(min = 136.dp)
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (clusters.size > 1) {
                                        stringResource(R.string.duplicates_ai_cluster_progress, currentClusterIdx + 1, clusters.size)
                                    } else {
                                        "${selectedIndex + 1} / ${assets.size}"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        val hasNextGroup = currentClusterIdx < clusters.size - 1
                        FilledTonalIconButton(
                            onClick = { if (hasNextGroup) currentClusterIdx++ },
                            enabled = hasNextGroup,
                            shapes = IconButtonDefaults.shapes(
                                shape = connectedTileShape(2, 3, outer = ControlTileHeight / 2),
                                pressedShape = pressedShape
                            ),
                            colors = navColors,
                            modifier = Modifier.size(width = ControlTileWidth, height = ControlTileHeight)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                                contentDescription = stringResource(R.string.cd_next_group)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            modifier = Modifier
                .padding(top = 2.dp)
                .size(18.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier
                .padding(top = 2.dp)
                .width(80.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
    }
}
