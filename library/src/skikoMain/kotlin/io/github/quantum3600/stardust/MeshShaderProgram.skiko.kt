package io.github.quantum3600.stardust

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asComposeShader
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder

internal class SkslMeshProgram(gridWidth: Int, gridHeight: Int) : MeshShaderProgram {
    private val effect = RuntimeEffect.makeForShader(MeshShaderSource.build(gridWidth, gridHeight))

    override fun createBrush(points: FloatArray, colors: FloatArray, size: Size): Brush {
        val builder = RuntimeShaderBuilder(effect)
        builder.uniform(MeshShaderSource.U_RESOLUTION, size.width, size.height)
        builder.uniform(MeshShaderSource.U_POINTS, points)
        builder.uniform(MeshShaderSource.U_COLORS, colors)
        return ShaderBrush(builder.makeShader().asComposeShader())
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
    val program = remember(width, height) { SkslMeshProgram(width, height) }
    ShaderMeshGradient(modifier, program, pointsProvider, colorsProvider)
}
