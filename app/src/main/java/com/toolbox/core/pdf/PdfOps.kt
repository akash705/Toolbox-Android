package com.toolbox.core.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileOutputStream

/**
 * PDF operations built purely on the platform's PdfRenderer (read) + PdfDocument (write).
 * Pages are rasterized (rendered to bitmaps then re-embedded), so no external dependency is
 * needed and everything runs offline. Output is image-based, not vector — acceptable for v1.
 */
object PdfOps {

    private const val RENDER_SCALE = 2f // ~144 DPI

    /** Merge several PDFs into one output file, preserving page order across documents. */
    fun merge(context: Context, uris: List<Uri>, out: File) {
        val doc = PdfDocument()
        var pageIndex = 0
        for (uri in uris) {
            forEachPage(context, uri) { bitmap, wPt, hPt ->
                writePage(doc, bitmap, wPt, hPt, pageIndex++)
            }
        }
        doc.writeTo(FileOutputStream(out))
        doc.close()
    }

    /** Rotate every page of a PDF by [degrees] (90/180/270). */
    fun rotate(context: Context, uri: Uri, degrees: Int, out: File) {
        val doc = PdfDocument()
        var pageIndex = 0
        forEachPage(context, uri) { bitmap, wPt, hPt ->
            val rotated = rotateBitmap(bitmap, degrees)
            val swap = degrees == 90 || degrees == 270
            writePage(doc, rotated, if (swap) hPt else wPt, if (swap) wPt else hPt, pageIndex++)
        }
        doc.writeTo(FileOutputStream(out))
        doc.close()
    }

    /** Extract the given 0-based page indices into a new PDF (a "split"). */
    fun extract(context: Context, uri: Uri, pageIndices: List<Int>, out: File) {
        val doc = PdfDocument()
        var outIndex = 0
        forEachPage(context, uri, filter = pageIndices.toSet()) { bitmap, wPt, hPt ->
            writePage(doc, bitmap, wPt, hPt, outIndex++)
        }
        doc.writeTo(FileOutputStream(out))
        doc.close()
    }

    /**
     * Rebuild a PDF with its pages in the given output order. [order] holds 0-based source page
     * indices in the desired sequence (a permutation; out-of-range entries are skipped).
     */
    fun reorder(context: Context, uri: Uri, order: List<Int>, out: File) {
        val doc = PdfDocument()
        openFd(context, uri)?.use { fd ->
            PdfRenderer(fd).use { renderer ->
                var outIndex = 0
                for (srcIndex in order) {
                    if (srcIndex !in 0 until renderer.pageCount) continue
                    renderer.openPage(srcIndex).use { page ->
                        val wPt = page.width
                        val hPt = page.height
                        val bmp = Bitmap.createBitmap(
                            (wPt * RENDER_SCALE).toInt().coerceAtLeast(1),
                            (hPt * RENDER_SCALE).toInt().coerceAtLeast(1),
                            Bitmap.Config.ARGB_8888,
                        )
                        Canvas(bmp).drawColor(Color.WHITE)
                        val m = Matrix().apply { setScale(RENDER_SCALE, RENDER_SCALE) }
                        page.render(bmp, null, m, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        writePage(doc, bmp, wPt, hPt, outIndex++)
                        bmp.recycle()
                    }
                }
            }
        }
        doc.writeTo(FileOutputStream(out))
        doc.close()
    }

    /**
     * Stamp [signature] onto one page at a normalized rect (0..1 in page space), rebuilding the
     * whole PDF. Other pages are copied through the same raster path unchanged.
     */
    fun signPages(
        context: Context,
        uri: Uri,
        pages: Set<Int>,
        signature: Bitmap,
        normLeft: Float,
        normTop: Float,
        normWidth: Float,
        normHeight: Float,
        out: File,
    ) {
        val doc = PdfDocument()
        var i = 0
        forEachPage(context, uri) { bitmap, wPt, hPt ->
            if (i in pages) {
                val left = normLeft * bitmap.width
                val top = normTop * bitmap.height
                val right = (normLeft + normWidth) * bitmap.width
                val bottom = (normTop + normHeight) * bitmap.height
                Canvas(bitmap).drawBitmap(signature, null, RectF(left, top, right, bottom), Paint(Paint.FILTER_BITMAP_FLAG))
            }
            writePage(doc, bitmap, wPt, hPt, i)
            i++
        }
        doc.writeTo(FileOutputStream(out))
        doc.close()
    }

    /** Build a PDF from a list of image uris, one page per image (a document scanner export). */
    fun imagesToPdf(context: Context, uris: List<Uri>, out: File) {
        val doc = PdfDocument()
        uris.forEachIndexed { index, uri ->
            val bmp = decodeSampled(context, uri, 2000) ?: return@forEachIndexed
            // Fit the page to the image aspect, capped so page dimensions stay reasonable (points).
            val maxPt = 1000f
            val s = maxPt / maxOf(bmp.width, bmp.height).toFloat()
            val wPt = (bmp.width * s).toInt().coerceAtLeast(1)
            val hPt = (bmp.height * s).toInt().coerceAtLeast(1)
            writePage(doc, bmp, wPt, hPt, index)
            bmp.recycle()
        }
        doc.writeTo(FileOutputStream(out))
        doc.close()
    }

    private fun decodeSampled(context: Context, uri: Uri, maxPx: Int): Bitmap? {
        val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxPx) sample *= 2
        val opts = android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }
        return context.contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, opts) }
    }

    /** Total page count for a PDF uri. */
    fun pageCount(context: Context, uri: Uri): Int {
        openFd(context, uri)?.use { fd ->
            PdfRenderer(fd).use { r -> return r.pageCount }
        }
        return 0
    }

    /** Render one page to a bitmap of [targetWidthPx] width (aspect preserved) for on-screen display. */
    fun renderPage(context: Context, uri: Uri, index: Int, targetWidthPx: Int): Bitmap? {
        openFd(context, uri)?.use { fd ->
            PdfRenderer(fd).use { renderer ->
                if (index !in 0 until renderer.pageCount) return null
                renderer.openPage(index).use { page ->
                    val scale = targetWidthPx.toFloat() / page.width.toFloat()
                    val w = targetWidthPx.coerceAtLeast(1)
                    val h = (page.height * scale).toInt().coerceAtLeast(1)
                    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    Canvas(bmp).drawColor(Color.WHITE)
                    val m = Matrix().apply { setScale(scale, scale) }
                    page.render(bmp, null, m, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    return bmp
                }
            }
        }
        return null
    }

    private inline fun forEachPage(
        context: Context,
        uri: Uri,
        filter: Set<Int>? = null,
        block: (bitmap: Bitmap, wPt: Int, hPt: Int) -> Unit,
    ) {
        openFd(context, uri)?.use { fd ->
            PdfRenderer(fd).use { renderer ->
                for (i in 0 until renderer.pageCount) {
                    if (filter != null && i !in filter) continue
                    renderer.openPage(i).use { page ->
                        val wPt = page.width
                        val hPt = page.height
                        val bmp = Bitmap.createBitmap(
                            (wPt * RENDER_SCALE).toInt().coerceAtLeast(1),
                            (hPt * RENDER_SCALE).toInt().coerceAtLeast(1),
                            Bitmap.Config.ARGB_8888,
                        )
                        Canvas(bmp).drawColor(Color.WHITE)
                        val m = Matrix().apply { setScale(RENDER_SCALE, RENDER_SCALE) }
                        page.render(bmp, null, m, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        block(bmp, wPt, hPt)
                        bmp.recycle()
                    }
                }
            }
        }
    }

    private fun writePage(doc: PdfDocument, bitmap: Bitmap, wPt: Int, hPt: Int, index: Int) {
        val info = PdfDocument.PageInfo.Builder(wPt.coerceAtLeast(1), hPt.coerceAtLeast(1), index).create()
        val page = doc.startPage(info)
        page.canvas.drawBitmap(bitmap, null, RectF(0f, 0f, wPt.toFloat(), hPt.toFloat()), Paint(Paint.FILTER_BITMAP_FLAG))
        doc.finishPage(page)
    }

    private fun rotateBitmap(src: Bitmap, degrees: Int): Bitmap {
        if (degrees % 360 == 0) return src
        val m = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
    }

    private fun openFd(context: Context, uri: Uri): ParcelFileDescriptor? =
        context.contentResolver.openFileDescriptor(uri, "r")

    /** Parse a page range like "1-3,5,8" (1-based) into 0-based indices within [pageCount]. */
    fun parseRange(range: String, pageCount: Int): List<Int> {
        val result = LinkedHashSet<Int>()
        for (part in range.split(",")) {
            val trimmed = part.trim()
            if (trimmed.isEmpty()) continue
            if ("-" in trimmed) {
                val (a, b) = trimmed.split("-").let { it.getOrNull(0) to it.getOrNull(1) }
                val start = a?.trim()?.toIntOrNull() ?: continue
                val end = b?.trim()?.toIntOrNull() ?: continue
                for (p in start..end) if (p in 1..pageCount) result.add(p - 1)
            } else {
                trimmed.toIntOrNull()?.let { if (it in 1..pageCount) result.add(it - 1) }
            }
        }
        return result.toList()
    }
}
