package com.markvoronin.immichswipe.feature.swipe.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import com.markvoronin.immichswipe.core.IconPosition

val MaterialGreen = Color(0xFF2E7D32)
val KeepGreen = Color(0xFF00C853)
val MaterialRed = Color(0xFFC62828)


fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

fun IconPosition.toHorizontalAlignment(): Alignment.Horizontal = when (this) {
    IconPosition.TOP_LEFT, IconPosition.BOTTOM_LEFT -> Alignment.Start
    else -> Alignment.End
}

private class ContinuousRotation(var angle: Int) {
    var target = angle.toFloat()
}

/** Animation target for a 0/90/180/270 [angle] that keeps turning clockwise when 270 wraps to 0. */
@Composable
fun rememberContinuousRotation(angle: Int): Float {
    val state = remember { ContinuousRotation(angle) }
    if (angle != state.angle) {
        val delta = angle - state.angle
        state.target += if (delta == -270) 90 else delta
        state.angle = angle
    }
    return state.target
}

fun Modifier.rotateLayout(rotation: Int) = layout { measurable, constraints ->
    val isRotated = (rotation % 180) != 0
    val newConstraints = if (isRotated) {
        androidx.compose.ui.unit.Constraints(
            minWidth = constraints.minHeight,
            maxWidth = constraints.maxHeight,
            minHeight = constraints.minWidth,
            maxHeight = constraints.maxWidth,
        )
    } else constraints
    val placeable = measurable.measure(newConstraints)
    if (isRotated) {
        layout(placeable.height, placeable.width) {
            placeable.place((placeable.height - placeable.width) / 2, (placeable.width - placeable.height) / 2)
        }
    } else {
        layout(placeable.width, placeable.height) {
            placeable.place(0, 0)
        }
    }
}
