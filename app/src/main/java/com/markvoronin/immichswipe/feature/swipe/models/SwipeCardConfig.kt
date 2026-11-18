package com.markvoronin.immichswipe.feature.swipe.models

import com.markvoronin.immichswipe.core.ButtonStackOrientation
import com.markvoronin.immichswipe.core.ButtonStackStyle
import com.markvoronin.immichswipe.core.IconPosition
import com.markvoronin.immichswipe.core.ImmichOpenMode
import com.markvoronin.immichswipe.core.PlaybackBehavior
import com.markvoronin.immichswipe.core.SwipeAction
import com.markvoronin.immichswipe.domain.model.Asset

data class SwipeCardConfig(
    val baseUrl: String = "",
    val playbackBehavior: PlaybackBehavior,
    val fullscreenButtonPosition: IconPosition,
    val immichButtonPosition: IconPosition,
    val rotationButtonPosition: IconPosition,
    val muteButtonPosition: IconPosition,
    val downloadButtonPosition: IconPosition = IconPosition.TOP_LEFT,
    val shareButtonPosition: IconPosition = IconPosition.TOP_RIGHT,
    val infoButtonPosition: IconPosition = IconPosition.BOTTOM_LEFT,
    val fullSizeButtonPosition: IconPosition = IconPosition.BOTTOM_LEFT,
    val buttonStackOrientation: ButtonStackOrientation = ButtonStackOrientation.VERTICAL,
    val buttonStackStyle: ButtonStackStyle = ButtonStackStyle.CONNECTED,
    val showFullscreenButton: Boolean = true,
    val showImmichButton: Boolean = false,
    val showRotationButton: Boolean = true,
    val showMuteButton: Boolean = true,
    val showDownloadButton: Boolean = false,
    val showShareButton: Boolean = false,
    val showInfoButton: Boolean = false,
    val showFullSizeButton: Boolean = false,
    val immichOpenMode: ImmichOpenMode = ImmichOpenMode.APP,
    val immichLongPressWeb: Boolean = true,
    val tapToSwipeEnabled: Boolean = false,
    val swipeLeftAction: SwipeAction = SwipeAction.DELETE,
    val swipeRightAction: SwipeAction = SwipeAction.KEEP,
    val swipeUpAction: SwipeAction = SwipeAction.DETAILS,
    val swipeDownAction: SwipeAction = SwipeAction.UNDO,
    val canUndo: Boolean = false,
    val showSizeIndicator: Boolean = false,
    val rotationAngle: Int = 0,
    val isFullSize: Boolean = false
)

data class SwipeCardActions(
    val onSwipe: (SwipeAction) -> Unit,
    val onUndo: () -> Unit = {},
    val onRotateAsset: () -> Unit = {},
    val onDoubleTap: () -> Unit,
    val onOpenFullscreen: () -> Unit,
    val onDownload: (Asset) -> Unit = {},
    val onShare: (Asset) -> Unit = {},
    val onToggleMute: () -> Unit = {},
    val onToggleFullSize: () -> Unit = {},
    val loadAlbumNames: suspend (String) -> List<String> = { emptyList() },
    val saveDescription: suspend (String, String) -> Boolean = { _, _ -> false },
    val onSwipeOffsetChanged: (Float) -> Unit = {}
)
