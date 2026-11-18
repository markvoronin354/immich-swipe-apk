package com.markvoronin.immichswipe.feature.swipe.components

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.media3.exoplayer.ExoPlayer
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.core.SortOrder
import com.markvoronin.immichswipe.core.SwipeAction
import com.markvoronin.immichswipe.feature.swipe.SwipeUiState
import com.markvoronin.immichswipe.feature.swipe.SwipeViewModel
import com.markvoronin.immichswipe.feature.swipe.utils.supportsFullResolution

@Composable
fun SwipeScreenDialogs(
    uiState: SwipeUiState,
    viewModel: SwipeViewModel,
    sharedPlayer: ExoPlayer,
    cardBounds: () -> Rect? = { null }
) {
    val context = LocalContext.current

    if (uiState.showSummary) {
        SummaryDialog(
            uiState = uiState,
            onDismiss = { viewModel.toggleSummary(false) },
            onApply = { viewModel.applyChanges() },
            onUndoDecision = { viewModel.undoSpecificDecision(it) }
        )
    }

    if (uiState.isFullscreenMode && uiState.currentAsset != null) {
        val currentAsset = uiState.currentAsset!!
        val nextUnprocessedIndex = viewModel.getNextUnprocessedIndex(uiState)
        val nextAsset = if (nextUnprocessedIndex != -1 && nextUnprocessedIndex < uiState.assets.size) {
            uiState.assets[nextUnprocessedIndex]
        } else null

        FullscreenViewer(
            asset = currentAsset,
            nextAsset = nextAsset,
            isFavorite = uiState.isFavorite(currentAsset.id),
            isNextFavorite = nextAsset?.let { uiState.isFavorite(it.id) } ?: false,
            onSwipe = { viewModel.performSwipeAction(it) },
            onUndo = { viewModel.undo() },
            onToggleFavorite = { viewModel.toggleFavorite() },
            onToggleFavoriteNext = { nextAsset?.let { viewModel.toggleFavoriteFor(it.id) } },
            onClose = { viewModel.toggleFullscreen(false) },
            tapToSwipeEnabled = uiState.tapToSwipeEnabled,
            swipeLeftAction = uiState.swipeLeftAction,
            swipeRightAction = uiState.swipeRightAction,
            swipeDownAction = uiState.swipeDownAction,
            swipeDownUsesAction = uiState.fullscreenSwipeDownUsesAction,
            providedPlayer = sharedPlayer,
            showSizeIndicator = uiState.sortOrder == SortOrder.SIZE_DESC || uiState.sortOrder == SortOrder.SIZE_ASC,
            isMuted = uiState.isMuted,
            onToggleMute = { viewModel.toggleMute() },
            showMuteButton = true, // Mute button is always shown in fullscreen for videos
            rotation = uiState.getRotation(currentAsset.id),
            baseUrl = uiState.baseUrl,
            isFullSize = uiState.isFullResolution && currentAsset.supportsFullResolution,
            onToggleFullSize = { viewModel.toggleFullResolution() },
            showButtonLabels = uiState.showToolbarLabels,
            loadAlbumNames = { viewModel.loadAlbumNames(it) },
            canUndo = uiState.history.isNotEmpty(),
            cardBounds = cardBounds,
        )
    }

    if (uiState.showSuccessAnimation) {
        SuccessAnimationOverlay()
    }

    if (uiState.showResetConfirmation) {
        ResetConfirmationDialog(
            onDismiss = { viewModel.toggleResetConfirmation(false) },
            onConfirm = { viewModel.resetAlbumDecisions() }
        )
    }

    if (uiState.isBulkDeleteMode || uiState.isBulkKeepMode) {
        val isDelete = uiState.isBulkDeleteMode
        SwipeBulkOverlay(
            isDelete = isDelete,
            selectionCount = uiState.bulkSelection.size,
            onDismiss = { viewModel.exitBulkMode() }
        )
    }

    if (uiState.showAddToAlbumDialog) {
        val successTemplate = stringResource(R.string.add_to_album_success)
        val errorTemplate = stringResource(R.string.add_to_album_error)
        val permissionErrorTemplate = stringResource(R.string.add_to_album_permission_error)
        val resultMessage = { success: Boolean, albumName: String, missingPermission: String? ->
            when {
                success -> successTemplate.format(albumName)
                missingPermission != null -> permissionErrorTemplate.format(albumName, missingPermission)
                else -> errorTemplate.format(albumName)
            }
        }
        val lockSuccessMessage = stringResource(R.string.add_to_album_lock_success)
        val archiveSuccessMessage = stringResource(R.string.add_to_album_archive_success)
        val swipeActions = listOf(uiState.swipeLeftAction, uiState.swipeRightAction, uiState.swipeUpAction, uiState.swipeDownAction)

        AddToAlbumDialog(
            albums = uiState.albumsForAddToAlbum,
            isLoading = uiState.isFetchingAlbumsForDialog,
            showLockButton = uiState.showLockButton || SwipeAction.LOCK in swipeActions,
            showArchiveButton = uiState.showArchiveButton || SwipeAction.ARCHIVE in swipeActions,
            baseUrl = uiState.baseUrl,
            onAlbumSelect = { targetAlbum ->
                viewModel.addCurrentAssetToAlbum(targetAlbum) { success, albumName, missingPermission ->
                    viewModel.dismissAddToAlbumDialog()
                    Toast.makeText(context, resultMessage(success, albumName, missingPermission), Toast.LENGTH_SHORT).show()
                }
            },
            onCreateAlbum = { newAlbumName ->
                viewModel.createAlbumWithCurrentAsset(newAlbumName) { success, albumName, missingPermission ->
                    viewModel.dismissAddToAlbumDialog()
                    Toast.makeText(context, resultMessage(success, albumName, missingPermission), Toast.LENGTH_SHORT).show()
                }
            },
            onLockSelect = {
                viewModel.toggleLock()
                viewModel.dismissAddToAlbumDialog()
                Toast.makeText(context, lockSuccessMessage, Toast.LENGTH_SHORT).show()
            },
            onArchiveSelect = {
                viewModel.toggleArchive()
                viewModel.dismissAddToAlbumDialog()
                Toast.makeText(context, archiveSuccessMessage, Toast.LENGTH_SHORT).show()
            },
            onDismiss = { viewModel.dismissAddToAlbumDialog() }
        )
    }
}
