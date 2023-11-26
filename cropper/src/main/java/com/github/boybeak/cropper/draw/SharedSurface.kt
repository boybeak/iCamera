package com.github.boybeak.cropper.draw

import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import com.github.boybeak.cropper.egl.EglCore
import com.github.boybeak.cropper.egl.FullFrameRect
import com.github.boybeak.cropper.egl.Texture2dProgram
import com.github.boybeak.cropper.egl.WindowSurface

class SharedSurface(private val surface: Surface) : PreviewSurface.OnDrawFrameCallback {

    companion object {
        private const val TAG = "SharedSurface"
    }

    private var eglCore: EglCore? = null

    private var sharedSurface: WindowSurface? = null

    private var drawer: SizedFullFrameRect? = null

    private var sharedThread = HandlerThread("shared-${hashCode()}")
    private val sharedHandler by lazy { Handler() }

    private var attachedSurface: PreviewSurface? = null

    val isAttached: Boolean get() = attachedSurface != null

    fun attach(previewSurface: PreviewSurface) {
        sharedThread.start()
        sharedHandler.post {
            eglCore = EglCore(previewSurface.eglContext, EglCore.FLAG_RECORDABLE)
            sharedSurface = WindowSurface(eglCore!!, surface, false)
            sharedSurface?.makeCurrent()

            drawer = SizedFullFrameRect(Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT))
            val inputSize = previewSurface.getInputSize()
            drawer?.setInputSize(inputSize.width, inputSize.height)

            previewSurface.addOnDrawFrameCallback(this)
            attachedSurface = previewSurface
        }

    }

    fun detach() {
        attachedSurface?.removeOnDrawFrameCallback(this)
        attachedSurface = null

        drawer?.release(true)
        drawer = null

        sharedSurface?.release()
        sharedSurface = null

        eglCore?.release()
        eglCore = null

        sharedThread.quit()
    }

    override fun onDrawFrame(textureId: Int, matrix: FloatArray, timestamp: Long) {
        sharedHandler.post {
            sharedSurface?.setPresentationTime(timestamp)
            drawer?.drawFrame(textureId, matrix)
            sharedSurface?.swapBuffers()
        }
    }

}