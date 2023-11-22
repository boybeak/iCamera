package com.github.boybeak.icamera.app.fragment

import android.hardware.Camera
import android.os.Bundle
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.boybeak.icamera.app.R
import com.github.boybeak.cropper.draw.PreviewSurface

class PreviewFragment : Fragment() {

    private val previewSV: SurfaceView by lazy { requireView().findViewById(R.id.previewSV) }

    private val camera: Camera by lazy { Camera.open() }
    private var previewSurface: PreviewSurface? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_preview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        previewSV.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                previewSurface = PreviewSurface(holder.surface)
                previewSurface?.start {
                    camera.setPreviewTexture(it)
                    camera.startPreview()
                }
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                camera.stopPreview()
                camera.release()
                previewSurface?.stop()
            }
        })
    }

}