package com.toolbox.core.rate

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.toolbox.core.persistence.UserPreferencesRepository
import kotlinx.coroutines.launch

/**
 * Root-hosted "Rate us" prompt. Shows once the repository's [UserPreferencesRepository.shouldShowRatePrompt]
 * becomes true (first at the 3rd launch, then +5 launches after each "Not now"), with three actions:
 * Rate (opens the store, never auto-shows again), Not now (snooze), Never ask again (opt out).
 */
@Composable
fun RatePrompt() {
    val context = LocalContext.current
    val repo = remember { UserPreferencesRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()
    val shouldShow by repo.shouldShowRatePrompt.collectAsState(initial = false)

    // Hide immediately after an action so the dialog doesn't linger while DataStore updates.
    var actedThisSession by remember { mutableStateOf(false) }

    if (!shouldShow || actedThisSession) return

    AlertDialog(
        onDismissRequest = {
            actedThisSession = true
            scope.launch { repo.snoozeRate() }
        },
        title = { Text("Enjoying Toolbox?") },
        text = { Text("If these tools are useful, a quick rating on the Play Store really helps.") },
        confirmButton = {
            TextButton(onClick = {
                actedThisSession = true
                scope.launch { repo.markRated() }
                PlayStoreLauncher.open(context)
            }) { Text("Rate") }
        },
        dismissButton = {
            Row2(
                notNow = {
                    actedThisSession = true
                    scope.launch { repo.snoozeRate() }
                },
                never = {
                    actedThisSession = true
                    scope.launch { repo.setNeverAskRate() }
                },
            )
        },
    )
}

@Composable
private fun Row2(notNow: () -> Unit, never: () -> Unit) {
    androidx.compose.foundation.layout.Row {
        TextButton(onClick = never) { Text("Never") }
        TextButton(onClick = notNow) { Text("Not now") }
    }
}
