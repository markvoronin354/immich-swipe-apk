package com.markvoronin.immichswipe.feature.swipe.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.core.SortOrder
import com.markvoronin.immichswipe.feature.swipe.SwipeDecision
import com.markvoronin.immichswipe.feature.swipe.SwipeUiState
import com.markvoronin.immichswipe.feature.swipe.SwipeViewModel
import com.markvoronin.immichswipe.feature.swipe.utils.MaterialGreen
import com.markvoronin.immichswipe.feature.swipe.utils.MaterialRed
import kotlinx.coroutines.delay

/**
 * Borderless icon button: at rest it is just the icon. Pressing blooms a [shape] blob behind it that springs in
 * and spins slightly while the icon dips. When [checked] is non-null it is a toggle whose blob stays while on,
 * and [active] keeps the blob up while a menu or dialog opened by the button is showing. With [linger] the
 * blob also stays for half a second after a tap, so instant actions still get visible feedback.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BarIconButton(
    onClick: () -> Unit,
    buttonSize: Dp,
    shape: Shape,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    checked: Boolean? = null,
    active: Boolean = false,
    linger: Boolean = false,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    var lingering by remember { mutableStateOf(false) }
    LaunchedEffect(pressed) {
        if (pressed) {
            lingering = linger
        } else if (lingering) {
            delay(500)
            lingering = false
        }
    }
    val showBlob = pressed || active || lingering
    val spec = remember { MotionScheme.expressive().fastSpatialSpec<Float>() }
    val blobScale by animateFloatAsState(
        targetValue = if (showBlob || checked == true) 1f else 0f,
        animationSpec = spec,
        label = "barBlobScale"
    )
    val blobRotation by animateFloatAsState(
        targetValue = if (showBlob) 25f else 0f,
        animationSpec = spec,
        label = "barBlobRotation"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spec,
        label = "barIconScale"
    )
    val blobColor = if (checked == true) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer

    val clickModifier = if (checked == null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            role = Role.Button,
            onClick = onClick
        )
    } else {
        Modifier.toggleable(
            value = checked,
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            role = Role.Switch,
            onValueChange = { onClick() }
        )
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(buttonSize)
            .then(clickModifier)
            .graphicsLayer { alpha = if (enabled) 1f else 0.38f }
    ) {
        Box(
            Modifier
                .size(buttonSize)
                .graphicsLayer {
                    scaleX = blobScale
                    scaleY = blobScale
                    rotationZ = blobRotation
                }
                .background(blobColor, shape)
        )
        Box(Modifier.graphicsLayer { scaleX = iconScale; scaleY = iconScale }) {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground, content = content)
        }
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
        horizontalArrangement = if (uiState.showSwipeButtons) Arrangement.SpaceEvenly else Arrangement.SpaceBetween,
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

        val buttonSize = if (uiState.showSwipeButtons) 40.dp else 44.dp
        val iconSize = if (uiState.showSwipeButtons) 22.dp else 26.dp

        BarIconButton(
            onClick = { viewModel.undo() },
            enabled = uiState.history.isNotEmpty(),
            buttonSize = buttonSize,
            shape = MaterialShapes.Clover4Leaf.toShape(),
            linger = true
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Undo,
                contentDescription = stringResource(R.string.swipe_undo),
                modifier = Modifier.size(iconSize)
            )
        }

        if (uiState.showArchiveButton) {
            BarIconButton(onClick = { viewModel.toggleArchive() }, buttonSize = buttonSize, shape = MaterialShapes.Cookie6Sided.toShape(), linger = true) {
                Icon(
                    imageVector = Icons.Default.Archive,
                    contentDescription = stringResource(R.string.swipe_archive),
                    modifier = Modifier.size(iconSize)
                )
            }
        }

        if (uiState.showFavoriteButton) {
            val isFav = uiState.currentAsset?.let { uiState.isFavorite(it.id) } ?: false
            BarIconButton(
                onClick = { viewModel.toggleFavorite() },
                buttonSize = buttonSize,
                shape = MaterialShapes.Cookie9Sided.toShape(),
                checked = isFav
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
            BarIconButton(
                onClick = { viewModel.openAddToAlbumDialog() },
                buttonSize = buttonSize,
                shape = MaterialShapes.Flower.toShape(),
                active = uiState.showAddToAlbumDialog
            ) {
                Icon(
                    imageVector = Icons.Default.LibraryAdd,
                    contentDescription = stringResource(R.string.add_to_album_title),
                    modifier = Modifier.size(iconSize)
                )
            }
        }

        if (uiState.showLockButton) {
            BarIconButton(onClick = { viewModel.toggleLock() }, buttonSize = buttonSize, shape = MaterialShapes.Gem.toShape(), linger = true) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = stringResource(R.string.swipe_locked),
                    modifier = Modifier.size(iconSize)
                )
            }
        }

        Box {
            BarIconButton(
                onClick = { onSortMenuToggle(true) },
                buttonSize = buttonSize,
                shape = MaterialShapes.Cookie12Sided.toShape(),
                active = showSortMenu
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

                val tint = if (uiState.sortOrder != SortOrder.CHRONOLOGICAL_DESC) MaterialTheme.colorScheme.primary else LocalContentColor.current
                val baseSize = if (uiState.showSwipeButtons) 24.dp else 28.dp

                Icon(
                    imageVector = icon,
                    contentDescription = stringResource(R.string.settings_sort_order_label),
                    tint = tint,
                    modifier = Modifier.size(baseSize)
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
