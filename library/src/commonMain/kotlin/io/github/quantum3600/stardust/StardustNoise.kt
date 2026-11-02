package io.github.quantum3600.stardust

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode

/**
 * A standalone modifier for consumers who want to dust existing Compose layouts.
 */
fun Modifier.stardust(
    intensity: Float = 0.05f,
    blendMode: BlendMode = BlendMode.Overlay
): Modifier = this
