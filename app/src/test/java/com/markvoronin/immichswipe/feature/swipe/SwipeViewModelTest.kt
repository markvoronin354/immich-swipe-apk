package com.markvoronin.immichswipe.feature.swipe

import android.app.PendingIntent
import android.net.Uri
import com.markvoronin.immichswipe.core.ButtonPositions
import com.markvoronin.immichswipe.core.ButtonVisibility
import com.markvoronin.immichswipe.core.ConnectionStatus
import com.markvoronin.immichswipe.core.IconPosition
import com.markvoronin.immichswipe.core.PlaybackBehavior
import com.markvoronin.immichswipe.core.SessionConfig
import com.markvoronin.immichswipe.core.SessionManager
import com.markvoronin.immichswipe.core.SortCategory
import com.markvoronin.immichswipe.core.SortOrder
import com.markvoronin.immichswipe.core.SwipeBehavior
import com.markvoronin.immichswipe.core.SwipeSettings
import com.markvoronin.immichswipe.data.local.entity.SwipeDecisionEntity
import com.markvoronin.immichswipe.data.repository.AlbumRepository
import com.markvoronin.immichswipe.data.repository.AssetBatch
import com.markvoronin.immichswipe.data.repository.AssetRepository
import com.markvoronin.immichswipe.data.repository.LocalMediaRepository
import com.markvoronin.immichswipe.data.repository.SessionRepository
import com.markvoronin.immichswipe.data.repository.SwipeDecisionRepository
import com.markvoronin.immichswipe.domain.model.Album
import com.markvoronin.immichswipe.domain.model.Asset
import com.markvoronin.immichswipe.domain.model.ExifInfo
import com.markvoronin.immichswipe.domain.usecase.ApplySwipeDecisionsUseCase
import com.markvoronin.immichswipe.domain.usecase.ApplySwipeDecisionsUseCase.ApplyResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class SwipeViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    private fun asset(id: String, size: Long = 100) = Asset(
        id = id, ownerId = "o", fileCreatedAt = "2024-01-01T00:00:00Z", type = "IMAGE",
        exifInfo = ExifInfo(fileSizeInBytes = size)
    )

    private val a1 = asset("a1", 100)
    private val a2 = asset("a2", 200)
    private val a3 = asset("a3", 300)
    private val a4 = asset("a4", 400)
    private var assets = listOf(a1, a2, a3, a4)

    private val album = Album(id = "album", albumName = "Trip", assetCount = 4, albumThumbnailAssetId = null)

    private val sessionConfig = MutableStateFlow<SessionConfig?>(SessionConfig("https://photos.example.com", "key", "u1"))
    private val swipeSettings = MutableStateFlow(settings())
    private val sortOrder = MutableStateFlow(SortOrder.CHRONOLOGICAL_DESC)
    private val storedDecisions = MutableStateFlow<List<SwipeDecisionEntity>>(emptyList())

    private val assetRepository = mockk<AssetRepository>(relaxed = true) {
        // The view model also loads once before an album is chosen; that load stays empty.
        every { getAssetsByAlbum(any(), any(), any(), any(), any()) } answers {
            if (firstArg<String>() == "album") flowOf(AssetBatch(assets = assets, total = assets.size))
            else flowOf(AssetBatch(assets = emptyList(), total = 0))
        }
        every { applySort(any(), any(), any()) } answers { firstArg() }
        coEvery { getAssetDetail(any()) } answers { assets.first { it.id == firstArg<String>() } }
    }
    private val sessionRepository = mockk<SessionRepository>(relaxed = true) {
        every { sessionConfig } returns this@SwipeViewModelTest.sessionConfig
        every { swipeSettings } returns this@SwipeViewModelTest.swipeSettings
        every { sortOrder } returns this@SwipeViewModelTest.sortOrder
    }
    private val swipeDecisionRepository = mockk<SwipeDecisionRepository>(relaxed = true) {
        every { getDecisionsForAlbum(any(), "u1") } returns storedDecisions
    }
    private val albumRepository = mockk<AlbumRepository>(relaxed = true)
    private val sessionManager = mockk<SessionManager>(relaxed = true) {
        every { this@mockk.sessionConfig } returns this@SwipeViewModelTest.sessionConfig
        every { connectionStatus } returns MutableStateFlow(ConnectionStatus())
    }
    private val applyUseCase = mockk<ApplySwipeDecisionsUseCase>()
    private val localMediaRepository = mockk<LocalMediaRepository>(relaxed = true)

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun settings(
        autoNextOnFavorite: Boolean = false,
        syncLocalDeletion: Boolean = false,
        trashLocalDeletion: Boolean = true,
        order: SortOrder = SortOrder.CHRONOLOGICAL_DESC
    ) = SwipeSettings(
        positions = ButtonPositions(
            IconPosition.TOP_RIGHT, IconPosition.BOTTOM_LEFT, IconPosition.TOP_LEFT,
            IconPosition.BOTTOM_RIGHT, IconPosition.BOTTOM_LEFT, IconPosition.BOTTOM_RIGHT, IconPosition.BOTTOM_LEFT
        ),
        visibility = ButtonVisibility(
            fullscreen = true, immich = false, rotation = true, mute = false, download = true,
            share = true, fullSize = false, favorite = true, lock = false, archive = true, swipeButtons = false
        ),
        behavior = SwipeBehavior(
            PlaybackBehavior.PAUSE_OTHERS, autoNextOnFavorite, syncLocalDeletion, trashLocalDeletion, order, tapToSwipe = false
        )
    )

    private fun entity(id: String, decision: String, synced: Boolean = false, size: Long? = null) =
        SwipeDecisionEntity(
            assetId = id, albumId = album.id, userId = "u1", decision = decision,
            fileSize = size, isSynced = synced
        )

    /**
     * The sorting runs on a background dispatcher, so the screen is only ready after a short wait.
     * Waiting for loading and fetching to both end means the last refresh has been applied.
     */
    private fun SwipeViewModel.await(predicate: (SwipeUiState) -> Boolean): SwipeUiState =
        runBlocking { withTimeout(5_000.milliseconds) { uiState.first(predicate) } }

    private fun openAlbum(expectedAssets: Int = assets.size): SwipeViewModel {
        val vm = SwipeViewModel(
            assetRepository, sessionRepository, swipeDecisionRepository, albumRepository,
            sessionManager, applyUseCase, localMediaRepository
        )
        vm.initAlbum(album, userQuotaBytes = 5_000L)
        vm.await { it.assets.size == expectedAssets && !it.isLoading && !it.isFetchingAssets }
        return vm
    }

    private fun SwipeViewModel.decisions() = uiState.value.decisions

    private fun applyResult(
        toDelete: List<String> = emptyList(),
        toLock: List<String> = emptyList(),
        deletedAssets: List<Asset> = emptyList(),
        failedFavoriteIds: Set<String> = emptySet(),
        failed: Set<ApplySwipeDecisionsUseCase.SyncStep> = emptySet()
    ) = ApplyResult(
        toDelete = toDelete, toLock = toLock, nonSkipDecidedIds = emptyList(),
        deletedAssets = deletedAssets, failedSteps = failed, failedFavoriteIds = failedFavoriteIds
    )

    // --- loading ---

    @Test
    fun `opening an album loads its assets from the first one`() {
        val vm = openAlbum()
        val state = vm.uiState.value

        assertEquals(listOf("a1", "a2", "a3", "a4"), state.assets.map { it.id })
        assertEquals(0, state.currentIndex)
        assertEquals("Trip", state.albumName)
        assertEquals("album", state.albumId)
        assertEquals(4, state.remoteTotalCount)
        assertEquals(5_000L, state.userQuotaBytes)
        assertEquals("https://photos.example.com", state.baseUrl)
        assertNull(state.error)
    }

    @Test
    fun `saved decisions are restored and the first undecided asset is current`() {
        storedDecisions.value = listOf(
            entity("a1", "KEEP", synced = true), entity("a2", "DELETE"), entity("gone", "KEEP")
        )
        val vm = openAlbum()
        val state = vm.uiState.value

        assertEquals(2, state.currentIndex)
        // Decisions for assets that are no longer in the album are dropped.
        assertEquals(mapOf("a1" to SwipeDecision.KEEP, "a2" to SwipeDecision.DELETE), state.decisions)
        // Only the decisions not yet applied to the server can be undone.
        assertEquals(listOf("a2"), state.history)
    }

    @Test
    fun `an unknown saved decision counts as keep`() {
        storedDecisions.value = listOf(entity("a1", "SOMETHING_NEW"))
        assertEquals(SwipeDecision.KEEP, openAlbum().decisions()["a1"])
    }

    @Test
    fun `opening the same album again does not reload it`() {
        val vm = openAlbum()
        vm.initAlbum(album)

        verify(exactly = 1) { swipeDecisionRepository.getDecisionsForAlbum("album", "u1") }
    }

    @Test
    fun `settings are applied to the state`() {
        val vm = openAlbum()
        swipeSettings.value = settings(autoNextOnFavorite = true, syncLocalDeletion = true, order = SortOrder.SIZE_ASC)

        val state = vm.uiState.value
        assertTrue(state.autoNextOnFav)
        assertTrue(state.syncLocalDeletion)
        assertEquals(SortOrder.SIZE_ASC, state.sortOrder)
        assertEquals(SortCategory.SIZE, state.sortCategory)
        assertEquals(IconPosition.TOP_RIGHT, state.fullscreenButtonPosition)
    }

    @Test
    fun `the sort order decides the sort category`() {
        val vm = openAlbum()
        val expected = mapOf(
            SortOrder.CHRONOLOGICAL_ASC to SortCategory.TIME,
            SortOrder.SHUFFLED to SortCategory.TIME,
            SortOrder.SIZE_DESC to SortCategory.SIZE,
            SortOrder.TYPE_VIDEO_FIRST to SortCategory.TYPE,
            SortOrder.TYPE_PHOTO_FIRST_SHUFFLED to SortCategory.TYPE
        )
        expected.forEach { (order, category) ->
            swipeSettings.value = settings(order = order)
            assertEquals(order.name, category, vm.uiState.value.sortCategory)
        }
    }

    // --- swiping ---

    @Test
    fun `swiping records the decision, moves on and saves it`() {
        val vm = openAlbum()
        vm.onSwipe(SwipeDecision.DELETE)

        assertEquals(SwipeDecision.DELETE, vm.decisions()["a1"])
        assertEquals(1, vm.uiState.value.currentIndex)
        assertEquals(listOf("a1"), vm.uiState.value.history)
        coVerify { swipeDecisionRepository.saveDecision("a1", "album", "u1", "DELETE", 100L) }
    }

    @Test
    fun `swiping skips assets that already have a decision`() {
        storedDecisions.value = listOf(entity("a2", "KEEP"))
        val vm = openAlbum()
        assertEquals(0, vm.uiState.value.currentIndex)

        vm.onSwipe(SwipeDecision.KEEP)

        assertEquals(2, vm.uiState.value.currentIndex)
    }

    @Test
    fun `swiping the last asset moves past the end`() {
        val vm = openAlbum()
        repeat(4) { vm.onSwipe(SwipeDecision.KEEP) }

        assertEquals(4, vm.uiState.value.currentIndex)
        assertNull(vm.uiState.value.currentAsset)

        vm.onSwipe(SwipeDecision.KEEP)
        coVerify(exactly = 4) { swipeDecisionRepository.saveDecision(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `swiping does not save anything without a session`() {
        val vm = openAlbum()
        sessionConfig.value = null
        vm.onSwipe(SwipeDecision.KEEP)

        assertEquals(SwipeDecision.KEEP, vm.decisions()["a1"])
        coVerify(exactly = 0) { swipeDecisionRepository.saveDecision(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `archive and lock are swipes too`() {
        val vm = openAlbum()
        vm.toggleArchive()
        vm.toggleLock()

        assertEquals(SwipeDecision.ARCHIVE, vm.decisions()["a1"])
        assertEquals(SwipeDecision.LOCK, vm.decisions()["a2"])
    }

    @Test
    fun `undo takes back the last swipe`() {
        val vm = openAlbum()
        vm.onSwipe(SwipeDecision.KEEP)
        vm.onSwipe(SwipeDecision.DELETE)

        vm.undo()

        assertNull(vm.decisions()["a2"])
        assertEquals(SwipeDecision.KEEP, vm.decisions()["a1"])
        assertEquals(1, vm.uiState.value.currentIndex)
        assertEquals(listOf("a1"), vm.uiState.value.history)
        coVerify { swipeDecisionRepository.removeDecision("a2", "u1") }
    }

    @Test
    fun `undo with nothing to undo does nothing`() {
        val vm = openAlbum()
        vm.undo()

        coVerify(exactly = 0) { swipeDecisionRepository.removeDecision(any(), any()) }
    }

    @Test
    fun `a specific decision can be undone out of order`() {
        val vm = openAlbum()
        vm.onSwipe(SwipeDecision.KEEP)
        vm.onSwipe(SwipeDecision.DELETE)

        vm.undoSpecificDecision("a1")

        assertEquals(mapOf("a2" to SwipeDecision.DELETE), vm.decisions())
        assertEquals(listOf("a2"), vm.uiState.value.history)
        coVerify { swipeDecisionRepository.removeDecision("a1", "u1") }
    }

    // --- favourites, rotation, navigation ---

    @Test
    fun `favouriting flips the local flag`() {
        val vm = openAlbum()
        vm.toggleFavorite()
        assertTrue(vm.uiState.value.isFavorite("a1"))

        vm.toggleFavoriteFor("a1")
        assertFalse(vm.uiState.value.isFavorite("a1"))
        assertEquals(0, vm.uiState.value.currentIndex)
    }

    @Test
    fun `favouriting the current asset also keeps it when auto next is on`() {
        val vm = openAlbum()
        swipeSettings.value = settings(autoNextOnFavorite = true)

        vm.toggleFavorite()

        assertEquals(SwipeDecision.KEEP, vm.decisions()["a1"])
        assertEquals(1, vm.uiState.value.currentIndex)
    }

    @Test
    fun `favouriting another asset never swipes with auto next on`() {
        val vm = openAlbum()
        swipeSettings.value = settings(autoNextOnFavorite = true)

        vm.toggleFavoriteFor("a3")

        assertTrue(vm.decisions().isEmpty())
    }

    @Test
    fun `rotating turns a quarter each time, wraps around and is saved`() {
        val vm = openAlbum()

        repeat(4) { vm.rotateCurrentAsset() }

        assertEquals(0, vm.uiState.value.getRotation("a1"))
        coVerify { assetRepository.updateAssetRotation("a1", "u1", 90) }
        coVerify { assetRepository.updateAssetRotation("a1", "u1", 180) }
        coVerify { assetRepository.updateAssetRotation("a1", "u1", 270) }
        coVerify { assetRepository.updateAssetRotation("a1", "u1", 0) }
    }

    @Test
    fun `moving to an asset stays within the list`() {
        val vm = openAlbum()
        vm.onMoveToAsset(2)
        assertEquals(2, vm.uiState.value.currentIndex)

        vm.onMoveToAsset(9)
        vm.onMoveToAsset(-1)
        assertEquals(2, vm.uiState.value.currentIndex)
    }

    @Test
    fun `the next unprocessed index skips decided assets and reports the end`() {
        storedDecisions.value = listOf(entity("a2", "KEEP"))
        val vm = openAlbum()

        assertEquals(2, vm.getNextUnprocessedIndex())

        vm.onMoveToAsset(3)
        assertEquals(-1, vm.getNextUnprocessedIndex())
    }

    @Test
    fun `changing the sort order saves it and shows the loading state`() {
        val vm = openAlbum()
        vm.setSortOrder(SortOrder.SIZE_DESC)

        assertEquals(SortOrder.SIZE_DESC, vm.uiState.value.sortOrder)
        assertTrue(vm.uiState.value.isLoading)
        coVerify { sessionRepository.saveSortOrder(SortOrder.SIZE_DESC) }
    }

    @Test
    fun `choosing the current sort order again does nothing`() {
        val vm = openAlbum()
        vm.setSortOrder(SortOrder.CHRONOLOGICAL_DESC)

        assertFalse(vm.uiState.value.isLoading)
        coVerify(exactly = 0) { sessionRepository.saveSortOrder(any()) }
    }

    // --- bulk mode ---

    @Test
    fun `bulk delete marks the selection, saves it and moves past it`() {
        val vm = openAlbum()
        vm.enterBulkMode(isDelete = true)
        assertTrue(vm.uiState.value.isBulkDeleteMode)

        vm.setBulkSelection(setOf("a1", "a2"), last = 1)
        vm.executeBulkAction()

        val state = vm.uiState.value
        assertEquals(mapOf("a1" to SwipeDecision.DELETE, "a2" to SwipeDecision.DELETE), state.decisions)
        assertEquals(2, state.currentIndex)
        assertFalse(state.isBulkDeleteMode)
        assertTrue(state.bulkSelection.isEmpty())
        assertNull(state.bulkLastIndex)
        coVerify { swipeDecisionRepository.saveDecision("a1", "album", "u1", "DELETE", 100L) }
        coVerify { swipeDecisionRepository.saveDecision("a2", "album", "u1", "DELETE", 200L) }
    }

    @Test
    fun `bulk keep marks the selection as keep`() {
        val vm = openAlbum()
        vm.enterBulkMode(isDelete = false)
        vm.setBulkSelection(setOf("a3"), last = 2)
        vm.executeBulkAction()

        assertEquals(mapOf("a3" to SwipeDecision.KEEP), vm.decisions())
        assertEquals(3, vm.uiState.value.currentIndex)
    }

    @Test
    fun `an empty bulk selection just leaves bulk mode`() {
        val vm = openAlbum()
        vm.enterBulkMode(isDelete = true)
        vm.executeBulkAction()

        assertFalse(vm.uiState.value.isBulkDeleteMode)
        assertTrue(vm.decisions().isEmpty())
        coVerify(exactly = 0) { swipeDecisionRepository.saveDecision(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `leaving bulk mode drops the selection`() {
        val vm = openAlbum()
        vm.enterBulkMode(isDelete = false)
        vm.setBulkSelection(setOf("a1"))
        vm.exitBulkMode()

        assertFalse(vm.uiState.value.isBulkKeepMode)
        assertTrue(vm.uiState.value.bulkSelection.isEmpty())
    }

    // --- dialogs and requests ---

    @Test
    fun `the add to album dialog lists real albums only`() {
        val real = Album("r1", "Pets", assetCount = 3, albumThumbnailAssetId = null)
        val skip = Album("s", "SKIP", assetCount = 0, albumThumbnailAssetId = null)
        coEvery { albumRepository.getAlbumsRaw() } returns listOf(real, skip)
        val vm = openAlbum()

        vm.openAddToAlbumDialog()

        assertTrue(vm.uiState.value.showAddToAlbumDialog)
        assertEquals(listOf(real), vm.uiState.value.albumsForAddToAlbum)
        assertFalse(vm.uiState.value.isFetchingAlbumsForDialog)

        vm.dismissAddToAlbumDialog()
        assertFalse(vm.uiState.value.showAddToAlbumDialog)
    }

    @Test
    fun `the add to album dialog survives a failing album request`() {
        coEvery { albumRepository.getAlbumsRaw() } throws IOException("offline")
        val vm = openAlbum()

        vm.openAddToAlbumDialog()

        assertTrue(vm.uiState.value.showAddToAlbumDialog)
        assertFalse(vm.uiState.value.isFetchingAlbumsForDialog)
    }

    @Test
    fun `adding the current asset to an album reports success`() {
        val target = Album("r1", "Pets", assetCount = 3, albumThumbnailAssetId = null)
        coEvery { albumRepository.addAssetToAlbum("r1", "a1") } returns Result.success(Unit)
        val vm = openAlbum()
        var result: Pair<Boolean, String>? = null

        vm.addCurrentAssetToAlbum(target) { ok, name -> result = ok to name }

        assertEquals(true to "Pets", result)
        coVerify { assetRepository.deleteAlbumAssets(Album.VIRTUAL_ORPHANS_ID, "u1", listOf("a1")) }
    }

    @Test
    fun `a rejected album add reports failure and changes nothing`() {
        val target = Album("r1", "Pets", assetCount = 3, albumThumbnailAssetId = null)
        coEvery { albumRepository.addAssetToAlbum(any(), any()) } returns Result.failure(IOException("403"))
        val vm = openAlbum()
        var result: Pair<Boolean, String>? = null

        vm.addCurrentAssetToAlbum(target) { ok, name -> result = ok to name }

        assertEquals(false to "Pets", result)
        coVerify(exactly = 0) { assetRepository.deleteAlbumAssets(any(), any(), any()) }
    }

    @Test
    fun `a crashing album add reports failure`() {
        val target = Album("r1", "Pets", assetCount = 3, albumThumbnailAssetId = null)
        coEvery { albumRepository.addAssetToAlbum(any(), any()) } throws IllegalStateException("boom")
        val vm = openAlbum()
        var result: Pair<Boolean, String>? = null

        vm.addCurrentAssetToAlbum(target) { ok, name -> result = ok to name }

        assertEquals(false to "Pets", result)
    }

    @Test
    fun `download and share requests are delivered to the collector`() = runBlocking {
        val vm = openAlbum()
        vm.downloadAsset(a2)
        vm.shareAsset(a3)

        assertSame(a2, vm.downloadRequestSignal.first())
        assertSame(a3, vm.shareRequestSignal.first())
    }

    @Test
    fun `simple toggles and the tutorial flag`() {
        val vm = openAlbum()
        vm.toggleSummary(true)
        vm.toggleFullscreen(true)
        vm.toggleResetConfirmation(true)
        vm.toggleMute()
        vm.completeSwipeTutorial()

        val state = vm.uiState.value
        assertTrue(state.showSummary && state.isFullscreenMode && state.showResetConfirmation && state.isMuted)
        coVerify { sessionRepository.setHasCompletedSwipeTutorial(true) }

        vm.toggleMute()
        assertFalse(vm.uiState.value.isMuted)
    }

    // --- reset ---

    @Test
    fun `resetting the album removes every decision and starts over`() {
        storedDecisions.value = listOf(entity("a1", "KEEP"), entity("a2", "DELETE"))
        val vm = openAlbum()
        vm.toggleResetConfirmation(true)

        vm.resetAlbumDecisions()

        coVerify { swipeDecisionRepository.deleteDecisionsForAlbum("album", "u1") }
        coVerify { swipeDecisionRepository.removeDecisions(listOf("a1", "a2", "a3", "a4"), "u1") }
        val state = vm.await { it.decisions.isEmpty() && !it.showResetConfirmation }
        assertEquals(0, state.currentIndex)
        assertTrue(state.history.isEmpty())
    }

    // --- applying changes ---

    private fun stubApply(result: ApplyResult) = slot<Map<String, SwipeDecision>>().also { decisions ->
        coEvery {
            applyUseCase(any(), any(), capture(decisions), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns result
    }

    @Test
    fun `applying sends the decisions and removes deleted assets from the list`() {
        val vm = openAlbum()
        vm.onSwipe(SwipeDecision.DELETE)
        vm.onSwipe(SwipeDecision.KEEP)
        val sent = stubApply(applyResult(toDelete = listOf("a1"), deletedAssets = listOf(a1)))

        vm.applyChanges()

        assertEquals(mapOf("a1" to SwipeDecision.DELETE, "a2" to SwipeDecision.KEEP), sent.captured)
        val state = vm.await { "a1" !in it.assets.map { a -> a.id } }
        assertEquals(listOf("a2", "a3", "a4"), state.assets.map { it.id })
        assertEquals(mapOf("a2" to SwipeDecision.KEEP), state.decisions)
        assertEquals(3, state.remoteTotalCount)
        assertTrue(state.history.isEmpty())
        assertFalse(state.isSyncing)
        assertFalse(state.showSummary)
    }

    @Test
    fun `a clean apply shows the success animation for two seconds`() {
        val vm = openAlbum()
        vm.onSwipe(SwipeDecision.KEEP)
        stubApply(applyResult())

        vm.applyChanges()
        assertTrue(vm.uiState.value.showSuccessAnimation)

        dispatcher.scheduler.advanceTimeBy(2_001)
        assertFalse(vm.uiState.value.showSuccessAnimation)
    }

    @Test
    fun `an apply with failed steps raises the sync issue signal and no animation`() {
        val vm = openAlbum()
        vm.onSwipe(SwipeDecision.ARCHIVE)
        stubApply(applyResult(failed = setOf(ApplySwipeDecisionsUseCase.SyncStep.entries.first())))

        vm.applyChanges()

        assertFalse(vm.uiState.value.showSuccessAnimation)
        assertEquals(Unit, runBlocking { withTimeout(5_000.milliseconds) { vm.syncIssueSignal.first() } })
    }

    @Test
    fun `favourites that failed to sync are kept for the next try`() {
        val vm = openAlbum()
        vm.toggleFavoriteFor("a1")
        vm.toggleFavoriteFor("a2")
        stubApply(applyResult(failedFavoriteIds = setOf("a2")))

        vm.applyChanges()

        assertEquals(mapOf("a2" to true), vm.uiState.value.localFavorites)
    }

    @Test
    fun `a crash while applying is reported and stops the spinner`() {
        val vm = openAlbum()
        coEvery {
            applyUseCase(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } throws IOException("server down")

        vm.applyChanges()

        assertEquals("server down", vm.uiState.value.error)
        assertFalse(vm.uiState.value.isSyncing)
    }

    @Test
    fun `deleted assets are also removed from the device when that is enabled`() {
        val uris = listOf(mockk<Uri>())
        val intent = mockk<PendingIntent>()
        coEvery { localMediaRepository.findLocalMediaUris(listOf(a1)) } returns uris
        every { localMediaRepository.createDeletePendingIntent(uris, trash = false) } returns intent
        val vm = openAlbum()
        swipeSettings.value = settings(syncLocalDeletion = true, trashLocalDeletion = false)
        vm.onSwipe(SwipeDecision.DELETE)
        stubApply(applyResult(toDelete = listOf("a1"), deletedAssets = listOf(a1)))

        vm.applyChanges()

        assertSame(intent, vm.uiState.value.localDeletePendingIntent)
        vm.onLocalDeleteIntentHandled()
        assertNull(vm.uiState.value.localDeletePendingIntent)
    }

    @Test
    fun `local deletion is left alone when it is switched off`() {
        val vm = openAlbum()
        vm.onSwipe(SwipeDecision.DELETE)
        stubApply(applyResult(toDelete = listOf("a1"), deletedAssets = listOf(a1)))

        vm.applyChanges()

        coVerify(exactly = 0) { localMediaRepository.findLocalMediaUris(any()) }
        assertNull(vm.uiState.value.localDeletePendingIntent)
    }

    @Test
    fun `a failure while finding local files does not fail the apply`() {
        coEvery { localMediaRepository.findLocalMediaUris(any()) } throws SecurityException("no permission")
        val vm = openAlbum()
        swipeSettings.value = settings(syncLocalDeletion = true)
        vm.onSwipe(SwipeDecision.DELETE)
        stubApply(applyResult(toDelete = listOf("a1"), deletedAssets = listOf(a1)))

        vm.applyChanges()

        assertNull(vm.uiState.value.error)
        assertNull(vm.uiState.value.localDeletePendingIntent)
        assertFalse(vm.uiState.value.isSyncing)
    }
}
