package com.markvoronin.immichswipe.feature.swipe.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import com.markvoronin.immichswipe.core.IconPosition

val MaterialGreen = Color(0xFF2E7D32)
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
