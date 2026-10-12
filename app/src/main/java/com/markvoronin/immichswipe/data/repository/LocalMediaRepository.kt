package com.markvoronin.immichswipe.data.repository

import android.app.PendingIntent
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.markvoronin.immichswipe.core.AppLogger
import com.markvoronin.immichswipe.domain.model.Asset
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class LocalMediaRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Queries MediaStore for local media URIs matching the given list of Immich assets.
     * Matches by DISPLAY_NAME (original file name) and optionally SIZE.
     */
    suspend fun findLocalMediaUris(assets: List<Asset>): List<Uri> = withContext(Dispatchers.IO) {
        if (assets.isEmpty()) return@withContext emptyList()

        val matchedUris = mutableListOf<Uri>()

        // Map filenames to expected assets
        val filenameToAssets = assets.mapNotNull { asset ->
            asset.originalFileName?.let { name ->
                val cleanName = name.substringAfterLast('/').substringAfterLast('\\')
                cleanName to asset
            }
        }.groupBy({ it.first }, { it.second })

        if (filenameToAssets.isEmpty()) return@withContext emptyList()

        val contentResolver = context.contentResolver

        // Helper to query MediaStore table (Images or Video)
        fun queryTable(contentUri: Uri) {
            val projection = arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.SIZE
            )

            // Process filenames in chunks to keep SQL selection string bounded
            filenameToAssets.keys.chunked(100).forEach { filenameChunk ->
                val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} IN (${filenameChunk.joinToString(",") { "?" }})"
                val selectionArgs = filenameChunk.toTypedArray()

                try {
                    contentResolver.query(
                        contentUri,
                        projection,
                        selection,
                        selectionArgs,
                        null
                    )?.use { cursor ->
                        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                        val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                        val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)

                        while (cursor.moveToNext()) {
                            val id = cursor.getLong(idColumn)
                            val name = cursor.getString(nameColumn)
                            val size = cursor.getLong(sizeColumn)

                            val matchingAssets = filenameToAssets[name]
                            if (matchingAssets != null) {
                                // Match by size if available (allow 1MB tolerance)
                                val sizeMatches = matchingAssets.any { asset ->
                                    val assetSize = asset.exifInfo?.fileSizeInBytes
                                    assetSize == null || assetSize == 0L || abs(assetSize - size) < 1024 * 1024
                                }

                                if (sizeMatches) {
                                    val uri = ContentUris.withAppendedId(contentUri, id)
                                    matchedUris.add(uri)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    AppLogger.e("LocalMediaRepo", "Error querying MediaStore table $contentUri: ${e.message}", e)
                }
            }
        }

        queryTable(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        queryTable(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)

        AppLogger.i("LocalMediaRepo", "Matched ${matchedUris.size} local MediaStore URIs out of ${assets.size} requested assets")
        return@withContext matchedUris.distinct()
    }

    /**
     * Generates a PendingIntent to delete or trash the given URIs on Android 10/11+,
     * or performs direct deletion on legacy Android versions.
     */
    fun createDeletePendingIntent(uris: List<Uri>, trash: Boolean): PendingIntent? {
        if (uris.isEmpty()) return null

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // API 30+ (Android 11+)
                if (trash) {
                    MediaStore.createTrashRequest(context.contentResolver, uris, true)
                } else {
                    MediaStore.createDeleteRequest(context.contentResolver, uris)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // API 29 (Android 10)
                MediaStore.createDeleteRequest(context.contentResolver, uris)
            } else { // Android 9 and lower
                uris.forEach { uri ->
                    try {
                        context.contentResolver.delete(uri, null, null)
                    } catch (e: Exception) {
                        AppLogger.e("LocalMediaRepo", "Failed legacy deletion for $uri: ${e.message}")
                    }
                }
                null
            }
        } catch (e: Exception) {
            AppLogger.e("LocalMediaRepo", "Error creating delete PendingIntent: ${e.message}", e)
            null
        }
    }
}
