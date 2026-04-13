package com.toolbox.everyday.docscanner

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.PointF

/**
 * Apply perspective correction to a bitmap using 4 source corner points.
 * Maps the quadrilateral defined by corners to a rectangle.
 */
fun perspectiveTransform(
    source: Bitmap,
    corners: List<PointF>,
): Bitmap {
    if (corners.size != 4) return source

    // Calculate output dimensions from the corner distances
    val width = maxOf(
        distance(corners[0], corners[1]),
        distance(corners[3], corners[2]),
    ).toInt().coerceAtLeast(100)

    val height = maxOf(
        distance(corners[0], corners[3]),
        distance(corners[1], corners[2]),
    ).toInt().coerceAtLeast(100)

    val src = floatArrayOf(
        corners[0].x, corners[0].y,  // top-left
        corners[1].x, corners[1].y,  // top-right
        corners[2].x, corners[2].y,  // bottom-right
        corners[3].x, corners[3].y,  // bottom-left
    )

    val dst = floatArrayOf(
        0f, 0f,
        width.toFloat(), 0f,
        width.toFloat(), height.toFloat(),
        0f, height.toFloat(),
    )

    val matrix = Matrix()
    matrix.setPolyToPoly(src, 0, dst, 0, 4)

    return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        .let { transformed ->
            // Crop to expected dimensions
            val cropW = minOf(width, transformed.width)
            val cropH = minOf(height, transformed.height)
            if (cropW < transformed.width || cropH < transformed.height) {
                Bitmap.createBitmap(transformed, 0, 0, cropW, cropH)
            } else {
                transformed
            }
        }
}

private fun distance(a: PointF, b: PointF): Float {
    val dx = a.x - b.x
    val dy = a.y - b.y
    return kotlin.math.sqrt(dx * dx + dy * dy)
}
