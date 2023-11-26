package com.github.boybeak.icamera.app.encoder.video

import android.media.MediaCodecInfo
import android.util.Log
import android.util.Size

class SimpleVideoConfigAdapter(private val expectVideoSize: Size, private val level: Int = LEVEL_MEDIUM) :
    VideoEncoder.OnConfigListener {

    companion object {

        private const val TAG = "SimpleVideoConfig"

        const val LEVEL_LOW = -1
        const val LEVEL_MEDIUM = 0
        const val LEVEL_HIGH = 1

    }

    override fun onConfig(cap: MediaCodecInfo.VideoCapabilities): VideoConfig {
        var width = expectVideoSize.width
        var height = expectVideoSize.height

        if (width % 2 == 1) {
            width += 1
        }
        val supportedHeight = cap.getSupportedHeightsFor(width)
        height = supportedHeight.clamp(height)

        var move = 1
        while (!cap.isSizeSupported(width, height)) {
            if (height % 2 == 1) {
                height += 1
            } else {
                if (height > supportedHeight.upper) {
                    move = -1
                    height = supportedHeight.upper
                } else if (height < supportedHeight.lower) {
                    move = 1
                    height = supportedHeight.lower
                }
                height += move * 2
            }
        }

        val bitrate = cap.bitrateRange.clamp(calculateBitRate(width, height))

        val frameRate = cap.getSupportedFrameRatesFor(width, height).clamp(
            when(level) {
                LEVEL_LOW -> 24
                LEVEL_HIGH -> 30
                else -> 24
            }.toDouble()
        ).toInt()

        return VideoConfig(
            width, height, bitrate, frameRate,
            1
        ).also {
            Log.d(TAG, "onConfig res=$it")
        }
    }

    /**
     * https://source.android.com/compatibility/android-cdd.pdf
     */
    private fun calculateBitRate(width: Int, height: Int): Int {
        return when (height) {
            240 -> 384_000
            360 -> 1_000_000
            480 -> 2_000_000
            720 -> 4_000_000
            1080 -> 10_000_000
            else -> (4.8 * width * height).toInt()
        }
    }
}