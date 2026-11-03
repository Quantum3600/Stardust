package io.github.quantum3600.stardust

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * High-performance bicubic mesh gradient driven by primitive FloatArray providers.
 * Bypasses Compose Recomposition and Layout passes entirely during animation.
 */
@Composable
fun StardustMesh(
    width: Int,
    height: Int,
    pointsProvider: () -> FloatArray,
    colorsProvider: () -> FloatArray,
    modifier: Modifier = Modifier,
    uTangentsProvider: (() -> FloatArray)? = null,
    vTangentsProvider: (() -> FloatArray)? = null,
    dustIntensityProvider: () -> Float = { 0f },
    dustPhaseProvider: () -> Float = { 0f },
    downsampleFactorProvider: () -> Float = { 1f },
) {
    require(width > 1 && height > 1) { "Mesh dimensions must be greater than 1" }
    require(width * height <= StardustMeshShader.maxControlPoints) {
        "Mesh gradients support at most ${StardustMeshShader.maxControlPoints} control points"
    }

    val shader = remember(width, height) {
        StardustMeshShader(width, height)
    }

    DisposableEffect(shader) {
        onDispose {
            shader.close()
        }
    }

    Spacer(
        modifier = modifier.drawBehind {
            val points = pointsProvider()
            val colors = colorsProvider()
            val uTangents = uTangentsProvider?.invoke()
            val vTangents = vTangentsProvider?.invoke()
            val dustIntensity = dustIntensityProvider()
            val dustPhase = dustPhaseProvider()
            val downsampleFactor = downsampleFactorProvider()

            shader.update(points, colors, uTangents, vTangents, dustIntensity, dustPhase)
            shader.draw(this, downsampleFactor)
        }
    )
}

/**
 * Draws a bicubic mesh gradient with explicit control point arrays.
 */
@Composable
fun StardustMesh(
    width: Int,
    height: Int,
    points: Array<Offset>,
    colors: Array<Color>,
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") globalSubdivisions: Int = 32,
    dustIntensity: Float = 0f
) {
    require(width > 1 && height > 1) { "Mesh dimensions must be greater than 1" }
    require(points.size == width * height) { "points must contain width * height items" }
    require(colors.size == width * height) { "colors must contain width * height items" }
    require(width * height <= StardustMeshShader.maxControlPoints) {
        "Mesh gradients support at most ${StardustMeshShader.maxControlPoints} control points"
    }

    val flatPoints = remember(points) {
        val arr = FloatArray(points.size * 2)
        for (i in points.indices) {
            arr[i * 2] = points[i].x
            arr[i * 2 + 1] = points[i].y
        }
        arr
    }
    val flatColors = remember(colors) {
        val arr = FloatArray(colors.size * 4)
        for (i in colors.indices) {
            arr[i * 4] = colors[i].red
            arr[i * 4 + 1] = colors[i].green
            arr[i * 4 + 2] = colors[i].blue
            arr[i * 4 + 3] = colors[i].alpha
        }
        arr
    }

    StardustMesh(
        width = width,
        height = height,
        pointsProvider = { flatPoints },
        colorsProvider = { flatColors },
        modifier = modifier,
        dustIntensityProvider = { dustIntensity },
        dustPhaseProvider = { 0f }
    )
}

/**
 * Draws a bicubic mesh gradient driven by a [StardustMeshState].
 */
@Composable
fun StardustMesh(
    width: Int,
    height: Int,
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") globalSubdivisions: Int = 32,
    dustIntensity: Float = 0f,
    state: StardustMeshState,
) {
    StardustMesh(
        width = width,
        height = height,
        pointsProvider = { state.pointsArray() },
        colorsProvider = { state.colorsArray() },
        modifier = modifier,
        dustIntensityProvider = { dustIntensity },
        dustPhaseProvider = { 0f }
    )
}

// ----------------------------------------------------------------------------------
// Alias Functions: MeshGradient -> StardustMesh
// ----------------------------------------------------------------------------------

/**
 * Primary composable function for rendering mesh gradients in Stardust.
 * Exact alias matching original MeshGradient API.
 */
@Composable
fun MeshGradient(
    width: Int,
    height: Int,
    points: Array<Offset>,
    colors: Array<Color>,
    modifier: Modifier = Modifier,
    globalSubdivisions: Int = 32
) {
    StardustMesh(
        width = width,
        height = height,
        points = points,
        colors = colors,
        modifier = modifier,
        globalSubdivisions = globalSubdivisions
    )
}

/**
 * Primary composable function for rendering mesh gradients driven by a state.
 * Exact alias matching original MeshGradient API.
 */
@Composable
fun MeshGradient(
    width: Int,
    height: Int,
    modifier: Modifier = Modifier,
    globalSubdivisions: Int = 32,
    state: MeshGradientState
) {
    StardustMesh(
        width = width,
        height = height,
        modifier = modifier,
        globalSubdivisions = globalSubdivisions,
        state = state
    )
}

internal expect class StardustMeshShader(width: Int, height: Int) {
    fun update(
        points: Array<Offset>,
        colors: Array<Color>,
        uTangents: FloatArray? = null,
        vTangents: FloatArray? = null,
        dustIntensity: Float = 0f,
        dustPhase: Float = 0f
    )

    fun update(
        points: FloatArray,
        colors: FloatArray,
        uTangents: FloatArray? = null,
        vTangents: FloatArray? = null,
        dustIntensity: Float = 0f,
        dustPhase: Float = 0f
    )

    fun brush(size: androidx.compose.ui.geometry.Size): Brush
    fun draw(drawScope: DrawScope, downsampleFactor: Float = 1f)
    fun close()

    companion object {
        val maxControlPoints: Int
    }
}

internal typealias MeshGradientShader = StardustMeshShader
