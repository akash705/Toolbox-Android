package com.toolbox.conversion.numberbase

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private enum class Base(
    val label: String,
    val chipLabel: String,
    val radix: Int,
    val validChars: Regex,
    val keyboardType: KeyboardType,
) {
    Binary("Binary", "BIN", 2, Regex("[01]*"), KeyboardType.Number),
    Octal("Octal", "OCT", 8, Regex("[0-7]*"), KeyboardType.Number),
    Decimal("Decimal", "DEC", 10, Regex("[0-9]*"), KeyboardType.Number),
    Hexadecimal("Hexadecimal", "HEX", 16, Regex("[0-9a-fA-F]*"), KeyboardType.Ascii),
}

@Composable
fun NumberBaseScreen() {
    var inputText by rememberSaveable { mutableStateOf("") }
    var selectedBaseOrdinal by rememberSaveable { mutableIntStateOf(Base.Decimal.ordinal) }
    val selectedBase = Base.entries[selectedBaseOrdinal]
    val context = LocalContext.current

    val parsed: Long? = if (inputText.isNotEmpty()) {
        try {
            inputText.toLong(selectedBase.radix)
        } catch (_: NumberFormatException) {
            null
        }
    } else {
        null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        // Input field
        OutlinedTextField(
            value = inputText,
            onValueChange = { newValue ->
                if (newValue.isEmpty() || selectedBase.validChars.matches(newValue)) {
                    inputText = newValue
                }
            },
            label = { Text("Enter ${selectedBase.label.lowercase()} number") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = selectedBase.keyboardType),
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Base selector chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Base.entries.forEach { base ->
                FilterChip(
                    selected = selectedBase == base,
                    onClick = {
                        // Convert current value to new base input before switching
                        if (parsed != null) {
                            inputText = parsed.toString(base.radix).let {
                                if (base.radix == 16) it.uppercase() else it
                            }
                        } else {
                            inputText = ""
                        }
                        selectedBaseOrdinal = base.ordinal
                    },
                    label = { Text(base.chipLabel) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Conversions",
            style = MaterialTheme.typography.labelLarge,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Result cards
        Base.entries.forEach { base ->
            val convertedValue = if (parsed != null) {
                parsed.toString(base.radix).let {
                    if (base.radix == 16) it.uppercase() else it
                }
            } else {
                "—"
            }

            val isActiveBase = base == selectedBase

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isActiveBase) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = base.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isActiveBase) {
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = convertedValue,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace,
                            color = if (isActiveBase) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }
                    if (parsed != null) {
                        IconButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText(base.label, convertedValue))
                        }) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copy ${base.label}",
                                tint = if (isActiveBase) {
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
