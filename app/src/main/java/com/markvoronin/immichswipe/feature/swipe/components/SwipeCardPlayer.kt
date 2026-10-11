package com.markvoronin.immichswipe.feature.swipe.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

@Composable
fun rememberExoPlayerWithLifecycle(
    exoPlayer: ExoPlayer?,
    assetId: String,
    lifecycleOwner: LifecycleOwner,
    isFullscreenOpen: Boolean,
    isNext: Boolean,
    isMuted: Boolean,
    internalExoPlayer: ExoPlayer?,
    onVideoReadyChange: (Boolean) -> Unit,
    onShowLoadingIndicatorChange: (Boolean) -> Unit
) {
    DisposableEffect(exoPlayer, lifecycleOwner, assetId) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                val isSameAsset = exoPlayer?.currentMediaItem?.mediaId == assetId
                if (isSameAsset) {
                    onVideoReadyChange(true)
                    onShowLoadingIndicatorChange(false)
                }
            }
            override fun onPlaybackStateChanged(state: Int) {
                val isSameAsset = exoPlayer?.currentMediaItem?.mediaId == assetId
                if (state == Player.STATE_READY && isSameAsset) {
                    if ((exoPlayer.videoSize.height) == 0) {
                        onVideoReadyChange(true)
                    }
                    onShowLoadingIndicatorChange(false)
                } else if (state == Player.STATE_BUFFERING) {
                    // Only show indicator if buffering the correct asset, but keep isVideoReady true if it was already ready
                    if (isSameAsset) onShowLoadingIndicatorChange(true)
                } else if (state == Player.STATE_ENDED || state == Player.STATE_IDLE) {
                    onShowLoadingIndicatorChange(false)
                }
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                val isSameAsset = exoPlayer?.currentMediaItem?.mediaId == assetId
                if (isPlaying && isSameAsset) {
                    onShowLoadingIndicatorChange(false)
                }
            }
        }
        exoPlayer?.addListener(listener)
        var wasPlayingBeforeAppPause = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    if (!isFullscreenOpen) {
                        wasPlayingBeforeAppPause = exoPlayer?.playWhenReady == true
                        exoPlayer?.playWhenReady = false
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (!isNext && !isFullscreenOpen && wasPlayingBeforeAppPause) {
                        exoPlayer?.playWhenReady = true
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        exoPlayer?.volume = if (isMuted) 0f else 1f

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer?.removeListener(listener)
            internalExoPlayer?.stop()
            internalExoPlayer?.release()
        }
    }
}
