package com.markvoronin.immichswipe.domain.usecase

import com.markvoronin.immichswipe.data.api.ImmichApi
import com.markvoronin.immichswipe.data.api.UpdateAssetsRequest
import com.markvoronin.immichswipe.data.local.dao.AlbumAssetDao
import com.markvoronin.immichswipe.data.local.dao.SwipeDecisionDao
import com.markvoronin.immichswipe.data.local.entity.SyncHistoryEntity
import com.markvoronin.immichswipe.data.repository.AssetRepository
import com.markvoronin.immichswipe.data.repository.SwipeDecisionRepository
import com.markvoronin.immichswipe.domain.model.Asset
import com.markvoronin.immichswipe.domain.usecase.ApplySwipeDecisionsUseCase.SyncStep
import com.markvoronin.immichswipe.feature.swipe.SwipeDecision
import com.markvoronin.immichswipe.testutil.RecordedCall
import com.markvoronin.immichswipe.testutil.fake
import com.markvoronin.immichswipe.testutil.testSessionManager
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/**
 * Exercises the use case against fake server and database layers, focusing on what happens when a
 * server step fails: the affected items must stay pending and must not be reported as done.
 */
class ApplySwipeDecisionsUseCaseTest {

    private class Env(
        failingApi: Set<String> = emptySet(),
        failingDecisionDao: Set<String> = emptySet(),
        apiError: (String) -> Exception = { RuntimeException("$it failed") }
    ) {
        val api = mutableListOf<RecordedCall>()
        val decisionDao = mutableListOf<RecordedCall>()
        val albumAssetDao = mutableListOf<RecordedCall>()

        /** What the database reports as not yet synced; the tests set it before applying. */
        var unsyncedIds: List<String> = emptyList()

        val useCase: ApplySwipeDecisionsUseCase

        init {
            val fakeApi = fake<ImmichApi>(api) { name, _ ->
                when (name) {
                    in failingApi -> throw apiError(name)
                    else -> Unit
                }
            }
            val sessionManager = testSessionManager()
            val assets = AssetRepository(sessionManager, fakeApi, fake<AlbumAssetDao>(albumAssetDao))
            val decisions = SwipeDecisionRepository(fake<SwipeDecisionDao>(decisionDao) { name, _ ->
                when (name) {
                    in failingDecisionDao -> throw RuntimeException("$name failed")
                    "getUnsyncedAssetIds" -> unsyncedIds
                    else -> Unit
                }
            })
            useCase = ApplySwipeDecisionsUseCase(assets, decisions)
        }

        fun apiCalls(name: String) = api.filter { it.name == name }
        fun daoCalls(name: String) = decisionDao.filter { it.name == name }
    }

    private fun asset(id: String) = Asset(id = id, ownerId = "u", fileCreatedAt = "", type = "IMAGE")

    private fun apply(
        env: Env,
        decisions: Map<String, SwipeDecision>,
        favorites: Map<String, Boolean> = emptyMap(),
        assets: List<Asset> = decisions.keys.map(::asset),
        unsyncedIds: List<String> = decisions.keys.toList()
    ) = runBlocking {
        env.unsyncedIds = unsyncedIds
        env.useCase(
            userId = "u",
            currentAssets = assets,
            decisions = decisions,
            localRotations = emptyMap(),
            localFavorites = favorites,
            assetsEditedThisSession = emptySet(),
            deletedCount = decisions.values.count { it == SwipeDecision.DELETE },
            deletedSize = 1000L,
            lockedCount = decisions.values.count { it == SwipeDecision.LOCK }
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun Env.syncedIds() = daoCalls("markAsSynced").flatMap { it.args[0] as List<String> }

    private fun Env.history(): SyncHistoryEntity = daoCalls("insertSyncHistory").single().args[0] as SyncHistoryEntity

    private val mixed = mapOf(
        "del" to SwipeDecision.DELETE,
        "arc" to SwipeDecision.ARCHIVE,
        "keep" to SwipeDecision.KEEP
    )

    @Test
    fun `everything succeeds`() {
        val env = Env()
        val result = apply(env, mixed)

        assertFalse(result.hasFailures)
        assertEquals(listOf("del"), result.toDelete)
        assertEquals(listOf("del"), result.deletedAssets.map { it.id })
        assertEquals(1, env.apiCalls("deleteAssets").size)
        assertEquals(1, env.apiCalls("updateAssets").size)
        // Deleted assets are removed locally; archived and kept ones are marked synced.
        assertEquals(setOf("arc", "keep"), env.syncedIds().toSet())
        val history = env.history()
        assertEquals(1, history.deletedCount)
        assertEquals(1, history.archivedCount)
        assertEquals(1, history.keptCount)
    }

    @Test
    fun `failed delete stays pending and is not counted`() {
        val env = Env(failingApi = setOf("deleteAssets"))
        val result = apply(env, mapOf("del" to SwipeDecision.DELETE, "keep" to SwipeDecision.KEEP))

        assertEquals(setOf(SyncStep.DELETE), result.failedSteps)
        assertTrue(result.hasFailures)
        // The asset is not reported as deleted, so the screen keeps it and its decision.
        assertTrue(result.toDelete.isEmpty())
        assertTrue(result.deletedAssets.isEmpty())
        assertTrue(env.daoCalls("deleteDecisions").isEmpty())
        assertFalse("del" in env.syncedIds())
        // Nothing was applied, and kept items are deferred, so no history row is written.
        assertTrue(env.daoCalls("insertSyncHistory").isEmpty())
    }

    @Test
    fun `a 403 from the server is reported as a missing permission`() {
        val env = Env(
            failingApi = setOf("deleteAssets"),
            apiError = { HttpException(Response.error<Unit>(403, "".toResponseBody())) }
        )
        val result = apply(env, mapOf("del" to SwipeDecision.DELETE))

        assertTrue(result.permissionDenied)
        assertTrue(result.hasFailures)
    }

    @Test
    fun `other failures are not reported as a missing permission`() {
        val rejected = Env(
            failingApi = setOf("deleteAssets"),
            apiError = { HttpException(Response.error<Unit>(400, "".toResponseBody())) }
        )
        assertFalse(apply(rejected, mapOf("del" to SwipeDecision.DELETE)).permissionDenied)

        val offline = Env(failingApi = setOf("deleteAssets"))
        assertFalse(apply(offline, mapOf("del" to SwipeDecision.DELETE)).permissionDenied)
    }

    @Test
    fun `a failing step does not stop the others`() {
        val env = Env(failingApi = setOf("deleteAssets"))
        val result = apply(env, mixed)

        assertEquals(setOf(SyncStep.DELETE), result.failedSteps)
        assertEquals("archive still runs", 1, env.apiCalls("updateAssets").size)
        assertTrue("arc" in env.syncedIds())
        // Archive succeeded so it is recorded, but delete and kept are not.
        val history = env.history()
        assertEquals(0, history.deletedCount)
        assertEquals(0, history.keptCount)
        assertEquals(1, history.archivedCount)
    }

    @Test
    fun `failed archive is not marked synced`() {
        val env = Env(failingApi = setOf("updateAssets"))
        val result = apply(env, mixed)

        assertTrue(SyncStep.ARCHIVE in result.failedSteps)
        assertFalse("arc" in env.syncedIds())
        assertFalse("keep" in env.syncedIds())
        // Deleted fine, so it is recorded; kept stays pending so the retry counts it.
        val history = env.history()
        assertEquals(1, history.deletedCount)
        assertEquals(0, history.archivedCount)
        assertEquals(0, history.keptCount)
    }

    @Test
    fun `already synced keep and archive decisions are neither sent nor counted again`() {
        val env = Env()
        val decisions = mapOf(
            "arcOld" to SwipeDecision.ARCHIVE,
            "keepOld" to SwipeDecision.KEEP,
            "arcNew" to SwipeDecision.ARCHIVE,
            "keepNew" to SwipeDecision.KEEP,
            "del" to SwipeDecision.DELETE
        )
        apply(env, decisions, unsyncedIds = listOf("arcNew", "keepNew", "del"))

        val archived = (env.apiCalls("updateAssets").single().args[0] as UpdateAssetsRequest).ids
        assertEquals(listOf("arcNew"), archived)
        val history = env.history()
        assertEquals(1, history.archivedCount)
        assertEquals(1, history.keptCount)
        assertEquals(1, history.deletedCount)
    }

    @Test
    fun `a sync with nothing pending writes no history`() {
        val env = Env()
        apply(env, mapOf("arcOld" to SwipeDecision.ARCHIVE, "keepOld" to SwipeDecision.KEEP), unsyncedIds = emptyList())

        assertTrue(env.apiCalls("updateAssets").isEmpty())
        assertTrue(env.daoCalls("insertSyncHistory").isEmpty())
    }

    @Test
    fun `failed favorites are returned so they can be retried`() {
        val env = Env(failingApi = setOf("updateAssets"))
        val result = apply(env, mapOf("keep" to SwipeDecision.KEEP), favorites = mapOf("f1" to true, "f2" to false))

        assertTrue(SyncStep.FAVORITES in result.failedSteps)
        assertEquals(setOf("f1", "f2"), result.failedFavoriteIds)
    }

    @Test
    fun `successful favorites are not reported as failed`() {
        val env = Env()
        val result = apply(env, mapOf("keep" to SwipeDecision.KEEP), favorites = mapOf("f1" to true))

        assertFalse(result.hasFailures)
        assertTrue(result.failedFavoriteIds.isEmpty())
    }

    @Test
    fun `local cleanup failing after a successful server delete is not a sync failure`() {
        val env = Env(failingDecisionDao = setOf("deleteDecisions"))
        val result = apply(env, mapOf("del" to SwipeDecision.DELETE, "keep" to SwipeDecision.KEEP))

        assertFalse(result.hasFailures)
        // The server delete happened, so the asset counts as deleted and is recorded in the history.
        assertEquals(listOf("del"), result.toDelete)
        assertEquals(1, env.apiCalls("deleteAssets").size)
        assertEquals(1, env.history().deletedCount)
    }
}
