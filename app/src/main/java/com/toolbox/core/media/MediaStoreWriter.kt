package com.toolbox.core.media

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream

object MediaStoreWriter {

    data class Result(val uri: Uri?, val displayPath: String, val sizeBytes: Long)

    /**
     * Saves a bitmap as JPEG into the Pictures/<subfolder>/ public gallery location.
     * On API 29+ uses MediaStore RELATIVE_PATH + IS_PENDING. On 26-28 falls back to direct
     * file path under the legacy Pictures dir (requires WRITE_EXTERNAL_STORAGE, which the
     * manifest declares with maxSdkVersion="28").
     */
    fun saveJpeg(
        context: Context,
        bitmap: Bitmap,
        subfolder: String,
        baseName: String,
        quality: Int,
    ): Result {
        val displayName = "${baseName}_${System.currentTimeMillis()}.jpg"
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveViaMediaStore(context, bitmap, subfolder, displayName, quality)
        } else {
            saveViaLegacyPath(context, bitmap, subfolder, displayName, quality)
        }
    }

    private fun saveViaMediaStore(
        context: Context,
        bitmap: Bitmap,
        subfolder: String,
        displayName: String,
        quality: Int,
    ): Result {
        val resolver = context.contentResolver
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$subfolder")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(collection, values)
            ?: error("MediaStore insert returned null")

        var bytesWritten = 0L
        resolver.openOutputStream(uri)?.use { out ->
            val counter = CountingOutputStream(out)
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, counter)
            counter.flush()
            bytesWritten = counter.totalBytes
        } ?: error("Could not open output stream for $uri")

        values.clear()
        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
        resolver.update(uri, values, null, null)

        return Result(uri = uri, displayPath = "Pictures/$subfolder/$displayName", sizeBytes = bytesWritten)
    }

    private fun saveViaLegacyPath(
        context: Context,
        bitmap: Bitmap,
        subfolder: String,
        displayName: String,
        quality: Int,
    ): Result {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.WRITE_EXTERNAL_STORAGE,
        ) == PackageManager.PERMISSION_GRANTED
        check(granted) { "WRITE_EXTERNAL_STORAGE not granted (required on API ${Build.VERSION.SDK_INT})" }

        @Suppress("DEPRECATION")
        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val targetDir = File(picturesDir, subfolder).apply { mkdirs() }
        val file = File(targetDir, displayName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }

        @Suppress("DEPRECATION")
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DATA, file.absolutePath)
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        return Result(uri = uri, displayPath = "Pictures/$subfolder/$displayName", sizeBytes = file.length())
    }

    private class CountingOutputStream(private val delegate: java.io.OutputStream) : java.io.OutputStream() {
        var totalBytes: Long = 0
            private set

        override fun write(b: Int) {
            delegate.write(b)
            totalBytes++
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            delegate.write(b, off, len)
            totalBytes += len
        }

        override fun flush() = delegate.flush()
        override fun close() = delegate.close()
    }
}
