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
class MeshGradientState(
    initialPoints: Array<Offset>,
    initialColors: Array<Color> = emptyArray()
) {

    private val _points = mutableStateListOf<Animatable<Offset, AnimationVector2D>>().apply {
        initialPoints.forEach { add(Animatable(it, Offset.VectorConverter)) }
    }
    private val _colors = mutableStateListOf<Color>().apply {
        addAll(initialColors)
    }

    private var pointsVersion by mutableIntStateOf(0)
    private var colorsVersion by mutableIntStateOf(0)

    val points: List<Offset>
        get() {
            @Suppress("UNUSED_EXPRESSION")
            pointsVersion
            return _points.map { it.value }
        }

    val colors: List<Color> get() = _colors

    private val snapshotPoints = FloatArray(_points.size * 2)
    private val snapshotColors = FloatArray(_colors.size * 4)

    /** Returns a reusable float array of points: [x0,y0, x1,y1, ...]. Read inside draw phase to register dependency. */
    fun pointsArray(): FloatArray {
        @Suppress("UNUSED_EXPRESSION")
        pointsVersion
        var i = 0
        for (p in _points) {
            snapshotPoints[i++] = p.value.x
            snapshotPoints[i++] = p.value.y
        }
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
                pointsVersion++
            }
        }
    }

    fun setColor(index: Int, color: Color) {
        if (index in _colors.indices) {
            _colors[index] = color
            colorsVersion++
        }
    }

    suspend fun snapAllPoints(newOffsets: List<Offset>) {
        newOffsets.forEachIndexed { i, o ->
            if (i in _points.indices) {
                _points[i].snapTo(o)
            }
        }
        pointsVersion++
    }

    fun snapAllPoints(newOffsets: FloatArray) {
        val count = minOf(newOffsets.size / 2, _points.size)
        for (i in 0 until count) {
            val px = newOffsets[i * 2]
            val py = newOffsets[i * 2 + 1]
            snapshotPoints[i * 2] = px
            snapshotPoints[i * 2 + 1] = py
        }
        pointsVersion++
    }
}

/** Backward-compatible typealias for [MeshGradientState]. */
typealias StardustMeshState = MeshGradientState

/**
 * Creates and remembers a [MeshGradientState].
 *
 * @param points The initial list of control points for the mesh.
 * @param colors The initial list of colors for the mesh.
 */
@Composable
fun rememberMeshGradientState(
    points: Array<Offset>,
    colors: Array<Color> = emptyArray()
): MeshGradientState {
    return remember(points) {
        MeshGradientState(points, colors)
    }
}

/** Backward-compatible helper alias for [rememberMeshGradientState]. */
@Composable
fun rememberStardustMeshState(
    points: Array<Offset>,
    colors: Array<Color> = emptyArray()
): MeshGradientState = rememberMeshGradientState(points, colors)
