package com.markvoronin.immichswipe.feature.swipe.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.feature.swipe.SwipeUiState
import com.markvoronin.immichswipe.feature.swipe.utils.MaterialGreen
import com.markvoronin.immichswipe.feature.swipe.utils.MaterialRed


@Composable
fun SwipeHeader(
    uiState: SwipeUiState,
    onSummaryClick: () -> Unit,
    infoFontSize: TextUnit = 13.sp,
    iconSize: Dp = 20.dp
) {
    val animatedProgress by animateFloatAsState(
        targetValue = uiState.progress,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "ProgressBarAnimation"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
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
            
            val staticInfoX = totalWidth - infoWidthPx - paddingPx
            val infoIsInside = progressWidth > staticInfoX
            
            val infoTranslationX by animateFloatAsState(
                targetValue = if (infoIsInside) {
                    (progressWidth - infoWidthPx - paddingPx).coerceAtLeast(paddingPx)
                } else {
                    staticInfoX
                },
                animationSpec = spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow),
                label = "InfoTranslation"
            )

            // Layer 1: Dark Text (Background)
            Box(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp)) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = (uiState.isFetchingAssets || uiState.isLoading) && uiState.assets.isNotEmpty(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        FetchingAssetsIndicator(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = infoFontSize,
                            iconSize = 16.dp
                        )
                    }
                }

                HeaderInfo(
                    progressText = "${(uiState.progress * 100).toInt()}%",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = infoFontSize,
                    iconSize = iconSize,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .graphicsLayer { translationX = infoTranslationX }
                )
            }

            // Layer 2: Blue Bar Overlay with White Text
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
                    .background(Color.Black.copy(alpha = 0.1f))
            ) {
                // Unbounded width aligned to Start ensures the inner box doesn't center itself
                // when it overflows the parent's current progress width constraint.
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .wrapContentWidth(unbounded = true, align = Alignment.Start)
                        .requiredWidth(fullWidth)
                ) {
                    Box(modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp)) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = (uiState.isFetchingAssets || uiState.isLoading) && uiState.assets.isNotEmpty(),
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            FetchingAssetsIndicator(
                                color = Color.White,
                                fontSize = infoFontSize,
                                iconSize = 16.dp
                            )
                        }
                    }

                    HeaderInfo(
                        progressText = "${(uiState.progress * 100).toInt()}%",
                        color = Color.White,
                        fontSize = infoFontSize,
                        iconSize = iconSize,
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
            StatBadge(label = stringResource(R.string.swipe_keep), count = uiState.allKeptCount, color = MaterialGreen)
            if (uiState.skippedCount > 0) {
                StatBadge(label = stringResource(R.string.swipe_skip), count = uiState.skippedCount, color = Color(0xFFFFA500))
            }
            StatBadge(label = stringResource(R.string.swipe_delete), count = uiState.deletedCount, color = MaterialRed)
            StatBadge(label = stringResource(R.string.swipe_remaining), count = uiState.remainingCount, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun HeaderInfo(
    progressText: String,
    color: Color,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = TextUnit.Unspecified,
    iconSize: Dp = 14.dp
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = progressText,
            style = MaterialTheme.typography.labelSmall,
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            color = color
        )
        Spacer(Modifier.width(4.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.List,
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = color
        )
    }
}

@Composable
fun StatBadge(label: String, count: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "$count $label",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
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
        CircularProgressIndicator(
            modifier = Modifier.size(iconSize),
            strokeWidth = 2.dp,
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
