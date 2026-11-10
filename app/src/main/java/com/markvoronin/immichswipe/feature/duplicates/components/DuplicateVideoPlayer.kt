package com.markvoronin.immichswipe.feature.duplicates.components

import android.annotation.SuppressLint
import android.view.LayoutInflater
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import com.markvoronin.immichswipe.core.PlaybackBehavior
import com.markvoronin.immichswipe.core.cache.VideoCache
import com.markvoronin.immichswipe.core.player.PlayerLoadControlFactory
import com.markvoronin.immichswipe.data.datastore.SessionDataStore
import com.markvoronin.immichswipe.domain.model.Asset
import com.markvoronin.immichswipe.feature.common.MediaLoadingIndicator
import com.markvoronin.immichswipe.feature.swipe.components.previewButtonColors
import com.markvoronin.immichswipe.feature.swipe.components.previewTileColor
import com.markvoronin.immichswipe.feature.swipe.components.previewToggleColors
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicateVideoPlayer(
    asset: Asset,
    modifier: Modifier = Modifier,
    baseUrl: String = "",
    contentScale: ContentScale = ContentScale.Fit,
    onTap: (() -> Unit)? = null,
    videoSurfaceWrapper: @Composable (surfaceContent: @Composable () -> Unit) -> Unit = { surface -> surface() }
) {
    val context = LocalContext.current
    val sessionDataStore = remember(context) { SessionDataStore(context.applicationContext) }
    val storedBaseUrl by sessionDataStore.getBaseUrl().collectAsState(initial = null)

    val effectiveBaseUrl = (baseUrl.ifBlank { storedBaseUrl ?: "" }).removeSuffix("/")

    val playbackBehaviorStr by sessionDataStore.getAudioFocusMode().collectAsState(initial = null)
    val playbackBehavior = remember(playbackBehaviorStr) {
        playbackBehaviorStr?.let { try { PlaybackBehavior.valueOf(it) } catch(_: Exception) { PlaybackBehavior.PAUSE_OTHERS } } ?: PlaybackBehavior.PAUSE_OTHERS
    }
    val handleAudioFocus = (playbackBehavior != PlaybackBehavior.IGNORE)

    var isVideoReady by remember(asset.id) { mutableStateOf(false) }
    var isMuted by remember(asset.id) { mutableStateOf(false) }
    var isPlaying by remember(asset.id) { mutableStateOf(true) }

    var currentTime by remember(asset.id) { mutableLongStateOf(0L) }
    var duration by remember(asset.id) { mutableLongStateOf(0L) }
    var isScrubbing by remember(asset.id) { mutableStateOf(false) }
    var scrubValue by remember(asset.id) { mutableLongStateOf(0L) }

    var exoPlayer by remember(asset.id, handleAudioFocus, effectiveBaseUrl) { mutableStateOf<ExoPlayer?>(null) }

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

    val outerBoxModifier = if (onTap != null) {
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

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Same translucent tiles as the fullscreen toolbars.
            val tileColor = previewTileColor()
            if (duration > 0) {
                val timeToDisplay = if (isScrubbing) scrubValue else currentTime
                Box(modifier = Modifier.background(tileColor, CircleShape)) {
                    Text(
                        text = "${formatMediaTime(timeToDisplay)} / ${formatMediaTime(duration)}",
                        color = FloatingToolbarDefaults.vibrantFloatingToolbarColors().toolbarContentColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                FilledTonalIconButton(
                    onClick = {
                        val player = exoPlayer ?: return@FilledTonalIconButton
                        if (player.isPlaying) {
                            player.pause()
                        } else {
                            player.play()
                        }
                    },
                    shapes = IconButtonDefaults.shapes(),
                    colors = previewButtonColors(),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) stringResource(R.string.cd_pause) else stringResource(R.string.cd_play),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // SliderState works in 0..1 fractions of the duration.
                val seekBarColors = SliderDefaults.colors(inactiveTrackColor = tileColor)
                val durationMs = duration.toFloat().coerceAtLeast(1f)
                val sliderState = remember { SliderState() }
                SideEffect {
                    sliderState.value = ((if (isScrubbing) scrubValue else currentTime).toFloat() / durationMs).coerceIn(0f, 1f)
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
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                FilledTonalIconToggleButton(
                    checked = isMuted,
                    onCheckedChange = {
                        isMuted = it
                        exoPlayer?.volume = if (isMuted) 0f else 1f
                    },
                    shapes = IconButtonDefaults.toggleableShapes(),
                    colors = previewToggleColors(),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = if (isMuted) stringResource(R.string.cd_unmute) else stringResource(R.string.cd_mute),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
