package com.markvoronin.immichswipe.feature.duplicates

import com.markvoronin.immichswipe.core.SessionConfig
import com.markvoronin.immichswipe.core.SessionManager
import com.markvoronin.immichswipe.data.api.DeleteAssetsRequest
import com.markvoronin.immichswipe.data.api.DuplicateCluster
import com.markvoronin.immichswipe.data.api.ImmichApi
import com.markvoronin.immichswipe.data.api.UpdateAssetsRequest
import com.markvoronin.immichswipe.data.local.entity.SwipeDecisionEntity
import com.markvoronin.immichswipe.data.repository.SessionRepository
import com.markvoronin.immichswipe.data.repository.SwipeDecisionRepository
import com.markvoronin.immichswipe.domain.model.Album
import com.markvoronin.immichswipe.domain.model.Asset
import com.markvoronin.immichswipe.domain.model.ExifInfo
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
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
class DuplicatesViewModelTest {

    private fun asset(
        id: String,
        size: Long? = null,
        type: String = "IMAGE",
        favorite: Boolean = false,
        created: String = "2024-01-01T00:00:00Z",
        width: Int? = null,
        height: Int? = null
    ) = Asset(
        id = id, ownerId = "o", fileCreatedAt = created, type = type, isFavorite = favorite,
        exifInfo = ExifInfo(fileSizeInBytes = size, imageWidth = width, imageHeight = height)
    )

    private val small = asset("small", size = 100)
    private val medium = asset("medium", size = 200)
    private val large = asset("large", size = 300)
    private val video = asset("video", size = 5000, type = "VIDEO")
    private val lone1 = asset("lone1", size = 10)
    private val lone2 = asset("lone2", size = 20)

    private var clusters = listOf(
        DuplicateCluster(listOf(lone1, lone2)),
        DuplicateCluster(listOf(small, large, medium))
    )

    private val api = mockk<ImmichApi>(relaxed = true)
    private val sessionConfig = MutableStateFlow<SessionConfig?>(SessionConfig("https://photos.example.com", "key", "u1"))
    private val storedDecisions = MutableStateFlow<List<SwipeDecisionEntity>>(emptyList())
    private val tutorialDone = MutableStateFlow(false)
    private val skipDeepAnalyzeConfirmation = MutableStateFlow(false)

    private val swipeDecisionRepository = mockk<SwipeDecisionRepository>(relaxed = true) {
        every { getAllDecisionsForUser("u1") } returns storedDecisions
    }
    private val sessionRepository = mockk<SessionRepository>(relaxed = true) {
        every { hasCompletedDuplicatesTutorial } returns tutorialDone
        every { this@mockk.skipDeepAnalyzeConfirmation } returns this@DuplicatesViewModelTest.skipDeepAnalyzeConfirmation
    }
    private val sessionManager = mockk<SessionManager>(relaxed = true) {
        every { this@mockk.api } returns this@DuplicatesViewModelTest.api
        every { this@mockk.sessionConfig } returns this@DuplicatesViewModelTest.sessionConfig
        every { getUserId() } returns "u1"
    }
    private val bestShotAnalyzer = mockk<BestShotAnalyzer>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        coEvery { api.getDuplicates() } answers { clusters }
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun createViewModel() =
        DuplicatesViewModel(swipeDecisionRepository, sessionRepository, sessionManager, bestShotAnalyzer)

    private fun DuplicatesViewModel.assetIds() = uiState.value.clusters.map { c -> c.assets.map { it.id } }

    private fun DuplicatesViewModel.decisions() = uiState.value.decisions

    private fun entity(id: String, decision: String) =
        SwipeDecisionEntity(assetId = id, albumId = Album.VIRTUAL_DUPLICATES_ID, userId = "u1", decision = decision)

    // --- loading ---

    @Test
    fun `clusters are loaded biggest first with the largest file first inside each`() {
        val vm = createViewModel()

        assertEquals(listOf(listOf("large", "medium", "small"), listOf("lone2", "lone1")), vm.assetIds())
        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `a failing request is shown as an error`() {
        coEvery { api.getDuplicates() } throws IOException("offline")
        val state = createViewModel().uiState.value

        assertEquals("offline", state.error)
        assertFalse(state.isLoading)
        assertTrue(state.clusters.isEmpty())
    }

    @Test
    fun `without a session the load reports an error instead of crashing`() {
        every { sessionManager.api } returns null
        val state = createViewModel().uiState.value

        assertEquals("No active API session", state.error)
        assertFalse(state.isLoading)
    }

    @Test
    fun `reloading clears an earlier error`() {
        coEvery { api.getDuplicates() } throws IOException("offline")
        val vm = createViewModel()
        coEvery { api.getDuplicates() } answers { clusters }

        vm.loadDuplicates()

        assertNull(vm.uiState.value.error)
        assertEquals(2, vm.uiState.value.clusters.size)
    }

    @Test
    fun `session and tutorial flags are mirrored into the state`() {
        val vm = createViewModel()
        assertEquals("https://photos.example.com", vm.uiState.value.baseUrl)
        assertFalse(vm.uiState.value.hasCompletedDuplicatesTutorial)

        vm.completeDuplicatesTutorial()
        coVerify { sessionRepository.setHasCompletedDuplicatesTutorial(true) }

        sessionConfig.value = null
        assertEquals("", vm.uiState.value.baseUrl)
    }

    @Test
    fun `saved keep and delete decisions are restored and other ones ignored`() {
        storedDecisions.value = listOf(entity("small", "KEEP"), entity("large", "DELETE"), entity("medium", "SKIP"))
        val vm = createViewModel()

        assertEquals(mapOf("small" to DuplicateDecision.KEEP, "large" to DuplicateDecision.DELETE), vm.decisions())
    }

    @Test
    fun `without a user no decisions are restored`() {
        sessionConfig.value = null
        storedDecisions.value = listOf(entity("small", "KEEP"))

        assertTrue(createViewModel().decisions().isEmpty())
    }

    // --- sorting ---

    @Test
    fun `photos first and videos first reorder the clusters`() {
        clusters = listOf(
            DuplicateCluster(listOf(video, asset("v2", size = 1, type = "VIDEO"))),
            DuplicateCluster(listOf(small, medium)),
            DuplicateCluster(listOf(lone1, video.copy(id = "mixedVideo")))
        )
        val vm = createViewModel()

        vm.setSortOrder(DuplicatesSortOrder.PHOTOS_FIRST)
        assertEquals(DuplicatesSortOrder.PHOTOS_FIRST, vm.uiState.value.sortOrder)
        // Two photos, then one photo plus a video, then no photo at all.
        assertEquals(listOf("medium", "small"), vm.assetIds()[0])
        assertEquals(listOf("lone1", "mixedVideo"), vm.assetIds()[1])

        vm.setSortOrder(DuplicatesSortOrder.VIDEOS_FIRST)
        assertEquals(listOf("video", "v2"), vm.assetIds()[0])
        assertEquals(listOf("mixedVideo", "lone1"), vm.assetIds()[1])
    }

    // --- decisions ---

    @Test
    fun `tapping an asset cycles keep, delete, none and saves each step`() {
        val vm = createViewModel()

        vm.toggleDecision("large")
        assertEquals(DuplicateDecision.KEEP, vm.decisions()["large"])
        coVerify { swipeDecisionRepository.saveDecision("large", Album.VIRTUAL_DUPLICATES_ID, "u1", "KEEP", 300L) }

        vm.toggleDecision("large")
        assertEquals(DuplicateDecision.DELETE, vm.decisions()["large"])
        coVerify { swipeDecisionRepository.saveDecision("large", Album.VIRTUAL_DUPLICATES_ID, "u1", "DELETE", 300L) }

        vm.toggleDecision("large")
        assertEquals(DuplicateDecision.NONE, vm.decisions()["large"])
        coVerify { swipeDecisionRepository.removeDecision("large", "u1") }
    }

    @Test
    fun `decisions are not stored without a user but still change on screen`() {
        every { sessionManager.getUserId() } returns null
        val vm = createViewModel()

        vm.toggleDecision("large")

        assertEquals(DuplicateDecision.KEEP, vm.decisions()["large"])
        coVerify(exactly = 0) { swipeDecisionRepository.saveDecision(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `a cluster action sets every asset and repeating it clears them`() {
        val vm = createViewModel()
        val cluster = vm.uiState.value.clusters[0]
        val saved = slot<List<SwipeDecisionEntity>>()
        coEvery { swipeDecisionRepository.saveDecisions(capture(saved)) } returns Unit

        vm.setClusterDecisions(cluster, DuplicateDecision.DELETE)
        assertEquals(setOf(DuplicateDecision.DELETE), vm.decisions().values.toSet())
        assertEquals(setOf("large", "medium", "small"), vm.decisions().keys)
        assertEquals(listOf("DELETE", "DELETE", "DELETE"), saved.captured.map { it.decision })
        assertEquals(listOf(300L, 200L, 100L), saved.captured.map { it.fileSize })

        vm.setClusterDecisions(cluster, DuplicateDecision.DELETE)
        assertTrue(vm.decisions().isEmpty())
        coVerify { swipeDecisionRepository.removeDecisions(listOf("large", "medium", "small"), "u1") }
    }

    @Test
    fun `a cluster action on a partly decided cluster applies to all of it`() {
        val vm = createViewModel()
        val cluster = vm.uiState.value.clusters[0]
        vm.toggleDecision("large") // KEEP on one asset

        vm.setClusterDecisions(cluster, DuplicateDecision.KEEP)

        assertEquals(setOf(DuplicateDecision.KEEP), vm.decisions().values.toSet())
        assertEquals(3, vm.decisions().size)
    }

    @Test
    fun `a cluster action on an empty cluster does nothing`() {
        val vm = createViewModel()
        vm.setClusterDecisions(DuplicateClusterUiModel("empty", emptyList()), DuplicateDecision.KEEP)

        assertTrue(vm.decisions().isEmpty())
        coVerify(exactly = 0) { swipeDecisionRepository.saveDecisions(any()) }
    }

    @Test
    fun `clearing removes the decisions on screen and in storage`() {
        val vm = createViewModel()
        vm.toggleDecision("large")
        vm.toggleDecision("small")

        vm.clearAllDecisions()

        assertTrue(vm.decisions().isEmpty())
        assertNull(vm.uiState.value.selectedAutoSelectMode)
        coVerify { swipeDecisionRepository.removeDecisions(listOf("large", "small"), "u1") }
    }

    @Test
    fun `clearing with nothing decided touches no storage`() {
        createViewModel().resetDecisions()
        coVerify(exactly = 0) { swipeDecisionRepository.removeDecisions(any(), any()) }
    }

    // --- favourites ---

    @Test
    fun `favouriting keeps the asset and tells the server`() {
        val vm = createViewModel()
        vm.toggleFavorite(large)

        assertTrue(vm.uiState.value.isFavorite(large))
        assertEquals(DuplicateDecision.KEEP, vm.decisions()["large"])
        coVerify { swipeDecisionRepository.saveDecision("large", Album.VIRTUAL_DUPLICATES_ID, "u1", "KEEP", 300L) }
        coVerify { api.updateAssets(UpdateAssetsRequest(ids = listOf("large"), isFavorite = true)) }
    }

    @Test
    fun `removing a favourite does not record a decision`() {
        val favourite = large.copy(isFavorite = true)
        val vm = createViewModel()
        vm.toggleFavorite(favourite)

        assertFalse(vm.uiState.value.isFavorite(favourite))
        assertNull(vm.decisions()["large"])
        coVerify { api.updateAssets(UpdateAssetsRequest(ids = listOf("large"), isFavorite = false)) }
        coVerify(exactly = 0) { swipeDecisionRepository.saveDecision(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `a failed favourite request puts the star back`() {
        coEvery { api.updateAssets(any()) } throws IOException("offline")
        val vm = createViewModel()

        vm.toggleFavorite(large)

        assertFalse(vm.uiState.value.isFavorite(large))
    }

    // --- auto select ---

    @Test
    fun `keep largest keeps the biggest photo of each cluster and deletes the rest`() {
        clusters = listOf(DuplicateCluster(listOf(small, large, video, medium)), DuplicateCluster(listOf(lone1, lone2)))
        val vm = createViewModel()
        val saved = slot<List<SwipeDecisionEntity>>()
        coEvery { swipeDecisionRepository.saveDecisions(capture(saved)) } returns Unit

        vm.autoSelect(keepLargest = true)

        assertEquals(
            mapOf(
                "large" to DuplicateDecision.KEEP, "medium" to DuplicateDecision.DELETE,
                "small" to DuplicateDecision.DELETE,
                "lone2" to DuplicateDecision.KEEP, "lone1" to DuplicateDecision.DELETE
            ),
            vm.decisions()
        )
        // Videos are never decided automatically.
        assertNull(vm.decisions()["video"])
        assertEquals(AutoSelectMode.KEEP_LARGEST, vm.uiState.value.selectedAutoSelectMode)
        assertEquals(5, saved.captured.size)
        assertTrue(saved.captured.all { it.albumId == Album.VIRTUAL_DUPLICATES_ID && it.userId == "u1" })
    }

    @Test
    fun `keep smallest keeps the smallest known size`() {
        val unknownSize = asset("unknown", size = null)
        clusters = listOf(DuplicateCluster(listOf(medium, unknownSize, small)))
        val vm = createViewModel()

        vm.autoSelect(keepLargest = false)

        assertEquals(DuplicateDecision.KEEP, vm.decisions()["small"])
        assertEquals(DuplicateDecision.DELETE, vm.decisions()["medium"])
        assertEquals(DuplicateDecision.DELETE, vm.decisions()["unknown"])
        assertEquals(AutoSelectMode.KEEP_SMALLEST, vm.uiState.value.selectedAutoSelectMode)
    }

    @Test
    fun `auto select always keeps favourites`() {
        clusters = listOf(DuplicateCluster(listOf(small.copy(isFavorite = true), large)))
        val vm = createViewModel()

        vm.autoSelect(keepLargest = true)

        assertEquals(DuplicateDecision.KEEP, vm.decisions()["small"])
        assertEquals(DuplicateDecision.KEEP, vm.decisions()["large"])
    }

    @Test
    fun `best shot keeps what the analyzer picks and skips videos`() {
        clusters = listOf(DuplicateCluster(listOf(small, large, video)))
        val photos = slot<List<Asset>>()
        coEvery { bestShotAnalyzer.analyzeCluster(capture(photos), any()) } returns emptyList()
        every { bestShotAnalyzer.selectKeptAssetIds(any(), BestShotAnalyzer.BestShotMode.TOP_2) } returns setOf("small")
        val vm = createViewModel()

        vm.autoSelectBestShot(BestShotAnalyzer.BestShotMode.TOP_2)

        assertEquals(setOf("small", "large"), photos.captured.map { it.id }.toSet())
        assertEquals(
            mapOf("small" to DuplicateDecision.KEEP, "large" to DuplicateDecision.DELETE),
            vm.decisions()
        )
        assertEquals(AutoSelectMode.KEEP_BEST_SHOT_TOP_2, vm.uiState.value.selectedAutoSelectMode)
        assertFalse(vm.uiState.value.isAnalyzingAi)
        assertNull(vm.uiState.value.aiProgress)
        coVerify { bestShotAnalyzer.analyzeCluster(any(), "https://photos.example.com") }
    }

    @Test
    fun `skipping the deep analysis confirmation is saved and mirrored into the state`() {
        val vm = createViewModel()
        assertFalse(vm.uiState.value.skipDeepAnalyzeConfirmation)

        vm.skipDeepAnalyzeConfirmation()
        coVerify { sessionRepository.setSkipDeepAnalyzeConfirmation(true) }

        skipDeepAnalyzeConfirmation.value = true
        assertTrue(vm.uiState.value.skipDeepAnalyzeConfirmation)
    }

    @Test
    fun `deep analysis applies the pick to one cluster and skips videos`() {
        clusters = listOf(DuplicateCluster(listOf(small, large, video)))
        val photos = slot<List<Asset>>()
        coEvery { bestShotAnalyzer.analyzeClusterDeep(capture(photos), any()) } returns emptyList()
        every { bestShotAnalyzer.selectKeptAssetIds(any(), BestShotAnalyzer.BestShotMode.SINGLE_BEST) } returns setOf("large")
        val vm = createViewModel()

        vm.deepAnalyzeCluster(vm.uiState.value.clusters.first())

        assertEquals(setOf("small", "large"), photos.captured.map { it.id }.toSet())
        assertEquals(
            mapOf("small" to DuplicateDecision.DELETE, "large" to DuplicateDecision.KEEP),
            vm.decisions()
        )
        assertFalse(vm.uiState.value.isAnalyzingAi)
    }

    @Test
    fun `best shot modes map to their auto select modes`() {
        val expected = mapOf(
            BestShotAnalyzer.BestShotMode.AUTO to AutoSelectMode.KEEP_BEST_SHOT_AUTO,
            BestShotAnalyzer.BestShotMode.SINGLE_BEST to AutoSelectMode.KEEP_BEST_SHOT_SINGLE,
            BestShotAnalyzer.BestShotMode.TOP_2 to AutoSelectMode.KEEP_BEST_SHOT_TOP_2,
            BestShotAnalyzer.BestShotMode.TOP_3 to AutoSelectMode.KEEP_BEST_SHOT_TOP_3
        )
        expected.forEach { (mode, auto) ->
            val vm = createViewModel()
            vm.autoSelectBestShot(mode)
            assertEquals(auto, vm.uiState.value.selectedAutoSelectMode)
        }
    }

    @Test
    fun `best shot keeps everything undecided when the analyzer keeps nothing`() {
        every { bestShotAnalyzer.selectKeptAssetIds(any(), any()) } returns emptySet()
        val vm = createViewModel()

        vm.autoSelectBestShot()

        assertTrue(vm.decisions().isEmpty())
        coVerify(exactly = 0) { swipeDecisionRepository.saveDecisions(any()) }
    }

    @Test
    fun `cancelling a running analysis resets the progress`() {
        coEvery { bestShotAnalyzer.analyzeCluster(any(), any()) } coAnswers { awaitCancellation() }
        val vm = createViewModel()

        vm.autoSelectBestShot()
        assertTrue(vm.uiState.value.isAnalyzingAi)
        assertEquals(1 to 2, vm.uiState.value.aiProgress)

        vm.cancelBestShotAnalysis()
        assertFalse(vm.uiState.value.isAnalyzingAi)
        assertNull(vm.uiState.value.aiProgress)
        assertTrue(vm.decisions().isEmpty())
    }

    // --- sync ---

    @Test
    fun `syncing deletes the marked assets, records the history and reloads`() {
        val vm = createViewModel()
        vm.toggleDecision("large")            // KEEP
        vm.toggleDecision("small")
        vm.toggleDecision("small")            // DELETE
        vm.toggleDecision("medium")
        vm.toggleDecision("medium")           // DELETE

        vm.syncDeletions()

        coVerify { api.deleteAssets(match { it.ids.toSet() == setOf("small", "medium") && !it.force }) }
        coVerify { swipeDecisionRepository.removeDecisions(match { it.toSet() == setOf("small", "medium") }, "u1") }
        coVerify { swipeDecisionRepository.markAsSynced(listOf("large"), "u1") }
        coVerify {
            swipeDecisionRepository.saveSyncHistory(
                userId = "u1", deletedCount = 2, bytesSaved = 300L,
                keptCount = 1, archivedCount = 0, lockedCount = 0
            )
        }
        coVerify(exactly = 2) { api.getDuplicates() }
        assertFalse(vm.uiState.value.isSyncing)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `syncing only kept assets marks them synced without deleting`() {
        val vm = createViewModel()
        vm.toggleDecision("large")

        vm.syncDeletions()

        coVerify(exactly = 0) { api.deleteAssets(any<DeleteAssetsRequest>()) }
        coVerify { swipeDecisionRepository.markAsSynced(listOf("large"), "u1") }
        coVerify(exactly = 0) { swipeDecisionRepository.saveSyncHistory(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `a failing delete reports the error and keeps the decisions`() {
        coEvery { api.deleteAssets(any()) } throws IOException("server down")
        val vm = createViewModel()
        vm.toggleDecision("small")
        vm.toggleDecision("small")

        vm.syncDeletions()

        assertEquals("server down", vm.uiState.value.error)
        assertFalse(vm.uiState.value.isSyncing)
        assertEquals(DuplicateDecision.DELETE, vm.decisions()["small"])
        coVerify(exactly = 0) { swipeDecisionRepository.removeDecisions(any(), any()) }
        coVerify(exactly = 0) { swipeDecisionRepository.saveSyncHistory(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `the delete confirmation is a plain toggle`() {
        val vm = createViewModel()
        vm.toggleDeleteConfirmation(true)
        assertTrue(vm.uiState.value.showDeleteConfirmation)
        vm.toggleDeleteConfirmation(false)
        assertFalse(vm.uiState.value.showDeleteConfirmation)
    }
}
