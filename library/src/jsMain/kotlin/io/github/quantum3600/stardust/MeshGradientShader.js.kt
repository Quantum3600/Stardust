package io.github.quantum3600.stardust

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder
import org.jetbrains.skia.Shader

internal actual class MeshGradientShader actual constructor(
    private val width: Int,
    private val height: Int,
) {
    private val effect = RuntimeEffect.makeForShader(MeshGradientShaderSource)
    private val builder = RuntimeShaderBuilder(effect)
    private val maxPackedPoints = (maxControlPoints + 1) / 2
    private val pointUniforms = FloatArray(maxPackedPoints * 4)
    private val colorUniforms = FloatArray(maxControlPoints * 4)
    private var dirty = true
    private var currentBrush: Brush? = null
    private var currentSize = Size.Unspecified
    private var currentShader: Shader? = null

    private var currentDustIntensity: Float = 0f
    private var currentDustPhase: Float = 0f

    actual fun update(points: Array<Offset>, colors: Array<Color>, dustIntensity: Float, dustPhase: Float) {
        var changed = dirty
        if (currentDustIntensity != dustIntensity) {
            currentDustIntensity = dustIntensity
            changed = true
        }
        if (currentDustPhase != dustPhase) {
            currentDustPhase = dustPhase
            changed = true
        }
        for (i in points.indices) {
            val px = i * 2
            val cx = i * 4
            val point = points[i]
            val color = colors[i]
            if (pointUniforms[px] != point.x || pointUniforms[px + 1] != point.y ||
                colorUniforms[cx] != color.red || colorUniforms[cx + 1] != color.green ||
                colorUniforms[cx + 2] != color.blue || colorUniforms[cx + 3] != color.alpha
            ) {
                changed = true
            }
            pointUniforms[px] = point.x
            pointUniforms[px + 1] = point.y
            colorUniforms[cx] = color.red
            colorUniforms[cx + 1] = color.green
            colorUniforms[cx + 2] = color.blue
            colorUniforms[cx + 3] = color.alpha
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
        builder.uniform("uPackedPoints", pointUniforms)
        builder.uniform("uColors", colorUniforms)
        builder.uniform("uDustIntensity", currentDustIntensity)
        builder.uniform("uDustPhase", currentDustPhase)
        currentShader?.close()
        currentShader = builder.makeShader()
        currentBrush = object : ShaderBrush() {
            override fun createShader(size: Size): androidx.compose.ui.graphics.Shader = currentShader!!
        }
        dirty = false
        return currentBrush!!
    }

    actual fun draw(drawScope: DrawScope) {
        drawScope.drawRect(brush = brush(drawScope.size))
    }

    actual fun close() {
        currentShader?.close()
        currentShader = null
        currentBrush = null
    }

    actual companion object {
        actual val maxControlPoints: Int = 25
    }
}
