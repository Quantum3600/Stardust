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
