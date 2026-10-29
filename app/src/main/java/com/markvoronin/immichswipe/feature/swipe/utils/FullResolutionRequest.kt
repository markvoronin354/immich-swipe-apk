package com.markvoronin.immichswipe.feature.swipe.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.request.transformations
import coil3.size.Precision
import coil3.size.Size
import coil3.transform.Transformation
import com.markvoronin.immichswipe.domain.model.Asset

private const val FULL_RESOLUTION_MAX_SIDE = 4096

private val RAW_EXTENSIONS = setOf(
    "cr2", "cr3", "crw", "nef", "nrw", "arw", "sr2", "srf", "dng", "raf", "orf", "rw2", "pef", "3fr", "erf", "mrw", "srw"
)

private val Asset.isRaw: Boolean
    get() = (fileExtension ?: originalFileName?.substringAfterLast('.', ""))
        ?.removePrefix(".")?.lowercase() in RAW_EXTENSIONS

/**
 * Coil only applies the EXIF orientation to JPEG, WebP and HEIC, so a RAW original is decoded in sensor
 * orientation. The server renders the preview upright, which is why only the original needs this.
 */
private class ExifOrientationTransformation(private val orientation: Int) : Transformation() {
    override val cacheKey = "exif-orientation-$orientation"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            2 -> matrix.postScale(-1f, 1f)
            3 -> matrix.postRotate(180f)
            4 -> matrix.postScale(1f, -1f)
            5 -> { matrix.postRotate(90f); matrix.postScale(-1f, 1f) }
            6 -> matrix.postRotate(90f)
            7 -> { matrix.postRotate(270f); matrix.postScale(-1f, 1f) }
            8 -> matrix.postRotate(270f)
        }
        return Bitmap.createBitmap(input, 0, 0, input.width, input.height, matrix, true)
    }
}

/** Turns a RAW original upright; null for every other asset, whose original Coil already rotates. */
internal fun Asset.rawOrientationTransformation(): Transformation? =
    exifInfo?.orientation?.toIntOrNull()?.takeIf { isRaw && it in 2..8 }?.let { ExifOrientationTransformation(it) }

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
        .apply { asset.rawOrientationTransformation()?.let { transformations(it) } }
        .build()
}
