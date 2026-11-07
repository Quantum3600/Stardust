package io.github.quantum3600.stardust

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.DrawScope

/** A fragment-shader backed renderer (AGSL on Android 33+, SkSL on Skia targets). */
internal interface MeshShaderProgram {
    /** Uploads uniforms and returns a brush that paints the mesh over [size]. Called in the draw phase. */
    fun createBrush(points: FloatArray, colors: FloatArray, size: Size): Brush
}

/**
 * Reads [pointsProvider]/[colorsProvider] in the *draw* phase, so animating a
 * [MeshGradientState] only invalidates drawing — no recomposition, no relayout.
 */
@Composable
internal fun ShaderMeshGradient(
    modifier: Modifier,
    program: MeshShaderProgram,
    pointsProvider: () -> FloatArray,
    colorsProvider: () -> FloatArray,
) {
    Spacer(
        modifier.drawBehind { drawMesh(program, pointsProvider(), colorsProvider()) }
    )
}

internal fun DrawScope.drawMesh(program: MeshShaderProgram, points: FloatArray, colors: FloatArray) {
    if (size.width <= 0f || size.height <= 0f) return
    drawRect(brush = program.createBrush(points, colors, size))
}

/** Platform picks the best backend (Android: AGSL -> OpenGL, Skia targets: SkSL). */
@Composable
internal expect fun StardustMeshContent(
    modifier: Modifier,
    width: Int,
    height: Int,
    subdivisions: Int,
    pointsProvider: () -> FloatArray,
    colorsProvider: () -> FloatArray,
)

/** Platform implementation of the stardust mesh gradient modifier. */
@Composable
internal expect fun Modifier.stardustMeshContent(
    width: Int,
    height: Int,
    subdivisions: Int,
    pointsProvider: () -> FloatArray,
    colorsProvider: () -> FloatArray,
): Modifier
