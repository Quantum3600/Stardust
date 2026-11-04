package io.github.quantum3600.stardust

import org.jetbrains.skia.RuntimeEffect
import kotlin.test.Test
import kotlin.test.assertNotNull

class MeshGradientShaderTest {
    @Test
    fun testShaderSourceCompiles() {
        val effect = RuntimeEffect.makeForShader(MeshShaderSource.build(2, 2))
        assertNotNull(effect)
    }

    @Test
    fun testStardustNoiseShaderCompiles() {
        val source = """
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
    return half4(g, g, g, uIntensity);
}
"""
        val effect = RuntimeEffect.makeForShader(source)
        assertNotNull(effect)
    }
}
