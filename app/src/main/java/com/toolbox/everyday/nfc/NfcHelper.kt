package com.toolbox.everyday.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import java.nio.charset.Charset

data class TagInfo(
    val uid: String,
    val techList: List<String>,
    val isNdef: Boolean,
    val isWritable: Boolean,
    val maxSize: Int,
    val usedSize: Int,
    val ndefRecords: List<ParsedRecord>,
    val rawHex: String,
)

sealed class ParsedRecord {
    data class TextRecord(val text: String, val language: String) : ParsedRecord()
    data class UrlRecord(val url: String) : ParsedRecord()
    data class RawRecord(val tnf: Short, val type: String, val payload: String) : ParsedRecord()
}

fun parseTag(tag: Tag): TagInfo {
    val uid = tag.id?.joinToString(":") { "%02X".format(it) } ?: "Unknown"
    val techList = tag.techList.map { it.substringAfterLast('.') }

    val ndef = Ndef.get(tag)
    val records = mutableListOf<ParsedRecord>()
    var isWritable = false
    var maxSize = 0
    var usedSize = 0
    var isNdef = false

    if (ndef != null) {
        try {
            ndef.connect()
            isNdef = true
            isWritable = ndef.isWritable
            maxSize = ndef.maxSize
            val msg = ndef.ndefMessage
            usedSize = msg?.byteArrayLength ?: 0
            msg?.records?.forEach { record ->
                records.add(parseNdefRecord(record))
            }
            ndef.close()
        } catch (_: Exception) {
            try { ndef.close() } catch (_: Exception) {}
        }
    }

    val rawHex = tag.id?.joinToString(" ") { "%02X".format(it) } ?: ""

    return TagInfo(uid, techList, isNdef, isWritable, maxSize, usedSize, records, rawHex)
}

private fun parseNdefRecord(record: NdefRecord): ParsedRecord {
    return when {
        record.tnf == NdefRecord.TNF_WELL_KNOWN && record.type.contentEquals(NdefRecord.RTD_TEXT) -> {
            val payload = record.payload
            val encoding = if ((payload[0].toInt() and 0x80) == 0) Charsets.UTF_8 else Charsets.UTF_16
            val langLength = payload[0].toInt() and 0x3F
            val language = String(payload, 1, langLength, Charsets.US_ASCII)
            val text = String(payload, 1 + langLength, payload.size - 1 - langLength, encoding)
            ParsedRecord.TextRecord(text, language)
        }
        record.tnf == NdefRecord.TNF_WELL_KNOWN && record.type.contentEquals(NdefRecord.RTD_URI) -> {
            val uri = record.toUri()?.toString() ?: "Unknown URI"
            ParsedRecord.UrlRecord(uri)
        }
        else -> {
            val type = String(record.type, Charsets.US_ASCII)
            val payload = record.payload.joinToString(" ") { "%02X".format(it) }
            ParsedRecord.RawRecord(record.tnf, type, payload)
        }
    }
}

fun writeTextToTag(tag: Tag, text: String): Result<Unit> {
    return try {
        val record = NdefRecord.createTextRecord("en", text)
        val message = NdefMessage(arrayOf(record))
        writeNdefMessage(tag, message)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

fun writeUrlToTag(tag: Tag, url: String): Result<Unit> {
    return try {
        val record = NdefRecord.createUri(url)
        val message = NdefMessage(arrayOf(record))
        writeNdefMessage(tag, message)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

private fun writeNdefMessage(tag: Tag, message: NdefMessage): Result<Unit> {
    val ndef = Ndef.get(tag)
    if (ndef != null) {
        ndef.connect()
        if (!ndef.isWritable) {
            ndef.close()
            return Result.failure(Exception("Tag is read-only"))
        }
        if (message.byteArrayLength > ndef.maxSize) {
            ndef.close()
            return Result.failure(Exception("Data too large for tag (${message.byteArrayLength} > ${ndef.maxSize} bytes)"))
        }
        ndef.writeNdefMessage(message)
        ndef.close()
        return Result.success(Unit)
    }

    val formatable = NdefFormatable.get(tag)
    if (formatable != null) {
        formatable.connect()
        formatable.format(message)
        formatable.close()
        return Result.success(Unit)
    }

    return Result.failure(Exception("Tag does not support NDEF"))
}

fun formatTag(tag: Tag): Result<Unit> {
    return try {
        val formatable = NdefFormatable.get(tag)
            ?: return Result.failure(Exception("Tag cannot be formatted to NDEF"))
        formatable.connect()
        val emptyRecord = NdefRecord(NdefRecord.TNF_EMPTY, ByteArray(0), ByteArray(0), ByteArray(0))
        formatable.format(NdefMessage(arrayOf(emptyRecord)))
        formatable.close()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

fun eraseTag(tag: Tag): Result<Unit> {
    return try {
        val ndef = Ndef.get(tag)
            ?: return Result.failure(Exception("Tag is not NDEF formatted"))
        ndef.connect()
        if (!ndef.isWritable) {
            ndef.close()
            return Result.failure(Exception("Tag is read-only"))
        }
        val emptyRecord = NdefRecord(NdefRecord.TNF_EMPTY, ByteArray(0), ByteArray(0), ByteArray(0))
        ndef.writeNdefMessage(NdefMessage(arrayOf(emptyRecord)))
        ndef.close()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
