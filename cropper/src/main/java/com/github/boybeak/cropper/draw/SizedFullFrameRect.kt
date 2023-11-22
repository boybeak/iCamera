package com.github.boybeak.cropper.draw

import android.graphics.Rect
import android.opengl.GLES20
import android.util.Log
import com.github.boybeak.cropper.egl.FullFrameRect
import com.github.boybeak.cropper.egl.Texture2dProgram
import com.github.boybeak.cropper.utils.MutableSize
import kotlin.math.max

class SizedFullFrameRect(program: Texture2dProgram) : FullFrameRect(program) {

    companion object {
        private const val TAG = "SizedFullFrameRect"
    }

    val inputSize = MutableSize(0, 0)

    val outputSize = MutableSize(0, 0)

    private val viewport = Rect()

    fun setInputSize(width: Int, height: Int) {
        inputSize.width = width
        inputSize.height = height

        calculateBestViewPort()
    }

    fun setOutputSize(width: Int, height: Int) {
        outputSize.width = width
        outputSize.height = height

        calculateBestViewPort()
    }

    private fun calculateBestViewPort() {
        if (inputSize.isEmpty || outputSize.isEmpty) {
            return
        }

        val scale = max(outputSize.width.toFloat() / inputSize.width, outputSize.height.toFloat() / inputSize.height)
        val srcWidth = (inputSize.width * scale).toInt()
        val srcHeight = (inputSize.height * scale).toInt()

        val left = (outputSize.width - srcWidth) / 2
        val top = (outputSize.height - srcHeight) / 2
        viewport.set(left, top, left + srcWidth, top + srcHeight).also {
            Log.d(TAG, "calculateBestViewPort viewport=$it")
        }
    }

    override fun drawFrame(textureId: Int, texMatrix: FloatArray?) {
//        Log.d(TAG, "drawFrame viewport=${viewport}")
        if (!viewport.isEmpty) {
            GLES20.glViewport(viewport.left, viewport.top, viewport.width(), viewport.height())
        }
        super.drawFrame(textureId, texMatrix)
    }
}