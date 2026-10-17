package com.markvoronin.immichswipe.feature.duplicates

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.feature.duplicates.components.DuplicateClusterCard
import com.markvoronin.immichswipe.feature.duplicates.components.FullScreenPreviewData
import com.markvoronin.immichswipe.feature.duplicates.components.FullScreenPreviewModal
import com.markvoronin.immichswipe.feature.duplicates.components.InstagramZoomOverlay
import com.markvoronin.immichswipe.feature.duplicates.components.ZoomData
import com.markvoronin.immichswipe.feature.duplicates.components.formatSizeStr
import com.markvoronin.immichswipe.feature.settings.components.verticalFadingEdges
import com.markvoronin.immichswipe.feature.tutorial.LocalTutorialController
import com.markvoronin.immichswipe.feature.tutorial.ProvideTutorialController
import com.markvoronin.immichswipe.feature.tutorial.TutorialPhase
import com.markvoronin.immichswipe.feature.tutorial.TutorialStep
import com.markvoronin.immichswipe.feature.tutorial.rememberTutorialController
import com.markvoronin.immichswipe.feature.tutorial.tutorialTarget
import com.markvoronin.immichswipe.ui.theme.VirtualGold
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Composable
fun DuplicatesScreen(
    viewModel: DuplicatesViewModel,
    modifier: Modifier = Modifier,
    resetSignal: Flow<Unit>? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    var activeZoomData by remember { mutableStateOf<ZoomData?>(null) }
    var fullScreenPreviewData by remember { mutableStateOf<FullScreenPreviewData?>(null) }
    var activeClusterIndex by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var rootWindowOffset by remember { mutableStateOf(Offset.Zero) }
    var showResetDialog by remember { mutableStateOf(false) }

    LaunchedEffect(resetSignal) {
        resetSignal?.collect {
            showResetDialog = true
        }
    }

    val tutorialController = LocalTutorialController.current
        ?: rememberTutorialController()

    val duplicatesTutorialSteps = remember {
        listOf(
            TutorialStep(
                id = "clusters",
                titleRes = R.string.tutorial_duplicates_clusters_title,
                descriptionRes = R.string.tutorial_duplicates_clusters_desc,
                targetKey = "dup_cluster"
            ),
            TutorialStep(
                id = "selection",
                titleRes = R.string.tutorial_duplicates_selection_title,
                descriptionRes = R.string.tutorial_duplicates_selection_desc,
                targetKey = "dup_photos"
            ),
            TutorialStep(
                id = "action",
                titleRes = R.string.tutorial_duplicates_action_title,
                descriptionRes = R.string.tutorial_duplicates_action_desc,
                targetKey = "dup_delete"
            )
        )
    }

    LaunchedEffect(uiState.hasCompletedDuplicatesTutorial, uiState.isLoading) {
        if (!uiState.hasCompletedDuplicatesTutorial && !uiState.isLoading) {
            tutorialController.startTutorial(
                phase = TutorialPhase.DUPLICATES,
                steps = duplicatesTutorialSteps,
                onComplete = { viewModel.completeDuplicatesTutorial() }
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (tutorialController.phase == TutorialPhase.DUPLICATES && tutorialController.isVisible) {
                tutorialController.dismissTutorial()
            }
        }
    }

    val itemsToDelete = remember(uiState.decisions, uiState.clusters) {
        val deleteAssetIds = uiState.decisions.filterValues { it == DuplicateDecision.DELETE }.keys
        uiState.clusters.flatMap { it.assets }.filter { it.id in deleteAssetIds }
    }
    val deleteCount = itemsToDelete.size
    val deleteBytes = itemsToDelete.sumOf { it.exifInfo?.fileSizeInBytes ?: 0L }

    ProvideTutorialController(tutorialController) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .onGloballyPositioned { coords ->
                    rootWindowOffset = coords.positionInWindow()
                }
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
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
                        text = "No duplicates found!",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = pluralStringResource(
                                R.plurals.duplicates_groups_count,
                                uiState.clusters.size,
                                uiState.clusters.size
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            var selectMenuExpanded by remember { mutableStateOf(false) }

                            val selectButtonText = when (uiState.selectedAutoSelectMode) {
                                AutoSelectMode.KEEP_LARGEST -> stringResource(R.string.duplicates_keep_largest)
                                AutoSelectMode.KEEP_SMALLEST -> stringResource(R.string.duplicates_keep_smallest)
                                AutoSelectMode.KEEP_BEST_SHOT_AUTO -> stringResource(R.string.duplicates_best_shot_auto)
                                AutoSelectMode.KEEP_BEST_SHOT_SINGLE -> stringResource(R.string.duplicates_best_shot_single)
                                AutoSelectMode.KEEP_BEST_SHOT_TOP_2 -> stringResource(R.string.duplicates_best_shot_top_2)
                                AutoSelectMode.KEEP_BEST_SHOT_TOP_3 -> stringResource(R.string.duplicates_best_shot_top_3)
                                null -> stringResource(R.string.duplicates_select_button)
                            }

                            Box {
                                Surface(
                                    onClick = { selectMenuExpanded = true },
                                    color = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    tonalElevation = 2.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = selectButtonText,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                        Spacer(Modifier.width(2.dp))
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = selectMenuExpanded,
                                    onDismissRequest = { selectMenuExpanded = false },
                                    shape = RoundedCornerShape(20.dp),
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 3.dp,
                                    shadowElevation = 8.dp,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.duplicates_keep_largest)) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.ExpandMore,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        trailingIcon = if (uiState.selectedAutoSelectMode == AutoSelectMode.KEEP_LARGEST) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        } else null,
                                        onClick = {
                                            selectMenuExpanded = false
                                            viewModel.autoSelect(keepLargest = true)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.duplicates_keep_smallest)) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.ExpandLess,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        trailingIcon = if (uiState.selectedAutoSelectMode == AutoSelectMode.KEEP_SMALLEST) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        } else null,
                                        onClick = {
                                            selectMenuExpanded = false
                                            viewModel.autoSelect(keepLargest = false)
                                        }
                                    )
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.duplicates_best_shot_auto)) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        trailingIcon = if (uiState.selectedAutoSelectMode == AutoSelectMode.KEEP_BEST_SHOT_AUTO) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        } else null,
                                        onClick = {
                                            selectMenuExpanded = false
                                            viewModel.autoSelectBestShot(BestShotAnalyzer.BestShotMode.AUTO)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.duplicates_best_shot_single)) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        trailingIcon = if (uiState.selectedAutoSelectMode == AutoSelectMode.KEEP_BEST_SHOT_SINGLE) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        } else null,
                                        onClick = {
                                            selectMenuExpanded = false
                                            viewModel.autoSelectBestShot(BestShotAnalyzer.BestShotMode.SINGLE_BEST)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.duplicates_best_shot_top_2)) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        trailingIcon = if (uiState.selectedAutoSelectMode == AutoSelectMode.KEEP_BEST_SHOT_TOP_2) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        } else null,
                                        onClick = {
                                            selectMenuExpanded = false
                                            viewModel.autoSelectBestShot(BestShotAnalyzer.BestShotMode.TOP_2)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.duplicates_best_shot_top_3)) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        trailingIcon = if (uiState.selectedAutoSelectMode == AutoSelectMode.KEEP_BEST_SHOT_TOP_3) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        } else null,
                                        onClick = {
                                            selectMenuExpanded = false
                                            viewModel.autoSelectBestShot(BestShotAnalyzer.BestShotMode.TOP_3)
                                        }
                                    )
                                }
                            }

                            val containerColor by animateColorAsState(
                                targetValue = if (deleteCount > 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface,
                                animationSpec = tween(durationMillis = 200),
                                label = "DeleteButtonContainerColor"
                            )
                            val contentColor by animateColorAsState(
                                targetValue = if (deleteCount > 0) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface,
                                animationSpec = tween(durationMillis = 200),
                                label = "DeleteButtonContentColor"
                            )

                            Box(
                                modifier = Modifier.tutorialTarget("dup_delete", tutorialController),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Surface(
                                    onClick = { if (deleteCount > 0) viewModel.toggleDeleteConfirmation(true) },
                                    enabled = deleteCount > 0,
                                    color = containerColor,
                                    contentColor = contentColor,
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (deleteCount > 0) MaterialTheme.colorScheme.error.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    tonalElevation = 2.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteForever,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = if (deleteCount > 1) {
                                                "Delete ($deleteCount)"
                                            } else {
                                                stringResource(R.string.swipe_delete)
                                            },
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }
                            }
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .verticalFadingEdges(listState, length = 32.dp)
                            .navigationBarsPadding(),
                        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        itemsIndexed(uiState.clusters, key = { _, item -> item.clusterId }) { index, cluster ->
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
                                    apiKey = uiState.apiKey,
                                    modifier = if (index == 0) Modifier.tutorialTarget("dup_photos", tutorialController) else Modifier,
                                    onDecisionToggle = { assetId -> viewModel.toggleDecision(assetId) },
                                    onKeepAll = { viewModel.setClusterDecisions(cluster, DuplicateDecision.KEEP) },
                                    onDeleteAll = { viewModel.setClusterDecisions(cluster, DuplicateDecision.DELETE) },
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
                                    onZoomStateUpdate = { zoomData ->
                                        activeZoomData = zoomData
                                    }
                                )
                            }
                        }
                    }
                }
            }

            InstagramZoomOverlay(
                zoomData = activeZoomData,
                decision = activeZoomData?.let { uiState.decisions[it.asset.id] ?: DuplicateDecision.NONE } ?: DuplicateDecision.NONE,
                isFavorite = activeZoomData?.let { uiState.isFavorite(it.asset) } ?: false,
                rootWindowOffset = rootWindowOffset,
                baseUrl = uiState.baseUrl,
                apiKey = uiState.apiKey,
                onDismiss = { activeZoomData = null }
            )

            FullScreenPreviewModal(
                previewData = fullScreenPreviewData,
                decisions = uiState.decisions,
                isFavorite = { uiState.isFavorite(it) },
                baseUrl = uiState.baseUrl,
                apiKey = uiState.apiKey,
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
                            val visibleItem = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == targetIndex }
                            val viewportHeight = listState.layoutInfo.viewportSize.height - listState.layoutInfo.beforeContentPadding - listState.layoutInfo.afterContentPadding

                            if (visibleItem != null && viewportHeight > 0) {
                                val itemHeight = visibleItem.size
                                val targetOffset = -((viewportHeight - itemHeight) / 2)
                                listState.animateScrollToItem(targetIndex, targetOffset)
                            } else {
                                listState.scrollToItem(targetIndex, 0)
                                val item = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == targetIndex }
                                val vHeight = listState.layoutInfo.viewportSize.height - listState.layoutInfo.beforeContentPadding - listState.layoutInfo.afterContentPadding
                                val itemHeight = item?.size ?: 0
                                if (vHeight > 0 && itemHeight > 0) {
                                    val targetOffset = -((vHeight - itemHeight) / 2)
                                    listState.scrollToItem(targetIndex, targetOffset)
                                }
                            }
                        }
                    }
                }
            )

            if (showResetDialog) {
                AlertDialog(
                    onDismissRequest = { showResetDialog = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(stringResource(R.string.duplicates_reset_confirm_title))
                        }
                    },
                    text = { Text(stringResource(R.string.duplicates_reset_confirm_msg)) },
                    confirmButton = {
                        Button(
                            onClick = {
                                showResetDialog = false
                                viewModel.resetDecisions()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(stringResource(R.string.common_confirm))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showResetDialog = false }) {
                            Text(stringResource(R.string.common_cancel))
                        }
                    },
                    shape = RoundedCornerShape(24.dp)
                )
            }

            if (uiState.showDeleteConfirmation) {
                AlertDialog(
                    onDismissRequest = { viewModel.toggleDeleteConfirmation(false) },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(stringResource(R.string.duplicates_delete_confirm_title))
                        }
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
                        Button(
                            onClick = {
                                viewModel.toggleDeleteConfirmation(false)
                                viewModel.syncDeletions()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(stringResource(R.string.swipe_delete))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.toggleDeleteConfirmation(false) }) {
                            Text(stringResource(R.string.common_cancel))
                        }
                    },
                    shape = RoundedCornerShape(24.dp)
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
                            CircularProgressIndicator()
                            Text(
                                text = stringResource(R.string.swipe_syncing),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }

            if (uiState.isAnalyzingAi) {
                Dialog(onDismissRequest = {}) {
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
                            CircularProgressIndicator()
                            Text(
                                text = stringResource(R.string.duplicates_ai_analyzing),
                                style = MaterialTheme.typography.titleMedium
                            )
                            val progress = uiState.aiProgress
                            if (progress != null) {
                                Text(
                                    text = stringResource(
                                        R.string.duplicates_ai_cluster_progress,
                                        progress.first,
                                        progress.second
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
