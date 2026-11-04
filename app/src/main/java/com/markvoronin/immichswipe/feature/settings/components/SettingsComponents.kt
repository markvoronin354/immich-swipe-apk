package com.markvoronin.immichswipe.feature.settings.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

// How much the tapped button grows (as a fraction of its size); neighbours shrink to compensate.
private const val JiggleGrow = 0.07f

fun Modifier.verticalFadingEdges(
    scrollState: ScrollState,
    length: Dp = 32.dp
): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()

        val lengthPx = length.toPx()
        if (lengthPx <= 0f) return@drawWithContent

        val topFadePx = minOf(scrollState.value.toFloat(), lengthPx)

        val remainingScroll = (scrollState.maxValue - scrollState.value).toFloat().coerceAtLeast(0f)
        val bottomFadePx = if (scrollState.maxValue > 0 && scrollState.maxValue != Int.MAX_VALUE) {
            minOf(remainingScroll, lengthPx)
        } else if (scrollState.maxValue == Int.MAX_VALUE) {
            lengthPx
        } else {
            0f
        }

        if (topFadePx > 0f) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Black, Color.Transparent),
                    startY = 0f,
                    endY = topFadePx
                ),
                blendMode = BlendMode.DstOut
            )
        }

        if (bottomFadePx > 0f) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black),
                    startY = size.height - bottomFadePx,
                    endY = size.height
                ),
                blendMode = BlendMode.DstOut
            )
        }
    }

fun Modifier.verticalFadingEdges(
    lazyListState: LazyListState,
    length: Dp = 32.dp
): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()

        val lengthPx = length.toPx()
        if (lengthPx <= 0f) return@drawWithContent

        val topFadePx = if (lazyListState.firstVisibleItemIndex == 0) {
            minOf(lazyListState.firstVisibleItemScrollOffset.toFloat(), lengthPx)
        } else {
            lengthPx
        }

        val bottomFadePx = if (lazyListState.canScrollForward) {
            lengthPx
        } else {
            0f
        }

        if (topFadePx > 0f) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Black, Color.Transparent),
                    startY = 0f,
                    endY = topFadePx
                ),
                blendMode = BlendMode.DstOut
            )
        }

        if (bottomFadePx > 0f) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black),
                    startY = size.height - bottomFadePx,
                    endY = size.height
                ),
                blendMode = BlendMode.DstOut
            )
        }
    }

fun Modifier.verticalFadingEdges(
    lazyGridState: LazyGridState,
    length: Dp = 32.dp
): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()

        val lengthPx = length.toPx()
        if (lengthPx <= 0f) return@drawWithContent

        val topFadePx = if (lazyGridState.firstVisibleItemIndex == 0) {
            minOf(lazyGridState.firstVisibleItemScrollOffset.toFloat(), lengthPx)
        } else {
            lengthPx
        }

        val bottomFadePx = if (lazyGridState.canScrollForward) {
            lengthPx
        } else {
            0f
        }

        if (topFadePx > 0f) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Black, Color.Transparent),
                    startY = 0f,
                    endY = topFadePx
                ),
                blendMode = BlendMode.DstOut
            )
        }

        if (bottomFadePx > 0f) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black),
                    startY = size.height - bottomFadePx,
                    endY = size.height
                ),
                blendMode = BlendMode.DstOut
            )
        }
    }

fun Modifier.horizontalFadingEdges(
    lazyListState: LazyListState,
    length: Dp = 24.dp
): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()

        val lengthPx = length.toPx()
        if (lengthPx <= 0f) return@drawWithContent

        val leftFadePx = if (lazyListState.firstVisibleItemIndex == 0) {
            minOf(lazyListState.firstVisibleItemScrollOffset.toFloat(), lengthPx)
        } else {
            lengthPx
        }

        val rightFadePx = if (lazyListState.canScrollForward) {
            lengthPx
        } else {
            0f
        }

        if (leftFadePx > 0f) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Black, Color.Transparent),
                    startX = 0f,
                    endX = leftFadePx
                ),
                blendMode = BlendMode.DstOut
            )
        }

        if (rightFadePx > 0f) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, Color.Black),
                    startX = size.width - rightFadePx,
                    endX = size.width
                ),
                blendMode = BlendMode.DstOut
            )
        }
    }

@Composable
fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 0.dp
        ) {
            content()
        }
    }
}

/**
 * Separates rows inside a [SettingsSection] with a thin gap of page background, splitting it into tiles.
 * Pass [color] when the tiles sit on something other than the page (e.g. a popup).
 */
@Composable
fun SettingsItemGap(color: Color = MaterialTheme.colorScheme.background) {
    Spacer(
        Modifier
            .fillMaxWidth()
            .height(3.dp)
            .background(color)
    )
}

@Composable
fun SettingsClickableItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    val effectiveTitleColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    val effectiveIconColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = effectiveIconColor, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = effectiveTitleColor
            )
            if (subtitle.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
fun SettingsToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            if (subtitle.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
        SettingsSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

/** Settings switch with a check icon in the thumb when on (the expressive switch look). */
@Composable
fun SettingsSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        thumbContent = if (checked) {
            {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize)
                )
            }
        } else null
    )
}

@Composable
fun SettingsToggleItemSmall(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        SettingsSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.scale(0.8f)
        )
    }
}

/**
 * Tracks which button in a group was just tapped so the group can jiggle: the tapped button grows
 * slightly and its neighbours give way, keeping the group's total width constant.
 */
@Stable
class JiggleGroup(private val count: Int) {
    var tappedIndex by mutableIntStateOf(-1)
        private set
    var tapToken by mutableIntStateOf(0)
        private set

    fun tap(index: Int) {
        tappedIndex = index
        tapToken++
    }

    internal fun clear() {
        tappedIndex = -1
    }

    /** Animated size multiplier for the button at [index], ~1 at rest. */
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    fun factor(index: Int): Float {
        val target = when {
            tappedIndex < 0 -> 1f
            tappedIndex == index -> 1f + JiggleGrow
            else -> 1f - JiggleGrow / (count - 1).coerceAtLeast(1)
        }
        val spec = remember { MotionScheme.expressive().fastSpatialSpec<Float>() }
        return animateFloatAsState(targetValue = target, animationSpec = spec, label = "jiggleFactor").value
    }
}

/**
 * The jiggle is triggered by the tap itself, not the pressed state: inside a scrolling screen a quick tap
 * reports press and release together, so the pressed state is never observed.
 */
@Composable
fun rememberJiggleGroup(count: Int): JiggleGroup {
    val group = remember(count) { JiggleGroup(count) }
    LaunchedEffect(group.tapToken) {
        if (group.tappedIndex >= 0) {
            delay(120)
            group.clear()
        }
    }
    return group
}

/** A [Row] whose buttons jiggle together; call [JiggleGroup.tap] on click and size items with [JiggleGroup.factor]. */
@Composable
fun JiggleRow(
    count: Int,
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(8.dp),
    content: @Composable RowScope.(JiggleGroup) -> Unit
) {
    val group = rememberJiggleGroup(count)
    Row(modifier = modifier, horizontalArrangement = horizontalArrangement) {
        content(group)
    }
}

/** Expressive selector button: morphs shape when selected and keeps the settings colours and outline. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsToggleButton(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    ToggleButton(
        checked = selected,
        onCheckedChange = { onClick() },
        modifier = modifier.height(40.dp),
        colors = ToggleButtonDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        contentPadding = PaddingValues(horizontal = 8.dp),
        content = content
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ThemeButton(
    text: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Corners tighten when selected, with the expressive spring.
    val corner by animateDpAsState(
        targetValue = if (selected) 14.dp else 22.dp,
        animationSpec = remember { MotionScheme.expressive().fastSpatialSpec() },
        label = "themeButtonCorner"
    )
    Surface(
        onClick = onClick,
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(corner),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
