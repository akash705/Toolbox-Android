package com.toolbox.conversion.aspectratio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class Preset(val label: String, val w: Int, val h: Int)

private val presets = listOf(
    Preset("16:9", 16, 9),
    Preset("4:3", 4, 3),
    Preset("1:1", 1, 1),
    Preset("21:9", 21, 9),
    Preset("3:2", 3, 2),
    Preset("A4 Paper", 210, 297),
)

private fun gcd(a: Long, b: Long): Long = if (b == 0L) a else gcd(b, a % b)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AspectRatioScreen() {
    var widthInput by rememberSaveable { mutableStateOf("1920") }
    var heightInput by rememberSaveable { mutableStateOf("1080") }
    var resizeWidth by rememberSaveable { mutableStateOf("") }
    var resizeHeight by rememberSaveable { mutableStateOf("") }

    val w = widthInput.toLongOrNull()
    val h = heightInput.toLongOrNull()

    val ratioW: Long
    val ratioH: Long
    if (w != null && h != null && w > 0 && h > 0) {
        val g = gcd(w, h)
        ratioW = w / g
        ratioH = h / g
    } else {
        ratioW = 0
        ratioH = 0
    }

    val filledFieldColors = TextFieldDefaults.colors(
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        unfocusedIndicatorColor = Color.Transparent,
        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Dimensions card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "DIMENSIONS",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Icon(
                        Icons.Default.AspectRatio,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    TextField(
                        value = widthInput,
                        onValueChange = { if (it.all { c -> c.isDigit() }) widthInput = it },
                        label = { Text("WIDTH", style = MaterialTheme.typography.labelSmall, letterSpacing = 0.8.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = filledFieldColors,
                        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                    )
                    TextField(
                        value = heightInput,
                        onValueChange = { if (it.all { c -> c.isDigit() }) heightInput = it },
                        label = { Text("HEIGHT", style = MaterialTheme.typography.labelSmall, letterSpacing = 0.8.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = filledFieldColors,
                        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                    )
                }

                if (ratioW > 0 && ratioH > 0) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "SIMPLIFIED RATIO",
                            style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$ratioW : $ratioH",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }

        // Common Presets
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Common Presets",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    presets.forEach { preset ->
                        val isSelected = ratioW > 0 && ratioH > 0 &&
                            ratioW * preset.h.toLong() == ratioH * preset.w.toLong()
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                widthInput = preset.w.toString()
                                heightInput = preset.h.toString()
                                resizeWidth = ""
                                resizeHeight = ""
                            },
                            label = { Text(preset.label) },
                        )
                    }
                }
            }
        }

        // Proportional Resize + Visual Preview card
        if (ratioW > 0 && ratioH > 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "PROPORTIONAL RESIZE",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        TextField(
                            value = resizeWidth,
                            onValueChange = { newVal ->
                                if (newVal.all { c -> c.isDigit() }) {
                                    resizeWidth = newVal
                                    val newW = newVal.toLongOrNull()
                                    resizeHeight = if (newW != null && newW > 0) {
                                        (newW * ratioH / ratioW).toString()
                                    } else ""
                                }
                            },
                            label = { Text("NEW WIDTH", style = MaterialTheme.typography.labelSmall, letterSpacing = 0.8.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = filledFieldColors,
                            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                        )
                        TextField(
                            value = resizeHeight,
                            onValueChange = { newVal ->
                                if (newVal.all { c -> c.isDigit() }) {
                                    resizeHeight = newVal
                                    val newH = newVal.toLongOrNull()
                                    resizeWidth = if (newH != null && newH > 0) {
                                        (newH * ratioW / ratioH).toString()
                                    } else ""
                                }
                            },
                            label = { Text("NEW HEIGHT", style = MaterialTheme.typography.labelSmall, letterSpacing = 0.8.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = filledFieldColors,
                            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "VISUAL PREVIEW",
                            style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (resizeWidth.isNotEmpty() && resizeHeight.isNotEmpty()) {
                            Text(
                                text = "$resizeWidth × $resizeHeight px",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    val ratio = ratioW.toFloat() / ratioH.toFloat()
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (ratio >= 1f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .aspectRatio(ratio)
                                    .border(
                                        2.dp,
                                        MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(12.dp),
                                    )
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                        RoundedCornerShape(12.dp),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "$ratioW:$ratioH",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.4f)
                                    .aspectRatio(ratio)
                                    .border(
                                        2.dp,
                                        MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(12.dp),
                                    )
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                        RoundedCornerShape(12.dp),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "$ratioW:$ratioH",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
