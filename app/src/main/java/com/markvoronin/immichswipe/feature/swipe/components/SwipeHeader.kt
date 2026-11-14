package com.markvoronin.immichswipe.feature.swipe.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.feature.common.SolidShapeLoadingIndicator
import com.markvoronin.immichswipe.feature.swipe.SwipeUiState
import com.markvoronin.immichswipe.feature.swipe.utils.MaterialGreen
import com.markvoronin.immichswipe.feature.swipe.utils.MaterialRed


@Composable
fun SwipeHeader(
    uiState: SwipeUiState,
    onSummaryClick: () -> Unit,
    horizontalPadding: Dp,
    infoFontSize: TextUnit = 13.sp,
    iconSize: Dp = 20.dp
) {
    val animatedProgress by animateFloatAsState(
        targetValue = uiState.progress,
        animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
        label = "ProgressBarAnimation"
    )
    val isFetching = (uiState.isFetchingAssets || uiState.isLoading) && uiState.assets.isNotEmpty()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = 2.dp)
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            // The card's container color: on custom palettes surfaceVariant equals the ring's track color.
            .background(CardDefaults.cardColors().containerColor)
            .clickable { onSummaryClick() }
            .padding(start = 12.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Crossfade(
            targetState = isFetching,
            modifier = Modifier.weight(1f),
            animationSpec = MaterialTheme.motionScheme.slowEffectsSpec(),
            label = "HeaderLeadingContent"
        ) { fetching ->
            if (fetching) {
                FetchingAssetsIndicator(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = infoFontSize,
                    iconSize = 16.dp
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 3.dp,
                        trackColor = MaterialTheme.colorScheme.outlineVariant,
                        gapSize = 0.dp
                    )
                    RemainingInfo(
                        remainingText = stringResource(R.string.swipe_remaining_left, uiState.remainingCount),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = infoFontSize
                    )
                }
            }
        }

        // Delete first, keep second: the same order as the swipe directions.
        CountBadge(count = uiState.deletedCount, description = "${uiState.deletedCount} ${stringResource(R.string.swipe_delete)}", icon = Icons.Default.Delete, color = MaterialRed)
        Spacer(Modifier.width(10.dp))
        CountBadge(count = uiState.allKeptCount, description = "${uiState.allKeptCount} ${stringResource(R.string.swipe_keep)}", icon = Icons.Default.CheckCircle, color = MaterialGreen)
        Spacer(Modifier.width(12.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.List,
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CountBadge(count: Int, description: String, icon: ImageVector, color: Color) {
    Row(
        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = color
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun RemainingInfo(
    remainingText: String,
    color: Color,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 13.sp
) {
    Text(
        text = remainingText,
        style = MaterialTheme.typography.labelSmall,
        fontSize = fontSize,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = modifier
    )
}

@Composable
fun FetchingAssetsIndicator(
    color: Color,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 13.sp,
    iconSize: Dp = 16.dp
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SolidShapeLoadingIndicator(
            size = iconSize * 1.5f,
            color = color
        )
        Text(
            text = stringResource(R.string.swipe_fetching_assets),
            style = MaterialTheme.typography.labelSmall,
            fontSize = fontSize,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}
