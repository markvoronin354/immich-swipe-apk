package com.markvoronin.immichswipe.feature.swipe.components


import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.GestureCancellationException
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Precision
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.core.AppLogger
import com.markvoronin.immichswipe.domain.model.Asset
import com.markvoronin.immichswipe.feature.common.MediaLoadingIndicator
import com.markvoronin.immichswipe.feature.swipe.SwipeDecision
import com.markvoronin.immichswipe.feature.swipe.utils.MaterialGreen
import com.markvoronin.immichswipe.feature.swipe.utils.MaterialRed
import com.markvoronin.immichswipe.feature.swipe.utils.findActivity
import com.markvoronin.immichswipe.feature.swipe.utils.formatSize
import com.markvoronin.immichswipe.feature.swipe.utils.parseDurationMs
import com.markvoronin.immichswipe.feature.swipe.utils.fullResolutionRequest
import com.markvoronin.immichswipe.feature.swipe.utils.rotateLayout
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

/** Keeps the KEEP/DELETE badge below the close button, which spans 50dp to 98dp from the top. */
private val FullscreenBadgeTopPadding = 110.dp

@OptIn(UnstableApi::class)
@Composable
fun FullscreenViewer(
    asset: Asset,
    nextAsset: Asset? = null,
    isFavorite: Boolean,
    isNextFavorite: Boolean = false,
    onSwipe: (SwipeDecision) -> Unit,
    onUndo: () -> Unit,
    onToggleFavorite: () -> Unit = {},
    onToggleFavoriteNext: () -> Unit = {},
    onClose: () -> Unit,
    tapToSwipeEnabled: Boolean = false,
    providedPlayer: ExoPlayer? = null,
    showSizeIndicator: Boolean = false,
    isMuted: Boolean = false,
    onToggleMute: () -> Unit = {},
    showMuteButton: Boolean = true,
    rotation: Int = 0,
    baseUrl: String = "",
    isFullSize: Boolean = false,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val scope = rememberCoroutineScope()
    val motionScheme = MaterialTheme.motionScheme

    var isHoldingByPress by remember(asset.id) { mutableStateOf(false) }
    var wasHoldDetected by remember(asset.id) { mutableStateOf(false) }
    val currentIsHolding by rememberUpdatedState(isHoldingByPress || wasHoldDetected)
    var pausedByHoldState by remember(asset.id) { mutableStateOf(false) }
    var ignoreNextTap by remember { mutableStateOf(false) }
    var isZoomedIn by remember(asset.id) { mutableStateOf(false) }
    var isFullSizeLoaded by remember(asset.id, isFullSize) { mutableStateOf(false) }

    val swipeY = remember { Animatable(0f) }
    val swipeX = remember { Animatable(0f) }

    val currentOnSwipe by rememberUpdatedState(onSwipe)

    val performSwipe: (SwipeDecision) -> Unit = { decision ->
        scope.launch {
            if (currentIsHolding) return@launch
            val targetX = if (decision == SwipeDecision.KEEP) 2000f else -2000f
            launch { swipeY.animateTo(0f, tween(180)) }
            swipeX.animateTo(targetX, tween(180))
            currentOnSwipe(decision)
            swipeX.snapTo(0f)
            swipeY.snapTo(0f)
        }
    }

    var toggleControllerTrigger by remember { mutableIntStateOf(0) }

    var isVideoReady by remember(asset.id) { mutableStateOf(false) }
    var showLoadingIndicator by remember(asset.id, providedPlayer) {
        val isSameAsset = providedPlayer?.currentMediaItem?.mediaId == asset.id
        mutableStateOf(asset.type == "VIDEO" && !(isSameAsset && providedPlayer.playbackState == Player.STATE_READY))
    }
    LaunchedEffect(asset.id, isVideoReady) {
        AppLogger.d("Fullscreen", "Video readiness: ready=$isVideoReady, indicator=$showLoadingIndicator, asset=${asset.id}")
        if (isVideoReady) {
            showLoadingIndicator = false
        } else if (asset.type == "VIDEO") {
            // Delay showing the indicator to avoid flickering on fast transitions
            delay(500.milliseconds)
            if (!isVideoReady) {
                showLoadingIndicator = true
            }
        }
    }

    var controlsVisible by remember { mutableStateOf(true) }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    val controlsOffset by animateDpAsState(
        targetValue = if (controlsVisible && isLandscape) 64.dp else 0.dp,
        animationSpec = motionScheme.fastSpatialSpec(),
        label = "ControlsShift"
    )

    val baseUrlClean = baseUrl.removeSuffix("/")

    val exoPlayer = providedPlayer // Use shared player exclusively for Fullscreen to avoid heavy instantiations

    LaunchedEffect(asset.id) {
        swipeX.snapTo(0f)
        swipeY.snapTo(0f)
        val isSameAssetAndReady = (exoPlayer?.currentMediaItem?.mediaId == asset.id) && (exoPlayer.playbackState == Player.STATE_READY)
        if (!isSameAssetAndReady) {
            isVideoReady = false
            showLoadingIndicator = true
        }
    }

    LaunchedEffect(exoPlayer, isMuted) {
        exoPlayer?.volume = if (isMuted) 0f else 1f
    }

    var wasPlayingBeforeHold by remember(asset.id) { mutableStateOf(false) }

    LaunchedEffect(pausedByHoldState) {
        if (pausedByHoldState) {
            wasPlayingBeforeHold = exoPlayer?.playWhenReady == true
            exoPlayer?.pause()
        } else if (wasPlayingBeforeHold) {
            exoPlayer?.play()
        }
    }

    // Manage orientation for the lifetime of the FullscreenViewer
    DisposableEffect(Unit) {
        @SuppressLint("SourceLockedOrientationActivity")
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
        onDispose {
            @SuppressLint("SourceLockedOrientationActivity")
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    DisposableEffect(exoPlayer, asset.id) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                val isSameAsset = exoPlayer?.currentMediaItem?.mediaId == asset.id
                if (isSameAsset) {
                    isVideoReady = true
                    showLoadingIndicator = false
                }
            }
            override fun onPlaybackStateChanged(state: Int) {
                val isSameAsset = exoPlayer?.currentMediaItem?.mediaId == asset.id
                AppLogger.d("Fullscreen", "Playback state changed: $state, asset=${asset.id}, same=$isSameAsset")
                if (state == Player.STATE_READY && isSameAsset) {
                    if (exoPlayer.videoSize.height == 0) {
                        isVideoReady = true
                    }
                    showLoadingIndicator = false
                } else if (state == Player.STATE_BUFFERING) {
                    if (isSameAsset) showLoadingIndicator = true
                } else if (state == Player.STATE_ENDED || state == Player.STATE_IDLE) {
                    showLoadingIndicator = false
                }
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                val isSameAsset = exoPlayer?.currentMediaItem?.mediaId == asset.id
                AppLogger.d("Fullscreen", "Is playing changed: $isPlaying, asset=${asset.id}, same=$isSameAsset")
                if (isPlaying && isSameAsset) {
                    showLoadingIndicator = false
                }
            }
            override fun onPlayerError(error: PlaybackException) {
                AppLogger.e("Fullscreen", "Player error: ${error.message}", error)
                showLoadingIndicator = false
            }
        }

        exoPlayer?.addListener(listener)

        onDispose {
            exoPlayer?.removeListener(listener)
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        val dialogView = LocalView.current

        SideEffect {
            val window = (dialogView.parent as? DialogWindowProvider)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, dialogView)
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }

        val fadeAlpha = (1f - (swipeY.value / 1000f)).coerceIn(0f, 1f)
        // Live copies of the media for the glass buttons to refract; black is drawn first so empty pixels don't smear.
        val mediaBackdrop = rememberLayerBackdrop {
            drawRect(Color.Black)
            drawContent()
        }
        val nextMediaBackdrop = rememberLayerBackdrop {
            drawRect(Color.Black)
            drawContent()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = fadeAlpha))
        ) {
            if (nextAsset != null) {
                val swipeProgress = (abs(swipeX.value) / 500f).coerceIn(0f, 1f)
                val nextScale = 0.85f + (0.15f * swipeProgress)
                val nextAlpha = (0.6f + (0.4f * swipeProgress)) * fadeAlpha

                val nextPhotoRequest = remember(nextAsset.id, baseUrlClean, nextAsset.isGif) {
                    ImageRequest.Builder(context)
                        .data(
                            if (nextAsset.isGif) "$baseUrlClean/api/assets/${nextAsset.id}/original"
                            else "$baseUrlClean/api/assets/${nextAsset.id}/thumbnail?format=WEBP&size=preview&edited=true"
                        )
                        .memoryCacheKey("${nextAsset.id}-preview")
                        .placeholderMemoryCacheKey("${nextAsset.id}-preview")
                        .crossfade(false)
                        .precision(Precision.INEXACT)
                        .build()
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = nextScale
                            scaleY = nextScale
                            alpha = nextAlpha
                        },
                    contentAlignment = Alignment.Center
                ) {
                    SubcomposeAsyncImage(
                        model = nextPhotoRequest,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().layerBackdrop(nextMediaBackdrop),
                        error = {
                            AssetErrorView()
                        }
                    )

                    if (showSizeIndicator && nextAsset.type != "VIDEO") {
                        AnimatedVisibility(
                            visible = controlsVisible && !isHoldingByPress && !wasHoldDetected,
                            enter = fadeIn(MaterialTheme.motionScheme.slowEffectsSpec()),
                            exit = fadeOut(MaterialTheme.motionScheme.slowEffectsSpec()),
                            modifier = Modifier.align(Alignment.BottomCenter)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .padding(bottom = previewToolbarBottom(isLandscape) + PreviewToolbarHeight + PreviewToolbarGap),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = formatSize(nextAsset.exifInfo?.fileSizeInBytes ?: 0L),
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    if (nextAsset.type == "VIDEO") {
                        // Same controls the video gets once it is current, so they don't pop in after the swipe.
                        VideoControls(
                            visible = controlsVisible && !isHoldingByPress && !wasHoldDetected,
                            isFullscreen = true,
                            isLandscape = isLandscape,
                            controlsOffset = controlsOffset,
                            backdrop = nextMediaBackdrop,
                            timeMs = 0L,
                            durationMs = parseDurationMs(nextAsset.duration),
                            showSize = showSizeIndicator,
                            fileSize = nextAsset.exifInfo?.fileSizeInBytes,
                            onSeek = {},
                            onSeekFinished = {},
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }

                    PreviewControls(
                        backdrop = nextMediaBackdrop,
                        visible = controlsVisible && !isHoldingByPress && !wasHoldDetected,
                        isFavorite = isNextFavorite,
                        onToggleFavorite = onToggleFavoriteNext,
                        onClose = onClose,
                        onUndo = onUndo,
                        isLandscape = isLandscape,
                        showMuteButton = showMuteButton,
                        isMuted = isMuted,
                        onToggleMute = onToggleMute,
                        assetType = nextAsset.type
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isZoomedIn) {
                        if (isZoomedIn) return@pointerInput
                        detectDragGestures(
                            onDragEnd = {
                                scope.launch {
                                    if (currentIsHolding) {
                                        launch { swipeY.animateTo(0f) }
                                        launch { swipeX.animateTo(0f, motionScheme.fastSpatialSpec()) }
                                        wasHoldDetected = false
                                        pausedByHoldState = false
                                        return@launch
                                    }
                                    val currentX = swipeX.value
                                    val currentY = swipeY.value

                                    if (currentY > 120 && abs(currentX) < 100) {
                                        onClose()
                                    } else if (currentX > 150) {
                                        performSwipe(SwipeDecision.KEEP)
                                    } else if (currentX < -150) {
                                        performSwipe(SwipeDecision.DELETE)
                                    } else {
                                        launch { swipeY.animateTo(0f) }
                                        launch { swipeX.animateTo(0f, motionScheme.fastSpatialSpec()) }
                                    }
                                }
                            },
                            onDragCancel = {
                                scope.launch {
                                    launch { swipeY.animateTo(0f) }
                                    launch { swipeX.animateTo(0f, motionScheme.fastSpatialSpec()) }
                                    wasHoldDetected = false
                                    pausedByHoldState = false
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                if (currentIsHolding) {
                                    return@detectDragGestures
                                }
                                scope.launch {
                                    swipeY.snapTo((swipeY.value + dragAmount.y).coerceAtLeast(0f))
                                    swipeX.snapTo(swipeX.value + dragAmount.x)
                                }
                            }
                        )
                    }
                    .offset { IntOffset(swipeX.value.roundToInt(), swipeY.value.roundToInt()) }
                    .graphicsLayer {
                        rotationZ = swipeX.value / 60f
                    }
                    .background(Color.Black.copy(alpha = fadeAlpha)),
                contentAlignment = Alignment.Center
            ) {
            val finalControlsVisible = controlsVisible && !isHoldingByPress && !wasHoldDetected

            Box(modifier = Modifier.fillMaxSize().layerBackdrop(mediaBackdrop), contentAlignment = Alignment.Center) {
            if (asset.type == "VIDEO" && exoPlayer != null) {
                SharedVideoPlayer(
                    player = exoPlayer,
                    isFullscreen = true,
                    assetId = asset.id,
                    baseUrl = baseUrlClean,
                    isMuted = isMuted,
                    isPaused = pausedByHoldState,
                    isVideoReady = isVideoReady,
                    toggleControllerTrigger = toggleControllerTrigger,
                    showControls = finalControlsVisible,
                    fileSize = asset.exifInfo?.fileSizeInBytes,
                    showSize = showSizeIndicator,
                    onControllerVisibilityChanged = { /* Now handled externally via controlsVisible */ },
                    controlsOffset = controlsOffset,
                    videoSurfaceWrapper = { surface ->
                        Box(modifier = Modifier.fillMaxSize()) {
                            ZoomableBox(
                                modifier = Modifier.fillMaxSize(),
                                resetOnRelease = false,
                                onIsZoomedChanged = { isZoomedIn = it },
                                onTap = { offset, size ->
                                    if (!ignoreNextTap) {
                                        val width = size.width.toFloat()
                                        if (tapToSwipeEnabled && !isZoomedIn && (offset.x < width * 0.3f || offset.x > width * 0.7f)) {
                                            when {
                                                offset.x < width * 0.3f -> performSwipe(
                                                    SwipeDecision.DELETE)
                                                offset.x > width * 0.7f -> performSwipe(
                                                    SwipeDecision.KEEP)
                                            }
                                        } else if (asset.type == "VIDEO") {
                                            exoPlayer.togglePlayPause()
                                            toggleControllerTrigger++
                                        }
                                    }
                                    ignoreNextTap = false
                                },
                                onPress = { offset, size ->
                                    ignoreNextTap = false
                                    wasHoldDetected = false
                                    val wasReleased = withTimeoutOrNull(500.milliseconds) {
                                        awaitRelease()
                                        true
                                    }
                                    if (wasReleased == true) {
                                        // Fast tap detected
                                        val width = size.width.toFloat()
                                        if (tapToSwipeEnabled && !isZoomedIn && (offset.x < width * 0.3f || offset.x > width * 0.7f)) {
                                            when {
                                                offset.x < width * 0.3f -> {
                                                    performSwipe(SwipeDecision.DELETE)
                                                    ignoreNextTap = true
                                                }

                                                offset.x > width * 0.7f -> {
                                                    performSwipe(SwipeDecision.KEEP)
                                                    ignoreNextTap = true
                                                }
                                            }
                                        }
                                    } else {
                                        // Hold detected
                                        ignoreNextTap = true
                                        isHoldingByPress = true
                                        wasHoldDetected = true
                                        if (asset.type == "VIDEO") {
                                            pausedByHoldState = true
                                        }
                                        try {
                                            awaitRelease()
                                            isHoldingByPress = false
                                            pausedByHoldState = false
                                            wasHoldDetected = false
                                        } catch (_: GestureCancellationException) {
                                            isHoldingByPress = false
                                        }
                                    }
                                },
                                aspectRatio = asset.exifInfo?.let {
                                    val baseAR =
                                        it.displayAspectRatio
                                    if (rotation % 180 != 0) 1f / baseAR else baseAR
                                }
                            ) {
                                val animatedRotation by animateFloatAsState(
                                    targetValue = rotation.toFloat(),
                                    animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(), label = "FullscreenVideoRotation"
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer { rotationZ = animatedRotation }
                                        .rotateLayout(rotation)
                                ) {
                                    surface()
                                }
                            }

                            if (showLoadingIndicator) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    MediaLoadingIndicator()
                                }
                            }
                        }
                    }
                )
            } else {
                ZoomableBox(
                    modifier = Modifier.fillMaxSize(),
                    resetOnRelease = false,
                    onIsZoomedChanged = { isZoomedIn = it },
                    onTap = { offset, size ->
                        if (!ignoreNextTap) {
                            val width = size.width.toFloat()
                            if (tapToSwipeEnabled && !isZoomedIn && (offset.x < width * 0.3f || offset.x > width * 0.7f)) {
                                when {
                                    offset.x < width * 0.3f -> performSwipe(SwipeDecision.DELETE)
                                    offset.x > width * 0.7f -> performSwipe(SwipeDecision.KEEP)
                                }
                            }
                        }
                        ignoreNextTap = false
                    },
                    onPress = { offset, size ->
                        ignoreNextTap = false
                        wasHoldDetected = false
                        val wasReleased = withTimeoutOrNull(500.milliseconds) {
                            awaitRelease()
                            true
                        }
                        if (wasReleased == true) {
                            val width = size.width.toFloat()
                            if (tapToSwipeEnabled && !isZoomedIn && (offset.x < width * 0.3f || offset.x > width * 0.7f)) {
                                when {
                                    offset.x < width * 0.3f -> {
                                        performSwipe(SwipeDecision.DELETE)
                                        ignoreNextTap = true
                                    }

                                    offset.x > width * 0.7f -> {
                                        performSwipe(SwipeDecision.KEEP)
                                        ignoreNextTap = true
                                    }
                                }
                            }
                        } else {
                            ignoreNextTap = true
                            isHoldingByPress = true
                            wasHoldDetected = true
                            try {
                                awaitRelease()
                                isHoldingByPress = false
                                wasHoldDetected = false
                            } catch (_: GestureCancellationException) {
                                isHoldingByPress = false
                            }
                        }
                    },
                    aspectRatio = asset.exifInfo?.let {
                        val baseAR =
                            it.displayAspectRatio
                        if (rotation % 180 != 0) 1f / baseAR else baseAR
                    }
                ) {
                    val animatedRotation by animateFloatAsState(
                        targetValue = rotation.toFloat(),
                        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(), label = "FullscreenRotation"
                    )

                    val photoRequest = remember(asset.id, baseUrlClean, asset.isGif, isFullSize, asset.exifInfo?.orientation) {
                        if (isFullSize) {
                            fullResolutionRequest(context, baseUrlClean, asset)
                        } else {
                            ImageRequest.Builder(context)
                                .data(
                                    if (asset.isGif) "$baseUrlClean/api/assets/${asset.id}/original"
                                    else "$baseUrlClean/api/assets/${asset.id}/thumbnail?format=WEBP&size=preview&edited=true"
                                )
                                .memoryCacheKey("${asset.id}-preview")
                                .placeholderMemoryCacheKey("${asset.id}-preview")
                                .crossfade(false)
                                .precision(Precision.INEXACT)
                                .build()
                        }
                    }

                    SubcomposeAsyncImage(
                        model = photoRequest,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { rotationZ = animatedRotation }
                            .rotateLayout(rotation),
                        contentScale = ContentScale.Fit,
                        onSuccess = { isFullSizeLoaded = true },
                        error = {
                            AssetErrorView()
                        }
                    )
                }
            }
            }

            if (isFullSize && !isFullSizeLoaded && asset.type != "VIDEO") {
                // The preview stays visible underneath, so keep this small and see-through.
                MediaLoadingIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    size = 32.dp,
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                )
            }

            if (showSizeIndicator && asset.type != "VIDEO") {
                AnimatedVisibility(
                    visible = controlsVisible && !isHoldingByPress && !wasHoldDetected,
                    enter = fadeIn(MaterialTheme.motionScheme.slowEffectsSpec()),
                    exit = fadeOut(MaterialTheme.motionScheme.slowEffectsSpec()),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Surface(
                        modifier = Modifier
                            .padding(bottom = previewToolbarBottom(isLandscape) + PreviewToolbarHeight + PreviewToolbarGap),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = formatSize(asset.exifInfo?.fileSizeInBytes ?: 0L),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (swipeX.value > 0f) IndicatorBadge(
                stringResource(R.string.swipe_keep_upper),
                Icons.Default.Check,
                MaterialGreen,
                Alignment.TopStart,
                topPadding = FullscreenBadgeTopPadding
            ) { (swipeX.value / 200f).coerceIn(0f, 1f) * 0.9f }
            else if (swipeX.value < 0f) IndicatorBadge(
                stringResource(R.string.swipe_delete_upper),
                Icons.Default.Delete,
                MaterialRed,
                Alignment.TopEnd,
                topPadding = FullscreenBadgeTopPadding
            ) { (-swipeX.value / 200f).coerceIn(0f, 1f) * 0.9f }

                PreviewControls(
                    backdrop = mediaBackdrop,
                    visible = controlsVisible && !isHoldingByPress && !wasHoldDetected,
                    isFavorite = isFavorite,
                    onToggleFavorite = onToggleFavorite,
                    onClose = onClose,
                    onUndo = onUndo,
                    isLandscape = isLandscape,
                    showMuteButton = showMuteButton,
                    isMuted = isMuted,
                    onToggleMute = onToggleMute,
                    assetType = asset.type
                )

        }
    }
}
}
