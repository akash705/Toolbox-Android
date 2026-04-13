package com.toolbox.everyday.docscanner

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.io.FileOutputStream

data class ScannedPage(
    val original: Bitmap,
    val processed: Bitmap,
    val filter: ImageFilter = ImageFilter.Original,
)

class DocScannerViewModel : ViewModel() {
    private val _pages = MutableStateFlow<List<ScannedPage>>(emptyList())
    val pages: StateFlow<List<ScannedPage>> = _pages

    private val _currentCapture = MutableStateFlow<Bitmap?>(null)
    val currentCapture: StateFlow<Bitmap?> = _currentCapture

    private val _corners = MutableStateFlow<List<PointF>>(emptyList())
    val corners: StateFlow<List<PointF>> = _corners

    private val _selectedFilter = MutableStateFlow(ImageFilter.Original)
    val selectedFilter: StateFlow<ImageFilter> = _selectedFilter

    fun setCapturedImage(bitmap: Bitmap) {
        _currentCapture.value = bitmap
        // Default corners to image edges
        _corners.value = listOf(
            PointF(0f, 0f),
            PointF(bitmap.width.toFloat(), 0f),
            PointF(bitmap.width.toFloat(), bitmap.height.toFloat()),
            PointF(0f, bitmap.height.toFloat()),
        )
    }

    fun updateCorner(index: Int, point: PointF) {
        val current = _corners.value.toMutableList()
        if (index in current.indices) {
            current[index] = point
            _corners.value = current
        }
    }

    fun setFilter(filter: ImageFilter) {
        _selectedFilter.value = filter
    }

    fun confirmPage() {
        val capture = _currentCapture.value ?: return
        val corners = _corners.value
        val cropped = if (corners.size == 4) perspectiveTransform(capture, corners) else capture
        val filtered = applyFilter(cropped, _selectedFilter.value)
        _pages.value = _pages.value + ScannedPage(capture, filtered, _selectedFilter.value)
        _currentCapture.value = null
        _corners.value = emptyList()
        _selectedFilter.value = ImageFilter.Original
    }

    fun removePage(index: Int) {
        _pages.value = _pages.value.toMutableList().apply {
            if (index in indices) removeAt(index)
        }
    }

    fun clearCapture() {
        _currentCapture.value = null
        _corners.value = emptyList()
    }

    fun saveAsImage(context: Context, pageIndex: Int): Result<Uri?> {
        return try {
            val page = _pages.value.getOrNull(pageIndex) ?: return Result.failure(Exception("No page"))
            val bitmap = page.processed
            val filename = "DocScan_${System.currentTimeMillis()}.jpg"

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= 29) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/DocScanner")
                }
            }

            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues,
            )
            uri?.let { u ->
                context.contentResolver.openOutputStream(u)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
            }
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun saveAsPdf(context: Context): Result<Uri?> {
        return try {
            val pageList = _pages.value
            if (pageList.isEmpty()) return Result.failure(Exception("No pages to save"))

            val pdfDocument = PdfDocument()
            // A4 at 72 dpi: 595 x 842 points
            val pageWidth = 595
            val pageHeight = 842

            pageList.forEachIndexed { index, page ->
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                val pdfPage = pdfDocument.startPage(pageInfo)
                val canvas = pdfPage.canvas

                val bitmap = page.processed
                // Scale bitmap to fit page while maintaining aspect ratio
                val scaleX = pageWidth.toFloat() / bitmap.width
                val scaleY = pageHeight.toFloat() / bitmap.height
                val scale = minOf(scaleX, scaleY)
                val dx = (pageWidth - bitmap.width * scale) / 2
                val dy = (pageHeight - bitmap.height * scale) / 2

                canvas.translate(dx, dy)
                canvas.scale(scale, scale)
                canvas.drawBitmap(bitmap, 0f, 0f, null)

                pdfDocument.finishPage(pdfPage)
            }

            val filename = "DocScan_${System.currentTimeMillis()}.pdf"
            val uri: Uri?

            if (Build.VERSION.SDK_INT >= 29) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, filename)
                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(MediaStore.Downloads.RELATIVE_PATH, "Download/Toolbox")
                }
                uri = context.contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    contentValues,
                )
                uri?.let { u ->
                    context.contentResolver.openOutputStream(u)?.use { out ->
                        pdfDocument.writeTo(out)
                    }
                }
            } else {
                val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                val file = File(dir, filename)
                FileOutputStream(file).use { out ->
                    pdfDocument.writeTo(out)
                }
                uri = Uri.fromFile(file)
            }

            pdfDocument.close()
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
