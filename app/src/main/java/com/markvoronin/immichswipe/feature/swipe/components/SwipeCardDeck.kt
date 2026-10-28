package com.markvoronin.immichswipe.feature.swipe.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.ExoPlayer
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.core.DoubleTapAction
import com.markvoronin.immichswipe.core.SortOrder
import com.markvoronin.immichswipe.feature.home.components.ErrorView
import com.markvoronin.immichswipe.feature.swipe.SwipeUiState
import com.markvoronin.immichswipe.feature.swipe.SwipeViewModel
import com.markvoronin.immichswipe.feature.swipe.models.SwipeCardActions
import com.markvoronin.immichswipe.feature.swipe.models.SwipeCardConfig
import com.markvoronin.immichswipe.feature.swipe.utils.supportsFullResolution

@Composable
fun SwipeCardDeck(
    uiState: SwipeUiState,
    viewModel: SwipeViewModel,
    sharedPlayer: ExoPlayer,
    modifier: Modifier = Modifier,
    onTopCardSwipeOffsetChanged: (Float) -> Unit = {}
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (uiState.isLoading && uiState.assets.isEmpty()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator()
                if (uiState.syncTotalCount > 0) {
                    Text(
                        text = stringResource(R.string.swipe_loading_metadata, uiState.syncLoadedCount, uiState.syncTotalCount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LinearProgressIndicator(
                        progress = { (uiState.syncLoadedCount.toFloat() / uiState.syncTotalCount).coerceIn(0f, 1f) },
                        modifier = Modifier.width(200.dp).clip(RoundedCornerShape(4.dp))
                    )
                }
            }
        } else if (uiState.error != null && uiState.assets.isEmpty()) {
            ErrorView(error = uiState.error) {
                viewModel.retryLoading()
            }
        } else if (uiState.currentIndex < uiState.assets.size) {
            val currentIndex = uiState.currentIndex
            val assets = uiState.assets

            val isBulk = uiState.isBulkDeleteMode || uiState.isBulkKeepMode
            val mainIndex = uiState.bulkLastIndex ?: currentIndex
            var undoneAssetId by remember { mutableStateOf<String?>(null) }
            var heldNextIndex by remember { mutableIntStateOf(-1) }
            val nextUnprocessedIndex = if (isBulk) {
                val candidate = (uiState.bulkLastIndex ?: currentIndex) + 1
                if (candidate < assets.size) candidate else -1
            } else if (undoneAssetId == assets[mainIndex].id) {
                // Keep the old background card until the restored card has settled.
                heldNextIndex
            } else {
                viewModel.getNextUnprocessedIndex(uiState)
            }

            val visibleIndices = listOfNotNull(
                mainIndex,
                nextUnprocessedIndex.takeIf { it != -1 && it != mainIndex }
            ).distinct().reversed()

            var topCardOffsetX by remember(mainIndex) { mutableFloatStateOf(0f) }

            visibleIndices.forEach { index ->
                val asset = assets[index]
                val isNextCard = index > mainIndex
                key(asset.id) {
                    SwipeCard(
                        asset = asset,
                        isNext = isNextCard,
                        isFullscreenOpen = uiState.isFullscreenMode,
                        enterFromBelow = asset.id == undoneAssetId,
                        onEnterFinished = { undoneAssetId = null },
                        providedPlayer = if (!isNextCard && !uiState.isFullscreenMode) sharedPlayer else null,
                        isMuted = uiState.isMuted,
                        topCardSwipeOffset = if (isNextCard) topCardOffsetX else 0f,
                        config = SwipeCardConfig(
                            baseUrl = uiState.baseUrl,
                            playbackBehavior = uiState.playbackBehavior,
                            fullscreenButtonPosition = uiState.fullscreenButtonPosition,
                            immichButtonPosition = uiState.immichButtonPosition,
                            rotationButtonPosition = uiState.rotationButtonPosition,
                            muteButtonPosition = uiState.muteButtonPosition,
                            downloadButtonPosition = uiState.downloadButtonPosition,
                            shareButtonPosition = uiState.shareButtonPosition,
                            fullSizeButtonPosition = uiState.fullSizeButtonPosition,
                            buttonStackOrientation = uiState.buttonStackOrientation,
                            showFullscreenButton = uiState.showFullscreenButton,
                            showImmichButton = uiState.showImmichButton,
                            immichOpenMode = uiState.immichOpenMode,
                            immichLongPressWeb = uiState.immichLongPressWeb,
                            showRotationButton = uiState.showRotationButton,
                            showMuteButton = uiState.showMuteButton,
                            showDownloadButton = uiState.showDownloadButton,
                            showShareButton = uiState.showShareButton,
                            showFullSizeButton = uiState.showFullSizeButton,
                            cardDisplayMode = uiState.cardDisplayMode,
                            tapToSwipeEnabled = uiState.tapToSwipeEnabled,
                            canUndo = !isBulk && uiState.history.isNotEmpty(),
                            showSizeIndicator = (uiState.sortOrder == SortOrder.SIZE_DESC) || (uiState.sortOrder == SortOrder.SIZE_ASC),
                            rotationAngle = uiState.getRotation(asset.id),
                            isFullSize = uiState.isFullResolution && !isNextCard && asset.supportsFullResolution
                        ),
                        actions = SwipeCardActions(
                            onSwipe = { viewModel.onSwipe(it) },
                            onUndo = {
                                undoneAssetId = uiState.history.lastOrNull()
                                heldNextIndex = nextUnprocessedIndex
                                viewModel.undo()
                            },
                            onRotateAsset = { viewModel.rotateCurrentAsset() },
                            onDoubleTap = {
                                if (uiState.doubleTapEnabled) {
                                    when (uiState.doubleTapAction) {
                                        DoubleTapAction.FULLSCREEN -> viewModel.toggleFullscreen(true)
                                        DoubleTapAction.FAVORITE -> viewModel.toggleFavorite()
                                    }
                                }
                            },
                            onOpenFullscreen = { viewModel.toggleFullscreen(true) },
                            onDownload = { viewModel.downloadAsset(it) },
                            onShare = { viewModel.shareAsset(it) },
                            onToggleMute = { viewModel.toggleMute() },
                            onToggleFullSize = { viewModel.toggleFullResolution() },
                            onSwipeOffsetChanged = { offset ->
                                if (!isNextCard) {
                                    topCardOffsetX = offset
                                    onTopCardSwipeOffsetChanged(offset)
                                }
                            }
                        )
                    )
                }
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Celebration,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.swipe_congratulations),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = { viewModel.toggleSummary(true) },
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp)
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.swipe_sync_changes),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
