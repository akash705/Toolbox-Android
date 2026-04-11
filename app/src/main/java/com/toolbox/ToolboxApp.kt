package com.toolbox

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
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
import kotlinx.coroutines.launch
import com.toolbox.conversion.numberbase.NumberBaseScreen
import com.toolbox.conversion.percentage.PercentageScreen
import com.toolbox.conversion.tipcalculator.TipCalculatorScreen
import com.toolbox.conversion.unitconverter.UnitConverterScreen
import com.toolbox.core.persistence.ThemeMode
import com.toolbox.core.ui.components.ToolScaffold
import com.toolbox.core.ui.theme.ToolboxTheme
import com.toolbox.dashboard.DashboardScreen
import com.toolbox.dashboard.allTools
import com.toolbox.favorites.FavoritesScreen
import com.toolbox.everyday.colorpicker.ColorPickerScreen
import com.toolbox.everyday.counter.CounterScreen
import com.toolbox.everyday.magnifier.MagnifierScreen
import com.toolbox.everyday.qrscanner.QrScannerScreen
import com.toolbox.lighting.FlashlightScreen
import com.toolbox.everyday.random.RandomScreen
import com.toolbox.everyday.stopwatch.StopwatchTimerScreen
import com.toolbox.everyday.heartrate.HeartRateScreen
import com.toolbox.everyday.mirror.MirrorScreen
import com.toolbox.motion.pedometer.PedometerScreen
import com.toolbox.measurement.barometer.BarometerScreen
import com.toolbox.measurement.gyroscope.GyroscopeScreen
import com.toolbox.measurement.humidity.HumidityScreen
import com.toolbox.measurement.compass.CompassScreen
import com.toolbox.measurement.level.LevelScreen
import com.toolbox.measurement.lightmeter.LightMeterScreen
import com.toolbox.measurement.metaldetector.MetalDetectorScreen
import com.toolbox.measurement.protractor.ProtractorScreen
import com.toolbox.measurement.ruler.RulerScreen
import com.toolbox.measurement.altitude.AltitudeScreen
import com.toolbox.measurement.soundmeter.SoundMeterScreen
import com.toolbox.measurement.speedometer.SpeedometerScreen
import com.toolbox.measurement.spectrum.SpectrumScreen
import com.toolbox.measurement.vibrometer.VibrometerScreen
import com.toolbox.nav.Barometer
import com.toolbox.nav.Humidity
import com.toolbox.nav.Gyroscope
import com.toolbox.nav.HeartRate
import com.toolbox.nav.Speedometer
import com.toolbox.nav.Altitude
import com.toolbox.nav.SpectrumAnalyzer
import com.toolbox.nav.Pedometer
import com.toolbox.nav.Compass
import com.toolbox.nav.ColorPicker
import com.toolbox.nav.Counter
import com.toolbox.nav.Dashboard
import com.toolbox.nav.Flashlight
import com.toolbox.nav.Level
import com.toolbox.nav.LightMeter
import com.toolbox.nav.Magnifier
import com.toolbox.nav.MetalDetector
import com.toolbox.nav.Mirror
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
import com.toolbox.nav.Vibrometer
import com.toolbox.settings.SettingsScreen

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ToolboxApp(themeMode: ThemeMode, launchToolId: String? = null) {
    ToolboxTheme(themeMode = themeMode) {
        Surface(modifier = Modifier.fillMaxSize()) {
            val navController = rememberNavController()

            // Handle deep-link from Quick Settings tile
            androidx.compose.runtime.LaunchedEffect(launchToolId) {
                if (launchToolId != null) {
                    val destination = toolDestination(launchToolId)
                    if (destination != null) {
                        navController.navigate(destination)
                    }
                }
            }

            SharedTransitionLayout {
                NavHost(
                    navController = navController,
                    startDestination = Dashboard,
                    enterTransition = { fadeIn(tween(300)) + slideInHorizontally(tween(300)) { it / 4 } },
                    exitTransition = { fadeOut(tween(300)) },
                    popEnterTransition = { fadeIn(tween(300)) },
                    popExitTransition = { fadeOut(tween(300)) + slideOutHorizontally(tween(300)) { it / 4 } },
                ) {
                    composable<Dashboard> {
                        var selectedTab by rememberSaveable { mutableIntStateOf(0) }

                        Scaffold(
                            topBar = {
                                Text(
                                    text = "Toolbox",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
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
                                    1 -> FavoritesScreen(
                                        onToolClick = { tool ->
                                            val destination = toolDestination(tool.id)
                                            if (destination != null) {
                                                navController.navigate(destination)
                                            }
                                        },
                                    )
                                    2 -> SettingsScreen()
                                }
                            }
                        }
                    }

                    // Tool screens
                    composable<Level> { ToolScreen("Bubble Level", "level", navController, this@SharedTransitionLayout, this@composable) { LevelScreen() } }
                    composable<Compass> { ToolScreen("Compass", "compass", navController, this@SharedTransitionLayout, this@composable) { CompassScreen() } }
                    composable<Protractor> { ToolScreen("Protractor", "protractor", navController, this@SharedTransitionLayout, this@composable) { ProtractorScreen() } }
                    composable<SoundMeter> { ToolScreen("Sound Meter", "sound_meter", navController, this@SharedTransitionLayout, this@composable) { SoundMeterScreen() } }
                    composable<Ruler> { ToolScreen("Ruler", "ruler", navController, this@SharedTransitionLayout, this@composable) { RulerScreen() } }
                    composable<UnitConverter> { ToolScreen("Unit Converter", "unit_converter", navController, this@SharedTransitionLayout, this@composable) { UnitConverterScreen() } }
                    composable<PercentageCalculator> { ToolScreen("Percentage", "percentage", navController, this@SharedTransitionLayout, this@composable) { PercentageScreen() } }
                    composable<TipCalculator> { ToolScreen("Tip Calculator", "tip_calculator", navController, this@SharedTransitionLayout, this@composable) { TipCalculatorScreen() } }
                    composable<NumberBase> { ToolScreen("Number Base", "number_base", navController, this@SharedTransitionLayout, this@composable) { NumberBaseScreen() } }
                    composable<Flashlight> { ToolScreen("Flashlight", "flashlight", navController, this@SharedTransitionLayout, this@composable) { FlashlightScreen() } }
                    composable<QrScanner> { ToolScreen("QR Scanner", "qr_scanner", navController, this@SharedTransitionLayout, this@composable) { QrScannerScreen() } }
                    composable<Counter> { ToolScreen("Counter", "counter", navController, this@SharedTransitionLayout, this@composable) { CounterScreen() } }
                    composable<StopwatchTimer> { ToolScreen("Stopwatch & Timer", "stopwatch_timer", navController, this@SharedTransitionLayout, this@composable) { StopwatchTimerScreen() } }
                    composable<RandomGenerator> { ToolScreen("Random Generator", "random_generator", navController, this@SharedTransitionLayout, this@composable) { RandomScreen() } }
                    composable<ColorPicker> { ToolScreen("Color Picker", "color_picker", navController, this@SharedTransitionLayout, this@composable) { ColorPickerScreen() } }
                    composable<Magnifier> { ToolScreen("Magnifier", "magnifier", navController, this@SharedTransitionLayout, this@composable) { MagnifierScreen() } }
                    composable<Mirror> { ToolScreen("Mirror", "mirror", navController, this@SharedTransitionLayout, this@composable) { MirrorScreen() } }
                    composable<Vibrometer> { ToolScreen("Vibrometer", "vibrometer", navController, this@SharedTransitionLayout, this@composable) { VibrometerScreen() } }
                    composable<LightMeter> { ToolScreen("Light Meter", "light_meter", navController, this@SharedTransitionLayout, this@composable) { LightMeterScreen() } }
                    composable<MetalDetector> { ToolScreen("Metal Detector", "metal_detector", navController, this@SharedTransitionLayout, this@composable) { MetalDetectorScreen() } }
                    composable<Barometer> { ToolScreen("Barometer", "barometer", navController, this@SharedTransitionLayout, this@composable) { BarometerScreen() } }
                    composable<Humidity> { ToolScreen("Humidity", "humidity", navController, this@SharedTransitionLayout, this@composable) { HumidityScreen() } }
                    composable<Pedometer> { ToolScreen("Pedometer", "pedometer", navController, this@SharedTransitionLayout, this@composable) { PedometerScreen() } }
                    composable<Gyroscope> { ToolScreen("Gyroscope", "gyroscope", navController, this@SharedTransitionLayout, this@composable) { GyroscopeScreen() } }
                    composable<HeartRate> { ToolScreen("Heart Rate", "heart_rate", navController, this@SharedTransitionLayout, this@composable) { HeartRateScreen() } }
                    composable<SpectrumAnalyzer> { ToolScreen("Spectrum Analyzer", "spectrum_analyzer", navController, this@SharedTransitionLayout, this@composable) { SpectrumScreen() } }
                    composable<Speedometer> { ToolScreen("Speedometer", "speedometer", navController, this@SharedTransitionLayout, this@composable) { SpeedometerScreen() } }
                    composable<Altitude> { ToolScreen("Altitude", "altitude", navController, this@SharedTransitionLayout, this@composable) { AltitudeScreen() } }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ToolScreen(
    title: String,
    toolId: String,
    navController: NavController,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    content: @Composable () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = remember { com.toolbox.core.persistence.UserPreferencesRepository(context) }
    val favoriteIds by repository.favoriteToolIds.collectAsState(initial = emptySet())
    val isFavorite = toolId in favoriteIds
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    val helpText = remember(toolId) { allTools.find { it.id == toolId }?.description?.takeIf { it.isNotBlank() } }

    val sharedModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            Modifier.sharedBounds(
                rememberSharedContentState(key = "tool_$toolId"),
                animatedVisibilityScope = animatedVisibilityScope,
                resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                enter = fadeIn(),
                exit = fadeOut(),
            )
        }
    } else {
        Modifier
    }

    ToolScaffold(
        title = title,
        onBack = { navController.popBackStack() },
        modifier = sharedModifier,
        helpText = helpText,
        actions = {
            IconButton(onClick = { scope.launch { repository.toggleFavorite(toolId) } }) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    ) { padding ->
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
    "mirror" -> Mirror
    "vibrometer" -> Vibrometer
    "light_meter" -> LightMeter
    "metal_detector" -> MetalDetector
    "barometer" -> Barometer
    "humidity" -> Humidity
    "pedometer" -> Pedometer
    "gyroscope" -> Gyroscope
    "heart_rate" -> HeartRate
    "spectrum_analyzer" -> SpectrumAnalyzer
    "speedometer" -> Speedometer
    "altitude" -> Altitude
    else -> null
}
