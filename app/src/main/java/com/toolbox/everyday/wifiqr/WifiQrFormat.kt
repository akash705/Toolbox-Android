package com.toolbox.everyday.wifiqr

enum class WifiSecurity(val label: String, val wifiUriToken: String) {
    WPA2("WPA / WPA2", "WPA"),
    WPA3("WPA3", "WPA"),
    WEP("WEP", "WEP"),
    NONE("None", "nopass"),
}

object WifiQrFormat {

    fun build(
        ssid: String,
        password: String,
        security: WifiSecurity,
        hidden: Boolean,
    ): String {
        val sb = StringBuilder("WIFI:")
        sb.append("T:").append(security.wifiUriToken).append(';')
        sb.append("S:").append(encodeField(ssid)).append(';')
        if (security != WifiSecurity.NONE) {
            sb.append("P:").append(encodeField(password)).append(';')
        }
        if (hidden) {
            sb.append("H:true").append(';')
        }
        sb.append(';')
        return sb.toString()
    }

    private fun encodeField(value: String): String {
        val escaped = buildString {
            for (ch in value) {
                when (ch) {
                    '\\', ';', ',', ':', '"' -> append('\\').append(ch)
                    else -> append(ch)
                }
            }
        }
        return if (isAllHex(value) && value.isNotEmpty()) "\"$escaped\"" else escaped
    }

    private fun isAllHex(s: String): Boolean =
        s.isNotEmpty() && s.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }
}
