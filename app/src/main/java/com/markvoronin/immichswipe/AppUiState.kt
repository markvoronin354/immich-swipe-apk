package com.markvoronin.immichswipe

import com.markvoronin.immichswipe.core.AppPalette
import com.markvoronin.immichswipe.core.AppTheme
import com.markvoronin.immichswipe.core.PaletteStyle


data class AppUiState(
    val isLoading: Boolean = true,
    val isLoggedIn: Boolean = false,
    val activeUserId: String? = null,
    val themeMode: AppTheme = AppTheme.SYSTEM,
    val palette: AppPalette = AppPalette.WALLPAPER,
    val customPaletteSeed: Int = AppPalette.DEFAULT_CUSTOM_SEED,
    val customPaletteStyle: PaletteStyle = PaletteStyle.BALANCED
)
