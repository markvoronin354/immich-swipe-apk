package com.markvoronin.immichswipe.feature.swipe.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Precision
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.domain.model.Album
import kotlin.math.abs

/** Id of the strip's last tile: dropping there opens the full album picker rather than adding to an album. */
const val AlbumDropMoreId = "more"

private val TileWidth = 72.dp
private val TileGap = 8.dp
private val TileRowPadding = 16.dp

// How near a row's edge the finger starts scrolling it, and the fastest it scrolls.
private val AutoScrollEdge = 64.dp
private val AutoScrollSpeed = 600.dp

/** Where the finger is while a card is dragged over the album strip, and which tile it is over. */
@Stable
class AlbumDropState {
    var isActive by mutableStateOf(false)
        private set
    private var pointer by mutableStateOf<Offset?>(null)
    internal val tileRects = mutableStateMapOf<String, Rect>()
    internal val pointerOnScreen: Offset? get() = pointer

    /** Where the scrolling part of the strip is on screen. */
    internal var scrollBounds: Rect? = null

    /** The swipe deck's top-left on screen; the card reports its pointer relative to this. */
    var deckOrigin: Offset = Offset.Zero

    /** The tile under the finger, once it is down in the strip or just above it. */
    val hoveredId: String? by derivedStateOf {
        val p = pointer ?: return@derivedStateOf null
        val rects = tileRects.values
        if (!isActive || rects.isEmpty()) return@derivedStateOf null
        val reachTop = rects.minOf { it.top } - rects.first().height
        if (p.y < reachTop) return@derivedStateOf null
        tileRects.minByOrNull { abs(p.x - it.value.center.x) }?.key
    }

    fun move(cardLocal: Offset) {
        isActive = true
        pointer = deckOrigin + cardLocal
    }

    fun end() {
        isActive = false
        pointer = null
    }
}

/** -1 to 1: which way, and how fast, the strip should scroll for a finger at [pointer]; 0 away from its edges. */
internal fun autoScrollDirection(pointer: Offset, bounds: Rect, edge: Float): Float {
    if (pointer.y < bounds.top - bounds.height) return 0f
    return when {
        pointer.x < bounds.left + edge -> -(1f - ((pointer.x - bounds.left) / edge).coerceIn(0f, 1f))
        // Past the row's right edge is the More tile, which has to be reachable without scrolling.
        pointer.x in (bounds.right - edge)..bounds.right -> (pointer.x - (bounds.right - edge)) / edge
        else -> 0f
    }
}

private fun Modifier.edgeFades(listState: LazyListState, color: Color, width: Dp) = drawWithContent {
    drawContent()
    val w = width.toPx()
    if (listState.canScrollBackward) {
        drawRect(Brush.horizontalGradient(listOf(color, Color.Transparent), startX = 0f, endX = w), size = Size(w, size.height))
    }
    if (listState.canScrollForward) {
        drawRect(
            Brush.horizontalGradient(listOf(Color.Transparent, color), startX = size.width - w, endX = size.width),
            topLeft = Offset(size.width - w, 0f),
            size = Size(w, size.height)
        )
    }
}

/** The toast text for the result of adding a photo to an album. */
@Composable
fun rememberAddToAlbumMessage(): (success: Boolean, albumName: String, missingPermission: String?) -> String {
    val successTemplate = stringResource(R.string.add_to_album_success)
    val errorTemplate = stringResource(R.string.add_to_album_error)
    val permissionErrorTemplate = stringResource(R.string.add_to_album_permission_error)
    return { success, albumName, missingPermission ->
        when {
            success -> successTemplate.format(albumName)
            missingPermission != null -> permissionErrorTemplate.format(albumName, missingPermission)
            else -> errorTemplate.format(albumName)
        }
    }
}

/**
 * Albums to drop the dragged card on, rising over the toolbar while a card is pulled down.
 * [showMore] adds the tile that opens the full album picker, for when the toolbar has no album button to do so.
 */
@Composable
fun AlbumDropStrip(
    state: AlbumDropState,
    albums: List<Album>,
    baseUrl: String,
    showMore: Boolean,
    modifier: Modifier = Modifier,
) {
    val motionScheme = MaterialTheme.motionScheme
    val haptic = LocalHapticFeedback.current
    val hovered = state.hoveredId
    LaunchedEffect(hovered) {
        if (hovered != null) haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
    }
    LaunchedEffect(state.isActive) {
        if (!state.isActive) state.tileRects.clear()
    }

    val listState = rememberLazyListState()
    val density = LocalDensity.current
    // The albums reload as a drag starts, and the one just used moves to the front. The row keeps the album it showed
    // in view by key, so without this the moved one would sit off to the left.
    val albumOrder = remember(albums) { albums.map { it.id } }
    LaunchedEffect(albumOrder) {
        if (state.isActive) listState.scrollToItem(0)
    }
    // Every drag starts at the recent albums; a finger near either edge of the row scrolls it.
    LaunchedEffect(state.isActive) {
        if (!state.isActive) return@LaunchedEffect
        listState.scrollToItem(0)
        val edge = with(density) { AutoScrollEdge.toPx() }
        val speed = with(density) { AutoScrollSpeed.toPx() }
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val seconds = (now - last) / 1_000_000_000f
            last = now
            val pointer = state.pointerOnScreen
            val bounds = state.scrollBounds
            if (pointer != null && bounds != null) {
                val direction = autoScrollDirection(pointer, bounds, edge)
                if (direction != 0f) listState.scrollBy(direction * speed * seconds)
            }
        }
    }

    AnimatedVisibility(
        visible = state.isActive,
        enter = slideInVertically(motionScheme.defaultSpatialSpec()) { it } + fadeIn(motionScheme.defaultEffectsSpec()),
        exit = slideOutVertically(motionScheme.fastSpatialSpec()) { it } + fadeOut(motionScheme.fastEffectsSpec()),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            val fadeColor = MaterialTheme.colorScheme.surfaceContainerHigh
            Row {
                LazyRow(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .onGloballyPositioned { state.scrollBounds = Rect(it.positionOnScreen(), it.size.toSize()) }
                        .edgeFades(listState, fadeColor, 24.dp),
                    contentPadding = PaddingValues(
                        start = TileRowPadding,
                        end = if (showMore) 0.dp else TileRowPadding,
                        top = 12.dp,
                        bottom = 12.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(TileGap),
                    userScrollEnabled = false
                ) {
                    items(albums, key = { it.id }) { album ->
                        DisposableEffect(Unit) { onDispose { state.tileRects.remove(album.id) } }
                        DropTile(
                            label = album.albumName,
                            count = album.assetCount,
                            selected = hovered == album.id,
                            onPositioned = { state.tileRects[album.id] = it }
                        ) {
                            AlbumCover(album, baseUrl)
                        }
                    }
                }
                if (showMore) {
                    // Pinned past the end of the row, so it stays in reach however far the albums scroll.
                    Box(Modifier.padding(start = TileGap, end = TileRowPadding, top = 12.dp, bottom = 12.dp)) {
                        DisposableEffect(Unit) { onDispose { state.tileRects.remove(AlbumDropMoreId) } }
                        DropTile(
                            label = stringResource(R.string.swipe_album_more),
                            count = null,
                            selected = hovered == AlbumDropMoreId,
                            onPositioned = { state.tileRects[AlbumDropMoreId] = it }
                        ) {
                            Icon(
                                imageVector = Icons.Default.LibraryAdd,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(28.dp).align(Alignment.Center)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DropTile(
    label: String,
    count: Int?,
    selected: Boolean,
    onPositioned: (Rect) -> Unit,
    cover: @Composable BoxScope.() -> Unit,
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.15f else 1f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "DropTileScale"
    )
    val accent = MaterialTheme.colorScheme.primary
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(TileWidth)
            // Measured before the scale, so a tile growing under the finger can't push itself away from it.
            .onGloballyPositioned { onPositioned(Rect(it.positionOnScreen(), it.size.toSize())) }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .border(2.dp, if (selected) accent else MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.large),
            content = cover
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) accent else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
        )
        if (count != null) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BoxScope.AlbumCover(album: Album, baseUrl: String) {
    val context = LocalContext.current
    val thumbnailId = album.albumThumbnailAssetId
    val base = baseUrl.removeSuffix("/")
    if (thumbnailId != null && base.isNotEmpty()) {
        val request = remember(thumbnailId, base) {
            ImageRequest.Builder(context)
                .data("$base/api/assets/$thumbnailId/thumbnail?format=WEBP&size=thumbnail")
                .crossfade(true)
                .precision(Precision.INEXACT)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build()
        }
        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    } else {
        Icon(
            imageVector = Icons.Default.PhotoLibrary,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(28.dp).align(Alignment.Center)
        )
    }
}
