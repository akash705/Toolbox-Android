package com.toolbox.everyday.whitenoise

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.FilterDrama
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Water
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.security.SecureRandom

private enum class NoiseType(
    val label: String,
    val icon: ImageVector,
) {
    White("White Noise", Icons.Default.Grain),
    Pink("Pink Noise", Icons.Default.Spa),
    Brown("Brown Noise", Icons.Default.FilterDrama),
    Rain("Rain", Icons.Default.WaterDrop),
    Ocean("Ocean Waves", Icons.Default.Water),
    Forest("Forest", Icons.Default.Forest),
    Fire("Campfire", Icons.Default.LocalFireDepartment),
    Wind("Fan", Icons.Default.Air),
}

private enum class SleepTimer(val label: String, val minutes: Int) {
    Min15("15 Min", 15),
    Min30("30 Min", 30),
    Hr1("1 Hr", 60),
    Hr2("2 Hr", 120),
    Off("Off", 0),
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WhiteNoiseScreen() {
    var selectedNoise by rememberSaveable { mutableStateOf(NoiseType.White.name) }
    var isPlaying by rememberSaveable { mutableStateOf(false) }
    var volume by rememberSaveable { mutableFloatStateOf(0.7f) }
    var selectedTimer by rememberSaveable { mutableStateOf(SleepTimer.Off.name) }
    var elapsedSeconds by rememberSaveable { mutableLongStateOf(0L) }

    val noiseType = NoiseType.valueOf(selectedNoise)
    val sleepTimer = SleepTimer.valueOf(selectedTimer)

    // Audio playback
    val audioTrack = remember { mutableStateOf<AudioTrack?>(null) }

    fun startPlayback() {
        stopPlayback(audioTrack.value)
        val sampleRate = 44100
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        track.setVolume(volume)
        track.play()
        audioTrack.value = track

        Thread {
            val buffer = ShortArray(bufferSize / 2)
            val random = SecureRandom()
            var brownState = 0.0
            // Pink noise state (Voss-McCartney)
            val pinkRows = IntArray(16)
            var pinkRunningSum = 0
            var pinkIndex = 0

            while (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                for (i in buffer.indices) {
                    val sample = when (NoiseType.valueOf(selectedNoise)) {
                        NoiseType.White -> (random.nextGaussian() * 4000).toInt().toShort()
                        NoiseType.Pink -> {
                            pinkIndex = (pinkIndex + 1) % 65536
                            var numChanged = 0
                            var temp = pinkIndex
                            while (temp and 1 == 0 && numChanged < pinkRows.size) {
                                temp = temp shr 1
                                numChanged++
                            }
                            if (numChanged < pinkRows.size) {
                                pinkRunningSum -= pinkRows[numChanged]
                                val newRandom = (random.nextGaussian() * 500).toInt()
                                pinkRunningSum += newRandom
                                pinkRows[numChanged] = newRandom
                            }
                            (pinkRunningSum + random.nextGaussian() * 500).toInt().coerceIn(-32000, 32000).toShort()
                        }
                        NoiseType.Brown -> {
                            brownState += random.nextGaussian() * 200
                            brownState = brownState.coerceIn(-32000.0, 32000.0) * 0.998
                            brownState.toInt().toShort()
                        }
                        NoiseType.Rain -> {
                            val base = (random.nextGaussian() * 2000).toInt()
                            val droplet = if (random.nextInt(500) == 0) (random.nextInt(8000) - 4000) else 0
                            (base + droplet).coerceIn(-32000, 32000).toShort()
                        }
                        NoiseType.Ocean -> {
                            brownState += random.nextGaussian() * 300
                            brownState = brownState.coerceIn(-32000.0, 32000.0) * 0.995
                            val mod = Math.sin(i.toDouble() / sampleRate * 0.15 * Math.PI * 2) * 0.4 + 0.6
                            (brownState * mod).toInt().toShort()
                        }
                        NoiseType.Forest -> {
                            pinkIndex = (pinkIndex + 1) % 65536
                            var numChanged = 0
                            var temp = pinkIndex
                            while (temp and 1 == 0 && numChanged < pinkRows.size) {
                                temp = temp shr 1
                                numChanged++
                            }
                            if (numChanged < pinkRows.size) {
                                pinkRunningSum -= pinkRows[numChanged]
                                val newRandom = (random.nextGaussian() * 300).toInt()
                                pinkRunningSum += newRandom
                                pinkRows[numChanged] = newRandom
                            }
                            val chirp = if (random.nextInt(2000) == 0)
                                (Math.sin(i.toDouble() * 2000.0 / sampleRate * Math.PI * 2) * 3000).toInt() else 0
                            (pinkRunningSum + chirp).coerceIn(-32000, 32000).toShort()
                        }
                        NoiseType.Fire -> {
                            brownState += random.nextGaussian() * 250
                            brownState = brownState.coerceIn(-32000.0, 32000.0) * 0.997
                            val crackle = if (random.nextInt(300) == 0) (random.nextInt(10000) - 5000) else 0
                            (brownState + crackle).toInt().coerceIn(-32000, 32000).toShort()
                        }
                        NoiseType.Wind -> {
                            brownState += random.nextGaussian() * 150
                            brownState = brownState.coerceIn(-32000.0, 32000.0) * 0.999
                            val mod = Math.sin(i.toDouble() / sampleRate * 0.08 * Math.PI * 2) * 0.5 + 0.5
                            (brownState * mod).toInt().toShort()
                        }
                    }
                    buffer[i] = sample
                }
                try {
                    track.write(buffer, 0, buffer.size)
                } catch (_: Exception) {
                    break
                }
            }
        }.start()
    }

    // Volume changes
    LaunchedEffect(volume) {
        audioTrack.value?.setVolume(volume)
    }

    // Elapsed timer
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                delay(1000)
                elapsedSeconds++

                // Check sleep timer
                if (sleepTimer != SleepTimer.Off && elapsedSeconds >= sleepTimer.minutes * 60L) {
                    isPlaying = false
                    elapsedSeconds = 0
                    break
                }
            }
        }
    }

    // Start/stop audio
    LaunchedEffect(isPlaying, selectedNoise) {
        if (isPlaying) {
            startPlayback()
        } else {
            stopPlayback(audioTrack.value)
            audioTrack.value = null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            stopPlayback(audioTrack.value)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Play/pause button
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable {
                    if (isPlaying) {
                        isPlaying = false
                        elapsedSeconds = 0
                    } else {
                        isPlaying = true
                        elapsedSeconds = 0
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(48.dp),
            )
        }

        // Timer display
        val minutes = elapsedSeconds / 60
        val seconds = elapsedSeconds % 60
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "%02d:%02d".format(minutes, seconds),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "ELAPSED TIME",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 2.sp,
            )
        }

        // Sound type card - 2 column grid
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Sound Type",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(12.dp))

                // 2-column grid matching Stitch design
                val noiseTypes = NoiseType.entries
                for (rowIndex in 0 until (noiseTypes.size + 1) / 2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        for (colIndex in 0 until 2) {
                            val index = rowIndex * 2 + colIndex
                            if (index < noiseTypes.size) {
                                val noise = noiseTypes[index]
                                val isSelected = noise.name == selectedNoise
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant,
                                        )
                                        .clickable { selectedNoise = noise.name }
                                        .padding(vertical = 14.dp, horizontal = 8.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Icon(
                                            imageVector = noise.icon,
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(24.dp),
                                        )
                                        Text(
                                            text = noise.label.uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    if (rowIndex < (noiseTypes.size + 1) / 2 - 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        // Sleep timer card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Sleep Timer",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(12.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SleepTimer.entries.forEach { timer ->
                        FilterChip(
                            selected = timer.name == selectedTimer,
                            onClick = { selectedTimer = timer.name },
                            label = { Text(timer.label) },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Volume",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Slider(
                        value = volume,
                        onValueChange = { volume = it },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

private fun stopPlayback(track: AudioTrack?) {
    try {
        track?.stop()
        track?.release()
    } catch (_: Exception) {}
}
