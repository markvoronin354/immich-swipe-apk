package com.markvoronin.immichswipe.feature.swipe.components

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.outlined.VideocamOff
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Precision
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.core.AppLogger
import com.markvoronin.immichswipe.core.CardDisplayMode
import com.markvoronin.immichswipe.feature.swipe.utils.formatMediaTime
import com.markvoronin.immichswipe.feature.swipe.utils.formatSize
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds


private fun rimBrush() = Brush.linearGradient(
    listOf(Color.White.copy(alpha = 0.5f), Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.35f))
)

/** Small glass-look surface for the player controls: dark scrim plus rim highlight. */
internal fun Modifier.glassSurface(shape: Shape): Modifier = this
    .background(Color.Black.copy(alpha = 0.25f), shape)
    .border(1.dp, rimBrush(), shape)

/** `backdrop` and `lensHeight` are unused; kept so call sites don't change. */
@Suppress("UNUSED_PARAMETER")
internal fun Modifier.glassSurface(backdrop: Backdrop, shape: Shape, lensHeight: Dp): Modifier = glassSurface(shape)

/** Pauses a playing video, or resumes it (restarting from the beginning if it has ended). */
internal fun Player.togglePlayPause() {
    if (playWhenReady && playbackState != Player.STATE_ENDED) {
        pause()
    } else {
        if (playbackState == Player.STATE_ENDED) {
            seekTo(0)
        }
        play()
    }
}

@OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedVideoPlayer(
    player: Player,
    isFullscreen: Boolean,
    assetId: String? = null,
    baseUrl: String = "",
    isMuted: Boolean = false,
    isPaused: Boolean = false,
    isVideoReady: Boolean = true,
    toggleControllerTrigger: Int = 0,
    showControls: Boolean = true,
    cardDisplayMode: CardDisplayMode = CardDisplayMode.FIT,
    fileSize: Long? = null,
    showSize: Boolean = false,
    onControllerVisibilityChanged: ((Boolean) -> Unit)? = null,
    controlsOffset: Dp = 0.dp,
    // Lets the caller record the video surface (without these controls) into a backdrop it owns.
    backdrop: LayerBackdrop? = null,
    videoSurfaceWrapper: @Composable (surfaceContent: @Composable () -> Unit) -> Unit = { surfaceContent -> surfaceContent() },
) {
    var currentTime by remember(assetId) { mutableLongStateOf(0L) }
    var duration by remember(assetId) { mutableLongStateOf(0L) }
    var isPlayWhenReady by remember(player) { mutableStateOf(player.playWhenReady) }
    var isScrubbing by remember(assetId) { mutableStateOf(value = false) }
    var scrubValue by remember(assetId) { mutableLongStateOf(0L) }

    var hasPlaybackError by remember(player, assetId) { mutableStateOf(false) }

        var currentMediaId by remember(player, assetId) { mutableStateOf(player.currentMediaItem?.mediaId) }

        DisposableEffect(player, assetId) {
            val listener = object : Player.Listener {
                override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                    isPlayWhenReady = playWhenReady
                }
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    currentMediaId = mediaItem?.mediaId
                    hasPlaybackError = false
                }
                override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                    currentMediaId = player.currentMediaItem?.mediaId
                }
                override fun onPlaybackStateChanged(playbackState: Int) {
                    // Force a re-check of the media ID when state changes
                    val newMediaId = player.currentMediaItem?.mediaId
                    if (currentMediaId != newMediaId) {
                        currentMediaId = newMediaId
                        hasPlaybackError = false
                    }
                }
                override fun onPlayerError(error: PlaybackException) {
                    AppLogger.e("VideoPlayer", "Playback error for asset=$assetId: ${error.message}", error)
                    hasPlaybackError = true
                }
            }
            player.addListener(listener)
            currentMediaId = player.currentMediaItem?.mediaId
            onDispose { player.removeListener(listener) }
        }

        val isCorrectMediaBound = (assetId == null || currentMediaId == assetId)
        val videoAlpha = if (isVideoReady && isCorrectMediaBound) 1f else 0f

        LaunchedEffect(player, isPaused, assetId) {
            AppLogger.d("VideoPlayer", "SharedVideoPlayer Effect: asset=$assetId, isPaused=$isPaused, isFullscreen=$isFullscreen")
            if (isPaused) {
                player.pause()
            } else {
                while (true) {
                    if (!isScrubbing) {
                        currentTime = player.currentPosition
                        duration = player.duration.coerceAtLeast(0L)
                    }
                    delay(500.milliseconds)
                }
            }
        }

        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val baseUrlClean = baseUrl.removeSuffix("/")

        // Records the video itself (not the controls) so the glass controls can refract it without a feedback loop.
        // Empty (letterbox) pixels are filled with what is really behind them: black fullscreen, the card colour on the card.
        val emptyColor = if (isFullscreen) Color.Black else CardDefaults.cardColors().containerColor
        val ownBackdrop = rememberLayerBackdrop {
            drawRect(emptyColor)
            drawContent()
        }
        val playerBackdrop = backdrop ?: ownBackdrop

        Box(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize().layerBackdrop(playerBackdrop)) {
            videoSurfaceWrapper {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (assetId != null && baseUrlClean.isNotEmpty() && !isVideoReady) {
                        val context = LocalContext.current
                        val thumbnailRequest = remember(assetId, baseUrlClean) {
                            ImageRequest.Builder(context)
                                .data("$baseUrlClean/api/assets/$assetId/thumbnail?format=WEBP&size=preview&edited=true")
                                .memoryCacheKey("$assetId-preview")
                                .placeholderMemoryCacheKey("$assetId-preview")
                                .crossfade(false)
                                .precision(Precision.INEXACT)
                                .build()
                        }
                        SubcomposeAsyncImage(
                            model = thumbnailRequest,
                            contentDescription = null,
                            contentScale = if (isFullscreen) {
                                ContentScale.Fit
                            } else {
                                if (cardDisplayMode == CardDisplayMode.FILL) ContentScale.Crop else ContentScale.Fit
                            },
                            modifier = Modifier.fillMaxSize(),
                            error = {
                                AssetErrorView(
                                    title = stringResource(R.string.swipe_video_error),
                                    icon = Icons.Outlined.VideocamOff
                                )
                            }
                        )
                    }

                    if (hasPlaybackError) {
                        AssetErrorView(
                            title = stringResource(R.string.swipe_video_error),
                            icon = Icons.Outlined.VideocamOff
                        )
                    }

                    val playerViewRef = remember { mutableStateOf<PlayerView?>(null) }
                    
                    LaunchedEffect(toggleControllerTrigger) {
                        if (toggleControllerTrigger > 0 && isFullscreen) {
                            playerViewRef.value?.let { view ->
                                AppLogger.d("VideoPlayer", "Toggling controller: visible=${view.isControllerFullyVisible}")
                                if (view.isControllerFullyVisible) view.hideController() else view.showController()
                            }
                        }
                    }

                    key(isFullscreen) { // Only re-create AndroidView when switching to/from fullscreen. DO NOT key by assetId.
                        @SuppressLint("InflateParams")
                        AndroidView(
                            factory = { context ->
                                AppLogger.d("VideoPlayer", "AndroidView Factory: isFullscreen=$isFullscreen, asset=$assetId")
                                val view = LayoutInflater.from(context).inflate(R.layout.view_player_texture, null) as PlayerView
                                view.setControllerVisibilityListener(
                                    PlayerView.ControllerVisibilityListener { visibility ->
                                        onControllerVisibilityChanged?.invoke(visibility == View.VISIBLE)
                                    }
                                )
                                
                                // Clear content on player reset to prevent showing stale frames from previous videos
                                view.setKeepContentOnPlayerReset(false)
                                
                                playerViewRef.value = view
                                view
                            },
                            update = { view ->
                                val isCorrectMedia = (assetId == null || currentMediaId == assetId)
                                if (isCorrectMedia) {
                                    if (view.player != player) {
                                        view.player = player
                                    }
                                } else {
                                    if (view.player != null) {
                                        view.player = null
                                    }
                                }

                                view.useController = false
                                player.volume = if (isMuted) 0f else 1f
                                view.resizeMode = if (isFullscreen) {
                                    AspectRatioFrameLayout.RESIZE_MODE_FIT
                                } else {
                                    if (cardDisplayMode == CardDisplayMode.FILL) {
                                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                    } else {
                                        AspectRatioFrameLayout.RESIZE_MODE_FIT
                                    }
                                }

                                if (toggleControllerTrigger > 0 && isFullscreen) {
                                    if (view.isControllerFullyVisible) view.hideController() else view.showController()
                                }
                            },
                            onRelease = { view ->
                                AppLogger.d("VideoPlayer", "AndroidView Release: Detaching player (fullscreen=$isFullscreen), asset=$assetId")
                                view.player = null
                            },
                            modifier = Modifier.fillMaxSize().graphicsLayer { alpha = videoAlpha }
                        )
                    }
                }
            }
            }

            val indicatorsVisible = (showSize && fileSize != null) || (duration > 0) || isFullscreen
            // Controls visibility logic - respect showControls even in fullscreen to allow hiding on hold
            val finalShowControls = showControls
            
            if (indicatorsVisible) {
                VideoControls(
                    visible = finalShowControls,
                    isFullscreen = isFullscreen,
                    isLandscape = isLandscape,
                    controlsOffset = controlsOffset,
                    backdrop = playerBackdrop,
                    timeMs = if (isScrubbing) scrubValue else currentTime,
                    durationMs = duration,
                    showSize = showSize,
                    fileSize = fileSize,
                    onSeek = {
                        isScrubbing = true
                        scrubValue = it
                        player.seekTo(it)
                    },
                    onSeekFinished = {
                        isScrubbing = false
                        player.seekTo(scrubValue)
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            AnimatedVisibility(
                // Shown while the video is paused (e.g. by a tap); hidden during hold-to-pause along with the other controls.
                visible = !isPlayWhenReady && isCorrectMediaBound && finalShowControls,
                enter = fadeIn() + scaleIn(initialScale = 0.8f),
                exit = fadeOut() + scaleOut(targetScale = 1.2f),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Box(
                    modifier = Modifier
                        .requiredSize(64.dp)
                        .glassSurface(playerBackdrop, CircleShape, 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = stringResource(R.string.cd_paused),
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.requiredSize(32.dp)
                    )
                }
            }
        }
}

/**
 * Timestamp and size pills, plus the seekbar in fullscreen. Also drawn on the next fullscreen card before its video
 * loads, so the controls are already in place when it becomes the current one.
 */
@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VideoControls(
    visible: Boolean,
    isFullscreen: Boolean,
    isLandscape: Boolean,
    controlsOffset: Dp,
    backdrop: Backdrop,
    timeMs: Long,
    durationMs: Long,
    showSize: Boolean,
    fileSize: Long?,
    onSeek: (Long) -> Unit,
    onSeekFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .padding(bottom = (if (isFullscreen) (if (isLandscape) 12.dp else 24.dp) else 12.dp) + (if (isLandscape) 0.dp else controlsOffset))
                .padding(horizontal = if (isFullscreen) 24.dp else 16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // The card's pills are larger than the fullscreen ones; a pill's lens can't exceed its corner radius.
            val pillTextStyle = if (isFullscreen) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium
            val pillVerticalPadding = if (isFullscreen) 2.dp else 4.dp
            val pillLens = if (isFullscreen) 4.dp else 6.dp

            // 1. Indicators Row (Timestamp, Size) - Now ABOVE
            Row(
                // Landscape fullscreen: nudge down so the pills clear the favorite button above them.
                modifier = if (isFullscreen && isLandscape) Modifier.offset(y = 8.dp) else Modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (durationMs > 0 || isFullscreen) {
                    Box(
                        modifier = Modifier
                            .glassSurface(backdrop, CircleShape, pillLens)
                            .padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = "${formatMediaTime(timeMs)} / ${formatMediaTime(durationMs)}",
                            color = Color.White,
                            style = pillTextStyle,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = pillVerticalPadding)
                        )
                    }
                }

                if (showSize && fileSize != null) {
                    if (durationMs > 0 || isFullscreen) Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .glassSurface(backdrop, CircleShape, pillLens)
                            .padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = formatSize(fileSize),
                            color = Color.White,
                            style = pillTextStyle,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = pillVerticalPadding)
                        )
                    }
                }
            }

            // 2. Progress Bar Row (Slider) - Now BELOW
            if (isFullscreen) {
                val seekBarColors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                )
                Spacer(Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // SliderState works in 0..1 fractions of the duration.
                    val sliderDuration = durationMs.toFloat().coerceAtLeast(1f)
                    val sliderState = remember { SliderState() }
                    SideEffect {
                        sliderState.value = (timeMs.toFloat() / sliderDuration).coerceIn(0f, 1f)
                    }

                    Slider(
                        state = sliderState,
                        onValueChange = {
                            sliderState.value = it
                            onSeek((it * sliderDuration).toLong())
                        },
                        onValueChangeFinished = onSeekFinished,
                        colors = seekBarColors,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
