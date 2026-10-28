package com.markvoronin.immichswipe.data.repository

import android.content.Context
import com.markvoronin.immichswipe.core.AppTheme
import com.markvoronin.immichswipe.core.ButtonPositions
import com.markvoronin.immichswipe.core.ButtonStackOrientation
import com.markvoronin.immichswipe.core.ButtonVisibility
import com.markvoronin.immichswipe.core.CardDisplayMode
import com.markvoronin.immichswipe.core.DoubleTapAction
import com.markvoronin.immichswipe.core.IconPosition
import com.markvoronin.immichswipe.core.ImmichOpenMode
import com.markvoronin.immichswipe.core.PlaybackBehavior
import com.markvoronin.immichswipe.core.ReminderInterval
import com.markvoronin.immichswipe.core.SessionConfig
import com.markvoronin.immichswipe.core.SortOrder
import com.markvoronin.immichswipe.core.SwipeBehavior
import com.markvoronin.immichswipe.core.SwipeSettings
import com.markvoronin.immichswipe.core.util.combine6
import com.markvoronin.immichswipe.core.util.combine7
import com.markvoronin.immichswipe.data.datastore.SessionDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class SessionRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val accountRepository: AccountRepository
) : LoginSessionStore {

    private val dataStore = SessionDataStore(context)


    override val savedServerUrls: Flow<List<String>> = combine(
        dataStore.getSavedServerUrls(),
        accountRepository.allAccounts
    ) { dataStoreUrls, accounts ->
        val accountUrls = accounts.map { it.baseUrl }.toSet()
        (dataStoreUrls + accountUrls)
            .filter { it.isNotBlank() }
            .map { it.trim().removeSuffix("/") }
            .distinct()
            .sorted()
    }


    override val sessionConfig: Flow<SessionConfig?> = combine(
        dataStore.getBaseUrl(),
        dataStore.getApiKey(),
        dataStore.getUserId()
    ) { url, key, userId ->
        if (url != null && key != null && userId != null) {
            SessionConfig(url, key, userId)
        } else {
            null
        }
    }


    val playbackBehavior: Flow<PlaybackBehavior> = dataStore.getAudioFocusMode().map { modeString ->
        if (modeString == null) return@map PlaybackBehavior.PAUSE_OTHERS
        try {
            PlaybackBehavior.valueOf(modeString)
        } catch (_: Exception) {
            PlaybackBehavior.PAUSE_OTHERS
        }
    }


    val themeMode: Flow<AppTheme> = dataStore.getThemeMode().map {
        it?.let { try { AppTheme.valueOf(it) } catch(_: Exception) { AppTheme.SYSTEM } } ?: AppTheme.SYSTEM
    }


    val dynamicColor: Flow<Boolean> = dataStore.isDynamicColor()


    val fullscreenButtonPosition: Flow<IconPosition> = dataStore.getFullscreenIconPosition().map {
        it?.let { try { IconPosition.valueOf(it) } catch(_: Exception) { IconPosition.TOP_RIGHT } } ?: IconPosition.TOP_RIGHT
    }


    val immichButtonPosition: Flow<IconPosition> = dataStore.getImmichIconPosition().map {
        it?.let { try { IconPosition.valueOf(it) } catch(_: Exception) { IconPosition.BOTTOM_LEFT } } ?: IconPosition.BOTTOM_LEFT
    }

    val immichOpenMode: Flow<ImmichOpenMode> = dataStore.getImmichOpenMode().map {
        it?.let { try { ImmichOpenMode.valueOf(it) } catch(_: Exception) { ImmichOpenMode.APP } } ?: ImmichOpenMode.APP
    }

    val immichLongPressWeb: Flow<Boolean> = dataStore.getImmichLongPressWeb()


    val reminderEnabled: Flow<Boolean> = dataStore.isReminderEnabled()


    val reminderInterval: Flow<ReminderInterval> = dataStore.getReminderInterval().map {
        it?.let { try { ReminderInterval.valueOf(it) } catch(_: Exception) { ReminderInterval.OFTEN } } ?: ReminderInterval.OFTEN
    }

    val reminderCustomMinDays: Flow<Long> = dataStore.getReminderCustomMinDays()
    val reminderCustomMaxDays: Flow<Long> = dataStore.getReminderCustomMaxDays()


    val rotationButtonPosition: Flow<IconPosition> = dataStore.getCardDisplayIconPosition().map {
        it?.let { try { IconPosition.valueOf(it) } catch(_: Exception) { IconPosition.TOP_LEFT } } ?: IconPosition.TOP_LEFT
    }
    val cardDisplayButtonPosition: Flow<IconPosition> get() = rotationButtonPosition


    val muteButtonPosition: Flow<IconPosition> = dataStore.getMuteIconPosition().map {
        it?.let { try { IconPosition.valueOf(it) } catch(_: Exception) { IconPosition.BOTTOM_RIGHT } } ?: IconPosition.BOTTOM_RIGHT
    }


    val downloadButtonPosition: Flow<IconPosition> = dataStore.getDownloadIconPosition().map {
        it?.let { try { IconPosition.valueOf(it) } catch(_: Exception) { IconPosition.BOTTOM_LEFT } } ?: IconPosition.BOTTOM_LEFT
    }


    val shareButtonPosition: Flow<IconPosition> = dataStore.getShareIconPosition().map {
        it?.let { try { IconPosition.valueOf(it) } catch(_: Exception) { IconPosition.BOTTOM_RIGHT } } ?: IconPosition.BOTTOM_RIGHT
    }

    val fullSizeButtonPosition: Flow<IconPosition> = dataStore.getFullSizeIconPosition().map {
        it?.let { try { IconPosition.valueOf(it) } catch(_: Exception) { IconPosition.BOTTOM_LEFT } } ?: IconPosition.BOTTOM_LEFT
    }

    val buttonStackOrientation: Flow<ButtonStackOrientation> = dataStore.getButtonStackOrientation().map {
        it?.let { try { ButtonStackOrientation.valueOf(it) } catch(_: Exception) { ButtonStackOrientation.VERTICAL } } ?: ButtonStackOrientation.VERTICAL
    }

    val showFullscreenButton: Flow<Boolean> = dataStore.isShowFullscreenIcon()
    val showImmichButton: Flow<Boolean> = dataStore.isShowImmichIcon()
    val showRotationButton: Flow<Boolean> = dataStore.isShowCardDisplayIcon()
    val showCardDisplayButton: Flow<Boolean> get() = showRotationButton
    val showMuteButton: Flow<Boolean> = dataStore.isShowMuteIcon()
    val showDownloadButton: Flow<Boolean> = dataStore.isShowDownloadIcon()
    val showShareButton: Flow<Boolean> = dataStore.isShowShareIcon()
    val showFullSizeButton: Flow<Boolean> = dataStore.isShowFullSizeIcon()


    val defaultLayoutGrid: Flow<Boolean> = dataStore.isDefaultLayoutGrid()

    val showFavoriteButton: Flow<Boolean> = dataStore.isShowFavorite()
    val autoNextOnFav: Flow<Boolean> = dataStore.isAutoNextOnFav()
    val includeArchived: Flow<Boolean> = dataStore.isIncludeArchived()
    val showSwipeButtons: Flow<Boolean> = dataStore.isShowSwipeButtons()
    val showArchiveButton: Flow<Boolean> = dataStore.isShowArchiveButton()
    val showLockButton: Flow<Boolean> = dataStore.isShowLockButton()
    val showAddToAlbumButton: Flow<Boolean> = dataStore.isShowAddToAlbumButton()
    val backupWarningShown: Flow<Boolean> = dataStore.isBackupWarningShown()
    val syncLocalDeletion: Flow<Boolean> = dataStore.isSyncLocalDeletion()
    val trashLocalDeletion: Flow<Boolean> = dataStore.isTrashLocalDeletion()
    val tapToSwipeEnabled: Flow<Boolean> = dataStore.isTapToSwipeEnabled()
    
    val doubleTapEnabled: Flow<Boolean> = dataStore.isDoubleTapEnabled()
    val doubleTapAction: Flow<DoubleTapAction> = dataStore.getDoubleTapAction().map {
        it?.let { try { DoubleTapAction.valueOf(it) } catch(_: Exception) { DoubleTapAction.FULLSCREEN } } ?: DoubleTapAction.FULLSCREEN
    }
    
    val sortOrder: Flow<SortOrder> = dataStore.getSortOrder().map {
        it?.let { try { SortOrder.valueOf(it) } catch(_: Exception) { SortOrder.CHRONOLOGICAL_DESC } } ?: SortOrder.CHRONOLOGICAL_DESC
    }


    val defaultCardDisplayMode: Flow<CardDisplayMode> = dataStore.getDefaultCardDisplayMode().map {
        it?.let { try { CardDisplayMode.valueOf(it) } catch(_: Exception) { CardDisplayMode.FIT } } ?: CardDisplayMode.FIT
    }


    override suspend fun saveSession(baseUrl: String, token: String, userId: String) {
        dataStore.saveSession(baseUrl, token, userId)
    }


    suspend fun savePlaybackBehavior(behavior: PlaybackBehavior) {
        dataStore.saveAudioFocusMode(behavior.name)
    }


    suspend fun saveThemeMode(theme: AppTheme) {
        dataStore.saveThemeMode(theme.name)
    }


    suspend fun saveDynamicColor(enabled: Boolean) {
        dataStore.saveDynamicColor(enabled)
    }


    suspend fun saveFullscreenButtonPosition(pos: IconPosition) {
        dataStore.saveFullscreenIconPosition(pos.name)
    }


    suspend fun saveImmichButtonPosition(pos: IconPosition) {
        dataStore.saveImmichIconPosition(pos.name)
    }

    suspend fun saveImmichOpenMode(mode: ImmichOpenMode) {
        dataStore.saveImmichOpenMode(mode.name)
    }

    suspend fun saveImmichLongPressWeb(enabled: Boolean) {
        dataStore.saveImmichLongPressWeb(enabled)
    }

    suspend fun saveRotationButtonPosition(pos: IconPosition) {
        dataStore.saveCardDisplayIconPosition(pos.name)
    }


    suspend fun saveMuteButtonPosition(pos: IconPosition) {
        dataStore.saveMuteIconPosition(pos.name)
    }


    suspend fun saveDownloadButtonPosition(pos: IconPosition) {
        dataStore.saveDownloadIconPosition(pos.name)
    }


    suspend fun saveShareButtonPosition(pos: IconPosition) {
        dataStore.saveShareIconPosition(pos.name)
    }

    suspend fun saveFullSizeButtonPosition(pos: IconPosition) {
        dataStore.saveFullSizeIconPosition(pos.name)
    }

    suspend fun saveButtonStackOrientation(orientation: ButtonStackOrientation) {
        dataStore.saveButtonStackOrientation(orientation.name)
    }

    suspend fun saveShowFullscreenButton(show: Boolean) { dataStore.saveShowFullscreenIcon(show) }
    suspend fun saveShowImmichButton(show: Boolean) { dataStore.saveShowImmichIcon(show) }
    suspend fun saveShowRotationButton(show: Boolean) { dataStore.saveShowCardDisplayIcon(show) }
    suspend fun saveShowMuteButton(show: Boolean) { dataStore.saveShowMuteIcon(show) }
    suspend fun saveShowDownloadButton(show: Boolean) { dataStore.saveShowDownloadIcon(show) }
    suspend fun saveShowShareButton(show: Boolean) { dataStore.saveShowShareIcon(show) }
    suspend fun saveShowFullSizeButton(show: Boolean) { dataStore.saveShowFullSizeIcon(show) }


    suspend fun saveDefaultLayoutGrid(isGrid: Boolean) {
        dataStore.saveDefaultLayoutGrid(isGrid)
    }

    suspend fun saveShowFavorite(show: Boolean) { dataStore.saveShowFavorite(show) }
    suspend fun saveAutoNextOnFav(autoNextOnFav: Boolean) { dataStore.saveAutoNextOnFav(autoNextOnFav) }
    suspend fun saveShowSwipeButtons(show: Boolean) { dataStore.saveShowSwipeButtons(show) }
    suspend fun saveShowArchiveButton(show: Boolean) { dataStore.saveShowArchiveButton(show) }
    suspend fun saveShowLockButton(show: Boolean) { dataStore.saveShowLockButton(show) }
    suspend fun saveShowAddToAlbumButton(show: Boolean) { dataStore.saveShowAddToAlbumButton(show) }
    suspend fun saveBackupWarningShown(shown: Boolean) { dataStore.saveBackupWarningShown(shown) }
    suspend fun saveSyncLocalDeletion(sync: Boolean) { dataStore.saveSyncLocalDeletion(sync) }
    suspend fun saveTapToSwipeEnabled(enabled: Boolean) { dataStore.saveTapToSwipeEnabled(enabled) }
    suspend fun saveDoubleTapEnabled(enabled: Boolean) { dataStore.saveDoubleTapEnabled(enabled) }
    suspend fun saveDoubleTapAction(action: DoubleTapAction) { dataStore.saveDoubleTapAction(action.name) }
    suspend fun saveSortOrder(order: SortOrder) { dataStore.saveSortOrder(order.name) }

    suspend fun saveReminderEnabled(enabled: Boolean) { dataStore.saveReminderEnabled(enabled) }

    suspend fun saveReminderInterval(interval: ReminderInterval) { dataStore.saveReminderInterval(interval.name) }

    suspend fun saveReminderCustomMinDays(days: Long) { dataStore.saveReminderCustomMinDays(days) }

    suspend fun saveReminderCustomMaxDays(days: Long) { dataStore.saveReminderCustomMaxDays(days) }


    suspend fun clearSession() {
        dataStore.clearSession()
    }

    override suspend fun saveRecentServerUrl(url: String) {
        dataStore.saveRecentServerUrl(url)
    }

    override suspend fun removeSavedServerUrl(url: String) {
        dataStore.removeSavedServerUrl(url)
    }

    /** Encrypts API keys saved as plaintext by older versions (idempotent). */
    suspend fun migrateLegacySecrets() {
        dataStore.encryptStoredApiKeyIfNeeded()
        accountRepository.encryptLegacyApiKeys()
    }

    suspend fun initializeTutorialState() {
        dataStore.initializeTutorialStateForInstallOrUpgrade()
    }

    /**
     * Every setting the swipe screen reacts to, combined into one typed, de-duplicated flow. Built from
     * small typed groups so nothing needs unchecked casts.
     */
    val swipeSettings: Flow<SwipeSettings> = combine(
        combine7(
            fullscreenButtonPosition, immichButtonPosition, cardDisplayButtonPosition,
            muteButtonPosition, downloadButtonPosition, shareButtonPosition, fullSizeButtonPosition
        ) { fullscreen, immich, rotation, mute, download, share, fullSize ->
            ButtonPositions(fullscreen, immich, rotation, mute, download, share, fullSize)
        },
        combine(
            combine7(
                showFullscreenButton, showImmichButton, showCardDisplayButton,
                showMuteButton, showDownloadButton, showShareButton, showFullSizeButton
            ) { fullscreen, immich, rotation, mute, download, share, fullSize ->
                listOf(fullscreen, immich, rotation, mute, download, share, fullSize)
            },
            showFavoriteButton, showLockButton, showArchiveButton, showSwipeButtons
        ) { first, favorite, lock, archive, swipeButtons ->
            ButtonVisibility(
                fullscreen = first[0], immich = first[1], rotation = first[2],
                mute = first[3], download = first[4], share = first[5], fullSize = first[6],
                favorite = favorite, lock = lock, archive = archive, swipeButtons = swipeButtons
            )
        },
        combine6(
            playbackBehavior, autoNextOnFav, syncLocalDeletion,
            trashLocalDeletion, sortOrder, tapToSwipeEnabled
        ) { playback, autoNext, sync, trash, order, tap ->
            SwipeBehavior(playback, autoNext, sync, trash, order, tap)
        }
    ) { positions, visibility, behavior ->
        SwipeSettings(positions, visibility, behavior)
    }.distinctUntilChanged()

    val hasCompletedSwipeTutorial: Flow<Boolean> = dataStore.hasCompletedSwipeTutorial()
    suspend fun setHasCompletedSwipeTutorial(completed: Boolean) {
        dataStore.setHasCompletedSwipeTutorial(completed)
    }

    val hasCompletedDuplicatesTutorial: Flow<Boolean> = dataStore.hasCompletedDuplicatesTutorial()
    suspend fun setHasCompletedDuplicatesTutorial(completed: Boolean) {
        dataStore.setHasCompletedDuplicatesTutorial(completed)
    }

    val skipDeepAnalyzeConfirmation: Flow<Boolean> = dataStore.skipDeepAnalyzeConfirmation()
    suspend fun setSkipDeepAnalyzeConfirmation(skip: Boolean) {
        dataStore.setSkipDeepAnalyzeConfirmation(skip)
    }

    suspend fun resetTutorials() {
        dataStore.resetTutorials()
    }
}
