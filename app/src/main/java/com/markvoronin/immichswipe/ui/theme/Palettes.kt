package com.markvoronin.immichswipe.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.markvoronin.immichswipe.core.AppPalette

// Ocean, Forest, Sunset, Rose and Mono color schemes, taken from Orion Store (RookieEnough/Orion-Store).

private val OceanLight = lightColorScheme(
    primary = Color(0xFF006592), onPrimary = Color(0xFFF5F9FF), primaryContainer = Color(0xFF31B8FF), onPrimaryContainer = Color(0xFF00324A),
    inversePrimary = Color(0xFF31B8FF), secondary = Color(0xFF4A6274), onSecondary = Color(0xFFF5F9FF), secondaryContainer = Color(0xFFCDE6FB),
    onSecondaryContainer = Color(0xFF3D5466), tertiary = Color(0xFF9C414F), onTertiary = Color(0xFFFFF7F7), tertiaryContainer = Color(0xFFFD8D9A),
    onTertiaryContainer = Color(0xFF5D1021), background = Color(0xFFF8F9FD), onBackground = Color(0xFF2C3339), surface = Color(0xFFF8F9FD),
    onSurface = Color(0xFF2C3339), surfaceVariant = Color(0xFFDDE3EB), onSurfaceVariant = Color(0xFF596066), surfaceTint = Color.Transparent,
    inverseSurface = Color(0xFF0B0F11), inverseOnSurface = Color(0xFF9B9DA1), error = Color(0xFFA83836), onError = Color(0xFFFFF7F6),
    errorContainer = Color(0xFFFA746F), onErrorContainer = Color(0xFF6E0A12), outline = Color(0xFF757B82), outlineVariant = Color(0xFFACB3BA),
    scrim = Color(0xFF000000), surfaceBright = Color(0xFFF8F9FD), surfaceContainer = Color(0xFFEAEEF4), surfaceContainerHigh = Color(0xFFE3E9EF),
    surfaceContainerHighest = Color(0xFFDDE3EB), surfaceContainerLow = Color(0xFFF0F4F9), surfaceContainerLowest = Color(0xFFFFFFFF), surfaceDim = Color(0xFFD4DBE2),
)

private val OceanDark = darkColorScheme(
    primary = Color(0xFF89CEFF), onPrimary = Color(0xFF004564), primaryContainer = Color(0xFF00A8EE), onPrimaryContainer = Color(0xFF002335),
    inversePrimary = Color(0xFF006591), secondary = Color(0xFFB1CADE), onSecondary = Color(0xFF2B4354), secondaryContainer = Color(0xFF263E4F),
    onSecondaryContainer = Color(0xFFAAC2D7), tertiary = Color(0xFFFFA2AB), onTertiary = Color(0xFF6A1B2A), tertiaryContainer = Color(0xFFFD8D9A),
    onTertiaryContainer = Color(0xFF5D1021), background = Color(0xFF0B0F11), onBackground = Color(0xFFDFE6EE), surface = Color(0xFF0B0F11),
    onSurface = Color(0xFFDFE6EE), surfaceVariant = Color(0xFF20272C), onSurfaceVariant = Color(0xFFA5ACB3), surfaceTint = Color.Transparent,
    inverseSurface = Color(0xFFF8F9FD), inverseOnSurface = Color(0xFF525559), error = Color(0xFFFA746F), onError = Color(0xFF490006),
    errorContainer = Color(0xFF871F21), onErrorContainer = Color(0xFFFF9993), outline = Color(0xFF6F767D), outlineVariant = Color(0xFF42494F),
    scrim = Color(0xFF000000), surfaceBright = Color(0xFF262D33), surfaceContainer = Color(0xFF151A1E), surfaceContainerHigh = Color(0xFF1B2025),
    surfaceContainerHighest = Color(0xFF20272C), surfaceContainerLow = Color(0xFF0F1418), surfaceContainerLowest = Color(0xFF000000), surfaceDim = Color(0xFF0B0F11),
)

private val ForestLight = lightColorScheme(
    primary = Color(0xFF196D33), onPrimary = Color(0xFFE9FFE6), primaryContainer = Color(0xFFA2F6AB), onPrimaryContainer = Color(0xFF005F27),
    inversePrimary = Color(0xFFAAFEB3), secondary = Color(0xFF4D654E), onSecondary = Color(0xFFE9FFE6), secondaryContainer = Color(0xFFCEEACD),
    onSecondaryContainer = Color(0xFF405741), tertiary = Color(0xFF865400), onTertiary = Color(0xFFFFF7F3), tertiaryContainer = Color(0xFFFFAD3B),
    onTertiaryContainer = Color(0xFF543300), background = Color(0xFFF8FAF3), onBackground = Color(0xFF2D342C), surface = Color(0xFFF8FAF3),
    onSurface = Color(0xFF2D342C), surfaceVariant = Color(0xFFDDE5D9), onSurfaceVariant = Color(0xFF596158), surfaceTint = Color.Transparent,
    inverseSurface = Color(0xFF0C0F0B), inverseOnSurface = Color(0xFF9B9E98), error = Color(0xFFA73B21), onError = Color(0xFFFFF7F6),
    errorContainer = Color(0xFFFD795A), onErrorContainer = Color(0xFF6E1400), outline = Color(0xFF757D73), outlineVariant = Color(0xFFACB4A9),
    scrim = Color(0xFF000000), surfaceBright = Color(0xFFF8FAF3), surfaceContainer = Color(0xFFEAF0E6), surfaceContainerHigh = Color(0xFFE4EAE0),
    surfaceContainerHighest = Color(0xFFDDE5D9), surfaceContainerLow = Color(0xFFF1F5EC), surfaceContainerLowest = Color(0xFFFFFFFF), surfaceDim = Color(0xFFD4DCD1),
)

private val ForestDark = darkColorScheme(
    primary = Color(0xFF87D991), onPrimary = Color(0xFF004B1D), primaryContainer = Color(0xFF016027), onPrimaryContainer = Color(0xFFA3F6AC),
    inversePrimary = Color(0xFF196D33), secondary = Color(0xFFB3CEB1), onSecondary = Color(0xFF2F4631), secondaryContainer = Color(0xFF2A412C),
    onSecondaryContainer = Color(0xFFACC6AA), tertiary = Color(0xFFFFC073), onTertiary = Color(0xFF613C00), tertiaryContainer = Color(0xFFFFAD3B),
    onTertiaryContainer = Color(0xFF543300), background = Color(0xFF0C0F0B), onBackground = Color(0xFFE0E8DC), surface = Color(0xFF0C0F0B),
    onSurface = Color(0xFFE0E8DC), surfaceVariant = Color(0xFF202820), onSurfaceVariant = Color(0xFFA5ADA2), surfaceTint = Color.Transparent,
    inverseSurface = Color(0xFFF8FAF3), inverseOnSurface = Color(0xFF525651), error = Color(0xFFF97758), onError = Color(0xFF450900),
    errorContainer = Color(0xFF85230A), onErrorContainer = Color(0xFFFF9B82), outline = Color(0xFF70786E), outlineVariant = Color(0xFF424A41),
    scrim = Color(0xFF000000), surfaceBright = Color(0xFF272E26), surfaceContainer = Color(0xFF151B15), surfaceContainerHigh = Color(0xFF1B211B),
    surfaceContainerHighest = Color(0xFF202820), surfaceContainerLow = Color(0xFF101510), surfaceContainerLowest = Color(0xFF000000), surfaceDim = Color(0xFF0C0F0B),
)

private val SunsetLight = lightColorScheme(
    primary = Color(0xFF9E4400), onPrimary = Color(0xFFFFF7F5), primaryContainer = Color(0xFFF77F37), onPrimaryContainer = Color(0xFF3F1700),
    inversePrimary = Color(0xFFF77F37), secondary = Color(0xFF7C5743), onSecondary = Color(0xFFFFF7F5), secondaryContainer = Color(0xFFFFDBCA),
    onSecondaryContainer = Color(0xFF6D4937), tertiary = Color(0xFF6950A4), onTertiary = Color(0xFFFDF7FF), tertiaryContainer = Color(0xFFBA9FFA),
    onTertiaryContainer = Color(0xFF371B70), background = Color(0xFFFFF8F6), onBackground = Color(0xFF3D2F29), surface = Color(0xFFFFF8F6),
    onSurface = Color(0xFF3D2F29), surfaceVariant = Color(0xFFF5DED3), onSurfaceVariant = Color(0xFF6D5C54), surfaceTint = Color.Transparent,
    inverseSurface = Color(0xFF130D0A), inverseOnSurface = Color(0xFFA79A95), error = Color(0xFFA73B21), onError = Color(0xFFFFF7F6),
    errorContainer = Color(0xFFFD795A), onErrorContainer = Color(0xFF6E1400), outline = Color(0xFF89776E), outlineVariant = Color(0xFFC3AEA4),
    scrim = Color(0xFF000000), surfaceBright = Color(0xFFFFF8F6), surfaceContainer = Color(0xFFFEEAE1), surfaceContainerHigh = Color(0xFFF9E4DB),
    surfaceContainerHighest = Color(0xFFF5DED3), surfaceContainerLow = Color(0xFFFFF1EB), surfaceContainerLowest = Color(0xFFFFFFFF), surfaceDim = Color(0xFFECD5CB),
)

private val SunsetDark = darkColorScheme(
    primary = Color(0xFFFFB690), onPrimary = Color(0xFF6E2D00), primaryContainer = Color(0xFFB54E00), onPrimaryContainer = Color(0xFFFFFFFF),
    inversePrimary = Color(0xFF9E4400), secondary = Color(0xFFEDBCA4), onSecondary = Color(0xFF593827), secondaryContainer = Color(0xFF543422),
    onSecondaryContainer = Color(0xFFE5B59D), tertiary = Color(0xFFC6AFFF), onTertiary = Color(0xFF41277B), tertiaryContainer = Color(0xFFBA9FFA),
    onTertiaryContainer = Color(0xFF371B70), background = Color(0xFF130D0A), onBackground = Color(0xFFF8E1D6), surface = Color(0xFF130D0A),
    onSurface = Color(0xFFF8E1D6), surfaceVariant = Color(0xFF30231C), onSurfaceVariant = Color(0xFFBCA79D), surfaceTint = Color.Transparent,
    inverseSurface = Color(0xFFFFF8F6), inverseOnSurface = Color(0xFF5D534E), error = Color(0xFFF97758), onError = Color(0xFF450900),
    errorContainer = Color(0xFF85230A), onErrorContainer = Color(0xFFFF9B82), outline = Color(0xFF847269), outlineVariant = Color(0xFF54453D),
    scrim = Color(0xFF000000), surfaceBright = Color(0xFF372922), surfaceContainer = Color(0xFF211712), surfaceContainerHigh = Color(0xFF281D18),
    surfaceContainerHighest = Color(0xFF30231C), surfaceContainerLow = Color(0xFF1A120D), surfaceContainerLowest = Color(0xFF000000), surfaceDim = Color(0xFF130D0A),
)

private val RoseLight = lightColorScheme(
    primary = Color(0xFFA3356A), onPrimary = Color(0xFFFFF7F7), primaryContainer = Color(0xFFFB7BB3), onPrimaryContainer = Color(0xFF54002F),
    inversePrimary = Color(0xFFFB7BB3), secondary = Color(0xFF795562), onSecondary = Color(0xFFFFF7F7), secondaryContainer = Color(0xFFFFD9E4),
    onSecondaryContainer = Color(0xFF6A4855), tertiary = Color(0xFF006B60), onTertiary = Color(0xFFE2FFF8), tertiaryContainer = Color(0xFF8DFFEC),
    onTertiaryContainer = Color(0xFF006258), background = Color(0xFFFFF8F8), onBackground = Color(0xFF3C2F33), surface = Color(0xFFFFF8F8),
    onSurface = Color(0xFF3C2F33), surfaceVariant = Color(0xFFF2DDE3), onSurfaceVariant = Color(0xFF6B5B60), surfaceTint = Color.Transparent,
    inverseSurface = Color(0xFF130D0F), inverseOnSurface = Color(0xFFA69A9D), error = Color(0xFFA8364B), onError = Color(0xFFFFF7F7),
    errorContainer = Color(0xFFF97386), onErrorContainer = Color(0xFF6E0523), outline = Color(0xFF87767B), outlineVariant = Color(0xFFC0ADB2),
    scrim = Color(0xFF000000), surfaceBright = Color(0xFFFFF8F8), surfaceContainer = Color(0xFFFCE9EE), surfaceContainerHigh = Color(0xFFF7E3E8),
    surfaceContainerHighest = Color(0xFFF2DDE3), surfaceContainerLow = Color(0xFFFFF0F3), surfaceContainerLowest = Color(0xFFFFFFFF), surfaceDim = Color(0xFFEAD5DA),
)

private val RoseDark = darkColorScheme(
    primary = Color(0xFFFFB0CD), onPrimary = Color(0xFF7B124A), primaryContainer = Color(0xFF93285D), onPrimaryContainer = Color(0xFFFFDAE5),
    inversePrimary = Color(0xFFA3356A), secondary = Color(0xFFE8BBC9), onSecondary = Color(0xFF573743), secondaryContainer = Color(0xFF52323E),
    onSecondaryContainer = Color(0xFFE0B3C2), tertiary = Color(0xFFE5FFF9), onTertiary = Color(0xFF006B60), tertiaryContainer = Color(0xFF8DFFEC),
    onTertiaryContainer = Color(0xFF006258), background = Color(0xFF130D0F), onBackground = Color(0xFFF5E0E6), surface = Color(0xFF130D0F),
    onSurface = Color(0xFFF5E0E6), surfaceVariant = Color(0xFF2F2227), onSurfaceVariant = Color(0xFFB9A6AB), surfaceTint = Color.Transparent,
    inverseSurface = Color(0xFFFFF8F8), inverseOnSurface = Color(0xFF5C5355), error = Color(0xFFF97386), onError = Color(0xFF490013),
    errorContainer = Color(0xFF871C34), onErrorContainer = Color(0xFFFF97A3), outline = Color(0xFF827176), outlineVariant = Color(0xFF524449),
    scrim = Color(0xFF000000), surfaceBright = Color(0xFF35292D), surfaceContainer = Color(0xFF20171A), surfaceContainerHigh = Color(0xFF271D20),
    surfaceContainerHighest = Color(0xFF2F2227), surfaceContainerLow = Color(0xFF191114), surfaceContainerLowest = Color(0xFF000000), surfaceDim = Color(0xFF130D0F),
)

private val MonoLight = lightColorScheme(
    primary = Color(0xFF1F2024), onPrimary = Color(0xFFFFFFFF), primaryContainer = Color(0xFFC4C6CE), onPrimaryContainer = Color(0xFF111215),
    inversePrimary = Color(0xFFFFFFFF), secondary = Color(0xFF3A3C41), onSecondary = Color(0xFFFFFFFF), secondaryContainer = Color(0xFFD3D5DC),
    onSecondaryContainer = Color(0xFF1A1B1F), tertiary = Color(0xFF4A4C52), onTertiary = Color(0xFFFFFFFF), tertiaryContainer = Color(0xFFE0E2E9),
    onTertiaryContainer = Color(0xFF1F2024), background = Color(0xFFF9F9FE), onBackground = Color(0xFF2E333A), surface = Color(0xFFF9F9FE),
    onSurface = Color(0xFF2E333A), surfaceVariant = Color(0xFFDFE2EC), onSurfaceVariant = Color(0xFF5B5F67), surfaceTint = Color.Transparent,
    inverseSurface = Color(0xFF0C0E12), inverseOnSurface = Color(0xFF9C9DA1), error = Color(0xFFA83836), onError = Color(0xFFFFF7F6),
    errorContainer = Color(0xFFFA746F), onErrorContainer = Color(0xFF6E0A12), outline = Color(0xFF767B83), outlineVariant = Color(0xFFAEB2BB),
    scrim = Color(0xFF000000), surfaceBright = Color(0xFFF9F9FE), surfaceContainer = Color(0xFFEBEEF5), surfaceContainerHigh = Color(0xFFE5E8F0),
    surfaceContainerHighest = Color(0xFFDFE2EC), surfaceContainerLow = Color(0xFFF2F3FA), surfaceContainerLowest = Color(0xFFFFFFFF), surfaceDim = Color(0xFFD6DAE4),
)

private val MonoDark = darkColorScheme(
    primary = Color(0xFFFFFFFF), onPrimary = Color(0xFF1A1B1F), primaryContainer = Color(0xFF6B6E75), onPrimaryContainer = Color(0xFFFFFFFF),
    inversePrimary = Color(0xFF3A3C41), secondary = Color(0xFFE3E4EA), onSecondary = Color(0xFF1A1B1F), secondaryContainer = Color(0xFF55585F),
    onSecondaryContainer = Color(0xFFF2F3F8), tertiary = Color(0xFFFFFFFF), onTertiary = Color(0xFF2A2C31), tertiaryContainer = Color(0xFFD4D6DD),
    onTertiaryContainer = Color(0xFF1F2024), background = Color(0xFF0C0E12), onBackground = Color(0xFFE2E5EF), surface = Color(0xFF0C0E12),
    onSurface = Color(0xFFE2E5EF), surfaceVariant = Color(0xFF21262D), onSurfaceVariant = Color(0xFFA7ABB4), surfaceTint = Color.Transparent,
    inverseSurface = Color(0xFFF9F9FE), inverseOnSurface = Color(0xFF535559), error = Color(0xFFFA746F), onError = Color(0xFF490006),
    errorContainer = Color(0xFF871F21), onErrorContainer = Color(0xFFFF9993), outline = Color(0xFF71767E), outlineVariant = Color(0xFF444850),
    scrim = Color(0xFF000000), surfaceBright = Color(0xFF282C34), surfaceContainer = Color(0xFF161A1F), surfaceContainerHigh = Color(0xFF1C2026),
    surfaceContainerHighest = Color(0xFF21262D), surfaceContainerLow = Color(0xFF111418), surfaceContainerLowest = Color(0xFF000000), surfaceDim = Color(0xFF0C0E12),
)

/** Scheme for every palette except [AppPalette.WALLPAPER], which the theme resolves from the system colors. */
fun AppPalette.colorScheme(dark: Boolean): ColorScheme = when (this) {
    AppPalette.OCEAN -> if (dark) OceanDark else OceanLight
    AppPalette.FOREST -> if (dark) ForestDark else ForestLight
    AppPalette.SUNSET -> if (dark) SunsetDark else SunsetLight
    AppPalette.ROSE -> if (dark) RoseDark else RoseLight
    AppPalette.MONO, AppPalette.WALLPAPER -> if (dark) MonoDark else MonoLight
}
