package io.github.om252345.composemeshgradient

import org.jetbrains.skia.RuntimeEffect
import kotlin.test.Test
import kotlin.test.assertNotNull

class MeshGradientShaderTest {
    @Test
    fun testShaderSourceCompiles() {
        val effect = RuntimeEffect.makeForShader(MeshGradientShaderSource)
        assertNotNull(effect)
    }
}
