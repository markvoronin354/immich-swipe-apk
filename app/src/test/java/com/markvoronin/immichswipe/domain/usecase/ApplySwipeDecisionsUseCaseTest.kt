package com.markvoronin.immichswipe.domain.usecase

import android.content.ContextWrapper
import com.markvoronin.immichswipe.core.SessionManager
import com.markvoronin.immichswipe.data.api.ImmichApi
import com.markvoronin.immichswipe.data.local.dao.AlbumAssetDao
import com.markvoronin.immichswipe.data.local.dao.SwipeDecisionDao
import com.markvoronin.immichswipe.data.local.entity.SyncHistoryEntity
import com.markvoronin.immichswipe.data.repository.AlbumRepository
import com.markvoronin.immichswipe.data.repository.AssetRepository
import com.markvoronin.immichswipe.data.repository.SwipeDecisionRepository
import com.markvoronin.immichswipe.domain.model.Album
import com.markvoronin.immichswipe.domain.model.Asset
import com.markvoronin.immichswipe.domain.usecase.ApplySwipeDecisionsUseCase.SyncStep
import com.markvoronin.immichswipe.feature.swipe.SwipeDecision
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.lang.reflect.Proxy

/** Builds a recording stand-in for an interface. [handler] may override the default result. */
private inline fun <reified T : Any> fake(
    calls: MutableList<Call>,
    crossinline handler: (name: String, args: List<Any?>) -> Any? = { _, _ -> Unit }
): T = Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, rawArgs ->
    val args = (rawArgs ?: emptyArray()).toList()
    val real = if (args.lastOrNull() is kotlin.coroutines.Continuation<*>) args.dropLast(1) else args
    calls += Call(method.name, real)
    handler(method.name, real)
} as T


private class Call(val name: String, val args: List<Any?>)

/**
 * Exercises the use case against fake server and database layers, focusing on what happens when a
 * server step fails: the affected items must stay pending and must not be reported as done.
 */
class ApplySwipeDecisionsUseCaseTest {

    private class Env(
        failingApi: Set<String> = emptySet(),
        albums: List<Album> = emptyList(),
        addToAlbumResponse: Response<Unit> = Response.success(Unit)
    ) {
        val api = mutableListOf<Call>()
        val decisionDao = mutableListOf<Call>()
        val albumAssetDao = mutableListOf<Call>()

        val useCase: ApplySwipeDecisionsUseCase

        init {
            val fakeApi = fake<ImmichApi>(api) { name, _ ->
                when {
                    name in failingApi -> throw RuntimeException("$name failed")
                    name == "getAlbums" -> albums
                    name == "addAssetsToAlbum" || name == "removeAssetsFromAlbum" -> addToAlbumResponse
                    name == "createAlbum" -> throw RuntimeException("not scripted")
                    else -> Unit
                }
            }
            val sessionManager = SessionManager(ContextWrapper(null))
            val assets = AssetRepository(sessionManager, fakeApi, fake<AlbumAssetDao>(albumAssetDao))
            val albumsRepo = AlbumRepository(sessionManager, fakeApi)
            val decisions = SwipeDecisionRepository(fake<SwipeDecisionDao>(decisionDao))
            useCase = ApplySwipeDecisionsUseCase(assets, decisions, albumsRepo)
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
        skipFeatureEnabled: Boolean = true
    ) = runBlocking {
        env.useCase(
            userId = "u",
            currentAssets = assets,
            decisions = decisions,
            localRotations = emptyMap(),
            localFavorites = favorites,
            assetsEditedThisSession = emptySet(),
            deletedCount = decisions.values.count { it == SwipeDecision.DELETE },
            deletedSize = 1000L,
            keptCount = decisions.values.count { it == SwipeDecision.KEEP },
            archiveCount = decisions.values.count { it == SwipeDecision.ARCHIVE },
            lockedCount = decisions.values.count { it == SwipeDecision.LOCK },
            skipFeatureEnabled = skipFeatureEnabled
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
        assertTrue("keep" in env.syncedIds())
        // Deleted fine, so it is recorded; kept is deferred until the retry.
        val history = env.history()
        assertEquals(1, history.deletedCount)
        assertEquals(0, history.archivedCount)
        assertEquals(0, history.keptCount)
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
    fun `skip album lookup failure is reported and the skipped asset stays pending`() {
        val env = Env(failingApi = setOf("getAlbums"))
        val result = apply(env, mapOf("skip" to SwipeDecision.SKIP))

        assertTrue(SyncStep.SKIP_ALBUM in result.failedSteps)
        assertFalse("skip" in env.syncedIds())
    }

    @Test
    fun `skip album rejected by the server is reported`() {
        val skipAlbum = Album(id = "skipId", albumName = "SKIP", assetCount = 0, albumThumbnailAssetId = null)
        val rejected = Response.error<Unit>(500, "".toResponseBody())
        val env = Env(albums = listOf(skipAlbum), addToAlbumResponse = rejected)
        val result = apply(env, mapOf("skip" to SwipeDecision.SKIP))

        // The album repository swallows the error and returns false: it must still be surfaced.
        assertTrue(SyncStep.SKIP_ALBUM in result.failedSteps)
        assertFalse("skip" in env.syncedIds())
    }

    @Test
    fun `with the skip feature disabled no skip album request is ever made`() {
        // getAlbums would fail, but it must not even be called.
        val env = Env(failingApi = setOf("getAlbums"))
        val result = apply(env, mixed + ("skip" to SwipeDecision.SKIP), skipFeatureEnabled = false)

        assertFalse(result.hasFailures)
        assertTrue(env.apiCalls("getAlbums").isEmpty())
        assertTrue(env.apiCalls("createAlbum").isEmpty())
        assertTrue(env.apiCalls("addAssetsToAlbum").isEmpty())
        assertTrue(env.apiCalls("removeAssetsFromAlbum").isEmpty())
    }

    @Test
    fun `the skip feature flag defaults to the app wide switch`() {
        // Guards the shipped default: the feature must stay off unless it is deliberately enabled.
        assertFalse(Album.IS_SKIP_FEATURE_ENABLED)
    }
}
