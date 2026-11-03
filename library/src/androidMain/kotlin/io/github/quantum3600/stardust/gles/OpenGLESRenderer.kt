package io.github.quantum3600.stardust.gles

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.view.Surface
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

internal class OpenGLESRenderer {
    private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
    private var eglConfig: EGLConfig? = null

    private var glProgram: Int = 0
    private var vertexShader: Int = 0
    private var fragmentShader: Int = 0
    private var vboId: Int = 0

    private var aPositionLoc: Int = -1
    private var uResolutionLoc: Int = -1
    private var uWidthLoc: Int = -1
    private var uHeightLoc: Int = -1
    private var uPackedPointsLoc: Int = -1
    private var uColorsLoc: Int = -1
    private var uUTangentsLoc: Int = -1
    private var uVTangentsLoc: Int = -1
    private var uGrainIntensityLoc: Int = -1
    private var uGrainSeedLoc: Int = -1

    private var isInitialized = false
    private val quadVertices = floatArrayOf(
        -1.0f, -1.0f,
         1.0f, -1.0f,
        -1.0f,  1.0f,
         1.0f,  1.0f
    )

    fun init(surface: Surface): Boolean {
        if (isInitialized) return true

        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (eglDisplay == EGL14.EGL_NO_DISPLAY) return false

        val version = IntArray(2)
        if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) return false

        val attribList = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, 0x0004, // EGL_OPENGL_ES2_BIT
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
            EGL14.EGL_NONE
        )

        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        if (!EGL14.eglChooseConfig(eglDisplay, attribList, 0, configs, 0, configs.size, numConfigs, 0) || numConfigs[0] == 0) {
            return false
        }
        val config = configs[0] ?: return false
        eglConfig = config

        // Try GLES 3.0 context first, then GLES 2.0 fallback
        val contextAttribsES3 = intArrayOf(
            0x3098, 3, // EGL_CONTEXT_CLIENT_VERSION = 3
            EGL14.EGL_NONE
        )
        var context = EGL14.eglCreateContext(eglDisplay, config, EGL14.EGL_NO_CONTEXT, contextAttribsES3, 0)
        if (context == EGL14.EGL_NO_CONTEXT) {
            val contextAttribsES2 = intArrayOf(
                0x3098, 2, // EGL_CONTEXT_CLIENT_VERSION = 2
                EGL14.EGL_NONE
            )
            context = EGL14.eglCreateContext(eglDisplay, config, EGL14.EGL_NO_CONTEXT, contextAttribsES2, 0)
        }
        if (context == EGL14.EGL_NO_CONTEXT) return false
        eglContext = context

        val surfaceAttribs = intArrayOf(EGL14.EGL_NONE)
        eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, config, surface, surfaceAttribs, 0)
        if (eglSurface == EGL14.EGL_NO_SURFACE) return false

        if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) return false

        if (!compileShadersAndProgram()) return false

        setupVertexBuffer()
        isInitialized = true
        return true
    }

    private fun compileShadersAndProgram(): Boolean {
        // Try ES 3.0 shaders first
        var vShader = loadShader(GLES20.GL_VERTEX_SHADER, StardustMeshGLSLShaderSource.VertexShaderES30)
        var fShader = loadShader(GLES20.GL_FRAGMENT_SHADER, StardustMeshGLSLShaderSource.FragmentShaderES30)
        var prog = 0

        if (vShader != 0 && fShader != 0) {
            prog = createProgram(vShader, fShader)
        }

        // Fallback to ES 1.0 (GLES 2.0 spec) shaders if ES 3.0 failed
        if (prog == 0) {
            if (vShader != 0) GLES20.glDeleteShader(vShader)
            if (fShader != 0) GLES20.glDeleteShader(fShader)

            vShader = loadShader(GLES20.GL_VERTEX_SHADER, StardustMeshGLSLShaderSource.VertexShaderES10)
            fShader = loadShader(GLES20.GL_FRAGMENT_SHADER, StardustMeshGLSLShaderSource.FragmentShaderES10)
            if (vShader != 0 && fShader != 0) {
                prog = createProgram(vShader, fShader)
            }
        }

        if (prog == 0) return false

        vertexShader = vShader
        fragmentShader = fShader
        glProgram = prog

        aPositionLoc = GLES20.glGetAttribLocation(glProgram, "aPosition")
        uResolutionLoc = GLES20.glGetUniformLocation(glProgram, "uResolution")
        uWidthLoc = GLES20.glGetUniformLocation(glProgram, "uWidth")
        uHeightLoc = GLES20.glGetUniformLocation(glProgram, "uHeight")
        uPackedPointsLoc = GLES20.glGetUniformLocation(glProgram, "uPackedPoints")
        uColorsLoc = GLES20.glGetUniformLocation(glProgram, "uColors")
        uUTangentsLoc = GLES20.glGetUniformLocation(glProgram, "uUTangents")
        uVTangentsLoc = GLES20.glGetUniformLocation(glProgram, "uVTangents")
        uGrainIntensityLoc = GLES20.glGetUniformLocation(glProgram, "uGrainIntensity")
        uGrainSeedLoc = GLES20.glGetUniformLocation(glProgram, "uGrainSeed")

        return true
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        if (shader == 0) return 0
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            GLES20.glDeleteShader(shader)
            return 0
        }
        return shader
    }

    private fun createProgram(vShader: Int, fShader: Int): Int {
        val program = GLES20.glCreateProgram()
        if (program == 0) return 0
        GLES20.glAttachShader(program, vShader)
        GLES20.glAttachShader(program, fShader)
        GLES20.glLinkProgram(program)
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            GLES20.glDeleteProgram(program)
            return 0
        }
        return program
    }

    private fun setupVertexBuffer() {
        val fb: FloatBuffer = ByteBuffer.allocateDirect(quadVertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        fb.put(quadVertices).position(0)

        val vbos = IntArray(1)
        GLES20.glGenBuffers(1, vbos, 0)
        vboId = vbos[0]

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId)
        GLES20.glBufferData(
            GLES20.GL_ARRAY_BUFFER,
            quadVertices.size * 4,
            fb,
            GLES20.GL_STATIC_DRAW
        )
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
    }

    fun renderFrame(
        width: Int,
        height: Int,
        meshWidth: Int,
        meshHeight: Int,
        packedPoints: FloatArray,
        colors: FloatArray,
        uTangents: FloatArray,
        vTangents: FloatArray,
        grainIntensity: Float,
        grainSeed: Float
    ): Boolean {
        if (!isInitialized || eglDisplay == EGL14.EGL_NO_DISPLAY || eglSurface == EGL14.EGL_NO_SURFACE) {
            return false
        }

        EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
        GLES20.glViewport(0, 0, width, height)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        GLES20.glUseProgram(glProgram)

        GLES20.glUniform2f(uResolutionLoc, width.toFloat(), height.toFloat())
        GLES20.glUniform1i(uWidthLoc, meshWidth)
        GLES20.glUniform1i(uHeightLoc, meshHeight)

        GLES20.glUniform4fv(uPackedPointsLoc, minOf(packedPoints.size / 4, 13), packedPoints, 0)
        GLES20.glUniform4fv(uColorsLoc, minOf(colors.size / 4, 25), colors, 0)
        GLES20.glUniform4fv(uUTangentsLoc, minOf(uTangents.size / 4, 25), uTangents, 0)
        GLES20.glUniform4fv(uVTangentsLoc, minOf(vTangents.size / 4, 25), vTangents, 0)
        GLES20.glUniform1f(uGrainIntensityLoc, grainIntensity)
        GLES20.glUniform1f(uGrainSeedLoc, grainSeed)

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId)
        GLES20.glEnableVertexAttribArray(aPositionLoc)
        GLES20.glVertexAttribPointer(aPositionLoc, 2, GLES20.GL_FLOAT, false, 8, 0)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(aPositionLoc)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)

        return EGL14.eglSwapBuffers(eglDisplay, eglSurface)
    }

    fun release() {
        if (!isInitialized) return
        if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)

            if (vboId != 0) {
                GLES20.glDeleteBuffers(1, intArrayOf(vboId), 0)
                vboId = 0
            }
            if (glProgram != 0) {
                GLES20.glDeleteProgram(glProgram)
                glProgram = 0
            }
            if (vertexShader != 0) {
                GLES20.glDeleteShader(vertexShader)
                vertexShader = 0
            }
            if (fragmentShader != 0) {
                GLES20.glDeleteShader(fragmentShader)
                fragmentShader = 0
            }

            if (eglSurface != EGL14.EGL_NO_SURFACE) {
                EGL14.eglDestroySurface(eglDisplay, eglSurface)
                eglSurface = EGL14.EGL_NO_SURFACE
            }
            if (eglContext != EGL14.EGL_NO_CONTEXT) {
                EGL14.eglDestroyContext(eglDisplay, eglContext)
                eglContext = EGL14.EGL_NO_CONTEXT
            }
            EGL14.eglTerminate(eglDisplay)
            eglDisplay = EGL14.EGL_NO_DISPLAY
        }
        isInitialized = false
    }
}
