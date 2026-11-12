package com.markvoronin.immichswipe.feature.duplicates.components

import android.annotation.SuppressLint
import android.view.LayoutInflater
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.core.cache.VideoCache
import com.markvoronin.immichswipe.core.player.PlayerLoadControlFactory
import com.markvoronin.immichswipe.domain.model.Asset
import com.markvoronin.immichswipe.feature.common.MediaLoadingIndicator
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@OptIn(UnstableApi::class)
@Composable
fun DuplicateVideoPlayer(
    asset: Asset,
    modifier: Modifier = Modifier,
    baseUrl: String = "",
    // False when the playback setting says to ignore other apps' audio.
    handleAudioFocus: Boolean = true,
    contentScale: ContentScale = ContentScale.Fit,
    // Off when a wrapper handles taps itself, such as the zoom box in the duplicates fullscreen.
    tapToPause: Boolean = true,
    controlsVisible: Boolean = true,
    // True while the video is actually playing, not while it loads or is paused.
    onIsPlayingChanged: (Boolean) -> Unit = {},
    // Set by a caller that keeps mute across videos; null keeps it per video.
    muted: Boolean? = null,
    onMutedChange: (Boolean) -> Unit = {},
    videoSurfaceWrapper: @Composable (surfaceContent: @Composable () -> Unit) -> Unit = { surface -> surface() }
) {
    val context = LocalContext.current
    val effectiveBaseUrl = baseUrl.removeSuffix("/")

    var isVideoReady by remember(asset.id) { mutableStateOf(false) }
    var localMuted by remember(asset.id) { mutableStateOf(false) }
    val isMuted = muted ?: localMuted
    var isPlaying by remember(asset.id) { mutableStateOf(true) }

    var currentTime by remember(asset.id) { mutableLongStateOf(0L) }
    var duration by remember(asset.id) { mutableLongStateOf(0L) }
    var isScrubbing by remember(asset.id) { mutableStateOf(false) }
    var scrubValue by remember(asset.id) { mutableLongStateOf(0L) }

    var exoPlayer by remember(asset.id, handleAudioFocus, effectiveBaseUrl) { mutableStateOf<ExoPlayer?>(null) }

    val currentOnIsPlayingChanged by rememberUpdatedState(onIsPlayingChanged)
    LaunchedEffect(isPlaying, isVideoReady) {
        currentOnIsPlayingChanged(isPlaying && isVideoReady)
    }

    DisposableEffect(asset.id, handleAudioFocus, effectiveBaseUrl) {
        if (effectiveBaseUrl.isEmpty()) {
            return@DisposableEffect onDispose {}
        }

        val loadControl = PlayerLoadControlFactory.createDuplicateLoadControl(context)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        val player = ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, handleAudioFocus)
            .build().apply {
                repeatMode = Player.REPEAT_MODE_ONE
                val videoUrl = "$effectiveBaseUrl/api/assets/${asset.id}/video/playback"
                val dataSourceFactory = VideoCache.getCacheDataSourceFactory(context)
                val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(
                        MediaItem.Builder()
                            .setUri(videoUrl)
                            .setMediaId(asset.id)
                            .setCustomCacheKey(asset.id)
                            .build()
                    )
                setMediaSource(mediaSource)
                prepare()
                playWhenReady = true
            }

        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    isVideoReady = true
                }
            }
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (playing) {
                    isVideoReady = true
                }
            }
        }
        player.addListener(listener)
        exoPlayer = player

        onDispose {
            player.removeListener(listener)
            player.stop()
            player.release()
            exoPlayer = null
        }
    }

    LaunchedEffect(exoPlayer, asset.id) {
        val player = exoPlayer ?: return@LaunchedEffect
        while (true) {
            if (!isScrubbing) {
                currentTime = player.currentPosition
                duration = player.duration.coerceAtLeast(0L)
            }
            delay(200.milliseconds)
        }
    }

    val outerBoxModifier = if (!tapToPause) {
        modifier
    } else {
        modifier.clickable {
            val player = exoPlayer ?: return@clickable
            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
        }
    }

    Box(
        modifier = outerBoxModifier
    ) {
        videoSurfaceWrapper {
            Box(modifier = Modifier.fillMaxSize()) {
                if (effectiveBaseUrl.isNotEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data("$effectiveBaseUrl/api/assets/${asset.id}/thumbnail?format=WEBP&size=preview")
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = contentScale,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                @SuppressLint("InflateParams")
                AndroidView(
                    factory = { ctx ->
                        val view = LayoutInflater.from(ctx).inflate(R.layout.view_player_texture, null) as PlayerView
                        view.useController = false
                        view.resizeMode = if (contentScale == ContentScale.Crop) {
                            AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        } else {
                            AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                        view.player = exoPlayer
                        view
                    },
                    update = { view ->
                        if (view.player != exoPlayer) {
                            view.player = exoPlayer
                        }
                        exoPlayer?.volume = if (isMuted) 0f else 1f
                    },
                    onRelease = { view ->
                        view.player = null
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = if (isVideoReady) 1f else 0f
                        }
                )
            }
        }

        if (!isVideoReady) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                MediaLoadingIndicator()
            }
        }

        // One light row over a soft scrim: plain icons, inline times and a thin seekbar.
        val controlColor = Color.White
        val timeStyle = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum")
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
            exit = fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))))
                    .padding(start = 4.dp, end = 4.dp, top = 16.dp, bottom = 4.dp)
            ) {
                IconButton(
                    onClick = {
                        val player = exoPlayer ?: return@IconButton
                        if (player.isPlaying) {
                            player.pause()
                        } else {
                            player.play()
                        }
                    },
                    shapes = IconButtonDefaults.shapes(),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = controlColor)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) stringResource(R.string.cd_pause) else stringResource(R.string.cd_play)
                    )
                }

                val timeToDisplay = if (isScrubbing) scrubValue else currentTime
                if (duration > 0) {
                    Text(text = formatMediaTime(timeToDisplay), color = controlColor, style = timeStyle)
                    Spacer(modifier = Modifier.width(12.dp))
                }

                // SliderState works in 0..1 fractions of the duration.
                val seekBarColors = SliderDefaults.colors(inactiveTrackColor = controlColor.copy(alpha = 0.3f))
                val durationMs = duration.toFloat().coerceAtLeast(1f)
                val sliderState = remember { SliderState() }
                val sliderInteraction = remember { MutableInteractionSource() }
                SideEffect {
                    sliderState.value = (timeToDisplay.toFloat() / durationMs).coerceIn(0f, 1f)
                }

                Slider(
                    state = sliderState,
                    onValueChange = {
                        sliderState.value = it
                        isScrubbing = true
                        scrubValue = (it * durationMs).toLong()
                        exoPlayer?.seekTo(scrubValue)
                    },
                    onValueChangeFinished = {
                        isScrubbing = false
                        exoPlayer?.seekTo(scrubValue)
                    },
                    colors = seekBarColors,
                    interactionSource = sliderInteraction,
                    thumb = {
                        SliderDefaults.Thumb(
                            interactionSource = sliderInteraction,
                            colors = seekBarColors,
                            thumbSize = DpSize(4.dp, 20.dp)
                        )
                    },
                    track = {
                        SliderDefaults.Track(
                            sliderState = it,
                            colors = seekBarColors,
                            drawStopIndicator = null,
                            thumbTrackGapSize = 4.dp,
                            modifier = Modifier.height(4.dp)
                        )
                    },
                    modifier = Modifier.weight(1f)
                )

                if (duration > 0) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = formatMediaTime(duration), color = controlColor, style = timeStyle)
                }

                IconToggleButton(
                    checked = isMuted,
                    onCheckedChange = {
                        localMuted = it
                        onMutedChange(it)
                        exoPlayer?.volume = if (it) 0f else 1f
                    },
                    shapes = IconButtonDefaults.toggleableShapes(),
                    colors = IconButtonDefaults.iconToggleButtonColors(
                        contentColor = controlColor,
                        checkedContentColor = controlColor
                    )
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = if (isMuted) stringResource(R.string.cd_unmute) else stringResource(R.string.cd_mute)
                    )
                }
            }
        }
    }
}
