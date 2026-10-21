package com.markvoronin.immichswipe.feature.swipe

import android.content.ContentValues
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.core.AppLogger
import com.markvoronin.immichswipe.core.ConnectionLevel
import com.markvoronin.immichswipe.core.PlaybackBehavior
import com.markvoronin.immichswipe.core.cache.VideoCache
import com.markvoronin.immichswipe.core.cache.VideoPreloader
import com.markvoronin.immichswipe.core.net.MediaHttp
import com.markvoronin.immichswipe.core.player.PlayerLoadControlFactory
import com.markvoronin.immichswipe.core.util.rethrowIfCancelled
import com.markvoronin.immichswipe.domain.model.Album
import com.markvoronin.immichswipe.feature.swipe.components.SwipeScreenContent
import com.markvoronin.immichswipe.feature.swipe.components.SwipeScreenDialogs
import com.markvoronin.immichswipe.feature.tutorial.GestureAnimationType
import com.markvoronin.immichswipe.feature.tutorial.LocalTutorialController
import com.markvoronin.immichswipe.feature.tutorial.TooltipPlacement
import com.markvoronin.immichswipe.feature.tutorial.TutorialPhase
import com.markvoronin.immichswipe.feature.tutorial.TutorialStep
import com.markvoronin.immichswipe.feature.tutorial.rememberTutorialController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds

@OptIn(UnstableApi::class)
@Composable
fun SwipeScreen(
    album: Album,
    sessionKey: String,
    resetSignal: Flow<Unit>,
    modifier: Modifier = Modifier,
    userQuotaBytes: Long? = null,
    onBack: () -> Unit = {}
) {
    val viewModel: SwipeViewModel = hiltViewModel(key = "$sessionKey-${album.id}")

    LaunchedEffect(album, userQuotaBytes) {
        viewModel.initAlbum(album, userQuotaBytes)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tutorialController = LocalTutorialController.current ?: rememberTutorialController()

    val swipeTutorialSteps = remember {
        listOf(
            TutorialStep(
                id = "gestures",
                titleRes = R.string.tutorial_swipe_gestures_title,
                descriptionRes = R.string.tutorial_swipe_gestures_desc,
                targetKey = "swipe_deck",
                placement = TooltipPlacement.TOP,
                gestureAnimation = GestureAnimationType.NONE,
                targetPaddingDp = 0,
                targetCornerRadiusDp = 16
            ),
            TutorialStep(
                id = "header",
                titleRes = R.string.tutorial_swipe_header_title,
                descriptionRes = R.string.tutorial_swipe_header_desc,
                targetKey = "swipe_header"
            ),
            TutorialStep(
                id = "actions",
                titleRes = R.string.tutorial_swipe_actions_title,
                descriptionRes = R.string.tutorial_swipe_actions_desc,
                targetKey = "swipe_actions"
            )
        )
    }

    LaunchedEffect(uiState.hasCompletedSwipeTutorial, uiState.assets.isNotEmpty()) {
        if (!uiState.hasCompletedSwipeTutorial && uiState.assets.isNotEmpty()) {
            tutorialController.startTutorial(
                phase = TutorialPhase.SWIPE,
                steps = swipeTutorialSteps,
                onComplete = { viewModel.completeSwipeTutorial() }
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (tutorialController.phase == TutorialPhase.SWIPE && tutorialController.isVisible) {
                tutorialController.dismissTutorial()
            }
        }
    }

    BackHandler(enabled = true) {
        val state = viewModel.uiState.value
        when {
            state.showSummary -> viewModel.toggleSummary(false)
            state.isFullscreenMode -> viewModel.toggleFullscreen(false)
            state.showResetConfirmation -> viewModel.toggleResetConfirmation(false)
            state.isBulkDeleteMode || state.isBulkKeepMode -> viewModel.exitBulkMode()
            else -> onBack()
        }
    }
    
    LaunchedEffect(resetSignal) {
        resetSignal.collect {
            viewModel.toggleResetConfirmation(visible = true)
        }
    }
    val localCtx = LocalContext.current
    
    // Background Preloader Trigger - Deferred to prevent background IO / decoding from stealing CPU frames during animations
    LaunchedEffect(uiState.currentIndex, uiState.baseUrl) {
        val currentIndex = uiState.currentIndex
        val assets = uiState.assets
        val baseUrl = uiState.baseUrl.removeSuffix("/")
        
        if (baseUrl.isEmpty()) return@LaunchedEffect

        // Wait 350ms for active gestures, card swipes, or top bar transitions to finish smoothly before preloading
        delay(350.milliseconds)

        // Cancel preload for the previous asset if it was a video
        if (currentIndex > 0) {
            val prevAsset = assets.getOrNull(currentIndex - 1)
            if (prevAsset?.type == "VIDEO") {
                VideoPreloader.cancel(prevAsset.id)
            }
        }

        // Preload next 3 assets
        for (i in 1..3) {
            val nextIndex = currentIndex + i
            if (nextIndex < assets.size) {
                val nextAsset = assets[nextIndex]
                if (nextAsset.type == "VIDEO") {
                    val videoUrl = "$baseUrl/api/assets/${nextAsset.id}/video/playback"
                    VideoPreloader.preload(
                        context = localCtx.applicationContext,
                        assetId = nextAsset.id,
                        videoUrl = videoUrl,
                    )
                } else {
                    // Preload Image to Coil RAM Cache
                    val imageUrl = if (nextAsset.isGif) "$baseUrl/api/assets/${nextAsset.id}/original"
                    else "$baseUrl/api/assets/${nextAsset.id}/thumbnail?format=WEBP&size=preview&edited=true"
                    val request = ImageRequest.Builder(localCtx)
                        .data(imageUrl)
                        .memoryCacheKey("${nextAsset.id}-preview")
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .build()
                    localCtx.imageLoader.enqueue(request)
                }
            }
        }
    }

     val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val playbackBehavior = uiState.playbackBehavior
    val currentAsset = uiState.currentAsset
    
    val baseUrl = uiState.baseUrl.removeSuffix("/")

     val sharedPlayer: ExoPlayer = remember {
        val loadControl = PlayerLoadControlFactory.createSwipeLoadControl(context)
        
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()
        val handleAudioFocus = (playbackBehavior != PlaybackBehavior.IGNORE)

        ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, handleAudioFocus)
            .build().apply {
                repeatMode = Player.REPEAT_MODE_ONE
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        AppLogger.d("ExoPlayer", "State changed: $state (ready=${state == Player.STATE_READY})")
                    }
                    override fun onPlayerError(error: PlaybackException) {
                        AppLogger.e("ExoPlayer", "Error: ${error.message}", error)
                    }
                    override fun onVideoSizeChanged(videoSize: VideoSize) {
                        AppLogger.d("ExoPlayer", "Video size: ${videoSize.width}x${videoSize.height}")
                    }
                })
            }
    }

     LaunchedEffect(currentAsset?.id) {
        if (currentAsset?.type == "VIDEO") {
            // Do NOT stop/clear the player, as it forces the underlying surface to go blank
            // which causes a flicker when recycling the TextureView.
            // Just update the media source.
            val videoUrl = "$baseUrl/api/assets/${currentAsset.id}/video/playback"

            val dataSourceFactory = VideoCache.getCacheDataSourceFactory(context)
            val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.Builder()
                    .setUri(videoUrl)
                    .setMediaId(currentAsset.id)
                    .setCustomCacheKey(currentAsset.id) // Ensure consistent cache mapping
                    .build())
            
            // Using setMediaSource with resetPosition=true is smoother than stop()+clear()
            sharedPlayer.setMediaSource(mediaSource, true)
            sharedPlayer.prepare()
            sharedPlayer.playWhenReady = true
        } else {
            sharedPlayer.stop()
            sharedPlayer.clearMediaItems()
        }
    }

     LaunchedEffect(playbackBehavior) {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()
        val handleAudioFocus = (playbackBehavior != PlaybackBehavior.IGNORE)
        sharedPlayer.setAudioAttributes(audioAttributes, handleAudioFocus)
    }

     LaunchedEffect(uiState.isMuted) {
        sharedPlayer.volume = if (uiState.isMuted) 0f else 1f
    }

    DisposableEffect(Unit) {
        onDispose { sharedPlayer.release() }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val currentType by rememberUpdatedState(currentAsset?.type)
    DisposableEffect(lifecycleOwner) {
        var wasPlayingBeforeAppPause = false
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                wasPlayingBeforeAppPause = sharedPlayer.playWhenReady
                sharedPlayer.playWhenReady = false
            } else if (event == Lifecycle.Event.ON_RESUME) {
                if (currentType == "VIDEO" && wasPlayingBeforeAppPause) {
                    sharedPlayer.playWhenReady = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { _ ->
        viewModel.onLocalDeleteIntentHandled()
    }

    LaunchedEffect(uiState.localDeletePendingIntent) {
        uiState.localDeletePendingIntent?.let { pendingIntent ->
            try {
                deleteLauncher.launch(
                    IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                )
            } catch (e: Exception) {
                e.rethrowIfCancelled()
                viewModel.onLocalDeleteIntentHandled()
            }
        }
    }

    var showSortMenu by remember { mutableStateOf(value = false) }
    val connectionStatus = uiState.connectionStatus

    LaunchedEffect(connectionStatus.level) {
        if ((uiState.error != null) && (connectionStatus.level == ConnectionLevel.ONLINE)) {
            viewModel.retryLoading()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.syncIssueSignal.collect {
            Toast.makeText(context, R.string.swipe_sync_partial_failure, Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.downloadRequestSignal.collect { asset ->
            val baseUrl = uiState.baseUrl.removeSuffix("/")
            if (baseUrl.isEmpty()) return@collect

            val rawName = asset.originalFileName?.substringAfterLast('/')?.substringAfterLast('\\')
                ?: "immich_${asset.id}.${asset.fileExtension ?: "jpg"}"
            val fileName = if (rawName.contains(".")) rawName else "$rawName.${asset.fileExtension ?: "jpg"}"

            Toast.makeText(context, resources.getString(R.string.swipe_download_started, fileName), Toast.LENGTH_SHORT).show()

            scope.launch(Dispatchers.IO) {
                // The API key is sent as a header on every request, never in the URL, so it cannot
                // end up in server, proxy or browser logs.
                val urlsToTry = listOf(
                    "$baseUrl/api/assets/${asset.id}/original",
                    "$baseUrl/api/asset/file/${asset.id}"
                )

                // Shared client: it adds the API key itself, and only for requests to the active server.
                val client = MediaHttp.client(context)

                var response: Response? = null
                for (downloadUrl in urlsToTry) {
                    try {
                        val req = Request.Builder()
                            .url(downloadUrl)
                            .build()
                        val resp = client.newCall(req).execute()
                        if (resp.isSuccessful) {
                            response = resp
                            break
                        } else {
                            resp.close()
                        }
                    } catch (e: Exception) {
                        e.rethrowIfCancelled()
                        AppLogger.w("Download", "Failed download endpoint $downloadUrl: ${e.message}")
                    }
                }

                if (response == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, resources.getString(R.string.swipe_download_server_error), Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                val body = response.body
                val mimeType = if (asset.type == "VIDEO") "video/*" else if (asset.isGif) "image/gif" else "image/*"

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val contentValues = ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                        }
                        val resolver = context.contentResolver
                        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                            ?: throw IOException("Failed to create MediaStore entry")

                        resolver.openOutputStream(uri)?.use { output ->
                            body.byteStream().use { input ->
                                input.copyTo(output)
                            }
                        } ?: throw IOException("Failed to open output stream")
                    } else {
                        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        downloadsDir.mkdirs()
                        val targetFile = File(downloadsDir, fileName)
                        targetFile.outputStream().use { output ->
                            body.byteStream().use { input ->
                                input.copyTo(output)
                            }
                        }
                    }

                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, resources.getString(R.string.swipe_download_saved, fileName), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.rethrowIfCancelled()
                    AppLogger.e("Download", "Error saving downloaded file", e)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, resources.getString(R.string.swipe_download_save_error, e.message.orEmpty()), Toast.LENGTH_LONG).show()
                    }
                } finally {
                    body.close()
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.shareRequestSignal.collect { asset ->
            val baseUrl = uiState.baseUrl.removeSuffix("/")
            if (baseUrl.isEmpty()) return@collect

            Toast.makeText(context, resources.getString(R.string.swipe_share_preparing), Toast.LENGTH_SHORT).show()

            scope.launch(Dispatchers.IO) {
                // The API key is sent as a header on every request, never in the URL, so it cannot
                // end up in server, proxy or browser logs.
                val urlsToTry = listOf(
                    "$baseUrl/api/assets/${asset.id}/original",
                    "$baseUrl/api/asset/file/${asset.id}"
                )

                // Shared client: it adds the API key itself, and only for requests to the active server.
                val client = MediaHttp.client(context)

                var response: Response? = null
                for (shareUrl in urlsToTry) {
                    try {
                        val req = Request.Builder()
                            .url(shareUrl)
                            .build()
                        val resp = client.newCall(req).execute()
                        if (resp.isSuccessful) {
                            response = resp
                            break
                        } else {
                            resp.close()
                        }
                    } catch (e: Exception) {
                        e.rethrowIfCancelled()
                        AppLogger.w("Share", "Failed share endpoint $shareUrl: ${e.message}")
                    }
                }

                if (response == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, resources.getString(R.string.swipe_share_server_error), Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                try {
                    val body = response.body
                    val fileName = asset.originalFileName?.substringAfterLast('/')?.substringAfterLast('\\')
                        ?: "immich_${asset.id}.${asset.fileExtension ?: "jpg"}"

                    val cacheDir = File(context.cacheDir, "shared_assets").apply { mkdirs() }
                    val file = File(cacheDir, fileName)

                    file.outputStream().use { output ->
                        body.byteStream().use { input ->
                            input.copyTo(output)
                        }
                    }
                    body.close()

                    val contentUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )

                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = if (asset.type == "VIDEO") "video/*" else if (asset.isGif) "image/gif" else "image/*"
                        putExtra(Intent.EXTRA_STREAM, contentUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, resources.getString(R.string.swipe_share_chooser_title)))
                } catch (e: Exception) {
                    e.rethrowIfCancelled()
                    AppLogger.e("Share", "Error sharing asset", e)
                    launch(Dispatchers.Main) {
                        Toast.makeText(context, resources.getString(R.string.swipe_share_error, e.message.orEmpty()), Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    var topCardSwipeOffset by remember(uiState.currentIndex) { mutableFloatStateOf(0f) }

    SwipeScreenContent(
        uiState = uiState,
        viewModel = viewModel,
        sharedPlayer = sharedPlayer,
        tutorialController = tutorialController,
        showSortMenu = showSortMenu,
        onSortMenuToggle = { showSortMenu = it },
        topCardSwipeOffset = topCardSwipeOffset,
        onTopCardSwipeOffsetChanged = { topCardSwipeOffset = it },
        modifier = modifier
    )

    SwipeScreenDialogs(
        uiState = uiState,
        viewModel = viewModel,
        sharedPlayer = sharedPlayer
    )
}
