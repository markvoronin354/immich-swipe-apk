package com.markvoronin.immichswipe

import android.app.Application
import android.os.Build
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.memory.MemoryCache
import coil3.request.crossfade
import com.markvoronin.immichswipe.core.AppLogger
import com.markvoronin.immichswipe.core.cache.CacheManager
import com.markvoronin.immichswipe.core.util.MemoryTier
import dagger.hilt.android.HiltAndroidApp
import okio.Path.Companion.toOkioPath

@HiltAndroidApp
class ImmichSwipeApplication : Application(), SingletonImageLoader.Factory {
    override fun onCreate() {
        super.onCreate()
        // Initialized here (not in the activity) so background work such as workers is logged too.
        AppLogger.init(this)
        CacheManager.schedulePeriodicMaintenance(this)
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        val memoryCachePercent = when (MemoryTier.getMemoryTier(this)) {
            MemoryTier.LOW -> 0.15
            MemoryTier.MEDIUM -> 0.25
            MemoryTier.HIGH -> 0.35
        }

        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, memoryCachePercent)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache").toOkioPath())
                    .maxSizeBytes(250 * 1024 * 1024L) // 250MB cap
                    .build()
            }
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(AnimatedImageDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .crossfade(true)
            .build()
    }
}
