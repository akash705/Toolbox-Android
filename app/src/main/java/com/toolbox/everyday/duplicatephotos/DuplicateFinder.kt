package com.toolbox.everyday.duplicatephotos

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import java.security.MessageDigest

/** One image on the device. */
data class PhotoRef(
    val uri: Uri,
    val displayName: String,
    val sizeBytes: Long,
    val dateModified: Long,
)

/** A set of images with identical byte content; keep one, the rest are redundant. */
data class DuplicateGroup(val photos: List<PhotoRef>) {
    val wastedBytes: Long get() = photos.drop(1).sumOf { it.sizeBytes }
}

/**
 * Finds EXACT duplicate images (identical file bytes). We deliberately do not do perceptual /
 * near-duplicate matching: this tool deletes files, and a false "near match" could destroy a photo
 * the user wanted. Strategy: bucket by exact size (cheap), then hash only the buckets that collide.
 */
object DuplicateFinder {

    fun scan(context: Context, onProgress: (done: Int, total: Int) -> Unit): List<DuplicateGroup> {
        val all = queryImages(context)
        // Only images whose size collides with another can possibly be duplicates.
        val bySize = all.groupBy { it.sizeBytes }.filterValues { it.size > 1 }
        val candidates = bySize.values.flatten()
        val total = candidates.size
        if (total == 0) return emptyList()

        val byHash = LinkedHashMap<String, MutableList<PhotoRef>>()
        var done = 0
        for (photo in candidates) {
            val hash = hashOf(context, photo.uri)
            if (hash != null) {
                byHash.getOrPut("${photo.sizeBytes}:$hash") { mutableListOf() }.add(photo)
            }
            done++
            onProgress(done, total)
        }
        return byHash.values
            .filter { it.size > 1 }
            .map { group -> DuplicateGroup(group.sortedByDescending { it.dateModified }) }
            .sortedByDescending { it.wastedBytes }
    }

    private fun queryImages(context: Context): List<PhotoRef> {
        val out = mutableListOf<PhotoRef>()
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_MODIFIED,
        )
        context.contentResolver.query(collection, projection, null, null, null)?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val sizeCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val dateCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            while (c.moveToNext()) {
                val size = c.getLong(sizeCol)
                if (size <= 0) continue
                val id = c.getLong(idCol)
                out.add(
                    PhotoRef(
                        uri = ContentUris.withAppendedId(collection, id),
                        displayName = c.getString(nameCol) ?: "image",
                        sizeBytes = size,
                        dateModified = c.getLong(dateCol),
                    ),
                )
            }
        }
        return out
    }

    private fun hashOf(context: Context, uri: Uri): String? = runCatching {
        val digest = MessageDigest.getInstance("SHA-256")
        context.contentResolver.openInputStream(uri)?.use { input ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buf)
                if (read < 0) break
                digest.update(buf, 0, read)
            }
        } ?: return null
        digest.digest().joinToString("") { "%02x".format(it) }
    }.getOrNull()
}
