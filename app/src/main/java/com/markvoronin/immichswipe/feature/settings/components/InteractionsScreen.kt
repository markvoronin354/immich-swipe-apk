package com.markvoronin.immichswipe.feature.settings.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.core.DoubleTapAction
import com.markvoronin.immichswipe.feature.settings.SettingsUiState
import com.markvoronin.immichswipe.feature.settings.SettingsViewModel

// Inner padding of each tile inside a section.
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

        // Section 1: Button visibility
        SettingsSection(
            title = stringResource(R.string.settings_section_button_visibility),
            icon = Icons.Default.Visibility
        ) {
            Column {
                // 0. Add to album
                Column(modifier = TilePadding) {
                    SettingsToggleItemSmall(
                        title = stringResource(R.string.settings_show_add_to_album_label),
                        checked = uiState.showAddToAlbumButton,
                        onCheckedChange = { viewModel.setShowAddToAlbumButton(it) },
                        icon = Icons.Default.LibraryAdd
                    )
                }

                SettingsItemGap()

                // 1. Keep/Delete
                Column(modifier = TilePadding) {
                    SettingsToggleItemSmall(
                        title = stringResource(R.string.settings_show_swipe_buttons_label),
                        checked = uiState.showSwipeButtons,
                        onCheckedChange = { viewModel.setShowSwipeButtons(it) },
                        icon = Icons.Default.AdsClick
                    )
                }

                SettingsItemGap()

                // 2. Archive
                Column(modifier = TilePadding) {
                    SettingsToggleItemSmall(
                        title = stringResource(R.string.settings_show_archive_label),
                        checked = uiState.showArchiveButton,
                        onCheckedChange = { viewModel.setShowArchiveButton(it) },
                        icon = Icons.Default.Archive
                    )
                }

                SettingsItemGap()

                // 3. Lock
                Column(modifier = TilePadding) {
                    SettingsToggleItemSmall(
                        title = stringResource(R.string.settings_show_lock_label),
                        checked = uiState.showLockButton,
                        onCheckedChange = { viewModel.setShowLockButton(it) },
                        icon = Icons.Default.Lock
                    )
                }

                SettingsItemGap()

                // 4. Favorite (its Auto-advance sub-option stays in the same tile)
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
                                modifier = Modifier.padding(start = 40.dp, end = 16.dp, bottom = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Section 2: Actions
        SettingsSection(
            title = stringResource(R.string.settings_section_actions),
            icon = Icons.Default.TouchApp
        ) {
            Column {
                // 5. Tap to swipe
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
                        modifier = Modifier.padding(start = 40.dp, end = 16.dp, bottom = 8.dp)
                    )
                }

                SettingsItemGap()

                // 6. Double tap
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
                        JiggleRow(
                            count = 2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 8.dp)
                        ) { jiggle ->
                            ThemeModeButton(
                                text = stringResource(R.string.settings_double_tap_fullscreen),
                                icon = Icons.Default.Fullscreen,
                                selected = uiState.doubleTapAction == DoubleTapAction.FULLSCREEN,
                                onClick = { jiggle.tap(0); viewModel.setDoubleTapAction(DoubleTapAction.FULLSCREEN) },
                                modifier = Modifier.weight(jiggle.factor(0)),
                                index = 0, count = 2
                            )
                            ThemeModeButton(
                                text = stringResource(R.string.settings_double_tap_favorite),
                                icon = Icons.Default.Favorite,
                                selected = uiState.doubleTapAction == DoubleTapAction.FAVORITE,
                                onClick = { jiggle.tap(1); viewModel.setDoubleTapAction(DoubleTapAction.FAVORITE) },
                                modifier = Modifier.weight(jiggle.factor(1)),
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
