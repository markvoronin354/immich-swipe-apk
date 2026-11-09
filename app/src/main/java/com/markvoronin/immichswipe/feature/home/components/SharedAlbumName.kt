package com.markvoronin.immichswipe.feature.home.components

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
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
 */
@Composable
fun Modifier.sharedAlbumName(albumId: String): Modifier {
    val scope = LocalSharedTransitionScope.current ?: return this
    // Same expressive spring as the page slide, so the name lands with the page.
    val boundsSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Rect>()
    val boundsTransform = remember(boundsSpec) { BoundsTransform { _, _ -> boundsSpec } }
    return with(scope) {
        this@sharedAlbumName.sharedBounds(
            sharedContentState = rememberSharedContentState("album-name-$albumId"),
            animatedVisibilityScope = LocalNavAnimatedContentScope.current,
            boundsTransform = boundsTransform,
        )
    }
}
