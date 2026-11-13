package com.markvoronin.immichswipe.feature.duplicates.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
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
import com.markvoronin.immichswipe.feature.swipe.components.AssetInfoDialog
import com.markvoronin.immichswipe.feature.swipe.components.ZoomableBox
import com.markvoronin.immichswipe.feature.swipe.components.checkedWidthFactor
import com.markvoronin.immichswipe.feature.swipe.components.connectedTileShape
import com.markvoronin.immichswipe.feature.swipe.components.previewTileColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

// Small tiles keep the chrome quiet around the photo; the buttons still get a 48dp touch target.
private val ControlTileWidth = 48.dp
private val ControlTileHeight = 40.dp
private val ThumbShape = RoundedCornerShape(12.dp)
// A 16dp badge inset 4dp shares its centre with the 12dp corner arc, so it never touches the corner or the 2dp frame.
private val ThumbBadgeSize = 16.dp
private val ThumbBadgeInset = 4.dp

// Taps don't decide anything here (only the decision buttons do), so a double-tap is free to zoom.
private const val DoubleTapZoom = 5f

// The scheme's springs settle to 0.01px, a long invisible tail before the next cluster can take over; stop within a pixel.
private fun FiniteAnimationSpec<Float>.settlingWithinPixel(): FiniteAnimationSpec<Float> =
    (this as? SpringSpec<Float>)?.let { spring(it.dampingRatio, it.stiffness, visibilityThreshold = 1f) } ?: this

// The same spring without the bounce, so a closing photo lands in its thumbnail instead of shrinking past it.
internal fun FiniteAnimationSpec<Float>.withoutBounce(): FiniteAnimationSpec<Float> =
    (this as? SpringSpec<Float>)?.let { spring(1f, it.stiffness, visibilityThreshold = 0.01f) } ?: this

// Where a ContentScale.Fit photo of [aspectRatio] sits in [size]; the whole box when the ratio is unknown.
internal fun fitRect(size: Size, aspectRatio: Float?): Rect {
    if (aspectRatio == null || aspectRatio <= 0f || size.height <= 0f) return Rect(Offset.Zero, size)
    val fitted = if (aspectRatio > size.width / size.height) {
        Size(size.width, size.width / aspectRatio)
    } else {
        Size(size.height * aspectRatio, size.height)
    }
    return Rect(Offset((size.width - fitted.width) / 2f, (size.height - fitted.height) / 2f), fitted)
}

internal fun rectAround(center: Offset, width: Float, height: Float) =
    Rect(center - Offset(width / 2f, height / 2f), Size(width, height))

internal class RoundRectClip(private val rect: Rect, private val radius: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) =
        Outline.Rounded(RoundRect(rect, CornerRadius(radius)))
}

// Favourite, info, close and group navigation sit back behind the decision buttons: a faint tint of the toolbar color
// with icons in the full color.
@Composable
private fun quietTileColor(): Color =
    FloatingToolbarDefaults.vibrantFloatingToolbarColors().toolbarContainerColor.copy(alpha = 0.22f)

// The lighter of the toolbar's two colors, so icons stay readable on the black backdrop in light and dark themes.
@Composable
private fun quietContentColor(): Color {
    val colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors()
    return if (colors.toolbarContainerColor.luminance() > colors.toolbarContentColor.luminance()) {
        colors.toolbarContainerColor
    } else {
        colors.toolbarContentColor
    }
}

@Composable
private fun quietButtonColors(): IconButtonColors {
    val accent = FloatingToolbarDefaults.vibrantFloatingToolbarColors().toolbarContainerColor
    return IconButtonDefaults.filledTonalIconButtonColors(
        containerColor = quietTileColor(),
        contentColor = quietContentColor(),
        disabledContainerColor = accent.copy(alpha = 0.1f),
        disabledContentColor = quietContentColor().copy(alpha = 0.38f)
    )
}

// A checked favourite gets a stronger tint; its filled icon and round shape carry the rest, so it never outshines the
// decision buttons.
@Composable
private fun quietToggleColors(): IconToggleButtonColors {
    val accent = FloatingToolbarDefaults.vibrantFloatingToolbarColors().toolbarContainerColor
    return IconButtonDefaults.filledTonalIconToggleButtonColors(
        containerColor = quietTileColor(),
        contentColor = quietContentColor(),
        checkedContainerColor = accent.copy(alpha = 0.4f),
        checkedContentColor = quietContentColor()
    )
}

@Composable
fun FullScreenPreviewModal(
    previewData: FullScreenPreviewData?,
    decisions: Map<String, DuplicateDecision>,
    isFavorite: (Asset) -> Boolean,
    baseUrl: String = "",
    handleAudioFocus: Boolean = true,
    loadAlbumNames: suspend (String) -> List<String> = { emptyList() },
    onDecisionChange: (String, DuplicateDecision) -> Unit,
    onFavoriteToggle: (Asset) -> Unit,
    onClusterChanged: (Int) -> Unit = {},
    // A card thumbnail's bounds in window coordinates, for the photo to grow from and shrink back into.
    thumbnailBounds: (String) -> Rect? = { null },
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

    val currentOnDecisionChange by rememberUpdatedState(onDecisionChange)
    val currentOnFavoriteToggle by rememberUpdatedState(onFavoriteToggle)
    val currentLoadAlbumNames by rememberUpdatedState(loadAlbumNames)

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
            } else if (!targetAsset.isGif) {
                // Only the preview: originals are 6-20 MB downloads and decode to tens of MB each.
                // The preview shows at once when the photo opens, while its original loads.
                val thumbUrl = "$baseUrlClean/api/assets/${targetAsset.id}/thumbnail?format=WEBP&size=preview"
                val thumbRequest = ImageRequest.Builder(context)
                    .data(thumbUrl)
                    .build()
                context.imageLoader.enqueue(thumbRequest)
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

    val glideSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    // Slides the strip until [index] sits in the centre (offset 0 after the side padding). Jumps instead when that
    // thumbnail isn't laid out yet, as when the strip first appears or a new cluster opens.
    suspend fun LazyListState.glideTo(index: Int) {
        val target = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
        if (target != null) animateScrollBy(target.offset.toFloat(), glideSpec) else scrollToItem(index, 0)
    }

    LaunchedEffect(carouselState, carouselWidthPx) {
        if (carouselWidthPx > 0 && !isUserDragging && selectedIndex in assets.indices) {
            carouselState.scrollToItem(selectedIndex, 0)
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
                        selectedIndex = closest
                        carouselState.glideTo(closest)
                    }
                }
            }
        }
    }

    var dismissOffsetY by remember { mutableFloatStateOf(0f) }
    // Stays set through a close that follows a drag, so the photo leaves from where the finger let go.
    var isDraggingDismiss by remember { mutableStateOf(false) }

    val motionScheme = MaterialTheme.motionScheme
    val animDismissOffsetY by animateFloatAsState(
        targetValue = dismissOffsetY,
        animationSpec = motionScheme.fastSpatialSpec(),
        label = "FullScreenDismiss"
    )

    // Only read in draw-phase lambdas, so a drag doesn't recompose the whole viewer.
    fun dismissDrag() = if (isDraggingDismiss) dismissOffsetY else animDismissOffsetY

    // Live horizontal drag (px) while pulling past the first/last asset towards a neighbouring cluster.
    var boundaryDragX by remember { mutableFloatStateOf(0f) }
    var isDraggingBoundary by remember { mutableStateOf(false) }
    // Takes over from the finger on release: slides on to the neighbouring cluster or back to the current one.
    val boundarySettle = remember { Animatable(0f) }
    // +1 / -1 while the next / previous cluster's entry asset peeks in, through the drag and the settle after it.
    var boundaryDirection by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    // 0 with the photo in its card thumbnail, 1 open. Opening and every way of closing animate this one value.
    val presence = remember { Animatable(0f) }
    // A thumbnail half scrolled out of its row is clipped and no longer 3:4, so the photo fades instead.
    fun visibleThumbnail(assetId: String?) = assetId?.let(thumbnailBounds)
        ?.takeIf { it.height > 0f && abs(it.width / it.height - 0.75f) < 0.01f }
    var thumbnailRect by remember { mutableStateOf(visibleThumbnail(previewData.cluster.assets.getOrNull(previewData.initialIndex)?.id)) }
    var isClosing by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        presence.animateTo(1f, motionScheme.defaultSpatialSpec())
    }
    fun close() {
        if (isClosing) return
        isClosing = true
        thumbnailRect = visibleThumbnail(assets.getOrNull(selectedIndex)?.id)
        scope.launch {
            presence.animateTo(0f, motionScheme.defaultSpatialSpec<Float>().withoutBounce())
            onDismiss()
        }
    }
    fun backdropAlpha() = presence.value.coerceIn(0f, 1f) * (1f - dismissDrag() / 800f).coerceIn(0f, 1f)
    fun chromeAlpha() = presence.value.coerceIn(0f, 1f) * (1f - dismissDrag() / 400f).coerceIn(0f, 1f)

    // A tap on a video toggles its controls; while it plays they also hide after a few idle seconds.
    var isTouching by remember { mutableStateOf(false) }
    // Shared by every video here, so swiping to the next one keeps it muted.
    var isMuted by remember { mutableStateOf(false) }

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
        onDismissRequest = { close() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val dialogView = LocalView.current
        SideEffect {
            val window = (dialogView.parent as? DialogWindowProvider)?.window
            if (window != null) {
                // The photo's own transition replaces the window fade and dim.
                window.setWindowAnimations(0)
                window.setDimAmount(0f)
                val insetsController = WindowCompat.getInsetsController(window, dialogView)
                insetsController.show(WindowInsetsCompat.Type.statusBars())
                insetsController.isAppearanceLightStatusBars = false
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind { drawRect(Color.Black, alpha = backdropAlpha()) }
                // Watches without consuming: a finger anywhere holds off the auto-hide.
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            isTouching = awaitPointerEvent(PointerEventPass.Initial).changes.any { it.pressed }
                        }
                    }
                }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                val currentAsset = assets.getOrNull(selectedIndex) ?: assets.first()
                val currentDecision = decisions[currentAsset.id] ?: DuplicateDecision.NONE
                val currentIsFav = isFavorite(currentAsset)

                var playerControlsVisible by remember(currentAsset.id) { mutableStateOf(true) }
                var isVideoPlaying by remember(currentAsset.id) { mutableStateOf(false) }
                LaunchedEffect(currentAsset.id, playerControlsVisible, isVideoPlaying, isTouching) {
                    if (playerControlsVisible && isVideoPlaying && !isTouching) {
                        delay(3.seconds)
                        playerControlsVisible = false
                    }
                }

                var isFileNameExpanded by remember(currentAsset.id) { mutableStateOf(false) }
                var showMetadataDialog by remember { mutableStateOf(false) }

                val buttonColors = quietButtonColors()
                val pressedShape = RoundedCornerShape(10.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { alpha = chromeAlpha() }
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
                            colors = quietToggleColors(),
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
                        onClick = { close() },
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
                    AssetInfoDialog(
                        asset = currentAsset,
                        baseUrl = baseUrlClean,
                        loadAlbumNames = currentLoadAlbumNames,
                        onDismiss = { showMetadataDialog = false }
                    )
                }

                var isZoomedIn by remember { mutableStateOf(false) }
                var mediaOrigin by remember { mutableStateOf<Offset?>(null) }
                val thumbnailCornerPx = MaterialTheme.shapes.large.topStart.toPx(Size.Zero, density)

                LaunchedEffect(currentAsset.id) {
                    dismissOffsetY = 0f
                    isDraggingDismiss = false
                    boundaryDragX = 0f
                    isDraggingBoundary = false
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        // Outside the sliding layer, so the finger's positions don't shift as the photo follows it.
                        .pointerInput(currentAsset.id, isZoomedIn) {
                            if (isZoomedIn) return@pointerInput
                            awaitEachGesture {
                                val down = awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)
                                // Still sliding to a neighbouring cluster from the last swipe.
                                if (boundaryDirection != 0 || isClosing) return@awaitEachGesture
                                val startY = down.position.y
                                val controlsHeightPx = with(density) { 70.dp.toPx() }
                                val isTouchInControls = currentAsset.type == "VIDEO" && playerControlsVisible &&
                                    startY > (size.height - controlsHeightPx)

                                if (isTouchInControls) {
                                    // Touch started in video controls bar (Play/Pause, Seekbar, Mute). Let child controls handle it.
                                    return@awaitEachGesture
                                }

                                var isDraggingHorizontal = false
                                var isDraggingVertical = false
                                var totalDragX = 0f
                                var totalDragY = 0f
                                val touchSlop = viewConfiguration.touchSlop
                                val velocityTracker = VelocityTracker()

                                do {
                                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                    if (event.changes.size == 1) {
                                        val change = event.changes[0]
                                        velocityTracker.addPointerInputChange(change)
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
                                            boundaryDirection = if (pullingNext) 1 else if (pullingPrevious) -1 else 0
                                            change.consume()
                                        } else if (isDraggingVertical) {
                                            dismissOffsetY = (totalDragY - touchSlop).coerceAtLeast(0f)
                                            change.consume()
                                        }
                                    }
                                } while (event.changes.any { it.pressed })

                                if (isDraggingVertical) {
                                    if (dismissOffsetY > 200f) {
                                        close()
                                    } else {
                                        isDraggingDismiss = false
                                        dismissOffsetY = 0f
                                    }
                                } else if (isDraggingBoundary) {
                                    val releaseX = boundaryDragX
                                    val velocityX = velocityTracker.calculateVelocity().x
                                    val widthPx = size.width.toFloat()
                                    scope.launch {
                                        boundarySettle.snapTo(releaseX)
                                        isDraggingBoundary = false
                                        boundaryDragX = 0f
                                        if (abs(releaseX) > 100f) {
                                            val toNext = releaseX < 0f
                                            boundarySettle.animateTo(
                                                targetValue = if (toNext) -widthPx else widthPx,
                                                animationSpec = motionScheme.defaultSpatialSpec<Float>().settlingWithinPixel(),
                                                initialVelocity = velocityX
                                            )
                                            // The peek already brought the new cluster in, so skip the button slide.
                                            val target = currentClusterIdx + if (toNext) 1 else -1
                                            lastClusterIdx = target
                                            enterClusterAtLast = !toNext
                                            currentClusterIdx = target
                                        } else {
                                            boundarySettle.animateTo(0f, motionScheme.fastSpatialSpec<Float>().settlingWithinPixel(), velocityX)
                                        }
                                        boundaryDirection = 0
                                        boundarySettle.snapTo(0f)
                                    }
                                } else if (isDraggingHorizontal) {
                                    if (totalDragX < -100f && selectedIndex < assets.size - 1) {
                                        selectedIndex++
                                    } else if (totalDragX > 100f && selectedIndex > 0) {
                                        selectedIndex--
                                    }
                                }
                            }
                        }
                ) {
                    // The neighbouring cluster's entry photo, sliding in from the side as the current one leaves.
                    val peekAsset = when (boundaryDirection) {
                        1 -> clusters.getOrNull(currentClusterIdx + 1)?.assets?.firstOrNull()
                        -1 -> clusters.getOrNull(currentClusterIdx - 1)?.assets?.lastOrNull()
                        else -> null
                    }
                    if (peekAsset != null) {
                        val peekRequest = remember(peekAsset.id, baseUrlClean) {
                            ImageRequest.Builder(context)
                                .data("$baseUrlClean/api/assets/${peekAsset.id}/thumbnail?format=WEBP&size=preview")
                                .crossfade(false)
                                .build()
                        }
                        AsyncImage(
                            model = peekRequest,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    val dragX = if (isDraggingBoundary) boundaryDragX else boundarySettle.value
                                    val progress = (abs(dragX) / size.width).coerceIn(0f, 1f)
                                    translationX = dragX + boundaryDirection * size.width
                                    val scale = 0.9f + 0.1f * progress
                                    scaleX = scale
                                    scaleY = scale
                                    alpha = progress
                                }
                        )
                    }

                    // Lets ZoomableBox keep panning within the photo itself; null (box bounds) when EXIF has no size.
                    val zoomAspectRatio = currentAsset.exifInfo
                        ?.takeIf { (it.imageWidth ?: 0) > 0 && (it.imageHeight ?: 0) > 0 }
                        ?.displayAspectRatio

                    val swipeModifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { mediaOrigin = it.positionInWindow() }
                        // The container transform: the photo's visible rect moves between its card thumbnail, cropped like
                        // the card, and its fitted place here, shrinking as it follows a drag down.
                        .graphicsLayer {
                            val origin = mediaOrigin
                            if (origin == null) {
                                alpha = 0f
                                return@graphicsLayer
                            }
                            val fit = fitRect(size, zoomAspectRatio)
                            val drag = dismissDrag()
                            val dragScale = 1f - 0.25f * (drag / size.height).coerceIn(0f, 1f)
                            val open = rectAround(fit.center + Offset(0f, drag), fit.width * dragScale, fit.height * dragScale)
                            val p = presence.value
                            val thumb = thumbnailRect?.translate(-origin.x, -origin.y)
                            val visible = lerp(thumb ?: rectAround(fit.center, fit.width * 0.8f, fit.height * 0.8f), open, p)
                            val scale = max(visible.width / fit.width, visible.height / fit.height)
                            transformOrigin = TransformOrigin(fit.center.x / size.width, fit.center.y / size.height)
                            scaleX = scale
                            scaleY = scale
                            translationX = visible.center.x - fit.center.x
                            translationY = visible.center.y - fit.center.y
                            shape = RoundRectClip(
                                rectAround(fit.center, visible.width / scale, visible.height / scale),
                                thumbnailCornerPx * (1f - p).coerceIn(0f, 1f) / scale
                            )
                            // Open and at rest, a zoomed photo may spill past its fitted rect.
                            clip = p != 1f || drag != 0f
                            // Crossfades with the thumbnail, which has its own frame, badges and tint: quickly on the way out of
                            // it, and over the second half of the flight back, so the photo dissolves into it as it lands.
                            val fadeRange = if (isClosing) 0.5f else 0.15f
                            alpha = if (thumb == null) p.coerceIn(0f, 1f) else (p / fadeRange).coerceIn(0f, 1f)
                        }
                        .graphicsLayer {
                            val dragX = if (isDraggingBoundary) boundaryDragX else boundarySettle.value
                            val progress = (abs(dragX) / size.width).coerceIn(0f, 1f)
                            translationX = dragX + clusterSlide.value * size.width * 0.25f
                            val scale = 1f - 0.1f * progress
                            scaleX = scale
                            scaleY = scale
                            alpha = (1f - abs(clusterSlide.value)) * (1f - progress)
                        }

                    if (currentAsset.type == "VIDEO") {
                        DuplicateVideoPlayer(
                            asset = currentAsset,
                            baseUrl = baseUrlClean,
                            handleAudioFocus = handleAudioFocus,
                            modifier = swipeModifier,
                            contentScale = ContentScale.Fit,
                            tapToPause = false,
                            controlsVisible = playerControlsVisible,
                            onIsPlayingChanged = { isVideoPlaying = it },
                            muted = isMuted,
                            onMutedChange = { isMuted = it },
                            videoSurfaceWrapper = { videoSurface ->
                                ZoomableBox(
                                    modifier = Modifier.fillMaxSize(),
                                    resetOnRelease = false,
                                    aspectRatio = zoomAspectRatio,
                                    doubleTapScale = DoubleTapZoom,
                                    onTap = { _, _ -> playerControlsVisible = !playerControlsVisible },
                                    onIsZoomedChanged = { zoomed ->
                                        isZoomedIn = zoomed
                                    }
                                ) {
                                    videoSurface()
                                }
                            }
                        )
                    } else {
                        var isImageLoading by remember(currentAsset.id) { mutableStateOf(true) }
                        var isOriginalShown by remember(currentAsset.id) { mutableStateOf(false) }

                        Box(modifier = swipeModifier) {
                            ZoomableBox(
                                modifier = Modifier.fillMaxSize(),
                                resetOnRelease = false,
                                aspectRatio = zoomAspectRatio,
                                doubleTapScale = DoubleTapZoom,
                                onIsZoomedChanged = { zoomed ->
                                    isZoomedIn = zoomed
                                }
                            ) {
                                // Usually cached from the card and the carousel, so the photo appears at once while its original loads.
                                if (!isOriginalShown && !currentAsset.isGif) {
                                    val previewRequest = remember(currentAsset.id, baseUrlClean) {
                                        ImageRequest.Builder(context)
                                            .data("$baseUrlClean/api/assets/${currentAsset.id}/thumbnail?format=WEBP&size=preview")
                                            .crossfade(false)
                                            .build()
                                    }
                                    AsyncImage(
                                        model = previewRequest,
                                        contentDescription = null,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
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
                                        isOriginalShown = state is coil3.compose.AsyncImagePainter.State.Success
                                    }
                                )
                            }

                            if (isImageLoading) {
                                // The preview stays visible underneath, so keep this small and see-through, as in the swipe fullscreen.
                                MediaLoadingIndicator(
                                    modifier = Modifier.align(Alignment.Center),
                                    size = 32.dp,
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { alpha = chromeAlpha() }
                        .background(Color.Black.copy(alpha = 0.6f))
                        .navigationBarsPadding()
                        .padding(top = 8.dp, bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // The current photo's decision, and the only way to change it here; tapping the selected one clears it.
                    val toolbarContentColor = FloatingToolbarDefaults.vibrantFloatingToolbarColors().toolbarContentColor
                    Row(
                        modifier = Modifier.padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            Triple(DuplicateDecision.DELETE, Icons.Default.Delete, R.string.swipe_delete_upper),
                            Triple(DuplicateDecision.KEEP, Icons.Default.Check, R.string.swipe_keep_upper)
                        ).forEachIndexed { index, (decision, icon, label) ->
                            val checked = currentDecision == decision
                            DecisionToggleButton(
                                checked = checked,
                                onClick = {
                                    currentOnDecisionChange(currentAsset.id, if (checked) DuplicateDecision.NONE else decision)
                                },
                                index = index,
                                icon = icon,
                                iconAtEnd = decision == DuplicateDecision.DELETE,
                                label = stringResource(label),
                                checkedColor = decisionColor(decision),
                                contentColor = toolbarContentColor
                            )
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
                                    size = ThumbBadgeSize,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(ThumbBadgeInset)
                                )

                                // Favourite and video share one pill on the same line as the decision badge.
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

                    Spacer(modifier = Modifier.height(12.dp))

                    // Previous group, position, next group as one connected group, like the swipe fullscreen toolbar.
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
                            colors = buttonColors,
                            modifier = Modifier.size(width = ControlTileWidth, height = ControlTileHeight)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.NavigateBefore,
                                contentDescription = stringResource(R.string.cd_previous_group)
                            )
                        }

                        Surface(
                            shape = connectedTileShape(1, 3),
                            color = quietTileColor(),
                            contentColor = quietContentColor(),
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
                            colors = buttonColors,
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

/** One half of the delete/keep pair: just the label at rest, the icon slides out on its outer side once it's selected. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DecisionToggleButton(
    checked: Boolean,
    onClick: () -> Unit,
    index: Int,
    icon: ImageVector,
    iconAtEnd: Boolean,
    label: String,
    checkedColor: Color,
    contentColor: Color
) {
    val motionScheme = MaterialTheme.motionScheme
    ToggleButton(
        checked = checked,
        onCheckedChange = { onClick() },
        shapes = ToggleButtonShapes(
            shape = connectedTileShape(index, 2, outer = ControlTileHeight / 2),
            pressedShape = RoundedCornerShape(10.dp),
            checkedShape = CircleShape
        ),
        colors = ToggleButtonDefaults.colors(
            containerColor = previewTileColor(),
            contentColor = contentColor,
            checkedContainerColor = checkedColor,
            checkedContentColor = Color.White
        ),
        modifier = Modifier.height(ControlTileHeight),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        val slidingIcon: @Composable () -> Unit = {
            // Grows outward from the label's side.
            val anchor = if (iconAtEnd) Alignment.Start else Alignment.End
            AnimatedVisibility(
                visible = checked,
                enter = expandHorizontally(motionScheme.defaultSpatialSpec(), expandFrom = anchor) +
                    fadeIn(motionScheme.defaultEffectsSpec()),
                exit = shrinkHorizontally(motionScheme.defaultSpatialSpec(), shrinkTowards = anchor) +
                    fadeOut(motionScheme.fastEffectsSpec())
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val spacer = @Composable { Spacer(Modifier.width(ButtonDefaults.iconSpacingFor(ControlTileHeight))) }
                    if (iconAtEnd) spacer()
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.iconSizeFor(ControlTileHeight))
                    )
                    if (!iconAtEnd) spacer()
                }
            }
        }
        if (!iconAtEnd) slidingIcon()
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        if (iconAtEnd) slidingIcon()
    }
}

