package com.markvoronin.immichswipe.data.repository

import com.markvoronin.immichswipe.core.AppLogger
import com.markvoronin.immichswipe.core.SessionManager
import com.markvoronin.immichswipe.data.api.AddAssetsToAlbumRequest
import com.markvoronin.immichswipe.data.api.ImmichApi
import com.markvoronin.immichswipe.data.api.SearchAssetsRequest
import com.markvoronin.immichswipe.domain.model.Album
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class AlbumRepository @Inject constructor(
    private val sessionManager: SessionManager,
    private val customApi: ImmichApi? = null
) {
    private val api: ImmichApi
        get() = customApi ?: sessionManager.api ?: error("No active API session")

    suspend fun getAlbumsRaw(): List<Album> {
        return api.getAlbums()
    }


    suspend fun getAlbumArchiveCount(albumId: String): Int {
        return try {
            api.getSearchStatistics(
                SearchAssetsRequest(albumIds = listOf(albumId), visibility = "archive")
            ).total
        } catch (_: Exception) {
            0
        }
    }

    suspend fun refineAlbumCounts(albums: List<Album>): List<Album> {
        val semaphore = Semaphore(15)
        return coroutineScope {
            albums.map { album ->
                async {
                    semaphore.withPermit {
                        val archiveCount = getAlbumArchiveCount(album.id)
                        album.copy(assetCount = (album.assetCount - archiveCount).coerceAtLeast(0))
                    }
                }
            }.awaitAll()
        }
    }




    suspend fun createAlbum(albumName: String, assetIds: List<String>? = null): Album? {
        return try {
            api.createAlbum(com.markvoronin.immichswipe.data.api.CreateAlbumRequest(
                albumName = albumName,
                assetIds = assetIds
            ))
        } catch (e: Exception) {
            AppLogger.e("AlbumRepository", "Error creating album $albumName", e)
            null
        }
    }

    suspend fun addAssetToAlbum(albumId: String, assetId: String): Boolean {
        return addAssetsToAlbum(albumId, listOf(assetId))
    }

    suspend fun addAssetsToAlbum(albumId: String, assetIds: List<String>): Boolean {
        return try {
            val response = api.addAssetsToAlbum(albumId,
                AddAssetsToAlbumRequest(ids = assetIds)
            )
            response.isSuccessful
        } catch (e: Exception) {
            AppLogger.e("AlbumRepository", "Error adding assets to album $albumId", e)
            false
        }
    }

    suspend fun removeAssetsFromAlbum(albumId: String, assetIds: List<String>): Boolean {
        return try {
            val response = api.removeAssetsFromAlbum(albumId,
                AddAssetsToAlbumRequest(ids = assetIds)
            )
            response.isSuccessful
        } catch (e: Exception) {
            AppLogger.e("AlbumRepository", "Error removing assets from album $albumId", e)
            false
        }
    }
}
