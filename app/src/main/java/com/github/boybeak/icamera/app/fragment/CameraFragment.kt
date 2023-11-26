package com.github.boybeak.icamera.app.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.github.boybeak.cropper.draw.PreviewSurface
import com.github.boybeak.cropper.draw.SharedSurface
import com.github.boybeak.icamera.app.R
import com.github.boybeak.icamera.app.camera.CameraID
import com.github.boybeak.icamera.app.camera.SimpleCamera
import java.io.File

class CameraFragment : Fragment() {

    private val mainPreviewSv: SurfaceView by lazy { requireView().findViewById(R.id.mainPreviewSv) }
    private val cropSharedSv: SurfaceView by lazy { requireView().findViewById(R.id.cropSharedSv) }
    private val attachToggleBtn: Button by lazy { requireView().findViewById(R.id.attachToggleBtn) }
    private val toggleBtn: Button by lazy { requireView().findViewById(R.id.toggleBtn) }
    private val recordBtn: Button by lazy { requireView().findViewById(R.id.recordBtn) }

    private val simpleCamera by lazy { SimpleCamera(requireContext()) }

    private var sharedSurface: SharedSurface? = null

    private var recordingFile: File? = null

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
        toggleBtn.setOnClickListener {
            simpleCamera.toggle()
        }
        recordBtn.setOnClickListener {
            if (simpleCamera.isRecording()) {
                simpleCamera.stopRecord()
                Toast.makeText(
                    requireContext(),
                    "saved: ${recordingFile!!.absolutePath}",
                    Toast.LENGTH_SHORT
                ).show()
                recordingFile = null
                recordBtn.text = "Start"
            } else {
                recordingFile = File(view.context.externalCacheDir, "${System.currentTimeMillis()}.mp4")
                simpleCamera.startRecord(recordingFile!!)
                recordBtn.text = "Recording"
            }
        }
    }

}