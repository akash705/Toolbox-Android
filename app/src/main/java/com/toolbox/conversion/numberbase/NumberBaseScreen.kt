package com.toolbox.conversion.numberbase

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

private data class BaseField(
    val label: String,
    val radix: Int,
    val validChars: Regex,
    val keyboardType: KeyboardType,
)

private val fields = listOf(
    BaseField("Decimal", 10, Regex("[0-9]*"), KeyboardType.Number),
    BaseField("Binary", 2, Regex("[01]*"), KeyboardType.Number),
    BaseField("Octal", 8, Regex("[0-7]*"), KeyboardType.Number),
    BaseField("Hexadecimal", 16, Regex("[0-9a-fA-F]*"), KeyboardType.Ascii),
)

@Composable
fun NumberBaseScreen() {
    var values by rememberSaveable { mutableStateOf(listOf("", "", "", "")) }
    var activeIndex by rememberSaveable { mutableStateOf(-1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        fields.forEachIndexed { index, field ->
            OutlinedTextField(
                value = values[index],
                onValueChange = { newValue ->
                    if (newValue.isEmpty() || field.validChars.matches(newValue)) {
                        activeIndex = index
                        val parsed = try {
                            newValue.toLong(field.radix)
                        } catch (_: NumberFormatException) {
                            null
                        }
                        values = if (parsed != null) {
                            fields.mapIndexed { i, f ->
                                if (i == index) newValue
                                else parsed.toString(f.radix).let {
                                    if (f.radix == 16) it.uppercase() else it
                                }
                            }
                        } else if (newValue.isEmpty()) {
                            listOf("", "", "", "")
                        } else {
                            values.toMutableList().apply { set(index, newValue) }
                        }
                    }
                },
                label = { Text(field.label) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = field.keyboardType),
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
