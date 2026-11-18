package com.markvoronin.immichswipe.feature.swipe.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop

/** A button's full size, the 48dp minimum touch target: a 28dp icon with 10dp around it. */
internal val SwipeActionButtonSize = 48.dp

/** The 24dp icons are drawn 28dp in a full-size button. */
private const val IconScale = 28f / 24f

/** The side of the card's round buttons; `ActionButtonsOverlay` shrinks it when a corner's buttons wouldn't fit. */
internal val LocalSwipeActionButtonSize = compositionLocalOf<Dp> { SwipeActionButtonSize }

/** True when the button sits in a pill with its neighbours, which draws the glass; the button then has none of its own. */
internal val LocalSwipeActionButtonMerged = compositionLocalOf { false }

@Composable
fun SwipeActionIconButton(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val size = LocalSwipeActionButtonSize.current
    val merged = LocalSwipeActionButtonMerged.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .then(if (merged) Modifier else Modifier.glassCircle(backdrop))
            .clip(if (merged) RectangleShape else CircleShape)
            .combinedClickable(
                enabled = enabled,
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            // The icon shrinks with the circle.
            Box(
                modifier = Modifier.graphicsLayer {
                    val scale = size / SwipeActionButtonSize * IconScale
                    scaleX = scale
                    scaleY = scale
                }
            ) {
                content()
            }
        }
    }
}
