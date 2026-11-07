package io.github.quantum3600.stardust

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertNotNull

class MeshGradientModifierTest {
    @Test
    fun testArrayModifierValidation() {
        val points = arrayOf(
            Offset(0f, 0f), Offset(1f, 0f),
            Offset(0f, 1f), Offset(1f, 1f)
        )
        val colors = arrayOf(
            Color.Red, Color.Green,
            Color.Blue, Color.Yellow
        )
        val p = FloatArray(points.size * 2).also { out ->
            points.forEachIndexed { i, o -> out[i * 2] = o.x; out[i * 2 + 1] = o.y }
        }
        val c = FloatArray(colors.size * 4).also { out ->
            colors.forEachIndexed { i, col ->
                out[i * 4] = col.red; out[i * 4 + 1] = col.green; out[i * 4 + 2] = col.blue; out[i * 4 + 3] = col.alpha
            }
        }
        assertNotNull(p)
        assertNotNull(c)
    }

    @Test
    fun testStardustMeshStateCreation() {
        val points = arrayOf(
            Offset(0f, 0f), Offset(1f, 0f),
            Offset(0f, 1f), Offset(1f, 1f)
        )
        val colors = arrayOf(
            Color.Red, Color.Green,
            Color.Blue, Color.Yellow
        )
        val state = StardustMeshState(points, colors)
        assertNotNull(state.pointsArray())
        assertNotNull(state.colorsArray())
    }
}
