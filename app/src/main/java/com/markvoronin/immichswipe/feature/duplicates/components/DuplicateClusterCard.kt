package com.markvoronin.immichswipe.feature.duplicates.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.domain.model.Asset
import com.markvoronin.immichswipe.feature.duplicates.DuplicateClusterUiModel
import com.markvoronin.immichswipe.feature.duplicates.DuplicateDecision
import com.markvoronin.immichswipe.feature.settings.components.animatedCookieShape
import com.markvoronin.immichswipe.feature.settings.components.unselectedTileColor
import com.markvoronin.immichswipe.feature.swipe.components.checkedWidthFactor
import com.markvoronin.immichswipe.feature.swipe.components.connectedTileShape
import com.markvoronin.immichswipe.feature.swipe.utils.MaterialGreen
import com.markvoronin.immichswipe.feature.swipe.utils.MaterialRed
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

internal val ClusterCardPadding = 12.dp
internal val ClusterPhotoGap = 4.dp
private val ClusterHeaderHeight = 48.dp
private val ClusterButtonHeight = 48.dp

/** Everything in the card but the photos: padding, header, photo captions (labelLarge + bodySmall) and buttons. */
internal val ClusterCardChromeHeight =
    ClusterCardPadding * 2 + ClusterHeaderHeight + 12.dp + 8.dp + 36.dp + 12.dp + ClusterButtonHeight

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DuplicateClusterCard(
    cluster: DuplicateClusterUiModel,
    decisions: Map<String, DuplicateDecision>,
    activeZoomAssetId: String?,
    modifier: Modifier = Modifier,
    photosModifier: Modifier = Modifier,
    baseUrl: String = "",
    onDecisionToggle: (String) -> Unit,
    onKeepAll: () -> Unit = {},
    onDeleteAll: () -> Unit = {},
    onAnalyze: () -> Unit = {},
    onAssetLongPress: (Asset) -> Unit,
    onThumbnailBoundsChanged: (Asset, Rect?) -> Unit,
    isFavorite: (Asset) -> Boolean = { it.isFavorite },
    onOpenFullScreen: () -> Unit = {
        cluster.assets.firstOrNull()?.let { onAssetLongPress(it) }
    }
) {
    val sortedInCluster = cluster.assets.count { asset ->
        (decisions[asset.id] ?: DuplicateDecision.NONE) != DuplicateDecision.NONE
    }
    val isSorted = sortedInCluster == cluster.assets.size
    val isAllKeep = cluster.assets.isNotEmpty() && cluster.assets.all { decisions[it.id] == DuplicateDecision.KEEP }
    val isAllDelete = cluster.assets.isNotEmpty() && cluster.assets.all { decisions[it.id] == DuplicateDecision.DELETE }
    val totalBytes = cluster.assets.sumOf { it.exifInfo?.fileSizeInBytes ?: 0L }
    val canAnalyze = cluster.assets.any { !it.isVideo }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(modifier = Modifier.padding(ClusterCardPadding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ClusterHeaderHeight)
                    .padding(start = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ClusterStatusBadge(isSorted = isSorted)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.duplicates_cluster_progress, sortedInCluster, cluster.assets.size),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatSizeStr(totalBytes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Spacer(Modifier.width(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    val tileCount = if (canAnalyze) 2 else 1
                    if (canAnalyze) {
                        ClusterIconTile(
                            onClick = onAnalyze,
                            shape = connectedTileShape(0, tileCount, outer = 20.dp),
                            icon = Icons.Default.AutoAwesome,
                            contentDescription = stringResource(R.string.duplicates_deep_analyze)
                        )
                    }
                    ClusterIconTile(
                        onClick = onOpenFullScreen,
                        shape = connectedTileShape(tileCount - 1, tileCount, outer = 20.dp),
                        icon = Icons.Default.Fullscreen,
                        contentDescription = stringResource(R.string.settings_fullscreen_pos_label)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            BoxWithConstraints(modifier = photosModifier.fillMaxWidth()) {
                val availableWidth = maxWidth
                val lastIndex = cluster.assets.lastIndex
                if (cluster.assets.size <= 2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ClusterPhotoGap)
                    ) {
                        cluster.assets.forEachIndexed { index, asset ->
                            DuplicateAssetItem(
                                asset = asset,
                                decision = decisions[asset.id] ?: DuplicateDecision.NONE,
                                isFavorite = isFavorite(asset),
                                isBeingZoomed = activeZoomAssetId == asset.id,
                                isFirst = index == 0,
                                isLast = index == lastIndex,
                                baseUrl = baseUrl,
                                modifier = Modifier.weight(1f),
                                onToggle = { onDecisionToggle(asset.id) },
                                onLongPress = { onAssetLongPress(asset) },
                                onBoundsChanged = { bounds -> onThumbnailBoundsChanged(asset, bounds) }
                            )
                        }
                    }
                } else {
                    val itemWidth = ((availableWidth - ClusterPhotoGap) / 2.15f).coerceAtLeast(135.dp)
                    val rowState = rememberLazyListState()
                    val flingBehavior = ScrollableDefaults.flingBehavior()
                    LazyRow(
                        state = rowState,
                        userScrollEnabled = false,
                        horizontalArrangement = Arrangement.spacedBy(ClusterPhotoGap),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalRowDrag(rowState, flingBehavior)
                    ) {
                        itemsIndexed(cluster.assets, key = { _, asset -> asset.id }) { index, asset ->
                            DuplicateAssetItem(
                                asset = asset,
                                decision = decisions[asset.id] ?: DuplicateDecision.NONE,
                                isFavorite = isFavorite(asset),
                                isBeingZoomed = activeZoomAssetId == asset.id,
                                isFirst = index == 0,
                                isLast = index == lastIndex,
                                baseUrl = baseUrl,
                                modifier = Modifier.width(itemWidth),
                                onToggle = { onDecisionToggle(asset.id) },
                                onLongPress = { onAssetLongPress(asset) },
                                onBoundsChanged = { bounds -> onThumbnailBoundsChanged(asset, bounds) }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // A connected button group: the checked button fills with its decision color, widens and morphs into a pill.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                ClusterDecisionButton(
                    checked = isAllKeep,
                    onClick = onKeepAll,
                    index = 0,
                    label = stringResource(R.string.duplicates_keep_all),
                    icon = Icons.Default.DoneAll,
                    checkedColor = MaterialGreen
                )
                ClusterDecisionButton(
                    checked = isAllDelete,
                    onClick = onDeleteAll,
                    index = 1,
                    label = stringResource(R.string.duplicates_delete_all),
                    icon = Icons.Default.DeleteSweep,
                    checkedColor = MaterialRed
                )
            }
        }
    }
}

/** Neutral circle while the group has undecided photos; a green cookie with a check once all are decided. */
@Composable
private fun ClusterStatusBadge(isSorted: Boolean) {
    val motionScheme = MaterialTheme.motionScheme
    val containerColor by animateColorAsState(
        targetValue = if (isSorted) MaterialGreen else unselectedTileColor(),
        animationSpec = motionScheme.defaultEffectsSpec(),
        label = "statusContainer"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSorted) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = motionScheme.defaultEffectsSpec(),
        label = "statusContent"
    )
    val rotation by animateFloatAsState(
        targetValue = if (isSorted) 90f else 0f,
        animationSpec = motionScheme.slowSpatialSpec(),
        label = "statusRotation"
    )
    val shape = animatedCookieShape(isSorted, motionScheme.defaultSpatialSpec())
    Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer { rotationZ = rotation }
                .background(containerColor, shape)
        )
        Crossfade(targetState = isSorted, animationSpec = motionScheme.fastEffectsSpec(), label = "statusIcon") { sorted ->
            Icon(
                imageVector = if (sorted) Icons.Default.Check else Icons.Default.ContentCopy,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ClusterIconTile(
    onClick: () -> Unit,
    shape: Shape,
    icon: ImageVector,
    contentDescription: String
) {
    FilledTonalIconButton(
        onClick = onClick,
        shapes = IconButtonDefaults.shapes(shape = shape, pressedShape = RoundedCornerShape(10.dp)),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = unselectedTileColor(),
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = Modifier.size(IconButtonDefaults.smallContainerSize(IconButtonDefaults.IconButtonWidthOption.Wide))
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(IconButtonDefaults.smallIconSize)
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RowScope.ClusterDecisionButton(
    checked: Boolean,
    onClick: () -> Unit,
    index: Int,
    label: String,
    icon: ImageVector,
    checkedColor: Color
) {
    ToggleButton(
        checked = checked,
        onCheckedChange = { onClick() },
        shapes = ToggleButtonShapes(
            shape = connectedTileShape(index, 2, outer = ClusterButtonHeight / 2),
            pressedShape = RoundedCornerShape(10.dp),
            checkedShape = CircleShape
        ),
        colors = ToggleButtonDefaults.colors(
            containerColor = unselectedTileColor(),
            contentColor = MaterialTheme.colorScheme.onSurface,
            checkedContainerColor = checkedColor,
            checkedContentColor = Color.White
        ),
        modifier = Modifier
            .weight(checkedWidthFactor(checked))
            .height(ClusterButtonHeight),
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.iconSizeFor(ClusterButtonHeight))
        )
        Spacer(Modifier.width(ButtonDefaults.iconSpacingFor(ClusterButtonHeight)))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (checked) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
