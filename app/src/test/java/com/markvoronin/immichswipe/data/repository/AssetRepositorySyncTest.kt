package com.markvoronin.immichswipe.data.repository

import com.markvoronin.immichswipe.core.SessionManager
import com.markvoronin.immichswipe.data.api.ImmichApi
import com.markvoronin.immichswipe.data.api.SearchAssetResult
import com.markvoronin.immichswipe.data.api.SearchAssetsRequest
import com.markvoronin.immichswipe.data.api.SearchResponse
import com.markvoronin.immichswipe.data.api.SearchStatisticsResponse
import com.markvoronin.immichswipe.data.local.dao.AlbumAssetDao
import com.markvoronin.immichswipe.data.local.entity.AlbumAssetEntity
import com.markvoronin.immichswipe.domain.model.Album
import com.markvoronin.immichswipe.domain.model.Asset
import com.markvoronin.immichswipe.testutil.fake
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class AssetRepositorySyncTest {

    private fun asset(id: String) = Asset(id = id, ownerId = "u", fileCreatedAt = "", type = "IMAGE")

    @Test
    fun `assets removed locally while a sync is paging are not brought back by it`() = runBlocking {
        // The local cache already holds a1; the server has a1 on page 1 and a2, a3 on page 2.
        val db = linkedMapOf("a1" to AlbumAssetEntity(Album.VIRTUAL_ALL_ID, "a1", "u"))
        @Suppress("UNCHECKED_CAST")
        val dao = fake<AlbumAssetDao> { name, args ->
            when (name) {
                "getAssetsChronologicalDesc" -> db.values.toList()
                "getAssetCountForAlbum" -> db.size
                "insertAlbumAssets" -> (args[0] as List<AlbumAssetEntity>).forEach { db[it.assetId] = it }
                "deleteAssets" -> (args[0] as List<String>).forEach { db.remove(it) }
                "deleteAlbumAssets" -> (args[2] as List<String>).forEach { db.remove(it) }
                else -> Unit
            }
        }
        val sessionManager = mockk<SessionManager> { every { getUserId() } returns "u" }
        lateinit var repo: AssetRepository
        val api = fake<ImmichApi> { name, args ->
            when (name) {
                "getSearchStatistics" -> SearchStatisticsResponse(3)
                "searchAssets" -> if ((args[0] as SearchAssetsRequest).page == 1) {
                    SearchResponse(SearchAssetResult(listOf(asset("a1")), 3, nextPage = "2"))
                } else {
                    // a1 and a3 are deleted while page 2 is in flight: its response predates the delete.
                    runBlocking { repo.deleteLocalAssets(listOf("a1", "a3")) }
                    SearchResponse(SearchAssetResult(listOf(asset("a2"), asset("a3")), 3))
                }
                else -> Unit
            }
        }
        repo = AssetRepository(sessionManager, api, dao)

        val batches = repo.getAssetsByAlbum(Album.VIRTUAL_ALL_ID, userId = "u").toList()

        // Cache snapshot, page 1 (before the delete), page 2, then the final batch read back from the cache.
        assertEquals(
            listOf(listOf("a1"), listOf("a1"), listOf("a2"), listOf("a2")),
            batches.map { batch -> batch.assets.map { it.id } }
        )
        assertEquals(listOf("a2"), db.keys.toList())
    }
}
