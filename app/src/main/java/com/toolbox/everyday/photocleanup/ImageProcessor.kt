package com.toolbox.everyday.photocleanup

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import com.toolbox.core.media.MediaStoreWriter
import kotlin.math.max

/**
 * Decode + re-encode pipeline for Photo Cleanup.
 *
 * Both the Compress and Strip Metadata operations use full re-encode (R7). Re-encode discards
 * embedded EXIF, thumbnails, and maker-note blobs by definition — there is no separate "strip"
 * step needed. The Strip Metadata path additionally honors the user's keep-orientation toggle
 * by baking the orientation into the pixels before re-encoding.
 */
object ImageProcessor {

    data class ProcessResult(
        val originalBytes: Long,
        val outputBytes: Long,
        val outputPath: String,
        val outputUri: Uri?,
    )

    fun compress(
        context: Context,
        sourceUri: Uri,
        quality: Int,
        maxLongEdgePx: Int?,
    ): ProcessResult = process(
        context = context,
        sourceUri = sourceUri,
        quality = quality,
        maxLongEdgePx = maxLongEdgePx,
        applyOrientation = false,
        baseName = "compressed",
    )

    fun stripMetadata(
        context: Context,
        sourceUri: Uri,
        keepOrientation: Boolean,
        quality: Int = 95,
    ): ProcessResult = process(
        context = context,
        sourceUri = sourceUri,
        quality = quality,
        maxLongEdgePx = null,
        applyOrientation = !keepOrientation,
        baseName = "clean",
    )

    private fun process(
        context: Context,
        sourceUri: Uri,
        quality: Int,
        maxLongEdgePx: Int?,
        applyOrientation: Boolean,
        baseName: String,
    ): ProcessResult {
        val originalBytes = sizeOf(context, sourceUri)

        val bounds = readBounds(context, sourceUri)
            ?: error("Could not decode image bounds for $sourceUri")
        val sample = if (maxLongEdgePx != null) {
            calcInSampleSize(bounds.outWidth, bounds.outHeight, maxLongEdgePx)
        } else 1

        val decoded = decodeBitmap(context, sourceUri, sample)
            ?: error("Could not decode bitmap for $sourceUri")

        val rotated = if (applyOrientation) {
            applyExifOrientation(context, sourceUri, decoded).also {
                if (it !== decoded) decoded.recycle()
            }
        } else decoded

        val resized = if (maxLongEdgePx != null) {
            resizeIfLarger(rotated, maxLongEdgePx).also {
                if (it !== rotated) rotated.recycle()
            }
        } else rotated

        val saved = MediaStoreWriter.saveJpeg(
            context = context,
            bitmap = resized,
            subfolder = "Toolbox",
            baseName = baseName,
            quality = quality,
        )
        resized.recycle()

        return ProcessResult(
            originalBytes = originalBytes,
            outputBytes = saved.sizeBytes,
            outputPath = saved.displayPath,
            outputUri = saved.uri,
        )
    }

    private fun sizeOf(context: Context, uri: Uri): Long {
        return runCatching {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length }
        }.getOrNull() ?: -1L
    }

    private fun readBounds(context: Context, uri: Uri): BitmapFactory.Options? {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        return if (opts.outWidth > 0 && opts.outHeight > 0) opts else null
    }

    private fun decodeBitmap(context: Context, uri: Uri, sample: Int): Bitmap? {
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        }
    }

    private fun calcInSampleSize(width: Int, height: Int, targetLongEdge: Int): Int {
        val longEdge = max(width, height)
        if (longEdge <= targetLongEdge) return 1
        var sample = 1
        while (longEdge / (sample * 2) >= targetLongEdge) sample *= 2
        return sample
    }

    private fun resizeIfLarger(bitmap: Bitmap, targetLongEdge: Int): Bitmap {
        val longEdge = max(bitmap.width, bitmap.height)
        if (longEdge <= targetLongEdge) return bitmap
        val scale = targetLongEdge.toFloat() / longEdge
        val newW = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val newH = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }

    private fun applyExifOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation = context.contentResolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        } ?: ExifInterface.ORIENTATION_NORMAL

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f); matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f); matrix.postScale(-1f, 1f)
            }
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
