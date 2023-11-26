package com.github.boybeak.icamera.app.encoder.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import java.lang.IllegalArgumentException

class AudioCapture(private val audioSource: Int, private val sampleRate: Int) {

    private var audioRecord: AudioRecord? = null

    val isRecording get() = audioRecord != null

    private var bufferSize: Int = -1

    @SuppressLint("MissingPermission")
    fun start() {
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        if (bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            throw IllegalArgumentException("Illegal args lead to bad bufferSize")
        }
        audioRecord = AudioRecord(audioSource, sampleRate, channelConfig, audioFormat, bufferSize)
        audioRecord?.startRecording()
    }

    fun stop() {
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null

        bufferSize = -1
    }

    fun read(): ByteArray {
        val bytes = ByteArray(bufferSize)
        audioRecord?.read(bytes, 0, bufferSize)
        return bytes
    }

}