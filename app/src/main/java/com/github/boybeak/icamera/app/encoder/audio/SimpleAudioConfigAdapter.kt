package com.github.boybeak.icamera.app.encoder.audio

import android.media.CamcorderProfile
import android.media.MediaCodecInfo
import android.media.MediaRecorder
import com.github.boybeak.icamera.app.encoder.audio.AudioConfig
import com.github.boybeak.icamera.app.encoder.audio.AudioEncoder

class SimpleAudioConfigAdapter : AudioEncoder.OnConfigListener {

    companion object {
        private const val TAG = "SimpleAudioConfig"
    }

    override fun onConfig(cap: MediaCodecInfo.AudioCapabilities): AudioConfig {
        val lowProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_LOW)
        return AudioConfig(MediaRecorder.AudioSource.MIC, 44100, 1,
            cap.bitrateRange.clamp(lowProfile.audioBitRate)
        )
    }
}