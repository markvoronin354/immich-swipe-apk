package com.markvoronin.immichswipe.core.cache

import com.markvoronin.immichswipe.core.util.MemoryTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CachePolicyTest {
    private val mb = 1024 * 1024L
    private val plentyOfSpace = 100 * 1024 * mb

    @Test
    fun `video cache grows with the memory tier`() {
        assertEquals(256 * mb, CachePolicy.videoCacheBytes(MemoryTier.LOW, plentyOfSpace))
        assertEquals(512 * mb, CachePolicy.videoCacheBytes(MemoryTier.MEDIUM, plentyOfSpace))
        assertEquals(1024 * mb, CachePolicy.videoCacheBytes(MemoryTier.HIGH, plentyOfSpace))
    }

    @Test
    fun `video cache never claims more than a fifth of the free space`() {
        // 1 GB free: a fifth is about 205 MB, below the 512 MB tier budget.
        assertEquals(1024 * mb / 5, CachePolicy.videoCacheBytes(MemoryTier.MEDIUM, 1024 * mb))
    }

    @Test
    fun `video cache keeps a usable minimum on a nearly full device`() {
        assertEquals(64 * mb, CachePolicy.videoCacheBytes(MemoryTier.HIGH, 100 * mb))
        assertEquals(64 * mb, CachePolicy.videoCacheBytes(MemoryTier.HIGH, 0))
    }

    @Test
    fun `other cache limit follows the tier only`() {
        assertEquals(256 * mb, CachePolicy.otherCacheLimitBytes(MemoryTier.LOW))
        assertTrue(CachePolicy.otherCacheLimitBytes(MemoryTier.HIGH) > CachePolicy.otherCacheLimitBytes(MemoryTier.MEDIUM))
    }
}
