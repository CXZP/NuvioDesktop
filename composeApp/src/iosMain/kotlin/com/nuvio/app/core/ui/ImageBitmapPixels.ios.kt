package com.nuvio.app.core.ui

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo

actual fun imageBitmapFromArgb(pixels: IntArray, width: Int, height: Int): ImageBitmap =
    Image.makeRaster(
        imageInfo = ImageInfo(width, height, ColorType.BGRA_8888, ColorAlphaType.OPAQUE),
        bytes = argbToBgraBytes(pixels),
        rowBytes = width * 4,
    ).toComposeImageBitmap()

private fun argbToBgraBytes(pixels: IntArray): ByteArray {
    val bytes = ByteArray(pixels.size * 4)
    for (i in pixels.indices) {
        val pixel = pixels[i]
        bytes[i * 4] = pixel.toByte()
        bytes[i * 4 + 1] = (pixel shr 8).toByte()
        bytes[i * 4 + 2] = (pixel shr 16).toByte()
        bytes[i * 4 + 3] = (pixel ushr 24).toByte()
    }
    return bytes
}
