package io.github.om252345.composemeshgradient

import android.graphics.Shader
import android.os.Build
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import java.lang.reflect.Constructor
import java.lang.reflect.Method

internal actual class MeshGradientShader actual constructor(
    private val width: Int,
    private val height: Int,
) {
    private val pointUniforms = FloatArray(maxControlPoints * 4)
    private val colorUniforms = FloatArray(maxControlPoints * 4)
    private var currentBrush: Brush? = null
    private var currentSize = Size.Unspecified
    private var dirty = true
    private var fallbackStart = Color.Transparent
    private var fallbackEnd = Color.Transparent

    actual fun update(points: Array<Offset>, colors: Array<Color>) {
        var changed = dirty
        for (i in points.indices) {
            val pointOffset = i * 4
            val colorOffset = i * 4
            val point = points[i]
            val color = colors[i]
            if (pointUniforms[pointOffset] != point.x ||
                pointUniforms[pointOffset + 1] != point.y ||
                colorUniforms[colorOffset] != color.red ||
                colorUniforms[colorOffset + 1] != color.green ||
                colorUniforms[colorOffset + 2] != color.blue ||
                colorUniforms[colorOffset + 3] != color.alpha
            ) {
                changed = true
            }
            pointUniforms[pointOffset] = point.x
            pointUniforms[pointOffset + 1] = point.y
            pointUniforms[pointOffset + 2] = 0f
            pointUniforms[pointOffset + 3] = 0f
            floatArrayOf(color.red, color.green, color.blue, color.alpha).copyInto(
                colorUniforms,
                destinationOffset = colorOffset,
            )
        }
        fallbackStart = colors.firstOrNull() ?: Color.Transparent
        fallbackEnd = colors.lastOrNull() ?: Color.Transparent
        if (changed) {
            dirty = true
            currentBrush = null
        }
    }

    actual fun brush(size: Size): Brush {
        if (!dirty && currentBrush != null && currentSize == size) return currentBrush!!
        currentSize = size
        currentBrush = createRuntimeBrush(size)
        dirty = false
        return currentBrush!!
    }

    private fun createRuntimeBrush(size: Size): Brush {
        if (Build.VERSION.SDK_INT < 33) {
            return Brush.linearGradient(listOf(fallbackStart, fallbackEnd))
        }
        val shader = RuntimeShaderReflection.create(
            source = MeshGradientShaderSource,
            width = width,
            height = height,
            resolution = floatArrayOf(size.width, size.height),
            points = pointUniforms,
            colors = colorUniforms,
        )
        return object : ShaderBrush() {
            override fun createShader(size: Size): androidx.compose.ui.graphics.Shader = shader
        }
    }

    actual companion object {
        actual val maxControlPoints: Int = 64
    }
}

private object RuntimeShaderReflection {
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

    fun create(
        source: String,
        width: Int,
        height: Int,
        resolution: FloatArray,
        points: FloatArray,
        colors: FloatArray,
    ): Shader {
        val instance = requireNotNull(constructor).newInstance(source)
        requireNotNull(setFloatUniform).invoke(instance, "uResolution", resolution)
        requireNotNull(setIntUniform).invoke(instance, "uWidth", width)
        requireNotNull(setIntUniform).invoke(instance, "uHeight", height)
        requireNotNull(setFloatUniform).invoke(instance, "uPoints", points)
        requireNotNull(setFloatUniform).invoke(instance, "uColors", colors)
        return instance as Shader
    }
}
