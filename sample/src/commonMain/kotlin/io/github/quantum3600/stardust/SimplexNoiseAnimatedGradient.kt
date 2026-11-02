package io.github.quantum3600.stardust

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import io.github.quantum3600.stardust.StardustMesh
import io.github.quantum3600.stardust.rememberMeshGradientState
import io.github.quantum3600.stardust.utils.SimplexNoise

/**
 * A composable that demonstrates a fluid, continuous animation of a mesh gradient
 * using Simplex Noise with zero heap allocations and synchronized VSYNC frame updates.
 */
@Composable
fun SimplexNoiseMeshExample(modifier: Modifier) {
    val width = 4
    val height = 4

    // 1. Define the colors and initial grid layout
    val colors = remember {
        listOf(
            Color(0xffF36E21), Color(0xffF0A92A), Color(0xffE7D043), Color(0xffE7E95D),
            Color(0xffDFF168), Color(0xffB1E192), Color(0xff7FD3A9), Color(0xff5EC6B8),
            Color(0xff4AB8C2), Color(0xff4099B9), Color(0xff3B79A8), Color(0xff395B93),
            Color(0xff36447C), Color(0xff332E66), Color(0xff301E4E), Color(0xff2D1137)
        )
    }

    val initialPoints = remember {
        Array(width * height) { i ->
            val col = i % width
            val row = i / width
            Offset(x = col / (width - 1f), y = row / (height - 1f))
        }
    }

    // A stable copy of the original points to base noise calculations on
    val basePointsX = remember { FloatArray(width * height) { i -> initialPoints[i].x } }
    val basePointsY = remember { FloatArray(width * height) { i -> initialPoints[i].y } }

    // 2. Create and remember the state that will drive the animation
    val meshState =
        rememberMeshGradientState(points = initialPoints, colors = colors.toTypedArray())

    // 3. Use a LaunchedEffect to run the continuous animation loop
    LaunchedEffect(Unit) {
        var time = 0f
        val currentPointsX = basePointsX.copyOf()
        val currentPointsY = basePointsY.copyOf()
        val targetPointsX = basePointsX.copyOf()
        val targetPointsY = basePointsY.copyOf()
        val framePoints = FloatArray(width * height * 2)

        var lastFrameTime = 0L

        // This loop runs synchronized to VSYNC with zero suspensions and zero allocations
        while (true) {
            withFrameNanos { frameTime ->
                if (lastFrameTime == 0L) {
                    lastFrameTime = frameTime
                }
                val deltaTime = (frameTime - lastFrameTime) / 1_000_000_000.0f
                lastFrameTime = frameTime
                time += deltaTime

                val animationSpeed = 0.15f
                for (i in 0 until (width * height)) {
                    val col = i % width
                    val row = i / width
                    val isBorder = row == 0 || row == height - 1 || col == 0 || col == width - 1

                    if (isBorder) {
                        targetPointsX[i] = basePointsX[i]
                        targetPointsY[i] = basePointsY[i]
                    } else {
                        val bx = basePointsX[i]
                        val by = basePointsY[i]
                        val noiseX = SimplexNoise.noise(bx * 1.5f, time * animationSpeed + i) * 0.12f
                        val noiseY = SimplexNoise.noise(by * 1.5f, time * animationSpeed + i + 100f) * 0.12f
                        targetPointsX[i] = bx + noiseX
                        targetPointsY[i] = by + noiseY
                    }
                }

                val factor = (4f * deltaTime).coerceIn(0f, 1f)
                for (i in 0 until (width * height)) {
                    currentPointsX[i] = currentPointsX[i] + (targetPointsX[i] - currentPointsX[i]) * factor
                    currentPointsY[i] = currentPointsY[i] + (targetPointsY[i] - currentPointsY[i]) * factor
                    framePoints[i * 2] = currentPointsX[i]
                    framePoints[i * 2 + 1] = currentPointsY[i]
                }

                meshState.snapAllPoints(framePoints)
            }
        }
    }

    // 4. Render the MeshGradient, feeding it the animated points from the state
    StardustMesh(
        modifier = modifier,
        width = width,
        height = height,
        state = meshState,
        dustIntensity = 0.05f,
    )
}
