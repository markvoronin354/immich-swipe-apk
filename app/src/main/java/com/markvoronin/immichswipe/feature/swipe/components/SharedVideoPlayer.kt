package com.markvoronin.immichswipe.feature.swipe.components

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.outlined.VideocamOff
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
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
                            contentScale = ContentScale.Fit,
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
                                view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT

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
                    isPlaying = isPlayWhenReady && isCorrectMediaBound,
                    onTogglePlay = { player.togglePlayPause() },
                    onSeek = {
                        isScrubbing = true
                        scrubValue = it
                        player.seekTo(it)
                    },
                    onSeekFinished = {
                        // The polled time is from before the drag until the next poll; start from the drop point.
                        currentTime = scrubValue
                        isScrubbing = false
                        player.seekTo(scrubValue)
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            AnimatedVisibility(
                // Shown while the video is paused (e.g. by a tap); hidden during hold-to-pause along with the other controls.
                // Fullscreen shows the paused state on its play/pause button instead.
                visible = !isFullscreen && !isPlayWhenReady && isCorrectMediaBound && finalShowControls,
                enter = fadeIn(MaterialTheme.motionScheme.slowEffectsSpec()) + scaleIn(MaterialTheme.motionScheme.defaultSpatialSpec(), initialScale = 0.8f),
                exit = fadeOut(MaterialTheme.motionScheme.slowEffectsSpec()) + scaleOut(MaterialTheme.motionScheme.defaultSpatialSpec(), targetScale = 1.2f),
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

// Material's play_arrow split down the middle into two quads, each of which morphs into one of pause's bars.
// Points run top-start, top-end, bottom-end, bottom-start in a 24-unit box.
private val PlayStart = floatArrayOf(8f, 5f, 13.5f, 8.5f, 13.5f, 15.5f, 8f, 19f)
private val PlayEnd = floatArrayOf(13.5f, 8.5f, 19f, 12f, 19f, 12f, 13.5f, 15.5f)
private val PauseStart = floatArrayOf(6f, 5f, 10f, 5f, 10f, 19f, 6f, 19f)
private val PauseEnd = floatArrayOf(14f, 5f, 18f, 5f, 18f, 19f, 14f, 19f)

/** Play icon at [progress] 0, pause icon at 1, morphing and turning half a turn in between (pause looks the same upside down). */
@Composable
internal fun PlayPauseIcon(progress: () -> Float, contentDescription: String, color: Color) {
    val path = remember { Path() }
    Canvas(modifier = Modifier.size(24.dp).semantics { this.contentDescription = contentDescription }) {
        val t = progress()
        val unit = size.width / 24f
        path.reset()
        for ((from, to) in listOf(PlayStart to PauseStart, PlayEnd to PauseEnd)) {
            for (i in 0 until 4) {
                val x = lerp(from[i * 2], to[i * 2], t) * unit
                val y = lerp(from[i * 2 + 1], to[i * 2 + 1], t) * unit
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
        }
        rotate(180f * t) { drawPath(path, color) }
    }
}

/**
 * Timestamp and size pills, plus the play/pause button and seekbar in fullscreen. Also drawn on the next fullscreen card
 * before its video loads, so the controls are already in place when it becomes the current one.
 */
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
    isPlaying: Boolean = false,
    onTogglePlay: () -> Unit = {},
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(MaterialTheme.motionScheme.slowEffectsSpec()),
        exit = fadeOut(MaterialTheme.motionScheme.slowEffectsSpec()),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                // Fullscreen: sits above PreviewControls' floating toolbar (plus a 4dp gap).
                .padding(bottom = (if (isFullscreen) (if (isLandscape) 12.dp else 24.dp) + PreviewToolbarHeight + PreviewToolbarGap else 12.dp) + (if (isLandscape) 0.dp else controlsOffset))
                .padding(horizontal = if (isFullscreen) 12.dp else 16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // The card's pills are larger than the fullscreen ones; a pill's lens can't exceed its corner radius.
            val pillTextStyle = if (isFullscreen) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium
            val pillVerticalPadding = if (isFullscreen) 2.dp else 4.dp
            val pillLens = if (isFullscreen) 4.dp else 6.dp
            // Fullscreen uses a plain Material 3 tonal pill; the card keeps its glass look.
            val pillModifier = if (isFullscreen) {
                Modifier.background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
            } else {
                Modifier.glassSurface(backdrop, CircleShape, pillLens)
            }
            val pillTextColor = if (isFullscreen) MaterialTheme.colorScheme.onSecondaryContainer else Color.White

            // 1. Indicators Row (Timestamp, Size) - Now ABOVE. In fullscreen they sit beside the play/pause button instead.
            if (!isFullscreen) Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (durationMs > 0) {
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
                    if (durationMs > 0) Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = pillModifier
                            .padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = formatSize(fileSize),
                            color = pillTextColor,
                            style = pillTextStyle,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = pillVerticalPadding)
                        )
                    }
                }
            }

            // 2. Fullscreen: play/pause and the times on one row, the seekbar on its own row below.
            if (isFullscreen) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val playProgress = animateFloatAsState(
                        targetValue = if (isPlaying) 1f else 0f,
                        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
                        label = "PlayPauseMorph"
                    )
                    IconButton(onClick = onTogglePlay, shapes = IconButtonDefaults.shapes()) {
                        PlayPauseIcon(
                            progress = { playProgress.value },
                            contentDescription = stringResource(if (isPlaying) R.string.cd_pause else R.string.cd_play),
                            color = Color.White
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    if (showSize && fileSize != null) {
                        Box(modifier = pillModifier.padding(horizontal = 4.dp)) {
                            Text(
                                text = formatSize(fileSize),
                                color = pillTextColor,
                                style = pillTextStyle,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = pillVerticalPadding)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                    }
                    Text(
                        text = "${formatMediaTime(timeMs)} / ${formatMediaTime(durationMs)}",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }

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
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.4f),
                        activeTickColor = Color.White,
                        inactiveTickColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
