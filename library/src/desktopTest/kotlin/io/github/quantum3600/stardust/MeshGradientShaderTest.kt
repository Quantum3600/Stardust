package io.github.quantum3600.stardust

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
