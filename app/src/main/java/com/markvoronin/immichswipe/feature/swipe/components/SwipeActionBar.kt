package com.markvoronin.immichswipe.feature.swipe.components

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
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
 * [label] is shown as small text under the icon.
 */
@Composable
private fun ActionTile(
    onClick: () -> Unit,
    shape: Shape,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    checked: Boolean? = null,
    icon: @Composable () -> Unit
) {
    val pressedShape = RoundedCornerShape(10.dp)
    val tileModifier = modifier.height(56.dp)
    val content: @Composable () -> Unit = {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            icon()
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp, end = 4.dp)
            )
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SwipeActionBar(
    uiState: SwipeUiState,
    viewModel: SwipeViewModel,
    showSortMenu: Boolean,
    onSortMenuToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (uiState.showSwipeButtons) 16.dp else 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (uiState.showSwipeButtons) {
            val haptic = LocalHapticFeedback.current
            var isLongPressActive by remember { mutableStateOf(false) }
            var dragStartedX by remember { mutableFloatStateOf(0f) }
            val currentUiStateDelete by rememberUpdatedState(uiState)
            val densityDelete = LocalDensity.current
            val stepPxDelete = with(densityDelete) { 36.dp.toPx() }

            FloatingActionButton(
                onClick = { if (!uiState.isBulkDeleteMode && !uiState.isBulkKeepMode) viewModel.onSwipe(SwipeDecision.DELETE) },
                containerColor = if (uiState.isBulkDeleteMode) MaterialRed else MaterialTheme.colorScheme.errorContainer,
                contentColor = if (uiState.isBulkDeleteMode) Color.White else MaterialTheme.colorScheme.onErrorContainer,
                shape = CircleShape,
                modifier = Modifier
                    .size(48.dp)
                    .pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { offset ->
                                isLongPressActive = true
                                dragStartedX = offset.x
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.enterBulkMode(isDelete = true)
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
                                    val state = currentUiStateDelete
                                    val totalDrag = change.position.x - dragStartedX
                                    if (totalDrag > 0) {
                                        val itemsToSelect = (totalDrag / stepPxDelete).toInt()
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
            ) {
                Icon(
                    imageVector = if (uiState.isBulkDeleteMode) Icons.Default.DeleteSweep else Icons.Default.Delete,
                    contentDescription = stringResource(R.string.swipe_delete)
                )
            }
        }

        // A connected group: separate tiles with a gap between them, fully rounded only at the outer ends.
        val iconSize = 24.dp
        val tileCount = 2 + listOf(
            uiState.showArchiveButton,
            uiState.showFavoriteButton,
            uiState.showAddToAlbumButton,
            uiState.showLockButton
        ).count { it }

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            var index = 0
            // The tapped tile grows and its neighbours give way; the weights always sum to the same total.
            val jiggle = rememberJiggleGroup(tileCount, grow = 0.14f)

            val undoIndex = index++
            ActionTile(
                onClick = { jiggle.tap(undoIndex); viewModel.undo() },
                label = stringResource(R.string.swipe_undo),
                enabled = uiState.history.isNotEmpty(),
                shape = connectedTileShape(undoIndex, tileCount, outer = 24.dp),
                modifier = Modifier.weight(jiggle.factor(undoIndex))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = stringResource(R.string.swipe_undo),
                    modifier = Modifier.size(iconSize)
                )
            }

            if (uiState.showArchiveButton) {
                val archiveIndex = index++
                ActionTile(
                    onClick = { jiggle.tap(archiveIndex); viewModel.toggleArchive() },
                    label = stringResource(R.string.swipe_archive),
                    shape = connectedTileShape(archiveIndex, tileCount, outer = 24.dp),
                    modifier = Modifier.weight(jiggle.factor(archiveIndex))
                ) {
                    Icon(
                        imageVector = Icons.Default.Archive,
                        contentDescription = stringResource(R.string.swipe_archive),
                        modifier = Modifier.size(iconSize)
                    )
                }
            }

            if (uiState.showFavoriteButton) {
                val isFav = uiState.currentAsset?.let { uiState.isFavorite(it.id) } ?: false
                val favoriteIndex = index++
                ActionTile(
                    onClick = { jiggle.tap(favoriteIndex); viewModel.toggleFavorite() },
                    label = stringResource(R.string.swipe_label_favorite),
                    shape = connectedTileShape(favoriteIndex, tileCount, outer = 24.dp),
                    checked = isFav,
                    modifier = Modifier.weight(jiggle.factor(favoriteIndex))
                ) {
                    Icon(
                        imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = stringResource(R.string.swipe_favorite),
                        tint = if (isFav) Color.Red else LocalContentColor.current,
                        modifier = Modifier.size(iconSize)
                    )
                }
            }

            if (uiState.showAddToAlbumButton) {
                val albumIndex = index++
                ActionTile(
                    onClick = { jiggle.tap(albumIndex); viewModel.openAddToAlbumDialog() },
                    label = stringResource(R.string.swipe_label_album),
                    shape = connectedTileShape(albumIndex, tileCount, outer = 24.dp),
                    checked = uiState.showAddToAlbumDialog,
                    modifier = Modifier.weight(jiggle.factor(albumIndex))
                ) {
                    Icon(
                        imageVector = Icons.Default.LibraryAdd,
                        contentDescription = stringResource(R.string.add_to_album_title),
                        modifier = Modifier.size(iconSize)
                    )
                }
            }

            if (uiState.showLockButton) {
                val lockIndex = index++
                ActionTile(
                    onClick = { jiggle.tap(lockIndex); viewModel.toggleLock() },
                    label = stringResource(R.string.swipe_locked),
                    shape = connectedTileShape(lockIndex, tileCount, outer = 24.dp),
                    modifier = Modifier.weight(jiggle.factor(lockIndex))
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = stringResource(R.string.swipe_locked),
                        modifier = Modifier.size(iconSize)
                    )
                }
            }

            val sortIndex = index
            Box(modifier = Modifier.weight(jiggle.factor(sortIndex))) {
                ActionTile(
                    onClick = { jiggle.tap(sortIndex); onSortMenuToggle(true) },
                    label = stringResource(R.string.swipe_label_sort),
                    shape = connectedTileShape(sortIndex, tileCount, outer = 24.dp),
                    checked = showSortMenu,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val icon = when (uiState.sortOrder) {
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

                if (showSortMenu) {
                    SortOrderPopupMenu(
                        uiState = uiState,
                        viewModel = viewModel,
                        onDismiss = { onSortMenuToggle(false) }
                    )
                }
            }
        }

        if (uiState.showSwipeButtons) {
            val haptic = LocalHapticFeedback.current
            var isLongPressActive by remember { mutableStateOf(false) }
            var dragStartedX by remember { mutableFloatStateOf(0f) }
            val currentUiStateKeep by rememberUpdatedState(uiState)
            val densityKeep = LocalDensity.current
            val stepPxKeep = with(densityKeep) { 36.dp.toPx() }

            FloatingActionButton(
                onClick = { if (!uiState.isBulkKeepMode && !uiState.isBulkDeleteMode) viewModel.onSwipe(SwipeDecision.KEEP) },
                containerColor = if (uiState.isBulkKeepMode) MaterialGreen else MaterialGreen,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .size(48.dp)
                    .pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { offset ->
                                isLongPressActive = true
                                dragStartedX = offset.x
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.enterBulkMode(isDelete = false)
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
                                    val state = currentUiStateKeep
                                    val totalDrag = dragStartedX - change.position.x
                                    if (totalDrag > 0) {
                                        val itemsToSelect = (totalDrag / stepPxKeep).toInt()
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
            ) {
                Icon(
                    imageVector = if (uiState.isBulkKeepMode) Icons.Default.DoneAll else Icons.Default.Check,
                    contentDescription = stringResource(R.string.swipe_keep)
                )
            }
        }
    }
}
