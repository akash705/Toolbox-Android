package com.toolbox.everyday.qrgen

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import com.toolbox.core.qr.QrEncoder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Holds all QR Generator input for every content type at once, so switching type never
 * wipes what the user typed for another type (R4). Derives an explicit preview state
 * (R3a) that the encoder can never crash on.
 */
class QrGeneratorViewModel : ViewModel() {

    /** All fields for all types held together; switching [Fields.type] preserves the rest. */
    data class Fields(
        val type: QrContentType = QrContentType.Text,
        val text: String = "",
        val url: String = "",
        val emailAddress: String = "",
        val emailSubject: String = "",
        val emailBody: String = "",
        val phone: String = "",
        val smsNumber: String = "",
        val smsMessage: String = "",
        val vcardName: String = "",
        val vcardPhone: String = "",
        val vcardEmail: String = "",
        val vcardOrg: String = "",
        val geoLat: String = "",
        val geoLng: String = "",
    )

    sealed interface Preview {
        /** No usable input yet — show a placeholder. */
        data object Placeholder : Preview
        /** Input present but failing field validation. */
        data class Invalid(val message: String) : Preview
        /** A rendered QR bitmap ready to share/save. */
        data class Valid(val bitmap: Bitmap) : Preview
        /** Content is well-formed but exceeds QR capacity. */
        data object TooLong : Preview
    }

    private val _fields = MutableStateFlow(Fields())
    val fields: StateFlow<Fields> = _fields.asStateFlow()

    private val _preview = MutableStateFlow<Preview>(Preview.Placeholder)
    val preview: StateFlow<Preview> = _preview.asStateFlow()

    fun selectType(type: QrContentType) {
        _fields.update { it.copy(type = type) }
        recompute()
    }

    fun update(transform: (Fields) -> Fields) {
        _fields.update(transform)
        recompute()
    }

    private fun recompute() {
        _preview.value = derivePreview(_fields.value)
    }

    /** Validate the current type's fields, then encode. Returns the preview state. */
    private fun derivePreview(f: Fields): Preview {
        val content: QrContent = when (f.type) {
            QrContentType.Text -> {
                if (f.text.isBlank()) return Preview.Placeholder
                QrContent.Text(f.text)
            }
            QrContentType.Url -> {
                if (f.url.isBlank()) return Preview.Placeholder
                QrContent.Url(f.url)
            }
            QrContentType.Email -> {
                if (f.emailAddress.isBlank()) return Preview.Placeholder
                if (!f.emailAddress.contains("@") || f.emailAddress.contains(" ")) {
                    return Preview.Invalid("Enter a valid email address")
                }
                QrContent.Email(f.emailAddress, f.emailSubject, f.emailBody)
            }
            QrContentType.Phone -> {
                if (f.phone.isBlank()) return Preview.Placeholder
                if (!isDiallable(f.phone)) return Preview.Invalid("Enter a valid phone number")
                QrContent.Phone(f.phone)
            }
            QrContentType.Sms -> {
                if (f.smsNumber.isBlank()) return Preview.Placeholder
                if (!isDiallable(f.smsNumber)) return Preview.Invalid("Enter a valid phone number")
                QrContent.Sms(f.smsNumber, f.smsMessage)
            }
            QrContentType.VCard -> {
                if (f.vcardName.isBlank() && f.vcardPhone.isBlank() &&
                    f.vcardEmail.isBlank() && f.vcardOrg.isBlank()
                ) {
                    return Preview.Placeholder
                }
                if (f.vcardName.isBlank()) return Preview.Invalid("A contact name is required")
                if (f.vcardEmail.isNotBlank() && !f.vcardEmail.contains("@")) {
                    return Preview.Invalid("Enter a valid email address")
                }
                QrContent.VCard(f.vcardName, f.vcardPhone, f.vcardEmail, f.vcardOrg)
            }
            QrContentType.Geo -> {
                if (f.geoLat.isBlank() && f.geoLng.isBlank()) return Preview.Placeholder
                val lat = f.geoLat.toDoubleOrNull()
                val lng = f.geoLng.toDoubleOrNull()
                if (lat == null || lat < -90.0 || lat > 90.0) {
                    return Preview.Invalid("Latitude must be between -90 and 90")
                }
                if (lng == null || lng < -180.0 || lng > 180.0) {
                    return Preview.Invalid("Longitude must be between -180 and 180")
                }
                QrContent.Geo(lat, lng)
            }
        }

        return when (val r = QrEncoder.encodeResult(content.toPayload(), sizePx = 640)) {
            QrEncoder.Result.Blank -> Preview.Placeholder
            QrEncoder.Result.TooLong -> Preview.TooLong
            is QrEncoder.Result.Ok -> Preview.Valid(r.bitmap)
        }
    }

    private fun isDiallable(s: String): Boolean {
        val trimmed = s.trim()
        return trimmed.isNotEmpty() && trimmed.all { it.isDigit() || it in "+-() " } &&
            trimmed.any { it.isDigit() }
    }
}
