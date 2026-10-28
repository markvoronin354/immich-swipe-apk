package com.markvoronin.immichswipe.feature.swipe.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.outlined.Hd
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.core.ButtonStackOrientation
import com.markvoronin.immichswipe.core.IconPosition
import com.markvoronin.immichswipe.core.ImmichLauncher
import com.markvoronin.immichswipe.domain.model.Asset
import com.markvoronin.immichswipe.feature.swipe.models.SwipeCardActions
import com.markvoronin.immichswipe.feature.swipe.models.SwipeCardConfig
import com.markvoronin.immichswipe.feature.swipe.utils.supportsFullResolution

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
                        SwipeActionIconButton(
                            icon = Icons.Default.Fullscreen,
                            contentDescription = stringResource(R.string.settings_fullscreen_pos_label),
                            enabled = !isNext,
                            onClick = actions.onOpenFullscreen
                        )
                    }
                }
                if (config.showRotationButton && asset.type != "VIDEO" && config.rotationButtonPosition == pos) {
                    buttons.add {
                        SwipeActionIconButton(
                            icon = Icons.AutoMirrored.Filled.RotateRight,
                            contentDescription = stringResource(R.string.swipe_rotate_asset),
                            enabled = !isNext,
                            onClick = actions.onRotateAsset
                        )
                    }
                }
                if (config.showImmichButton && config.immichButtonPosition == pos) {
                    buttons.add {
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
                }
                if (config.showMuteButton && config.muteButtonPosition == pos && asset.type == "VIDEO") {
                    buttons.add {
                        SwipeActionIconButton(
                            icon = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = stringResource(R.string.cd_mute),
                            enabled = !isNext,
                            onClick = onToggleMuteWithIndicator
                        )
                    }
                }
                if (config.showDownloadButton && config.downloadButtonPosition == pos) {
                    buttons.add {
                        SwipeActionIconButton(
                            icon = Icons.Default.FileDownload,
                            contentDescription = stringResource(R.string.cd_download),
                            enabled = !isNext,
                            onClick = { actions.onDownload(asset) }
                        )
                    }
                }
                if (config.showShareButton && config.shareButtonPosition == pos) {
                    buttons.add {
                        SwipeActionIconButton(
                            icon = Icons.Default.Share,
                            contentDescription = stringResource(R.string.cd_share),
                            enabled = !isNext,
                            onClick = { actions.onShare(asset) }
                        )
                    }
                }

                if (config.showFullSizeButton && config.fullSizeButtonPosition == pos && asset.supportsFullResolution) {
                    buttons.add {
                        SwipeActionIconButton(
                            icon = if (config.isFullSize) Icons.Default.Hd else Icons.Outlined.Hd,
                            contentDescription = stringResource(R.string.cd_full_size),
                            enabled = !isNext,
                            onClick = actions.onToggleFullSize
                        )
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
