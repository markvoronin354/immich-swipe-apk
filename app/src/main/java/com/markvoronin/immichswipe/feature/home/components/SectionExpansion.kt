package com.markvoronin.immichswipe.feature.home.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import com.markvoronin.immichswipe.feature.home.AlbumStatus
import kotlin.math.roundToInt

/**
 * Expand/collapse state for one album section.
 *
 * A single [progress] animation drives every row in the section, so rows shrink together and
 * content below follows in lockstep. Rows are only kept in the lazy list while the section is
 * expanded or animating ([rowsPresent]); a collapsed section at rest contributes no items.
 */
internal class SectionExpansion(initiallyExpanded: Boolean) {
    val progress = Animatable(if (initiallyExpanded) 1f else 0f)

    /** Whether the section's rows should be emitted into the lazy list at all. */
    var rowsPresent by mutableStateOf(initiallyExpanded)
        private set

    /** True once fully expanded and at rest; while false, only a screenful of rows is emitted. */
    var settled by mutableStateOf(initiallyExpanded)
        private set

    suspend fun animateTo(expanded: Boolean, spec: AnimationSpec<Float>) {
        if (!progress.isRunning && progress.value == (if (expanded) 1f else 0f)) return
        settled = false
        if (expanded) {
            rowsPresent = true
            progress.animateTo(1f, spec)
            settled = true
        } else {
            progress.animateTo(0f, spec)
            rowsPresent = false
        }
    }
}

@Composable
internal fun rememberSectionExpansions(collapsedCategories: Set<AlbumStatus>): Map<AlbumStatus, SectionExpansion> {
    val spec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    return AlbumStatus.entries.associateWith { status ->
        key(status) {
            val expanded = status !in collapsedCategories
            val expansion = remember { SectionExpansion(expanded) }
            LaunchedEffect(expanded) { expansion.animateTo(expanded, spec) }
            expansion
        }
    }
}

/**
 * Upper bound on rows that can be on screen at once, assuming rows are at least the M3 minimum
 * list item height. Used to cap how many rows take part in an expand/collapse animation; rows
 * past this are below the viewport anyway.
 */
@Composable
internal fun rememberAnimatedRowLimit(itemsPerRow: Int = 1): Int {
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    return remember(screenHeightDp, itemsPerRow) { (screenHeightDp / 56 + 2) * itemsPerRow }
}

/** Scales this item's height and opacity by [fraction], clipping the content as it shrinks. */
internal fun Modifier.expandFraction(fraction: () -> Float): Modifier = this
    .graphicsLayer {
        alpha = fraction().coerceIn(0f, 1f)
        clip = true
    }
    .layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val height = (placeable.height * fraction()).roundToInt().coerceAtLeast(0)
        layout(placeable.width, height) { placeable.place(0, 0) }
    }
