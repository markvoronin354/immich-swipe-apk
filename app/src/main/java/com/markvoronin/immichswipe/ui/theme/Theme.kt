package com.markvoronin.immichswipe.ui.theme

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import com.markvoronin.immichswipe.core.AppPalette
import com.markvoronin.immichswipe.core.PaletteStyle

private val DarkColorScheme = darkColorScheme(
    primary = Blue80,
    onPrimary = Color(0xFF00315B),
    primaryContainer = Color(0xff1a5487),
    onPrimaryContainer = Color(0xFFD3E3FD),
    secondary = BlueGrey80,
    onSecondary = Color(0xFF253140),
    secondaryContainer = Color(0xFF3B4758),
    onSecondaryContainer = Color(0xFFD7E3F8),
    tertiary = LightBlue80,
    onTertiary = Color(0xFF3B2948),
    tertiaryContainer = Color(0xFF523F5F),
    onTertiaryContainer = Color(0xFFF3DAFF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF111318),
    onBackground = Color(0xFFE1E2E8),
    surface = Color(0xFF111318),
    onSurface = Color(0xFFE1E2E8),
    surfaceContainerLowest = Color(0xFF14171E),
    surfaceContainerLow = Color(0xFF1B1E26),
    surfaceContainer = Color(0xFF222630),
    surfaceContainerHigh = Color(0xFF282C38),
    surfaceContainerHighest = Color(0xFF2F3442),
    surfaceVariant = Color(0xFF222630),
    onSurfaceVariant = Color(0xFFC4C6CF),
    outline = Color(0xFF8E9099),
    outlineVariant = Color(0xFF383C45),
    surfaceTint = Color.Transparent,
)

private val LightColorScheme = lightColorScheme(
    primary = Blue40,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xff94b9f4),
    onPrimaryContainer = Color(0xFF001C3B),
    secondary = BlueGrey40,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD7E3F8),
    onSecondaryContainer = Color(0xFF101C2B),
    tertiary = LightBlue40,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF3DAFF),
    onTertiaryContainer = Color(0xFF251431),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF8F9FE),
    onBackground = Color(0xFF191C20),
    surface = Color(0xFFF8F9FE),
    onSurface = Color(0xFF191C20),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF2F3F8),
    surfaceContainer = Color(0xFFECEEF4),
    surfaceContainerHigh = Color(0xFFE6E8EE),
    surfaceContainerHighest = Color(0xFFE0E2EC),
    surfaceVariant = Color(0xFFE0E2EC),
    onSurfaceVariant = Color(0xFF43474E),
    outline = Color(0xFF74777F),
    outlineVariant = Color(0xFFC4C6CF),
    surfaceTint = Color.Transparent,
)

@Composable
fun ImmichSwipeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    palette: AppPalette = AppPalette.WALLPAPER,
    customSeed: Int = AppPalette.DEFAULT_CUSTOM_SEED,
    customStyle: PaletteStyle = PaletteStyle.BALANCED,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val (lightScheme, darkScheme) = remember(palette, customSeed, customStyle, context) {
        when {
            palette == AppPalette.WALLPAPER && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                dynamicLightColorScheme(context) to dynamicDarkColorScheme(context)

            palette == AppPalette.WALLPAPER -> LightColorScheme to DarkColorScheme
            else -> palette.colorScheme(false, customSeed, customStyle) to palette.colorScheme(true, customSeed, customStyle)
        }
    }
    val darkFraction by animateFloatAsState(
        targetValue = if (darkTheme) 1f else 0f,
        animationSpec = MotionScheme.expressive().slowEffectsSpec(),
        label = "themeDarkFraction"
    )
    val colorScheme = when (darkFraction) {
        0f -> lightScheme
        1f -> darkScheme
        else -> lerp(lightScheme, darkScheme, darkFraction)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            // enableEdgeToEdge() in MainActivity handles window insets and system bar icon colors
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        motionScheme = MotionScheme.expressive(),
        content = content
    )
}

private fun lerp(start: ColorScheme, stop: ColorScheme, fraction: Float) = start.copy(
    primary = lerp(start.primary, stop.primary, fraction),
    onPrimary = lerp(start.onPrimary, stop.onPrimary, fraction),
    primaryContainer = lerp(start.primaryContainer, stop.primaryContainer, fraction),
    onPrimaryContainer = lerp(start.onPrimaryContainer, stop.onPrimaryContainer, fraction),
    inversePrimary = lerp(start.inversePrimary, stop.inversePrimary, fraction),
    secondary = lerp(start.secondary, stop.secondary, fraction),
    onSecondary = lerp(start.onSecondary, stop.onSecondary, fraction),
    secondaryContainer = lerp(start.secondaryContainer, stop.secondaryContainer, fraction),
    onSecondaryContainer = lerp(start.onSecondaryContainer, stop.onSecondaryContainer, fraction),
    tertiary = lerp(start.tertiary, stop.tertiary, fraction),
    onTertiary = lerp(start.onTertiary, stop.onTertiary, fraction),
    tertiaryContainer = lerp(start.tertiaryContainer, stop.tertiaryContainer, fraction),
    onTertiaryContainer = lerp(start.onTertiaryContainer, stop.onTertiaryContainer, fraction),
    background = lerp(start.background, stop.background, fraction),
    onBackground = lerp(start.onBackground, stop.onBackground, fraction),
    surface = lerp(start.surface, stop.surface, fraction),
    onSurface = lerp(start.onSurface, stop.onSurface, fraction),
    surfaceVariant = lerp(start.surfaceVariant, stop.surfaceVariant, fraction),
    onSurfaceVariant = lerp(start.onSurfaceVariant, stop.onSurfaceVariant, fraction),
    surfaceTint = lerp(start.surfaceTint, stop.surfaceTint, fraction),
    inverseSurface = lerp(start.inverseSurface, stop.inverseSurface, fraction),
    inverseOnSurface = lerp(start.inverseOnSurface, stop.inverseOnSurface, fraction),
    error = lerp(start.error, stop.error, fraction),
    onError = lerp(start.onError, stop.onError, fraction),
    errorContainer = lerp(start.errorContainer, stop.errorContainer, fraction),
    onErrorContainer = lerp(start.onErrorContainer, stop.onErrorContainer, fraction),
    outline = lerp(start.outline, stop.outline, fraction),
    outlineVariant = lerp(start.outlineVariant, stop.outlineVariant, fraction),
    scrim = lerp(start.scrim, stop.scrim, fraction),
    surfaceBright = lerp(start.surfaceBright, stop.surfaceBright, fraction),
    surfaceDim = lerp(start.surfaceDim, stop.surfaceDim, fraction),
    surfaceContainer = lerp(start.surfaceContainer, stop.surfaceContainer, fraction),
    surfaceContainerHigh = lerp(start.surfaceContainerHigh, stop.surfaceContainerHigh, fraction),
    surfaceContainerHighest = lerp(start.surfaceContainerHighest, stop.surfaceContainerHighest, fraction),
    surfaceContainerLow = lerp(start.surfaceContainerLow, stop.surfaceContainerLow, fraction),
    surfaceContainerLowest = lerp(start.surfaceContainerLowest, stop.surfaceContainerLowest, fraction),
    primaryFixed = lerp(start.primaryFixed, stop.primaryFixed, fraction),
    primaryFixedDim = lerp(start.primaryFixedDim, stop.primaryFixedDim, fraction),
    onPrimaryFixed = lerp(start.onPrimaryFixed, stop.onPrimaryFixed, fraction),
    onPrimaryFixedVariant = lerp(start.onPrimaryFixedVariant, stop.onPrimaryFixedVariant, fraction),
    secondaryFixed = lerp(start.secondaryFixed, stop.secondaryFixed, fraction),
    secondaryFixedDim = lerp(start.secondaryFixedDim, stop.secondaryFixedDim, fraction),
    onSecondaryFixed = lerp(start.onSecondaryFixed, stop.onSecondaryFixed, fraction),
    onSecondaryFixedVariant = lerp(start.onSecondaryFixedVariant, stop.onSecondaryFixedVariant, fraction),
    tertiaryFixed = lerp(start.tertiaryFixed, stop.tertiaryFixed, fraction),
    tertiaryFixedDim = lerp(start.tertiaryFixedDim, stop.tertiaryFixedDim, fraction),
    onTertiaryFixed = lerp(start.onTertiaryFixed, stop.onTertiaryFixed, fraction),
    onTertiaryFixedVariant = lerp(start.onTertiaryFixedVariant, stop.onTertiaryFixedVariant, fraction),
)
