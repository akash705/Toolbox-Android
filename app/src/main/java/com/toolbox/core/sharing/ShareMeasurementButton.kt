package com.toolbox.core.sharing

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

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

@Composable
fun ShareButton(
    toolName: String,
    value: String,
    unit: String,
    label: String? = null,
    accentColorInt: Int = 0xFF1976D2.toInt(),
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    OutlinedButton(
        onClick = {
            MeasurementCardSharer.share(
                context = context,
                toolName = toolName,
                value = value,
                unit = unit,
                label = label,
                accentColorInt = accentColorInt,
            )
        },
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Share,
            contentDescription = "Share measurement",
            modifier = Modifier.size(20.dp),
        )
    }
}
