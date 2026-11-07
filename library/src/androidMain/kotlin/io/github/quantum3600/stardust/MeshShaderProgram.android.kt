package io.github.quantum3600.stardust

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ShaderBrush
import android.graphics.RuntimeShader
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import io.github.quantum3600.stardust.utils.MeshInterpolation

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal class AgslMeshProgram(gridWidth: Int, gridHeight: Int) : MeshShaderProgram {
    private val shader = RuntimeShader(MeshShaderSource.build(gridWidth, gridHeight))
    private val brush = ShaderBrush(shader)

    override fun createBrush(points: FloatArray, colors: FloatArray, size: Size): Brush {
        shader.setFloatUniform(MeshShaderSource.U_RESOLUTION, size.width, size.height)
        shader.setFloatUniform(MeshShaderSource.U_POINTS, points)
        shader.setFloatUniform(MeshShaderSource.U_COLORS, colors)
        return brush
    }
}

@Composable
internal actual fun StardustMeshContent(
    modifier: Modifier,
    width: Int,
    height: Int,
    subdivisions: Int,
    pointsProvider: () -> FloatArray,
    colorsProvider: () -> FloatArray,
) {
    // API 33+: AGSL. If the shader fails to compile on a quirky driver, fall back to OpenGL too.
    val agsl: MeshShaderProgram? = remember(width, height) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching { AgslMeshProgram(width, height) }.getOrNull()
        } else null
    }
    if (agsl != null) {
        ShaderMeshGradient(modifier, agsl, pointsProvider, colorsProvider)
    } else {
        GlStardustMesh(modifier, width, height, subdivisions, pointsProvider, colorsProvider)
    }
}

@Composable
internal actual fun Modifier.stardustMeshContent(
    width: Int,
    height: Int,
    subdivisions: Int,
    pointsProvider: () -> FloatArray,
    colorsProvider: () -> FloatArray,
): Modifier {
    val agsl: MeshShaderProgram? = remember(width, height) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching { AgslMeshProgram(width, height) }.getOrNull()
        } else null
    }
    return if (agsl != null) {
        this.drawBehind { drawMesh(agsl, pointsProvider(), colorsProvider()) }
    } else {
        this.drawBehind {
            drawFallbackMesh(width, height, subdivisions, pointsProvider(), colorsProvider())
        }
    }
}

private fun DrawScope.drawFallbackMesh(
    gridWidth: Int,
    gridHeight: Int,
    subdivisions: Int,
    points: FloatArray,
    colors: FloatArray,
) {
    if (size.width <= 0f || size.height <= 0f) return
    val controlCount = gridWidth * gridHeight
    if (points.size < controlCount * 2 || colors.size < controlCount * 4) return

    val ptsList = List(controlCount) { i ->
        Offset(points[i * 2], points[i * 2 + 1])
    }
    val colList = List(controlCount) { i ->
        Color(colors[i * 4], colors[i * 4 + 1], colors[i * 4 + 2], colors[i * 4 + 3])
    }

    val result = MeshInterpolation.generateMeshVerticesAndColors(
        ptsList, colList, gridWidth, gridHeight, subdivisions, subdivisions
    )

    val scaledVerts = FloatArray(result.vertices.size) { i ->
        if (i % 2 == 0) result.vertices[i] * size.width else result.vertices[i] * size.height
    }

    val intColors = IntArray(result.colors.size / 4) { i ->
        val r = (result.colors[i * 4] * 255f).toInt().coerceIn(0, 255)
        val g = (result.colors[i * 4 + 1] * 255f).toInt().coerceIn(0, 255)
        val b = (result.colors[i * 4 + 2] * 255f).toInt().coerceIn(0, 255)
        val a = (result.colors[i * 4 + 3] * 255f).toInt().coerceIn(0, 255)
        (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    val indicesShort = ShortArray(result.indices.size) { i -> result.indices[i].toShort() }

    drawContext.canvas.nativeCanvas.drawVertices(
        android.graphics.Canvas.VertexMode.TRIANGLES,
        scaledVerts.size,
        scaledVerts,
        0,
        null,
        0,
        intColors,
        0,
        indicesShort,
        0,
        indicesShort.size,
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    )
}
