package com.github.boybeak.icamera.app.encoder

import android.media.MediaCodecInfo
import android.os.Handler
import android.os.Looper
import android.view.Surface
import com.github.boybeak.icamera.app.encoder.audio.AudioConfig
import com.github.boybeak.icamera.app.encoder.audio.AudioEncoder
import com.github.boybeak.icamera.app.encoder.video.VideoConfig
import com.github.boybeak.icamera.app.encoder.video.VideoEncoder
import java.io.File
import java.lang.IllegalStateException

/**
 *
 */
class AVEncoder {

    companion object {
        private const val TAG = "AVEncoder"
    }

    private var muxerProxy: MediaMuxerProxy? = null
    private var audioEncoder: AudioEncoder? = null
    private var videoEncoder: VideoEncoder? = null

    val isStarted: Boolean get() = muxerProxy != null

    private var eventHandler: Handler? = null

    private var synchronizer: TimeSynchronizer? = null

    fun prepare(onAudioConfig: AudioEncoder.OnConfigListener,
                onVideoConfig: VideoEncoder.OnConfigListener,
                synchronizer: TimeSynchronizer,
                onPrepared: (AVEncoder) -> Unit) {
        eventHandler = Handler(Looper.myLooper()!!)
        audioEncoder = AudioEncoder(synchronizer)
        videoEncoder = VideoEncoder(synchronizer)

        var isAudioPrepared = false
        var isVideoPrepared = false

        fun callOnPrepared() {
            if (isAudioPrepared && isVideoPrepared) {
                eventHandler?.post { onPrepared.invoke(this) }
            }
        }

        synchronizer.reset()

        audioEncoder?.prepare(object : AudioEncoder.OnConfigListener {
            override fun onConfig(capabilities: MediaCodecInfo.AudioCapabilities): AudioConfig {
                isAudioPrepared = true
                callOnPrepared()
                return onAudioConfig.onConfig(capabilities)
            }
        })
        videoEncoder?.prepare(object : VideoEncoder.OnConfigListener {
            override fun onConfig(capabilities: MediaCodecInfo.VideoCapabilities): VideoConfig {
                isVideoPrepared = true
                callOnPrepared()
                return onVideoConfig.onConfig(capabilities)
            }
        })
    }

    fun start(output: File, onStart: (Surface) -> Unit) {
        if (audioEncoder == null || videoEncoder == null) {
            throw IllegalStateException("You must call prepare before call start")
        }

        muxerProxy = MediaMuxerProxy(output)

        audioEncoder?.start(muxerProxy!!)
        videoEncoder?.start(muxerProxy!!, onStart)
    }

    fun stop() {
        var isAudioStopped = false
        var isVideoStopped = false
        fun doMuxerStop() {
            if (!isAudioStopped || !isVideoStopped) {
                return
            }
            eventHandler?.post {
                muxerProxy?.stop()
                muxerProxy?.release()

                muxerProxy = null
            }
            eventHandler = null
        }

        audioEncoder?.stop {
            isAudioStopped = true
            doMuxerStop()
        }
        audioEncoder = null

        videoEncoder?.stop {
            isVideoStopped = true
            doMuxerStop()
        }
        videoEncoder = null
    }

}