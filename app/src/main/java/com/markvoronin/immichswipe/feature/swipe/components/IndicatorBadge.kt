package com.markvoronin.immichswipe.feature.swipe.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.markvoronin.immichswipe.R

private const val MaxAlpha = 0.9f

private val RoundedBlack = FontFamily(
    Font(R.font.nunito, FontWeight.Black, variationSettings = FontVariation.Settings(FontVariation.weight(900)))
)

@Composable
fun IndicatorBadge(
    text: String,
    color: Color,
    align: Alignment,
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

    val density = LocalDensity.current
    val baseStyle = MaterialTheme.typography.displaySmall.copy(fontFamily = RoundedBlack, fontWeight = FontWeight.Black)
    val dropShadow = Shadow(
        color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f),
        offset = with(density) { Offset(0f, 2.dp.toPx()) },
        blurRadius = with(density) { 8.dp.toPx() }
    )
    val glow = Shadow(color = color.copy(alpha = 0.7f), blurRadius = with(density) { 10.dp.toPx() })

    Box(
        // The alpha layer must span the whole card: when it sat inside the padding, its bounds clipped the label's scale pop.
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha() }
            .padding(horizontal = 60.dp)
            .padding(top = topPadding, bottom = 28.dp),
        contentAlignment = align
    ) {
        // Text takes a single shadow, so the dark drop shadow sits on a copy underneath the glowing label.
        Box(
            modifier = Modifier.graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
        ) {
            Text(text = text, color = color, style = baseStyle.copy(shadow = dropShadow))
            Text(text = text, color = color, style = baseStyle.copy(shadow = glow))
        }
    }
}
