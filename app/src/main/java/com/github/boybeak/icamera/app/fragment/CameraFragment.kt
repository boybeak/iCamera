package com.github.boybeak.icamera.app.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.github.boybeak.cropper.draw.PreviewSurface
import com.github.boybeak.cropper.draw.SharedSurface
import com.github.boybeak.icamera.app.R
import com.github.boybeak.icamera.app.camera.CameraID
import com.github.boybeak.icamera.app.camera.SimpleCamera

class CameraFragment : Fragment() {

    private val mainPreviewSv: SurfaceView by lazy { requireView().findViewById(R.id.mainPreviewSv) }

    private val cropSharedSv: SurfaceView by lazy { requireView().findViewById(R.id.cropSharedSv) }

    private val attachToggleBtn: Button by lazy { requireView().findViewById(R.id.attachToggleBtn) }

    private val simpleCamera by lazy { SimpleCamera(requireContext()) }

    private var sharedSurface: SharedSurface? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_camera, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainPreviewSv.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                simpleCamera.open(CameraID.FACING_BACK.id, mainPreviewSv)
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                simpleCamera.close()
            }
        })
        attachToggleBtn.setOnClickListener {
            if (sharedSurface == null) {
                sharedSurface = SharedSurface(cropSharedSv.holder.surface)
                sharedSurface?.attach(simpleCamera.getPreviewingSurface())

                attachToggleBtn.text = "Attached"
            } else {
                sharedSurface?.detach()
                sharedSurface = null

                attachToggleBtn.text = "Attach"
            }
        }
    }

}