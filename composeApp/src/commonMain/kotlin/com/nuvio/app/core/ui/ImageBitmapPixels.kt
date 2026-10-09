package com.nuvio.app.core.ui

import androidx.compose.ui.graphics.ImageBitmap

/** Builds an opaque [ImageBitmap] from 0xAARRGGBB pixels, row by row. */
expect fun imageBitmapFromArgb(pixels: IntArray, width: Int, height: Int): ImageBitmap
