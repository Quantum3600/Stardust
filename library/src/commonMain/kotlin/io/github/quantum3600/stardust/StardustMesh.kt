package io.github.quantum3600.stardust

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import io.github.quantum3600.stardust.utils.AnimatedMeshState
import io.github.quantum3600.stardust.utils.rememberSimplexAnimatedMesh

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

/** Mesh gradient driven by a [StardustMeshState]; animation only triggers redraws. */
@Composable
fun StardustMesh(
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

// --- Composable Modifier APIs ---

/**
 * Modifier that applies a mesh gradient from static arrays behind the content of the composable.
 */
@Composable
fun Modifier.stardustMesh(
    width: Int,
    height: Int,
    points: Array<Offset>,
    colors: Array<Color>,
    globalSubdivisions: Int = 32,
): Modifier {
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
    return this.stardustMeshContent(width, height, globalSubdivisions, { p }, { c })
}

/**
 * Modifier that applies a mesh gradient driven by a [StardustMeshState] behind the content of the composable.
 */
@Composable
fun Modifier.stardustMesh(
    width: Int,
    height: Int,
    state: StardustMeshState,
    globalSubdivisions: Int = 32,
): Modifier {
    return this.stardustMeshContent(
        width,
        height,
        globalSubdivisions,
        pointsProvider = { state.pointsArray() },
        colorsProvider = { state.colorsArray() },
    )
}

/**
 * Modifier that applies an animated mesh gradient driven by an [AnimatedMeshState] behind the composable content.
 */
@Composable
fun Modifier.stardustMesh(
    width: Int,
    height: Int,
    animatedState: AnimatedMeshState,
    globalSubdivisions: Int = 32,
): Modifier = this.stardustMesh(width, height, animatedState.state, globalSubdivisions)

/**
 * Modifier that creates and applies a continuous simplex-noise animated mesh gradient behind the composable content.
 *
 * @param width Number of control points per row.
 * @param height Number of control points per column.
 * @param colors Colors for the control points (`width * height` entries).
 * @param animationSpeed Speed multiplier for the noise animation loop.
 * @param noiseIntensity Intensity of displacement for inner mesh points.
 * @param globalSubdivisions Subdivision tessellation for OpenGL fallback (Android < 33).
 */
@Composable
fun Modifier.stardustMeshAnimation(
    width: Int,
    height: Int,
    colors: List<Color>,
    animationSpeed: Float = 0.3f,
    noiseIntensity: Float = 0.2f,
    globalSubdivisions: Int = 32,
): Modifier {
    val anim = rememberSimplexAnimatedMesh(
        width = width,
        height = height,
        colors = colors,
        animationSpeed = animationSpeed,
        noiseIntensity = noiseIntensity,
    )
    return this.stardustMesh(width, height, anim.state, globalSubdivisions)
}

/**
 * Modifier that applies a mesh gradient driven by [pointsProvider] and [colorsProvider] behind the content.
 */
@Composable
fun Modifier.stardustMesh(
    width: Int,
    height: Int,
    pointsProvider: () -> FloatArray,
    colorsProvider: () -> FloatArray,
    globalSubdivisions: Int = 32,
): Modifier = this.stardustMeshContent(width, height, globalSubdivisions, pointsProvider, colorsProvider)
