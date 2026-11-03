package io.github.quantum3600.stardust

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

/**
 * A state holder for [StardustMesh] that allows for programmatic animation of control points.
 *
 * @param initialPoints The starting positions of the mesh's control points.
 * @param initialColors The starting colors of the mesh's control points.
 */
@Stable
class StardustMeshState(
    initialPoints: Array<Offset>,
    initialColors: Array<Color>
) {

    // Original API data
    private val _points = mutableStateListOf<Animatable<Offset, AnimationVector2D>>().apply {
        initialPoints.forEach { add(Animatable(it, Offset.VectorConverter)) }
    }
    private val _colors = mutableStateListOf<Color>().apply {
        addAll(initialColors)
    }

    private var pointsVersion by mutableIntStateOf(0)
    private var colorsVersion by mutableIntStateOf(0)

    // -------- Backward-compatible properties --------
    val points: List<Offset>
        get() {
            @Suppress("UNUSED_EXPRESSION")
            pointsVersion
            val list = ArrayList<Offset>(snapshotPoints.size / 2)
            for (i in 0 until (snapshotPoints.size / 2)) {
                list.add(Offset(snapshotPoints[i * 2], snapshotPoints[i * 2 + 1]))
            }
            return list
        }

    val colors: List<Color> get() = _colors

    // -------- Snapshot arrays for renderer --------
    private val snapshotPoints = FloatArray(_points.size * 2).apply {
        initialPoints.forEachIndexed { i, p ->
            this[i * 2] = p.x
            this[i * 2 + 1] = p.y
        }
    }
    private val snapshotColors = FloatArray(_colors.size * 4).apply {
        initialColors.forEachIndexed { i, c ->
            this[i * 4] = c.red
            this[i * 4 + 1] = c.green
            this[i * 4 + 2] = c.blue
            this[i * 4 + 3] = c.alpha
        }
    }

    /** Returns a reusable float array of points: [x0,y0, x1,y1, ...]. Read inside draw phase to register dependency. */
    fun pointsArray(): FloatArray {
        @Suppress("UNUSED_EXPRESSION")
        pointsVersion
        return snapshotPoints
    }

    /** Returns a reusable float array of colors: [r,g,b,a, r,g,b,a, ...]. Read inside draw phase to register dependency. */
    fun colorsArray(): FloatArray {
        @Suppress("UNUSED_EXPRESSION")
        colorsVersion
        var i = 0
        for (c in _colors) {
            snapshotColors[i++] = c.red
            snapshotColors[i++] = c.green
            snapshotColors[i++] = c.blue
            snapshotColors[i++] = c.alpha
        }
        return snapshotColors
    }

    // -------- State mutation helpers --------
    suspend fun animatePoint(index: Int, target: Offset, spec: AnimationSpec<Offset>) {
        if (index in _points.indices) {
            _points[index].animateTo(target, spec) {
                snapshotPoints[index * 2] = value.x
                snapshotPoints[index * 2 + 1] = value.y
                pointsVersion++
            }
        }
    }

    fun setColor(index: Int, color: Color) {
        _colors[index] = color
        colorsVersion++
    }

    fun snapAllPoints(newOffsets: List<Offset>) {
        val count = minOf(newOffsets.size, snapshotPoints.size / 2)
        for (i in 0 until count) {
            val o = newOffsets[i]
            snapshotPoints[i * 2] = o.x
            snapshotPoints[i * 2 + 1] = o.y
        }
        pointsVersion++
    }

    fun snapAllPoints(newOffsets: Array<Offset>) {
        val count = minOf(newOffsets.size, snapshotPoints.size / 2)
        for (i in 0 until count) {
            val o = newOffsets[i]
            snapshotPoints[i * 2] = o.x
            snapshotPoints[i * 2 + 1] = o.y
        }
        pointsVersion++
    }

    fun snapAllPoints(newOffsets: FloatArray) {
        val count = minOf(newOffsets.size, snapshotPoints.size)
        newOffsets.copyInto(snapshotPoints, 0, 0, count)
        pointsVersion++
    }
}

/** Backward-compatible typealias for [StardustMeshState]. */
typealias MeshGradientState = StardustMeshState

/**
 * Creates and remembers a [StardustMeshState].
 *
 * @param points The initial list of control points for the mesh.
 * @param colors The initial list of colors for the mesh.
 */
@Composable
fun rememberStardustMeshState(
    points: Array<Offset>,
    colors: Array<Color>
): StardustMeshState {
    return remember(points) {
        StardustMeshState(points, colors)
    }
}

/** Backward-compatible helper alias for [rememberStardustMeshState]. */
@Composable
fun rememberMeshGradientState(
    points: Array<Offset>,
    colors: Array<Color>
): StardustMeshState = rememberStardustMeshState(points, colors)
