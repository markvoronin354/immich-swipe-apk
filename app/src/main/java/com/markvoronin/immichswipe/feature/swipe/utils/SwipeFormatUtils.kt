package com.markvoronin.immichswipe.feature.swipe.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import com.markvoronin.immichswipe.R
import java.util.Locale


@Composable
fun formatSize(bytes: Long): String {
    var value = bytes / 1024.0
    var unit = 0
    // Step up once the number would round to 4 digits, so the text never exceeds 3 digits.
    while (value >= 999.5 && unit < 3) {
        value /= 1024.0
        unit++
    }
    val decimals = when {
        unit == 0 || value >= 99.95 -> 0
        value >= 9.995 -> 1
        else -> 2
    }
    val number = String.format(LocalLocale.current.platformLocale, "%.${decimals}f", value)
    val unitRes = when (unit) {
        0 -> R.string.size_unit_kb
        1 -> R.string.size_unit_mb
        2 -> R.string.size_unit_gb
        else -> R.string.size_unit_tb
    }
    return stringResource(unitRes, number)
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
