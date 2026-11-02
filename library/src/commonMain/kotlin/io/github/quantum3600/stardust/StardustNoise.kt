package io.github.quantum3600.stardust

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush

/**
 * A standalone modifier that applies procedural stardust/film grain noise over any Compose component or layout.
 *
 * @param intensity Noise grain intensity (0.0f = disabled, 0.05f = subtle grain, 0.2f = heavy noise).
 * @param seed Seed value or frame animation phase for noise generation.
 * @param blendMode Blend mode used to layer noise over the composable content. Defaults to [BlendMode.Overlay].
 */
fun Modifier.stardust(
    intensity: Float = 0.05f,
    seed: Float = 0f,
    blendMode: BlendMode = BlendMode.Overlay
): Modifier = this.drawWithCache {
    if (intensity <= 0f) {
        onDrawWithContent {
            drawContent()
        }
    } else {
        val brush = StardustNoiseShader.create(size, intensity, seed)
        onDrawWithContent {
            drawContent()
            if (brush != null) {
                drawRect(
                    brush = brush,
                    blendMode = blendMode
                )
            }
        }
    }
}

internal expect object StardustNoiseShader {
    fun create(size: Size, intensity: Float, seed: Float): Brush?
}
