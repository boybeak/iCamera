package com.github.boybeak.icamera.app.encoder

interface TimeSynchronizer {
    fun reset()
    fun getTimestamp(): Long
}