package com.github.boybeak.icamera.app.encoder

import android.media.MediaCodec.BufferInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import java.nio.ByteBuffer

class MediaMuxerProxy(output: File) {

    private val muxer: MediaMuxer = MediaMuxer(output.absolutePath,
        MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

    private var trackCount = 0
    var hasVideoTrack = false
        private set
    var hasAudioTrack = false
        private set

    var isStarted = false

    fun start() {
        if (!hasAudioTrack || !hasVideoTrack) {
            return
        }
        muxer.start()
        isStarted = true
    }

    fun addVideoTrack(format: MediaFormat): Int {
        hasVideoTrack = true
        return addTrack(format)
    }

    fun addAudioTrack(format: MediaFormat): Int {
        hasAudioTrack = true
        return addTrack(format)
    }

    private fun addTrack(format: MediaFormat): Int {
        trackCount++
        return muxer.addTrack(format)
    }

    fun writeSampleData(trackIndex: Int, buffer: ByteBuffer, bufferInfo: BufferInfo) {
        if (!isStarted) {
            return
        }
        muxer.writeSampleData(trackIndex, buffer, bufferInfo)
    }

    fun stop() {
        muxer.stop()
        isStarted = false
    }

    fun release() {
        muxer.release()
    }

}