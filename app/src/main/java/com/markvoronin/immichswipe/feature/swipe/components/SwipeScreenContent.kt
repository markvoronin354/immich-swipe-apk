package com.markvoronin.immichswipe.feature.swipe.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.markvoronin.immichswipe.R
import androidx.compose.ui.unit.toSize
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
    onCardBoundsChanged: (Rect) -> Unit = {},
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
                        onSummaryClick = { viewModel.toggleSummary(visible = true) },
                        horizontalPadding = toolbarHorizontalPadding(uiState.showSwipeButtons)
                    )
                }

                val chevronRotation by animateFloatAsState(
                    targetValue = if (uiState.showTimeline) 0f else 180f,
                    animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                    label = "TimelineChevron"
                )
                val chevronColor = MaterialTheme.colorScheme.onSurfaceVariant
                val chevronDescription = stringResource(R.string.settings_show_timeline_label)
                // With the header's 2dp and the deck's 4dp, this 14dp row makes the gap above the card 20dp, like the gap below it.
                Box(
                    modifier = Modifier
                        .width(96.dp)
                        .height(14.dp)
                        .clip(MaterialTheme.shapes.extraLarge)
                        .semantics { contentDescription = chevronDescription }
                        .clickable(role = Role.Button) { viewModel.toggleTimeline() },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(width = 28.dp, height = 7.dp).rotate(chevronRotation)) {
                        val strokeWidth = 2.5.dp.toPx()
                        val inset = strokeWidth / 2
                        val peak = Path().apply {
                            moveTo(inset, size.height - inset)
                            lineTo(size.width / 2, inset)
                            lineTo(size.width - inset, size.height - inset)
                        }
                        drawPath(
                            path = peak,
                            color = chevronColor,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                }

                val nextUnprocessedIndex = viewModel.getNextUnprocessedIndex(uiState)

                AnimatedVisibility(
                    visible = uiState.showTimeline,
                    enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()) + expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
                    exit = fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()) + shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec())
                ) {
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
                }

                SwipeCardDeck(
                    uiState = uiState,
                    viewModel = viewModel,
                    sharedPlayer = sharedPlayer,
                    onTopCardSwipeOffsetChanged = onTopCardSwipeOffsetChanged,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        // The top card fills the deck, so these are its bounds at rest.
                        .onGloballyPositioned { onCardBoundsChanged(Rect(it.positionOnScreen(), it.size.toSize())) }
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
