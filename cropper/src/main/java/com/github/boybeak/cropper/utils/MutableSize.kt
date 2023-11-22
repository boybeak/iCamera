package com.github.boybeak.cropper.utils

data class MutableSize(var width: Int, var height: Int) {
    val isEmpty: Boolean get() = width == 0 || height == 0
}
