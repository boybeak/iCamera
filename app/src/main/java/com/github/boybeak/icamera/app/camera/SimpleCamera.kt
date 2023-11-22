package com.github.boybeak.icamera.app.camera

import android.content.Context
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.view.Display
import android.view.Surface
import android.view.SurfaceView
import android.view.WindowManager
import com.github.boybeak.cropper.draw.PreviewSurface
import java.lang.IllegalStateException


class SimpleCamera(context: Context) {

    private val display: Display by lazy { (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay }

    private var camera: Camera? = null
    private var cameraId: Int = -1

    private var previewSurfaceView: SurfaceView? = null
    private var previewSurface: PreviewSurface? = null

    fun open(id: Int, surfaceView: SurfaceView) {
        if (id == cameraId) {
            return
        }
        if (camera != null) {
            close()
        }
        camera = Camera.open(id)

        val params = camera?.parameters
        params?.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO
        val previewSize = findBestSize(display, id, params!!.supportedPreviewSizes, surfaceView.width, surfaceView.height)
        params.setPreviewSize(previewSize.width, previewSize.height)
        camera?.parameters = params

        camera?.setDisplayOrientation(getCameraDisplayOrientation(display, id))

        previewSurface = PreviewSurface(surfaceView.holder.surface)

        previewSurface?.start {
            when(display.rotation) {
                Surface.ROTATION_0, Surface.ROTATION_180 -> previewSurface?.setInputSize(previewSize.height, previewSize.width)
                Surface.ROTATION_90, Surface.ROTATION_270 -> previewSurface?.setInputSize(previewSize.width, previewSize.height)
            }
            camera?.setPreviewTexture(it)
            camera?.startPreview()

        }
    }

    fun toggle() {
        when(cameraId) {
            CameraID.FACING_FRONT.id -> {
                open(CameraID.FACING_BACK.id, previewSurfaceView!!)
            }
            CameraID.FACING_BACK.id -> {
                open(CameraID.FACING_FRONT.id, previewSurfaceView!!)
            }
        }
    }

    fun close() {
        camera?.stopPreview()
        camera?.release()
        camera = null

        previewSurface?.stop()
        previewSurface = null

        previewSurfaceView = null
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