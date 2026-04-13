package com.toolbox.everyday.docscanner

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint

enum class ImageFilter(val label: String) {
    Original("Original"),
    Grayscale("Grayscale"),
    HighContrast("High Contrast"),
    BlackWhite("B&W"),
}

fun applyFilter(source: Bitmap, filter: ImageFilter): Bitmap {
    if (filter == ImageFilter.Original) return source

    val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    val paint = Paint()

    when (filter) {
        ImageFilter.Grayscale -> {
            val cm = ColorMatrix()
            cm.setSaturation(0f)
            paint.colorFilter = ColorMatrixColorFilter(cm)
        }
        ImageFilter.HighContrast -> {
            val contrast = 1.5f
            val translate = (-0.5f * contrast + 0.5f) * 255f
            val cm = ColorMatrix(floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f,
            ))
            // Also desaturate slightly
            val grayCm = ColorMatrix()
            grayCm.setSaturation(0.2f)
            cm.postConcat(grayCm)
            paint.colorFilter = ColorMatrixColorFilter(cm)
        }
        ImageFilter.BlackWhite -> {
            // Grayscale with high contrast threshold effect
            val threshold = 2.0f
            val translate = (-0.5f * threshold + 0.5f) * 255f
            val cm = ColorMatrix()
            cm.setSaturation(0f)
            val contrastCm = ColorMatrix(floatArrayOf(
                threshold, 0f, 0f, 0f, translate,
                0f, threshold, 0f, 0f, translate,
                0f, 0f, threshold, 0f, translate,
                0f, 0f, 0f, 1f, 0f,
            ))
            cm.postConcat(contrastCm)
            paint.colorFilter = ColorMatrixColorFilter(cm)
        }
        ImageFilter.Original -> { /* no-op */ }
    }

    canvas.drawBitmap(source, 0f, 0f, paint)
    return result
}
