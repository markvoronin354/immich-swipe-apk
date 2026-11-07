package com.markvoronin.immichswipe.core

import android.os.Build
import androidx.annotation.StringRes
import com.markvoronin.immichswipe.R

/** [WALLPAPER] follows the system colors, [CUSTOM] is generated from a user-picked seed; the rest have fixed colors. */
enum class AppPalette(@StringRes val labelRes: Int) {
    OCEAN(R.string.settings_palette_ocean),
    FOREST(R.string.settings_palette_forest),
    SUNSET(R.string.settings_palette_sunset),
    ROSE(R.string.settings_palette_rose),
    MONO(R.string.settings_palette_mono),
    WALLPAPER(R.string.settings_palette_wallpaper),
    CUSTOM(R.string.settings_palette_custom);

    val isAvailable: Boolean
        get() = this != WALLPAPER || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    companion object {
        /** Seed for [CUSTOM] until the user picks one (indigo). */
        val DEFAULT_CUSTOM_SEED: Int = 0xFF6366F1.toInt()
    }
}
