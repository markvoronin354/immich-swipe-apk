package com.markvoronin.immichswipe.feature.swipe.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.core.SortOrder
import com.markvoronin.immichswipe.feature.settings.components.rememberJiggleGroup
import com.markvoronin.immichswipe.feature.swipe.SwipeDecision
import com.markvoronin.immichswipe.feature.swipe.SwipeUiState
import com.markvoronin.immichswipe.feature.swipe.SwipeViewModel
import com.markvoronin.immichswipe.feature.swipe.utils.MaterialGreen
import com.markvoronin.immichswipe.feature.swipe.utils.MaterialRed

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
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
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
            shapes = IconButtonDefaults.toggleableShapes(shape = shape, pressedShape = pressedShape, checkedShape = shape),
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

@Composable
private fun RowScope.AnimatedTileSlot(
    visible: Boolean,
    weight: Float,
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
    showButtonLabels: Boolean = true,
    undoEnabled: Boolean = true,
    isFavorite: Boolean = false,
    isAlbumDialogOpen: Boolean = false,
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
            enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start),
            exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.Start)
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

            Row(modifier = Modifier.fillMaxWidth()) {
                var index = 0
                val jiggle = rememberJiggleGroup(tileCount, grow = 0.14f)

                val undoIndex = index++
                ActionTile(
                    onClick = { jiggle.tap(undoIndex); onUndo() },
                    label = stringResource(R.string.swipe_undo),
                    showLabel = showLabels,
                    enabled = undoEnabled,
                    shape = animatedTileShape(undoIndex, tileCount),
                    modifier = Modifier.weight(jiggle.factor(undoIndex))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = stringResource(R.string.swipe_undo),
                        modifier = Modifier.size(iconSize)
                    )
                }

                val archiveIndex = if (showArchiveButton) index++ else -1
                AnimatedTileSlot(
                    visible = showArchiveButton,
                    weight = if (archiveIndex >= 0) jiggle.factor(archiveIndex) else 1f
                ) {
                    ActionTile(
                        onClick = { jiggle.tap(archiveIndex); onArchive() },
                        label = stringResource(R.string.swipe_archive),
                        showLabel = showLabels,
                        shape = animatedTileShape(archiveIndex, tileCount),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Archive,
                            contentDescription = stringResource(R.string.swipe_archive),
                            modifier = Modifier.size(iconSize)
                        )
                    }
                }

                val favoriteIndex = if (showFavoriteButton) index++ else -1
                AnimatedTileSlot(
                    visible = showFavoriteButton,
                    weight = if (favoriteIndex >= 0) jiggle.factor(favoriteIndex) else 1f
                ) {
                    ActionTile(
                        onClick = { jiggle.tap(favoriteIndex); onFavorite() },
                        label = stringResource(R.string.swipe_label_favorite),
                        showLabel = showLabels,
                        shape = animatedTileShape(favoriteIndex, tileCount),
                        checked = isFavorite,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = stringResource(R.string.swipe_favorite),
                            tint = if (isFavorite) Color.Red else LocalContentColor.current,
                            modifier = Modifier.size(iconSize)
                        )
                    }
                }

                val albumIndex = if (showAddToAlbumButton) index++ else -1
                AnimatedTileSlot(
                    visible = showAddToAlbumButton,
                    weight = if (albumIndex >= 0) jiggle.factor(albumIndex) else 1f
                ) {
                    ActionTile(
                        onClick = { jiggle.tap(albumIndex); onAddToAlbum() },
                        label = stringResource(R.string.swipe_label_album),
                        showLabel = showLabels,
                        shape = animatedTileShape(albumIndex, tileCount),
                        checked = isAlbumDialogOpen,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.LibraryAdd,
                            contentDescription = stringResource(R.string.add_to_album_title),
                            modifier = Modifier.size(iconSize)
                        )
                    }
                }

                val lockIndex = if (showLockButton) index++ else -1
                AnimatedTileSlot(
                    visible = showLockButton,
                    weight = if (lockIndex >= 0) jiggle.factor(lockIndex) else 1f
                ) {
                    ActionTile(
                        onClick = { jiggle.tap(lockIndex); onLock() },
                        label = stringResource(R.string.swipe_locked),
                        showLabel = showLabels,
                        shape = animatedTileShape(lockIndex, tileCount),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = stringResource(R.string.swipe_locked),
                            modifier = Modifier.size(iconSize)
                        )
                    }
                }

                val sortIndex = index
                Spacer(Modifier.width(3.dp))
                Box(modifier = Modifier.weight(jiggle.factor(sortIndex))) {
                    ActionTile(
                        onClick = { jiggle.tap(sortIndex); onSort() },
                        label = stringResource(R.string.swipe_label_sort),
                        showLabel = showLabels,
                        shape = animatedTileShape(sortIndex, tileCount),
                        checked = isSortMenuOpen,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val icon = when (sortOrder) {
                            SortOrder.SHUFFLED -> Icons.Default.Shuffle
                            SortOrder.CHRONOLOGICAL_ASC -> Icons.Default.ArrowUpward
                            SortOrder.CHRONOLOGICAL_DESC -> Icons.Default.ArrowDownward
                            SortOrder.SIZE_DESC -> Icons.Default.ExpandMore
                            SortOrder.SIZE_ASC -> Icons.Default.ExpandLess
                            SortOrder.TYPE_VIDEO_FIRST, SortOrder.TYPE_VIDEO_FIRST_ASC, SortOrder.TYPE_VIDEO_FIRST_SHUFFLED -> Icons.Default.Videocam
                            SortOrder.TYPE_PHOTO_FIRST, SortOrder.TYPE_PHOTO_FIRST_ASC, SortOrder.TYPE_PHOTO_FIRST_SHUFFLED -> Icons.Default.Image
                        }

                        Icon(
                            imageVector = icon,
                            contentDescription = stringResource(R.string.settings_sort_order_label),
                            modifier = Modifier.size(iconSize)
                        )
                    }

                    sortMenu()
                }
            }
        }

        AnimatedVisibility(
            visible = showSwipeButtons,
            enter = fadeIn() + expandHorizontally(expandFrom = Alignment.End),
            exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.End)
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
            }
        }
    }
}

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
    SwipeActionBarLayout(
        showSwipeButtons = uiState.showSwipeButtons,
        showArchiveButton = uiState.showArchiveButton,
        showFavoriteButton = uiState.showFavoriteButton,
        showAddToAlbumButton = uiState.showAddToAlbumButton,
        showLockButton = uiState.showLockButton,
        sortOrder = uiState.sortOrder,
        showButtonLabels = uiState.showToolbarLabels,
        undoEnabled = uiState.history.isNotEmpty(),
        isFavorite = uiState.currentAsset?.let { uiState.isFavorite(it.id) } ?: false,
        isAlbumDialogOpen = uiState.showAddToAlbumDialog,
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
