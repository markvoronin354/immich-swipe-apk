package com.markvoronin.immichswipe.feature.duplicates

import android.content.Context
import android.graphics.Bitmap
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.request.transformations
import coil3.toBitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.components.containers.Category
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker.FaceLandmarkerOptions
import com.markvoronin.immichswipe.core.AppLogger
import com.markvoronin.immichswipe.domain.model.Asset
import com.markvoronin.immichswipe.feature.swipe.utils.rawOrientationTransformation
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class BestShotAnalyzer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Share the app-wide Coil loader (and its caches) instead of creating a private one.
    private val imageLoader: ImageLoader get() = SingletonImageLoader.get(context)

    private var faceLandmarker: FaceLandmarker? = null
    private var landmarkerInitFailed = false

    /**
     * The model is loaded on first use (on the analysis dispatcher) rather than at injection time,
     * and can be released with [release] when the screen that needs it goes away.
     */
    @Synchronized
    private fun landmarker(): FaceLandmarker? {
        faceLandmarker?.let { return it }
        if (landmarkerInitFailed) return null
        return try {
            val options = FaceLandmarkerOptions.builder()
                .setBaseOptions(BaseOptions.builder().setModelAssetPath("face_landmarker.task").build())
                .setRunningMode(RunningMode.IMAGE)
                .setNumFaces(MAX_FACES)
                .setMinFaceDetectionConfidence(MIN_FACE_CONFIDENCE)
                .setMinFacePresenceConfidence(MIN_FACE_CONFIDENCE)
                .setOutputFaceBlendshapes(true)
                .build()
            FaceLandmarker.createFromOptions(context, options).also { faceLandmarker = it }
        } catch (e: Exception) {
            landmarkerInitFailed = true
            AppLogger.e("BestShotAnalyzer", "Failed to initialize FaceLandmarker", e)
            null
        }
    }

    @Synchronized
    fun release() {
        faceLandmarker?.close()
        faceLandmarker = null
    }

    private companion object {
        const val TAG = "BestShotAnalyzer"
        const val MAX_FACES = 30
        // Below MediaPipe's 0.5 default so small, soft faces in the 1024px preview are still found.
        const val MIN_FACE_CONFIDENCE = 0.3f
        // Blendshapes are noise on faces smaller than this many pixels wide.
        const val MIN_FACE_PX = 48
        // Originals are scaled down to fit this, which also bounds the decoded bitmap to about 50 MB.
        const val DEEP_MAX_SIDE = 4096
        const val ANALYSIS_SIDE = 1024
    }

    // Pixel box of a detected face in the coordinates of the full image being analyzed.
    private class Face(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
        val blendshapes: List<Category>
    ) {
        val width get() = right - left
        val centerX get() = (left + right) / 2f
        val centerY get() = (top + bottom) / 2f

        fun containsCenterOf(other: Face) =
            other.centerX in left..right && other.centerY in top..bottom

        // Faces cut by the tile edge give unreliable blendshapes and are found whole by a neighbouring tile.
        fun isWithin(tile: Tile) =
            left > tile.x && top > tile.y && right < tile.x + tile.width && bottom < tile.y + tile.height

        fun isCentredIn(tile: Tile) =
            centerX >= tile.cellLeft && centerX < tile.cellRight && centerY >= tile.cellTop && centerY < tile.cellBottom
    }

    data class AssetScore(
        val assetId: String,
        val totalScore: Float,
        val hasFaces: Boolean,
        val sharpnessScore: Float,
        val eyesOpen: Float?,
        val smile: Float?,
        // False when the image could not be loaded; such assets are never recommended for deletion.
        val analyzed: Boolean = true
    )

    private data class Measurement(
        val asset: Asset,
        val sharpness: Float,
        val eyesOpen: Float?,
        val smile: Float?
    )

    enum class BestShotMode {
        AUTO,
        SINGLE_BEST,
        TOP_2,
        TOP_3
    }

    suspend fun analyzeCluster(assets: List<Asset>, baseUrl: String): List<AssetScore> =
        scoreCluster(assets) { asset ->
            fetchThumbnailBitmap(asset, baseUrl)?.let { measureAsset(asset, it) }
        }

    /**
     * Like [analyzeCluster], but downloads the originals and searches them tile by tile, which is the
     * only way to read the faces in a large group photo. Costs one full-size download per photo.
     */
    suspend fun analyzeClusterDeep(assets: List<Asset>, baseUrl: String): List<AssetScore> =
        scoreCluster(assets) { asset ->
            fetchOriginalBitmap(asset, baseUrl)?.let { original ->
                try {
                    measureDeep(asset, original)
                } finally {
                    original.recycle()
                }
            }
        }

    private suspend fun scoreCluster(
        assets: List<Asset>,
        measure: suspend (Asset) -> Measurement?
    ): List<AssetScore> = withContext(Dispatchers.Default) {
        if (assets.isEmpty()) return@withContext emptyList()

        val scores = mutableListOf<AssetScore>()
        val measurements = mutableListOf<Measurement>()
        assets.forEach { asset ->
            if (asset.type.equals("VIDEO", ignoreCase = true)) {
                scores += analyzeVideoAsset(asset)
            } else {
                val measurement = measure(asset)
                if (measurement != null) {
                    measurements += measurement
                } else {
                    scores += AssetScore(asset.id, 0f, hasFaces = false, 0f, null, null, analyzed = false)
                }
            }
        }

        // Sharpness only matters relative to the other shots of the same scene.
        val maxSharpness = measurements.maxOfOrNull { it.sharpness } ?: 0f
        measurements.mapTo(scores) { scoreMeasurement(it, maxSharpness) }
        scores.forEach {
            AppLogger.d(
                TAG,
                "${it.assetId}: score=%.3f eyes=%s smile=%s sharpness=%.2f".format(
                    it.totalScore, it.eyesOpen?.let { v -> "%.2f".format(v) }, it.smile?.let { v -> "%.2f".format(v) }, it.sharpnessScore
                )
            )
        }

        scores.sortedWith(byBestFirst)
    }

    fun selectKeptAssetIds(scores: List<AssetScore>, mode: BestShotMode): Set<String> {
        if (scores.isEmpty()) return emptySet()

        // Assets that could not be analyzed are always kept rather than guessed at.
        val unanalyzedIds = scores.filterNot { it.analyzed }.map { it.assetId }.toSet()
        val sortedScores = scores.filter { it.analyzed }.sortedWith(byBestFirst)
        if (sortedScores.isEmpty()) return unanalyzedIds
        val topScore = sortedScores.first().totalScore
        // At least one shot of a cluster is always left for deletion.
        val maxKeepLimit = maxOf(1, sortedScores.size - 1)

        return unanalyzedIds + when (mode) {
            BestShotMode.SINGLE_BEST -> {
                setOf(sortedScores.first().assetId)
            }
            BestShotMode.TOP_2 -> {
                sortedScores.take(minOf(2, maxKeepLimit)).map { it.assetId }.toSet()
            }
            BestShotMode.TOP_3 -> {
                sortedScores.take(minOf(3, maxKeepLimit)).map { it.assetId }.toSet()
            }
            BestShotMode.AUTO -> {
                val scoreThreshold = maxOf(0f, topScore - 0.05f)

                val topTierAssets = sortedScores.filter { it.totalScore >= scoreThreshold }
                if (topTierAssets.isNotEmpty()) {
                    topTierAssets.take(maxKeepLimit).map { it.assetId }.toSet()
                } else {
                    setOf(sortedScores.first().assetId)
                }
            }
        }
    }

    // Ties are common once scores saturate, so fall back to sharpness rather than input order.
    private val byBestFirst = compareByDescending<AssetScore> { it.totalScore }.thenByDescending { it.sharpnessScore }

    private fun analyzeVideoAsset(asset: Asset): AssetScore {
        val fileSize = asset.exifInfo?.fileSizeInBytes ?: 0L
        val fileSizeScore = (fileSize.toFloat() / (100 * 1024 * 1024f)).coerceIn(0f, 1f)
        val score = (fileSizeScore * 0.5f) + (asset.resolutionScore() * 0.5f)
        return AssetScore(
            assetId = asset.id,
            totalScore = score,
            hasFaces = false,
            sharpnessScore = 0f,
            eyesOpen = null,
            smile = null
        )
    }

    private suspend fun fetchThumbnailBitmap(asset: Asset, baseUrl: String): Bitmap? =
        ImageRequest.Builder(context)
            .data("${baseUrl.removeSuffix("/")}/api/assets/${asset.id}/thumbnail?size=preview")
            .size(ANALYSIS_SIDE) // Limit size for performance
            .allowHardware(false) // Software bitmap for pixel access
            .build()
            .loadBitmap()

    private suspend fun fetchOriginalBitmap(asset: Asset, baseUrl: String): Bitmap? =
        ImageRequest.Builder(context)
            .data("${baseUrl.removeSuffix("/")}/api/assets/${asset.id}/original")
            // Coil also scales up to fit, so never ask for more than the original has.
            .size(minOf(DEEP_MAX_SIDE, asset.longSidePx() ?: DEEP_MAX_SIDE))
            .allowHardware(false)
            // A one-off multi-megabyte original should not push the thumbnails the app relies on out of the caches.
            .memoryCachePolicy(CachePolicy.DISABLED)
            .diskCachePolicy(CachePolicy.DISABLED)
            // A RAW original is decoded sideways, which the face detector cannot read.
            .apply { asset.rawOrientationTransformation()?.let { transformations(it) } }
            .build()
            .loadBitmap()

    private suspend fun ImageRequest.loadBitmap(): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val result = imageLoader.execute(this@loadBitmap)
            if (result is SuccessResult) result.image.toBitmap() else null
        } catch (e: OutOfMemoryError) {
            AppLogger.e(TAG, "Out of memory decoding $data", e)
            null
        }
    }

    // The MediaPipe task is not thread-safe, so detection is serialized.
    @Synchronized
    private fun detectFaces(bitmap: Bitmap, offsetX: Float = 0f, offsetY: Float = 0f, scale: Float = 1f): List<Face> {
        val result = landmarker()?.detect(BitmapImageBuilder(bitmap).build()) ?: return emptyList()
        val blendshapes = result.faceBlendshapes().orElse(null) ?: return emptyList()
        return result.faceLandmarks().zip(blendshapes) { landmarks, shapes ->
            Face(
                left = offsetX + landmarks.minOf { it.x() } * bitmap.width * scale,
                top = offsetY + landmarks.minOf { it.y() } * bitmap.height * scale,
                right = offsetX + landmarks.maxOf { it.x() } * bitmap.width * scale,
                bottom = offsetY + landmarks.maxOf { it.y() } * bitmap.height * scale,
                blendshapes = shapes
            )
        }
    }

    private fun measureAsset(asset: Asset, bitmap: Bitmap): Measurement {
        val sharpness = calculateSharpness(bitmap)
        val detected = detectFaces(bitmap)
        // Only faces big enough to read are scored; the rest are just logged.
        val faces = detected.filter { it.width >= MIN_FACE_PX }
        AppLogger.d(
            TAG,
            "${asset.id}: ${bitmap.width}x${bitmap.height}, ${detected.size} faces detected, " +
                "${faces.size} usable, widths(px)=${detected.map { it.width.toInt() }}"
        )
        return summarize(asset, sharpness, faces)
    }

    private suspend fun measureDeep(asset: Asset, original: Bitmap): Measurement {
        val startedAt = System.currentTimeMillis()

        // The normal-size pass still catches the large faces, which a tile would cut in half.
        val shrink = ANALYSIS_SIDE.toFloat() / maxOf(original.width, original.height)
        val overview = if (shrink < 1f) {
            Bitmap.createScaledBitmap(
                original,
                (original.width * shrink).roundToInt().coerceAtLeast(1),
                (original.height * shrink).roundToInt().coerceAtLeast(1),
                true
            )
        } else {
            original
        }
        val sharpness = calculateSharpness(overview)
        val large = detectFaces(overview, scale = original.width.toFloat() / overview.width)
        if (overview !== original) overview.recycle()

        val tiles = tilesFor(original.width, original.height)
        val small = mutableListOf<Face>()
        for (tile in tiles) {
            currentCoroutineContext().ensureActive()
            val crop = Bitmap.createBitmap(original, tile.x, tile.y, tile.width, tile.height)
            try {
                detectFaces(crop, tile.x.toFloat(), tile.y.toFloat()).filterTo(small) { face ->
                    face.isWithin(tile) && face.isCentredIn(tile) && large.none { it.containsCenterOf(face) }
                }
            } finally {
                if (crop !== original) crop.recycle()
            }
        }

        val faces = (large + small).filter { it.width >= MIN_FACE_PX }
        AppLogger.d(
            TAG,
            "${asset.id}: deep ${original.width}x${original.height} " +
                "(original ${asset.exifInfo?.imageWidth}x${asset.exifInfo?.imageHeight}), ${tiles.size} tiles, " +
                "${large.size} large + ${small.size} small faces detected, ${faces.size} usable, " +
                "${System.currentTimeMillis() - startedAt} ms"
        )
        return summarize(asset, sharpness, faces)
    }

    private fun summarize(asset: Asset, sharpness: Float, faces: List<Face>): Measurement {
        if (faces.isEmpty()) return Measurement(asset, sharpness, null, null)

        // Averaged rather than taking the worst face: the number of faces found differs between shots,
        // and a minimum would penalize the shot that simply found more of them.
        val eyesOpen = faces.map { 1f - (it.blendshapes.blendshape("eyeBlinkLeft") + it.blendshapes.blendshape("eyeBlinkRight")) / 2f }
            .average().toFloat()
        val smile = faces.map { (it.blendshapes.blendshape("mouthSmileLeft") + it.blendshapes.blendshape("mouthSmileRight")) / 2f }
            .average().toFloat()
        return Measurement(asset, sharpness, eyesOpen, smile)
    }

    private fun List<Category>.blendshape(name: String): Float =
        firstOrNull { it.categoryName() == name }?.score() ?: 0f

    private fun scoreMeasurement(m: Measurement, maxSharpness: Float): AssetScore {
        val relativeSharpness = if (maxSharpness > 0f) m.sharpness / maxSharpness else 0f
        // Shots without a detected face use a neutral eye score, so a lost face ranks below an
        // open-eyed one but above a blink.
        val score = ((m.eyesOpen ?: 0.5f) * 0.5f) + ((m.smile ?: 0f) * 0.2f) +
            (relativeSharpness * 0.2f) + (m.asset.resolutionScore() * 0.1f)
        return AssetScore(m.asset.id, score, m.eyesOpen != null, relativeSharpness, m.eyesOpen, m.smile)
    }

    private fun calculateSharpness(bitmap: Bitmap): Float {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val gray = IntArray(width * height)
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16) and 0xff
            val g = (p shr 8) and 0xff
            val b = p and 0xff
            gray[i] = (r * 0.299f + g * 0.587f + b * 0.114f).toInt()
        }

        var sumLaplacian = 0.0
        var sumLaplacianSq = 0.0
        var count = 0

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val top = gray[(y - 1) * width + x]
                val left = gray[y * width + (x - 1)]
                val center = gray[y * width + x]
                val right = gray[y * width + (x + 1)]
                val bottom = gray[(y + 1) * width + x]

                val laplacian = top + left + right + bottom - 4 * center
                sumLaplacian += laplacian
                sumLaplacianSq += laplacian * laplacian
                count++
            }
        }

        if (count == 0) return 0f
        val mean = sumLaplacian / count
        val variance = (sumLaplacianSq / count) - (mean * mean)

        return variance.toFloat()
    }

    private fun Asset.longSidePx(): Int? =
        maxOf(exifInfo?.imageWidth ?: 0, exifInfo?.imageHeight ?: 0).takeIf { it > 0 }

    private fun Asset.resolutionScore(): Float {
        val width = exifInfo?.imageWidth ?: 0
        val height = exifInfo?.imageHeight ?: 0
        val res = (width * height).toFloat()
        // Max expected is around 24MP (6000x4000) = 24,000,000
        return (res / 24_000_000f).coerceIn(0f, 1f)
    }
}

private const val TILE_PX = 512
private const val TILE_OVERLAP = 256
private const val TILE_STRIDE = TILE_PX - TILE_OVERLAP

/**
 * A square crop of the image to run face detection on, plus the [cell] of the image it is responsible for.
 * Cells partition the image, so a face is only counted by the one tile its centre falls in, while the
 * overlap around each cell lets that tile see the whole face.
 */
internal class Tile(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val cellLeft: Int,
    val cellTop: Int,
    val cellRight: Int,
    val cellBottom: Int
)

internal fun tilesFor(width: Int, height: Int): List<Tile> {
    val columns = tileSpans(width)
    val rows = tileSpans(height)
    return rows.flatMap { row ->
        columns.map { column ->
            Tile(column.start, row.start, column.size, row.size, column.cellStart, row.cellStart, column.cellEnd, row.cellEnd)
        }
    }
}

private class TileSpan(val start: Int, val size: Int, val cellStart: Int, val cellEnd: Int)

private fun tileSpans(length: Int): List<TileSpan> {
    if (length <= TILE_PX) return listOf(TileSpan(0, length, 0, length))
    val size = TILE_PX
    return (0 until length step TILE_STRIDE).map { cellStart ->
        val start = (cellStart - TILE_OVERLAP / 2).coerceIn(0, length - size)
        TileSpan(start, size, cellStart, minOf(cellStart + TILE_STRIDE, length))
    }
}
