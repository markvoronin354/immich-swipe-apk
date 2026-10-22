package com.markvoronin.immichswipe.core.cache

import android.content.Context
import android.os.storage.StorageManager
import com.markvoronin.immichswipe.core.util.MemoryTier
import java.io.IOException

/**
 * How much disk the app's caches may use. Small devices get smaller caches, and no cache ever claims
 * more than a fifth of the free space, so a nearly full phone is not filled up by cached video.
 */
object CachePolicy {
    private const val MB = 1024 * 1024L
    private const val MIN_VIDEO_CACHE_BYTES = 64 * MB

    /** Disk budget of the ExoPlayer video cache. */
    fun videoCacheBytes(tier: MemoryTier, usableSpaceBytes: Long): Long =
        tierBudget(tier)
            .coerceAtMost(usableSpaceBytes / 5)
            .coerceAtLeast(MIN_VIDEO_CACHE_BYTES)

    /** Upper bound for the other cached files (shared copies and the like) before cleanup kicks in. */
    fun otherCacheLimitBytes(tier: MemoryTier): Long = tierBudget(tier)

    fun videoCacheBytes(context: Context): Long =
        videoCacheBytes(MemoryTier.getMemoryTier(context), availableBytes(context))

    fun otherCacheLimitBytes(context: Context): Long =
        otherCacheLimitBytes(MemoryTier.getMemoryTier(context))

    /**
     * Space the app can really use for cache. Android counts what it could free by clearing other apps'
     * caches, which is the right measure. If the system cannot answer, fall back to the plain free space.
     */
    @Suppress("UsableSpace")
    private fun availableBytes(context: Context): Long = try {
        val storageManager = context.getSystemService(StorageManager::class.java)
        storageManager.getAllocatableBytes(storageManager.getUuidForPath(context.cacheDir))
    } catch (_: IOException) {
        context.cacheDir.usableSpace
    }

    private fun tierBudget(tier: MemoryTier): Long = when (tier) {
        MemoryTier.LOW -> 256 * MB
        MemoryTier.MEDIUM -> 512 * MB
        MemoryTier.HIGH -> 1024 * MB
    }
}
