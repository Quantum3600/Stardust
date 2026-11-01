package io.github.om252345.composemeshgradient

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder
import org.jetbrains.skia.Shader

internal actual class MeshGradientShader actual constructor(
    private val width: Int,
    private val height: Int,
) {
    private val effect = RuntimeEffect.makeForShader(MeshGradientShaderSource)
    private val builder = RuntimeShaderBuilder(effect)
    private val pointUniforms = FloatArray(maxControlPoints * 4)
    private val colorUniforms = FloatArray(maxControlPoints * 4)
    private var dirty = true
    private var currentBrush: Brush? = null
    private var currentSize = Size.Unspecified
    private var currentShader: Shader? = null

    actual fun update(points: Array<Offset>, colors: Array<Color>) {
        var changed = dirty
        for (i in points.indices) {
            val p = i * 4
            val point = points[i]
            val color = colors[i]
            if (pointUniforms[p] != point.x || pointUniforms[p + 1] != point.y ||
                colorUniforms[p] != color.red || colorUniforms[p + 1] != color.green ||
                colorUniforms[p + 2] != color.blue || colorUniforms[p + 3] != color.alpha
            ) changed = true
            pointUniforms[p] = point.x
            pointUniforms[p + 1] = point.y
            pointUniforms[p + 2] = 0f
            pointUniforms[p + 3] = 0f
            floatArrayOf(color.red, color.green, color.blue, color.alpha).copyInto(
                colorUniforms,
                destinationOffset = p,
            )
        }
        if (changed) {
            dirty = true
            currentBrush = null
            currentShader?.close()
            currentShader = null
        }
    }

    actual fun brush(size: Size): Brush {
        if (!dirty && currentBrush != null && currentSize == size) return currentBrush!!
        currentSize = size
        builder.uniform("uResolution", size.width, size.height)
        builder.uniform("uWidth", width)
        builder.uniform("uHeight", height)
        builder.uniform("uPoints", pointUniforms)
        builder.uniform("uColors", colorUniforms)
        currentShader?.close()
        currentShader = builder.makeShader()
        currentBrush = object : ShaderBrush() {
            override fun createShader(size: Size): androidx.compose.ui.graphics.Shader = currentShader!!
        }
        dirty = false
        return currentBrush!!
    }

    actual companion object {
        actual val maxControlPoints: Int = 64
    }
}
