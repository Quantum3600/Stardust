package io.github.quantum3600.stardust

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDirection.Companion.Content

/**
 * Mesh gradient from static arrays. Re-composes when the arrays change.
 *
 * @param globalSubdivisions only used by the OpenGL fallback (Android < 33); shader backends are per-pixel.
 */
@Composable
fun StardustMesh(
    modifier: Modifier = Modifier,
    width: Int,
    height: Int,
    points: Array<Offset>,
    colors: Array<Color>,
    globalSubdivisions: Int = 32,
) {
    require(points.size == width * height) { "points must have width*height entries" }
    require(colors.size == width * height) { "colors must have width*height entries" }

    val p = FloatArray(points.size * 2).also { out ->
        points.forEachIndexed { i, o -> out[i * 2] = o.x; out[i * 2 + 1] = o.y }
    }
    val c = FloatArray(colors.size * 4).also { out ->
        colors.forEachIndexed { i, col ->
            out[i * 4] = col.red; out[i * 4 + 1] = col.green; out[i * 4 + 2] = col.blue; out[i * 4 + 3] = col.alpha
        }
    }
    StardustMeshContent(modifier, width, height, globalSubdivisions, { p }, { c })
}

/** Mesh gradient driven by a [MeshGradientState]; animation only triggers redraws. */
@Composable
fun MeshGradient(
    modifier: Modifier = Modifier,
    width: Int,
    height: Int,
    globalSubdivisions: Int = 32,
    state: StardustMeshState,
) {
    StardustMeshContent(
        modifier, width, height, globalSubdivisions,
        pointsProvider = { state.pointsArray() },
        colorsProvider = { state.colorsArray() },
    )
}
