package io.github.quantum3600.stardust.utils

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

/**
 * A data class to hold the generated mesh points and colors.
 */
data class MeshData(
    val points: Array<Offset>,
    val colors: Array<Color>
)

/**
 * Generates a grid of points for a mesh with customizable randomness applied only to inner points.
 * Boundary points are kept static to ensure the mesh fits its container perfectly.
 *
 * @param width The number of points horizontally.
 * @param height The number of points vertically.
 * @param randomness The amount of random offset to apply to each inner point. 0f is a perfect grid,
 * higher values create more distortion. A good range is 0f to 0.2f.
 * @return An array of [Offset] points for the mesh.
 */
fun generateMeshPoints(width: Int, height: Int, randomness: Float = 0.1f): Array<Offset> {
    require(width > 1 && height > 1) { "Mesh dimensions must be greater than 1" }
    require(randomness in 0f..1f) { "Randomness must be between 0.0 and 1.0" }

    return Array(width * height) { i ->
        val col = i % width
        val row = i / width

        val isBorder = row == 0 || row == height - 1 || col == 0 || col == width - 1

        val pointX = col.toFloat() / (width - 1).toFloat()
        val pointY = row.toFloat() / (height - 1).toFloat()

        if (isBorder) {
            // Border points are not randomized
            Offset(pointX, pointY)
        } else {
            // Inner points can be randomized
            val randomX = (Random.nextFloat() * 2f - 1f) * randomness
            val randomY = (Random.nextFloat() * 2f - 1f) * randomness
            Offset(
                x = (pointX + randomX).coerceIn(0f, 1f),
                y = (pointY + randomY).coerceIn(0f, 1f)
            )
        }
    }
}

/**
 * Generates a full color array for a mesh from a list of 1 or 2 base colors.
 *
 * @param baseColors A list containing one or two [Color] values.
 * @param width The number of points horizontally.
 * @param height The number of points vertically.
 * @param colorVariance A value from 0.0 to 1.0 that introduces random variations
 * to the hue and lightness of each color, creating a more textured gradient.
 * @return An array of [Color] values for the mesh.
 */
fun generateMeshColors(
    baseColors: List<Color>,
    width: Int,
    height: Int,
    colorVariance: Float = 0.0f
): Array<Color> {
    require(width > 1 && height > 1) { "Mesh dimensions must be greater than 1" }
    require(baseColors.isNotEmpty() && baseColors.size <= 2) { "baseColors list must contain 1 or 2 colors." }
    require(colorVariance in 0f..1f) { "colorVariance must be between 0.0 and 1.0" }

    if (baseColors.size == 1) {
        // If only one color, fill the mesh with it.
        return Array(width * height) { baseColors[0] }
    }

    val startColor = baseColors[0]
    val endColor = baseColors[1]

    return Array(width * height) { i ->
        val row = i / width
        // Interpolate based on the row number to create a vertical gradient
        val fraction = row.toFloat() / (height - 1).toFloat()
        val interpolatedColor = lerp(startColor, endColor, fraction)

        if (colorVariance > 0) {
            applyColorVariance(interpolatedColor, colorVariance)
        } else {
            interpolatedColor
        }
    }
}

/**
 * Generates a full color array for a mesh using 2D spatial grid coordinates
 * fx = col / (width - 1) and fy = row / (height - 1), smoothly blending colors across
 * 4 corners (top-left, top-right, bottom-left, bottom-right) to produce rich,
 * multi-directional (diagonal, radial, and 4-corner) color gradients.
 *
 * @param width The number of horizontal points.
 * @param height The number of vertical points.
 * @param topLeft Color for the top-left corner.
 * @param topRight Color for the top-right corner.
 * @param bottomLeft Color for the bottom-left corner.
 * @param bottomRight Color for the bottom-right corner.
 * @param colorVariance Optional random color shift applied to HSL components (0f to 1f).
 * @return An array of [Color] values of size width * height.
 */
fun generateSpatialGridColors(
    width: Int,
    height: Int,
    topLeft: Color,
    topRight: Color,
    bottomLeft: Color,
    bottomRight: Color,
    colorVariance: Float = 0.0f
): Array<Color> {
    require(width > 1 && height > 1) { "Mesh dimensions must be greater than 1" }
    require(colorVariance in 0f..1f) { "colorVariance must be between 0.0 and 1.0" }

    return Array(width * height) { i ->
        val col = i % width
        val row = i / width
        val fx = col.toFloat() / (width - 1).toFloat()
        val fy = row.toFloat() / (height - 1).toFloat()

        val top = lerp(topLeft, topRight, fx)
        val bottom = lerp(bottomLeft, bottomRight, fx)
        val blended = lerp(top, bottom, fy)

        if (colorVariance > 0f) {
            applyColorVariance(blended, colorVariance)
        } else {
            blended
        }
    }
}

/**
 * Computes default Catmull-Rom tangent vectors for a 2D grid of [points] of size [width] x [height].
 * Uses zero-allocation scalar float math.
 */
internal fun computeDefaultTangents(
    width: Int,
    height: Int,
    points: Array<Offset>,
    uTangentsOut: FloatArray,
    vTangentsOut: FloatArray
) {
    for (r in 0 until height) {
        val rOffset = r * width
        for (c in 0 until width) {
            val idx = rOffset + c
            val curr = points[idx]

            val uLeadingX: Float
            val uLeadingY: Float
            val uTrailingX: Float
            val uTrailingY: Float
            if (c > 0 && c < width - 1) {
                val prev = points[rOffset + (c - 1)]
                val next = points[rOffset + (c + 1)]
                uLeadingX = (prev.x - next.x) / 6f
                uLeadingY = (prev.y - next.y) / 6f
                uTrailingX = (next.x - prev.x) / 6f
                uTrailingY = (next.y - prev.y) / 6f
            } else if (c == 0) {
                val next = points[rOffset + 1]
                uLeadingX = (curr.x - next.x) / 3f
                uLeadingY = (curr.y - next.y) / 3f
                uTrailingX = (next.x - curr.x) / 3f
                uTrailingY = (next.y - curr.y) / 3f
            } else {
                val prev = points[rOffset + (width - 2)]
                uLeadingX = (prev.x - curr.x) / 3f
                uLeadingY = (prev.y - curr.y) / 3f
                uTrailingX = (curr.x - prev.x) / 3f
                uTrailingY = (curr.y - prev.y) / 3f
            }

            val vTopX: Float
            val vTopY: Float
            val vBottomX: Float
            val vBottomY: Float
            if (r > 0 && r < height - 1) {
                val prev = points[(r - 1) * width + c]
                val next = points[(r + 1) * width + c]
                vTopX = (prev.x - next.x) / 6f
                vTopY = (prev.y - next.y) / 6f
                vBottomX = (next.x - prev.x) / 6f
                vBottomY = (next.y - prev.y) / 6f
            } else if (r == 0) {
                val next = points[1 * width + c]
                vTopX = (curr.x - next.x) / 3f
                vTopY = (curr.y - next.y) / 3f
                vBottomX = (next.x - curr.x) / 3f
                vBottomY = (next.y - curr.y) / 3f
            } else {
                val prev = points[(height - 2) * width + c]
                vTopX = (prev.x - curr.x) / 3f
                vTopY = (prev.y - curr.y) / 3f
                vBottomX = (curr.x - prev.x) / 3f
                vBottomY = (curr.y - prev.y) / 3f
            }

            val uBase = idx * 4
            uTangentsOut[uBase + 0] = uLeadingX
            uTangentsOut[uBase + 1] = uLeadingY
            uTangentsOut[uBase + 2] = uTrailingX
            uTangentsOut[uBase + 3] = uTrailingY

            val vBase = idx * 4
            vTangentsOut[vBase + 0] = vTopX
            vTangentsOut[vBase + 1] = vTopY
            vTangentsOut[vBase + 2] = vBottomX
            vTangentsOut[vBase + 3] = vBottomY
        }
    }
}

/**
 * Computes default Catmull-Rom tangent vectors for a flat [points] FloatArray [x0, y0, x1, y1, ...].
 * Uses zero-allocation scalar float math.
 */
internal fun computeDefaultTangents(
    width: Int,
    height: Int,
    points: FloatArray,
    uTangentsOut: FloatArray,
    vTangentsOut: FloatArray
) {
    for (r in 0 until height) {
        val rOffset = r * width
        for (c in 0 until width) {
            val idx = rOffset + c
            val currPx = points[idx * 2]
            val currPy = points[idx * 2 + 1]

            val uLeadingX: Float
            val uLeadingY: Float
            val uTrailingX: Float
            val uTrailingY: Float
            if (c > 0 && c < width - 1) {
                val prevPx = points[(rOffset + (c - 1)) * 2]
                val prevPy = points[(rOffset + (c - 1)) * 2 + 1]
                val nextPx = points[(rOffset + (c + 1)) * 2]
                val nextPy = points[(rOffset + (c + 1)) * 2 + 1]
                uLeadingX = (prevPx - nextPx) / 6f
                uLeadingY = (prevPy - nextPy) / 6f
                uTrailingX = (nextPx - prevPx) / 6f
                uTrailingY = (nextPy - prevPy) / 6f
            } else if (c == 0) {
                val nextPx = points[(rOffset + 1) * 2]
                val nextPy = points[(rOffset + 1) * 2 + 1]
                uLeadingX = (currPx - nextPx) / 3f
                uLeadingY = (currPy - nextPy) / 3f
                uTrailingX = (nextPx - currPx) / 3f
                uTrailingY = (nextPy - currPy) / 3f
            } else {
                val prevPx = points[(rOffset + (width - 2)) * 2]
                val prevPy = points[(rOffset + (width - 2)) * 2 + 1]
                uLeadingX = (prevPx - currPx) / 3f
                uLeadingY = (prevPy - currPy) / 3f
                uTrailingX = (currPx - prevPx) / 3f
                uTrailingY = (currPy - prevPy) / 3f
            }

            val vTopX: Float
            val vTopY: Float
            val vBottomX: Float
            val vBottomY: Float
            if (r > 0 && r < height - 1) {
                val prevPx = points[((r - 1) * width + c) * 2]
                val prevPy = points[((r - 1) * width + c) * 2 + 1]
                val nextPx = points[((r + 1) * width + c) * 2]
                val nextPy = points[((r + 1) * width + c) * 2 + 1]
                vTopX = (prevPx - nextPx) / 6f
                vTopY = (prevPy - nextPy) / 6f
                vBottomX = (nextPx - prevPx) / 6f
                vBottomY = (nextPy - prevPy) / 6f
            } else if (r == 0) {
                val nextPx = points[(1 * width + c) * 2]
                val nextPy = points[(1 * width + c) * 2 + 1]
                vTopX = (currPx - nextPx) / 3f
                vTopY = (currPy - nextPy) / 3f
                vBottomX = (nextPx - currPx) / 3f
                vBottomY = (nextPy - currPy) / 3f
            } else {
                val prevPx = points[((height - 2) * width + c) * 2]
                val prevPy = points[((height - 2) * width + c) * 2 + 1]
                vTopX = (prevPx - currPx) / 3f
                vTopY = (prevPy - currPy) / 3f
                vBottomX = (currPx - prevPx) / 3f
                vBottomY = (currPy - prevPy) / 3f
            }

            val uBase = idx * 4
            uTangentsOut[uBase + 0] = uLeadingX
            uTangentsOut[uBase + 1] = uLeadingY
            uTangentsOut[uBase + 2] = uTrailingX
            uTangentsOut[uBase + 3] = uTrailingY

            val vBase = idx * 4
            vTangentsOut[vBase + 0] = vTopX
            vTangentsOut[vBase + 1] = vTopY
            vTangentsOut[vBase + 2] = vBottomX
            vTangentsOut[vBase + 3] = vBottomY
        }
    }
}

/**
 * A composable that remembers mesh data and automatically selects the correct color scheme
 * based on the system's light or dark theme.
 *
 * @param lightThemeColors The list of 1 or 2 base colors for the light theme.
 * @param darkThemeColors The list of 1 or 2 base colors for the dark theme.
 * @param width The width of the mesh (e.g., 2, 3, or 4).
 * @param height The height of the mesh (e.g., 2, 3, or 4).
 * @param pointRandomness The amount of random distortion to apply to the mesh points.
 * @param colorVariance The amount of random variation to apply to the mesh colors.
 * @return A [MeshData] object containing the points and colors for the current theme.
 */
@Composable
fun rememberThemedMeshData(
    lightThemeColors: List<Color>,
    darkThemeColors: List<Color>,
    width: Int,
    height: Int,
    pointRandomness: Float = 0.1f,
    colorVariance: Float = 0.0f
): MeshData {
    val isDark = isSystemInDarkTheme()

    return remember(isDark, width, height, pointRandomness, colorVariance, lightThemeColors, darkThemeColors) {
        val points = generateMeshPoints(width, height, pointRandomness)
        val colors = generateMeshColors(
            baseColors = if (isDark) darkThemeColors else lightThemeColors,
            width = width,
            height = height,
            colorVariance = colorVariance
        )
        MeshData(points, colors)
    }
}

/**
 * Applies random variance to a color's HSL values.
 */
private fun applyColorVariance(color: Color, variance: Float): Color {
    val hueShift = (Random.nextFloat() * 2f - 1f) * variance
    val lightnessShift = (Random.nextFloat() * 2f - 1f) * 0.2f * variance
    return Color(
        (color.red + hueShift).coerceIn(0f, 1f),
        (color.green + lightnessShift).coerceIn(0f, 1f),
        (color.blue - hueShift).coerceIn(0f, 1f),
        color.alpha
    )
}
