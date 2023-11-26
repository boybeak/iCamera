package com.github.boybeak.icamera.app.encoder.audio

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.github.boybeak.icamera.app.encoder.MediaMuxerProxy
import com.github.boybeak.icamera.app.encoder.TimeSynchronizer
import java.lang.IllegalStateException

class AudioEncoder(private val synchronizer: TimeSynchronizer) {

    companion object {
        private const val TAG = "AudioEncoder"
        private const val MIME_TYPE = "audio/mp4a-latm"
    }

    private var audioCodec: MediaCodec? = null

    private var encoderThread: HandlerThread? = null
    private var encoderHandler: Handler? = null

    private var muxerProxy: MediaMuxerProxy? = null

    private var stopCallback: (() -> Unit)? = null

    private val callback = object : MediaCodec.Callback() {
        override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
            if (audioCapture == null) {
                codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                doRealStop()
            } else {
                val bytes = audioCapture?.read() ?: return
                val inputBuffer = codec.getInputBuffer(index)
                inputBuffer?.clear()
                inputBuffer?.put(bytes)
                val timeUs = synchronizer.getTimestamp()
//                Log.d(TAG, "onOutputBufferAvailable(${Thread.currentThread().name}) timeUs-A=$timeUs")
                codec.queueInputBuffer(index, 0, bytes.size, timeUs, 0)
            }
        }

        override fun onOutputBufferAvailable(
            codec: MediaCodec,
            index: Int,
            info: MediaCodec.BufferInfo
        ) {
            muxerProxy?.writeSampleData(audioTrackId, codec.getOutputBuffer(index)!!, info)
            audioCodec?.releaseOutputBuffer(index, false)
        }

        override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
        }

        override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
            Log.d(TAG, "onOutputFormatChanged-A")
            audioTrackId = muxerProxy!!.addAudioTrack(codec.outputFormat)
            muxerProxy?.start()
        }
    }

    private var audioTrackId = -1

    private var audioCapture: AudioCapture? = null

    fun prepare(onConfig: OnConfigListener) {
        encoderThread = HandlerThread("audio-encoder")
        encoderThread!!.start()
        encoderHandler = Handler(encoderThread!!.looper)

        encoderHandler?.post {
            audioCodec = MediaCodec.createEncoderByType(MIME_TYPE)
            val config = onConfig.onConfig(
                audioCodec!!.codecInfo.getCapabilitiesForType(MIME_TYPE).audioCapabilities
            )
            audioCapture = AudioCapture(config.audioSource, config.sampleRate)
            val mediaFormat = config.toMediaFormat(MIME_TYPE).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 8192)
            }
            audioCodec?.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        }
    }

    fun start(muxerProxy: MediaMuxerProxy) {
        if (audioCodec == null) {
            throw IllegalStateException("Call prepare before start")
        }
        this.muxerProxy = muxerProxy

        encoderHandler?.post {
            audioCodec?.setCallback(callback)
            audioCapture?.start()
            audioCodec?.start()
        }
    }

    fun stop(callback: (() -> Unit)? = null) {
        encoderHandler?.post {
            Log.d(TAG, "Encoder->A stop")
            stopCallback = callback
            audioCapture?.stop()
            audioCapture = null
        }
    }

    private fun doRealStop() {
        Log.d(TAG,  "Encoder->A doRealStop")
        audioCodec?.stop()
        audioCodec?.release()

        audioCodec = null

        stopCallback?.invoke()
        stopCallback = null

        encoderHandler?.post {
            encoderThread?.quitSafely()
            encoderThread = null
            encoderHandler = null
        }
    }

    interface OnConfigListener {
        fun onConfig(capabilities: MediaCodecInfo.AudioCapabilities): AudioConfig
    }

}