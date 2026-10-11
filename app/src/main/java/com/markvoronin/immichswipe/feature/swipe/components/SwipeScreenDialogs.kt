package com.markvoronin.immichswipe.feature.swipe.components

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.media3.exoplayer.ExoPlayer
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.core.SortOrder
import com.markvoronin.immichswipe.feature.swipe.SwipeUiState
import com.markvoronin.immichswipe.feature.swipe.SwipeViewModel

@Composable
fun SwipeScreenDialogs(
    uiState: SwipeUiState,
    viewModel: SwipeViewModel,
    sharedPlayer: ExoPlayer
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
        val nextUnprocessedIndex = viewModel.getNextUnprocessedIndex()
        val nextAsset = if (nextUnprocessedIndex != -1 && nextUnprocessedIndex < uiState.assets.size) {
            uiState.assets[nextUnprocessedIndex]
        } else null

        FullscreenViewer(
            asset = currentAsset,
            nextAsset = nextAsset,
            isFavorite = uiState.isFavorite(currentAsset.id),
            onSwipe = { viewModel.onSwipe(it) },
            onUndo = { viewModel.undo() },
            onToggleFavorite = { viewModel.toggleFavorite() },
            onClose = { viewModel.toggleFullscreen(false) },
            tapToSwipeEnabled = uiState.tapToSwipeEnabled,
            providedPlayer = sharedPlayer,
            showSizeIndicator = uiState.sortOrder == SortOrder.SIZE_DESC || uiState.sortOrder == SortOrder.SIZE_ASC,
            isMuted = uiState.isMuted,
            onToggleMute = { viewModel.toggleMute() },
            showMuteButton = true, // Mute button is always shown in fullscreen for videos
            downloadButtonPosition = uiState.downloadButtonPosition,
            showDownloadButton = false, // Download button hidden in fullscreen mode as requested
            onDownload = { viewModel.downloadAsset(it) },
            shareButtonPosition = uiState.shareButtonPosition,
            showShareButton = false, // Share button hidden in fullscreen mode for consistency
            onShare = { viewModel.shareAsset(it) },
            rotation = uiState.getRotation(currentAsset.id),
            baseUrl = uiState.baseUrl,
            apiKey = uiState.apiKey
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
        val lockSuccessMessage = stringResource(R.string.add_to_album_lock_success)

        AddToAlbumDialog(
            albums = uiState.albumsForAddToAlbum,
            isLoading = uiState.isFetchingAlbumsForDialog,
            showLockButton = uiState.showLockButton,
            baseUrl = uiState.baseUrl,
            apiKey = uiState.apiKey,
            onAlbumSelect = { targetAlbum ->
                viewModel.addCurrentAssetToAlbum(targetAlbum) { success, albumName ->
                    viewModel.dismissAddToAlbumDialog()
                    val message = if (success) {
                        successTemplate.format(albumName)
                    } else {
                        errorTemplate.format(albumName)
                    }
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            },
            onLockSelect = {
                viewModel.toggleLock()
                viewModel.dismissAddToAlbumDialog()
                Toast.makeText(context, lockSuccessMessage, Toast.LENGTH_SHORT).show()
            },
            onDismiss = { viewModel.dismissAddToAlbumDialog() }
        )
    }
}
