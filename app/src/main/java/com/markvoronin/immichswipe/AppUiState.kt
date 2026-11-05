package com.markvoronin.immichswipe

import com.markvoronin.immichswipe.core.AppPalette
import com.markvoronin.immichswipe.core.AppTheme


data class AppUiState(
    val isLoading: Boolean = true,
    val isLoggedIn: Boolean = false,
    val activeUserId: String? = null,
    val themeMode: AppTheme = AppTheme.SYSTEM,
    val palette: AppPalette = AppPalette.WALLPAPER
)
