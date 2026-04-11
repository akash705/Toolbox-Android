package com.toolbox.core.sharing

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generates a branded measurement card PNG and shares it via the system share sheet.
 */
object MeasurementCardSharer {

    fun share(
        context: Context,
        toolName: String,
        value: String,
        unit: String,
        label: String? = null,
        accentColorInt: Int = 0xFF1976D2.toInt(),
    ) {
        val bitmap = renderCard(toolName, value, unit, label, accentColorInt)
        val file = saveToCacheDir(context, bitmap)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share reading"))
    }

    private fun renderCard(
        toolName: String,
        value: String,
        unit: String,
        label: String?,
        accentColor: Int,
    ): Bitmap {
        val width = 800
        val height = 480
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        val bgPaint = Paint().apply { color = 0xFFF5F7FA.toInt(); isAntiAlias = true }
        canvas.drawRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), 32f, 32f, bgPaint)

        // Accent bar at top
        val accentPaint = Paint().apply { color = accentColor; isAntiAlias = true }
        canvas.drawRoundRect(RectF(0f, 0f, width.toFloat(), 8f), 32f, 32f, accentPaint)
        canvas.drawRect(RectF(0f, 4f, width.toFloat(), 8f), accentPaint)

        // Tool name
        val toolPaint = Paint().apply {
            color = 0xFF607D8B.toInt()
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(toolName.uppercase(), 48f, 64f, toolPaint)

        // Main value
        val valuePaint = Paint().apply {
            color = 0xFF212121.toInt()
            textSize = 120f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(value, 48f, 220f, valuePaint)

        // Unit
        val unitPaint = Paint().apply {
            color = accentColor
            textSize = 56f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val valueWidth = valuePaint.measureText(value)
        canvas.drawText(unit, 48f + valueWidth + 16f, 220f, unitPaint)

        // Label (if provided)
        if (label != null) {
            val labelPaint = Paint().apply {
                color = 0xFF9E9E9E.toInt()
                textSize = 36f
                typeface = Typeface.DEFAULT
                isAntiAlias = true
            }
            canvas.drawText(label, 48f, 280f, labelPaint)
        }

        // Timestamp
        val datePaint = Paint().apply {
            color = 0xFFBDBDBD.toInt()
            textSize = 28f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }
        val timestamp = SimpleDateFormat("MMM d, yyyy  h:mm a", Locale.getDefault()).format(Date())
        canvas.drawText(timestamp, 48f, height - 60f, datePaint)

        // Branding
        val brandPaint = Paint().apply {
            color = 0xFFBDBDBD.toInt()
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            isAntiAlias = true
        }
        val brandText = "Toolbox"
        val brandWidth = brandPaint.measureText(brandText)
        canvas.drawText(brandText, width - 48f - brandWidth, height - 60f, brandPaint)

        return bitmap
    }

    private fun saveToCacheDir(context: Context, bitmap: Bitmap): File {
        val dir = File(context.cacheDir, "shared_cards")
        dir.mkdirs()
        val file = File(dir, "measurement_card.png")
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file
    }
}
