package io.github.quantum3600.stardust

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Shader
import android.os.Build
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.IntSize
import io.github.quantum3600.stardust.gles.HardwareBufferOffscreenPipeline
import io.github.quantum3600.stardust.utils.MeshInterpolation
import io.github.quantum3600.stardust.utils.computeDefaultTangents
import io.github.quantum3600.stardust.utils.lerp
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import kotlin.math.max
import kotlin.math.min

internal actual class StardustMeshShader actual constructor(
    private val width: Int,
    private val height: Int,
) {
    private val maxPackedPoints = (maxControlPoints + 1) / 2
    private val pointUniforms = FloatArray(maxPackedPoints * 4)
    private val colorUniforms = FloatArray(maxControlPoints * 4)
    private val uTangentUniforms = FloatArray(maxControlPoints * 4)
    private val vTangentUniforms = FloatArray(maxControlPoints * 4)
    private var currentBrush: Brush? = null
    private var currentSize = Size.Unspecified
    private var dirty = true
    private var fallbackStart = Color.Transparent
    private var fallbackEnd = Color.Transparent

    // Pre-API 33 OpenGL ES HardwareBuffer offscreen pipeline
    private var latestGLESFrame: ImageBitmap? = null
    private var offscreenPipeline: Any? = null

    init {
        if (Build.VERSION.SDK_INT in 29..32) {
            try {
                offscreenPipeline = HardwareBufferOffscreenPipeline { frame ->
                    latestGLESFrame = frame
                }
            } catch (_: Throwable) {
                offscreenPipeline = null
            }
        }
    }

    // Fallback tessellation buffers (Pre-API 33 fallback)
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
            val point = points[i]
            val px = i * 2
            if (pointUniforms[px] != point.x || pointUniforms[px + 1] != point.y) {
                pointsChanged = true
                pointUniforms[px] = point.x
                pointUniforms[px + 1] = point.y
            }
            cachedPoints[i] = point
        }
        if (pointsChanged) changed = true

        val colorCount = minOf(colors.size, maxControlPoints)
        for (i in 0 until colorCount) {
            val color = colors[i]
            val cx = i * 4
            if (colorUniforms[cx] != color.red ||
                colorUniforms[cx + 1] != color.green ||
                colorUniforms[cx + 2] != color.blue ||
                colorUniforms[cx + 3] != color.alpha
            ) {
                changed = true
                colorUniforms[cx] = color.red
                colorUniforms[cx + 1] = color.green
                colorUniforms[cx + 2] = color.blue
                colorUniforms[cx + 3] = color.alpha
            }
            cachedColors[i] = color
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

        fallbackStart = colors.firstOrNull() ?: Color.Transparent
        fallbackEnd = colors.lastOrNull() ?: Color.Transparent
        if (changed) {
            dirty = true
            fallbackDirty = true
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
            cachedPoints[i] = Offset(points[px], points[px + 1])
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
            cachedColors[i] = Color(colors[cx], colors[cx + 1], colors[cx + 2], colors[cx + 3])
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

        fallbackStart = if (colors.size >= 4) Color(colors[0], colors[1], colors[2], colors[3]) else Color.Transparent
        fallbackEnd = if (colors.size >= 4) Color(colors[colors.size - 4], colors[colors.size - 3], colors[colors.size - 2], colors[colors.size - 1]) else Color.Transparent

        if (changed) {
            dirty = true
            fallbackDirty = true
        }
    }

    private var runtimeShaderInstance: Shader? = null
    private val resolutionArray = FloatArray(2)

    actual fun brush(size: Size): Brush {
        if (Build.VERSION.SDK_INT >= 33 && RuntimeShaderReflection.isAvailable) {
            if (!dirty && currentBrush != null && currentSize == size) return currentBrush!!
            currentSize = size
            if (runtimeShaderInstance == null) {
                runtimeShaderInstance = RuntimeShaderReflection.createShader(MeshGradientShaderSource)
            }
            val shader = runtimeShaderInstance
            if (shader != null) {
                resolutionArray[0] = size.width
                resolutionArray[1] = size.height
                RuntimeShaderReflection.updateUniforms(
                    instance = shader,
                    width = width,
                    height = height,
                    resolution = resolutionArray,
                    points = pointUniforms,
                    colors = colorUniforms,
                    uTangents = uTangentUniforms,
                    vTangents = vTangentUniforms,
                    grainIntensity = currentDustIntensity,
                    grainSeed = currentDustPhase,
                )
                if (currentBrush == null) {
                    currentBrush = object : ShaderBrush() {
                        override fun createShader(size: Size): androidx.compose.ui.graphics.Shader = shader
                    }
                }
                dirty = false
                return currentBrush!!
            }
        }
        return Brush.linearGradient(listOf(fallbackStart, fallbackEnd))
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
            if (Build.VERSION.SDK_INT >= 33 && RuntimeShaderReflection.isAvailable) {
                drawScope.drawRect(brush = brush(drawScope.size))
            } else if (Build.VERSION.SDK_INT >= 29 && offscreenPipeline is HardwareBufferOffscreenPipeline) {
                val pipeline = offscreenPipeline as HardwareBufferOffscreenPipeline
                val w = drawScope.size.width.toInt().coerceAtLeast(1)
                val h = drawScope.size.height.toInt().coerceAtLeast(1)

                pipeline.render(
                    width = w,
                    height = h,
                    meshWidth = width,
                    meshHeight = height,
                    packedPoints = pointUniforms,
                    colors = colorUniforms,
                    uTangents = uTangentUniforms,
                    vTangents = vTangentUniforms,
                    grainIntensity = currentDustIntensity,
                    grainSeed = currentDustPhase
                )

                val frame = latestGLESFrame
                if (frame != null) {
                    drawScope.drawImage(
                        image = frame,
                        dstSize = IntSize(w, h),
                        filterQuality = FilterQuality.Low
                    )
                } else {
                    drawFallback(drawScope)
                }
            } else {
                drawFallback(drawScope)
            }
        }
    }

    actual fun close() {
        currentBrush = null
        runtimeShaderInstance = null
        offscreenBitmap = null
        offscreenCanvas = null
        if (Build.VERSION.SDK_INT >= 29 && offscreenPipeline is HardwareBufferOffscreenPipeline) {
            (offscreenPipeline as HardwareBufferOffscreenPipeline).release()
            offscreenPipeline = null
        }
        latestGLESFrame = null
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
                fallbackIndices!!,
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

    private fun evaluateCubicPatchPosition(
        cellX: Int,
        cellY: Int,
        u: Float,
        v: Float,
        size: Size
    ): Offset {
        val tl = getCachedPoint(cellX, cellY)
        val tr = getCachedPoint(cellX + 1, cellY)
        val bl = getCachedPoint(cellX, cellY + 1)
        val br = getCachedPoint(cellX + 1, cellY + 1)

        val idxTL = cellY * width + cellX
        val idxTR = cellY * width + (cellX + 1)
        val idxBL = (cellY + 1) * width + cellX
        val idxBR = (cellY + 1) * width + (cellX + 1)

        val uTanTL_trailing = Offset(uTangentUniforms[idxTL * 4 + 2], uTangentUniforms[idxTL * 4 + 3])
        val uTanTR_leading = Offset(uTangentUniforms[idxTR * 4 + 0], uTangentUniforms[idxTR * 4 + 1])
        val uTanBL_trailing = Offset(uTangentUniforms[idxBL * 4 + 2], uTangentUniforms[idxBL * 4 + 3])
        val uTanBR_leading = Offset(uTangentUniforms[idxBR * 4 + 0], uTangentUniforms[idxBR * 4 + 1])

        val vTanTL_bottom = Offset(vTangentUniforms[idxTL * 4 + 2], vTangentUniforms[idxTL * 4 + 3])
        val vTanTR_bottom = Offset(vTangentUniforms[idxTR * 4 + 2], vTangentUniforms[idxTR * 4 + 3])
        val vTanBL_top = Offset(vTangentUniforms[idxBL * 4 + 0], vTangentUniforms[idxBL * 4 + 1])
        val vTanBR_top = Offset(vTangentUniforms[idxBR * 4 + 0], vTangentUniforms[idxBR * 4 + 1])

        val oneMinusU = 1f - u
        val bu0 = oneMinusU * oneMinusU * oneMinusU
        val bu1 = 3f * oneMinusU * oneMinusU * u
        val bu2 = 3f * oneMinusU * u * u
        val bu3 = u * u * u

        val p0 = tl * bu0 + (tl + uTanTL_trailing) * bu1 + (tr + uTanTR_leading) * bu2 + tr * bu3
        val p1 = bl * bu0 + (bl + uTanBL_trailing) * bu1 + (br + uTanBR_leading) * bu2 + br * bu3

        val vTan0 = lerp(vTanTL_bottom, vTanTR_bottom, u)
        val vTan1 = lerp(vTanBL_top, vTanBR_top, u)

        val oneMinusV = 1f - v
        val bv0 = oneMinusV * oneMinusV * oneMinusV
        val bv1 = 3f * oneMinusV * oneMinusV * v
        val bv2 = 3f * oneMinusV * v * v
        val bv3 = v * v * v

        val posNormalized = p0 * bv0 + (p0 + vTan0) * bv1 + (p1 + vTan1) * bv2 + p1 * bv3
        return Offset(posNormalized.x * size.width, posNormalized.y * size.height)
    }

    private fun evaluateFallbackMesh(size: Size, verts: FloatArray, cols: IntArray) {
        val posX = rowPositionsX ?: return
        val colR = rowColorsR ?: return
        val colG = rowColorsG ?: return
        val colB = rowColorsB ?: return
        val colA = rowColorsA ?: return

        val subdivX = subdivPerPatch

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

        // Evaluate cubic patch positions and colors across grid
        for (iy in 0 until totalY) {
            val gridY = (iy.toFloat() / (totalY - 1)) * (height - 1)
            val cellY = gridY.toInt().coerceIn(0, height - 2)
            val v = (gridY - cellY).coerceIn(0f, 1f)

            for (ix in 0 until totalX) {
                val gridX = (ix.toFloat() / (totalX - 1)) * (width - 1)
                val cellX = gridX.toInt().coerceIn(0, width - 2)
                val u = (gridX - cellX).coerceIn(0f, 1f)

                val pos = evaluateCubicPatchPosition(cellX, cellY, u, v, size)
                val outIdx = iy * totalX + ix
                verts[outIdx * 2] = pos.x
                verts[outIdx * 2 + 1] = pos.y

                val r0 = max(cellY - 1, 0) * totalX + ix
                val r1 = cellY * totalX + ix
                val r2 = (cellY + 1) * totalX + ix
                val r3 = min(cellY + 2, height - 1) * totalX + ix

                val cr = MeshInterpolation.catmullRom(colR[r0], colR[r1], colR[r2], colR[r3], v)
                val cg = MeshInterpolation.catmullRom(colG[r0], colG[r1], colG[r2], colG[r3], v)
                val cb = MeshInterpolation.catmullRom(colB[r0], colB[r1], colB[r2], colB[r3], v)
                val ca = MeshInterpolation.catmullRom(colA[r0], colA[r1], colA[r2], colA[r3], v)

                val aInt = (ca.coerceIn(0f, 1f) * 255f + 0.5f).toInt()
                val rInt = (cr.coerceIn(0f, 1f) * 255f + 0.5f).toInt()
                val gInt = (cg.coerceIn(0f, 1f) * 255f + 0.5f).toInt()
                val bInt = (cb.coerceIn(0f, 1f) * 255f + 0.5f).toInt()
                cols[outIdx] = (aInt shl 24) or (rInt shl 16) or (gInt shl 8) or bInt
            }
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

    fun createShader(source: String): Shader? {
        return try {
            requireNotNull(constructor).newInstance(source) as Shader
        } catch (_: Throwable) {
            null
        }
    }

    fun updateUniforms(
        instance: Shader,
        width: Int,
        height: Int,
        resolution: FloatArray,
        points: FloatArray,
        colors: FloatArray,
        uTangents: FloatArray,
        vTangents: FloatArray,
        grainIntensity: Float,
        grainSeed: Float,
    ) {
        setFloatUniform?.invoke(instance, "uResolution", resolution)
        setIntUniform?.invoke(instance, "uWidth", width)
        setIntUniform?.invoke(instance, "uHeight", height)
        setFloatUniform?.invoke(instance, "uPackedPoints", points)
        setFloatUniform?.invoke(instance, "uColors", colors)
        setFloatUniform?.invoke(instance, "uUTangents", uTangents)
        setFloatUniform?.invoke(instance, "uVTangents", vTangents)
        setFloatUniformScalar?.invoke(instance, "uGrainIntensity", grainIntensity)
        setFloatUniformScalar?.invoke(instance, "uGrainSeed", grainSeed)
    }
}
