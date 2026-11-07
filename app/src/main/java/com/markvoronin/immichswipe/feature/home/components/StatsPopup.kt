package com.markvoronin.immichswipe.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LibraryAddCheck
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            val scrollState = rememberScrollState()

            Column(modifier = Modifier.fillMaxSize()) {
            // Header: sits outside the scrolling content so it stays pinned
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.common_close), modifier = Modifier.size(28.dp))
                }
                Text(
                    text = stringResource(R.string.stats_popup_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
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
                Spacer(Modifier.height(12.dp))

                StatsSectionHeader(
                    title = stringResource(R.string.stats_section_global),
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(Modifier.height(4.dp))

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
                            badgeColor = MaterialTheme.colorScheme.errorContainer,
                            badgeContentColor = MaterialTheme.colorScheme.onErrorContainer,
                            shape = gridTileShape(row = 0, column = 0),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = stringResource(R.string.stats_bytes_saved),
                            value = formatSize(stats.totalBytesSaved),
                            icon = Icons.Default.CloudDone,
                            badgeColor = MaterialTheme.colorScheme.secondaryContainer,
                            badgeContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
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
                            badgeColor = MaterialTheme.colorScheme.primaryContainer,
                            badgeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            shape = gridTileShape(row = 1, column = 0),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = stringResource(R.string.stats_albums_completed),
                            value = "${stats.completedAlbums} / ${stats.totalAlbums}",
                            icon = Icons.Default.LibraryAddCheck,
                            badgeColor = MaterialTheme.colorScheme.tertiaryContainer,
                            badgeContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            shape = gridTileShape(row = 1, column = 1),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                StatsSectionHeader(
                    title = stringResource(R.string.stats_section_weekly),
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(Modifier.height(4.dp))

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

                Spacer(Modifier.height(24.dp))

                StatsSectionHeader(
                    title = stringResource(R.string.stats_distribution_title),
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(Modifier.height(8.dp))

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
internal fun gridTileShape(row: Int, column: Int, outer: Dp = 24.dp, inner: Dp = 6.dp) = RoundedCornerShape(
    topStart = if (row == 0 && column == 0) outer else inner,
    topEnd = if (row == 0 && column == 1) outer else inner,
    bottomStart = if (row == 1 && column == 0) outer else inner,
    bottomEnd = if (row == 1 && column == 1) outer else inner
)

/** Section title styled like the settings section headers: primary dot + uppercase label. */
@Composable
private fun StatsSectionHeader(title: String, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(horizontal = 4.dp, vertical = 8.dp)
    ) {
        Spacer(
            Modifier
                .size(8.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

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
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StatCard(
    label: String,
    value: String,
    icon: ImageVector,
    badgeColor: Color,
    badgeContentColor: Color,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp)
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        // Compact two-row layout (same as the sorting summary): badge + value on top, label centered below.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Neutral tile; the expressive cookie badge carries the only accent color.
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(badgeColor, MaterialShapes.Cookie9Sided.toShape()),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = badgeContentColor, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(8.dp))
                // Shrinks long values (e.g. "147.37 MB") so they fit beside the badge.
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.End),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = 14.sp,
                        maxFontSize = MaterialTheme.typography.headlineSmall.fontSize
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
            Text(text = label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
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
            Text(text = label, style = MaterialTheme.typography.labelLarge)
            Text(text = "${(percentValue * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { percentValue },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            drawStopIndicator = {}
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
