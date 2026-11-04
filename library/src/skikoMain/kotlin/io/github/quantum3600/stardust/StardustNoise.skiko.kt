package io.github.quantum3600.stardust

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ShaderBrush
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder

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

    // Compiled once; only the uniforms change per draw
    private val effect: RuntimeEffect by lazy { RuntimeEffect.makeForShader(ShaderSource) }

    actual fun create(size: Size, intensity: Float, seed: Float): Brush? {
        if (size.width <= 0f || size.height <= 0f) return null
        val builder = RuntimeShaderBuilder(effect)
        builder.uniform("uResolution", size.width, size.height)
        builder.uniform("uIntensity", intensity)
        builder.uniform("uSeed", seed)
        return ShaderBrush(builder.makeShader())
    }
}