package com.markvoronin.immichswipe.feature.swipe.utils

import android.content.Context
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.size.Precision
import com.markvoronin.immichswipe.domain.model.Asset

private const val FULL_RESOLUTION_MAX_SIDE = 4096

/** Videos have no still to enlarge and GIFs already show their original. */
val Asset.supportsFullResolution: Boolean get() = type != "VIDEO" && !isGif

/**
 * The asset's original, shared by the card and the fullscreen viewer. Both build the identical request
 * so the viewer reuses the bitmap the card already decoded instead of downloading it a second time.
 */
fun fullResolutionRequest(context: Context, baseUrl: String, asset: Asset): ImageRequest {
    // Never decode past the original's own size or the GPU's texture limit.
    val longSide = maxOf(asset.exifInfo?.imageWidth ?: 0, asset.exifInfo?.imageHeight ?: 0)
    return ImageRequest.Builder(context)
        .data("$baseUrl/api/assets/${asset.id}/original")
        .memoryCacheKey("${asset.id}-original")
        .placeholderMemoryCacheKey("${asset.id}-preview")
        .size(if (longSide > 0) minOf(FULL_RESOLUTION_MAX_SIDE, longSide) else FULL_RESOLUTION_MAX_SIDE)
        .allowHardware(true)
        .crossfade(false)
        .precision(Precision.INEXACT)
        // A one-off multi-megabyte original should not be written to disk.
        .diskCachePolicy(CachePolicy.DISABLED)
        .build()
}
