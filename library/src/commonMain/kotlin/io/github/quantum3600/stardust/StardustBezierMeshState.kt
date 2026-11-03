package io.github.quantum3600.stardust

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import io.github.quantum3600.stardust.data.BezierPoint

/**
 * State holder for [StardustMesh] using explicit 2D [BezierPoint] grids and colors.
 * Allows programmatic manipulation of Bézier control positions and tangent handles.
 */
@Stable
class StardustBezierMeshState(
    initialPoints: List<List<BezierPoint>>,
    initialColors: List<List<Color>>
) {
    val height: Int = initialPoints.size
    val width: Int = initialPoints.firstOrNull()?.size ?: 0

    private val snapshotPoints = FloatArray(width * height * 2)
    private val snapshotColors = FloatArray(width * height * 4)
    private val snapshotUTangents = FloatArray(MeshGradientShader.maxControlPoints * 4)
    private val snapshotVTangents = FloatArray(MeshGradientShader.maxControlPoints * 4)

    private var version by mutableIntStateOf(0)

    init {
        setBezierPoints(initialPoints)
        setColors(initialColors)
    }

    fun setBezierPoints(grid: List<List<BezierPoint>>) {
        var idx = 0
        grid.flatten().forEach { bp ->
            if (idx < width * height) {
                snapshotPoints[idx * 2] = bp.position.x
                snapshotPoints[idx * 2 + 1] = bp.position.y

                val uBase = idx * 4
                snapshotUTangents[uBase + 0] = bp.uTangentLeading.x
                snapshotUTangents[uBase + 1] = bp.uTangentLeading.y
                snapshotUTangents[uBase + 2] = bp.uTangentTrailing.x
                snapshotUTangents[uBase + 3] = bp.uTangentTrailing.y

                val vBase = idx * 4
                snapshotVTangents[vBase + 0] = bp.vTangentTop.x
                snapshotVTangents[vBase + 1] = bp.vTangentTop.y
                snapshotVTangents[vBase + 2] = bp.vTangentBottom.x
                snapshotVTangents[vBase + 3] = bp.vTangentBottom.y

                idx++
            }
        }
        version++
    }

    fun setColors(grid: List<List<Color>>) {
        var idx = 0
        grid.flatten().forEach { c ->
            if (idx < width * height) {
                snapshotColors[idx * 4 + 0] = c.red
                snapshotColors[idx * 4 + 1] = c.green
                snapshotColors[idx * 4 + 2] = c.blue
                snapshotColors[idx * 4 + 3] = c.alpha
                idx++
            }
        }
        version++
    }

    fun setBezierPoint(row: Int, col: Int, point: BezierPoint) {
        val idx = row * width + col
        if (idx in 0 until (width * height)) {
            snapshotPoints[idx * 2] = point.position.x
            snapshotPoints[idx * 2 + 1] = point.position.y

            val uBase = idx * 4
            snapshotUTangents[uBase + 0] = point.uTangentLeading.x
            snapshotUTangents[uBase + 1] = point.uTangentLeading.y
            snapshotUTangents[uBase + 2] = point.uTangentTrailing.x
            snapshotUTangents[uBase + 3] = point.uTangentTrailing.y

            val vBase = idx * 4
            snapshotVTangents[vBase + 0] = point.vTangentTop.x
            snapshotVTangents[vBase + 1] = point.vTangentTop.y
            snapshotVTangents[vBase + 2] = point.vTangentBottom.x
            snapshotVTangents[vBase + 3] = point.vTangentBottom.y

            version++
        }
    }

    fun setColor(row: Int, col: Int, color: Color) {
        val idx = row * width + col
        if (idx in 0 until (width * height)) {
            snapshotColors[idx * 4 + 0] = color.red
            snapshotColors[idx * 4 + 1] = color.green
            snapshotColors[idx * 4 + 2] = color.blue
            snapshotColors[idx * 4 + 3] = color.alpha
            version++
        }
    }

    fun pointsArray(): FloatArray {
        @Suppress("UNUSED_EXPRESSION")
        version
        return snapshotPoints
    }

    fun colorsArray(): FloatArray {
        @Suppress("UNUSED_EXPRESSION")
        version
        return snapshotColors
    }

    fun uTangentsArray(): FloatArray {
        @Suppress("UNUSED_EXPRESSION")
        version
        return snapshotUTangents
    }

    fun vTangentsArray(): FloatArray {
        @Suppress("UNUSED_EXPRESSION")
        version
        return snapshotVTangents
    }
}

/** Backward-compatible typealias for [StardustBezierMeshState]. */
typealias MeshBezierState = StardustBezierMeshState

/** Backward-compatible typealias for [StardustBezierMeshState]. */
typealias BezierMeshGradientState = StardustBezierMeshState

/**
 * Creates and remembers a [StardustBezierMeshState].
 */
@Composable
fun rememberStardustBezierMeshState(
    points: List<List<BezierPoint>>,
    colors: List<List<Color>>
): StardustBezierMeshState {
    return remember(points, colors) {
        StardustBezierMeshState(points, colors)
    }
}

/** Backward-compatible helper alias for [rememberStardustBezierMeshState]. */
@Composable
fun rememberMeshBezierState(
    points: List<List<BezierPoint>>,
    colors: List<List<Color>>
): StardustBezierMeshState = rememberStardustBezierMeshState(points, colors)
