package com.toolbox.everyday.ttsreader

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun TtsReaderScreen() {
    val context = LocalContext.current
    var text by rememberSaveable { mutableStateOf("") }
    var rate by rememberSaveable { mutableFloatStateOf(1.0f) }
    var pitch by rememberSaveable { mutableFloatStateOf(1.0f) }
    var ready by remember { mutableStateOf(false) }
    var initError by remember { mutableStateOf<String?>(null) }
    var speaking by remember { mutableStateOf(false) }

    val tts = remember {
        var instance: TextToSpeech? = null
        instance = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = instance?.setLanguage(Locale.getDefault())
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    initError = "Default language not supported by the system TTS engine."
                } else {
                    ready = true
                }
            } else {
                initError = "TTS engine could not start. Install/enable a TTS engine in system settings."
            }
        }
        instance
    }

    LaunchedEffect(tts, ready) {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) { speaking = true }
            override fun onDone(utteranceId: String?) { speaking = false }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) { speaking = false }
        })
    }

    DisposableEffect(tts) {
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Text to read", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("Paste or type text…") },
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    minLines = 6,
                )
                Spacer(Modifier.height(12.dp))

                Text("Speech rate: ${"%.1f".format(rate)}×", style = MaterialTheme.typography.bodyMedium)
                Slider(value = rate, onValueChange = { rate = it }, valueRange = 0.5f..2.0f)

                Spacer(Modifier.height(4.dp))
                Text("Pitch: ${"%.1f".format(pitch)}×", style = MaterialTheme.typography.bodyMedium)
                Slider(value = pitch, onValueChange = { pitch = it }, valueRange = 0.5f..1.8f)

                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            tts.setSpeechRate(rate)
                            tts.setPitch(pitch)
                            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts_${System.currentTimeMillis()}")
                        },
                        enabled = ready && text.isNotBlank() && !speaking,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (speaking) "Speaking…" else "Speak")
                    }
                    FilledTonalButton(
                        onClick = { tts.stop(); speaking = false },
                        enabled = speaking,
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop")
                    }
                }

                initError?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                if (!ready && initError == null) {
                    Spacer(Modifier.height(8.dp))
                    Text("Initializing TTS engine…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("How it works", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Uses your phone's built-in text-to-speech engine — no network needed. Voice quality and language depend on the engine you have installed (Settings → Accessibility → Text-to-speech).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

