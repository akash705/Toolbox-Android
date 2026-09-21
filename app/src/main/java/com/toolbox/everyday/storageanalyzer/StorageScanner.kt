package com.toolbox.everyday.storageanalyzer

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import java.util.PriorityQueue

data class CategoryStat(val name: String, val bytes: Long, val count: Int)
data class FileEntry(val uri: Uri, val name: String, val bytes: Long)
data class FolderEntry(val name: String, val path: String, val bytes: Long)

data class StorageReport(
    val totalBytes: Long,
    val fileCount: Int,
    val categories: List<CategoryStat>,
    val largestFiles: List<FileEntry>,
    val largestFolders: List<FolderEntry>,
)

/**
 * Analyzes a folder the user picked through the Storage Access Framework (ACTION_OPEN_DOCUMENT_TREE)
 * and reports total size, a per-type breakdown, and the largest files and folders inside it.
 *
 * Uses SAF/DocumentsContract only — no MANAGE_EXTERNAL_STORAGE and no runtime storage permission,
 * so it needs no Play Console declaration. Reads only document metadata (name/size/mime); deletion
 * is a separate, explicitly confirmed per-file action via DocumentsContract.deleteDocument.
 */
object StorageScanner {

    private const val TOP_LIMIT = 40

    fun scan(context: Context, treeUri: Uri, onProgress: (files: Int) -> Unit): StorageReport {
        val resolver = context.contentResolver
        val rootId = DocumentsContract.getTreeDocumentId(treeUri)

        val catBytes = HashMap<String, Long>()
        val catCount = HashMap<String, Int>()
        val folders = ArrayList<FolderEntry>()
        val topFiles = PriorityQueue<FileEntry>(TOP_LIMIT + 1, compareBy { it.bytes })
        var total = 0L
        var count = 0

        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
        )

        fun visit(docId: String, path: String, depth: Int): Long {
            var subtotal = 0L
            if (depth > 40) return 0L // guard against pathological trees
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
            runCatching {
                resolver.query(childrenUri, projection, null, null, null)?.use { c ->
                    val idCol = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameCol = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val mimeCol = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    val sizeCol = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
                    while (c.moveToNext()) {
                        val id = c.getString(idCol) ?: continue
                        val name = c.getString(nameCol) ?: id
                        val mime = c.getString(mimeCol) ?: ""
                        if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                            val childPath = if (path.isEmpty()) name else "$path/$name"
                            val sz = visit(id, childPath, depth + 1)
                            if (sz > 0) folders.add(FolderEntry(name, childPath, sz))
                            subtotal += sz
                        } else {
                            val size = if (!c.isNull(sizeCol)) c.getLong(sizeCol) else 0L
                            if (size <= 0L) continue
                            total += size
                            subtotal += size
                            count++
                            if (count % 300 == 0) onProgress(count)
                            val cat = categoryOf(mime, name)
                            catBytes[cat] = (catBytes[cat] ?: 0L) + size
                            catCount[cat] = (catCount[cat] ?: 0) + 1
                            topFiles.offer(FileEntry(DocumentsContract.buildDocumentUriUsingTree(treeUri, id), name, size))
                            if (topFiles.size > TOP_LIMIT) topFiles.poll()
                        }
                    }
                }
            }
            return subtotal
        }

        runCatching { visit(rootId, "", 0) }
        onProgress(count)

        val categories = catBytes.entries
            .map { CategoryStat(it.key, it.value, catCount[it.key] ?: 0) }
            .sortedByDescending { it.bytes }
        val largestFiles = topFiles.sortedByDescending { it.bytes }
        val largestFolders = folders.sortedByDescending { it.bytes }.take(TOP_LIMIT)

        return StorageReport(total, count, categories, largestFiles, largestFolders)
    }

    private fun categoryOf(mime: String, name: String): String {
        when {
            mime.startsWith("image/") -> return "Images"
            mime.startsWith("video/") -> return "Videos"
            mime.startsWith("audio/") -> return "Audio"
            mime == "application/vnd.android.package-archive" -> return "Apps"
            mime.startsWith("text/") ||
                mime == "application/pdf" ||
                mime.contains("msword") || mime.contains("officedocument") ||
                mime.contains("ms-excel") || mime.contains("ms-powerpoint") ||
                mime.contains("epub") -> return "Documents"
            mime.contains("zip") || mime.contains("rar") || mime.contains("7z") ||
                mime.contains("tar") || mime.contains("gzip") -> return "Archives"
        }
        // Fall back to the file extension when the provider reports a generic mime type.
        return when (name.substringAfterLast('.', "").lowercase()) {
            "jpg", "jpeg", "png", "gif", "webp", "heic", "heif", "bmp" -> "Images"
            "mp4", "mkv", "3gp", "webm", "avi", "mov", "m4v" -> "Videos"
            "mp3", "m4a", "aac", "wav", "ogg", "flac", "opus", "amr" -> "Audio"
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "epub", "csv" -> "Documents"
            "zip", "rar", "7z", "tar", "gz", "xz" -> "Archives"
            "apk", "apks", "xapk" -> "Apps"
            else -> "Other"
        }
    }
}
