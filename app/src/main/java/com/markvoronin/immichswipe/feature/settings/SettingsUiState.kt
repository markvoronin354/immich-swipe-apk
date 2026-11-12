package com.markvoronin.immichswipe.feature.settings

import com.markvoronin.immichswipe.core.AppPalette
import com.markvoronin.immichswipe.core.PaletteStyle
import com.markvoronin.immichswipe.core.AppTheme
import com.markvoronin.immichswipe.core.ButtonStackOrientation
import com.markvoronin.immichswipe.core.DoubleTapAction
import com.markvoronin.immichswipe.core.IconPosition
import com.markvoronin.immichswipe.core.ImmichOpenMode
import com.markvoronin.immichswipe.core.PlaybackBehavior
import com.markvoronin.immichswipe.core.ReminderInterval
import com.markvoronin.immichswipe.core.SortOrder
import com.markvoronin.immichswipe.core.ToolbarButton

enum class DatabaseScope {
    ALL, USER
}

enum class DatabaseAction {
    DELETE, EXPORT, IMPORT
}


data class SettingsUiState(
    val isLoading: Boolean = false,
    val userName: String = "",
    val userQuotaBytes: Long? = null,
    val savedAccountsCount: Int = 1,
    val playbackBehavior: PlaybackBehavior = PlaybackBehavior.PAUSE_OTHERS,
    val themeMode: AppTheme = AppTheme.SYSTEM,
    val palette: AppPalette = AppPalette.WALLPAPER,
    val customPaletteSeed: Int = AppPalette.DEFAULT_CUSTOM_SEED,
    val customPaletteStyle: PaletteStyle = PaletteStyle.BALANCED,
    val fullscreenButtonPosition: IconPosition = IconPosition.TOP_RIGHT,
    val immichButtonPosition: IconPosition = IconPosition.BOTTOM_LEFT,
    val immichOpenMode: ImmichOpenMode = ImmichOpenMode.APP,
    val immichLongPressWeb: Boolean = true,
    val rotationButtonPosition: IconPosition = IconPosition.TOP_LEFT,
    val muteButtonPosition: IconPosition = IconPosition.BOTTOM_RIGHT,
    val downloadButtonPosition: IconPosition = IconPosition.BOTTOM_LEFT,
    val shareButtonPosition: IconPosition = IconPosition.BOTTOM_RIGHT,
    val fullSizeButtonPosition: IconPosition = IconPosition.BOTTOM_RIGHT,
    val buttonStackOrientation: ButtonStackOrientation = ButtonStackOrientation.VERTICAL,
    val showFullscreenButton: Boolean = true,
    val showImmichButton: Boolean = false,
    val showRotationButton: Boolean = true,
    val showMuteButton: Boolean = true,
    val showDownloadButton: Boolean = true,
    val showShareButton: Boolean = false,
    val showFullSizeButton: Boolean = true,
    val keepFullResolution: Boolean = false,
    val showFavoriteButton: Boolean = true,
    val autoNextOnFav: Boolean = false,
    val includeArchived: Boolean = false,
    val sortOrder: SortOrder = SortOrder.CHRONOLOGICAL_DESC,
    val showLogsDialog: Boolean = false,
    val showSwipeButtons: Boolean = false,
    val showArchiveButton: Boolean = true,
    val showLockButton: Boolean = false,
    val showAddToAlbumButton: Boolean = true,
    val showToolbarLabels: Boolean = true,
    val toolbarButtonOrder: List<ToolbarButton> = ToolbarButton.entries,
    val syncLocalDeletion: Boolean = false,
    val trashLocalDeletion: Boolean = true,
    val tapToSwipeEnabled: Boolean = false,
    val doubleTapEnabled: Boolean = true,
    val doubleTapAction: DoubleTapAction = DoubleTapAction.FULLSCREEN,
    val showActionButtonsDialog: Boolean = false,
    val showInteractionsDialog: Boolean = false,
    val showCustomPaletteEditor: Boolean = false,
    val showClearCacheConfirmation: Boolean = false,
    val reminderEnabled: Boolean = false,
    val reminderInterval: ReminderInterval = ReminderInterval.OFTEN,
    val reminderCustomMinDays: Long = 3L,
    val reminderCustomMaxDays: Long = 7L,
    val showCustomReminderDialog: Boolean = false,
    
    // Database actions
    val pendingDatabaseAction: DatabaseAction? = null,
    val pendingDatabaseScope: DatabaseScope? = null,
    val databaseActionStatus: String? = null,
)
