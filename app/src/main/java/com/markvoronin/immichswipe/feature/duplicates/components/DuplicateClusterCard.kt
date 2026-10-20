package com.markvoronin.immichswipe.feature.duplicates.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.domain.model.Asset
import com.markvoronin.immichswipe.feature.duplicates.DuplicateClusterUiModel
import com.markvoronin.immichswipe.feature.duplicates.DuplicateDecision

@Composable
fun DuplicateClusterCard(
    cluster: DuplicateClusterUiModel,
    decisions: Map<String, DuplicateDecision>,
    activeZoomAssetId: String?,
    modifier: Modifier = Modifier,
    baseUrl: String = "",
    apiKey: String = "",
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
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "$sortedInCluster/${cluster.assets.size}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = onKeepAll,
                        shape = RoundedCornerShape(12.dp),
                        color = if (isAllKeep) Color(0xFF4CAF50) else Color(0xFF4CAF50).copy(alpha = 0.15f),
                        contentColor = if (isAllKeep) Color.White else Color(0xFF4CAF50)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = stringResource(R.string.duplicates_keep_all),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Surface(
                        onClick = onDeleteAll,
                        shape = RoundedCornerShape(12.dp),
                        color = if (isAllDelete) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                        contentColor = if (isAllDelete) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.error
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = stringResource(R.string.duplicates_delete_all),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

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
                                apiKey = apiKey,
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
                    LazyRow(
                        state = rowState,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
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
                                apiKey = apiKey,
                                modifier = Modifier.width(itemWidth),
                                onToggle = { onDecisionToggle(asset.id) },
                                onLongPress = { onAssetLongPress(asset) },
                                onZoomStateUpdate = onZoomStateUpdate
                            )
                        }
                    }
                }
            }
        }
    }
}
