package com.markvoronin.immichswipe.feature.swipe.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.LibraryAddCheck
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.core.SortOrder
import com.markvoronin.immichswipe.core.ToolbarButton
import com.markvoronin.immichswipe.feature.common.FavoriteIcon
import com.markvoronin.immichswipe.feature.common.LockIcon
import com.markvoronin.immichswipe.feature.common.bounce
import com.markvoronin.immichswipe.feature.swipe.SwipeDecision
import com.markvoronin.immichswipe.feature.swipe.SwipeUiState
import com.markvoronin.immichswipe.feature.swipe.SwipeViewModel
import com.markvoronin.immichswipe.feature.swipe.utils.MaterialGreen
import com.markvoronin.immichswipe.feature.swipe.utils.MaterialRed
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * One tile of the connected action group, styled like the fullscreen toolbar. When [checked] is non-null it is a
 * toggle whose tile inverts while on; it is also used to mark a button whose menu or dialog is open.
 * [label] is shown as small text under the icon when [showLabel] is true.
 */
@Composable
private fun ActionTile(
    onClick: () -> Unit,
    shape: Shape,
    label: String,
    showLabel: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    checked: Boolean? = null,
    icon: @Composable () -> Unit
) {
    val pressedShape = RoundedCornerShape(10.dp)
    val height by animateDpAsState(
        targetValue = if (showLabel) 56.dp else 48.dp,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "tileHeight"
    )
    val tileModifier = modifier.height(height)
    val content: @Composable () -> Unit = {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            icon()
            AnimatedVisibility(
                visible = showLabel,
                enter = fadeIn(MaterialTheme.motionScheme.slowEffectsSpec()) + expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
                exit = fadeOut(MaterialTheme.motionScheme.slowEffectsSpec()) + shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec())
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp, end = 4.dp)
                )
            }
        }
    }
    if (checked == null) {
        FilledTonalIconButton(
            onClick = onClick,
            shapes = IconButtonDefaults.shapes(shape = shape, pressedShape = pressedShape),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            enabled = enabled,
            modifier = tileModifier,
            content = content
        )
    } else {
        FilledTonalIconToggleButton(
            checked = checked,
            onCheckedChange = { onClick() },
            shapes = IconButtonDefaults.toggleableShapes(shape = shape, pressedShape = pressedShape, checkedShape = CircleShape),
            colors = IconButtonDefaults.filledTonalIconToggleButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                checkedContentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ),
            enabled = enabled,
            modifier = tileModifier,
            content = content
        )
    }
}

@Composable
private fun animatedTileShape(index: Int, count: Int): Shape {
    val spec = MaterialTheme.motionScheme.defaultSpatialSpec<Dp>()
    val start by animateDpAsState(if (index == 0) 24.dp else 6.dp, spec, label = "tileStart")
    val end by animateDpAsState(if (index == count - 1) 24.dp else 6.dp, spec, label = "tileEnd")
    return RoundedCornerShape(topStart = start, bottomStart = start, topEnd = end, bottomEnd = end)
}

/** Weight multiplier that widens a tile while it is checked, as in an M3 Expressive button group. */
@Composable
internal fun checkedWidthFactor(checked: Boolean): Float {
    val factor by animateFloatAsState(
        targetValue = if (checked) 1.2f else 1f,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "checkedWidth"
    )
    return factor
}

@Composable
private fun RowScope.AnimatedTileSlot(
    visible: Boolean,
    weight: Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "tileVisibility"
    )
    if (progress > 0f) {
        Spacer(Modifier.width(3.dp * progress))
        Box(
            modifier = Modifier
                .weight(weight * progress)
                .then(modifier)
                .graphicsLayer {
                    alpha = progress.coerceIn(0f, 1f)
                    clip = true
                }
        ) {
            content()
        }
    }
}

@Composable
fun SwipeActionBarLayout(
    showSwipeButtons: Boolean,
    showArchiveButton: Boolean,
    showFavoriteButton: Boolean,
    showAddToAlbumButton: Boolean,
    showLockButton: Boolean,
    sortOrder: SortOrder,
    modifier: Modifier = Modifier,
    buttonOrder: List<ToolbarButton> = ToolbarButton.entries,
    // When set, the bar is a preview: taps are blocked and the middle buttons can be long-press dragged to reorder.
    onReorder: ((List<ToolbarButton>) -> Unit)? = null,
    showButtonLabels: Boolean = true,
    undoEnabled: Boolean = true,
    isFavorite: Boolean = false,
    // Bumped on every favourite toggle; favoriteToggledOn says whether that toggle added the favourite.
    favoriteToggleCount: Int = 0,
    favoriteToggledOn: Boolean = false,
    isAlbumDialogOpen: Boolean = false,
    albumAddedCount: Int = 0,
    isSortMenuOpen: Boolean = false,
    isBulkDeleteMode: Boolean = false,
    isBulkKeepMode: Boolean = false,
    onDelete: () -> Unit = {},
    onKeep: () -> Unit = {},
    onUndo: () -> Unit = {},
    onArchive: () -> Unit = {},
    onFavorite: () -> Unit = {},
    onAddToAlbum: () -> Unit = {},
    onLock: () -> Unit = {},
    onSort: () -> Unit = {},
    deleteButtonModifier: Modifier = Modifier,
    keepButtonModifier: Modifier = Modifier,
    sortMenu: @Composable () -> Unit = {}
) {
    val horizontalPadding by animateDpAsState(
        targetValue = if (showSwipeButtons) 16.dp else 24.dp,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "barPadding"
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedVisibility(
            visible = showSwipeButtons,
            // No fade and no clip: either one makes the FAB's shadow render as a grey square while animating.
            // Scaling from the outer edge keeps the button inside the space the expand reserves.
            enter = expandHorizontally(MaterialTheme.motionScheme.defaultSpatialSpec(), expandFrom = Alignment.Start, clip = false) +
                scaleIn(MaterialTheme.motionScheme.defaultSpatialSpec(), initialScale = 0f, transformOrigin = TransformOrigin(0f, 0.5f)),
            exit = shrinkHorizontally(MaterialTheme.motionScheme.defaultSpatialSpec(), shrinkTowards = Alignment.Start, clip = false) +
                scaleOut(MaterialTheme.motionScheme.defaultSpatialSpec(), targetScale = 0f, transformOrigin = TransformOrigin(0f, 0.5f))
        ) {
            Box(Modifier.padding(end = 12.dp)) {
                FloatingActionButton(
                    onClick = onDelete,
                    containerColor = if (isBulkDeleteMode) MaterialRed else MaterialTheme.colorScheme.errorContainer,
                    contentColor = if (isBulkDeleteMode) Color.White else MaterialTheme.colorScheme.onErrorContainer,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(48.dp)
                        .then(deleteButtonModifier)
                ) {
                    Icon(
                        imageVector = if (isBulkDeleteMode) Icons.Default.DeleteSweep else Icons.Default.Delete,
                        contentDescription = stringResource(R.string.swipe_delete)
                    )
                }
                if (onReorder != null) Box(Modifier.matchParentSize().blockTouches())
            }
        }

        val iconSize = 24.dp
        val tileCount = 2 + listOf(
            showArchiveButton,
            showFavoriteButton,
            showAddToAlbumButton,
            showLockButton
        ).count { it }

        val labels = listOfNotNull(
            stringResource(R.string.swipe_undo),
            stringResource(R.string.swipe_archive).takeIf { showArchiveButton },
            stringResource(R.string.swipe_label_favorite).takeIf { showFavoriteButton },
            stringResource(R.string.swipe_label_album).takeIf { showAddToAlbumButton },
            stringResource(R.string.swipe_locked).takeIf { showLockButton },
            stringResource(R.string.swipe_label_sort)
        )
        val labelStyle = MaterialTheme.typography.labelSmall
        val textMeasurer = rememberTextMeasurer()
        val density = LocalDensity.current
        val widestLabel = remember(labels, labelStyle, density) {
            with(density) { labels.maxOf { textMeasurer.measure(it, labelStyle).size.width }.toDp() }
        }

        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            val tileWidth = (maxWidth - 3.dp * (tileCount - 1)) / tileCount
            val showLabels = showButtonLabels && widestLabel + 16.dp <= tileWidth

            // Reorder state. localOrder holds the order while dragging and until the saved order catches up.
            val haptic = LocalHapticFeedback.current
            val scope = rememberCoroutineScope()
            val spatialSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
            val gapPx = with(density) { 3.dp.toPx() }
            var localOrder by remember { mutableStateOf<List<ToolbarButton>?>(null) }
            var dragged by remember { mutableStateOf<ToolbarButton?>(null) }
            var dragOffset by remember { mutableFloatStateOf(0f) }
            // Extra horizontal offset per tile, animated to 0 so tiles glide into their new slot after a swap or drop.
            val shifts = remember { ToolbarButton.entries.associateWith { Animatable(0f) } }
            LaunchedEffect(buttonOrder) { if (dragged == null) localOrder = null }
            val order = localOrder ?: buttonOrder
            val shown = buildSet {
                if (showArchiveButton) add(ToolbarButton.ARCHIVE)
                if (showFavoriteButton) add(ToolbarButton.FAVORITE)
                if (showAddToAlbumButton) add(ToolbarButton.ALBUM)
                if (showLockButton) add(ToolbarButton.LOCK)
            }
            val currentShown by rememberUpdatedState(shown)
            val currentOrder by rememberUpdatedState(order)
            val currentButtonOrder by rememberUpdatedState(buttonOrder)
            val currentTileCount by rememberUpdatedState(tileCount)
            val currentOnReorder by rememberUpdatedState(onReorder)

            fun glide(button: ToolbarButton, from: Float) {
                scope.launch(start = CoroutineStart.UNDISPATCHED) {
                    shifts.getValue(button).snapTo(from)
                    shifts.getValue(button).animateTo(0f, spatialSpec)
                }
            }

            fun drop() {
                val button = dragged ?: return
                glide(button, dragOffset)
                dragged = null
                dragOffset = 0f
                val final = localOrder
                if (final != null && final != currentButtonOrder) currentOnReorder?.invoke(final) else localOrder = null
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                var index = 0

                val undoIndex = index++
                // Same as the fullscreen undo: the arrow winds back a full turn, against the way it points in RTL too.
                val undoScale = remember { Animatable(1f) }
                val undoSpin = remember { Animatable(0f) }
                val undoDirection = if (LocalLayoutDirection.current == LayoutDirection.Rtl) 1f else -1f
                val motionScheme = MaterialTheme.motionScheme
                ActionTile(
                    onClick = {
                        scope.bounce(undoScale, motionScheme)
                        scope.launch {
                            undoSpin.snapTo(0f)
                            undoSpin.animateTo(360f, motionScheme.defaultSpatialSpec())
                        }
                        onUndo()
                    },
                    label = stringResource(R.string.swipe_undo),
                    showLabel = showLabels,
                    enabled = undoEnabled,
                    shape = animatedTileShape(undoIndex, tileCount),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = stringResource(R.string.swipe_undo),
                        modifier = Modifier
                            .size(iconSize)
                            .graphicsLayer {
                                scaleX = undoScale.value
                                scaleY = undoScale.value
                                rotationZ = undoSpin.value * undoDirection
                            }
                    )
                }

                order.forEach { button ->
                    key(button) {
                        val tileIndex = if (button in shown) index++ else -1
                        val scale by animateFloatAsState(
                            targetValue = if (dragged == button) 1.08f else 1f,
                            animationSpec = spatialSpec,
                            label = "dragScale"
                        )
                        val widthFactor = checkedWidthFactor(
                            (button == ToolbarButton.FAVORITE && isFavorite) || (button == ToolbarButton.ALBUM && isAlbumDialogOpen)
                        )
                        AnimatedTileSlot(
                            visible = button in shown,
                            weight = if (tileIndex >= 0) widthFactor else 1f,
                            modifier = Modifier
                                .zIndex(if (dragged == button) 1f else 0f)
                                .graphicsLayer {
                                    translationX = shifts.getValue(button).value + if (dragged == button) dragOffset else 0f
                                    scaleX = scale
                                    scaleY = scale
                                }
                        ) {
                            when (button) {
                                ToolbarButton.ARCHIVE -> {
                                    // Squashes and dips, as if something dropped into the box.
                                    val archiveScale = remember { Animatable(1f) }
                                    val archiveDrop = remember { Animatable(0f) }
                                    ActionTile(
                                        onClick = {
                                            scope.bounce(archiveScale, motionScheme)
                                            scope.launch {
                                                archiveDrop.animateTo(1f, motionScheme.fastSpatialSpec())
                                                archiveDrop.animateTo(0f, motionScheme.defaultSpatialSpec())
                                            }
                                            onArchive()
                                        },
                                        label = stringResource(R.string.swipe_archive),
                                        showLabel = showLabels,
                                        shape = animatedTileShape(tileIndex, tileCount),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Archive,
                                            contentDescription = stringResource(R.string.swipe_archive),
                                            modifier = Modifier
                                                .size(iconSize)
                                                .graphicsLayer {
                                                    scaleX = archiveScale.value
                                                    scaleY = archiveScale.value
                                                    translationY = archiveDrop.value * 4.dp.toPx()
                                                }
                                        )
                                    }
                                }

                                ToolbarButton.FAVORITE -> {
                                    val favoriteScale = remember { Animatable(1f) }
                                    val favoriteBurst = remember { Animatable(1f) }
                                    // Driven by toggles from anywhere (this tile, a double-tap on the card), not by isFavorite,
                                    // so moving to an already-favourite asset doesn't celebrate. Starts at the current count so
                                    // the tile reappearing doesn't replay the last toggle.
                                    var handledToggles by remember { mutableIntStateOf(favoriteToggleCount) }
                                    LaunchedEffect(favoriteToggleCount) {
                                        if (favoriteToggleCount == handledToggles) return@LaunchedEffect
                                        handledToggles = favoriteToggleCount
                                        bounce(favoriteScale, motionScheme)
                                        if (favoriteToggledOn) launch {
                                            favoriteBurst.snapTo(0f)
                                            favoriteBurst.animateTo(1f, motionScheme.slowSpatialSpec())
                                        }
                                    }
                                    ActionTile(
                                        onClick = onFavorite,
                                        label = stringResource(R.string.swipe_label_favorite),
                                        showLabel = showLabels,
                                        shape = animatedTileShape(tileIndex, tileCount),
                                        checked = isFavorite,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        CompositionLocalProvider(
                                            LocalContentColor provides if (isFavorite) Color.Red else LocalContentColor.current
                                        ) {
                                            FavoriteIcon(
                                                isFavorite = isFavorite,
                                                scale = { favoriteScale.value },
                                                burst = { favoriteBurst.value },
                                                contentDescription = stringResource(R.string.swipe_favorite)
                                            )
                                        }
                                    }
                                }

                                ToolbarButton.ALBUM -> {
                                    // Turns into a check for a moment after an add succeeds. Starts at the current count so
                                    // the tile reappearing doesn't replay the last add.
                                    val albumScale = remember { Animatable(1f) }
                                    var showAdded by remember { mutableStateOf(false) }
                                    var handledAdds by remember { mutableIntStateOf(albumAddedCount) }
                                    LaunchedEffect(albumAddedCount) {
                                        if (albumAddedCount == handledAdds) return@LaunchedEffect
                                        handledAdds = albumAddedCount
                                        showAdded = true
                                        bounce(albumScale, motionScheme)
                                        delay(1000.milliseconds)
                                        showAdded = false
                                    }
                                    ActionTile(
                                        onClick = onAddToAlbum,
                                        label = stringResource(R.string.swipe_label_album),
                                        showLabel = showLabels,
                                        shape = animatedTileShape(tileIndex, tileCount),
                                        checked = isAlbumDialogOpen,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        AnimatedContent(
                                            targetState = showAdded,
                                            transitionSpec = { iconSwap(motionScheme) },
                                            label = "albumIcon"
                                        ) { added ->
                                            Icon(
                                                imageVector = if (added) Icons.Default.LibraryAddCheck else Icons.Default.LibraryAdd,
                                                contentDescription = stringResource(R.string.add_to_album_title),
                                                modifier = Modifier
                                                    .size(iconSize)
                                                    .graphicsLayer {
                                                        scaleX = albumScale.value
                                                        scaleY = albumScale.value
                                                    }
                                            )
                                        }
                                    }
                                }

                                ToolbarButton.LOCK -> {
                                    // The shackle pops open and clunks back shut.
                                    val lockScale = remember { Animatable(1f) }
                                    val lockLift = remember { Animatable(0f) }
                                    ActionTile(
                                        onClick = {
                                            scope.bounce(lockScale, motionScheme)
                                            scope.launch {
                                                lockLift.animateTo(1f, motionScheme.fastSpatialSpec())
                                                lockLift.animateTo(0f, motionScheme.fastSpatialSpec())
                                            }
                                            onLock()
                                        },
                                        label = stringResource(R.string.swipe_locked),
                                        showLabel = showLabels,
                                        shape = animatedTileShape(tileIndex, tileCount),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        LockIcon(
                                            lift = { lockLift.value },
                                            scale = { lockScale.value },
                                            contentDescription = stringResource(R.string.swipe_locked)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                val sortIndex = index
                Spacer(Modifier.width(3.dp))
                Box(modifier = Modifier.weight(checkedWidthFactor(isSortMenuOpen))) {
                    ActionTile(
                        onClick = onSort,
                        label = stringResource(R.string.swipe_label_sort),
                        showLabel = showLabels,
                        shape = animatedTileShape(sortIndex, tileCount),
                        checked = isSortMenuOpen,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Ascending is the descending icon turned upside down.
                        val icon = when (sortOrder) {
                            SortOrder.SHUFFLED -> Icons.Default.Shuffle
                            SortOrder.CHRONOLOGICAL_ASC, SortOrder.CHRONOLOGICAL_DESC -> Icons.Default.ArrowDownward
                            SortOrder.SIZE_DESC, SortOrder.SIZE_ASC -> Icons.Default.ExpandMore
                            SortOrder.TYPE_VIDEO_FIRST, SortOrder.TYPE_VIDEO_FIRST_ASC, SortOrder.TYPE_VIDEO_FIRST_SHUFFLED -> Icons.Default.Videocam
                            SortOrder.TYPE_PHOTO_FIRST, SortOrder.TYPE_PHOTO_FIRST_ASC, SortOrder.TYPE_PHOTO_FIRST_SHUFFLED -> Icons.Default.Image
                        }
                        val flipped = sortOrder == SortOrder.CHRONOLOGICAL_ASC || sortOrder == SortOrder.SIZE_ASC

                        // Every change turns the icon half a turn: the old one spins out as the new one spins in, so
                        // flipping the order reads as the arrow turning over.
                        AnimatedContent(
                            targetState = icon to flipped,
                            transitionSpec = { iconSwap(motionScheme) },
                            label = "sortIcon"
                        ) { (targetIcon, targetFlipped) ->
                            val spin by transition.animateFloat(
                                transitionSpec = { motionScheme.defaultSpatialSpec() },
                                label = "sortSpin"
                            ) { state ->
                                when (state) {
                                    EnterExitState.PreEnter -> -180f
                                    EnterExitState.Visible -> 0f
                                    EnterExitState.PostExit -> 180f
                                }
                            }
                            Icon(
                                imageVector = targetIcon,
                                contentDescription = stringResource(R.string.settings_sort_order_label),
                                modifier = Modifier
                                    .size(iconSize)
                                    .graphicsLayer { rotationZ = (if (targetFlipped) 180f else 0f) + spin }
                            )
                        }
                    }

                    sortMenu()
                }
            }

            if (onReorder != null) {
                // Sits on top of the tiles: blocks taps, and a long-press on a middle tile starts dragging it.
                Box(
                    Modifier
                        .matchParentSize()
                        .pointerInput(Unit) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { offset ->
                                    val step = (size.width + gapPx) / currentTileCount
                                    val visible = currentOrder.filter { it in currentShown }
                                    // Slot 0 is Undo, so the middle tiles start at slot 1.
                                    val hit = visible.getOrNull((offset.x / step).toInt() - 1)
                                    if (hit != null) {
                                        localOrder = currentOrder
                                        dragged = hit
                                        dragOffset = 0f
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                },
                                onDrag = { change, amount ->
                                    val button = dragged
                                    val current = localOrder
                                    if (button != null && current != null) {
                                        change.consume()
                                        val step = (size.width + gapPx) / currentTileCount
                                        val visible = current.filter { it in currentShown }
                                        val pos = visible.indexOf(button)
                                        dragOffset = (dragOffset + amount.x).coerceIn(-pos * step, (visible.size - 1 - pos) * step)
                                        val dir = if (dragOffset > step / 2) 1 else if (dragOffset < -step / 2) -1 else 0
                                        val neighbor = if (dir != 0) visible.getOrNull(pos + dir) else null
                                        if (neighbor != null) {
                                            val i = current.indexOf(button)
                                            val j = current.indexOf(neighbor)
                                            localOrder = current.toMutableList().apply { this[i] = neighbor; this[j] = button }
                                            dragOffset -= dir * step
                                            glide(neighbor, dir * step)
                                            haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                        }
                                    }
                                },
                                onDragEnd = { drop() },
                                onDragCancel = { drop() }
                            )
                        }
                )
            }
        }

        AnimatedVisibility(
            visible = showSwipeButtons,
            // Same as the delete button, mirrored.
            enter = expandHorizontally(MaterialTheme.motionScheme.defaultSpatialSpec(), expandFrom = Alignment.End, clip = false) +
                scaleIn(MaterialTheme.motionScheme.defaultSpatialSpec(), initialScale = 0f, transformOrigin = TransformOrigin(1f, 0.5f)),
            exit = shrinkHorizontally(MaterialTheme.motionScheme.defaultSpatialSpec(), shrinkTowards = Alignment.End, clip = false) +
                scaleOut(MaterialTheme.motionScheme.defaultSpatialSpec(), targetScale = 0f, transformOrigin = TransformOrigin(1f, 0.5f))
        ) {
            Box(Modifier.padding(start = 12.dp)) {
                FloatingActionButton(
                    onClick = onKeep,
                    containerColor = if (isBulkKeepMode) MaterialGreen else MaterialGreen,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(48.dp)
                        .then(keepButtonModifier)
                ) {
                    Icon(
                        imageVector = if (isBulkKeepMode) Icons.Default.DoneAll else Icons.Default.Check,
                        contentDescription = stringResource(R.string.swipe_keep)
                    )
                }
                if (onReorder != null) Box(Modifier.matchParentSize().blockTouches())
            }
        }
    }
}

private data class FavoriteToggles(val count: Int, val toggledOn: Boolean)

/** One tile icon fading and scaling out as the next fades and scales in. */
private fun iconSwap(motionScheme: MotionScheme): ContentTransform =
    (fadeIn(motionScheme.fastEffectsSpec()) + scaleIn(motionScheme.fastSpatialSpec(), initialScale = 0.6f)) togetherWith
        (fadeOut(motionScheme.fastEffectsSpec()) + scaleOut(motionScheme.fastSpatialSpec(), targetScale = 0.6f))

private fun Modifier.blockTouches(): Modifier =
    pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } }

private fun Modifier.bulkDragSelect(
    haptic: HapticFeedback,
    uiState: () -> SwipeUiState,
    viewModel: SwipeViewModel,
    isDelete: Boolean
): Modifier = pointerInput(Unit) {
    var isLongPressActive = false
    var dragStartedX = 0f
    val stepPx = 36.dp.toPx()
    detectDragGesturesAfterLongPress(
        onDragStart = { offset ->
            isLongPressActive = true
            dragStartedX = offset.x
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            viewModel.enterBulkMode(isDelete = isDelete)
        },
        onDragEnd = {
            isLongPressActive = false
            viewModel.executeBulkAction()
        },
        onDragCancel = {
            isLongPressActive = false
            viewModel.exitBulkMode()
        },
        onDrag = { change, _ ->
            change.consume()
            if (isLongPressActive) {
                val state = uiState()
                val totalDrag = if (isDelete) change.position.x - dragStartedX else dragStartedX - change.position.x
                if (totalDrag > 0) {
                    val itemsToSelect = (totalDrag / stepPx).toInt()
                    val selection = mutableSetOf<String>()
                    var lastIdx = state.currentIndex
                    for (i in 0..itemsToSelect) {
                        val idx = state.currentIndex + i
                        if (idx < state.assets.size) {
                            selection.add(state.assets[idx].id)
                            lastIdx = idx
                        }
                    }
                    viewModel.setBulkSelection(selection, lastIdx)
                } else {
                    viewModel.setBulkSelection(
                        setOfNotNull(state.assets.getOrNull(state.currentIndex)?.id),
                        state.currentIndex
                    )
                }
            }
        }
    )
}

@Composable
fun SwipeActionBar(
    uiState: SwipeUiState,
    viewModel: SwipeViewModel,
    showSortMenu: Boolean,
    onSortMenuToggle: (Boolean) -> Unit
) {
    val notBulk = !uiState.isBulkDeleteMode && !uiState.isBulkKeepMode
    val haptic = LocalHapticFeedback.current
    val currentUiState by rememberUpdatedState(uiState)
    // Every favourite toggle lands in localFavorites, whoever made it (the toolbar, a double-tap on the card), and even
    // when auto-next has already moved on to the next asset; diffing it tells the heart when and which way to animate.
    var favoriteToggles by remember { mutableStateOf(FavoriteToggles(0, false)) }
    var previousFavorites by remember { mutableStateOf(uiState.localFavorites) }
    LaunchedEffect(uiState.localFavorites) {
        val toggled = uiState.localFavorites.entries.firstOrNull { (id, favorite) -> previousFavorites[id] != favorite }
        previousFavorites = uiState.localFavorites
        if (toggled != null) favoriteToggles = FavoriteToggles(favoriteToggles.count + 1, toggled.value)
    }
    SwipeActionBarLayout(
        showSwipeButtons = uiState.showSwipeButtons,
        showArchiveButton = uiState.showArchiveButton,
        showFavoriteButton = uiState.showFavoriteButton,
        showAddToAlbumButton = uiState.showAddToAlbumButton,
        showLockButton = uiState.showLockButton,
        sortOrder = uiState.sortOrder,
        showButtonLabels = uiState.showToolbarLabels,
        buttonOrder = uiState.toolbarButtonOrder,
        undoEnabled = uiState.history.isNotEmpty(),
        isFavorite = uiState.currentAsset?.let { uiState.isFavorite(it.id) } ?: false,
        favoriteToggleCount = favoriteToggles.count,
        favoriteToggledOn = favoriteToggles.toggledOn,
        isAlbumDialogOpen = uiState.showAddToAlbumDialog,
        albumAddedCount = uiState.albumAddSuccessCount,
        isSortMenuOpen = showSortMenu,
        isBulkDeleteMode = uiState.isBulkDeleteMode,
        isBulkKeepMode = uiState.isBulkKeepMode,
        onDelete = { if (notBulk) viewModel.onSwipe(SwipeDecision.DELETE) },
        onKeep = { if (notBulk) viewModel.onSwipe(SwipeDecision.KEEP) },
        onUndo = viewModel::undo,
        onArchive = viewModel::toggleArchive,
        onFavorite = viewModel::toggleFavorite,
        onAddToAlbum = viewModel::openAddToAlbumDialog,
        onLock = viewModel::toggleLock,
        onSort = { onSortMenuToggle(true) },
        deleteButtonModifier = Modifier.bulkDragSelect(haptic, { currentUiState }, viewModel, isDelete = true),
        keepButtonModifier = Modifier.bulkDragSelect(haptic, { currentUiState }, viewModel, isDelete = false),
        sortMenu = {
            if (showSortMenu) {
                SortOrderPopupMenu(
                    uiState = uiState,
                    viewModel = viewModel,
                    onDismiss = { onSortMenuToggle(false) }
                )
            }
        }
    )
}
