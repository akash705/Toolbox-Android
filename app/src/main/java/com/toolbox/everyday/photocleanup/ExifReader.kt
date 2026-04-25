package com.toolbox.everyday.photocleanup

import android.content.Context
import android.media.ExifInterface
import android.net.Uri

/**
 * Reads EXIF metadata for the Strip Metadata preview.
 *
 * Per requirements R4 + R4a: surface only field-presence (group categories), never raw values.
 * GPS coordinates, device serials, owner names, and raw timestamps must not appear in the UI,
 * logs, or error messages.
 */
object ExifReader {

    data class FieldPresence(
        val gps: Boolean,
        val device: Boolean,
        val timestamps: Boolean,
        val thumbnail: Boolean,
        val orientation: Boolean,
        val totalTagCount: Int,
    )

    private val GPS_TAGS = arrayOf(
        ExifInterface.TAG_GPS_LATITUDE,
        ExifInterface.TAG_GPS_LONGITUDE,
        ExifInterface.TAG_GPS_ALTITUDE,
        ExifInterface.TAG_GPS_LATITUDE_REF,
        ExifInterface.TAG_GPS_LONGITUDE_REF,
        ExifInterface.TAG_GPS_TIMESTAMP,
        ExifInterface.TAG_GPS_DATESTAMP,
    )

    private val DEVICE_TAGS = arrayOf(
        ExifInterface.TAG_MAKE,
        ExifInterface.TAG_MODEL,
        ExifInterface.TAG_SOFTWARE,
        ExifInterface.TAG_ARTIST,
    )

    private val TIMESTAMP_TAGS = arrayOf(
        ExifInterface.TAG_DATETIME,
        ExifInterface.TAG_DATETIME_ORIGINAL,
        ExifInterface.TAG_DATETIME_DIGITIZED,
    )

    private val ALL_KNOWN_TAGS = GPS_TAGS + DEVICE_TAGS + TIMESTAMP_TAGS + arrayOf(
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.TAG_IMAGE_WIDTH,
        ExifInterface.TAG_IMAGE_LENGTH,
        ExifInterface.TAG_F_NUMBER,
        ExifInterface.TAG_EXPOSURE_TIME,
        ExifInterface.TAG_FOCAL_LENGTH,
        ExifInterface.TAG_ISO_SPEED_RATINGS,
        ExifInterface.TAG_FLASH,
        ExifInterface.TAG_WHITE_BALANCE,
    )

    fun read(context: Context, uri: Uri): FieldPresence? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                FieldPresence(
                    gps = anyPresent(exif, GPS_TAGS),
                    device = anyPresent(exif, DEVICE_TAGS),
                    timestamps = anyPresent(exif, TIMESTAMP_TAGS),
                    thumbnail = exif.hasThumbnail(),
                    orientation = exif.getAttribute(ExifInterface.TAG_ORIENTATION) != null,
                    totalTagCount = ALL_KNOWN_TAGS.count { exif.getAttribute(it) != null },
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun anyPresent(exif: ExifInterface, tags: Array<String>): Boolean =
        tags.any { exif.getAttribute(it) != null }
}
