# Toolbox — Android Utility App

A privacy-respecting, offline-first Android utility app with 25+ tools across measurement, conversion, and everyday use. Built with Kotlin + Jetpack Compose + Material 3.

## Features

### Measurement
| Tool | Description |
|------|-------------|
| Bubble Level | Circular + linear level with pitch/roll readout |
| Compass | Compass rose with magnetic/true north toggle |
| Protractor | On-screen angle measurement |
| Sound Meter | Live dB meter with min/avg/max stats |
| Ruler | Full-width ruler with cm/in toggle and credit-card calibration |
| Light Meter | Ambient lux sensor readout |
| Barometer | Atmospheric pressure via pressure sensor |
| Altitude | Elevation estimate from barometric pressure |
| Gyroscope | Real-time angular velocity on all three axes |
| Humidity | Relative humidity (on supported hardware) |
| Metal Detector | Magnetic field strength as a proximity indicator |
| Speedometer | GPS-based speed with unit toggle |
| Vibrometer | Vibration intensity from accelerometer |
| Spectrum Analyzer | Real-time audio frequency spectrum via FFT |

### Conversion
| Tool | Description |
|------|-------------|
| Unit Converter | 10+ categories — length, weight, temperature, volume, and more |
| Percentage Calculator | Percent of, percent change, and reverse calculations |
| Tip Calculator | Bill splitting with custom tip % |
| Number Base | Convert between binary, octal, decimal, and hex |

### Everyday
| Tool | Description |
|------|-------------|
| QR Scanner | Scan barcodes/QR codes and generate QR codes from text |
| Counter | Persistent multi-step counter with DataStore persistence |
| Stopwatch & Timer | Stopwatch with laps; countdown timer with background notification |
| Random | Dice rolls, coin flip, and random number generator |
| Color Picker | Sample any color from the camera feed |
| Magnifier | Camera-based magnifier with zoom control |
| Mirror | Front camera as a mirror |
| Heart Rate | Estimate BPM via rear camera finger placement |

### Other
| Tool | Description |
|------|-------------|
| Flashlight | Torch with brightness control, SOS mode, and strobe |
| Pedometer | Step counter using the hardware step sensor |

## Platform Features

- **Quick Settings Tiles** — Flashlight, Sound Meter, Bubble Level, and Timer directly from the notification shade
- **App Shortcuts** — Jump to frequently used tools from the launcher long-press menu
- **Favorites** — Pin any tool to a customizable favorites screen
- **Share Measurements** — Export a measurement card image from any sensor tool
- **Dynamic theming** — Material You color system on Android 12+, static fallback on older devices
- **Predictive back** — Full support for Android 14 predictive back gesture

## Tech Stack

- **Language**: Kotlin 2.0+
- **UI**: Jetpack Compose + Material 3
- **Navigation**: Type-safe Navigation Compose 2.8+ (`@Serializable` routes)
- **Camera**: CameraX 1.6+ with `camera-compose` viewfinder
- **QR**: ZXing core 3.5.3 (custom `ImageAnalysis.Analyzer`, no external Activity)
- **Persistence**: DataStore Preferences (counter state, favorites, theme)
- **Timer**: `AlarmManager.setExactAndAllowWhileIdle()` + foreground service for live notification
- **Sensors**: `DisposableEffect` + `LifecycleEventObserver` with low-pass filtering
- **Min SDK**: 26 (Android 8.0) · **Target SDK**: 36

## Architecture

Single-module, package-per-feature layout. No DI framework — manual `ViewModelProvider.Factory` injection kept simple for MVP scale.

```
app/
├── core/          # Shared — sensors, camera, permissions, UI components, sharing
├── dashboard/     # Home screen grid with search
├── measurement/   # Sensor-based measurement tools
├── conversion/    # Unit/math conversion tools
├── everyday/      # Camera and general-purpose tools
├── lighting/      # Flashlight
├── motion/        # Pedometer
├── favorites/     # Pinned tools screen
├── settings/      # Theme + preferences
├── tiles/         # Quick Settings tile services
└── nav/           # Navigation destinations
```

## Building

```bash
# Debug build
./gradlew assembleDebug

# Release build (requires signing config)
./gradlew assembleRelease
```

Requires Android Studio Meerkat or newer, JDK 21.

## Privacy

- No analytics, no crash reporting, no ads
- No network requests — fully offline
- Runtime permissions are requested lazily, only when the relevant tool is opened
- All sensor data stays on-device

## License

MIT
