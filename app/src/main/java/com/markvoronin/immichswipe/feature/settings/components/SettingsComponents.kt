package com.markvoronin.immichswipe.feature.settings.components

import android.graphics.Matrix
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
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
import com.markvoronin.immichswipe.core.PaletteStyle
import com.markvoronin.immichswipe.core.AppTheme
import com.markvoronin.immichswipe.feature.swipe.components.connectedTileShape
import com.markvoronin.immichswipe.ui.theme.colorScheme
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

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
    topContent: @Composable () -> Unit = {},
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
        topContent()
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

@Composable
fun SettingsItemGap(color: Color = MaterialTheme.colorScheme.background) {
    Spacer(
        Modifier
            .fillMaxWidth()
            .height(3.dp)
            .background(color)
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SettingsItemIcon(
    icon: ImageVector,
    isDestructive: Boolean = false,
    checked: Boolean? = null,
    shapeRotation: Float = 0f
) {
    val background = when {
        isDestructive -> MaterialTheme.colorScheme.error
        checked == null -> MaterialTheme.colorScheme.secondaryContainer
        checked -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val tint = when {
        isDestructive -> MaterialTheme.colorScheme.onError
        checked == null -> MaterialTheme.colorScheme.onSecondaryContainer
        checked -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.outline
    }
    val shape = when {
        isDestructive -> MaterialShapes.Gem.toShape()
        checked == null -> MaterialShapes.Cookie6Sided.toShape()
        else -> animatedCookieShape(checked, MaterialTheme.motionScheme.fastSpatialSpec())
    }
    Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer { rotationZ = shapeRotation }
                .background(background, shape)
        )
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
    }
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsItemIcon(icon, isDestructive)
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
        SettingsItemIcon(icon, checked = checked)
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
    icon: ImageVector,
    description: String? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        if (checked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        animatedCookieShape(checked, MaterialTheme.motionScheme.fastSpatialSpec())
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (checked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp)
                )
            }
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
        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(start = 58.dp, end = 16.dp, bottom = 8.dp)
            )
        }
    }
}

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

    @Composable
    fun factor(index: Int): Float {
        val target = when {
            tappedIndex < 0 -> 1f
            tappedIndex == index -> 1f + grow
            else -> 1f - grow / (count - 1).coerceAtLeast(1)
        }
        val spec = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
        return animateFloatAsState(targetValue = target, animationSpec = spec, label = "jiggleFactor").value
    }
}

@Composable
fun rememberJiggleGroup(count: Int, grow: Float = JiggleGrow): JiggleGroup {
    val group = remember(count, grow) { JiggleGroup(count, grow) }
    LaunchedEffect(group.tapToken) {
        if (group.tappedIndex >= 0) {
            delay(120.milliseconds)
            group.clear()
        }
    }
    return group
}

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
            enter = fadeIn(MaterialTheme.motionScheme.slowEffectsSpec()) + expandHorizontally(MaterialTheme.motionScheme.defaultSpatialSpec(), expandFrom = Alignment.Start),
            exit = fadeOut(MaterialTheme.motionScheme.slowEffectsSpec()) + shrinkHorizontally(MaterialTheme.motionScheme.defaultSpatialSpec(), shrinkTowards = Alignment.Start)
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

@Composable
internal fun unselectedTileColor(): Color =
    if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) {
        MaterialTheme.colorScheme.surfaceContainerHighest
    } else {
        MaterialTheme.colorScheme.surfaceContainerLowest
    }

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
    customSeed: Int,
    customStyle: PaletteStyle,
    onSelect: (AppPalette) -> Unit,
    onEditCustom: () -> Unit
) {
    val context = LocalContext.current
    val dark = when (themeMode) {
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }
    fun schemeFor(palette: AppPalette) =
        if (palette == AppPalette.WALLPAPER && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            palette.colorScheme(dark, customSeed, customStyle)
        }
    SettingsExpandableItem(
        title = stringResource(R.string.settings_palette_label),
        subtitle = stringResource(R.string.settings_palette_desc),
        icon = Icons.Default.Palette
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                AppPalette.entries.filter { it.isAvailable }.forEach { palette ->
                    val scheme = remember(palette, dark, customSeed, customStyle) { schemeFor(palette) }
                    PaletteSwatch(
                        primary = scheme.primary,
                        secondary = scheme.secondaryContainer,
                        tertiary = scheme.tertiary,
                        label = stringResource(palette.labelRes),
                        selected = selected == palette,
                        onClick = { onSelect(palette) },
                        onLongClick = if (palette == AppPalette.CUSTOM) onEditCustom else null
                    )
                }
            }
            // The editor entry slides out below the swatches while the custom palette is selected.
            AnimatedVisibility(
                visible = selected == AppPalette.CUSTOM,
                enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeIn(MaterialTheme.motionScheme.slowEffectsSpec()),
                exit = shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeOut(MaterialTheme.motionScheme.slowEffectsSpec())
            ) {
                Column {
                    SettingsItemGap()
                    SettingsClickableItem(
                        title = stringResource(R.string.settings_custom_palette_edit),
                        subtitle = stringResource(R.string.settings_custom_palette_edit_desc),
                        icon = Icons.Default.Edit,
                        onClick = onEditCustom
                    )
                }
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
internal fun animatedCookieShape(cookie: Boolean, animationSpec: FiniteAnimationSpec<Float>): Shape {
    val progress by animateFloatAsState(
        targetValue = if (cookie) 1f else 0f,
        animationSpec = animationSpec,
        label = "cookieMorph"
    )
    val morph = remember { Morph(MaterialShapes.Circle, MaterialShapes.Cookie9Sided) }
    return MorphShape(morph, progress)
}

@Composable
private fun PaletteSwatch(
    primary: Color,
    secondary: Color,
    tertiary: Color,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val swatchShape = animatedCookieShape(selected, MaterialTheme.motionScheme.slowSpatialSpec())
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(swatchShape)
                .combinedClickable(role = Role.RadioButton, onLongClick = onLongClick, onClick = onClick)
                .border(
                    2.5.dp,
                    if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                    swatchShape
                )
                .padding(5.dp)
                .clip(swatchShape)
        ) {
            SwatchFill(primary, secondary, tertiary)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SettingsExpandableItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(if (expanded) 180f else 0f, animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(), label = "expandChevron")
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SettingsItemIcon(icon, shapeRotation = chevronRotation)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Icon(
                Icons.Default.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.rotate(chevronRotation)
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeIn(MaterialTheme.motionScheme.slowEffectsSpec()),
            exit = shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeOut(MaterialTheme.motionScheme.slowEffectsSpec())
        ) {
            content()
        }
    }
}

@Composable
private fun SwatchFill(primary: Color, secondary: Color, tertiary: Color) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth().background(primary))
        Row(Modifier.weight(1f).fillMaxWidth()) {
            Box(Modifier.weight(1f).fillMaxSize().background(secondary))
            Box(Modifier.weight(1f).fillMaxSize().background(tertiary))
        }
    }
}
