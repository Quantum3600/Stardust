package io.github.om252345.composemeshgradient

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Vertices
import androidx.compose.ui.graphics.VertexMode
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import io.github.om252345.composemeshgradient.utils.MeshInterpolation

/**
 * Composable that displays a mesh gradient using Compose's multiplatform Canvas.
 *
 * This composable is reactive to changes in the `points` list. When a new list is provided,
 * it will update the mesh and redraw. This allows for animations driven by Compose state.
 *
 * @param modifier The modifier to be applied to the layout.
 * @param width The number of control points horizontally.
 * @param height The number of control points vertically.
 * @param points The list of control points that define the mesh's shape. The size must be `width * height`.
 * @param colors The list of colors for each control point. The size must be `width * height`.
 */
@Composable
fun MeshGradient(
    modifier: Modifier = Modifier,
    width: Int,
    height: Int,
    points: Array<Offset>,
    colors: Array<Color>,
    globalSubdivisions: Int = 32,
) {
    require(width > 1 && height > 1) { "Mesh dimensions must be greater than 1" }
    require(globalSubdivisions > 0) { "globalSubdivisions must be greater than zero" }
    require(points.size == width * height) { "points must contain width * height items" }
    require(colors.size == width * height) { "colors must contain width * height items" }

    val mesh = remember(width, height, globalSubdivisions) {
        MeshBuffer(width, height, globalSubdivisions)
    }

    Canvas(modifier) {
        mesh.update(points, colors, size.width, size.height)
        drawIntoCanvas { canvas ->
            canvas.drawVertices(mesh.vertices, BlendMode.SrcOver, mesh.paint)
        }
    }
}

@Composable
fun MeshGradient(
    modifier: Modifier = Modifier,
    width: Int,
    height: Int,
    globalSubdivisions: Int = 32,
    state: MeshGradientState
) {
    MeshGradient(
        modifier = modifier,
        width = width,
        height = height,
        globalSubdivisions = globalSubdivisions,
        points = state.points.toTypedArray(),
        colors = state.colors.toTypedArray()
    )
}

private class MeshBuffer(
    private val width: Int,
    private val height: Int,
    private val subdivisions: Int
) {
    private val pointCount = (width - 1) * subdivisions + 1
    private val rowCount = (height - 1) * subdivisions + 1
    private val positions = FloatArray(pointCount * rowCount * 2)
    private val textureCoordinates = FloatArray(positions.size)
    private val colors = IntArray(pointCount * rowCount)
    private val lastPoints = FloatArray(width * height * 2)
    private val lastColors = IntArray(width * height)
    private var lastCanvasWidth = Float.NaN
    private var lastCanvasHeight = Float.NaN
    private var hasUpdate = false
    private val indices = buildList {
        for (y in 0 until rowCount - 1) {
            for (x in 0 until pointCount - 1) {
                val topLeft = y * pointCount + x
                val topRight = topLeft + 1
                val bottomLeft = topLeft + pointCount
                val bottomRight = bottomLeft + 1
                add(topLeft)
                add(bottomLeft)
                add(bottomRight)
                add(topLeft)
                add(bottomRight)
                add(topRight)
            }
        }
    }
    private val shortIndices = ShortArray(indices.size) { indices[it].toShort() }
    val paint = Paint()
    var vertices = Vertices(VertexMode.Triangles, positions, textureCoordinates, colors, shortIndices)
        private set

    fun update(points: Array<Offset>, pointColors: Array<Color>, canvasWidth: Float, canvasHeight: Float) {
        var changed = !hasUpdate || canvasWidth != lastCanvasWidth || canvasHeight != lastCanvasHeight
        for (i in points.indices) {
            val pointIndex = i * 2
            val color = pointColors[i].toArgb()
            if (lastPoints[pointIndex] != points[i].x ||
                lastPoints[pointIndex + 1] != points[i].y ||
                lastColors[i] != color
            ) {
                changed = true
            }
            lastPoints[pointIndex] = points[i].x
            lastPoints[pointIndex + 1] = points[i].y
            lastColors[i] = color
        }
        if (!changed) return

        val result = MeshInterpolation.generateMeshVerticesAndColors(
            points.toList(), pointColors.toList(), width, height, subdivisions, subdivisions
        )
        for (i in 0 until pointCount * rowCount) {
            positions[i * 2] = result.vertices[i * 2] * canvasWidth
            positions[i * 2 + 1] = result.vertices[i * 2 + 1] * canvasHeight
            colors[i] = Color(
                result.colors[i * 4], result.colors[i * 4 + 1],
                result.colors[i * 4 + 2], result.colors[i * 4 + 3]
            ).toArgb()
        }
        vertices = Vertices(VertexMode.Triangles, positions, textureCoordinates, colors, shortIndices)
        lastCanvasWidth = canvasWidth
        lastCanvasHeight = canvasHeight
        hasUpdate = true
    }
}