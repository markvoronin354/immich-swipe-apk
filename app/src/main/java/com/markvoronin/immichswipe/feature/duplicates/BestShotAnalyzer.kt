package com.markvoronin.immichswipe.feature.duplicates

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmapOrNull
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker.FaceLandmarkerOptions
import com.markvoronin.immichswipe.core.AppLogger
import com.markvoronin.immichswipe.domain.model.Asset
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow
import kotlin.math.sqrt

@Singleton
class BestShotAnalyzer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val imageLoader = ImageLoader(context)
    private var faceLandmarker: FaceLandmarker? = null

    init {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("face_landmarker.task")
            .build()
            
        val options = FaceLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.IMAGE)
            .setNumFaces(3)
            .build()

        try {
            faceLandmarker = FaceLandmarker.createFromOptions(context, options)
        } catch (e: Exception) {
            AppLogger.e("BestShotAnalyzer", "Failed to initialize FaceLandmarker", e)
        }
    }

    data class AssetScore(
        val assetId: String,
        val totalScore: Float,
        val hasFaces: Boolean,
        val sharpnessScore: Float,
        val avgEar: Float?,
        val avgMar: Float?
    )

    enum class BestShotMode {
        AUTO,
        SINGLE_BEST,
        TOP_2,
        TOP_3
    }

    suspend fun analyzeCluster(assets: List<Asset>, baseUrl: String, apiKey: String): List<AssetScore> = withContext(Dispatchers.Default) {
        if (assets.isEmpty()) return@withContext emptyList()

        val scores = assets.map { asset ->
            if (asset.type.equals("VIDEO", ignoreCase = true)) {
                analyzeVideoAsset(asset)
            } else {
                val bitmap = fetchThumbnailBitmap(asset, baseUrl, apiKey)
                if (bitmap != null) {
                    analyzeAsset(asset, bitmap)
                } else {
                    analyzeVideoAsset(asset)
                }
            }
        }

        scores.sortedByDescending { it.totalScore }
    }

    fun selectKeptAssetIds(scores: List<AssetScore>, mode: BestShotMode): Set<String> {
        if (scores.isEmpty()) return emptySet()

        val sortedScores = scores.sortedByDescending { it.totalScore }
        val topScore = sortedScores.first().totalScore

        return when (mode) {
            BestShotMode.SINGLE_BEST -> {
                setOf(sortedScores.first().assetId)
            }
            BestShotMode.TOP_2 -> {
                sortedScores.take(2).map { it.assetId }.toSet()
            }
            BestShotMode.TOP_3 -> {
                sortedScores.take(3).map { it.assetId }.toSet()
            }
            BestShotMode.AUTO -> {
                val scoreThreshold = maxOf(0f, topScore - 0.05f)
                val maxKeepLimit = maxOf(2, sortedScores.size - 1)

                val topTierAssets = sortedScores.filter { it.totalScore >= scoreThreshold }
                if (topTierAssets.isNotEmpty()) {
                    topTierAssets.take(maxKeepLimit).map { it.assetId }.toSet()
                } else {
                    setOf(sortedScores.first().assetId)
                }
            }
        }
    }

    suspend fun findBestShot(assets: List<Asset>, baseUrl: String, apiKey: String): AssetScore? {
        return analyzeCluster(assets, baseUrl, apiKey).firstOrNull()
    }

    private fun analyzeVideoAsset(asset: Asset): AssetScore {
        val fileSize = asset.exifInfo?.fileSizeInBytes ?: 0L
        val fileSizeScore = (fileSize.toFloat() / (100 * 1024 * 1024f)).coerceIn(0f, 1f)
        val score = (fileSizeScore * 0.5f) + (asset.resolutionScore() * 0.5f)
        return AssetScore(
            assetId = asset.id,
            totalScore = score,
            hasFaces = false,
            sharpnessScore = 0f,
            avgEar = null,
            avgMar = null
        )
    }

    private suspend fun fetchThumbnailBitmap(asset: Asset, baseUrl: String, apiKey: String): Bitmap? = withContext(Dispatchers.IO) {
        val request = ImageRequest.Builder(context)
            .data("${baseUrl.removeSuffix("/")}/api/assets/${asset.id}/thumbnail?size=preview")
            .addHeader("x-api-key", apiKey)
            .size(512) // Limit size for performance
            .allowHardware(false) // Software bitmap for pixel access
            .build()

        val result = imageLoader.execute(request)
        if (result is SuccessResult) {
            result.drawable.toBitmapOrNull()
        } else {
            null
        }
    }

    private fun analyzeAsset(asset: Asset, bitmap: Bitmap): AssetScore {
        val mpImage = BitmapImageBuilder(bitmap).build()
        val result = faceLandmarker?.detect(mpImage)
        
        val sharpness = calculateSharpness(bitmap)

        if (result == null || result.faceLandmarks().isEmpty()) {
            val score = (sharpness * 0.7f) + (asset.resolutionScore() * 0.3f)
            return AssetScore(asset.id, score, hasFaces = false, sharpness, null, null)
        }

        val faces = result.faceLandmarks()
        
        val earScores = faces.map { calculateEAR(it) }
        val marScores = faces.map { calculateMAR(it) }
        
        val avgEar = earScores.average().toFloat()
        val avgMar = marScores.average().toFloat()
        
        // Penalize blinking heavily. Normalize EAR (usually around 0.2 - 0.35). If EAR < 0.2, eyes might be closed.
        val earScore = (avgEar / 0.3f).coerceIn(0f, 1f)
        
        // Smiles. Normalize MAR. Smiling tends to increase MAR slightly or change shape, but basic MAR > 0.1 usually means mouth open/smiling.
        val marScore = (avgMar / 0.2f).coerceIn(0f, 1f)

        val faceScore = (earScore * 0.6f) + (marScore * 0.2f) + (sharpness * 0.1f) + (asset.resolutionScore() * 0.1f)
        
        return AssetScore(asset.id, faceScore, hasFaces = true, sharpness, avgEar, avgMar)
    }

    private fun distance(p1: NormalizedLandmark, p2: NormalizedLandmark): Float {
        return sqrt((p1.x() - p2.x()).pow(2) + (p1.y() - p2.y()).pow(2))
    }

    private fun calculateEAR(landmarks: List<NormalizedLandmark>): Float {
        // Left Eye
        val leftV1 = distance(landmarks[159], landmarks[145])
        val leftV2 = distance(landmarks[158], landmarks[153])
        val leftH = distance(landmarks[33], landmarks[133])
        val leftEar = (leftV1 + leftV2) / (2.0f * leftH)

        // Right Eye
        val rightV1 = distance(landmarks[386], landmarks[374])
        val rightV2 = distance(landmarks[385], landmarks[373])
        val rightH = distance(landmarks[362], landmarks[263])
        val rightEar = (rightV1 + rightV2) / (2.0f * rightH)

        return (leftEar + rightEar) / 2.0f
    }

    private fun calculateMAR(landmarks: List<NormalizedLandmark>): Float {
        val v1 = distance(landmarks[13], landmarks[14])
        val v2 = distance(landmarks[81], landmarks[178])
        val h = distance(landmarks[78], landmarks[308])
        return (v1 + v2) / (2.0f * h)
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

        // Normalize variance somewhat (varies a lot, usually 0 to 2000+)
        // Cap at 2000 for normalization
        return (variance / 2000.0).coerceIn(0.0, 1.0).toFloat()
    }

    private fun Asset.resolutionScore(): Float {
        val width = exifInfo?.imageWidth ?: 0
        val height = exifInfo?.imageHeight ?: 0
        val res = (width * height).toFloat()
        // Max expected is around 24MP (6000x4000) = 24,000,000
        return (res / 24_000_000f).coerceIn(0f, 1f)
    }
}
