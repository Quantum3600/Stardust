package io.github.quantum3600.stardust

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntSize
import io.github.quantum3600.stardust.utils.computeDefaultTangents
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder
import org.jetbrains.skia.Shader

internal actual class StardustMeshShader actual constructor(
    private val width: Int,
    private val height: Int,
) {
    private val effect = RuntimeEffect.makeForShader(StardustMeshShaderSource)
    private val builder = RuntimeShaderBuilder(effect)
    private val maxPackedPoints = (maxControlPoints + 1) / 2
    private val pointUniforms = FloatArray(maxPackedPoints * 4)
    private val colorUniforms = FloatArray(maxControlPoints * 4)
    private val uTangentUniforms = FloatArray(maxControlPoints * 4)
    private val vTangentUniforms = FloatArray(maxControlPoints * 4)
    private var dirty = true
    private var currentBrush: Brush? = null
    private var currentSize = Size.Unspecified
    private var currentShader: Shader? = null

    private var currentDustIntensity: Float = 0f
    private var currentDustPhase: Float = 0f

    private var offscreenBitmap: ImageBitmap? = null
    private var offscreenCanvas: androidx.compose.ui.graphics.Canvas? = null
    private val offscreenScope = CanvasDrawScope()

    actual fun update(
        points: Array<Offset>,
        colors: Array<Color>,
        uTangents: FloatArray?,
        vTangents: FloatArray?,
        dustIntensity: Float,
        dustPhase: Float
    ) {
        var changed = dirty
        if (currentDustIntensity != dustIntensity) {
            currentDustIntensity = dustIntensity
            changed = true
        }
        if (currentDustPhase != dustPhase) {
            currentDustPhase = dustPhase
            changed = true
        }

        var pointsChanged = false
        val pointCount = minOf(points.size, maxControlPoints)
        for (i in 0 until pointCount) {
            val px = i * 2
            val point = points[i]
            if (pointUniforms[px] != point.x || pointUniforms[px + 1] != point.y) {
                pointsChanged = true
                pointUniforms[px] = point.x
                pointUniforms[px + 1] = point.y
            }
        }
        if (pointsChanged) changed = true

        val colorCount = minOf(colors.size, maxControlPoints)
        for (i in 0 until colorCount) {
            val cx = i * 4
            val color = colors[i]
            if (colorUniforms[cx] != color.red || colorUniforms[cx + 1] != color.green ||
                colorUniforms[cx + 2] != color.blue || colorUniforms[cx + 3] != color.alpha
            ) {
                changed = true
                colorUniforms[cx] = color.red
                colorUniforms[cx + 1] = color.green
                colorUniforms[cx + 2] = color.blue
                colorUniforms[cx + 3] = color.alpha
            }
        }

        if (uTangents != null && vTangents != null) {
            for (i in uTangents.indices) {
                if (uTangentUniforms[i] != uTangents[i]) {
                    uTangentUniforms[i] = uTangents[i]
                    changed = true
                }
            }
            for (i in vTangents.indices) {
                if (vTangentUniforms[i] != vTangents[i]) {
                    vTangentUniforms[i] = vTangents[i]
                    changed = true
                }
            }
        } else if (pointsChanged || dirty) {
            computeDefaultTangents(width, height, points, uTangentUniforms, vTangentUniforms)
            changed = true
        }

        if (changed) {
            dirty = true
        }
    }

    actual fun update(
        points: FloatArray,
        colors: FloatArray,
        uTangents: FloatArray?,
        vTangents: FloatArray?,
        dustIntensity: Float,
        dustPhase: Float
    ) {
        var changed = dirty
        if (currentDustIntensity != dustIntensity) {
            currentDustIntensity = dustIntensity
            changed = true
        }
        if (currentDustPhase != dustPhase) {
            currentDustPhase = dustPhase
            changed = true
        }

        var pointsChanged = false
        val pointCount = minOf(points.size / 2, maxControlPoints)
        for (i in 0 until pointCount) {
            val px = i * 2
            if (pointUniforms[px] != points[px] || pointUniforms[px + 1] != points[px + 1]) {
                pointsChanged = true
                pointUniforms[px] = points[px]
                pointUniforms[px + 1] = points[px + 1]
            }
        }
        if (pointsChanged) changed = true

        val colorCount = minOf(colors.size / 4, maxControlPoints)
        for (i in 0 until colorCount) {
            val cx = i * 4
            if (colorUniforms[cx] != colors[cx] ||
                colorUniforms[cx + 1] != colors[cx + 1] ||
                colorUniforms[cx + 2] != colors[cx + 2] ||
                colorUniforms[cx + 3] != colors[cx + 3]
            ) {
                changed = true
                colorUniforms[cx] = colors[cx]
                colorUniforms[cx + 1] = colors[cx + 1]
                colorUniforms[cx + 2] = colors[cx + 2]
                colorUniforms[cx + 3] = colors[cx + 3]
            }
        }

        if (uTangents != null && vTangents != null) {
            for (i in uTangents.indices) {
                if (uTangentUniforms[i] != uTangents[i]) {
                    uTangentUniforms[i] = uTangents[i]
                    changed = true
                }
            }
            for (i in vTangents.indices) {
                if (vTangentUniforms[i] != vTangents[i]) {
                    vTangentUniforms[i] = vTangents[i]
                    changed = true
                }
            }
        } else if (pointsChanged || dirty) {
            computeDefaultTangents(width, height, points, uTangentUniforms, vTangentUniforms)
            changed = true
        }

        if (changed) {
            dirty = true
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
        builder.uniform("uUTangents", uTangentUniforms)
        builder.uniform("uVTangents", vTangentUniforms)
        builder.uniform("uGrainIntensity", currentDustIntensity)
        builder.uniform("uGrainSeed", currentDustPhase)

        val newShader = builder.makeShader()
        currentShader?.close()
        currentShader = newShader
        currentBrush = object : ShaderBrush() {
            override fun createShader(size: Size): androidx.compose.ui.graphics.Shader = newShader
        }
        dirty = false
        return currentBrush!!
    }

    actual fun draw(drawScope: DrawScope, downsampleFactor: Float) {
        if (downsampleFactor in 0.1f..0.999f) {
            val targetWidth = (drawScope.size.width * downsampleFactor).toInt().coerceAtLeast(1)
            val targetHeight = (drawScope.size.height * downsampleFactor).toInt().coerceAtLeast(1)
            val targetSize = Size(targetWidth.toFloat(), targetHeight.toFloat())

            var bitmap = offscreenBitmap
            if (bitmap == null || bitmap.width != targetWidth || bitmap.height != targetHeight) {
                bitmap = ImageBitmap(targetWidth, targetHeight)
                offscreenBitmap = bitmap
                offscreenCanvas = androidx.compose.ui.graphics.Canvas(bitmap)
            }
            val canvas = offscreenCanvas!!
            offscreenScope.draw(
                density = drawScope,
                layoutDirection = drawScope.layoutDirection,
                canvas = canvas,
                size = targetSize
            ) {
                drawRect(brush = brush(targetSize))
            }
            drawScope.drawImage(
                image = bitmap,
                dstSize = IntSize(drawScope.size.width.toInt(), drawScope.size.height.toInt()),
                filterQuality = FilterQuality.Medium
            )
        } else {
            drawScope.drawRect(brush = brush(drawScope.size))
        }
    }

    actual fun close() {
        currentShader?.close()
        currentShader = null
        currentBrush = null
        offscreenBitmap = null
        offscreenCanvas = null
    }

    actual companion object {
        actual val maxControlPoints: Int = 25
    }
}
