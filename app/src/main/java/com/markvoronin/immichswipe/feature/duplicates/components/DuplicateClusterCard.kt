package com.markvoronin.immichswipe.feature.duplicates.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.domain.model.Asset
import com.markvoronin.immichswipe.feature.duplicates.DuplicateClusterUiModel
import com.markvoronin.immichswipe.feature.duplicates.DuplicateDecision
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun DuplicateClusterCard(
    cluster: DuplicateClusterUiModel,
    decisions: Map<String, DuplicateDecision>,
    activeZoomAssetId: String?,
    modifier: Modifier = Modifier,
    baseUrl: String = "",
    onDecisionToggle: (String) -> Unit,
    onKeepAll: () -> Unit = {},
    onDeleteAll: () -> Unit = {},
    onAssetLongPress: (Asset) -> Unit,
    onZoomStateUpdate: (ZoomData?) -> Unit,
    isFavorite: (Asset) -> Boolean = { it.isFavorite },
    onOpenFullScreen: () -> Unit = {
        cluster.assets.firstOrNull()?.let { onAssetLongPress(it) }
    }
) {
    val unsortedInCluster = cluster.assets.count { asset ->
        decisions[asset.id] == null || decisions[asset.id] == DuplicateDecision.NONE
    }
    val isSorted = unsortedInCluster == 0
    val sortedInCluster = cluster.assets.size - unsortedInCluster
    val isAllKeep = cluster.assets.isNotEmpty() && cluster.assets.all { decisions[it.id] == DuplicateDecision.KEEP }
    val isAllDelete = cluster.assets.isNotEmpty() && cluster.assets.all { decisions[it.id] == DuplicateDecision.DELETE }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = if (isSorted) {
            BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.85f))
        } else {
            BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
        }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f))

                Text(
                    text = "$sortedInCluster/${cluster.assets.size}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    IconButton(
                        onClick = onOpenFullScreen,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = stringResource(R.string.settings_fullscreen_pos_label),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
                val availableWidth = maxWidth
                if (cluster.assets.size <= 2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        cluster.assets.forEach { asset ->
                            val decision = decisions[asset.id] ?: DuplicateDecision.NONE
                            val isBeingZoomed = activeZoomAssetId == asset.id
                            DuplicateAssetItem(
                                asset = asset,
                                decision = decision,
                                isFavorite = isFavorite(asset),
                                isBeingZoomed = isBeingZoomed,
                                baseUrl = baseUrl,
                                modifier = Modifier.weight(1f),
                                onToggle = { onDecisionToggle(asset.id) },
                                onLongPress = { onAssetLongPress(asset) },
                                onZoomStateUpdate = onZoomStateUpdate
                            )
                        }
                    }
                } else {
                    val itemWidth = ((availableWidth - 8.dp) / 2.15f).coerceAtLeast(135.dp)
                    val rowState = rememberLazyListState()
                    val flingBehavior = ScrollableDefaults.flingBehavior()
                    LazyRow(
                        state = rowState,
                        userScrollEnabled = false,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalRowDrag(rowState, flingBehavior)
                    ) {
                        items(cluster.assets, key = { it.id }) { asset ->
                            val decision = decisions[asset.id] ?: DuplicateDecision.NONE
                            val isBeingZoomed = activeZoomAssetId == asset.id
                            DuplicateAssetItem(
                                asset = asset,
                                decision = decision,
                                isFavorite = isFavorite(asset),
                                isBeingZoomed = isBeingZoomed,
                                baseUrl = baseUrl,
                                modifier = Modifier.width(itemWidth),
                                onToggle = { onDecisionToggle(asset.id) },
                                onLongPress = { onAssetLongPress(asset) },
                                onZoomStateUpdate = onZoomStateUpdate
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    onClick = onKeepAll,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isAllKeep) Color(0xFF4CAF50) else Color(0xFF4CAF50).copy(alpha = 0.15f),
                    contentColor = if (isAllKeep) Color.White else Color(0xFF4CAF50)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.duplicates_keep_all),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Surface(
                    onClick = onDeleteAll,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isAllDelete) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                    contentColor = if (isAllDelete) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.error
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.duplicates_delete_all),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Drives [rowState] from raw pointer events so a swipe that leans horizontal always scrolls the row.
 * The built-in row scroll gives up once its parent consumes an event, which happens when the parent
 * list is still flinging (it grabs the touch immediately) or crosses its vertical slop first.
 * Consuming the moves here also cancels the parent's drag. Pair it with `userScrollEnabled = false`.
 */
private fun Modifier.horizontalRowDrag(
    rowState: LazyListState,
    flingBehavior: FlingBehavior,
    maxVerticalToHorizontalRatio: Float = 1.5f
): Modifier = pointerInput(rowState, flingBehavior) {
    val decisionDistance = viewConfiguration.touchSlop * 0.4f
    coroutineScope {
        var flingJob: Job? = null
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            flingJob?.cancel()
            val velocityTracker = VelocityTracker()
            velocityTracker.addPosition(down.uptimeMillis, down.position)
            var total = Offset.Zero
            var horizontal: Boolean? = null
            var cancelled = false

            do {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id }
                if (change == null || event.changes.count { it.pressed } > 1) {
                    cancelled = true
                    break
                }
                val delta = change.positionChangeIgnoreConsumed()

                if (horizontal == null) {
                    total += delta
                    if (total.getDistance() >= decisionDistance) {
                        horizontal = abs(total.y) <= abs(total.x) * maxVerticalToHorizontalRatio
                    }
                }
                if (horizontal == true) {
                    velocityTracker.addPosition(change.uptimeMillis, change.position)
                    rowState.dispatchRawDelta(-delta.x)
                    change.consume()
                }
            } while (event.changes.any { it.pressed })

            if (horizontal == true && !cancelled) {
                val velocity = -velocityTracker.calculateVelocity().x
                flingJob = launch {
                    rowState.scroll { with(flingBehavior) { performFling(velocity) } }
                }
            }
        }
    }
}
