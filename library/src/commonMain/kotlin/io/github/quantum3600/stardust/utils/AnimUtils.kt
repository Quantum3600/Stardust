package io.github.quantum3600.stardust.utils

import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import io.github.quantum3600.stardust.StardustMeshState
import io.github.quantum3600.stardust.rememberStardustMeshState

@Composable
fun InfiniteTransition.animateOffset(
    initialValue: Offset,
    targetValue: Offset,
    animationSpec: InfiniteRepeatableSpec<Float>,
): State<Offset> {
    val xAnim = animateFloat(initialValue.x, targetValue.x, animationSpec)
    val yAnim = animateFloat(initialValue.y, targetValue.y, animationSpec)
    return derivedStateOf { Offset(xAnim.value, yAnim.value) }
}

interface AnimatedMeshState {
    /**
     * Preferred: pass to `MeshGradient(state = animated.state, ...)`. Animation then only
     * invalidates drawing (no per-frame recomposition or array allocation).
     */
    val state: StardustMeshState

    /** Snapshot of the current points/colors. Allocates; reading it in composition recomposes every frame. */
    val meshData: MeshData
}

/**
 * Continuous simplex-noise "lava lamp" animation. [colors] must have `width * height` entries.
 */
@Composable
fun rememberSimplexAnimatedMesh(
    width: Int,
    height: Int,
    colors: List<Color>,
    animationSpeed: Float = 0.3f,
    noiseIntensity: Float = 0.2f,
): AnimatedMeshState {
    require(colors.size == width * height) { "colors must have width*height entries" }

    val initialPoints = remember(width, height) { generateMeshPoints(width, height, randomness = 0f) }
    val meshState = rememberStardustMeshState(points = initialPoints, colors = colors.toTypedArray())

    // Read live without restarting the loop when the caller tweaks them
    val speed by rememberUpdatedState(animationSpeed)
    val intensity by rememberUpdatedState(noiseIntensity)

    LaunchedEffect(meshState, width) {
        val basePoints = initialPoints.toList()
        val currentPoints = initialPoints.toMutableList()
        var time = 0f
        var lastFrameTime = 0L

        while (true) {
            val frameTime = withFrameNanos { it }
            if (lastFrameTime == 0L) lastFrameTime = frameTime
            // clamp so resuming after a pause doesn't make the mesh jump
            val dt = ((frameTime - lastFrameTime) / 1_000_000_000f).coerceAtMost(0.1f)
            lastFrameTime = frameTime
            time += dt
            val blend = (8f * dt).coerceIn(0f, 1f)
            val amp = minOf(intensity, 0.35f / (maxOf(width, height) - 1))

            for (i in basePoints.indices) {
                val col = i % width
                val row = i / width // was `i / height` (wrong for non-square grids)
                val isBorder = row == 0 || row == height - 1 || col == 0 || col == width - 1
                val bp = basePoints[i]
                val target = if (isBorder) {
                    bp
                } else {
                    Offset(
                        bp.x + SimplexNoise.noise(bp.x * 1.5f, time * speed + i) * amp,
                        bp.y + SimplexNoise.noise(bp.y * 1.5f, time * speed + i + 100f) * amp,
                    )
                }
                currentPoints[i] = lerp(currentPoints[i], target, blend)
            }

            // Suspend call made directly in this coroutine (the old code launched a new coroutine per frame)
            meshState.snapAllPoints(currentPoints)
        }
    }

    return remember(meshState, colors) {
        object : AnimatedMeshState {
            override val state = meshState
            override val meshData: MeshData
                get() = MeshData(meshState.points.toTypedArray(), colors.toTypedArray())
        }
    }
}
