package com.toolbox.core.qr

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

object QrEncoder {

    /**
     * Guarded encode used by the QR Generator's live preview. Never throws to the UI:
     * blank content and over-capacity content are returned as typed states so the preview
     * can render placeholder / "too long" instead of crashing on every keystroke.
     * The plain [encode] below is left unchanged for existing callers (e.g. WiFi QR Share).
     */
    sealed interface Result {
        /** No content to encode yet. */
        data object Blank : Result
        /** A successfully encoded QR bitmap. */
        data class Ok(val bitmap: Bitmap) : Result
        /** Content exceeds QR capacity for the current settings. */
        data object TooLong : Result
    }

    fun encodeResult(
        content: String,
        sizePx: Int = 512,
        darkColor: Int = Color.BLACK,
        lightColor: Int = Color.WHITE,
    ): Result {
        if (content.isBlank()) return Result.Blank
        return try {
            Result.Ok(encode(content, sizePx, darkColor, lightColor))
        } catch (_: WriterException) {
            // ZXing throws WriterException when content overflows the symbol's capacity.
            Result.TooLong
        } catch (_: IllegalArgumentException) {
            // Defensive: some ZXing paths signal capacity/size issues as IllegalArgumentException.
            Result.TooLong
        }
    }

    fun encode(
        content: String,
        sizePx: Int = 512,
        darkColor: Int = Color.BLACK,
        lightColor: Int = Color.WHITE,
    ): Bitmap {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 1,
            EncodeHintType.CHARACTER_SET to "UTF-8",
        )
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val width = matrix.width
        val height = matrix.height
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (matrix.get(x, y)) darkColor else lightColor
            }
        }
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, width, 0, 0, width, height)
        }
    }
}
