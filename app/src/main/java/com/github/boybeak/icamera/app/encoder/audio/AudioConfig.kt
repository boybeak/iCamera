package com.github.boybeak.icamera.app.encoder.audio

import android.media.MediaFormat

data class AudioConfig(
    val audioSource: Int,
    val sampleRate: Int,
    val channelCount: Int,
    val bitRate: Int
) {
    fun toMediaFormat(mimeType: String): MediaFormat {
        return MediaFormat.createAudioFormat(mimeType, sampleRate, channelCount).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
        }
    }
}