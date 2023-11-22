package com.github.boybeak.icamera.app.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.github.boybeak.icamera.app.R

class IndexFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_index, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<Button>(R.id.previewBtn).setOnClickListener {
            findNavController().navigate(R.id.actionToPreview)
        }
        view.findViewById<Button>(R.id.sharedBtn).setOnClickListener {
            findNavController().navigate(R.id.actionToShared)
        }
        view.findViewById<Button>(R.id.cameraBtn).setOnClickListener {
            findNavController().navigate(R.id.actionToCamera)
        }
    }

}