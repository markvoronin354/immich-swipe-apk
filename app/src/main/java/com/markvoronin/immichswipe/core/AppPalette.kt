package com.markvoronin.immichswipe.core

import android.os.Build
import androidx.annotation.StringRes
import com.markvoronin.immichswipe.R

/** Every palette except [WALLPAPER] is generated from a fixed seed; [WALLPAPER] follows the system colors. */
enum class AppPalette(val seed: Long, @StringRes val labelRes: Int) {
    AURORA(0xFF5B5BD6, R.string.settings_palette_aurora),
    OCEAN(0xFF0E8A9A, R.string.settings_palette_ocean),
    EMBER(0xFFE0603A, R.string.settings_palette_ember),
    MEADOW(0xFF4E8F3A, R.string.settings_palette_meadow),
    GRAPHITE(0xFF607080, R.string.settings_palette_graphite),
    WALLPAPER(0xFF5B5BD6, R.string.settings_palette_wallpaper);

    val isAvailable: Boolean
        get() = this != WALLPAPER || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
}
