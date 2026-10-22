package com.markvoronin.immichswipe.data.repository

import com.markvoronin.immichswipe.data.api.DuplicateCluster
import com.markvoronin.immichswipe.data.api.ImmichApi
import com.markvoronin.immichswipe.data.api.SearchAssetsRequest
import com.markvoronin.immichswipe.data.api.SearchStatisticsResponse
import com.markvoronin.immichswipe.domain.model.Asset
import com.markvoronin.immichswipe.testutil.RecordedCall
import com.markvoronin.immichswipe.testutil.fake
import com.markvoronin.immichswipe.testutil.testSessionManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class AssetRepositoryCountsTest {

    private fun asset(id: String) = Asset(id = id, ownerId = "u", fileCreatedAt = "", type = "IMAGE")

    private fun repo(
        calls: MutableList<RecordedCall> = mutableListOf(),
        handler: (name: String, args: List<Any?>) -> Any?
    ): AssetRepository {
        val api = fake<ImmichApi>(calls) { name, args -> handler(name, args) }
        return AssetRepository(testSessionManager(), api, null)
    }

    private val statistics = { name: String, args: List<Any?> ->
        when (name) {
            "getSearchStatistics" -> when ((args[0] as SearchAssetsRequest).visibility) {
                "timeline" -> SearchStatisticsResponse(10)
                "archive" -> SearchStatisticsResponse(3)
                else -> SearchStatisticsResponse(0)
            }
            else -> Unit
        }
    }

    @Test
    fun `total count without archived uses the timeline only`() = runBlocking {
        val calls = mutableListOf<RecordedCall>()
        assertEquals(10, repo(calls, statistics).getTotalAssetCount(includeArchived = false).getOrThrow())
        assertEquals(1, calls.count { it.name == "getSearchStatistics" })
    }

    @Test
    fun `total count with archived adds the archive`() = runBlocking {
        assertEquals(13, repo(handler = statistics).getTotalAssetCount(includeArchived = true).getOrThrow())
        assertEquals(13, repo(handler = statistics).getOrphansCount(includeArchived = true).getOrThrow())
    }

    @Test
    fun `a failing server is a failure and not an empty library`() = runBlocking {
        val broken = repo { _, _ -> throw java.io.IOException("offline") }
        assertTrue(broken.getTotalAssetCount().isFailure)
        assertTrue(broken.getOrphansCount().isFailure)
        assertTrue(broken.getDuplicatesCount().isFailure)
    }

    @Test
    fun `duplicates count adds up the assets of every cluster`() = runBlocking {
        val clusters = listOf(
            DuplicateCluster(listOf(asset("1"), asset("2"))),
            DuplicateCluster(listOf(asset("3"), asset("4"), asset("5")))
        )
        assertEquals(5, repo { name, _ -> if (name == "getDuplicates") clusters else Unit }.getDuplicatesCount().getOrThrow())
    }

    @Test
    fun `cancellation is propagated by the counts`() {
        val cancelled = repo { _, _ -> throw CancellationException("cancelled") }
        try {
            runBlocking { cancelled.getTotalAssetCount() }
            fail("cancellation must not be captured")
        } catch (e: CancellationException) {
            assertEquals("cancelled", e.message)
        }
    }
}
