package com.markvoronin.immichswipe.data.repository

import com.markvoronin.immichswipe.core.AppLogger
import com.markvoronin.immichswipe.core.SessionManager
import com.markvoronin.immichswipe.core.util.rethrowIfCancelled
import com.markvoronin.immichswipe.core.util.runCatchingCancellable
import com.markvoronin.immichswipe.data.api.AddAssetsToAlbumRequest
import com.markvoronin.immichswipe.data.api.ImmichApi
import com.markvoronin.immichswipe.data.api.SearchAssetsRequest
import com.markvoronin.immichswipe.domain.model.Album
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import retrofit2.HttpException


// Provided by RepositoryModule (the constructor has optional parameters Dagger cannot satisfy).
class AlbumRepository(
    private val sessionManager: SessionManager,
    private val customApi: ImmichApi? = null
) {
    private val api: ImmichApi
        get() = customApi ?: sessionManager.api ?: error("No active API session")

    suspend fun getAlbumsRaw(): List<Album> {
        return api.getAlbums()
    }


    suspend fun getAlbumsForAsset(assetId: String): List<Album> {
        return api.getAlbumsForAsset(assetId)
    }

    suspend fun getAlbumArchiveCount(albumId: String): Int {
        return try {
            api.getSearchStatistics(
                SearchAssetsRequest(albumIds = listOf(albumId), visibility = "archive")
            ).total
        } catch (e: Exception) {
            e.rethrowIfCancelled()
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




    /** Creates an album. A failure is reported to the caller instead of being hidden as null. */
    suspend fun createAlbum(albumName: String, assetIds: List<String>? = null): Result<Album> =
        runCatchingCancellable {
            api.createAlbum(com.markvoronin.immichswipe.data.api.CreateAlbumRequest(
                albumName = albumName,
                assetIds = assetIds
            ))
        }.onFailure { AppLogger.e("AlbumRepository", "Error creating album $albumName", it) }

    suspend fun addAssetToAlbum(albumId: String, assetId: String): Result<Unit> =
        addAssetsToAlbum(albumId, listOf(assetId))

    suspend fun addAssetsToAlbum(albumId: String, assetIds: List<String>): Result<Unit> =
        runCatchingCancellable {
            val response = api.addAssetsToAlbum(albumId, AddAssetsToAlbumRequest(ids = assetIds))
            if (!response.isSuccessful) throw HttpException(response)
        }.onFailure { AppLogger.e("AlbumRepository", "Error adding assets to album $albumId", it) }

    suspend fun removeAssetsFromAlbum(albumId: String, assetIds: List<String>): Result<Unit> =
        runCatchingCancellable {
            val response = api.removeAssetsFromAlbum(albumId, AddAssetsToAlbumRequest(ids = assetIds))
            if (!response.isSuccessful) throw HttpException(response)
        }.onFailure { AppLogger.e("AlbumRepository", "Error removing assets from album $albumId", it) }
}
