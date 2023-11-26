package com.github.boybeak.icamera.app.encoder.video

import android.media.MediaCodec
import android.media.MediaCodecInfo.VideoCapabilities
import android.media.MediaFormat
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import com.github.boybeak.icamera.app.encoder.MediaMuxerProxy
import com.github.boybeak.icamera.app.encoder.TimeSynchronizer
import java.lang.IllegalStateException

class VideoEncoder(private val synchronizer: TimeSynchronizer) {

    companion object {
        private const val TAG = "VideoEncoder"
        private const val MIME_TYPE = "video/avc"
    }

    private var muxerProxy: MediaMuxerProxy? = null

    private var videoCodec: MediaCodec? = null

    private var encoderThread: HandlerThread? = null
    private var encoderHandler: Handler? = null

    private var callback = object : MediaCodec.Callback() {
        override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {}

        override fun onOutputBufferAvailable(
            codec: MediaCodec,
            index: Int,
            info: MediaCodec.BufferInfo
        ) {
            val timeUs = synchronizer.getTimestamp()
            info.presentationTimeUs = timeUs
//            Log.d(TAG, "onOutputBufferAvailable(${Thread.currentThread().name}) timeUs-V=$timeUs")
            muxerProxy?.writeSampleData(videoTrackId, codec.getOutputBuffer(index)!!, info)
            videoCodec?.releaseOutputBuffer(index, false)
        }

        override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
        }

        override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
            Log.d(TAG, "onOutputFormatChanged-V")
            videoTrackId = muxerProxy!!.addVideoTrack(codec.outputFormat)
            muxerProxy?.start()
        }
    }

    private var videoTrackId = -1

    fun prepare(onConfig: OnConfigListener) {
        encoderThread = HandlerThread("video-encoder")
        encoderThread?.start()
        encoderHandler = Handler(encoderThread!!.looper)
        encoderHandler?.post {
            videoCodec = MediaCodec.createEncoderByType(MIME_TYPE)
            val codecInfo = videoCodec!!.codecInfo
            val capabilities = codecInfo.getCapabilitiesForType(MIME_TYPE)
            val mediaFormat = onConfig.onConfig(capabilities.videoCapabilities).toMediaFormat(
                MIME_TYPE
            )
            videoCodec?.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        }
    }

    fun start(muxerProxy: MediaMuxerProxy, onStart: (Surface) -> Unit) {
        if (videoCodec == null) {
            throw IllegalStateException("Call prepare before start")
        }
        this.muxerProxy = muxerProxy
        encoderHandler?.post {
            videoCodec?.setCallback(callback)

            val inputSurface = videoCodec?.createInputSurface()!!

            encoderHandler?.post {
                videoCodec?.start()
            }
            onStart.invoke(inputSurface)
        }
    }

    fun stop(callback: () -> Unit) {
        encoderHandler?.post {
            Log.d(TAG, "Encoder->V stop")
            videoCodec?.signalEndOfInputStream()
            videoCodec?.stop()
            videoCodec?.release()

            videoCodec = null

            callback.invoke()
        }

        encoderHandler?.post {
            encoderThread?.quitSafely()
            encoderThread = null
            encoderHandler = null
        }

    }

    interface OnConfigListener {
        fun onConfig(capabilities: VideoCapabilities): VideoConfig
    }

}