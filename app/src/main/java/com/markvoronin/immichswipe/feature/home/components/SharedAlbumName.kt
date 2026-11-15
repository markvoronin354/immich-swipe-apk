package com.markvoronin.immichswipe.feature.home.components

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.navigation3.ui.LocalNavAnimatedContentScope

val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

@Composable
fun SharedTransitionHost(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    SharedTransitionLayout(modifier = modifier) {
        CompositionLocalProvider(LocalSharedTransitionScope provides this, content = content)
    }
}

/**
 * Flies an album's name between its home list/grid item and the swipe screen's top bar title. Both ends sit in
 * NavDisplay entries, so they share the page transition's clock.
 *
 * [listViewport] is the scrolling list the name sits in, if any. A name that list has scrolled partly out of view
 * rides its page instead of flying over the bars above the list.
 */
@Composable
fun Modifier.sharedAlbumName(albumId: String, listViewport: () -> LayoutCoordinates? = { null }): Modifier {
    val scope = LocalSharedTransitionScope.current ?: return this
    // Same spring as the swipe and duplicates page slide, so the name lands with the page.
    val boundsSpec = MaterialTheme.motionScheme.slowSpatialSpec<Rect>()
    val boundsTransform = remember(boundsSpec) { BoundsTransform { _, _ -> boundsSpec } }
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    // Re-checked mid-flight: on the way back, the list item is first placed after the transition has started.
    // Measured against the list, not the screen, so the page slides can't hide the name.
    val config = remember(listViewport) {
        object : SharedTransitionScope.SharedContentConfig {
            override val SharedTransitionScope.SharedContentState.isEnabled: Boolean
                get() {
                    val list = listViewport()?.takeIf { it.isAttached } ?: return true
                    val name = coordinates?.takeIf { it.isAttached } ?: return true
                    val bounds = list.localBoundingBoxOf(name, clipBounds = false)
                    return bounds.top >= 0f && bounds.bottom <= list.size.height
                }
            override val shouldKeepEnabledForOngoingAnimation: Boolean
                get() = false
        }
    }
    return with(scope) {
        this@sharedAlbumName
            .onPlaced { coordinates = it }
            .sharedBounds(
                sharedContentState = rememberSharedContentState("album-name-$albumId", config),
                animatedVisibilityScope = LocalNavAnimatedContentScope.current,
                boundsTransform = boundsTransform,
            )
    }
}
