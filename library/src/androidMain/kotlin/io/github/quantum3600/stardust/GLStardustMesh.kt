package io.github.quantum3600.stardust

import android.opengl.GLSurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/** OpenGL ES 3.0 fallback for Android < 33 (vertex-shader tessellated mesh). */
@Composable
internal fun GlStardustMesh(
    modifier: Modifier,
    width: Int,
    height: Int,
    subdivisions: Int,
    pointsProvider: () -> FloatArray,
    colorsProvider: () -> FloatArray,
) {
    // Renderer is bound to the GLSurfaceView at factory time, so recreate the whole view if the grid changes.
    key(width, height, subdivisions) {
        val renderer = remember { StardustMeshRenderer(width, height, subdivisions) }
        AndroidView(
            modifier = modifier,
            factory = { context ->
                GLSurfaceView(context).apply {
                    setEGLContextClientVersion(3) // shaders are "#version 300 es"
                    preserveEGLContextOnPause = true
                    setRenderer(renderer)
                    renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
                }
            },
            update = { view ->
                // State reads here are observed by AndroidView, so animations re-run this block.
                // Copy on the UI thread: the provider's arrays are reused and the GL thread reads later.
                val pts = pointsProvider().copyOf()
                val cols = colorsProvider().copyOf()
                view.queueEvent { renderer.updatePoints(pts, cols) }
                view.requestRender()
            },
        )
    }
}
