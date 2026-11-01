package io.github.om252345.composemeshgradient

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * Draws a bicubic mesh gradient with a platform GPU runtime shader.
 *
 * Control-point positions are normalized to the composable bounds. The shader evaluates the
 * bicubic surface and color interpolation per pixel, so animated points do not require CPU
 * tessellation or a triangle mesh.
 */
@Composable
fun MeshGradient(
    modifier: Modifier = Modifier,
    width: Int,
    height: Int,
    points: Array<Offset>,
    colors: Array<Color>,
    @Suppress("UNUSED_PARAMETER") globalSubdivisions: Int = 32,
) {
    require(width > 1 && height > 1) { "Mesh dimensions must be greater than 1" }
    require(points.size == width * height) { "points must contain width * height items" }
    require(colors.size == width * height) { "colors must contain width * height items" }
    require(width * height <= MeshGradientShader.maxControlPoints) {
        "Mesh gradients support at most ${MeshGradientShader.maxControlPoints} control points"
    }

    val shader = remember(width, height) {
        MeshGradientShader(width, height)
    }

    Canvas(modifier) {
        shader.update(points, colors)
        drawMeshGradient(shader)
    }
}

@Composable
fun MeshGradient(
    modifier: Modifier = Modifier,
    width: Int,
    height: Int,
    @Suppress("UNUSED_PARAMETER") globalSubdivisions: Int = 32,
    state: MeshGradientState,
) {
    MeshGradient(
        modifier = modifier,
        width = width,
        height = height,
        globalSubdivisions = globalSubdivisions,
        points = state.points.toTypedArray(),
        colors = state.colors.toTypedArray(),
    )
}

private fun DrawScope.drawMeshGradient(shader: MeshGradientShader) {
    drawRect(brush = shader.brush(size))
}

internal expect class MeshGradientShader(width: Int, height: Int) {
    fun update(points: Array<Offset>, colors: Array<Color>)
    fun brush(size: androidx.compose.ui.geometry.Size): Brush

    companion object {
        val maxControlPoints: Int
    }
}
