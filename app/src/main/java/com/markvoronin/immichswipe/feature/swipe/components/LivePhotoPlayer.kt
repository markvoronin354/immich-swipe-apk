package com.markvoronin.immichswipe.feature.swipe.components

import android.annotation.SuppressLint
import android.view.LayoutInflater
import androidx.annotation.OptIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.kyant.backdrop.Backdrop
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.core.AppLogger
import com.markvoronin.immichswipe.core.cache.VideoCache

/**
 * Plays a live photo's motion clip once over the photo. The player exists only while this is composed, and the surface
 * stays transparent until the first frame so the photo doesn't flash.
 */
@OptIn(UnstableApi::class)
@SuppressLint("InflateParams")
@Composable
fun LivePhotoSurface(
    videoId: String,
    baseUrl: String,
    isMuted: Boolean,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentOnFinished by rememberUpdatedState(onFinished)
    var hasFrame by remember(videoId) { mutableStateOf(false) }

    val player = remember(videoId) {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.Builder()
                .setUri("$baseUrl/api/assets/$videoId/video/playback")
                .setMediaId(videoId)
                .setCustomCacheKey(videoId)
                .build()
            setMediaSource(ProgressiveMediaSource.Factory(VideoCache.getCacheDataSourceFactory(context)).createMediaSource(mediaItem))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                hasFrame = true
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) currentOnFinished()
            }

            override fun onPlayerError(error: PlaybackException) {
                AppLogger.e("LivePhoto", "Playback error for clip=$videoId: ${error.message}", error)
                currentOnFinished()
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(player, isMuted) {
        player.volume = if (isMuted) 0f else 1f
    }

    val alpha by animateFloatAsState(
        targetValue = if (hasFrame) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "LivePhotoAlpha"
    )

    AndroidView(
        factory = { ctx ->
            (LayoutInflater.from(ctx).inflate(R.layout.view_player_texture, null) as PlayerView).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
        },
        update = { view -> if (view.player !== player) view.player = player },
        onRelease = { view -> view.player = null },
        modifier = modifier.graphicsLayer { this.alpha = alpha }
    )
}

/**
 * The "LIVE" pill on a live photo, on the card and in fullscreen; a tap plays the motion clip, or stops it while it plays.
 * As tall as the video time pill (32dp) and wider than the size pill, so it is easy to hit.
 */
@Composable
fun LivePhotoPill(backdrop: Backdrop, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .glassSurface(backdrop, CircleShape, 12.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .defaultMinSize(minWidth = 72.dp, minHeight = 32.dp)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.swipe_live_photo),
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}
