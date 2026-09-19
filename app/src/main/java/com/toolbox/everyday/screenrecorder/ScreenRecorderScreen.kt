package com.toolbox.everyday.screenrecorder

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ScreenRecorderScreen() {
    val context = LocalContext.current
    val recording by ScreenRecordState.recording.collectAsState()
    val dir = remember { File(context.filesDir, "screen_recordings").apply { mkdirs() } }
    var files by remember { mutableStateOf(listFiles(dir)) }
    var micEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(recording) { files = listFiles(dir) }

    val projectionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK && res.data != null) {
            val withAudio = micEnabled && hasAudioPermission(context)
            val intent = Intent(context, ScreenRecordService::class.java).apply {
                putExtra(ScreenRecordService.EXTRA_CODE, res.resultCode)
                putExtra(ScreenRecordService.EXTRA_DATA, res.data)
                putExtra(ScreenRecordService.EXTRA_AUDIO, withAudio)
            }
            context.startForegroundService(intent)
        }
    }

    val audioPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        micEnabled = granted
        if (!granted) Toast.makeText(context, "Microphone denied — recording without audio", Toast.LENGTH_SHORT).show()
    }

    fun startRecording() {
        val mpm = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projectionLauncher.launch(mpm.createScreenCaptureIntent())
    }
    fun stopRecording() {
        context.startService(Intent(context, ScreenRecordService::class.java).setAction(ScreenRecordService.ACTION_STOP))
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(8.dp))
        if (recording) {
            Button(
                onClick = { stopRecording() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) { Text("Stop recording") }
        } else {
            Button(onClick = { startRecording() }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text("Record screen")
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                "Record microphone audio",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f).padding(start = 12.dp),
            )
            Switch(
                enabled = !recording,
                checked = micEnabled,
                onCheckedChange = { want ->
                    if (want && !hasAudioPermission(context)) {
                        audioPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else {
                        micEnabled = want
                    }
                },
            )
        }
        Text(
            "Android will ask permission to capture your screen. Recording keeps going while you use other apps.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text("Recordings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))

        LazyColumn(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(files, key = { it.absolutePath }) { file ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)) {
                    Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { openVideo(context, file) }) { Icon(Icons.Default.PlayArrow, contentDescription = "Play") }
                        Text(SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(Date(file.lastModified())), Modifier.weight(1f))
                        IconButton(onClick = { shareVideo(context, file) }) { Icon(Icons.Default.Share, contentDescription = "Share") }
                        IconButton(onClick = { file.delete(); files = listFiles(dir) }) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
                    }
                }
            }
        }
    }
}

private fun hasAudioPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

private fun listFiles(dir: File): List<File> =
    dir.listFiles { f -> f.extension == "mp4" }?.sortedByDescending { it.lastModified() } ?: emptyList()

private fun uriFor(context: Context, file: File) =
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

private fun openVideo(context: Context, file: File) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uriFor(context, file), "video/mp4")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(intent)
}

private fun shareVideo(context: Context, file: File) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "video/mp4"
        putExtra(Intent.EXTRA_STREAM, uriFor(context, file))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share recording"))
}
