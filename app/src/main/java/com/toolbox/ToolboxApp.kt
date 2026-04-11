package com.toolbox

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.toolbox.conversion.numberbase.NumberBaseScreen
import com.toolbox.conversion.percentage.PercentageScreen
import com.toolbox.conversion.tipcalculator.TipCalculatorScreen
import com.toolbox.conversion.unitconverter.UnitConverterScreen
import com.toolbox.core.persistence.ThemeMode
import com.toolbox.core.ui.components.ToolScaffold
import com.toolbox.core.ui.theme.ToolboxTheme
import com.toolbox.dashboard.DashboardScreen
import com.toolbox.everyday.counter.CounterScreen
import com.toolbox.everyday.random.RandomScreen
import com.toolbox.everyday.stopwatch.StopwatchTimerScreen
import com.toolbox.measurement.compass.CompassScreen
import com.toolbox.measurement.level.LevelScreen
import com.toolbox.measurement.protractor.ProtractorScreen
import com.toolbox.measurement.ruler.RulerScreen
import com.toolbox.measurement.soundmeter.SoundMeterScreen
import com.toolbox.nav.Compass
import com.toolbox.nav.ColorPicker
import com.toolbox.nav.Counter
import com.toolbox.nav.Dashboard
import com.toolbox.nav.Flashlight
import com.toolbox.nav.Level
import com.toolbox.nav.Magnifier
import com.toolbox.nav.NumberBase
import com.toolbox.nav.PercentageCalculator
import com.toolbox.nav.Protractor
import com.toolbox.nav.QrScanner
import com.toolbox.nav.RandomGenerator
import com.toolbox.nav.Ruler
import com.toolbox.nav.SoundMeter
import com.toolbox.nav.StopwatchTimer
import com.toolbox.nav.TipCalculator
import com.toolbox.nav.UnitConverter

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ToolboxApp(themeMode: ThemeMode) {
    ToolboxTheme(themeMode = themeMode) {
        Surface(modifier = Modifier.fillMaxSize()) {
            val navController = rememberNavController()
            SharedTransitionLayout {
                NavHost(
                    navController = navController,
                    startDestination = Dashboard,
                ) {
                    composable<Dashboard> {
                        var selectedTab by rememberSaveable { mutableIntStateOf(0) }

                        Scaffold(
                            topBar = {
                                TopAppBar(
                                    title = {
                                        Text(
                                            text = "Toolbox",
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    },
                                    actions = {
                                        IconButton(onClick = { /* Profile / settings */ }) {
                                            Icon(
                                                Icons.Default.AccountCircle,
                                                contentDescription = "Profile",
                                                tint = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                    ),
                                )
                            },
                            bottomBar = {
                                NavigationBar {
                                    NavigationBarItem(
                                        selected = selectedTab == 0,
                                        onClick = { selectedTab = 0 },
                                        icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                                        label = { Text("Dashboard") },
                                    )
                                    NavigationBarItem(
                                        selected = selectedTab == 1,
                                        onClick = { selectedTab = 1 },
                                        icon = { Icon(Icons.Default.FavoriteBorder, contentDescription = null) },
                                        label = { Text("Favorites") },
                                    )
                                    NavigationBarItem(
                                        selected = selectedTab == 2,
                                        onClick = { selectedTab = 2 },
                                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                        label = { Text("Settings") },
                                    )
                                }
                            },
                        ) { padding ->
                            Box(modifier = Modifier.padding(padding)) {
                                when (selectedTab) {
                                    0 -> DashboardScreen(
                                        onToolClick = { tool ->
                                            val destination = toolDestination(tool.id)
                                            if (destination != null) {
                                                navController.navigate(destination)
                                            }
                                        },
                                        sharedTransitionScope = this@SharedTransitionLayout,
                                        animatedVisibilityScope = this@composable,
                                    )
                                    1 -> PlaceholderTab("Favorites coming soon")
                                    2 -> PlaceholderTab("Settings coming soon")
                                }
                            }
                        }
                    }

                    // Tool screens
                    composable<Level> { ToolScreen("Bubble Level", navController) { LevelScreen() } }
                    composable<Compass> { ToolScreen("Compass", navController) { CompassScreen() } }
                    composable<Protractor> { ToolScreen("Protractor", navController) { ProtractorScreen() } }
                    composable<SoundMeter> { ToolScreen("Sound Meter", navController) { SoundMeterScreen() } }
                    composable<Ruler> { ToolScreen("Ruler", navController) { RulerScreen() } }
                    composable<UnitConverter> { ToolScreen("Unit Converter", navController) { UnitConverterScreen() } }
                    composable<PercentageCalculator> { ToolScreen("Percentage", navController) { PercentageScreen() } }
                    composable<TipCalculator> { ToolScreen("Tip Calculator", navController) { TipCalculatorScreen() } }
                    composable<NumberBase> { ToolScreen("Number Base", navController) { NumberBaseScreen() } }
                    composable<Flashlight> { PlaceholderToolScreen("Flashlight") { navController.popBackStack() } }
                    composable<QrScanner> { PlaceholderToolScreen("QR Scanner") { navController.popBackStack() } }
                    composable<Counter> { ToolScreen("Counter", navController) { CounterScreen() } }
                    composable<StopwatchTimer> { ToolScreen("Stopwatch & Timer", navController) { StopwatchTimerScreen() } }
                    composable<RandomGenerator> { ToolScreen("Random Generator", navController) { RandomScreen() } }
                    composable<ColorPicker> { PlaceholderToolScreen("Color Picker") { navController.popBackStack() } }
                    composable<Magnifier> { PlaceholderToolScreen("Magnifier") { navController.popBackStack() } }
                }
            }
        }
    }
}

@Composable
private fun ToolScreen(title: String, navController: NavController, content: @Composable () -> Unit) {
    ToolScaffold(title = title, onBack = { navController.popBackStack() }) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            content()
        }
    }
}

@Composable
private fun PlaceholderTab(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PlaceholderToolScreen(title: String, onBack: () -> Unit) {
    ToolScaffold(title = title, onBack = onBack) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Coming soon",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun toolDestination(toolId: String): Any? = when (toolId) {
    "level" -> Level
    "compass" -> Compass
    "protractor" -> Protractor
    "sound_meter" -> SoundMeter
    "ruler" -> Ruler
    "unit_converter" -> UnitConverter
    "percentage" -> PercentageCalculator
    "tip_calculator" -> TipCalculator
    "number_base" -> NumberBase
    "flashlight" -> Flashlight
    "qr_scanner" -> QrScanner
    "counter" -> Counter
    "stopwatch_timer" -> StopwatchTimer
    "random_generator" -> RandomGenerator
    "color_picker" -> ColorPicker
    "magnifier" -> Magnifier
    else -> null
}
