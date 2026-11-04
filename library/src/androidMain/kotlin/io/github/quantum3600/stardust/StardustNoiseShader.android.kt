package io.github.quantum3600.stardust

import android.graphics.Shader
import android.os.Build
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ShaderBrush
import java.lang.reflect.Constructor
import java.lang.reflect.Method

internal actual object StardustNoiseShader {
    private const val ShaderSource = """
uniform float2 uResolution;
uniform float uIntensity;
uniform float uSeed;

float hash(float2 p, float seed) {
    float3 p3 = fract(p.xyx * 0.1031 + seed);
    p3 += float3(dot(p3, p3.yzx + 33.33));
    return fract((p3.x + p3.y) * p3.z);
}

half4 main(float2 coord) {
    float n = hash(coord, uSeed);
    float g = clamp(0.5 + (n - 0.5) * uIntensity, 0.0, 1.0);
    return half4(half3(g) * uIntensity, uIntensity);
}
"""

    actual fun create(size: Size, intensity: Float, seed: Float): Brush? {
        if (size.width <= 0f || size.height <= 0f) return null
        if (Build.VERSION.SDK_INT >= 33 && StardustNoiseReflection.isAvailable) {
            val shader = StardustNoiseReflection.create(ShaderSource, size.width, size.height, intensity, seed)
            return object : ShaderBrush() {
                override fun createShader(size: Size): androidx.compose.ui.graphics.Shader = shader
            }
        }
        return null
    }
}

private object StardustNoiseReflection {
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
    private val setFloatUniformScalar: Method? = runtimeShaderClass?.getMethod(
        "setFloatUniform",
        String::class.java,
        Float::class.javaPrimitiveType,
    )

    fun create(
        source: String,
        width: Float,
        height: Float,
        intensity: Float,
        seed: Float
    ): Shader {
        val instance = requireNotNull(constructor).newInstance(source)
        requireNotNull(setFloatUniform).invoke(instance, "uResolution", floatArrayOf(width, height))
        requireNotNull(setFloatUniformScalar).invoke(instance, "uIntensity", intensity)
        requireNotNull(setFloatUniformScalar).invoke(instance, "uSeed", seed)
        return instance as Shader
    }
}
