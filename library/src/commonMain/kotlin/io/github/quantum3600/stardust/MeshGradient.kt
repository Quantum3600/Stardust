package io.github.quantum3600.stardust

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import io.github.quantum3600.stardust.data.BezierPoint
import kotlin.jvm.JvmName

/**
 * Draws a bicubic mesh gradient with a platform GPU runtime shader.
 *
 * Control-point positions are normalized to the composable bounds. The shader evaluates the
 * bicubic surface and color interpolation per pixel, so animated points do not require CPU
 * tessellation or a triangle mesh.
 */
@Composable
fun StardustMesh(
    modifier: Modifier = Modifier,
    width: Int,
    height: Int,
    points: Array<Offset>,
    colors: Array<Color>,
    uTangents: FloatArray? = null,
    vTangents: FloatArray? = null,
    @Suppress("UNUSED_PARAMETER") globalSubdivisions: Int = 32,
    dustIntensity: Float = 0f,
    dustAnimationSpeed: Float = 0f,
) {
    require(width > 1 && height > 1) { "Mesh dimensions must be greater than 1" }
    require(points.size == width * height) { "points must contain width * height items" }
    require(colors.size == width * height) { "colors must contain width * height items" }
    require(width * height <= MeshGradientShader.maxControlPoints) {
        "Mesh gradients support at most ${MeshGradientShader.maxControlPoints} control points"
    }
    require(dustIntensity in 0f..1f) { "dustIntensity must be between 0.0 and 1.0" }

    val dustPhase = remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    androidx.compose.runtime.LaunchedEffect(dustAnimationSpeed) {
        if (dustAnimationSpeed > 0f) {
            var lastTime = androidx.compose.runtime.withFrameNanos { it }
            while (true) {
                val frameTime = androidx.compose.runtime.withFrameNanos { it }
                val dt = (frameTime - lastTime) / 1_000_000_000f
                lastTime = frameTime
                dustPhase.floatValue += dt * dustAnimationSpeed
            }
        }
    }

    val shader = remember(width, height) {
        MeshGradientShader(width, height)
    }

    DisposableEffect(shader) {
        onDispose {
            shader.close()
        }
    }

    Canvas(modifier) {
        val effectivePhase = if (dustAnimationSpeed > 0f) dustPhase.floatValue else dustAnimationSpeed
        shader.update(points, colors, uTangents, vTangents, dustIntensity, effectivePhase)
        drawMeshGradient(shader)
    }
}

/**
 * Draws a bicubic mesh gradient driven by a [StardustMeshState].
 */
@Composable
fun StardustMesh(
    modifier: Modifier = Modifier,
    width: Int,
    height: Int,
    @Suppress("UNUSED_PARAMETER") globalSubdivisions: Int = 32,
    dustIntensity: Float = 0f,
    dustAnimationSpeed: Float = 0f,
    state: StardustMeshState,
) {
    StardustMesh(
        modifier = modifier,
        width = width,
        height = height,
        globalSubdivisions = globalSubdivisions,
        dustIntensity = dustIntensity,
        dustAnimationSpeed = dustAnimationSpeed,
        points = state.points.toTypedArray(),
        colors = state.colors.toTypedArray(),
    )
}

/**
 * Draws a mesh gradient using 2D grid lists of [BezierPoint]s and colors.
 */
@Composable
@JvmName("StardustMeshBezier")
fun StardustMesh(
    bezierPoints: List<List<BezierPoint>>,
    colors: List<List<Color>>,
    modifier: Modifier = Modifier,
    grainIntensity: Float = 0f,
    grainSeed: Float = 0f,
) {
    val height = bezierPoints.size
    val width = bezierPoints.firstOrNull()?.size ?: 0
    require(width > 1 && height > 1) { "Mesh dimensions must be greater than 1" }
    require(colors.size == height && colors.all { it.size == width }) {
        "colors grid dimensions must match bezierPoints grid dimensions ($width x $height)"
    }
    require(width * height <= MeshGradientShader.maxControlPoints) {
        "Mesh gradients support at most ${MeshGradientShader.maxControlPoints} control points"
    }

    val flatPoints = remember(bezierPoints) {
        bezierPoints.flatten().map { it.position }.toTypedArray()
    }
    val flatColors = remember(colors) {
        colors.flatten().toTypedArray()
    }
    val uTangents = remember(bezierPoints) {
        val arr = FloatArray(MeshGradientShader.maxControlPoints * 4)
        bezierPoints.flatten().forEachIndexed { i, bp ->
            arr[i * 4 + 0] = bp.uTangentLeading.x
            arr[i * 4 + 1] = bp.uTangentLeading.y
            arr[i * 4 + 2] = bp.uTangentTrailing.x
            arr[i * 4 + 3] = bp.uTangentTrailing.y
        }
        arr
    }
    val vTangents = remember(bezierPoints) {
        val arr = FloatArray(MeshGradientShader.maxControlPoints * 4)
        bezierPoints.flatten().forEachIndexed { i, bp ->
            arr[i * 4 + 0] = bp.vTangentTop.x
            arr[i * 4 + 1] = bp.vTangentTop.y
            arr[i * 4 + 2] = bp.vTangentBottom.x
            arr[i * 4 + 3] = bp.vTangentBottom.y
        }
        arr
    }

    StardustMesh(
        modifier = modifier,
        width = width,
        height = height,
        points = flatPoints,
        colors = flatColors,
        uTangents = uTangents,
        vTangents = vTangents,
        dustIntensity = grainIntensity,
        dustAnimationSpeed = grainSeed
    )
}

/**
 * Draws a mesh gradient using 2D grid lists of [Offset] positions and colors.
 * Automatically generates cardinal Bézier tangents for full backward compatibility.
 */
@Composable
@JvmName("StardustMeshPositions")
fun StardustMesh(
    points: List<List<Offset>>,
    colors: List<List<Color>>,
    modifier: Modifier = Modifier,
    grainIntensity: Float = 0f,
    grainSeed: Float = 0f,
) {
    val height = points.size
    val width = points.firstOrNull()?.size ?: 0
    require(width > 1 && height > 1) { "Mesh dimensions must be greater than 1" }
    require(colors.size == height && colors.all { it.size == width }) {
        "colors grid dimensions must match points grid dimensions ($width x $height)"
    }
    require(width * height <= MeshGradientShader.maxControlPoints) {
        "Mesh gradients support at most ${MeshGradientShader.maxControlPoints} control points"
    }

    val flatPoints = remember(points) {
        points.flatten().toTypedArray()
    }
    val flatColors = remember(colors) {
        colors.flatten().toTypedArray()
    }

    StardustMesh(
        modifier = modifier,
        width = width,
        height = height,
        points = flatPoints,
        colors = flatColors,
        dustIntensity = grainIntensity,
        dustAnimationSpeed = grainSeed
    )
}

// ----------------------------------------------------------------------------------
// Alias Functions: MeshGradient -> StardustMesh
// ----------------------------------------------------------------------------------

/**
 * Draws a bicubic mesh gradient with explicit control point positions and colors.
 * Alias for [StardustMesh].
 */
@Composable
fun MeshGradient(
    width: Int,
    height: Int,
    points: Array<Offset>,
    colors: Array<Color>,
    modifier: Modifier = Modifier,
    globalSubdivisions: Int = 32,
    dustIntensity: Float = 0f,
    dustAnimationSpeed: Float = 0f,
) {
    StardustMesh(
        modifier = modifier,
        width = width,
        height = height,
        points = points,
        colors = colors,
        globalSubdivisions = globalSubdivisions,
        dustIntensity = dustIntensity,
        dustAnimationSpeed = dustAnimationSpeed,
    )
}

/**
 * Draws a bicubic mesh gradient driven by a [StardustMeshState].
 * Alias for [StardustMesh].
 */
@Composable
fun MeshGradient(
    width: Int,
    height: Int,
    modifier: Modifier = Modifier,
    globalSubdivisions: Int = 32,
    dustIntensity: Float = 0f,
    dustAnimationSpeed: Float = 0f,
    state: StardustMeshState,
) {
    StardustMesh(
        modifier = modifier,
        width = width,
        height = height,
        globalSubdivisions = globalSubdivisions,
        dustIntensity = dustIntensity,
        dustAnimationSpeed = dustAnimationSpeed,
        state = state,
    )
}

/**
 * Draws a mesh gradient using 2D grid lists of [BezierPoint]s and colors.
 * Alias for [StardustMesh].
 */
@Composable
@JvmName("MeshGradientBezier")
fun MeshGradient(
    bezierPoints: List<List<BezierPoint>>,
    colors: List<List<Color>>,
    modifier: Modifier = Modifier,
    grainIntensity: Float = 0f,
    grainSeed: Float = 0f,
) {
    StardustMesh(
        bezierPoints = bezierPoints,
        colors = colors,
        modifier = modifier,
        grainIntensity = grainIntensity,
        grainSeed = grainSeed
    )
}

/**
 * Draws a mesh gradient using 2D grid lists of [Offset] positions and colors.
 * Alias for [StardustMesh].
 */
@Composable
@JvmName("MeshGradientPositions")
fun MeshGradient(
    points: List<List<Offset>>,
    colors: List<List<Color>>,
    modifier: Modifier = Modifier,
    grainIntensity: Float = 0f,
    grainSeed: Float = 0f,
) {
    StardustMesh(
        points = points,
        colors = colors,
        modifier = modifier,
        grainIntensity = grainIntensity,
        grainSeed = grainSeed
    )
}

private fun DrawScope.drawMeshGradient(shader: MeshGradientShader) {
    shader.draw(this)
}

internal expect class MeshGradientShader(width: Int, height: Int) {
    fun update(
        points: Array<Offset>,
        colors: Array<Color>,
        uTangents: FloatArray? = null,
        vTangents: FloatArray? = null,
        dustIntensity: Float = 0f,
        dustPhase: Float = 0f
    )

    fun brush(size: androidx.compose.ui.geometry.Size): Brush
    fun draw(drawScope: DrawScope)
    fun close()

    companion object {
        val maxControlPoints: Int
    }
}
