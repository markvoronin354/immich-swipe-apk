package com.markvoronin.immichswipe.feature.swipe.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.media3.exoplayer.ExoPlayer
import com.markvoronin.immichswipe.feature.swipe.SwipeUiState
import com.markvoronin.immichswipe.feature.swipe.SwipeViewModel
import com.markvoronin.immichswipe.feature.tutorial.ProvideTutorialController
import com.markvoronin.immichswipe.feature.tutorial.TutorialController
import com.markvoronin.immichswipe.feature.tutorial.tutorialTarget

@Composable
fun SwipeScreenContent(
    uiState: SwipeUiState,
    viewModel: SwipeViewModel,
    sharedPlayer: ExoPlayer,
    tutorialController: TutorialController,
    showSortMenu: Boolean,
    onSortMenuToggle: (Boolean) -> Unit,
    topCardSwipeOffset: Float,
    onTopCardSwipeOffsetChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    ProvideTutorialController(tutorialController) {
        Box(modifier = modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.tutorialTarget("swipe_header", tutorialController)) {
                    SwipeHeader(
                        uiState = uiState,
                        onSummaryClick = { viewModel.toggleSummary(visible = true) }
                    )
                }

                val nextUnprocessedIndex = viewModel.getNextUnprocessedIndex(uiState)

                AssetTimeline(
                    assets = uiState.assets,
                    decisions = uiState.decisions,
                    currentIndex = uiState.currentIndex,
                    isFavorite = { uiState.isFavorite(it) },
                    isArchived = { uiState.isArchived(it) },
                    isLocked = { uiState.isLocked(it) },
                    onAssetClick = { viewModel.onMoveToAsset(it) },
                    baseUrl = uiState.baseUrl,
                    isBulkMode = uiState.isBulkDeleteMode || uiState.isBulkKeepMode,
                    bulkSelection = uiState.bulkSelection,
                    isBulkDelete = uiState.isBulkDeleteMode,
                    getRotation = { uiState.getRotation(it) },
                    swipeOffset = topCardSwipeOffset,
                    nextIndex = nextUnprocessedIndex
                )

                SwipeCardDeck(
                    uiState = uiState,
                    viewModel = viewModel,
                    sharedPlayer = sharedPlayer,
                    onTopCardSwipeOffsetChanged = onTopCardSwipeOffsetChanged,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .tutorialTarget("swipe_deck", tutorialController)
                        .zIndex(1f)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .navigationBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .tutorialTarget("swipe_actions", tutorialController)
                    ) {
                        SwipeActionBar(
                            uiState = uiState,
                            viewModel = viewModel,
                            showSortMenu = showSortMenu,
                            onSortMenuToggle = onSortMenuToggle
                        )
                    }

                    Spacer(Modifier.height(30.dp))
                }
            }
        }
    }
}
