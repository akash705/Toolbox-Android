package com.toolbox.everyday.sleeptimer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SleepTimerScreen() {
    val context = LocalContext.current
    var durationMin by remember { mutableIntStateOf(15) }
    val running by SleepTimerState.running.collectAsState()
    val remainingSec by SleepTimerState.remainingSec.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        if (running) {
            Text("Playing ambient sound", style = MaterialTheme.typography.titleMedium)
            Text(
                "%02d:%02d".format(remainingSec / 60, remainingSec % 60),
                fontSize = 64.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
            )
            OutlinedButton(onClick = { SleepAudioService.stop(context) }, shape = RoundedCornerShape(50)) { Text("Stop now") }
        } else {
            Text("Play ambient sound for", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5, 15, 30, 45, 60).forEach { m ->
                    FilterChip(
                        selected = durationMin == m,
                        onClick = { durationMin = m },
                        label = { Text("$m min") },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { SleepAudioService.start(context, durationMin * 60) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(50),
            ) { Text("Start") }
            Text(
                "The sound keeps playing while the screen is off or you switch apps, and stops automatically when the timer ends.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
