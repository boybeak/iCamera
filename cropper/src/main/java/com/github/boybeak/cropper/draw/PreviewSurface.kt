package com.github.boybeak.cropper.draw

import android.graphics.SurfaceTexture
import android.graphics.SurfaceTexture.OnFrameAvailableListener
import android.opengl.EGL14
import android.opengl.EGLContext
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.util.SizeF
import android.view.Surface
import com.github.boybeak.cropper.egl.EglCore
import com.github.boybeak.cropper.egl.FullFrameRect
import com.github.boybeak.cropper.egl.Texture2dProgram
import com.github.boybeak.cropper.egl.WindowSurface
import java.lang.IllegalStateException
import java.util.concurrent.CopyOnWriteArrayList

class PreviewSurface(private val surface: Surface) : OnFrameAvailableListener {

    companion object {
        private const val TAG = "PreviewSurface"
    }

    private val previewThread: HandlerThread by lazy { HandlerThread("cam-preview") }
    private val previewHandler: Handler by lazy { Handler(previewThread.looper) }

    private var eglCore: EglCore? = null

    private var windowSurface: WindowSurface? = null

    private var drawer: SizedFullFrameRect? = null

    private var textureId: Int = -1
    private var texture: SurfaceTexture? = null

    private val matrix = FloatArray(16)

    private val drawFrameCallbacks = CopyOnWriteArrayList<OnDrawFrameCallback>()

    var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
        private set

    fun setInputSize(width: Int, height: Int) {
        drawer?.setInputSize(width, height)
    }

    fun getInputSize(): Size {
        return drawer?.inputSize?.run {
            Size(width, height)
        } ?: Size(0, 0)
    }

    fun start(onStart: (SurfaceTexture) -> Unit) {
        previewThread.start()
        previewHandler.post {
            eglCore = EglCore()
            windowSurface = WindowSurface(eglCore!!, surface, false)
            windowSurface?.makeCurrent()

            drawer = SizedFullFrameRect(Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT))
            drawer?.setOutputSize(windowSurface!!.width, windowSurface!!.height)

            textureId = drawer?.createTextureObject() ?: throw IllegalStateException("Create texture error")
            texture = SurfaceTexture(textureId)

            eglContext = EGL14.eglGetCurrentContext()

            texture?.setOnFrameAvailableListener(this)

            onStart.invoke(texture!!)
        }
    }


    fun stop() {

        texture?.release()
        texture = null
        textureId = -1

        drawer?.release(true)
        drawer = null

        windowSurface?.release()
        windowSurface = null

        eglCore?.release()
        eglCore = null

        eglContext = EGL14.EGL_NO_CONTEXT

        previewThread.quit()
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        queue {
            surfaceTexture?.updateTexImage()
            surfaceTexture?.getTransformMatrix(matrix)

//            Log.d(TAG, "windowSurface.size=(${windowSurface?.width}, ${windowSurface?.height})")

            drawer?.drawFrame(textureId, matrix)

            windowSurface?.swapBuffers()

            drawFrameCallbacks.forEach {
                it.onDrawFrame(textureId, matrix, texture!!.timestamp)
            }
        }
    }

    fun queue(block: () -> Unit) {
        previewHandler.post(block)
    }

    fun addOnDrawFrameCallback(callback: OnDrawFrameCallback) {
        if (drawFrameCallbacks.contains(callback)) {
            return
        }
        drawFrameCallbacks.add(callback)
    }

    fun removeOnDrawFrameCallback(callback: OnDrawFrameCallback) {
        drawFrameCallbacks.remove(callback)
    }

    interface OnDrawFrameCallback {
        fun onDrawFrame(textureId: Int, matrix: FloatArray, timestamp: Long)
    }

}