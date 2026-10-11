package com.markvoronin.immichswipe.feature.swipe.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.core.IconPosition
import com.markvoronin.immichswipe.core.ImmichLauncher
import com.markvoronin.immichswipe.domain.model.Asset
import com.markvoronin.immichswipe.feature.swipe.models.SwipeCardActions
import com.markvoronin.immichswipe.feature.swipe.models.SwipeCardConfig
import com.markvoronin.immichswipe.feature.swipe.utils.toHorizontalAlignment

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
    onToggleMuteWithIndicator: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            listOf(Alignment.Start, Alignment.End).forEach { side ->
                Column(
                    modifier = Modifier
                        .align(if (side == Alignment.Start) Alignment.TopStart else Alignment.TopEnd)
                        .fillMaxHeight()
                        .padding(8.dp),
                    horizontalAlignment = side
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (config.showFullscreenButton && config.fullscreenButtonPosition.toHorizontalAlignment() == side && (config.fullscreenButtonPosition == IconPosition.TOP_LEFT || config.fullscreenButtonPosition == IconPosition.TOP_RIGHT)) {
                            SwipeActionIconButton(
                                icon = Icons.Default.Fullscreen,
                                contentDescription = stringResource(R.string.settings_fullscreen_pos_label),
                                enabled = !isNext,
                                onClick = actions.onOpenFullscreen
                            )
                        }
                        if (config.showRotationButton && asset.type != "VIDEO" && config.rotationButtonPosition.toHorizontalAlignment() == side && (config.rotationButtonPosition == IconPosition.TOP_LEFT || config.rotationButtonPosition == IconPosition.TOP_RIGHT)) {
                            SwipeActionIconButton(
                                icon = Icons.AutoMirrored.Filled.RotateRight,
                                contentDescription = stringResource(R.string.swipe_rotate_asset),
                                enabled = !isNext,
                                onClick = actions.onRotateAsset
                            )
                        }
                        if (config.showImmichButton && config.immichButtonPosition.toHorizontalAlignment() == side && (config.immichButtonPosition == IconPosition.TOP_LEFT || config.immichButtonPosition == IconPosition.TOP_RIGHT)) {
                            SwipeActionIconButton(
                                icon = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = stringResource(R.string.settings_immich_pos_label),
                                enabled = !isNext,
                                onClick = {
                                    ImmichLauncher.openAssetInImmich(context, baseUrl, asset.id, mode = config.immichOpenMode)
                                },
                                onLongClick = if (config.immichLongPressWeb && baseUrl.isNotBlank()) {
                                    { ImmichLauncher.openInWeb(context, baseUrl, asset.id) }
                                } else null
                            )
                        }
                        if (config.showMuteButton && config.muteButtonPosition.toHorizontalAlignment() == side && (config.muteButtonPosition == IconPosition.TOP_LEFT || config.muteButtonPosition == IconPosition.TOP_RIGHT) && asset.type == "VIDEO") {
                            SwipeActionIconButton(
                                icon = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Mute",
                                enabled = !isNext,
                                onClick = onToggleMuteWithIndicator
                            )
                        }
                        if (config.showDownloadButton && config.downloadButtonPosition.toHorizontalAlignment() == side && (config.downloadButtonPosition == IconPosition.TOP_LEFT || config.downloadButtonPosition == IconPosition.TOP_RIGHT)) {
                            SwipeActionIconButton(
                                icon = Icons.Default.FileDownload,
                                contentDescription = "Download",
                                enabled = !isNext,
                                onClick = { actions.onDownload(asset) }
                            )
                        }
                        if (config.showShareButton && config.shareButtonPosition.toHorizontalAlignment() == side && (config.shareButtonPosition == IconPosition.TOP_LEFT || config.shareButtonPosition == IconPosition.TOP_RIGHT)) {
                            SwipeActionIconButton(
                                icon = Icons.Default.Share,
                                contentDescription = "Share",
                                enabled = !isNext,
                                onClick = { actions.onShare(asset) }
                            )
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (config.showFullscreenButton && config.fullscreenButtonPosition.toHorizontalAlignment() == side && (config.fullscreenButtonPosition == IconPosition.BOTTOM_LEFT || config.fullscreenButtonPosition == IconPosition.BOTTOM_RIGHT)) {
                            SwipeActionIconButton(
                                icon = Icons.Default.Fullscreen,
                                contentDescription = stringResource(R.string.settings_fullscreen_pos_label),
                                enabled = !isNext,
                                onClick = actions.onOpenFullscreen
                            )
                        }
                        if (config.showRotationButton && asset.type != "VIDEO" && config.rotationButtonPosition.toHorizontalAlignment() == side && (config.rotationButtonPosition == IconPosition.BOTTOM_LEFT || config.rotationButtonPosition == IconPosition.BOTTOM_RIGHT)) {
                            SwipeActionIconButton(
                                icon = Icons.AutoMirrored.Filled.RotateRight,
                                contentDescription = stringResource(R.string.swipe_rotate_asset),
                                enabled = !isNext,
                                onClick = actions.onRotateAsset
                            )
                        }
                        if (config.showImmichButton && config.immichButtonPosition.toHorizontalAlignment() == side && (config.immichButtonPosition == IconPosition.BOTTOM_LEFT || config.immichButtonPosition == IconPosition.BOTTOM_RIGHT)) {
                            SwipeActionIconButton(
                                icon = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = stringResource(R.string.settings_immich_pos_label),
                                enabled = !isNext,
                                onClick = {
                                    ImmichLauncher.openAssetInImmich(context, baseUrl, asset.id, mode = config.immichOpenMode)
                                },
                                onLongClick = if (config.immichLongPressWeb && baseUrl.isNotBlank()) {
                                    { ImmichLauncher.openInWeb(context, baseUrl, asset.id) }
                                } else null
                            )
                        }
                        if (config.showMuteButton && config.muteButtonPosition.toHorizontalAlignment() == side && (config.muteButtonPosition == IconPosition.BOTTOM_LEFT || config.muteButtonPosition == IconPosition.BOTTOM_RIGHT) && asset.type == "VIDEO") {
                            SwipeActionIconButton(
                                icon = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Mute",
                                enabled = !isNext,
                                onClick = onToggleMuteWithIndicator
                            )
                        }
                        if (config.showDownloadButton && config.downloadButtonPosition.toHorizontalAlignment() == side && (config.downloadButtonPosition == IconPosition.BOTTOM_LEFT || config.downloadButtonPosition == IconPosition.BOTTOM_RIGHT)) {
                            SwipeActionIconButton(
                                icon = Icons.Default.FileDownload,
                                contentDescription = "Download",
                                enabled = !isNext,
                                onClick = { actions.onDownload(asset) }
                            )
                        }
                        if (config.showShareButton && config.shareButtonPosition.toHorizontalAlignment() == side && (config.shareButtonPosition == IconPosition.BOTTOM_LEFT || config.shareButtonPosition == IconPosition.BOTTOM_RIGHT)) {
                            SwipeActionIconButton(
                                icon = Icons.Default.Share,
                                contentDescription = "Share",
                                enabled = !isNext,
                                onClick = { actions.onShare(asset) }
                            )
                        }
                    }
                }
            }
        }
    }
}
