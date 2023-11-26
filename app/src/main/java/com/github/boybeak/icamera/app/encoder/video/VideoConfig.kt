package com.github.boybeak.icamera.app.encoder.video

import android.media.MediaCodecInfo
import android.media.MediaFormat

data class VideoConfig(
    val width: Int,
    val height: Int,
    val bitRate: Int,
    val frameRate: Int,
    val iFrameInterval: Int
) {
    fun toMediaFormat(mimeType: String): MediaFormat {
        return MediaFormat.createVideoFormat(mimeType, width, height).apply {
            setString(MediaFormat.KEY_MIME, mimeType)
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_WIDTH, width)
            setInteger(MediaFormat.KEY_HEIGHT, height)
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameInterval)
        }
    }
}