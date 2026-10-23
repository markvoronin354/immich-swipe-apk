package com.markvoronin.immichswipe.feature.home

import com.markvoronin.immichswipe.core.AppTheme
import com.markvoronin.immichswipe.core.ConnectionStatus
import com.markvoronin.immichswipe.core.PlaybackBehavior
import com.markvoronin.immichswipe.core.SessionConfig
import com.markvoronin.immichswipe.core.SessionManager
import com.markvoronin.immichswipe.core.SortOrder
import com.markvoronin.immichswipe.data.local.dao.AlbumDecisionCount
import com.markvoronin.immichswipe.data.local.dao.UnsyncedDecisionCounts
import com.markvoronin.immichswipe.data.local.entity.SyncHistoryEntity
import com.markvoronin.immichswipe.data.local.entity.UserAccountEntity
import com.markvoronin.immichswipe.data.repository.AccountRepository
import com.markvoronin.immichswipe.data.repository.AlbumRepository
import com.markvoronin.immichswipe.data.repository.AssetRepository
import com.markvoronin.immichswipe.data.repository.SessionRepository
import com.markvoronin.immichswipe.data.repository.SwipeDecisionRepository
import com.markvoronin.immichswipe.data.repository.UserRepository
import com.markvoronin.immichswipe.domain.model.Album
import com.markvoronin.immichswipe.domain.model.User
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    private val user = User(id = "u1", email = "a@b.c", name = "Ann")
    private val config = SessionConfig("https://photos.example.com", "key", "u1")
    private val rawAlbums = listOf(
        Album(id = "a1", albumName = "Trip", assetCount = 10, albumThumbnailAssetId = null),
        Album(id = "a2", albumName = "Pets", assetCount = 4, albumThumbnailAssetId = null)
    )
    private val refinedAlbums = rawAlbums.map { it.copy(assetCount = it.assetCount - 1) }

    private val sessionConfig = MutableStateFlow<SessionConfig?>(config)
    private val includeArchived = MutableStateFlow(false)
    private val accounts = MutableStateFlow<List<UserAccountEntity>>(emptyList())
    private val treatedCount = MutableStateFlow(0)
    private val unsyncedCount = MutableStateFlow(0)
    private val albumCounts = MutableStateFlow<List<AlbumDecisionCount>>(emptyList())
    private val history = MutableStateFlow<List<SyncHistoryEntity>>(emptyList())
    private val unsyncedDecisions = MutableStateFlow(UnsyncedDecisionCounts(0, 0))

    private val sessionRepository = mockk<SessionRepository>(relaxed = true) {
        every { playbackBehavior } returns flowOf(PlaybackBehavior.IGNORE)
        every { themeMode } returns flowOf(AppTheme.DARK)
        every { includeArchived } returns this@HomeViewModelTest.includeArchived
        every { sortOrder } returns flowOf(SortOrder.SIZE_DESC)
        every { defaultLayoutGrid } returns flowOf(true)
        every { backupWarningShown } returns flowOf(false)
    }
    private val albumRepository = mockk<AlbumRepository>(relaxed = true) {
        coEvery { getAlbumsRaw() } returns rawAlbums
        coEvery { refineAlbumCounts(any()) } returns refinedAlbums
    }
    private val swipeDecisionRepository = mockk<SwipeDecisionRepository>(relaxed = true) {
        every { getGlobalUniqueTreatedCount("u1") } returns treatedCount
        every { getGlobalUnsyncedCount("u1") } returns unsyncedCount
        every { getAllAlbumDecisionCounts("u1") } returns albumCounts
        every { getSyncHistory("u1") } returns history
        every { getUnsyncedDecisionCounts("u1") } returns unsyncedDecisions
    }
    private val assetRepository = mockk<AssetRepository>(relaxed = true) {
        coEvery { getTotalAssetCount(any()) } returns Result.success(100)
        coEvery { getOrphansCount(any()) } returns Result.success(7)
        coEvery { getDuplicatesCount() } returns Result.success(3)
    }
    private val accountRepository = mockk<AccountRepository>(relaxed = true) {
        every { allAccounts } returns accounts
    }
    private val userRepository = mockk<UserRepository>(relaxed = true) {
        coEvery { getCurrentUser() } returns user
    }
    private val sessionManager = mockk<SessionManager>(relaxed = true) {
        every { this@mockk.sessionConfig } returns this@HomeViewModelTest.sessionConfig
        every { connectionStatus } returns MutableStateFlow(ConnectionStatus())
        every { getUserId() } returns "u1"
    }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    /** Builds the view model and lets its initial load, including the minimum refresh time, finish. */
    private fun createViewModel(): HomeViewModel {
        val vm = HomeViewModel(
            sessionRepository, albumRepository, swipeDecisionRepository,
            assetRepository, accountRepository, userRepository, sessionManager
        )
        dispatcher.scheduler.advanceUntilIdle()
        return vm
    }

    private fun account(id: String) = UserAccountEntity(
        userId = id, baseUrl = "https://$id.example.com", apiKey = "k$id",
        userName = "Name $id", userEmail = "$id@example.com", avatarColor = "blue", lastActive = 5L
    )

    private fun syncEntry(timestamp: Long, deleted: Int = 0, bytes: Long = 0, locked: Int = 0, kept: Int = 0, archived: Int = 0) =
        SyncHistoryEntity(
            userId = "u1", timestamp = timestamp, deletedCount = deleted,
            bytesSaved = bytes, lockedCount = locked, keptCount = kept, archivedCount = archived
        )

    @Test
    fun `settings are mirrored into the state`() {
        val state = createViewModel().uiState.value

        assertEquals(PlaybackBehavior.IGNORE, state.playbackBehavior)
        assertEquals(AppTheme.DARK, state.themeMode)
        assertEquals(SortOrder.SIZE_DESC, state.sortOrder)
        assertTrue(state.isGridView)
        assertTrue(state.showBackupWarning)
        assertEquals(config.baseUrl, state.baseUrl)
    }

    @Test
    fun `the user and albums are loaded once the session is known`() {
        val state = createViewModel().uiState.value

        assertEquals(user, state.user)
        assertEquals(100, state.allAssetsCount)
        assertEquals(7, state.orphansCount)
        assertEquals(3, state.duplicatesCount)
        assertFalse(state.isLoading)
        assertFalse(state.isRefreshing)
        assertNull(state.error)
    }

    @Test
    fun `album counts are refined unless archived assets are included`() {
        assertEquals(refinedAlbums, createViewModel().uiState.value.albums)

        includeArchived.value = true
        assertEquals(rawAlbums, createViewModel().uiState.value.albums)
    }

    @Test
    fun `changing the archived setting reloads the albums with it`() {
        val vm = createViewModel()
        includeArchived.value = true
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.includeArchived)
        coVerify { assetRepository.getTotalAssetCount(true) }
    }

    @Test
    fun `a failing user request is shown as an error`() {
        // Offline: the user and the album requests all fail, so the later refresh cannot clear the error.
        coEvery { userRepository.getCurrentUser() } throws IOException("offline")
        coEvery { albumRepository.getAlbumsRaw() } throws IOException("offline")
        val state = createViewModel().uiState.value

        assertEquals("offline", state.error)
        assertFalse(state.isLoading)
    }

    @Test
    fun `a failing count keeps the previous number`() {
        val vm = createViewModel()
        coEvery { assetRepository.getTotalAssetCount(any()) } returns Result.failure(IOException("boom"))
        coEvery { assetRepository.getOrphansCount(any()) } returns Result.success(9)

        vm.refreshAlbums()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(100, vm.uiState.value.allAssetsCount)
        assertEquals(9, vm.uiState.value.orphansCount)
        assertFalse(vm.uiState.value.isRefreshing)
    }

    @Test
    fun `a failing refresh stops the spinner and keeps the albums`() {
        val vm = createViewModel()
        coEvery { albumRepository.getAlbumsRaw() } throws IOException("boom")

        vm.refreshAlbums()
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.value.isRefreshing)
        assertEquals(refinedAlbums, vm.uiState.value.albums)
    }

    @Test
    fun `logging out of the session clears the user and albums`() {
        val vm = createViewModel()
        sessionConfig.value = null
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals("", state.baseUrl)
        assertNull(state.user)
        assertTrue(state.albums.isEmpty())
        assertEquals(mapOf(Album.VIRTUAL_ALL_ID to 0), state.albumTreatedCounts)
    }

    @Test
    fun `treated counts per album include the global count for all photos`() {
        val vm = createViewModel()
        treatedCount.value = 12
        unsyncedCount.value = 5
        albumCounts.value = listOf(AlbumDecisionCount("a1", totalCount = 8, unsyncedCount = 2))

        val state = vm.uiState.value
        assertEquals(mapOf("a1" to 8, Album.VIRTUAL_ALL_ID to 12), state.albumTreatedCounts)
        assertEquals(mapOf("a1" to 2, Album.VIRTUAL_ALL_ID to 5), state.albumUnsyncedChanges)
    }

    @Test
    fun `stats add up the sync history and the pending decisions`() {
        val vm = createViewModel()
        val now = System.currentTimeMillis()
        val twoWeeksAgo = now - 14 * 24 * 60 * 60 * 1000L
        unsyncedDecisions.value = UnsyncedDecisionCounts(keptCount = 3, archivedCount = 1)
        history.value = listOf(
            syncEntry(now, deleted = 2, bytes = 1000, kept = 4, archived = 1, locked = 1),
            syncEntry(twoWeeksAgo, deleted = 5, bytes = 9000, kept = 6)
        )

        val stats = vm.uiState.value.stats
        assertEquals(7, stats.totalDeleted)
        assertEquals(10_000L, stats.totalBytesSaved)
        assertEquals(13, stats.totalKept)
        assertEquals(2, stats.totalArchived)
        assertEquals(1, stats.totalLocked)
        assertEquals(2, stats.weeklyDeleted)
        assertEquals(1000L, stats.weeklyBytesSaved)
    }

    @Test
    fun `saved accounts are listed for the profile popup`() {
        val vm = createViewModel()
        accounts.value = listOf(account("u1"), account("u2"))

        val saved = vm.uiState.value.savedAccounts
        assertEquals(listOf("u1", "u2"), saved.map { it.userId })
        assertEquals("Name u2", saved[1].userName)
    }

    @Test
    fun `picking an album opens the swipe tab and remembers where it came from`() {
        val vm = createViewModel()
        vm.onAlbumSelected(rawAlbums[0])

        val state = vm.uiState.value
        assertEquals(rawAlbums[0], state.selectedAlbum)
        assertEquals(HomeTab.SWIPE, state.currentTab)
        assertEquals(HomeTab.HOME, state.previousTab)
    }

    @Test
    fun `popups, search and categories are plain state toggles`() {
        val vm = createViewModel()

        vm.toggleProfilePopup(true)
        vm.toggleStatsPopup(true)
        vm.onSearchQueryChanged("trip")
        vm.toggleGlobalResetConfirmation(true)
        val open = vm.uiState.value
        assertTrue(open.showProfilePopup && open.showStatsPopup && open.showGlobalResetConfirmation)
        assertEquals("trip", open.searchQuery)

        vm.toggleCategory(AlbumStatus.COMPLETED)
        assertFalse(AlbumStatus.COMPLETED in vm.uiState.value.collapsedCategories)
        vm.toggleCategory(AlbumStatus.COMPLETED)
        assertTrue(AlbumStatus.COMPLETED in vm.uiState.value.collapsedCategories)
    }

    @Test
    fun `virtual album names keep earlier descriptions when only the name changes`() {
        val vm = createViewModel()
        vm.updateVirtualNames("v", "Name", "Description")
        vm.updateVirtualNames("v", "Renamed")

        assertEquals("Renamed", vm.uiState.value.virtualNames["v"])
        assertEquals("Description", vm.uiState.value.virtualDescriptions["v"])
    }

    @Test
    fun `starting and cancelling an extra login closes the profile popup`() {
        val vm = createViewModel()
        vm.toggleProfilePopup(true)

        vm.startAddAccount()
        assertTrue(vm.uiState.value.isLoggingInToAnotherAccount)
        assertFalse(vm.uiState.value.showProfilePopup)

        vm.cancelAddAccount()
        assertFalse(vm.uiState.value.isLoggingInToAnotherAccount)
    }

    @Test
    fun `the layout toggle saves the opposite of the current layout`() {
        createViewModel().toggleLayoutMode()
        coVerify { sessionRepository.saveDefaultLayoutGrid(false) }
    }

    @Test
    fun `dismissing the backup warning is remembered`() {
        createViewModel().dismissBackupWarning()
        coVerify { sessionRepository.saveBackupWarningShown(true) }
    }

    @Test
    fun `logging out deletes the account and then ends the session`() {
        val vm = createViewModel()
        vm.onAlbumSelected(rawAlbums[0])
        vm.logout()

        coVerifyOrder {
            accountRepository.deleteAccount("u1")
            sessionRepository.clearSession()
        }
        assertEquals(HomeTab.HOME, vm.uiState.value.currentTab)
    }

    @Test
    fun `removing another account leaves the session alone`() {
        createViewModel().removeAccount("u2")

        coVerify { accountRepository.deleteAccount("u2") }
        coVerify(exactly = 0) { sessionRepository.clearSession() }
    }

    @Test
    fun `removing the current account logs out`() {
        createViewModel().removeAccount("u1")

        coVerify { accountRepository.deleteAccount("u1") }
        coVerify { sessionRepository.clearSession() }
    }

    @Test
    fun `switching account saves its session and marks it active`() {
        coEvery { accountRepository.getAccount("u2") } returns account("u2")
        createViewModel().switchAccount("u2")

        coVerify { accountRepository.updateLastActive("u2") }
        coVerify { sessionRepository.saveSession("https://u2.example.com", "ku2", "u2") }
    }

    @Test
    fun `switching to an unknown account does nothing`() {
        coEvery { accountRepository.getAccount("ghost") } returns null
        createViewModel().switchAccount("ghost")

        coVerify(exactly = 0) { sessionRepository.saveSession(any(), any(), any()) }
        coVerify(exactly = 0) { accountRepository.updateLastActive(any()) }
    }

    @Test
    fun `a full reset clears the active user's decisions and refreshes`() {
        val vm = createViewModel()
        vm.toggleGlobalResetConfirmation(true)
        vm.resetAllDecisions()
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { swipeDecisionRepository.clearUserData("u1") }
        assertFalse(vm.uiState.value.showGlobalResetConfirmation)
        coVerify(atLeast = 2) { albumRepository.getAlbumsRaw() }
    }

    @Test
    fun `reset requests are delivered to the collector`() = runTest(dispatcher) {
        val vm = createViewModel()
        vm.requestReset()
        assertEquals(Unit, vm.resetRequestSignal.first())
    }
}
