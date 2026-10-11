package com.markvoronin.immichswipe.domain.usecase

import com.markvoronin.immichswipe.core.AppLogger
import com.markvoronin.immichswipe.data.repository.AlbumRepository
import com.markvoronin.immichswipe.data.repository.AssetRepository
import com.markvoronin.immichswipe.data.repository.SwipeDecisionRepository
import com.markvoronin.immichswipe.domain.model.Asset
import com.markvoronin.immichswipe.feature.swipe.SwipeDecision
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ApplySwipeDecisionsUseCase @Inject constructor(
    private val assetRepository: AssetRepository,
    private val swipeDecisionRepository: SwipeDecisionRepository,
    private val albumRepository: AlbumRepository
) {
    suspend operator fun invoke(
        userId: String,
        currentAssets: List<Asset>,
        decisions: Map<String, SwipeDecision>,
        localRotations: Map<String, Int>,
        localFavorites: Map<String, Boolean>,
        assetsEditedThisSession: Set<String>,
        deletedCount: Int,
        deletedSize: Long,
        keptCount: Int,
        archiveCount: Int,
        lockedCount: Int
    ): ApplyResult = withContext(Dispatchers.IO) {
        val toDelete = decisions.filter { it.value == SwipeDecision.DELETE }.keys.toList()
        val toArchive = decisions.filter { it.value == SwipeDecision.ARCHIVE }.keys.toList()
        val toLock = decisions.filter { it.value == SwipeDecision.LOCK }.keys.toList()
        val toSkip = decisions.filter { it.value == SwipeDecision.SKIP }.keys.toList()
        val allSwipedIds = decisions.keys.toList()

        AppLogger.i(
            "ApplySwipeDecisionsUseCase",
            "Applying changes: ${toDelete.size} to delete, $keptCount kept, $archiveCount archive, $lockedCount locked, ${toSkip.size} skipped"
        )

        // 1. Delete on server (and local cache via AssetRepository)
        if (toDelete.isNotEmpty()) {
            try {
                assetRepository.deleteAssets(toDelete)
                // Also delete these decisions from local DB as they are no longer relevant
                swipeDecisionRepository.removeDecisions(toDelete, userId)
            } catch (e: Exception) {
                AppLogger.e("ApplySwipeDecisionsUseCase", "Failed to delete assets on server: ${e.message}", e)
            }
        }

        // 1b. Sync archive decisions to Immich server
        if (toArchive.isNotEmpty()) {
            try {
                assetRepository.updateAssets(toArchive, visibility = "archive")
            } catch (e: Exception) {
                AppLogger.e("ApplySwipeDecisionsUseCase", "Failed to archive assets on server: ${e.message}", e)
            }
        }

        // 1c. Sync lock decisions to Immich server (Immich API expects "hidden" for locked/hidden assets)
        if (toLock.isNotEmpty()) {
            try {
                assetRepository.updateAssets(toLock, visibility = "hidden")
                assetRepository.deleteLocalAssets(toLock)
                swipeDecisionRepository.removeDecisions(toLock, userId)
            } catch (e: Exception) {
                AppLogger.e("ApplySwipeDecisionsUseCase", "Failed to lock assets on server: ${e.message}", e)
            }
        }

        // 1d. Add skipped assets to SKIP album on server and local DB
        if (toSkip.isNotEmpty()) {
            try {
                val albums = albumRepository.getAlbumsRaw()
                var existingSkipAlbum = albums.find { it.albumName.equals("SKIP", ignoreCase = true) }
                if (existingSkipAlbum == null) {
                    existingSkipAlbum = albumRepository.createAlbum("SKIP", toSkip)
                } else {
                    albumRepository.addAssetsToAlbum(existingSkipAlbum.id, toSkip)
                }
                if (existingSkipAlbum != null) {
                    val assetsToInsert = currentAssets.filter { it.id in toSkip }
                    if (assetsToInsert.isNotEmpty()) {
                        assetRepository.insertAlbumAssetsLocally(existingSkipAlbum.id, userId, assetsToInsert)
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("ApplySwipeDecisionsUseCase", "Failed to add skipped assets to SKIP album: ${e.message}", e)
            }
        }

        // 1e. Remove assets that received a decision OTHER than SKIP from the SKIP album on server and local DB
        val nonSkipDecidedIds = decisions.filter { it.value != SwipeDecision.SKIP }.keys.toList()
        if (nonSkipDecidedIds.isNotEmpty()) {
            try {
                val albums = albumRepository.getAlbumsRaw()
                val existingSkipAlbum = albums.find { it.albumName.equals("SKIP", ignoreCase = true) }
                if (existingSkipAlbum != null) {
                    albumRepository.removeAssetsFromAlbum(existingSkipAlbum.id, nonSkipDecidedIds)
                    assetRepository.removeAlbumAssetsLocally(existingSkipAlbum.id, userId, nonSkipDecidedIds)
                }
            } catch (e: Exception) {
                AppLogger.e("ApplySwipeDecisionsUseCase", "Failed to remove non-skipped assets from SKIP album: ${e.message}", e)
            }
        }

        // 2. Mark remaining decisions as synced in DB (KEPT, ARCHIVE, SKIP)
        val toMarkSynced = allSwipedIds.filter { it !in toDelete && it !in toLock }
        if (toMarkSynced.isNotEmpty()) {
            swipeDecisionRepository.markAsSynced(toMarkSynced, userId)
        }

        // 2b. Persist rotation changes permanently locally and sync to Immich server
        localRotations.forEach { (assetId, rot) ->
            assetRepository.updateAssetRotation(assetId, userId, rot)
            if (rot != 0 || assetsEditedThisSession.contains(assetId)) {
                try {
                    assetRepository.syncAssetRotationToServer(assetId, rot)
                } catch (e: Exception) {
                    AppLogger.e("ApplySwipeDecisionsUseCase", "Failed to sync rotation for $assetId: ${e.message}", e)
                }
            }
        }

        // 2c. Sync favorite changes to Immich server
        val favsToAdd = localFavorites.filter { it.value }.keys.toList()
        val favsToRemove = localFavorites.filter { !it.value }.keys.toList()
        if (favsToAdd.isNotEmpty()) {
            try {
                assetRepository.updateAssets(favsToAdd, isFavorite = true)
            } catch (e: Exception) {
                AppLogger.e("ApplySwipeDecisionsUseCase", "Failed to sync favorite add: ${e.message}", e)
            }
        }
        if (favsToRemove.isNotEmpty()) {
            try {
                assetRepository.updateAssets(favsToRemove, isFavorite = false)
            } catch (e: Exception) {
                AppLogger.e("ApplySwipeDecisionsUseCase", "Failed to sync favorite remove: ${e.message}", e)
            }
        }

        // 3. Save sync history
        swipeDecisionRepository.saveSyncHistory(
            userId = userId,
            deletedCount = deletedCount,
            bytesSaved = deletedSize,
            keptCount = keptCount,
            archivedCount = archiveCount,
            lockedCount = lockedCount
        )

        return@withContext ApplyResult(
            toDelete = toDelete,
            toLock = toLock,
            nonSkipDecidedIds = nonSkipDecidedIds
        )
    }

    data class ApplyResult(
        val toDelete: List<String>,
        val toLock: List<String>,
        val nonSkipDecidedIds: List<String>
    )
}
