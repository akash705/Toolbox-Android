package com.toolbox.core.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import androidx.compose.ui.geometry.Offset

/** Turns a signature (drawn strokes, typed text, or an imported image) into a transparent bitmap. */
object SignatureRender {

    /** Rasterize freehand strokes (each a polyline of px points in a [width]x[height] pad). */
    fun strokesToBitmap(strokes: List<List<Offset>>, width: Int, height: Int, inkColor: Int = Color.BLACK): Bitmap {
        val bmp = Bitmap.createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            strokeWidth = height * 0.03f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        for (stroke in strokes) {
            if (stroke.size < 2) {
                stroke.firstOrNull()?.let { canvas.drawPoint(it.x, it.y, paint) }
                continue
            }
            val path = android.graphics.Path()
            path.moveTo(stroke[0].x, stroke[0].y)
            for (i in 1 until stroke.size) path.lineTo(stroke[i].x, stroke[i].y)
            canvas.drawPath(path, paint)
        }
        return bmp
    }

    /** Render typed text in a cursive-ish typeface to a tightly-sized transparent bitmap. */
    fun textToBitmap(text: String, inkColor: Int = Color.BLACK): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            textSize = 140f
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
        }
        val width = (paint.measureText(text).coerceAtLeast(1f) + 40f).toInt()
        val fm = paint.fontMetrics
        val height = (fm.bottom - fm.top + 40f).toInt()
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Canvas(bmp).drawText(text, 20f, -fm.top + 20f, paint)
        return bmp
    }

    /** Decode an imported signature image (kept as-is, transparency preserved). */
    fun fromUri(context: Context, uri: Uri): Bitmap? =
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it) }
        }.getOrNull()
}
