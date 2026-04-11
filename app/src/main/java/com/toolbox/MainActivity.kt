package com.toolbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.toolbox.core.persistence.ThemeMode
import com.toolbox.core.persistence.UserPreferencesRepository

class MainActivity : ComponentActivity() {

    private val preferencesRepository by lazy { UserPreferencesRepository(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val themeMode by preferencesRepository.themeMode
                .collectAsState(initial = ThemeMode.System)
            ToolboxApp(themeMode = themeMode)
        }
    }
}
