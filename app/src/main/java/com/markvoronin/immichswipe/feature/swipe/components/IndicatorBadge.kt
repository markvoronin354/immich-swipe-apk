package com.markvoronin.immichswipe.feature.swipe.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val MaxAlpha = 0.9f

@Composable
fun IndicatorBadge(
    text: String,
    icon: ImageVector,
    color: Color,
    align: Alignment,
    contentColor: Color = Color.White,
    topPadding: Dp = 60.dp,
    alpha: () -> Float
) {
    // Springs in on appearance and pops slightly once the drag is far enough to commit.
    val committed by remember { derivedStateOf { alpha() >= MaxAlpha - 0.02f } }
    val scale = remember { Animatable(0.6f) }
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(committed) {
        if (committed) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
        scale.animateTo(
            targetValue = if (committed) 1.12f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        )
    }

    Box(
        // The alpha layer must span the whole card: when it sat inside the padding, its bounds clipped the pill's scale pop.
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha() }
            .padding(horizontal = 60.dp)
            .padding(top = topPadding, bottom = 28.dp),
        contentAlignment = align
    ) {
        Surface(
            color = color,
            contentColor = contentColor,
            shape = CircleShape,
            modifier = Modifier.graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
        ) {
            Row(
                modifier = Modifier.padding(start = 14.dp, end = 18.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(8.dp))
                Text(text = text, fontSize = 26.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}
