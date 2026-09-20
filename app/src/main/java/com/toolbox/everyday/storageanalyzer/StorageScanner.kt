package com.toolbox.everyday.storageanalyzer

import java.io.File
import java.util.PriorityQueue

data class CategoryStat(val name: String, val bytes: Long, val count: Int)
data class FileEntry(val path: String, val name: String, val bytes: Long)
data class FolderEntry(val path: String, val name: String, val bytes: Long)

data class StorageReport(
    val totalBytes: Long,
    val fileCount: Int,
    val categories: List<CategoryStat>,
    val largestFiles: List<FileEntry>,
    val largestFolders: List<FolderEntry>,
)

/**
 * Walks the shared-storage tree and reports total size, a per-type breakdown, and the largest
 * files and folders. Reads only metadata (name/size) — never file contents — and deletes nothing.
 * Requires All-Files-Access (API 30+) or READ_EXTERNAL_STORAGE (API 26-29); other apps' private
 * Android/data and Android/obb directories are unreadable even so and are skipped.
 */
object StorageScanner {

    private const val TOP_LIMIT = 40

    private val CATEGORIES = linkedMapOf(
        "Images" to setOf("jpg", "jpeg", "png", "gif", "webp", "heic", "heif", "bmp"),
        "Videos" to setOf("mp4", "mkv", "3gp", "webm", "avi", "mov", "m4v"),
        "Audio" to setOf("mp3", "m4a", "aac", "wav", "ogg", "flac", "opus", "amr"),
        "Documents" to setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "epub", "csv"),
        "Archives" to setOf("zip", "rar", "7z", "tar", "gz", "xz"),
        "Apps" to setOf("apk", "apks", "xapk"),
    )

    fun scan(root: File, onProgress: (files: Int) -> Unit): StorageReport {
        var total = 0L
        var count = 0
        val catBytes = HashMap<String, Long>()
        val catCount = HashMap<String, Int>()
        val folderSizes = HashMap<String, Long>()
        // Min-heap of the largest files seen so far, bounded to TOP_LIMIT.
        val topFiles = PriorityQueue<FileEntry>(TOP_LIMIT + 1, compareBy { it.bytes })
        val rootPath = root.absolutePath

        fun category(ext: String): String {
            val lower = ext.lowercase()
            for ((name, exts) in CATEGORIES) if (lower in exts) return name
            return "Other"
        }

        fun visit(dir: File) {
            val children = dir.listFiles() ?: return
            for (child in children) {
                if (child.isDirectory) {
                    val name = child.name
                    // Skip inaccessible per-app sandboxes to avoid errors and false zeros.
                    if ((dir.name == "Android") && (name == "data" || name == "obb")) continue
                    visit(child)
                } else {
                    val len = runCatching { child.length() }.getOrDefault(0L)
                    if (len <= 0L) continue
                    total += len
                    count++
                    if (count % 400 == 0) onProgress(count)

                    val cat = category(child.extension)
                    catBytes[cat] = (catBytes[cat] ?: 0L) + len
                    catCount[cat] = (catCount[cat] ?: 0) + 1

                    topFiles.offer(FileEntry(child.absolutePath, child.name, len))
                    if (topFiles.size > TOP_LIMIT) topFiles.poll()

                    var p: File? = child.parentFile
                    while (p != null) {
                        val path = p.absolutePath
                        folderSizes[path] = (folderSizes[path] ?: 0L) + len
                        if (path == rootPath) break
                        p = p.parentFile
                    }
                }
            }
        }

        runCatching { visit(root) }
        onProgress(count)

        val categories = (CATEGORIES.keys + "Other")
            .mapNotNull { name -> catBytes[name]?.let { CategoryStat(name, it, catCount[name] ?: 0) } }
            .sortedByDescending { it.bytes }

        val largestFiles = topFiles.sortedByDescending { it.bytes }

        val largestFolders = folderSizes
            .filterKeys { it != rootPath }
            .entries
            .sortedByDescending { it.value }
            .take(TOP_LIMIT)
            .map { FolderEntry(it.key, File(it.key).name, it.value) }

        return StorageReport(total, count, categories, largestFiles, largestFolders)
    }
}
