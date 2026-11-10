@file:Suppress("FunctionName")

package com.markvoronin.immichswipe.feature.duplicates

import android.widget.Toast
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.snapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.domain.model.Asset
import com.markvoronin.immichswipe.feature.common.WavyLoadingIndicator
import com.markvoronin.immichswipe.feature.duplicates.components.DuplicateClusterCard
import com.markvoronin.immichswipe.feature.duplicates.components.FullScreenPreviewData
import com.markvoronin.immichswipe.feature.duplicates.components.FullScreenPreviewModal
import com.markvoronin.immichswipe.feature.duplicates.components.InstagramZoomOverlay
import com.markvoronin.immichswipe.feature.duplicates.components.ZoomData
import com.markvoronin.immichswipe.feature.duplicates.components.formatSizeStr
import com.markvoronin.immichswipe.feature.settings.components.verticalFadingEdges
import com.markvoronin.immichswipe.feature.swipe.components.SortPopupItem
import com.markvoronin.immichswipe.feature.swipe.utils.MaterialGreen
import com.markvoronin.immichswipe.feature.swipe.utils.MaterialRed
import com.markvoronin.immichswipe.feature.tutorial.LocalTutorialController
import com.markvoronin.immichswipe.feature.tutorial.ProvideTutorialController
import com.markvoronin.immichswipe.feature.tutorial.TooltipPlacement
import com.markvoronin.immichswipe.feature.tutorial.TutorialPhase
import com.markvoronin.immichswipe.feature.tutorial.TutorialStep
import com.markvoronin.immichswipe.feature.tutorial.rememberTutorialController
import com.markvoronin.immichswipe.feature.tutorial.tutorialTarget
import com.markvoronin.immichswipe.ui.theme.VirtualGold
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DuplicatesScreen(
    viewModel: DuplicatesViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    var activeZoomData by remember { mutableStateOf<ZoomData?>(null) }
    // On-screen bounds of every composed grid thumbnail, keyed by asset id, for the screen-wide pinch-to-peek.
    val thumbnailBounds = remember { mutableMapOf<String, Pair<Asset, Rect>>() }
    var fullScreenPreviewData by remember { mutableStateOf<FullScreenPreviewData?>(null) }
    var activeClusterIndex by remember { mutableIntStateOf(0) }
    var deepAnalyzeTarget by remember { mutableStateOf<DuplicateClusterUiModel?>(null) }
    var listBottomPx by remember { mutableFloatStateOf(0f) }
    var bottomBarTopPx by remember { mutableFloatStateOf(0f) }
    // how far the bottom bar covers the list; 0 when the list already ends above the bar
    val bottomBarHeightPx = (listBottomPx - bottomBarTopPx).coerceAtLeast(0f)
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var rootWindowOffset by remember { mutableStateOf(Offset.Zero) }

    val tutorialController = LocalTutorialController.current
        ?: rememberTutorialController()

    val duplicatesTutorialSteps = remember {
        listOf(
            TutorialStep(
                id = "clusters",
                titleRes = R.string.tutorial_duplicates_clusters_title,
                descriptionRes = R.string.tutorial_duplicates_clusters_desc,
                targetKey = "dup_cluster",
                placement = TooltipPlacement.TOP
            ),
            TutorialStep(
                id = "selection",
                titleRes = R.string.tutorial_duplicates_selection_title,
                descriptionRes = R.string.tutorial_duplicates_selection_desc,
                targetKey = "dup_photos",
                placement = TooltipPlacement.TOP
            ),
            TutorialStep(
                id = "action",
                titleRes = R.string.tutorial_duplicates_action_title,
                descriptionRes = R.string.tutorial_duplicates_action_desc,
                targetKey = "dup_delete",
                placement = TooltipPlacement.BOTTOM
            )
        )
    }

    val navTransition = LocalNavAnimatedContentScope.current.transition
    LaunchedEffect(uiState.hasCompletedDuplicatesTutorial, uiState.isLoading) {
        if (!uiState.hasCompletedDuplicatesTutorial && !uiState.isLoading) {
            // The flying album name draws above everything, the tutorial included, until the slide-in settles.
            snapshotFlow { navTransition.currentState == EnterExitState.Visible }.first { it }
            tutorialController.startTutorial(
                phase = TutorialPhase.DUPLICATES,
                steps = duplicatesTutorialSteps,
                onComplete = { viewModel.completeDuplicatesTutorial() }
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if ((tutorialController.phase == TutorialPhase.DUPLICATES) && tutorialController.isVisible) {
                tutorialController.dismissTutorial()
            }
        }
    }

    val totalAssetsCount = remember(uiState.clusters) {
        uiState.clusters.sumOf { it.assets.size }
    }

    val itemsToKeep = remember(uiState.decisions, uiState.clusters) {
        val keepAssetIds = uiState.decisions.filterValues { it == DuplicateDecision.KEEP }.keys
        uiState.clusters.flatMap { it.assets }.filter { it.id in keepAssetIds }
    }
    val keepCount = itemsToKeep.size

    val itemsToDelete = remember(uiState.decisions, uiState.clusters) {
        val deleteAssetIds = uiState.decisions.filterValues { it == DuplicateDecision.DELETE }.keys
        uiState.clusters.flatMap { it.assets }.filter { it.id in deleteAssetIds }
    }
    val deleteCount = itemsToDelete.size
    val deleteBytes = itemsToDelete.sumOf { it.exifInfo?.fileSizeInBytes ?: 0L }

    val decidedCount = keepCount + deleteCount
    val remainingCount = (totalAssetsCount - decidedCount).coerceAtLeast(0)
    val progress = if (totalAssetsCount > 0) decidedCount.toFloat() / totalAssetsCount.toFloat() else 0f

    ProvideTutorialController(tutorialController) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .onGloballyPositioned { coords ->
                    rootWindowOffset = coords.positionInWindow()
                }
                .pinchToPeek(
                    thumbnails = thumbnailBounds,
                    windowOffset = { rootWindowOffset },
                    // A pinch can't start on the bottom bar, which covers the end of the list.
                    canStartAt = { point -> bottomBarTopPx <= 0f || point.y < bottomBarTopPx },
                    decisionOf = { asset -> uiState.decisions[asset.id] ?: DuplicateDecision.NONE },
                    onZoom = { zoomData -> activeZoomData = zoomData }
                )
        ) {
            val boxScope = this
            if (uiState.isLoading) {
                WavyLoadingIndicator(modifier = Modifier.align(Alignment.Center), delayed = true)
            } else if (uiState.clusters.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = VirtualGold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.duplicates_none_found),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            } else {
                val context = LocalContext.current
                Column(modifier = Modifier.fillMaxSize()) {
                    DuplicatesHeader(
                        clustersCount = uiState.clusters.size,
                        keepCount = keepCount,
                        deleteCount = deleteCount,
                        remainingCount = remainingCount,
                        progress = progress,
                        onSummaryClick = {
                            if (deleteCount > 0) {
                                viewModel.toggleDeleteConfirmation(true)
                            } else {
                                Toast.makeText(context, R.string.swipe_no_deletions, Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.tutorialTarget("dup_delete", tutorialController)
                    )

                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .onGloballyPositioned { coords ->
                                listBottomPx = coords.positionInWindow().y + coords.size.height
                            }
                    ) {
                        val density = LocalDensity.current
                        val bottomBarHeightDp = with(density) { bottomBarHeightPx.toDp() }

                        val cardContentWidthDp = (maxWidth - 64.dp).coerceAtLeast(100.dp)
                        val imageWidthDp = (cardContentWidthDp - 8.dp) / 2f
                        val imageHeightDp = imageWidthDp / 0.75f
                        val computedCardHeightDp = (120.dp + imageHeightDp).coerceAtMost(maxHeight * 0.7f)

                        val clearHeightDp = (maxHeight - bottomBarHeightDp).coerceAtLeast(1.dp)
                        val verticalPadding = ((clearHeightDp - computedCardHeightDp) / 2f).coerceAtLeast(32.dp)

                        val snapLayoutInfoProvider = remember(listState) {
                            SnapLayoutInfoProvider(
                                lazyListState = listState,
                                snapPosition = SnapPosition.Start
                            )
                        }

                        // The default snap spring reuses the fling's leftover velocity and overshoots
                        // the target. A tween with a monotonic easing settles without passing it.
                        val decaySpec = rememberSplineBasedDecay<Float>()
                        val snapFlingBehavior = remember(snapLayoutInfoProvider, decaySpec) {
                            snapFlingBehavior(
                                snapLayoutInfoProvider = snapLayoutInfoProvider,
                                decayAnimationSpec = decaySpec,
                                snapAnimationSpec = tween(durationMillis = 350, easing = LinearOutSlowInEasing)
                            )
                        }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalFadingEdges(listState, length = 64.dp),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                top = verticalPadding,
                                end = 16.dp,
                                bottom = verticalPadding + bottomBarHeightDp
                            ),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            flingBehavior = snapFlingBehavior
                        ) {
                            itemsIndexed(uiState.clusters, key = { _, item -> item.clusterId }) { index, cluster ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .graphicsLayer {
                                            val layoutInfo = listState.layoutInfo
                                            val beforePadding = layoutInfo.beforeContentPadding.toFloat()
                                            val viewportHeight = layoutInfo.viewportSize.height.toFloat()
                                            val clearHeight = (viewportHeight - bottomBarHeightPx).coerceAtLeast(1f)

                                            val itemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
                                            if (itemInfo != null) {
                                                val itemTop = itemInfo.offset.toFloat()
                                                val totalTravelDistance = (clearHeight - beforePadding).coerceAtLeast(1f)

                                                // item offsets exclude the content padding, so the snapped card sits at 0
                                                val distanceBelow = itemTop.coerceAtLeast(0f)
                                                val distanceAbove = (-itemTop).coerceAtLeast(0f)
                                                val distanceFromTarget = maxOf(distanceBelow, distanceAbove)

                                                val fraction = (distanceFromTarget / totalTravelDistance).coerceIn(0f, 1f)

                                                val scale = 1f - (fraction * 0.18f)
                                                val alpha = 1f - (fraction * 0.82f)

                                                scaleX = scale.coerceAtLeast(0.82f)
                                                scaleY = scale.coerceAtLeast(0.82f)
                                                this.alpha = alpha.coerceAtLeast(0.18f)
                                            }
                                        }
                                ) {
                                Box(
                                    modifier = if (index == 0) {
                                        Modifier.tutorialTarget("dup_cluster", tutorialController)
                                    } else Modifier
                                ) {
                                    DuplicateClusterCard(
                                        cluster = cluster,
                                        decisions = uiState.decisions,
                                        isFavorite = { uiState.isFavorite(it) },
                                        activeZoomAssetId = activeZoomData?.asset?.id,
                                        baseUrl = uiState.baseUrl,
                                        modifier = if (index == 0) Modifier.tutorialTarget("dup_photos", tutorialController) else Modifier,
                                        onDecisionToggle = { assetId -> viewModel.toggleDecision(assetId) },
                                        onKeepAll = { viewModel.setClusterDecisions(cluster, DuplicateDecision.KEEP) },
                                        onDeleteAll = { viewModel.setClusterDecisions(cluster, DuplicateDecision.DELETE) },
                                        onAnalyze = {
                                            if (uiState.skipDeepAnalyzeConfirmation) {
                                                viewModel.deepAnalyzeCluster(cluster)
                                            } else {
                                                deepAnalyzeTarget = cluster
                                            }
                                        },
                                        onAssetLongPress = { clickedAsset ->
                                            val clusterIdx = uiState.clusters.indexOfFirst { it.clusterId == cluster.clusterId }.coerceAtLeast(0)
                                            val initialIdx = cluster.assets.indexOfFirst { it.id == clickedAsset.id }.coerceAtLeast(0)
                                            activeClusterIndex = clusterIdx
                                            fullScreenPreviewData = FullScreenPreviewData(
                                                cluster = cluster,
                                                initialIndex = initialIdx,
                                                clusters = uiState.clusters,
                                                clusterIndex = clusterIdx
                                            )
                                        },
                                        onOpenFullScreen = {
                                            val clusterIdx = uiState.clusters.indexOfFirst { it.clusterId == cluster.clusterId }.coerceAtLeast(0)
                                            activeClusterIndex = clusterIdx
                                            fullScreenPreviewData = FullScreenPreviewData(
                                                cluster = cluster,
                                                initialIndex = 0,
                                                clusters = uiState.clusters,
                                                clusterIndex = clusterIdx
                                            )
                                        },
                                        onThumbnailBoundsChanged = { asset, bounds ->
                                            if (bounds == null) {
                                                thumbnailBounds.remove(asset.id)
                                            } else {
                                                thumbnailBounds[asset.id] = asset to bounds
                                                // Keep the peek anchored to its thumbnail if the list moves underneath it.
                                                val zoom = activeZoomData
                                                if (zoom != null && zoom.asset.id == asset.id && zoom.initialBounds != bounds) {
                                                    activeZoomData = zoom.copy(initialBounds = bounds)
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                DuplicatesBottomBar(
                    uiState = uiState,
                    viewModel = viewModel,
                    modifier = with(boxScope) {
                        Modifier
                            .align(Alignment.BottomCenter)
                            .onGloballyPositioned { coords ->
                                bottomBarTopPx = coords.positionInWindow().y
                            }
                    },
                    onSortOrderSelected = { newSortOrder ->
                        viewModel.setSortOrder(newSortOrder)
                        coroutineScope.launch {
                            val updatedState = viewModel.uiState.value
                            val targetIndex = updatedState.clusters.indexOfFirst { cluster ->
                                cluster.assets.any { asset ->
                                    updatedState.decisions[asset.id] == null || updatedState.decisions[asset.id] == DuplicateDecision.NONE
                                }
                            }.coerceAtLeast(0)

                            if (updatedState.clusters.isNotEmpty()) {
                                listState.animateScrollToItem(targetIndex, 0)
                            }
                        }
                    }
                )
            }

            InstagramZoomOverlay(
                zoomData = activeZoomData,
                decision = activeZoomData?.let { uiState.decisions[it.asset.id] ?: DuplicateDecision.NONE } ?: DuplicateDecision.NONE,
                isFavorite = activeZoomData?.let { uiState.isFavorite(it.asset) } ?: false,
                rootWindowOffset = rootWindowOffset,
                baseUrl = uiState.baseUrl,
                onDismiss = { activeZoomData = null }
            )

            FullScreenPreviewModal(
                previewData = fullScreenPreviewData,
                decisions = uiState.decisions,
                isFavorite = { uiState.isFavorite(it) },
                baseUrl = uiState.baseUrl,
                onDecisionToggle = { assetId -> viewModel.toggleDecision(assetId) },
                onFavoriteToggle = { asset -> viewModel.toggleFavorite(asset) },
                onClusterChanged = { index ->
                    activeClusterIndex = index
                },
                onDismiss = {
                    fullScreenPreviewData = null
                    coroutineScope.launch {
                        val targetIndex = activeClusterIndex.coerceIn(0, (uiState.clusters.size - 1).coerceAtLeast(0))
                        if (targetIndex in uiState.clusters.indices) {
                            listState.animateScrollToItem(targetIndex, 0)
                        }
                    }
                }
            )

            if (uiState.showResetConfirmation) {
                AlertDialog(
                    onDismissRequest = { viewModel.toggleResetConfirmation(false) },
                    icon = {
                        // Same gem shape and error color as the destructive settings dialogs.
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.error, MaterialShapes.Gem.toShape()),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onError
                            )
                        }
                    },
                    title = {
                        Text(
                            text = stringResource(R.string.duplicates_reset_confirm_title),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    text = { Text(stringResource(R.string.duplicates_reset_confirm_msg)) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.toggleResetConfirmation(false)
                                viewModel.resetDecisions()
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(stringResource(R.string.common_confirm))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.toggleResetConfirmation(false) }) {
                            Text(stringResource(R.string.common_cancel))
                        }
                    }
                )
            }

            deepAnalyzeTarget?.let { cluster ->
                val downloadBytes = cluster.assets
                    .filter { !it.type.equals("VIDEO", ignoreCase = true) }
                    .sumOf { it.exifInfo?.fileSizeInBytes ?: 0L }
                AlertDialog(
                    onDismissRequest = { deepAnalyzeTarget = null },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.duplicates_deep_analyze_title),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { deepAnalyzeTarget = null }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.common_close)
                                )
                            }
                        }
                    },
                    text = { Text(stringResource(R.string.duplicates_deep_analyze_msg, formatSizeStr(downloadBytes))) },
                    confirmButton = {
                        Button(
                            onClick = {
                                deepAnalyzeTarget = null
                                viewModel.deepAnalyzeCluster(cluster)
                            }
                        ) {
                            Text(stringResource(R.string.duplicates_deep_analyze_action))
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                deepAnalyzeTarget = null
                                viewModel.skipDeepAnalyzeConfirmation()
                                viewModel.deepAnalyzeCluster(cluster)
                            }
                        ) {
                            Text(stringResource(R.string.duplicates_deep_analyze_dont_ask))
                        }
                    }
                )
            }

            if (uiState.showDeleteConfirmation) {
                AlertDialog(
                    onDismissRequest = { viewModel.toggleDeleteConfirmation(false) },
                    icon = {
                        // Same gem shape and error color as the destructive settings dialogs.
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.error, MaterialShapes.Gem.toShape()),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onError
                            )
                        }
                    },
                    title = {
                        Text(
                            text = stringResource(R.string.duplicates_delete_confirm_title),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    text = {
                        Text(
                            pluralStringResource(
                                R.plurals.duplicates_delete_confirm_msg,
                                deleteCount,
                                deleteCount,
                                formatSizeStr(deleteBytes)
                            )
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.toggleDeleteConfirmation(false)
                                viewModel.syncDeletions()
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(stringResource(R.string.swipe_delete))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.toggleDeleteConfirmation(false) }) {
                            Text(stringResource(R.string.common_cancel))
                        }
                    }
                )
            }

            if (uiState.isSyncing) {
                Dialog(onDismissRequest = {}) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(24.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            WavyLoadingIndicator()
                            Text(
                                text = stringResource(R.string.swipe_syncing),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }

            if (uiState.isAnalyzingAi) {
                Dialog(onDismissRequest = { viewModel.cancelBestShotAnalysis() }) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            WavyLoadingIndicator()
                            Text(
                                text = stringResource(R.string.duplicates_ai_analyzing),
                                style = MaterialTheme.typography.titleMedium
                            )
                            val progressState = uiState.aiProgress
                            if (progressState != null) {
                                Text(
                                    text = stringResource(
                                        R.string.duplicates_ai_cluster_progress,
                                        progressState.first,
                                        progressState.second
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(
                                onClick = { viewModel.cancelBestShotAnalysis() }
                            ) {
                                Text(stringResource(R.string.common_cancel))
                            }
                        }
                    }
                }
            }
        }
    }
}
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DuplicatesHeader(
    clustersCount: Int,
    keepCount: Int,
    deleteCount: Int,
    remainingCount: Int,
    progress: Float,
    onSummaryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
        label = "DuplicatesProgressBarAnimation"
    )

    val groupsText = pluralStringResource(R.plurals.duplicates_groups_count, clustersCount, clustersCount)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(37.dp)
                .clip(RoundedCornerShape(19.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { onSummaryClick() }
        ) {
            val totalWidth = constraints.maxWidth.toFloat()
            val progressWidth = totalWidth * animatedProgress
            val fullWidth = maxWidth

            val density = LocalDensity.current
            val paddingPx = with(density) { 16.dp.toPx() }
            val infoWidthPx = with(density) { 54.dp.toPx() }
            val remainingWidthPx = with(density) { 40.dp.toPx() }

            val staticInfoX = totalWidth - infoWidthPx - paddingPx
            val minRequiredRoomForRemaining = remainingWidthPx + paddingPx * 2f
            val infoIsInside = progressWidth > staticInfoX

            val infoTranslationX by animateFloatAsState(
                targetValue = if (infoIsInside) {
                    (progressWidth - infoWidthPx - paddingPx).coerceAtLeast(paddingPx)
                } else {
                    staticInfoX
                },
                animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                label = "DuplicatesInfoTranslation"
            )

            val remainingTranslationX by animateFloatAsState(
                targetValue = if (progressWidth in paddingPx..minRequiredRoomForRemaining) {
                    (progressWidth + with(density) { 6.dp.toPx() })
                } else {
                    paddingPx
                },
                animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                label = "DuplicatesRemainingTranslation"
            )

            val remainingText = remainingCount.toString()

            // Layer 1: Dark Text (Background)
            Box(modifier = Modifier.fillMaxSize()) {
                RemainingInfo(
                    remainingText = remainingText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .graphicsLayer { translationX = remainingTranslationX }
                )

                HeaderInfo(
                    progressText = "${(progress * 100).toInt()}%",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .graphicsLayer { translationX = infoTranslationX }
                )
            }

            // Layer 2: Primary Bar Overlay with White Text
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .layout { measurable, constraints ->
                        val targetWidth = progressWidth.toInt().coerceIn(0, constraints.maxWidth)
                        val placeable = measurable.measure(
                            constraints.copy(
                                minWidth = targetWidth,
                                maxWidth = targetWidth
                            )
                        )
                        layout(targetWidth, placeable.height) {
                            placeable.placeRelative(0, 0)
                        }
                    }
                    .clipToBounds()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f)
                            )
                        )
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .wrapContentWidth(unbounded = true, align = Alignment.Start)
                        .requiredWidth(fullWidth)
                ) {
                    RemainingInfo(
                        remainingText = remainingText,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .graphicsLayer { translationX = remainingTranslationX }
                    )

                    HeaderInfo(
                        progressText = "${(progress * 100).toInt()}%",
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .graphicsLayer { translationX = infoTranslationX }
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatBadge(label = stringResource(R.string.swipe_keep), count = keepCount, color = MaterialGreen, shape = MaterialShapes.Cookie6Sided.toShape())
            StatBadge(label = stringResource(R.string.swipe_delete), count = deleteCount, color = MaterialRed, shape = MaterialShapes.Gem.toShape())
            StatBadge(text = groupsText, color = MaterialTheme.colorScheme.outline, shape = MaterialShapes.Flower.toShape())
        }
    }
}

@Composable
private fun RemainingInfo(
    remainingText: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Text(
        text = remainingText,
        style = MaterialTheme.typography.labelSmall,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = modifier
    )
}

@Composable
private fun HeaderInfo(
    progressText: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = progressText,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            color = color
        )
        Spacer(Modifier.width(4.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.List,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = color
        )
    }
}

@Composable
private fun StatBadge(label: String, count: Int, color: Color, shape: Shape) {
    StatBadge(text = "$count $label", color = color, shape = shape)
}

@Composable
private fun StatBadge(text: String, color: Color, shape: Shape) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(shape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DuplicatesBottomBar(
    uiState: DuplicatesUiState,
    viewModel: DuplicatesViewModel,
    onSortOrderSelected: (DuplicatesSortOrder) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            var sortMenuExpanded by remember { mutableStateOf(false) }

            val sortIcon = when (uiState.sortOrder) {
                DuplicatesSortOrder.CLUSTER_SIZE -> Icons.AutoMirrored.Filled.Sort
                DuplicatesSortOrder.PHOTOS_FIRST -> Icons.Default.Image
                DuplicatesSortOrder.VIDEOS_FIRST -> Icons.Default.Videocam
            }

            Box {
                FilledTonalIconButton(
                    onClick = { sortMenuExpanded = true },
                    modifier = Modifier.size(IconButtonDefaults.mediumContainerSize()),
                    shapes = IconButtonDefaults.shapes()
                ) {
                    Icon(
                        imageVector = sortIcon,
                        contentDescription = stringResource(R.string.duplicates_sort_button),
                        modifier = Modifier.size(IconButtonDefaults.mediumIconSize)
                    )
                }

                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false },
                    shape = RoundedCornerShape(20.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    shadowElevation = 0.dp, // The menu's alpha fade clips a shadow to its rectangular bounds, flashing grey corners.
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    SortPopupItem(
                        R.string.duplicates_sort_cluster_size,
                        Icons.AutoMirrored.Filled.Sort,
                        uiState.sortOrder == DuplicatesSortOrder.CLUSTER_SIZE
                    ) {
                        sortMenuExpanded = false
                        onSortOrderSelected(DuplicatesSortOrder.CLUSTER_SIZE)
                    }
                    SortPopupItem(
                        R.string.duplicates_sort_photos_first,
                        Icons.Default.Image,
                        uiState.sortOrder == DuplicatesSortOrder.PHOTOS_FIRST
                    ) {
                        sortMenuExpanded = false
                        onSortOrderSelected(DuplicatesSortOrder.PHOTOS_FIRST)
                    }
                    SortPopupItem(
                        R.string.duplicates_sort_videos_first,
                        Icons.Default.Videocam,
                        uiState.sortOrder == DuplicatesSortOrder.VIDEOS_FIRST
                    ) {
                        sortMenuExpanded = false
                        onSortOrderSelected(DuplicatesSortOrder.VIDEOS_FIRST)
                    }
                }
            }

            var selectMenuExpanded by remember { mutableStateOf(false) }

            val selectButtonText = when (uiState.selectedAutoSelectMode) {
                AutoSelectMode.KEEP_LARGEST -> stringResource(R.string.duplicates_select_largest)
                AutoSelectMode.KEEP_SMALLEST -> stringResource(R.string.duplicates_select_smallest)
                AutoSelectMode.KEEP_BEST_SHOT_AUTO -> stringResource(R.string.duplicates_select_best_shot_auto)
                AutoSelectMode.KEEP_BEST_SHOT_SINGLE -> stringResource(R.string.duplicates_select_best_shot_single)
                AutoSelectMode.KEEP_BEST_SHOT_TOP_2 -> stringResource(R.string.duplicates_select_best_shot_top_2)
                AutoSelectMode.KEEP_BEST_SHOT_TOP_3 -> stringResource(R.string.duplicates_select_best_shot_top_3)
                null -> stringResource(R.string.duplicates_select_button)
            }

            // Same icons as the matching menu items.
            val selectButtonIcon = when (uiState.selectedAutoSelectMode) {
                AutoSelectMode.KEEP_LARGEST -> Icons.Default.ExpandMore
                AutoSelectMode.KEEP_SMALLEST -> Icons.Default.ExpandLess
                else -> Icons.Default.AutoAwesome
            }

            Box {
                FilledTonalButton(
                    onClick = { selectMenuExpanded = true },
                    // Wide enough for the longest label so the button and its popup don't shift between modes.
                    modifier = Modifier.defaultMinSize(minWidth = 176.dp, minHeight = ButtonDefaults.MediumContainerHeight),
                    shapes = ButtonDefaults.shapes(),
                    contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight)
                ) {
                    Icon(
                        imageVector = selectButtonIcon,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.iconSizeFor(ButtonDefaults.MediumContainerHeight))
                    )
                    Spacer(Modifier.width(ButtonDefaults.iconSpacingFor(ButtonDefaults.MediumContainerHeight)))
                    Text(
                        text = selectButtonText,
                        fontWeight = FontWeight.Bold,
                        style = ButtonDefaults.textStyleFor(ButtonDefaults.MediumContainerHeight)
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null
                    )
                }

                DropdownMenu(
                    expanded = selectMenuExpanded,
                    onDismissRequest = { selectMenuExpanded = false },
                    shape = RoundedCornerShape(20.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    shadowElevation = 0.dp, // The menu's alpha fade clips a shadow to its rectangular bounds, flashing grey corners.
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    SortPopupItem(
                        R.string.duplicates_keep_largest,
                        Icons.Default.ExpandMore,
                        uiState.selectedAutoSelectMode == AutoSelectMode.KEEP_LARGEST
                    ) {
                        selectMenuExpanded = false
                        viewModel.autoSelect(keepLargest = true)
                    }
                    SortPopupItem(
                        R.string.duplicates_keep_smallest,
                        Icons.Default.ExpandLess,
                        uiState.selectedAutoSelectMode == AutoSelectMode.KEEP_SMALLEST
                    ) {
                        selectMenuExpanded = false
                        viewModel.autoSelect(keepLargest = false)
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                    SortPopupItem(
                        R.string.duplicates_best_shot_auto,
                        Icons.Default.AutoAwesome,
                        uiState.selectedAutoSelectMode == AutoSelectMode.KEEP_BEST_SHOT_AUTO
                    ) {
                        selectMenuExpanded = false
                        viewModel.autoSelectBestShot(BestShotAnalyzer.BestShotMode.AUTO)
                    }
                    SortPopupItem(
                        R.string.duplicates_best_shot_single,
                        Icons.Default.AutoAwesome,
                        uiState.selectedAutoSelectMode == AutoSelectMode.KEEP_BEST_SHOT_SINGLE
                    ) {
                        selectMenuExpanded = false
                        viewModel.autoSelectBestShot(BestShotAnalyzer.BestShotMode.SINGLE_BEST)
                    }
                    SortPopupItem(
                        R.string.duplicates_best_shot_top_2,
                        Icons.Default.AutoAwesome,
                        uiState.selectedAutoSelectMode == AutoSelectMode.KEEP_BEST_SHOT_TOP_2
                    ) {
                        selectMenuExpanded = false
                        viewModel.autoSelectBestShot(BestShotAnalyzer.BestShotMode.TOP_2)
                    }
                    SortPopupItem(
                        R.string.duplicates_best_shot_top_3,
                        Icons.Default.AutoAwesome,
                        uiState.selectedAutoSelectMode == AutoSelectMode.KEEP_BEST_SHOT_TOP_3
                    ) {
                        selectMenuExpanded = false
                        viewModel.autoSelectBestShot(BestShotAnalyzer.BestShotMode.TOP_3)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

/**
 * Pinch-to-peek for the duplicates grid. It runs on the whole screen instead of on each thumbnail, so every
 * finger counts: a pinch that starts on a thumbnail keeps zooming when fingers are lifted and put back down
 * anywhere, until all fingers are up. It listens in the Initial pass and consumes everything once active, so
 * the list and rows don't scroll and thumbnail taps don't fire mid-pinch.
 */
private fun Modifier.pinchToPeek(
    thumbnails: Map<String, Pair<Asset, Rect>>,
    windowOffset: () -> Offset,
    canStartAt: (Offset) -> Boolean,
    decisionOf: (Asset) -> DuplicateDecision,
    onZoom: (ZoomData) -> Unit,
): Modifier = pointerInput(Unit) {
    fun thumbnailAt(windowPoint: Offset): Asset? =
        if (!canStartAt(windowPoint)) null
        else thumbnails.values.firstOrNull { (_, bounds) -> bounds.contains(windowPoint) }?.first

    awaitEachGesture {
        val firstDown = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        var target: Asset? = null
        var lastBounds = Rect.Zero
        var scale = 1f
        var offset = Offset.Zero

        do {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val pressed = event.changes.filter { it.pressed }

            if (target == null && pressed.size >= 2) {
                val centroid = pressed.fold(Offset.Zero) { acc, change -> acc + change.position } / pressed.size.toFloat()
                target = thumbnailAt(centroid + windowOffset()) ?: thumbnailAt(firstDown.position + windowOffset())
            }

            val asset = target
            if (asset != null) {
                if (pressed.size >= 2) {
                    scale = (scale * event.calculateZoom()).coerceIn(1f, 4f)
                }
                val pan = event.calculatePan()
                val maxOffset = 250f * scale
                offset = Offset(
                    (offset.x + pan.x).coerceIn(-maxOffset, maxOffset),
                    (offset.y + pan.y).coerceIn(-maxOffset, maxOffset)
                )
                lastBounds = thumbnails[asset.id]?.second ?: lastBounds
                onZoom(ZoomData(asset, decisionOf(asset), lastBounds, scale, offset, isGestureActive = true))
                event.changes.forEach { it.consume() }
            }
        } while (event.changes.any { it.pressed })

        target?.let { asset ->
            lastBounds = thumbnails[asset.id]?.second ?: lastBounds
            onZoom(ZoomData(asset, decisionOf(asset), lastBounds, scale, offset, isGestureActive = false))
        }
    }
}

