package com.markvoronin.immichswipe.feature.settings.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.core.DoubleTapAction
import com.markvoronin.immichswipe.feature.settings.SettingsUiState
import com.markvoronin.immichswipe.feature.settings.SettingsViewModel
import com.markvoronin.immichswipe.feature.swipe.components.SwipeActionBarLayout

private val TilePadding = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)

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
                        .padding(top = 4.dp, bottom = 16.dp)
                        .clearAndSetSemantics { contentDescription = previewLabel }
                ) {
                    SwipeActionBarLayout(
                        showSwipeButtons = uiState.showSwipeButtons,
                        showArchiveButton = uiState.showArchiveButton,
                        showFavoriteButton = uiState.showFavoriteButton,
                        showAddToAlbumButton = uiState.showAddToAlbumButton,
                        showLockButton = uiState.showLockButton,
                        sortOrder = uiState.sortOrder,
                        showButtonLabels = uiState.showToolbarLabels
                    )
                    Box(
                        Modifier
                            .matchParentSize()
                            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } }
                    )
                }
            }
        ) {
            Column {
                Column(modifier = TilePadding) {
                    SettingsToggleItemSmall(
                        title = stringResource(R.string.settings_show_toolbar_labels_label),
                        checked = uiState.showToolbarLabels,
                        onCheckedChange = { viewModel.setShowToolbarLabels(it) },
                        icon = Icons.AutoMirrored.Filled.Label
                    )
                }

                SettingsItemGap()

                Column(modifier = TilePadding) {
                    SettingsToggleItemSmall(
                        title = stringResource(R.string.settings_show_add_to_album_label),
                        checked = uiState.showAddToAlbumButton,
                        onCheckedChange = { viewModel.setShowAddToAlbumButton(it) },
                        icon = Icons.Default.LibraryAdd
                    )
                }

                SettingsItemGap()

                Column(modifier = TilePadding) {
                    SettingsToggleItemSmall(
                        title = stringResource(R.string.settings_show_swipe_buttons_label),
                        checked = uiState.showSwipeButtons,
                        onCheckedChange = { viewModel.setShowSwipeButtons(it) },
                        icon = Icons.Default.AdsClick
                    )
                }

                SettingsItemGap()

                Column(modifier = TilePadding) {
                    SettingsToggleItemSmall(
                        title = stringResource(R.string.settings_show_archive_label),
                        checked = uiState.showArchiveButton,
                        onCheckedChange = { viewModel.setShowArchiveButton(it) },
                        icon = Icons.Default.Archive
                    )
                }

                SettingsItemGap()

                Column(modifier = TilePadding) {
                    SettingsToggleItemSmall(
                        title = stringResource(R.string.settings_show_lock_label),
                        checked = uiState.showLockButton,
                        onCheckedChange = { viewModel.setShowLockButton(it) },
                        icon = Icons.Default.Lock
                    )
                }

                SettingsItemGap()

                Column(modifier = TilePadding) {
                    SettingsToggleItemSmall(
                        title = stringResource(R.string.settings_tri_favorite),
                        checked = uiState.showFavoriteButton,
                        onCheckedChange = { viewModel.setShowFavorite(it) },
                        icon = Icons.Default.Favorite
                    )

                    AnimatedVisibility(
                        visible = uiState.showFavoriteButton,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column(modifier = Modifier.padding(start = 16.dp)) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

                            SettingsToggleItemSmall(
                                title = stringResource(R.string.settings_auto_next_label),
                                checked = uiState.autoNextOnFav,
                                onCheckedChange = { viewModel.setAutoNextOnFav(it) },
                                icon = Icons.AutoMirrored.Filled.Forward
                            )
                            Text(
                                text = stringResource(R.string.settings_auto_next_desc),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(start = 58.dp, end = 16.dp, bottom = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        SettingsSection(
            title = stringResource(R.string.settings_section_actions)
        ) {
            Column {
                Column(modifier = TilePadding) {
                    SettingsToggleItemSmall(
                        title = stringResource(R.string.settings_tap_to_swipe_label),
                        checked = uiState.tapToSwipeEnabled,
                        onCheckedChange = { viewModel.setTapToSwipeEnabled(it) },
                        icon = Icons.Default.TouchApp
                    )
                    Text(
                        text = stringResource(R.string.settings_tap_to_swipe_desc),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(start = 58.dp, end = 16.dp, bottom = 8.dp)
                    )
                }

                SettingsItemGap()

                Column(modifier = TilePadding) {
                    SettingsToggleItemSmall(
                        title = stringResource(R.string.settings_double_tap_label),
                        checked = uiState.doubleTapEnabled,
                        onCheckedChange = { viewModel.setDoubleTapEnabled(it) },
                        icon = Icons.Default.TouchApp
                    )

                    AnimatedVisibility(
                        visible = uiState.doubleTapEnabled,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 8.dp),
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

        Spacer(Modifier.navigationBarsPadding().height(24.dp))
    }
}
