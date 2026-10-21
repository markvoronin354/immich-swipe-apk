package com.markvoronin.immichswipe.domain.usecase

import com.markvoronin.immichswipe.core.AppLogger
import com.markvoronin.immichswipe.data.repository.AlbumRepository
import com.markvoronin.immichswipe.data.repository.AssetRepository
import com.markvoronin.immichswipe.data.repository.SwipeDecisionRepository
import com.markvoronin.immichswipe.domain.model.Album
import com.markvoronin.immichswipe.domain.model.Asset
import com.markvoronin.immichswipe.feature.swipe.SwipeDecision
import kotlinx.coroutines.CancellationException
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
        lockedCount: Int,
        // While the skip feature is disabled no SKIP album is read, created or cleaned up.
        skipFeatureEnabled: Boolean = Album.IS_SKIP_FEATURE_ENABLED
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

        val failedSteps = linkedSetOf<SyncStep>()
        val failedIds = HashSet<String>()

        // Runs one server step. On failure the step is recorded, the affected ids are flagged so
        // they stay pending, and the remaining steps still run.
        suspend fun step(kind: SyncStep, ids: Collection<String>, block: suspend () -> Unit): Boolean {
            return try {
                block()
                true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLogger.e("ApplySwipeDecisionsUseCase", "Sync step $kind failed: ${e.message}", e)
                failedSteps += kind
                failedIds += ids
                false
            }
        }

        // Local bookkeeping that follows a successful server call. Its failure must not turn the
        // already applied server change into a reported failure (and a pointless retry).
        suspend fun bestEffortLocal(what: String, block: suspend () -> Unit) {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLogger.w("ApplySwipeDecisionsUseCase", "Local step '$what' failed after the server change was applied: ${e.message}")
            }
        }

        // 1. Delete on server (and local cache via AssetRepository)
        val deleteOk = toDelete.isEmpty() || step(SyncStep.DELETE, toDelete) {
            assetRepository.deleteAssets(toDelete)
            // These decisions are no longer relevant once the assets are gone. The server step already
            // succeeded, so a local failure here is only logged.
            bestEffortLocal("remove delete decisions") { swipeDecisionRepository.removeDecisions(toDelete, userId) }
        }

        // 1b. Sync archive decisions to Immich server
        val archiveOk = toArchive.isEmpty() || step(SyncStep.ARCHIVE, toArchive) {
            assetRepository.updateAssets(toArchive, visibility = "archive")
        }

        // 1c. Sync lock decisions to Immich server (Immich API expects "locked" for locked/hidden assets)
        val lockOk = toLock.isEmpty() || step(SyncStep.LOCK, toLock) {
            assetRepository.updateAssets(toLock, visibility = "locked")
            bestEffortLocal("clean up locked assets") {
                assetRepository.deleteLocalAssets(toLock)
                swipeDecisionRepository.removeDecisions(toLock, userId)
            }
        }

        // 1d. Add skipped assets to SKIP album on server and local DB.
        // The album repository reports failures through its return value, so check it explicitly.
        if (skipFeatureEnabled && toSkip.isNotEmpty()) {
            step(SyncStep.SKIP_ALBUM, toSkip) {
                val albums = albumRepository.getAlbumsRaw()
                val existing = albums.find { it.albumName.equals("SKIP", ignoreCase = true) }
                val skipAlbumId = if (existing == null) {
                    checkNotNull(albumRepository.createAlbum("SKIP", toSkip)) { "Could not create the SKIP album" }.id
                } else {
                    check(albumRepository.addAssetsToAlbum(existing.id, toSkip)) { "Could not add assets to the SKIP album" }
                    existing.id
                }
                val assetsToInsert = currentAssets.filter { it.id in toSkip }
                if (assetsToInsert.isNotEmpty()) {
                    assetRepository.insertAlbumAssetsLocally(skipAlbumId, userId, assetsToInsert)
                }
            }
        }

        // 1e. Remove assets that received a decision OTHER than SKIP from the SKIP album
        val nonSkipDecidedIds = decisions.filter { it.value != SwipeDecision.SKIP }.keys.toList()
        if (skipFeatureEnabled && nonSkipDecidedIds.isNotEmpty()) {
            step(SyncStep.SKIP_ALBUM, nonSkipDecidedIds) {
                val albums = albumRepository.getAlbumsRaw()
                val skipAlbum = albums.find { it.albumName.equals("SKIP", ignoreCase = true) }
                if (skipAlbum != null) {
                    check(albumRepository.removeAssetsFromAlbum(skipAlbum.id, nonSkipDecidedIds)) {
                        "Could not remove assets from the SKIP album"
                    }
                    assetRepository.removeAlbumAssetsLocally(skipAlbum.id, userId, nonSkipDecidedIds)
                }
            }
        }

        // 2. Mark decisions as synced only when their server step succeeded (KEEP, ARCHIVE, SKIP)
        val toMarkSynced = allSwipedIds.filter { it !in toDelete && it !in toLock && it !in failedIds }
        if (toMarkSynced.isNotEmpty()) {
            swipeDecisionRepository.markAsSynced(toMarkSynced, userId)
        }

        // 2b. Persist rotation changes locally and sync to Immich server
        localRotations.forEach { (assetId, rot) ->
            assetRepository.updateAssetRotation(assetId, userId, rot)
            if (rot != 0 || assetsEditedThisSession.contains(assetId)) {
                step(SyncStep.ROTATION, emptyList()) {
                    assetRepository.syncAssetRotationToServer(assetId, rot)
                }
            }
        }

        // 2c. Sync favorite changes to Immich server
        val favsToAdd = localFavorites.filter { it.value }.keys.toList()
        val favsToRemove = localFavorites.filter { !it.value }.keys.toList()
        val failedFavorites = HashSet<String>()
        if (favsToAdd.isNotEmpty() && !step(SyncStep.FAVORITES, emptyList()) {
                assetRepository.updateAssets(favsToAdd, isFavorite = true)
            }
        ) failedFavorites += favsToAdd
        if (favsToRemove.isNotEmpty() && !step(SyncStep.FAVORITES, emptyList()) {
                assetRepository.updateAssets(favsToRemove, isFavorite = false)
            }
        ) failedFavorites += favsToRemove

        // 3. Save sync history, counting only what was really applied on the server. Kept items have
        // no server step, so they are only counted once everything succeeded; otherwise the retry
        // of this apply would count them a second time.
        val allServerStepsOk = deleteOk && archiveOk && lockOk
        val keptForHistory = if (allServerStepsOk) keptCount else 0
        val anyApplied = (toDelete.isNotEmpty() && deleteOk) || (toLock.isNotEmpty() && lockOk) ||
            (toArchive.isNotEmpty() && archiveOk) || keptForHistory > 0
        if (anyApplied) {
            swipeDecisionRepository.saveSyncHistory(
                userId = userId,
                deletedCount = if (deleteOk) deletedCount else 0,
                bytesSaved = if (deleteOk) deletedSize else 0,
                keptCount = keptForHistory,
                archivedCount = if (archiveOk) archiveCount else 0,
                lockedCount = if (lockOk) lockedCount else 0
            )
        }

        val succeededDelete = if (deleteOk) toDelete else emptyList()
        val deletedAssets = currentAssets.filter { it.id in succeededDelete }

        return@withContext ApplyResult(
            toDelete = succeededDelete,
            toLock = if (lockOk) toLock else emptyList(),
            nonSkipDecidedIds = nonSkipDecidedIds.filter { it !in failedIds },
            deletedAssets = deletedAssets,
            failedSteps = failedSteps,
            failedFavoriteIds = failedFavorites
        )
    }

    enum class SyncStep { DELETE, ARCHIVE, LOCK, SKIP_ALBUM, ROTATION, FAVORITES }

    data class ApplyResult(
        val toDelete: List<String>,
        val toLock: List<String>,
        val nonSkipDecidedIds: List<String>,
        val deletedAssets: List<Asset> = emptyList(),
        val failedSteps: Set<SyncStep> = emptySet(),
        val failedFavoriteIds: Set<String> = emptySet()
    ) {
        val hasFailures: Boolean get() = failedSteps.isNotEmpty()
    }
}
