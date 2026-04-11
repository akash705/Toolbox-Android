package com.toolbox.core.sharing

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun ShareMeasurementButton(
    toolName: String,
    value: String,
    unit: String,
    label: String? = null,
    accentColorInt: Int = 0xFF1976D2.toInt(),
) {
    val context = LocalContext.current
    IconButton(onClick = {
        MeasurementCardSharer.share(
            context = context,
            toolName = toolName,
            value = value,
            unit = unit,
            label = label,
            accentColorInt = accentColorInt,
        )
    }) {
        Icon(
            imageVector = Icons.Default.Share,
            contentDescription = "Share measurement",
        )
    }
}
