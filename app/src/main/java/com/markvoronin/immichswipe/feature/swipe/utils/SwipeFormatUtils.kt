package com.markvoronin.immichswipe.feature.swipe.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.markvoronin.immichswipe.R
import java.util.Locale


@Composable
fun formatSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> stringResource(R.string.size_unit_gb, gb)
        mb >= 1.0 -> stringResource(R.string.size_unit_mb, mb)
        else -> stringResource(R.string.size_unit_kb, kb)
    }
}

fun formatMediaTime(ms: Long): String {
    val totalSeconds = (ms / 1000).toInt()
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}

/** Parses Immich's "H:MM:SS.ffffff" asset duration into milliseconds; 0 if missing or malformed. */
fun parseDurationMs(duration: String?): Long {
    val parts = duration?.split(":") ?: return 0L
    if (parts.size != 3) return 0L
    val hours = parts[0].toLongOrNull() ?: return 0L
    val minutes = parts[1].toLongOrNull() ?: return 0L
    val seconds = parts[2].toDoubleOrNull() ?: return 0L
    return (hours * 3600 + minutes * 60) * 1000 + (seconds * 1000).toLong()
}
