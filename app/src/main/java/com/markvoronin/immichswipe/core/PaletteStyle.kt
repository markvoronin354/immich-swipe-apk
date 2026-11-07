package com.markvoronin.immichswipe.core

import androidx.annotation.StringRes
import com.markvoronin.immichswipe.R

/** How vivid the custom palette is: scales the primary's chroma, which secondary and tertiary follow, and the surface tint. */
enum class PaletteStyle(@StringRes val labelRes: Int) {
    SOFT(R.string.settings_palette_style_soft),
    BALANCED(R.string.settings_palette_style_balanced),
    VIVID(R.string.settings_palette_style_vivid)
}
