package io.github.quantum3600.stardust

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

internal class StardustMeshRenderer(
    private val gridWidth: Int,
    private val gridHeight: Int,
    private val globalSubdivisions: Int,
) : GLSurfaceView.Renderer {

    private var helper: StardustMeshRendererHelper? = null

    private val pointCount = gridWidth * gridHeight
    private val currentPoints = FloatArray(pointCount * 2)
    private val currentColors = FloatArray(pointCount * 4)

    override fun onSurfaceCreated(unused: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        // No face culling: the shader paths don't cull either, so folded meshes look identical everywhere.
        GLES20.glDisable(GLES20.GL_CULL_FACE)

        // New GL context => all GL objects must be rebuilt.
        helper = StardustMeshRendererHelper(gridWidth, gridHeight, globalSubdivisions).also { it.initBuffers() }
    }

    override fun onSurfaceChanged(unused: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(unused: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        helper?.draw(currentPoints, currentColors)
    }

    /** Must be called on the GL thread (via queueEvent). */
    fun updatePoints(newPoints: FloatArray, newColors: FloatArray) {
        require(newPoints.size == pointCount * 2) { "points FloatArray must be ${pointCount * 2} floats" }
        require(newColors.size == pointCount * 4) { "colors FloatArray must be ${pointCount * 4} floats" }
        System.arraycopy(newPoints, 0, currentPoints, 0, newPoints.size)
        System.arraycopy(newColors, 0, currentColors, 0, newColors.size)
    }
}
