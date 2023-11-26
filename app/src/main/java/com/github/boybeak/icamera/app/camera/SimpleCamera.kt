package com.github.boybeak.icamera.app.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.PixelFormat
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.media.ImageReader
import android.media.ImageReader.OnImageAvailableListener
import android.os.Handler
import android.util.Size
import android.view.Display
import android.view.Surface
import android.view.SurfaceView
import android.view.WindowManager
import com.github.boybeak.cropper.draw.PreviewSurface
import com.github.boybeak.cropper.draw.SharedSurface
import com.github.boybeak.icamera.app.encoder.AVEncoder
import com.github.boybeak.icamera.app.encoder.TimeSynchronizer
import com.github.boybeak.icamera.app.encoder.audio.SimpleAudioConfigAdapter
import com.github.boybeak.icamera.app.encoder.video.SimpleVideoConfigAdapter
import java.io.File
import java.lang.IllegalStateException


class SimpleCamera(context: Context) {

    private val display: Display by lazy { (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay }

    private var camera: Camera? = null
    private var cameraId: Int = -1

    private var previewSurfaceView: SurfaceView? = null
    private var previewSurface: PreviewSurface? = null

    private var previewSurfaceTexture: SurfaceTexture? = null

    fun open(id: Int, surfaceView: SurfaceView) {
        if (id == cameraId) {
            return
        }
        if (camera != null) {
            close()
        }

        previewSurface = PreviewSurface(surfaceView.holder.surface)
        previewSurfaceView = surfaceView
        previewSurface?.start { surfaceTexture ->
            previewSurfaceTexture = surfaceTexture

            val previewSize = openCameraOnly(id, surfaceTexture)
            when(display.rotation) {
                Surface.ROTATION_0, Surface.ROTATION_180 -> previewSurface?.setInputSize(previewSize.height, previewSize.width)
                Surface.ROTATION_90, Surface.ROTATION_270 -> previewSurface?.setInputSize(previewSize.width, previewSize.height)
            }
        }
    }

    private fun openCameraOnly(id: Int, surfaceTexture: SurfaceTexture): Camera.Size {
        camera = Camera.open(id)
        cameraId = id

        val params = camera?.parameters
        applyBestFocusMode(params!!)
        val previewSize = findBestSize(display, id, params.supportedPreviewSizes,
            previewSurfaceView!!.width, previewSurfaceView!!.height)
        params.setPreviewSize(previewSize.width, previewSize.height)
        camera?.parameters = params

        camera?.setDisplayOrientation(getCameraDisplayOrientation(display, id))

        camera?.setPreviewTexture(surfaceTexture)
        camera?.startPreview()

        return previewSize
    }
    private fun closeCameraOnly() {
        camera?.stopPreview()
        camera?.release()
        camera = null
    }

    fun toggle() {
        when(cameraId) {
            CameraID.FACING_FRONT.id -> {
                closeCameraOnly()
                openCameraOnly(CameraID.FACING_BACK.id, previewSurfaceTexture!!)
            }
            CameraID.FACING_BACK.id -> {
                closeCameraOnly()
                openCameraOnly(CameraID.FACING_FRONT.id, previewSurfaceTexture!!)
            }
        }
    }

    fun close() {
        closeCameraOnly()

        previewSurface?.stop()
        previewSurface = null

        previewSurfaceView = null
    }

    private val avEncoder = AVEncoder()
    private var recordSurface: SharedSurface? = null
    private val timeSynchronizer = object : TimeSynchronizer {

        private var startAt = 0L

        override fun reset() {
            startAt = System.currentTimeMillis() * 1000
        }

        override fun getTimestamp(): Long {
            return System.currentTimeMillis() * 1000 - startAt
        }
    }

    fun startRecord(output: File) {
        if (isRecording()) {
            return
        }
        avEncoder.prepare(SimpleAudioConfigAdapter(), SimpleVideoConfigAdapter(Size(previewSurfaceView!!.width, previewSurfaceView!!.height)), timeSynchronizer) {
            avEncoder.start(output) {
                recordSurface = SharedSurface(it)
                recordSurface?.attach(previewSurface!!)
            }
        }
    }

    fun stopRecord() {
        if (!isRecording()) {
            return
        }
        avEncoder.stop()
        recordSurface?.detach()
        recordSurface = null
    }

    fun isRecording(): Boolean {
        return avEncoder.isStarted
    }

    private var photoSurface: SharedSurface? = null
    fun takePhoto(callback: (Bitmap) -> Unit) {
        val imageReader = ImageReader.newInstance(previewSurfaceView!!.width, previewSurfaceView!!.height, PixelFormat.RGBA_8888, 1)
        photoSurface = SharedSurface(imageReader.surface)
        imageReader.setOnImageAvailableListener({
            val image = imageReader.acquireNextImage()
            // convert image to bitmap
        }, Handler())
        photoSurface?.attach(previewSurface!!)

    }

    fun getPreviewingSurface(): PreviewSurface {
        if (previewSurface == null) {
            throw IllegalStateException("Not previewing")
        }
        return previewSurface!!
    }

    private fun getCameraDisplayOrientation(
        display: Display, cameraId: Int
    ): Int {
        val info = CameraInfo()
        Camera.getCameraInfo(cameraId, info)
        var degrees = 0
        when (display.rotation) {
            Surface.ROTATION_0 -> degrees = 0
            Surface.ROTATION_90 -> degrees = 90
            Surface.ROTATION_180 -> degrees = 180
            Surface.ROTATION_270 -> degrees = 270
        }
        var result: Int
        return if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360
            (360 - result) % 360 // compensate the mirror
        } else {  // back-facing
            (info.orientation - degrees + 360) % 360
        }
    }

    private fun applyBestFocusMode(parameters: Camera.Parameters) {
        val preferFocusModes = arrayOf(
            Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO,
            Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE,
            Camera.Parameters.FOCUS_MODE_AUTO
        )
        for (mode in preferFocusModes) {
            if (parameters.supportedFocusModes.contains(mode)) {
                parameters.focusMode = mode
                break
            }
        }
    }

    private fun findBestSize(display: Display, cameraId: Int, sizes: List<Camera.Size>, dstWidth: Int, dstHeight: Int): Camera.Size {
        val sortedSizes = sizes.sortedBy { it.width + it.height / 10 } // 从小到大排序，以width为主键，height为次键
        val orientation = getCameraDisplayOrientation(display, cameraId)
        val guessSize = sortedSizes.firstOrNull {
            if (orientation == 90 || orientation == 270) {
                it.height >= dstWidth && it.width >= dstHeight
            } else {
                it.width >= dstWidth && it.height >= dstHeight
            }
        }
        return guessSize ?: sortedSizes.last()
    }

}