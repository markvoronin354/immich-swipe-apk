package com.markvoronin.immichswipe.feature.settings.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LibraryAddCheck
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.Tonality
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.core.PaletteStyle
import com.markvoronin.immichswipe.core.SortCategory
import com.markvoronin.immichswipe.feature.settings.SettingsUiState
import com.markvoronin.immichswipe.feature.settings.SettingsViewModel
import com.markvoronin.immichswipe.feature.home.components.StatCard
import com.markvoronin.immichswipe.feature.home.components.gridTileShape
import com.markvoronin.immichswipe.feature.swipe.components.CategorySegmentedRow
import com.markvoronin.immichswipe.feature.swipe.components.SortOptionGroup
import com.markvoronin.immichswipe.feature.swipe.components.SortPopupItem
import com.markvoronin.immichswipe.ui.theme.GREY_CHROMA
import com.markvoronin.immichswipe.ui.theme.MIN_CHROMA
import com.markvoronin.immichswipe.ui.theme.Tone
import com.markvoronin.immichswipe.ui.theme.customColorScheme

// Quick seeds and slider constants, from Orion Store's palette maker.
private val PRESET_SEEDS = listOf(
    0xFF6366F1, 0xFF0EA5E9, 0xFF14B8A6, 0xFF22C55E, 0xFF84CC16, 0xFFEAB308,
    0xFFF97316, 0xFFEF4444, 0xFFEC4899, 0xFFA855F7, 0xFF92400E, 0xFF64748B
).map { it.toInt() }
private const val SEED_TONE = 58.0
private const val MAX_CHROMA = .3
/** Left end of the intensity slider that gives a grey palette; the rest spans [MIN_CHROMA]..[MAX_CHROMA] evenly. */
private const val GREY_ZONE = .05f

private fun sliderToChroma(v: Float): Double =
    if (v < GREY_ZONE) 0.0 else MIN_CHROMA + (v - GREY_ZONE) / (1 - GREY_ZONE) * (MAX_CHROMA - MIN_CHROMA)

private fun chromaToSlider(c: Double): Float =
    if (c <= GREY_CHROMA) 0f
    else GREY_ZONE + (1 - GREY_ZONE) * ((c - MIN_CHROMA) / (MAX_CHROMA - MIN_CHROMA)).toFloat().coerceIn(0f, 1f)

/** Builds a palette from one seed color, previewed on the sort popup in light or dark before it is applied. */
@Composable
fun CustomPaletteScreen(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = true) {
        onBack()
    }

    val start = remember { Tone.lch(uiState.customPaletteSeed) }
    var seed by rememberSaveable { mutableIntStateOf(uiState.customPaletteSeed) }
    var hue by rememberSaveable { mutableFloatStateOf(start.second.toFloat()) }
    var chroma by rememberSaveable { mutableFloatStateOf(chromaToSlider(start.first)) }
    val appIsDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    var previewDark by rememberSaveable { mutableStateOf(appIsDark) }
    var style by rememberSaveable { mutableStateOf(uiState.customPaletteStyle) }

    fun pick(color: Int) {
        val (c, h) = Tone.lch(color)
        hue = h.toFloat()
        chroma = chromaToSlider(c)
        seed = color
    }
    fun slide() {
        seed = Tone.at(hue.toDouble(), sliderToChroma(chroma), SEED_TONE)
    }

    val scheme = remember(seed, previewDark, style) { customColorScheme(seed, previewDark, style) }
    val hueTrack = remember { Brush.horizontalGradient((0..12).map { Color(Tone.at(it * 30.0, .16, 65.0)) }) }
    val chromaTrack = remember(hue) {
        val grey = Color(Tone.at(hue.toDouble(), 0.0, SEED_TONE))
        Brush.horizontalGradient(
            0f to grey,
            GREY_ZONE to grey,
            GREY_ZONE to Color(Tone.at(hue.toDouble(), MIN_CHROMA, SEED_TONE)),
            1f to Color(Tone.at(hue.toDouble(), MAX_CHROMA, SEED_TONE))
        )
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalFadingEdges(scrollState, length = 32.dp)
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(Modifier.height(16.dp))

        SettingsSection(title = stringResource(R.string.settings_custom_palette_preview)) {
            Column {
                Column(modifier = Modifier.padding(16.dp)) {
                    MaterialTheme(colorScheme = scheme) {
                        StatsPreview()
                        Spacer(Modifier.height(12.dp))
                        SortPopupPreview()
                    }
                }
                SettingsItemGap()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    ThemeModeButton(
                        text = stringResource(R.string.settings_theme_light),
                        icon = Icons.Default.LightMode,
                        selected = !previewDark,
                        onClick = { previewDark = false },
                        modifier = Modifier.weight(1f),
                        index = 0, count = 2
                    )
                    ThemeModeButton(
                        text = stringResource(R.string.settings_theme_dark),
                        icon = Icons.Default.DarkMode,
                        selected = previewDark,
                        onClick = { previewDark = true },
                        modifier = Modifier.weight(1f),
                        index = 1, count = 2
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        SettingsSection(title = stringResource(R.string.settings_custom_palette_style)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                PaletteStyle.entries.forEachIndexed { index, option ->
                    ThemeModeButton(
                        text = stringResource(option.labelRes),
                        icon = when (option) {
                            PaletteStyle.SOFT -> Icons.Default.Opacity
                            PaletteStyle.BALANCED -> Icons.Default.Tonality
                            PaletteStyle.VIVID -> Icons.Default.AutoAwesome
                        },
                        selected = style == option,
                        onClick = { style = option },
                        modifier = Modifier.weight(1f),
                        index = index, count = PaletteStyle.entries.size
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        SettingsSection(title = stringResource(R.string.settings_custom_palette_color)) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(stringResource(R.string.settings_custom_palette_hue), style = MaterialTheme.typography.labelLarge)
                TrackSlider(hue / 360f, hueTrack) { hue = it * 360f; slide() }
                Text(stringResource(R.string.settings_custom_palette_intensity), style = MaterialTheme.typography.labelLarge)
                TrackSlider(chroma, chromaTrack) { chroma = it; slide() }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PRESET_SEEDS.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .border(2.dp, if (color == seed) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape)
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(Color(color))
                                .clickable(role = Role.RadioButton) { pick(color) }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                viewModel.applyCustomPalette(seed, style)
                onBack()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(stringResource(R.string.settings_custom_palette_apply))
        }

        Spacer(Modifier.navigationBarsPadding().height(24.dp))
    }
}

/** A static copy of the sort popup, drawn in whatever [MaterialTheme] wraps it. Rows and tiles still react to taps. */
@Composable
private fun SortPopupPreview() {
    var selectedRow by remember { mutableIntStateOf(0) }
    var category by remember { mutableStateOf(SortCategory.SIZE) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            SortOptionGroup {
                SortPopupItem(R.string.settings_sort_biggest, Icons.Default.ExpandMore, selectedRow == 0) { selectedRow = 0 }
                SortPopupItem(R.string.settings_sort_smallest, Icons.Default.ExpandLess, selectedRow == 1) { selectedRow = 1 }
            }
            Spacer(Modifier.height(16.dp))
            CategorySegmentedRow(
                selectedCategory = category,
                onCategorySelected = { category = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        }
    }
}

/** The stats popup's 2x2 tile grid, so the secondary, primary and tertiary accents sit side by side. */
@Composable
private fun StatsPreview() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                StatCard(
                    label = stringResource(R.string.stats_deleted_count),
                    value = "42",
                    icon = Icons.Default.Delete,
                    badgeColor = MaterialTheme.colorScheme.errorContainer,
                    badgeContentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = gridTileShape(row = 0, column = 0),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = stringResource(R.string.stats_bytes_saved),
                    value = "1.2 GB",
                    icon = Icons.Default.CloudDone,
                    badgeColor = MaterialTheme.colorScheme.secondaryContainer,
                    badgeContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = gridTileShape(row = 0, column = 1),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                StatCard(
                    label = stringResource(R.string.stats_swiped_count),
                    value = "128",
                    icon = Icons.Default.Swipe,
                    badgeColor = MaterialTheme.colorScheme.primaryContainer,
                    badgeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = gridTileShape(row = 1, column = 0),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = stringResource(R.string.stats_albums_completed),
                    value = "3 / 12",
                    icon = Icons.Default.LibraryAddCheck,
                    badgeColor = MaterialTheme.colorScheme.tertiaryContainer,
                    badgeContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    shape = gridTileShape(row = 1, column = 1),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** A slider whose track is the color gradient, so the thumb sits exactly on the color it picks. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrackSlider(value: Float, track: Brush, onChange: (Float) -> Unit) {
    // Same wiring as the value-based Slider overload, which has no track slot.
    val state = remember { SliderState(value) }
    state.value = value
    Slider(
        state = state,
        onValueChange = onChange,
        colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.onSurface),
        track = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .clip(CircleShape)
                    .background(track)
            )
        }
    )
}
