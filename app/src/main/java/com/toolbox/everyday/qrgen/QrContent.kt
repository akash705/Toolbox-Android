package com.toolbox.everyday.qrgen

import java.net.URLEncoder

/**
 * The QR content types the generator supports. Each variant renders its de-facto-standard
 * payload string via [toPayload]. Field-level validation lives in the ViewModel (see
 * [QrGeneratorViewModel]); this type only formats and escapes.
 */
enum class QrContentType(val label: String) {
    Text("Text"),
    Url("URL"),
    Email("Email"),
    Phone("Phone"),
    Sms("SMS"),
    VCard("Contact"),
    Geo("Location"),
}

sealed interface QrContent {
    /** The encoded payload string, formatted per the type's de-facto standard. */
    fun toPayload(): String

    data class Text(val text: String) : QrContent {
        override fun toPayload(): String = text
    }

    data class Url(val url: String) : QrContent {
        override fun toPayload(): String = url.trim()
    }

    data class Email(val address: String, val subject: String, val body: String) : QrContent {
        override fun toPayload(): String {
            val params = buildList {
                if (subject.isNotBlank()) add("subject=" + urlEncode(subject))
                if (body.isNotBlank()) add("body=" + urlEncode(body))
            }
            val query = if (params.isEmpty()) "" else "?" + params.joinToString("&")
            return "mailto:" + address.trim() + query
        }
    }

    data class Phone(val number: String) : QrContent {
        override fun toPayload(): String = "tel:" + number.trim()
    }

    data class Sms(val number: String, val message: String) : QrContent {
        // SMSTO:<number>:<message> is the widely-scanned form.
        override fun toPayload(): String = "SMSTO:" + number.trim() + ":" + message
    }

    data class VCard(
        val name: String,
        val phone: String,
        val email: String,
        val org: String,
    ) : QrContent {
        override fun toPayload(): String = buildString {
            append("BEGIN:VCARD\n")
            append("VERSION:3.0\n")
            append("FN:").append(escapeVCard(name)).append('\n')
            if (phone.isNotBlank()) append("TEL:").append(escapeVCard(phone)).append('\n')
            if (email.isNotBlank()) append("EMAIL:").append(escapeVCard(email)).append('\n')
            if (org.isNotBlank()) append("ORG:").append(escapeVCard(org)).append('\n')
            append("END:VCARD")
        }
    }

    data class Geo(val lat: Double, val lng: Double) : QrContent {
        override fun toPayload(): String = "geo:$lat,$lng"
    }
}

private fun urlEncode(value: String): String =
    URLEncoder.encode(value, "UTF-8").replace("+", "%20")

// vCard 3.0 escapes backslash, comma, semicolon and newline within text values.
private fun escapeVCard(value: String): String = buildString {
    for (ch in value) {
        when (ch) {
            '\\' -> append("\\\\")
            ',' -> append("\\,")
            ';' -> append("\\;")
            '\n' -> append("\\n")
            else -> append(ch)
        }
    }
}
