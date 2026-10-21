package com.markvoronin.immichswipe.core

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.net.toUri
import com.markvoronin.immichswipe.R

object ImmichLauncher {
    private const val IMMICH_PACKAGE_NAME = "app.alextran.immich"


    fun openAssetInImmich(
        context: Context,
        baseUrl: String?,
        assetId: String,
        mode: ImmichOpenMode = ImmichOpenMode.APP
    ) {
        if (assetId.isBlank()) return
        AppLogger.i("ImmichLauncher", "Opening asset $assetId in Immich (mode=$mode)")

        if (mode == ImmichOpenMode.WEB) {
            if (!baseUrl.isNullOrBlank()) {
                openInWeb(context, baseUrl, assetId)
            } else {
                Toast.makeText(context, context.getString(R.string.launcher_no_server_url), Toast.LENGTH_SHORT).show()
            }
            return
        }

        val cleanBaseUrl = baseUrl?.removeSuffix("/") ?: ""
        val webUri = if (cleanBaseUrl.isNotBlank()) "$cleanBaseUrl/photos/$assetId".toUri() else null
        val customUri = "immich://asset?id=$assetId".toUri()

        var handled = tryLaunchAppUri(context, customUri)
        if (!handled && webUri != null) {
            handled = tryLaunchAppUri(context, webUri)
        }

        if (!handled) {
            if (webUri != null) {
                openInWeb(context, cleanBaseUrl, assetId)
            } else {
                openPlayStore(context)
            }
        }
    }

    private fun tryLaunchAppUri(context: Context, uri: Uri): Boolean {
        val appIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage(IMMICH_PACKAGE_NAME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(appIntent)
            true
        } catch (e: ActivityNotFoundException) {
            AppLogger.d("ImmichLauncher", "Failed to launch Immich app with URI ($uri): ${e.message}")
            false
        }
    }

    /**
     * Opens the web URL ($baseUrl/photos/$assetId) in standard browser.
     */
    fun openInWeb(context: Context, baseUrl: String, assetId: String) {
        if (baseUrl.isBlank()) return
        val cleanBaseUrl = baseUrl.removeSuffix("/")
        val url = "$cleanBaseUrl/photos/$assetId"
        val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            AppLogger.e("ImmichLauncher", "Failed to open web URL: $url", e)
            Toast.makeText(context, context.getString(R.string.launcher_no_browser), Toast.LENGTH_SHORT).show()
        }
    }

    private fun openPlayStore(context: Context) {
        try {
            val playStoreIntent = Intent(
                Intent.ACTION_VIEW,
                "market://details?id=$IMMICH_PACKAGE_NAME".toUri()
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(playStoreIntent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, context.getString(R.string.launcher_immich_not_installed), Toast.LENGTH_SHORT).show()
        }
    }
}
