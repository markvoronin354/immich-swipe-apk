package com.markvoronin.immichswipe.feature.settings.components

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SwipeDown
import androidx.compose.material.icons.filled.SwipeLeft
import androidx.compose.material.icons.filled.SwipeRight
import androidx.compose.material.icons.filled.SwipeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.core.SwipeAction
import com.markvoronin.immichswipe.feature.settings.SettingsUiState
import com.markvoronin.immichswipe.feature.settings.SettingsViewModel
import com.markvoronin.immichswipe.feature.swipe.utils.icon
import com.markvoronin.immichswipe.feature.swipe.utils.labelRes

@Composable
fun SwipeActionsScreen(
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
        ResetSwipeActionsDialog(
            onConfirm = {
                showResetDialog = false
                viewModel.resetSwipeActions()
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

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SwipeActionCard(
                title = stringResource(R.string.settings_swipe_left_label),
                icon = Icons.Default.SwipeLeft,
                actions = SideSwipeActions,
                selected = uiState.swipeLeftAction,
                onSelect = viewModel::setSwipeLeftAction
            )

            SwipeActionCard(
                title = stringResource(R.string.settings_swipe_right_label),
                icon = Icons.Default.SwipeRight,
                actions = SideSwipeActions,
                selected = uiState.swipeRightAction,
                onSelect = viewModel::setSwipeRightAction
            )

            SwipeActionCard(
                title = stringResource(R.string.settings_swipe_up_label),
                icon = Icons.Default.SwipeUp,
                actions = SwipeUpActions,
                selected = uiState.swipeUpAction,
                onSelect = viewModel::setSwipeUpAction
            )

            SwipeActionCard(
                title = stringResource(R.string.settings_swipe_down_label),
                icon = Icons.Default.SwipeDown,
                actions = SwipeDownActions,
                selected = uiState.swipeDownAction,
                onSelect = viewModel::setSwipeDownAction
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 0.dp
            ) {
                SettingsToggleItemSmall(
                    title = stringResource(R.string.settings_fullscreen_swipe_down_label),
                    checked = uiState.fullscreenSwipeDownUsesAction,
                    onCheckedChange = { viewModel.setFullscreenSwipeDownUsesAction(it) },
                    icon = Icons.Default.Fullscreen,
                    description = stringResource(R.string.settings_fullscreen_swipe_down_desc)
                )
            }

            OutlinedButton(
                onClick = { showResetDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.settings_swipe_actions_reset_label))
            }
        }

        Spacer(Modifier.navigationBarsPadding().height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ResetSwipeActionsDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
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
                text = stringResource(R.string.settings_swipe_actions_reset_title),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = { Text(stringResource(R.string.settings_swipe_actions_reset_msg)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.settings_swipe_actions_reset_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

// Undo and Details are left out: they only make sense for a swipe down and a swipe up.
private val SideSwipeActions = listOf(
    SwipeAction.KEEP, SwipeAction.DELETE, SwipeAction.FAVORITE, SwipeAction.ARCHIVE, SwipeAction.LOCK
)

// The same top row as the sides, with Undo between Archive and Lock below it, and Album and Rating after Lock.
private val SwipeDownActions = listOf(
    SwipeAction.KEEP, SwipeAction.DELETE, SwipeAction.FAVORITE, SwipeAction.ARCHIVE, SwipeAction.UNDO, SwipeAction.LOCK,
    SwipeAction.ALBUM, SwipeAction.RATING
)

// Likewise, with Details in Undo's place.
private val SwipeUpActions = listOf(
    SwipeAction.KEEP, SwipeAction.DELETE, SwipeAction.FAVORITE, SwipeAction.ARCHIVE, SwipeAction.DETAILS, SwipeAction.LOCK
)

@Composable
private fun SwipeActionCard(
    title: String,
    icon: ImageVector,
    actions: List<SwipeAction>,
    selected: SwipeAction,
    onSelect: (SwipeAction) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = animatedCookieShape(true, MaterialTheme.motionScheme.fastSpatialSpec()),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(12.dp))

            // Rows of three: more labels don't fit one row on a phone.
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                actions.chunked(3).forEach { SwipeActionRow(it, selected, onSelect) }
            }
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
