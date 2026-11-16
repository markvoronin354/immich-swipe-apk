package com.markvoronin.immichswipe.feature.settings.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SwipeDown
import androidx.compose.material.icons.filled.SwipeLeft
import androidx.compose.material.icons.filled.SwipeRight
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.core.DoubleTapAction
import com.markvoronin.immichswipe.core.SwipeAction
import com.markvoronin.immichswipe.feature.settings.SettingsUiState
import com.markvoronin.immichswipe.feature.settings.SettingsViewModel
import com.markvoronin.immichswipe.feature.swipe.components.SwipeActionBarLayout
import com.markvoronin.immichswipe.feature.swipe.utils.icon
import com.markvoronin.immichswipe.feature.swipe.utils.labelRes

@Composable
fun InteractionsScreen(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = true) {
        onBack()
    }

    val scrollState = rememberScrollState()
    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        ResetInteractionsDialog(
            onConfirm = {
                showResetDialog = false
                viewModel.resetInteractions()
            },
            onDismiss = { showResetDialog = false }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalFadingEdges(scrollState, length = 32.dp)
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(Modifier.height(16.dp))

        val previewLabel = stringResource(R.string.settings_toolbar_preview)
        SettingsSection(
            title = stringResource(R.string.settings_section_toolbar),
            topContent = {
                Box(
                    modifier = Modifier
                        .layout { measurable, constraints ->
                            val extra = 32.dp.roundToPx()
                            val width = constraints.maxWidth + extra
                            val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
                            layout(constraints.maxWidth, placeable.height) { placeable.place(-extra / 2, 0) }
                        }
                        .padding(top = 4.dp, bottom = 8.dp)
                        .clearAndSetSemantics { contentDescription = previewLabel }
                ) {
                    SwipeActionBarLayout(
                        showSwipeButtons = uiState.showSwipeButtons,
                        swipeLeftAction = uiState.swipeLeftAction,
                        swipeRightAction = uiState.swipeRightAction,
                        showArchiveButton = uiState.showArchiveButton,
                        showFavoriteButton = uiState.showFavoriteButton,
                        showAddToAlbumButton = uiState.showAddToAlbumButton,
                        showLockButton = uiState.showLockButton,
                        sortOrder = uiState.sortOrder,
                        showButtonLabels = uiState.showToolbarLabels,
                        buttonOrder = uiState.toolbarButtonOrder,
                        onReorder = viewModel::setToolbarButtonOrder
                    )
                }
                Text(
                    text = stringResource(R.string.settings_toolbar_reorder_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                )
            }
        ) {
            Column {
                SettingsToggleItemSmall(
                    title = stringResource(R.string.settings_show_toolbar_labels_label),
                    checked = uiState.showToolbarLabels,
                    onCheckedChange = { viewModel.setShowToolbarLabels(it) },
                    icon = Icons.AutoMirrored.Filled.Label
                )

                SettingsItemGap()

                SettingsToggleItemSmall(
                    title = stringResource(R.string.settings_show_add_to_album_label),
                    checked = uiState.showAddToAlbumButton,
                    onCheckedChange = { viewModel.setShowAddToAlbumButton(it) },
                    icon = Icons.Default.LibraryAdd
                )

                SettingsItemGap()

                SettingsToggleItemSmall(
                    title = stringResource(R.string.settings_show_swipe_buttons_label),
                    checked = uiState.showSwipeButtons,
                    onCheckedChange = { viewModel.setShowSwipeButtons(it) },
                    icon = Icons.Default.AdsClick
                )

                SettingsItemGap()

                SettingsToggleItemSmall(
                    title = stringResource(R.string.settings_show_archive_label),
                    checked = uiState.showArchiveButton,
                    onCheckedChange = { viewModel.setShowArchiveButton(it) },
                    icon = Icons.Default.Archive
                )

                SettingsItemGap()

                SettingsToggleItemSmall(
                    title = stringResource(R.string.settings_show_lock_label),
                    checked = uiState.showLockButton,
                    onCheckedChange = { viewModel.setShowLockButton(it) },
                    icon = Icons.Default.Lock
                )

                SettingsItemGap()

                Column {
                    SettingsToggleItemSmall(
                        title = stringResource(R.string.settings_tri_favorite),
                        checked = uiState.showFavoriteButton,
                        onCheckedChange = { viewModel.setShowFavorite(it) },
                        icon = Icons.Default.Favorite
                    )

                    AnimatedVisibility(
                        visible = uiState.showFavoriteButton,
                        enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
                        exit = shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec())
                    ) {
                        Column(modifier = Modifier.padding(start = 16.dp)) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)

                            SettingsToggleItemSmall(
                                title = stringResource(R.string.settings_auto_next_label),
                                checked = uiState.autoNextOnFav,
                                onCheckedChange = { viewModel.setAutoNextOnFav(it) },
                                icon = Icons.AutoMirrored.Filled.Forward,
                                description = stringResource(R.string.settings_auto_next_desc)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        SettingsSection(title = stringResource(R.string.settings_section_swipe_actions)) {
            Column {
                SwipeActionItem(
                    title = stringResource(R.string.settings_swipe_left_label),
                    icon = Icons.Default.SwipeLeft,
                    actions = SideSwipeActions,
                    selected = uiState.swipeLeftAction,
                    onSelect = viewModel::setSwipeLeftAction
                )

                SettingsItemGap()

                SwipeActionItem(
                    title = stringResource(R.string.settings_swipe_right_label),
                    icon = Icons.Default.SwipeRight,
                    actions = SideSwipeActions,
                    selected = uiState.swipeRightAction,
                    onSelect = viewModel::setSwipeRightAction
                )

                SettingsItemGap()

                SwipeActionItem(
                    title = stringResource(R.string.settings_swipe_down_label),
                    icon = Icons.Default.SwipeDown,
                    actions = SwipeDownActions,
                    selected = uiState.swipeDownAction,
                    onSelect = viewModel::setSwipeDownAction
                )

                SettingsItemGap()

                SettingsToggleItemSmall(
                    title = stringResource(R.string.settings_fullscreen_swipe_down_label),
                    checked = uiState.fullscreenSwipeDownUsesAction,
                    onCheckedChange = { viewModel.setFullscreenSwipeDownUsesAction(it) },
                    icon = Icons.Default.Fullscreen,
                    description = stringResource(R.string.settings_fullscreen_swipe_down_desc)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        SettingsSection(
            title = stringResource(R.string.settings_section_actions)
        ) {
            Column {
                SettingsToggleItemSmall(
                    title = stringResource(R.string.settings_tap_to_swipe_label),
                    checked = uiState.tapToSwipeEnabled,
                    onCheckedChange = { viewModel.setTapToSwipeEnabled(it) },
                    icon = Icons.Default.TouchApp,
                    description = stringResource(R.string.settings_tap_to_swipe_desc)
                )

                SettingsItemGap()

                Column {
                    SettingsToggleItemSmall(
                        title = stringResource(R.string.settings_double_tap_label),
                        checked = uiState.doubleTapEnabled,
                        onCheckedChange = { viewModel.setDoubleTapEnabled(it) },
                        icon = Icons.Default.TouchApp
                    )

                    AnimatedVisibility(
                        visible = uiState.doubleTapEnabled,
                        enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
                        exit = shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec())
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, bottom = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            ThemeModeButton(
                                text = stringResource(R.string.settings_double_tap_fullscreen),
                                icon = Icons.Default.Fullscreen,
                                selected = uiState.doubleTapAction == DoubleTapAction.FULLSCREEN,
                                onClick = { viewModel.setDoubleTapAction(DoubleTapAction.FULLSCREEN) },
                                modifier = Modifier.weight(1f),
                                index = 0, count = 2
                            )
                            ThemeModeButton(
                                text = stringResource(R.string.settings_double_tap_favorite),
                                icon = Icons.Default.Favorite,
                                selected = uiState.doubleTapAction == DoubleTapAction.FAVORITE,
                                onClick = { viewModel.setDoubleTapAction(DoubleTapAction.FAVORITE) },
                                modifier = Modifier.weight(1f),
                                index = 1, count = 2
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        SettingsSection(
            title = stringResource(R.string.settings_section_layout)
        ) {
            SettingsToggleItemSmall(
                title = stringResource(R.string.settings_show_timeline_label),
                checked = uiState.showTimeline,
                onCheckedChange = { viewModel.setShowTimeline(it) },
                icon = Icons.Default.ViewCarousel
            )
        }

        Spacer(Modifier.height(16.dp))

        OutlinedButton(
            onClick = { showResetDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.settings_interactions_reset_label))
        }

        Spacer(Modifier.navigationBarsPadding().height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ResetInteractionsDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primary, MaterialShapes.Gem.toShape()),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        title = {
            Text(
                text = stringResource(R.string.settings_interactions_reset_title),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = { Text(stringResource(R.string.settings_interactions_reset_msg)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.settings_interactions_reset_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

// Undo is left out: it only makes sense for a swipe down.
private val SideSwipeActions = listOf(
    SwipeAction.KEEP, SwipeAction.DELETE, SwipeAction.FAVORITE, SwipeAction.ARCHIVE, SwipeAction.LOCK
)

// The same top row as the sides, with Undo between Archive and Lock below it.
private val SwipeDownActions = listOf(
    SwipeAction.KEEP, SwipeAction.DELETE, SwipeAction.FAVORITE, SwipeAction.ARCHIVE, SwipeAction.UNDO, SwipeAction.LOCK
)

@Composable
private fun SwipeActionItem(
    title: String,
    icon: ImageVector,
    actions: List<SwipeAction>,
    selected: SwipeAction,
    onSelect: (SwipeAction) -> Unit,
) {
    SettingsExpandableItem(
        title = title,
        subtitle = stringResource(selected.labelRes),
        icon = icon
    ) {
        // Rows of three: more labels don't fit one row on a phone.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            actions.chunked(3).forEach { SwipeActionRow(it, selected, onSelect) }
        }
    }
}

@Composable
private fun SwipeActionRow(
    actions: List<SwipeAction>,
    selected: SwipeAction,
    onSelect: (SwipeAction) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        actions.forEachIndexed { index, action ->
            ThemeModeButton(
                text = stringResource(action.labelRes),
                icon = action.icon,
                selected = selected == action,
                onClick = { onSelect(action) },
                modifier = Modifier.weight(1f),
                index = index, count = actions.size
            )
        }
    }
}
