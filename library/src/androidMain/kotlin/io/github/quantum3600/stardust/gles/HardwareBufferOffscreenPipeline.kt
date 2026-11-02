package io.github.quantum3600.stardust.gles

import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.graphics.PixelFormat
import android.hardware.HardwareBuffer
import android.media.Image
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

@RequiresApi(Build.VERSION_CODES.Q)
internal class HardwareBufferOffscreenPipeline(
    private val onFrameRendered: (ImageBitmap) -> Unit
) {
    private val renderThread = HandlerThread("Stardust-GL-Thread").apply { start() }
    private val renderHandler = Handler(renderThread.looper)

    private var imageReader: ImageReader? = null
    private val renderer = OpenGLESRenderer()
    private var currentImage: Image? = null
    private var isInitialized = false

    private var currentWidth = 0
    private var currentHeight = 0

    fun prepare(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        if (currentWidth == width && currentHeight == height && isInitialized) return

        renderHandler.post {
            releaseInternal()

            currentWidth = width
            currentHeight = height

            try {
                val usage = HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or HardwareBuffer.USAGE_GPU_COLOR_OUTPUT
                val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2, usage)
                reader.setOnImageAvailableListener({ ir ->
                    val image = ir.acquireLatestImage() ?: return@setOnImageAvailableListener
                    val hb = image.hardwareBuffer
                    if (hb != null) {
                        val bitmap = Bitmap.wrapHardwareBuffer(hb, ColorSpace.get(ColorSpace.Named.SRGB))
                        if (bitmap != null) {
                            val imageBitmap = bitmap.asImageBitmap()
                            onFrameRendered(imageBitmap)
                        }
                    }
                    currentImage?.close()
                    currentImage = image
                }, renderHandler)

                imageReader = reader
                isInitialized = renderer.init(reader.surface)
            } catch (_: Throwable) {
                isInitialized = false
            }
        }
    }

    fun render(
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
    ) {
        if (!isInitialized) {
            prepare(width, height)
        }
        renderHandler.post {
            if (isInitialized) {
                renderer.renderFrame(
                    width = width,
                    height = height,
                    meshWidth = meshWidth,
                    meshHeight = meshHeight,
                    packedPoints = packedPoints,
                    colors = colors,
                    uTangents = uTangents,
                    vTangents = vTangents,
                    grainIntensity = grainIntensity,
                    grainSeed = grainSeed
                )
            }
        }
    }

    private fun releaseInternal() {
        currentImage?.close()
        currentImage = null
        imageReader?.close()
        imageReader = null
        renderer.release()
        isInitialized = false
    }

    fun release() {
        renderHandler.post {
            releaseInternal()
            renderThread.quitSafely()
        }
    }
}
