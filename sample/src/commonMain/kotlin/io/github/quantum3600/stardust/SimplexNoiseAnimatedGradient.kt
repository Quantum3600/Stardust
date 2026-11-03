package io.github.quantum3600.stardust

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * A composable that demonstrates a fluid, continuous animation of a mesh gradient
 * using the high-level [AnimatedMeshGradient] API.
 */
@Composable
fun SimplexNoiseMeshExample(modifier: Modifier = Modifier) {
    val colors = remember {
        listOf(
            Color(0xffF36E21), Color(0xffF0A92A), Color(0xffE7D043), Color(0xffE7E95D),
            Color(0xffDFF168), Color(0xffB1E192), Color(0xff7FD3A9), Color(0xff5EC6B8),
            Color(0xff4AB8C2), Color(0xff4099B9), Color(0xff3B79A8), Color(0xff395B93),
            Color(0xff36447C), Color(0xff332E66), Color(0xff301E4E), Color(0xff2D1137)
        )
    }

    AnimatedStardustMesh(
        colors = colors,
        width = 4,
        height = 4,
        speed = 0.15f,
        wobbleAmount = 0.12f,
        dustIntensity = 0.05f,
        modifier = modifier
    )
}
