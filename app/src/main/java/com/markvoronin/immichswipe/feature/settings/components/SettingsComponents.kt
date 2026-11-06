package com.markvoronin.immichswipe.feature.settings.components

import android.graphics.Matrix
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.toPath
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.core.AppPalette
import com.markvoronin.immichswipe.core.AppTheme
import com.markvoronin.immichswipe.feature.swipe.components.connectedTileShape
import com.markvoronin.immichswipe.ui.theme.colorScheme
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
    content: @Composable () -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
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
        // The track is `primary`, so it always contrasts with the `onPrimary` thumb (unlike the default icon colour).
        colors = SwitchDefaults.colors(checkedIconColor = MaterialTheme.colorScheme.primary),
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
class JiggleGroup(private val count: Int, private val grow: Float = JiggleGrow) {
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
            tappedIndex == index -> 1f + grow
            else -> 1f - grow / (count - 1).coerceAtLeast(1)
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
fun rememberJiggleGroup(count: Int, grow: Float = JiggleGrow): JiggleGroup {
    val group = remember(count, grow) { JiggleGroup(count, grow) }
    LaunchedEffect(group.tapToken) {
        if (group.tappedIndex >= 0) {
            delay(120)
            group.clear()
        }
    }
    return group
}

/** Flat group button whose icon slides in beside the label only while the button is selected. */
@Composable
fun ThemeModeButton(
    text: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    index: Int = 0,
    count: Int = 1,
) {
    SettingsToggleButton(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        index = index,
        count = count
    ) {
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start),
            exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.Start)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
            }
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

/** Unselected tile colour: must contrast with the settings card (surfaceContainerHigh) in both light and dark. */
@Composable
private fun unselectedTileColor(): Color =
    if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) {
        MaterialTheme.colorScheme.surfaceContainerHighest
    } else {
        MaterialTheme.colorScheme.surfaceContainerLowest
    }

/** Expressive selector button: morphs shape when selected and keeps the settings colours and outline. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsToggleButton(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    index: Int = 0,
    count: Int = 1,
    tileShape: RoundedCornerShape? = null,
    checkedTileShape: RoundedCornerShape? = null,
    content: @Composable RowScope.() -> Unit
) {
    val shape = tileShape ?: connectedTileShape(index, count, outer = 24.dp)
    // Connected groups morph the selected button into a pill; custom tiles use their own checked shape.
    val checkedShape = checkedTileShape ?: if (tileShape == null) CircleShape else shape
    ToggleButton(
        checked = selected,
        onCheckedChange = { onClick() },
        shapes = ToggleButtonShapes(shape = shape, pressedShape = RoundedCornerShape(10.dp), checkedShape = checkedShape),
        modifier = modifier.height(40.dp),
        colors = ToggleButtonDefaults.colors(
            containerColor = unselectedTileColor(),
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        contentPadding = PaddingValues(horizontal = 8.dp),
        content = content
    )
}

@Composable
fun PaletteSelector(
    selected: AppPalette,
    themeMode: AppTheme,
    onSelect: (AppPalette) -> Unit
) {
    val context = LocalContext.current
    val dark = when (themeMode) {
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = stringResource(R.string.settings_palette_label),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = stringResource(R.string.settings_palette_desc),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AppPalette.entries.filter { it.isAvailable }.forEach { palette ->
                val scheme = remember(palette, dark) {
                    if (palette == AppPalette.WALLPAPER && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                    } else {
                        palette.colorScheme(dark)
                    }
                }
                PaletteSwatch(
                    primary = scheme.primary,
                    secondary = scheme.secondaryContainer,
                    tertiary = scheme.tertiary,
                    label = stringResource(palette.labelRes),
                    selected = selected == palette,
                    onClick = { onSelect(palette) }
                )
            }
        }
    }
}

private class MorphShape(private val morph: Morph, private val progress: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = morph.toPath(progress)
        path.transform(Matrix().apply { setScale(size.width, size.height) })
        return Outline.Generic(path.asComposePath())
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PaletteSwatch(
    primary: Color,
    secondary: Color,
    tertiary: Color,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    // The selected swatch morphs from a circle into a cookie shape.
    val morphProgress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
        label = "paletteMorph"
    )
    val morph = remember { Morph(MaterialShapes.Circle, MaterialShapes.Cookie9Sided) }
    val swatchShape = MorphShape(morph, morphProgress)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(swatchShape)
                .clickable(role = Role.RadioButton, onClick = onClick)
                .border(
                    2.5.dp,
                    if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                    swatchShape
                )
                .padding(5.dp)
                .clip(swatchShape)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxWidth().background(primary))
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    Box(Modifier.weight(1f).fillMaxSize().background(secondary))
                    Box(Modifier.weight(1f).fillMaxSize().background(tertiary))
                }
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ThemeButton(
    text: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    index: Int = 0,
    count: Int = 1,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(72.dp),
        shape = connectedTileShape(index, count, outer = 24.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else unselectedTileColor()
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
