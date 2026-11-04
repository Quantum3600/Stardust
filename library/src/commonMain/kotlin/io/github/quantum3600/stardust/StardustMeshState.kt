package io.github.quantum3600.stardust

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

/**
 * State holder for [MeshGradient]. Points are in normalized [0,1] space, origin top-left (y down).
 */
@Stable
class StardustMeshState(
    initialPoints: Array<Offset>,
    initialColors: Array<Color>,
) {
    init {
        require(initialPoints.size == initialColors.size) { "points and colors must have the same size" }
    }

    private val _points = mutableStateListOf<Animatable<Offset, AnimationVector2D>>().apply {
        initialPoints.forEach { add(Animatable(it, Offset.VectorConverter)) }
    }
    private val _colors = mutableStateListOf<Color>().apply { addAll(initialColors) }

    val points: List<Offset> get() = _points.map { it.value }
    val colors: List<Color> get() = _colors

    private val snapshotPoints = FloatArray(_points.size * 2)
    private val snapshotColors = FloatArray(_colors.size * 4)

    /** Reusable [x0,y0,x1,y1,...]. Reads Compose state, so call it from the draw phase. Don't keep it. */
    fun pointsArray(): FloatArray {
        var i = 0
        for (p in _points) {
            val v = p.value
            snapshotPoints[i++] = v.x
            snapshotPoints[i++] = v.y
        }
        return snapshotPoints
    }

    /** Reusable [r,g,b,a,...]. Reads Compose state, so call it from the draw phase. Don't keep it. */
    fun colorsArray(): FloatArray {
        var i = 0
        for (c in _colors) {
            snapshotColors[i++] = c.red
            snapshotColors[i++] = c.green
            snapshotColors[i++] = c.blue
            snapshotColors[i++] = c.alpha
        }
        return snapshotColors
    }

    suspend fun animatePoint(index: Int, target: Offset, spec: AnimationSpec<Offset>) {
        _points[index].animateTo(target, spec)
    }

    fun setColor(index: Int, color: Color) {
        _colors[index] = color
    }

    suspend fun snapAllPoints(newOffsets: List<Offset>) {
        newOffsets.forEachIndexed { i, o -> if (i in _points.indices) _points[i].snapTo(o) }
    }
}

@Composable
fun rememberStardustMeshState(
    points: Array<Offset>,
    colors: Array<Color>,
): StardustMeshState {
    // Keyed on content, not array identity (an inline arrayOf(...) would otherwise reset state every recomposition)
    return remember(points.contentHashCode(), colors.contentHashCode()) {
        StardustMeshState(points, colors)
    }
}
