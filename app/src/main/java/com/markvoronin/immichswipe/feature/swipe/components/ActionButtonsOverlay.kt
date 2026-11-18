package com.markvoronin.immichswipe.feature.swipe.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Hd
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.core.ButtonStackOrientation
import com.markvoronin.immichswipe.core.ButtonStackStyle
import com.markvoronin.immichswipe.core.IconPosition
import com.markvoronin.immichswipe.core.ImmichLauncher
import com.markvoronin.immichswipe.core.ImmichOpenMode
import com.markvoronin.immichswipe.domain.model.Asset
import com.markvoronin.immichswipe.feature.common.DownloadIcon
import com.markvoronin.immichswipe.feature.common.VolumeIcon
import com.markvoronin.immichswipe.feature.common.bounce
import com.markvoronin.immichswipe.feature.swipe.models.SwipeCardActions
import com.markvoronin.immichswipe.feature.swipe.models.SwipeCardConfig
import com.markvoronin.immichswipe.feature.swipe.utils.supportsFullResolution
import kotlinx.coroutines.launch

@Composable
fun ActionButtonsOverlay(
    visible: Boolean,
    isNext: Boolean,
    asset: Asset,
    config: SwipeCardConfig,
    actions: SwipeCardActions,
    isMuted: Boolean,
    baseUrl: String,
    context: Context,
    backdrop: Backdrop,
    onToggleMuteWithIndicator: () -> Unit,
    onOpenDetails: () -> Unit,
    // Size of each corner's button group (zero when empty), so the video time pill can move out of their way.
    onCornerSizeChanged: (IconPosition, IntSize) -> Unit = { _, _ -> }
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(MaterialTheme.motionScheme.slowEffectsSpec()),
        exit = fadeOut(MaterialTheme.motionScheme.slowEffectsSpec()),
        modifier = Modifier.fillMaxSize()
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val cornerButtons = IconPosition.entries.associateWith { pos ->
                val buttons = mutableListOf<@Composable () -> Unit>()

                if (config.showFullscreenButton && config.fullscreenButtonPosition == pos) {
                    buttons.add {
                        BouncingActionButton(backdrop = backdrop, enabled = !isNext, onClick = actions.onOpenFullscreen) {
                            ActionIcon(Icons.Default.Fullscreen, stringResource(R.string.settings_fullscreen_pos_label))
                        }
                    }
                }
                if (config.showInfoButton && config.infoButtonPosition == pos) {
                    buttons.add {
                        BouncingActionButton(backdrop = backdrop, enabled = !isNext, onClick = onOpenDetails) {
                            ActionIcon(Icons.Default.Info, stringResource(R.string.swipe_details))
                        }
                    }
                }
                if (config.showRotationButton && asset.type != "VIDEO" && config.rotationButtonPosition == pos) {
                    buttons.add {
                        // Turns with the photo, using the same spring as the photo's own rotation.
                        val rotation by animateFloatAsState(
                            targetValue = config.rotationAngle.toFloat(),
                            animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                            label = "RotateButton"
                        )
                        BouncingActionButton(backdrop = backdrop, enabled = !isNext, onClick = actions.onRotateAsset) {
                            ActionIcon(
                                icon = Icons.AutoMirrored.Filled.RotateRight,
                                contentDescription = stringResource(R.string.swipe_rotate_asset),
                                modifier = Modifier.graphicsLayer { rotationZ = rotation }
                            )
                        }
                    }
                }
                if (config.showImmichButton && config.immichButtonPosition == pos) {
                    buttons.add {
                        BouncingActionButton(
                            backdrop = backdrop,
                            enabled = !isNext,
                            onClick = {
                                ImmichLauncher.openAssetInImmich(context, baseUrl, asset.id, mode = config.immichOpenMode)
                            },
                            onLongClick = {
                                val altMode = if (config.immichOpenMode == ImmichOpenMode.WEB) ImmichOpenMode.APP else ImmichOpenMode.WEB
                                ImmichLauncher.openAssetInImmich(context, baseUrl, asset.id, mode = altMode)
                            }
                        ) {
                            ActionIcon(Icons.AutoMirrored.Filled.OpenInNew, stringResource(R.string.settings_immich_pos_label))
                        }
                    }
                }
                if (config.showMuteButton && config.muteButtonPosition == pos && asset.type == "VIDEO") {
                    buttons.add {
                        val scope = rememberCoroutineScope()
                        val motionScheme = MaterialTheme.motionScheme
                        val muteScale = remember { Animatable(1f) }
                        SwipeActionIconButton(
                            backdrop = backdrop,
                            enabled = !isNext,
                            onClick = {
                                scope.bounce(muteScale, motionScheme)
                                onToggleMuteWithIndicator()
                            }
                        ) {
                            VolumeIcon(
                                isMuted = isMuted,
                                scale = { muteScale.value },
                                contentDescription = stringResource(if (isMuted) R.string.cd_unmute else R.string.cd_mute)
                            )
                        }
                    }
                }
                if (config.showDownloadButton && config.downloadButtonPosition == pos) {
                    buttons.add {
                        val scope = rememberCoroutineScope()
                        val motionScheme = MaterialTheme.motionScheme
                        val downloadScale = remember { Animatable(1f) }
                        val downloadDrop = remember { Animatable(0f) }
                        SwipeActionIconButton(
                            backdrop = backdrop,
                            enabled = !isNext,
                            onClick = {
                                scope.bounce(downloadScale, motionScheme)
                                scope.launch {
                                    downloadDrop.snapTo(0f)
                                    downloadDrop.animateTo(1f, motionScheme.fastSpatialSpec())
                                    downloadDrop.snapTo(-1f)
                                    downloadDrop.animateTo(0f, motionScheme.defaultSpatialSpec())
                                }
                                actions.onDownload(asset)
                            }
                        ) {
                            DownloadIcon(
                                drop = { downloadDrop.value },
                                scale = { downloadScale.value },
                                contentDescription = stringResource(R.string.cd_download)
                            )
                        }
                    }
                }
                if (config.showShareButton && config.shareButtonPosition == pos) {
                    buttons.add {
                        BouncingActionButton(backdrop = backdrop, enabled = !isNext, onClick = { actions.onShare(asset) }) {
                            ActionIcon(Icons.Default.Share, stringResource(R.string.cd_share))
                        }
                    }
                }

                if (config.showFullSizeButton && config.fullSizeButtonPosition == pos && asset.supportsFullResolution) {
                    buttons.add {
                        BouncingActionButton(backdrop = backdrop, enabled = !isNext, onClick = actions.onToggleFullSize) {
                            Crossfade(
                                targetState = config.isFullSize,
                                animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                                label = "FullSizeIcon"
                            ) { isFullSize ->
                                ActionIcon(
                                    if (isFullSize) Icons.Default.Hd else Icons.Outlined.Hd,
                                    stringResource(R.string.cd_full_size)
                                )
                            }
                        }
                    }
                }

                buttons
            }

            val horizontal = config.buttonStackOrientation == ButtonStackOrientation.HORIZONTAL
            val separate = config.buttonStackStyle == ButtonStackStyle.SEPARATE
            val buttonSize = fittingButtonSize(cornerButtons.mapValues { it.value.size }, horizontal, maxWidth, maxHeight, separate)

            cornerButtons.forEach { (pos, buttons) ->
                val alignment = when (pos) {
                    IconPosition.TOP_LEFT -> Alignment.TopStart
                    IconPosition.TOP_RIGHT -> Alignment.TopEnd
                    IconPosition.BOTTOM_LEFT -> Alignment.BottomStart
                    IconPosition.BOTTOM_RIGHT -> Alignment.BottomEnd
                }

                if (buttons.isEmpty()) {
                    SideEffect { onCornerSizeChanged(pos, IntSize.Zero) }
                } else {
                    Box(
                        modifier = Modifier
                            .align(alignment)
                            .onSizeChanged { onCornerSizeChanged(pos, it) }
                            .padding(8.dp)
                    ) {
                        val merged = buttons.size > 1 && !separate
                        CompositionLocalProvider(
                            LocalSwipeActionButtonSize provides buttonSize,
                            LocalSwipeActionButtonMerged provides merged
                        ) {
                            // Several buttons in a corner touch edge to edge inside one pill.
                            val pill = if (merged) {
                                Modifier.glassCircle(backdrop).clip(CircleShape).drawBehind {
                                    // A hairline at each seam, inset from the pill's edges; it takes no layout room.
                                    val inset = buttonSize.toPx() / 4
                                    for (i in 1 until buttons.size) {
                                        val at = buttonSize.toPx() * i
                                        drawLine(
                                            color = Color.White.copy(alpha = 0.2f),
                                            start = if (horizontal) Offset(at, inset) else Offset(inset, at),
                                            end = if (horizontal) Offset(at, size.height - inset) else Offset(size.width - inset, at),
                                            strokeWidth = 1.dp.toPx()
                                        )
                                    }
                                }
                            } else {
                                Modifier
                            }
                            val gap = if (separate) ButtonGap else 0.dp
                            if (horizontal) {
                                Row(modifier = pill, horizontalArrangement = Arrangement.spacedBy(gap)) {
                                    buttons.forEach { button -> button() }
                                }
                            } else {
                                Column(modifier = pill, verticalArrangement = Arrangement.spacedBy(gap)) {
                                    buttons.forEach { button -> button() }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private val ButtonGap = 8.dp
private val CornerInset = 8.dp
private val MinButtonSize = 40.dp

// A corner pair per edge, and whether that edge is a row across the card.
private val CardEdges = listOf(
    listOf(IconPosition.TOP_LEFT, IconPosition.TOP_RIGHT) to true,
    listOf(IconPosition.BOTTOM_LEFT, IconPosition.BOTTOM_RIGHT) to true,
    listOf(IconPosition.TOP_LEFT, IconPosition.BOTTOM_LEFT) to false,
    listOf(IconPosition.TOP_RIGHT, IconPosition.BOTTOM_RIGHT) to false
)

/**
 * The button size at which every edge's corner groups fit the card, which keeps its 8dp corner inset (and, when `separate`, the 8dp gap between buttons):
 * the full 48dp when they fit, otherwise shrunk alike, never below 40dp. `counts` is the number of buttons in each corner.
 */
internal fun fittingButtonSize(counts: Map<IconPosition, Int>, horizontal: Boolean, width: Dp, height: Dp, separate: Boolean = false): Dp {
    val fitting = CardEdges.minOf { (corners, isRow) ->
        val groups = corners.map { counts[it] ?: 0 }.filter { it > 0 }
        // A stack running along the edge needs all its buttons' length; one across it, a single button's.
        val alongEdge = isRow == horizontal
        val buttons = groups.sumOf { if (alongEdge) it else 1 }
        val gaps = if (separate) groups.sumOf { if (alongEdge) it - 1 else 0 } else 0
        val room = (if (isRow) width else height) - CornerInset * 2 * groups.size - ButtonGap * gaps
        if (buttons == 0) Float.MAX_VALUE else room.value / buttons
    }
    return fitting.dp.coerceIn(MinButtonSize, SwipeActionButtonSize)
}

/** A glass action button whose content squashes and springs back when tapped or long-pressed. */
@Composable
private fun BouncingActionButton(
    backdrop: Backdrop,
    enabled: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val motionScheme = MaterialTheme.motionScheme
    val scale = remember { Animatable(1f) }
    SwipeActionIconButton(
        backdrop = backdrop,
        enabled = enabled,
        onClick = {
            scope.bounce(scale, motionScheme)
            onClick()
        },
        onLongClick = onLongClick?.let { longClick ->
            {
                scope.bounce(scale, motionScheme)
                longClick()
            }
        }
    ) {
        Box(
            modifier = Modifier.graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
        ) {
            content()
        }
    }
}

@Composable
private fun ActionIcon(icon: ImageVector, contentDescription: String, modifier: Modifier = Modifier) {
    Icon(imageVector = icon, contentDescription = contentDescription, modifier = modifier.size(24.dp))
}
