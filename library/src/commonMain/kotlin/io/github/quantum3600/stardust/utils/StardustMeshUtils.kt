package io.github.quantum3600.stardust.utils

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.random.Random

/** Generated mesh points and colors. */
class MeshData(
    val points: Array<Offset>,
    val colors: Array<Color>,
) {
    operator fun component1() = points
    operator fun component2() = colors
    fun copy(points: Array<Offset> = this.points, colors: Array<Color> = this.colors) = MeshData(points, colors)

    override fun equals(other: Any?): Boolean =
        other is MeshData && points.contentEquals(other.points) && colors.contentEquals(other.colors)

    override fun hashCode(): Int = 31 * points.contentHashCode() + colors.contentHashCode()
}

/**
 * Grid of points in normalized space (origin top-left, y down). Only inner points are randomized;
 * border points stay put so the mesh always fills its container.
 */
fun generateMeshPoints(
    width: Int,
    height: Int,
    randomness: Float = 0.1f,
    random: Random = Random.Default,
): Array<Offset> {
    require(width >= 2 && height >= 2) { "Mesh needs at least 2x2 points" }
    require(randomness in 0f..1f) { "Randomness must be between 0.0 and 1.0" }

    return Array(width * height) { i ->
        val col = i % width
        val row = i / width // was `i / height`, which is wrong for non-square grids

        val isBorder = row == 0 || row == height - 1 || col == 0 || col == width - 1
        val pointX = col.toFloat() / (width - 1)
        val pointY = row.toFloat() / (height - 1)

        if (isBorder) {
            Offset(pointX, pointY)
        } else {
            val rx = (random.nextFloat() * 2f - 1f) * randomness
            val ry = (random.nextFloat() * 2f - 1f) * randomness
            Offset((pointX + rx).coerceIn(0f, 1f), (pointY + ry).coerceIn(0f, 1f))
        }
    }
}

/**
 * Colors for a mesh from one or more base colors. With 2+ colors they are spread as evenly spaced stops
 * of a top-to-bottom linear (sRGB) gradient, with optional HSV jitter per point.
 *
 * Need an exact color per control point? Skip this function and pass your own `width * height` colors
 * (e.g. straight to `rememberSimplexAnimatedMesh`).
 */
fun generateMeshColors(
    baseColors: List<Color>,
    width: Int,
    height: Int,
    colorVariance: Float = 0.0f,
    random: Random = Random.Default,
): Array<Color> {
    require(width >= 2 && height >= 2) { "Mesh needs at least 2x2 points" }
    require(baseColors.isNotEmpty()) { "baseColors must contain at least 1 color." }
    require(colorVariance in 0f..1f) { "colorVariance must be between 0.0 and 1.0" }

    val n = baseColors.size
    return Array(width * height) { i ->
        val row = i / width
        val c = if (n == 1) {
            baseColors[0]
        } else {
            val pos = row.toFloat() / (height - 1) * (n - 1)
            val idx = pos.toInt().coerceAtMost(n - 2)
            lerp(baseColors[idx], baseColors[idx + 1], pos - idx)
        }
        if (colorVariance > 0f) applyColorVariance(c, colorVariance, random) else c
    }
}

/** Remembers mesh data and picks the light/dark colors from the system theme. */
@Composable
fun rememberThemedMeshData(
    lightThemeColors: List<Color>,
    darkThemeColors: List<Color>,
    width: Int,
    height: Int,
    pointRandomness: Float = 0.1f,
    colorVariance: Float = 0.0f,
): MeshData {
    val isDark = isSystemInDarkTheme()
    return remember(isDark, width, height, pointRandomness, colorVariance, lightThemeColors, darkThemeColors) {
        MeshData(
            points = generateMeshPoints(width, height, pointRandomness),
            colors = generateMeshColors(
                baseColors = if (isDark) darkThemeColors else lightThemeColors,
                width = width,
                height = height,
                colorVariance = colorVariance,
            ),
        )
    }
}

// ---- Pure-Kotlin HSV (replaces android.graphics.Color.colorToHSV / HSVToColor) ----

/** Jitters hue (+/-10 deg) and value (+/-0.2) scaled by [variance]; alpha is preserved. */
private fun applyColorVariance(color: Color, variance: Float, random: Random): Color {
    val (h, s, v) = color.toHsv()
    val hueShift = (random.nextFloat() * 2f - 1f) * 10f * variance
    val valueShift = (random.nextFloat() * 2f - 1f) * 0.2f * variance
    return hsvToColor((h + hueShift).mod(360f), s, (v + valueShift).coerceIn(0f, 1f), color.alpha)
}

private fun Color.toHsv(): Triple<Float, Float, Float> {
    val r = red; val g = green; val b = blue
    val max = maxOf(r, g, b)
    val d = max - minOf(r, g, b)
    val h = when {
        d == 0f -> 0f
        max == r -> 60f * (((g - b) / d).mod(6f))
        max == g -> 60f * ((b - r) / d + 2f)
        else -> 60f * ((r - g) / d + 4f)
    }
    return Triple(h, if (max == 0f) 0f else d / max, max)
}

private fun hsvToColor(h: Float, s: Float, v: Float, alpha: Float): Color {
    val c = v * s
    val hp = h.mod(360f) / 60f
    val x = c * (1f - abs(hp % 2f - 1f))
    val m = v - c
    val (r, g, b) = when (hp.toInt()) {
        0 -> Triple(c, x, 0f)
        1 -> Triple(x, c, 0f)
        2 -> Triple(0f, c, x)
        3 -> Triple(0f, x, c)
        4 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(
        (r + m).coerceIn(0f, 1f), (g + m).coerceIn(0f, 1f), (b + m).coerceIn(0f, 1f), alpha,
    )
}
