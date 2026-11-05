package com.markvoronin.immichswipe.core

import android.os.Build
import androidx.annotation.StringRes
import com.markvoronin.immichswipe.R

/** Every palette except [WALLPAPER] has fixed colors; [WALLPAPER] follows the system colors. */
enum class AppPalette(@StringRes val labelRes: Int) {
    OCEAN(R.string.settings_palette_ocean),
    FOREST(R.string.settings_palette_forest),
    SUNSET(R.string.settings_palette_sunset),
    ROSE(R.string.settings_palette_rose),
    MONO(R.string.settings_palette_mono),
    WALLPAPER(R.string.settings_palette_wallpaper);

    val isAvailable: Boolean
        get() = this != WALLPAPER || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
}
