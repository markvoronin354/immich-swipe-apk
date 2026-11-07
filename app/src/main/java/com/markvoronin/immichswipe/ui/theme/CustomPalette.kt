@file:Suppress("MatchingDeclarationName")

package com.markvoronin.immichswipe.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.markvoronin.immichswipe.core.PaletteStyle
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin

// Seed-color scheme generation, adapted from Orion Store's palette maker (RookieEnough/Orion-Store, PaletteMaker.kt).

/** Hue and chroma live in OKLCH, which holds a hue steady from light to dark; tone is CIELAB L*, the scale M3 tones
 *  use, so roles keep the same contrast as the built-in palettes. */
internal object Tone {
    private fun linear(c: Double) = if (c <= .04045) c / 12.92 else ((c + .055) / 1.055).pow(2.4)
    private fun gamma(c: Double) = if (c <= .0031308) 12.92 * c else 1.055 * c.pow(1 / 2.4) - .055

    /** OKLCH chroma and hue (degrees) of an sRGB color. */
    fun lch(argb: Int): Pair<Double, Double> {
        val r = linear(((argb shr 16) and 255) / 255.0); val g = linear(((argb shr 8) and 255) / 255.0); val b = linear((argb and 255) / 255.0)
        val l = cbrt(.4122214708 * r + .5363325363 * g + .0514459929 * b)
        val m = cbrt(.2119034982 * r + .6806995451 * g + .1073969566 * b)
        val s = cbrt(.0883024619 * r + .2817188376 * g + .6299787005 * b)
        val a = 1.9779984951 * l - 2.4285922050 * m + .4505937099 * s
        val bb = .0259040371 * l + .7827717662 * m - .8086757660 * s
        return hypot(a, bb) to (Math.toDegrees(atan2(bb, a)) + 360) % 360
    }

    private fun rgb(lightness: Double, a: Double, b: Double): DoubleArray {
        val l = (lightness + .3963377774 * a + .2158037573 * b).let { it * it * it }
        val m = (lightness - .1055613458 * a - .0638541728 * b).let { it * it * it }
        val s = (lightness - .0894841775 * a - 1.2914855480 * b).let { it * it * it }
        return doubleArrayOf(4.0767416621 * l - 3.3077115913 * m + .2309699292 * s, -1.2684380046 * l + 2.6097574011 * m - .3413193965 * s,
            -.0041960863 * l - .7034186147 * m + 1.7076147010 * s)
    }

    /** The color at [tone] (L*, 0..100) with this hue, at [chroma] or the most sRGB can show at that tone. */
    fun at(hue: Double, chroma: Double, tone: Double): Int {
        if (tone <= 0) return 0xFF000000.toInt()
        if (tone >= 100) return 0xFFFFFFFF.toInt()
        val y = if (tone > 8) ((tone + 16) / 116).pow(3) else tone / 903.2962962
        val ca = cos(Math.toRadians(hue)); val sa = sin(Math.toRadians(hue))
        fun solve(c: Double): DoubleArray {
            var lo = 0.0; var hi = 1.0; var out = rgb(.5, c * ca, c * sa)
            repeat(22) {
                val mid = (lo + hi) / 2; out = rgb(mid, c * ca, c * sa)
                if (.2126 * out[0] + .7152 * out[1] + .0722 * out[2] < y) lo = mid else hi = mid
            }
            return out
        }
        fun fits(p: DoubleArray) = p.all { it in -1e-4..1.0001 }
        var best = solve(chroma)
        if (!fits(best)) {
            best = solve(0.0); var lo = 0.0; var hi = chroma
            repeat(14) { val mid = (lo + hi) / 2; val p = solve(mid); if (fits(p)) { best = p; lo = mid } else hi = mid }
        }
        fun ch(v: Double) = (gamma(v.coerceIn(0.0, 1.0)) * 255).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (ch(best[0]) shl 16) or (ch(best[1]) shl 8) or ch(best[2])
    }
}

private class Tonal(val hue: Double, val chroma: Double) {
    fun t(n: Int) = Color(Tone.at(hue, chroma, n.toDouble()))
}

private val ErrorTones = Tonal(27.0, .18)

/** Seeds at or below this chroma give a grey palette. */
internal const val GREY_CHROMA = .03
/** Colored seeds are raised to at least this chroma so the palette never looks washed out. */
internal const val MIN_CHROMA = .1

/** Light or dark scheme built from one seed color; secondary, tertiary and neutrals are derived from its hue.
 *  [style] scales the primary's chroma (secondary and tertiary follow it) and the surface tint, as in Orion. */
fun customColorScheme(seed: Int, dark: Boolean, style: PaletteStyle): ColorScheme {
    val (c0, h) = Tone.lch(seed)
    val pc = when (style) {
        PaletteStyle.SOFT -> c0 * .55
        PaletteStyle.BALANCED -> if (c0 > GREY_CHROMA) maxOf(c0, MIN_CHROMA) else c0
        PaletteStyle.VIVID -> if (c0 > GREY_CHROMA) maxOf(c0 * 1.3, .15) else c0
    }.coerceAtMost(.32)
    val p = Tonal(h, pc)
    val s = Tonal(h, (pc * .35).coerceAtMost(.06))
    val t = Tonal((h + 60) % 360, pc * .8)
    val nk = when (style) {
        PaletteStyle.SOFT -> .006
        PaletteStyle.BALANCED -> .012
        PaletteStyle.VIVID -> .02
    }.coerceAtMost(pc)
    val n = Tonal(h, nk); val nv = Tonal(h, nk * 2); val e = ErrorTones
    return if (dark) darkColorScheme(
        primary = p.t(80), onPrimary = p.t(20), primaryContainer = p.t(30), onPrimaryContainer = p.t(90), inversePrimary = p.t(40),
        secondary = s.t(80), onSecondary = s.t(20), secondaryContainer = s.t(30), onSecondaryContainer = s.t(90),
        tertiary = t.t(80), onTertiary = t.t(20), tertiaryContainer = t.t(30), onTertiaryContainer = t.t(90),
        background = n.t(6), onBackground = n.t(90), surface = n.t(6), onSurface = n.t(90), surfaceVariant = nv.t(30), onSurfaceVariant = nv.t(80),
        surfaceTint = Color.Transparent, inverseSurface = n.t(90), inverseOnSurface = n.t(20), error = e.t(80), onError = e.t(20), errorContainer = e.t(30),
        onErrorContainer = e.t(90), outline = nv.t(60), outlineVariant = nv.t(30), scrim = Color.Black, surfaceBright = n.t(24), surfaceContainer = n.t(12),
        surfaceContainerHigh = n.t(17), surfaceContainerHighest = n.t(22), surfaceContainerLow = n.t(10), surfaceContainerLowest = n.t(4), surfaceDim = n.t(6),
    ) else lightColorScheme(
        primary = p.t(40), onPrimary = p.t(100), primaryContainer = p.t(90), onPrimaryContainer = p.t(10), inversePrimary = p.t(80),
        secondary = s.t(40), onSecondary = s.t(100), secondaryContainer = s.t(90), onSecondaryContainer = s.t(10),
        tertiary = t.t(40), onTertiary = t.t(100), tertiaryContainer = t.t(90), onTertiaryContainer = t.t(10),
        background = n.t(98), onBackground = n.t(10), surface = n.t(98), onSurface = n.t(10), surfaceVariant = nv.t(90), onSurfaceVariant = nv.t(30),
        surfaceTint = Color.Transparent, inverseSurface = n.t(20), inverseOnSurface = n.t(95), error = e.t(40), onError = e.t(100), errorContainer = e.t(90),
        onErrorContainer = e.t(10), outline = nv.t(50), outlineVariant = nv.t(80), scrim = Color.Black, surfaceBright = n.t(98), surfaceContainer = n.t(94),
        surfaceContainerHigh = n.t(92), surfaceContainerHighest = n.t(90), surfaceContainerLow = n.t(96), surfaceContainerLowest = n.t(100), surfaceDim = n.t(87),
    )
}
