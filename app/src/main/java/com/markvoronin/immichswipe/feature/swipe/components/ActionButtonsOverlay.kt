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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Hd
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.core.ButtonStackOrientation
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
    onToggleMuteWithIndicator: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(MaterialTheme.motionScheme.slowEffectsSpec()),
        exit = fadeOut(MaterialTheme.motionScheme.slowEffectsSpec()),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            IconPosition.entries.forEach { pos ->
                val alignment = when (pos) {
                    IconPosition.TOP_LEFT -> Alignment.TopStart
                    IconPosition.TOP_RIGHT -> Alignment.TopEnd
                    IconPosition.BOTTOM_LEFT -> Alignment.BottomStart
                    IconPosition.BOTTOM_RIGHT -> Alignment.BottomEnd
                }

                val buttons = mutableListOf<@Composable () -> Unit>()

                if (config.showFullscreenButton && config.fullscreenButtonPosition == pos) {
                    buttons.add {
                        BouncingActionButton(backdrop = backdrop, enabled = !isNext, onClick = actions.onOpenFullscreen) {
                            ActionIcon(Icons.Default.Fullscreen, stringResource(R.string.settings_fullscreen_pos_label))
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

                if (buttons.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(alignment)
                            .padding(8.dp)
                    ) {
                        if (config.buttonStackOrientation == ButtonStackOrientation.HORIZONTAL) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                buttons.forEach { button -> button() }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                buttons.forEach { button -> button() }
                            }
                        }
                    }
                }
            }
        }
    }
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
