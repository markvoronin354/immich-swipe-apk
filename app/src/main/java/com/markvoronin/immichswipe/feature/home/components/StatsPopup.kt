package com.markvoronin.immichswipe.feature.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LibraryAddCheck
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.feature.home.StatsUiData
import com.markvoronin.immichswipe.feature.settings.components.verticalFadingEdges
import com.markvoronin.immichswipe.feature.swipe.components.connectedTileShape

@Composable
fun StatsPopup(
    stats: StatsUiData,
    onClose: () -> Unit
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.9f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            val scrollState = rememberScrollState()

            Column(modifier = Modifier.fillMaxSize()) {
            // Header: sits outside the scrolling content so it stays pinned
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.common_close))
                }
                Text(
                    text = stringResource(R.string.stats_popup_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(48.dp))
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalFadingEdges(scrollState, length = 32.dp)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.stats_section_global),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(Modifier.height(12.dp))

                // Connected 2x2 group: tiles sit close with a small gap; only the group's outer corners are fully rounded.
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        StatCard(
                            label = stringResource(R.string.stats_deleted_count),
                            value = stats.totalDeleted.toString(),
                            icon = Icons.Default.Delete,
                            color = MaterialTheme.colorScheme.error,
                            shape = gridTileShape(row = 0, column = 0),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = stringResource(R.string.stats_bytes_saved),
                            value = formatSize(stats.totalBytesSaved),
                            icon = Icons.Default.CloudDone,
                            color = Color(0xFF388E3C),
                            shape = gridTileShape(row = 0, column = 1),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        StatCard(
                            label = stringResource(R.string.stats_swiped_count),
                            value = stats.totalSwiped.toString(),
                            icon = Icons.Default.Swipe,
                            color = MaterialTheme.colorScheme.primary,
                            shape = gridTileShape(row = 1, column = 0),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = stringResource(R.string.stats_albums_completed),
                            value = "${stats.completedAlbums} / ${stats.totalAlbums}",
                            icon = Icons.Default.LibraryAddCheck,
                            color = MaterialTheme.colorScheme.tertiary,
                            shape = gridTileShape(row = 1, column = 1),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.stats_section_weekly),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    WeeklyStatTile(
                        label = stringResource(R.string.stats_deleted_count),
                        value = stats.weeklyDeleted.toString(),
                        shape = connectedTileShape(index = 0, count = 2, outer = 24.dp),
                        modifier = Modifier.weight(1f)
                    )
                    WeeklyStatTile(
                        label = stringResource(R.string.stats_bytes_saved),
                        value = formatSize(stats.weeklyBytesSaved),
                        shape = connectedTileShape(index = 1, count = 2, outer = 24.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(32.dp))

                Text(
                    text = stringResource(R.string.stats_distribution_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(Modifier.height(16.dp))

                val distribution = stats.distribution
                if (distribution.isEmpty()) {
                    Text(
                        text = stringResource(R.string.stats_no_data),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        DistributionBar(label = stringResource(R.string.swipe_keep), percentValue = distribution["KEEP"] ?: 0f, color = Color(0xFF4CAF50))
                        DistributionBar(label = stringResource(R.string.swipe_delete), percentValue = distribution["DELETE"] ?: 0f, color = Color(0xFFF44336))
                        DistributionBar(label = stringResource(R.string.swipe_archive), percentValue = distribution["ARCHIVE"] ?: 0f, color = Color(0xFFFF9800))
                        DistributionBar(label = stringResource(R.string.swipe_locked), percentValue = distribution["LOCK"] ?: 0f, color = Color(0xFF9C27B0))
                    }
                }
                
                Spacer(Modifier.height(24.dp))
            }
            }
        }
    }
}

/** Shape of a tile in a 2x2 group: a corner is fully rounded only when no other tile touches it. */
private fun gridTileShape(row: Int, column: Int, outer: Dp = 24.dp, inner: Dp = 6.dp) = RoundedCornerShape(
    topStart = if (row == 0 && column == 0) outer else inner,
    topEnd = if (row == 0 && column == 1) outer else inner,
    bottomStart = if (row == 1 && column == 0) outer else inner,
    bottomEnd = if (row == 1 && column == 1) outer else inner
)

@Composable
private fun WeeklyStatTile(
    label: String,
    value: String,
    shape: Shape,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp)
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(8.dp))
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = color)
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.8f))
        }
    }
}

@Composable
fun DistributionBar(label: String, percentValue: Float, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.labelMedium)
            Text(text = "${(percentValue * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { percentValue },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            color = color,
            trackColor = color.copy(alpha = 0.1f)
        )
    }
}

@Composable
fun formatSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    
    return when {
        gb >= 1.0 -> stringResource(R.string.size_unit_gb, gb)
        mb >= 1.0 -> stringResource(R.string.size_unit_mb, mb)
        else -> stringResource(R.string.size_unit_kb, kb)
    }
}
