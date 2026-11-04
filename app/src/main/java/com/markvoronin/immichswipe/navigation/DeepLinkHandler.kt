package com.markvoronin.immichswipe.navigation

import android.content.Intent
import android.net.Uri
import com.markvoronin.immichswipe.core.AppLogger
import com.markvoronin.immichswipe.domain.model.Album

/**
 * Handles deep links for navigation across the app.
 * Supports URIs like:
 * - immichswipe://swipe/{albumId}
 * - immichswipe://album/{albumId}
 * - immichswipe://duplicates
 * - immichswipe://settings
 */
object DeepLinkHandler {

    /**
     * Parses an incoming Intent and extracts a NavKey destination if a valid deep link URI is present.
     */
    fun parseIntent(intent: Intent?): NavKey? {
        val uriString = intent?.dataString ?: return null
        return parseUri(uriString)
    }

    fun parseUri(uri: Uri?): NavKey? {
        return parseUri(uri?.toString())
    }

    fun parseUri(uriString: String?): NavKey? {
        if (uriString.isNullOrBlank()) return null
        val javaUri = runCatching { java.net.URI.create(uriString) }.getOrNull() ?: return null
        val scheme = javaUri.scheme ?: return null
        if ((scheme != "immichswipe") && (scheme != "http") && (scheme != "https")) {
            return null
        }
        AppLogger.d("DeepLinkHandler", "Parsing deep link URI: $uriString")
        val host = javaUri.host ?: return null
        val pathSegments = javaUri.path?.split("/")?.filter { it.isNotEmpty() } ?: emptyList()

        return when (host) {
            "swipe", "album" -> {
                val queryId = parseQueryParameter(javaUri.query, "id")
                val rawId = pathSegments.firstOrNull() ?: queryId ?: Album.VIRTUAL_ALL_ID
                NavKey.Swipe(normalizeAlbumId(rawId))
            }
            "duplicates" -> NavKey.Duplicates
            "settings" -> NavKey.Settings
            "home" -> parseHomePath(pathSegments)
            else -> null
        }
    }

    private fun parseQueryParameter(query: String?, key: String): String? {
        if (query.isNullOrBlank()) return null
        return query.split("&")
            .map { it.split("=", limit = 2) }
            .firstOrNull { it[0] == key }
            ?.getOrNull(1)
    }

    private fun parseHomePath(pathSegments: List<String>): NavKey {
        val firstSegment = pathSegments.firstOrNull() ?: return NavKey.Home
        return when (firstSegment) {
            "swipe", "album" -> {
                val rawId = pathSegments.getOrNull(1) ?: Album.VIRTUAL_ALL_ID
                NavKey.Swipe(normalizeAlbumId(rawId))
            }
            "duplicates" -> NavKey.Duplicates
            "settings" -> NavKey.Settings
            else -> NavKey.Home
        }
    }

    private fun normalizeAlbumId(rawId: String): String {
        return when (rawId) {
            "virtual-all", "virtual_all", "all" -> Album.VIRTUAL_ALL_ID
            "virtual-orphans", "orphans" -> Album.VIRTUAL_ORPHANS_ID
            "virtual-duplicates", "duplicates" -> Album.VIRTUAL_DUPLICATES_ID
            else -> rawId
        }
    }
}
