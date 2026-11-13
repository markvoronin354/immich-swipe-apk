package com.markvoronin.immichswipe.feature.swipe.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop

@Composable
fun SwipeActionIconButton(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .glassCircle(backdrop)
            .clip(CircleShape)
            .combinedClickable(
                enabled = enabled,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(8.dp)
    ) {
        CompositionLocalProvider(LocalContentColor provides Color.White, content = content)
    }
}
