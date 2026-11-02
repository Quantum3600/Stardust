package io.github.quantum3600.stardust

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Shader
import android.os.Build
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import io.github.quantum3600.stardust.utils.MeshInterpolation
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import kotlin.math.max
import kotlin.math.min

internal actual class MeshGradientShader actual constructor(
    private val width: Int,
    private val height: Int,
) {
    private val maxPackedPoints = (maxControlPoints + 1) / 2
    private val pointUniforms = FloatArray(maxPackedPoints * 4)
    private val colorUniforms = FloatArray(maxControlPoints * 4)
    private var currentBrush: Brush? = null
    private var currentSize = Size.Unspecified
    private var dirty = true
    private var fallbackStart = Color.Transparent
    private var fallbackEnd = Color.Transparent

    // Fallback tessellation buffers (Pre-API 33)
    private val subdivPerPatch = 16
    private val totalX = (width - 1) * subdivPerPatch + 1
    private val totalY = (height - 1) * subdivPerPatch + 1
    private val totalVertices = totalX * totalY
    private val indexCount = (totalX - 1) * (totalY - 1) * 6

    private val cachedPoints = Array(maxControlPoints) { Offset.Zero }
    private val cachedColors = Array(maxControlPoints) { Color.Transparent }

    private var fallbackVertices: FloatArray? = null
    private var fallbackColors: IntArray? = null
    private var fallbackIndices: ShortArray? = null
    private var rowPositionsX: FloatArray? = null
    private var rowColorsR: FloatArray? = null
    private var rowColorsG: FloatArray? = null
    private var rowColorsB: FloatArray? = null
    private var rowColorsA: FloatArray? = null
    private val fallbackPaint = Paint()
    private var fallbackDirty = true
    private var fallbackSize = Size.Unspecified

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
            val point = points[i]
            val color = colors[i]

            val px = i * 2
            if (pointUniforms[px] != point.x || pointUniforms[px + 1] != point.y) {
                changed = true
            }
            pointUniforms[px] = point.x
            pointUniforms[px + 1] = point.y

            val cx = i * 4
            if (colorUniforms[cx] != color.red ||
                colorUniforms[cx + 1] != color.green ||
                colorUniforms[cx + 2] != color.blue ||
                colorUniforms[cx + 3] != color.alpha
            ) {
                changed = true
            }
            colorUniforms[cx] = color.red
            colorUniforms[cx + 1] = color.green
            colorUniforms[cx + 2] = color.blue
            colorUniforms[cx + 3] = color.alpha

            cachedPoints[i] = point
            cachedColors[i] = color
        }
        fallbackStart = colors.firstOrNull() ?: Color.Transparent
        fallbackEnd = colors.lastOrNull() ?: Color.Transparent
        if (changed) {
            dirty = true
            fallbackDirty = true
            currentBrush = null
        }
    }

    actual fun brush(size: Size): Brush {
        if (Build.VERSION.SDK_INT >= 33 && RuntimeShaderReflection.isAvailable) {
            if (!dirty && currentBrush != null && currentSize == size) return currentBrush!!
            currentSize = size
            val shader = RuntimeShaderReflection.create(
                source = MeshGradientShaderSource,
                width = width,
                height = height,
                resolution = floatArrayOf(size.width, size.height),
                points = pointUniforms,
                colors = colorUniforms,
                dustIntensity = currentDustIntensity,
                dustPhase = currentDustPhase,
            )
            currentBrush = object : ShaderBrush() {
                override fun createShader(size: Size): androidx.compose.ui.graphics.Shader = shader
            }
            dirty = false
            return currentBrush!!
        }
        return Brush.linearGradient(listOf(fallbackStart, fallbackEnd))
    }

    actual fun draw(drawScope: DrawScope) {
        if (Build.VERSION.SDK_INT >= 33 && RuntimeShaderReflection.isAvailable) {
            drawScope.drawRect(brush = brush(drawScope.size))
        } else {
            drawFallback(drawScope)
        }
    }

    actual fun close() {
        currentBrush = null
    }

    private fun initFallbackBuffers() {
        if (fallbackIndices == null) {
            fallbackVertices = FloatArray(totalVertices * 2)
            fallbackColors = IntArray(totalVertices)
            rowPositionsX = FloatArray(height * totalX * 2)
            rowColorsR = FloatArray(height * totalX)
            rowColorsG = FloatArray(height * totalX)
            rowColorsB = FloatArray(height * totalX)
            rowColorsA = FloatArray(height * totalX)

            val indices = ShortArray(indexCount)
            var k = 0
            for (y in 0 until totalY - 1) {
                val row = y * totalX
                val next = (y + 1) * totalX
                for (x in 0 until totalX - 1) {
                    val tl = (row + x).toShort()
                    val tr = (row + x + 1).toShort()
                    val bl = (next + x).toShort()
                    val br = (next + x + 1).toShort()

                    indices[k++] = tl
                    indices[k++] = tr
                    indices[k++] = bl

                    indices[k++] = tr
                    indices[k++] = br
                    indices[k++] = bl
                }
            }
            fallbackIndices = indices
        }
    }

    private fun drawFallback(drawScope: DrawScope) {
        val size = drawScope.size
        if (size.width <= 0f || size.height <= 0f) return

        initFallbackBuffers()
        val verts = fallbackVertices ?: return
        val cols = fallbackColors ?: return
        val indices = fallbackIndices ?: return

        if (fallbackDirty || fallbackSize != size) {
            evaluateFallbackMesh(size, verts, cols)
            fallbackDirty = false
            fallbackSize = size
        }

        drawScope.drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawVertices(
                Canvas.VertexMode.TRIANGLES,
                totalVertices * 2,
                verts,
                0,
                null,
                0,
                cols,
                0,
                indices,
                0,
                indexCount,
                fallbackPaint
            )
        }
    }

    private fun getCachedPoint(x: Int, y: Int): Offset {
        val clampedX = x.coerceIn(0, width - 1)
        val clampedY = y.coerceIn(0, height - 1)
        return cachedPoints[clampedY * width + clampedX]
    }

    private fun getCachedColor(x: Int, y: Int): Color {
        val clampedX = x.coerceIn(0, width - 1)
        val clampedY = y.coerceIn(0, height - 1)
        return cachedColors[clampedY * width + clampedX]
    }

    private fun evaluateFallbackMesh(size: Size, verts: FloatArray, cols: IntArray) {
        val posX = rowPositionsX ?: return
        val colR = rowColorsR ?: return
        val colG = rowColorsG ?: return
        val colB = rowColorsB ?: return
        val colA = rowColorsA ?: return

        val subdivX = subdivPerPatch
        val subdivY = subdivPerPatch

        // Pass 1: Interpolate along X for each control row
        for (gy in 0 until height) {
            for (gx in 0 until width - 1) {
                val p0 = getCachedPoint(max(gx - 1, 0), gy)
                val p1 = getCachedPoint(gx, gy)
                val p2 = getCachedPoint(gx + 1, gy)
                val p3 = getCachedPoint(min(gx + 2, width - 1), gy)

                val c0 = getCachedColor(max(gx - 1, 0), gy)
                val c1 = getCachedColor(gx, gy)
                val c2 = getCachedColor(gx + 1, gy)
                val c3 = getCachedColor(min(gx + 2, width - 1), gy)

                for (i in 0 until subdivX) {
                    val t = i.toFloat() / subdivX.toFloat()
                    val px = MeshInterpolation.catmullRom(p0.x, p1.x, p2.x, p3.x, t)
                    val py = MeshInterpolation.catmullRom(p0.y, p1.y, p2.y, p3.y, t)
                    val cr = MeshInterpolation.catmullRom(c0.red, c1.red, c2.red, c3.red, t)
                    val cg = MeshInterpolation.catmullRom(c0.green, c1.green, c2.green, c3.green, t)
                    val cb = MeshInterpolation.catmullRom(c0.blue, c1.blue, c2.blue, c3.blue, t)
                    val ca = MeshInterpolation.catmullRom(c0.alpha, c1.alpha, c2.alpha, c3.alpha, t)

                    val ix = gx * subdivX + i
                    val rowIdx = gy * totalX + ix
                    posX[rowIdx * 2] = px
                    posX[rowIdx * 2 + 1] = py
                    colR[rowIdx] = cr
                    colG[rowIdx] = cg
                    colB[rowIdx] = cb
                    colA[rowIdx] = ca
                }
            }
            // Last point in row
            val lastIx = (width - 1) * subdivX
            val pLast = getCachedPoint(width - 1, gy)
            val cLast = getCachedColor(width - 1, gy)
            val lastRowIdx = gy * totalX + lastIx
            posX[lastRowIdx * 2] = pLast.x
            posX[lastRowIdx * 2 + 1] = pLast.y
            colR[lastRowIdx] = cLast.red
            colG[lastRowIdx] = cLast.green
            colB[lastRowIdx] = cLast.blue
            colA[lastRowIdx] = cLast.alpha
        }

        // Pass 2: Interpolate along Y for each column
        for (ix in 0 until totalX) {
            for (gy in 0 until height - 1) {
                val r0 = max(gy - 1, 0) * totalX + ix
                val r1 = gy * totalX + ix
                val r2 = (gy + 1) * totalX + ix
                val r3 = min(gy + 2, height - 1) * totalX + ix

                val p0x = posX[r0 * 2]; val p0y = posX[r0 * 2 + 1]
                val p1x = posX[r1 * 2]; val p1y = posX[r1 * 2 + 1]
                val p2x = posX[r2 * 2]; val p2y = posX[r2 * 2 + 1]
                val p3x = posX[r3 * 2]; val p3y = posX[r3 * 2 + 1]

                val c0r = colR[r0]; val c0g = colG[r0]; val c0b = colB[r0]; val c0a = colA[r0]
                val c1r = colR[r1]; val c1g = colG[r1]; val c1b = colB[r1]; val c1a = colA[r1]
                val c2r = colR[r2]; val c2g = colG[r2]; val c2b = colB[r2]; val c2a = colA[r2]
                val c3r = colR[r3]; val c3g = colG[r3]; val c3b = colB[r3]; val c3a = colA[r3]

                for (i in 0 until subdivY) {
                    val t = i.toFloat() / subdivY.toFloat()
                    val px = MeshInterpolation.catmullRom(p0x, p1x, p2x, p3x, t)
                    val py = MeshInterpolation.catmullRom(p0y, p1y, p2y, p3y, t)
                    val cr = MeshInterpolation.catmullRom(c0r, c1r, c2r, c3r, t)
                    val cg = MeshInterpolation.catmullRom(c0g, c1g, c2g, c3g, t)
                    val cb = MeshInterpolation.catmullRom(c0b, c1b, c2b, c3b, t)
                    val ca = MeshInterpolation.catmullRom(c0a, c1a, c2a, c3a, t)

                    val iy = gy * subdivY + i
                    val outIdx = iy * totalX + ix
                    verts[outIdx * 2] = px * size.width
                    verts[outIdx * 2 + 1] = py * size.height

                    val aInt = (ca.coerceIn(0f, 1f) * 255f + 0.5f).toInt()
                    val rInt = (cr.coerceIn(0f, 1f) * 255f + 0.5f).toInt()
                    val gInt = (cg.coerceIn(0f, 1f) * 255f + 0.5f).toInt()
                    val bInt = (cb.coerceIn(0f, 1f) * 255f + 0.5f).toInt()
                    cols[outIdx] = (aInt shl 24) or (rInt shl 16) or (gInt shl 8) or bInt
                }
            }
            // Last point in column
            val lastIy = (height - 1) * subdivY
            val rLast = (height - 1) * totalX + ix
            val outLastIdx = lastIy * totalX + ix
            verts[outLastIdx * 2] = posX[rLast * 2] * size.width
            verts[outLastIdx * 2 + 1] = posX[rLast * 2 + 1] * size.height

            val aInt = (colA[rLast].coerceIn(0f, 1f) * 255f + 0.5f).toInt()
            val rInt = (colR[rLast].coerceIn(0f, 1f) * 255f + 0.5f).toInt()
            val gInt = (colG[rLast].coerceIn(0f, 1f) * 255f + 0.5f).toInt()
            val bInt = (colB[rLast].coerceIn(0f, 1f) * 255f + 0.5f).toInt()
            cols[outLastIdx] = (aInt shl 24) or (rInt shl 16) or (gInt shl 8) or bInt
        }
    }

    actual companion object {
        actual val maxControlPoints: Int = 25
    }
}

private object RuntimeShaderReflection {
    val isAvailable: Boolean = try {
        Class.forName("android.graphics.RuntimeShader") != null
    } catch (_: Throwable) {
        false
    }

    private val runtimeShaderClass: Class<*>? = try {
        Class.forName("android.graphics.RuntimeShader")
    } catch (_: ClassNotFoundException) {
        null
    }
    private val constructor: Constructor<*>? = runtimeShaderClass?.getConstructor(String::class.java)
    private val setFloatUniform: Method? = runtimeShaderClass?.getMethod(
        "setFloatUniform",
        String::class.java,
        FloatArray::class.java,
    )
    private val setIntUniform: Method? = runtimeShaderClass?.getMethod(
        "setIntUniform",
        String::class.java,
        Int::class.javaPrimitiveType,
    )

    private val setFloatUniformScalar: Method? = runtimeShaderClass?.getMethod(
        "setFloatUniform",
        String::class.java,
        Float::class.javaPrimitiveType,
    )

    fun create(
        source: String,
        width: Int,
        height: Int,
        resolution: FloatArray,
        points: FloatArray,
        colors: FloatArray,
        dustIntensity: Float,
        dustPhase: Float,
    ): Shader {
        val instance = requireNotNull(constructor).newInstance(source)
        requireNotNull(setFloatUniform).invoke(instance, "uResolution", resolution)
        requireNotNull(setIntUniform).invoke(instance, "uWidth", width)
        requireNotNull(setIntUniform).invoke(instance, "uHeight", height)
        requireNotNull(setFloatUniform).invoke(instance, "uPackedPoints", points)
        requireNotNull(setFloatUniform).invoke(instance, "uColors", colors)
        requireNotNull(setFloatUniformScalar).invoke(instance, "uDustIntensity", dustIntensity)
        requireNotNull(setFloatUniformScalar).invoke(instance, "uDustPhase", dustPhase)
        return instance as Shader
    }
}
