---
title: "feat: Build Android Toolbox MVP with 16 utility tools"
type: feat
status: active
date: 2026-04-11
origin: docs/brainstorms/2026-04-11-android-toolbox-requirements.md
deepened: 2026-04-11
---

# feat: Build Android Toolbox MVP with 16 utility tools

## Overview

Build a comprehensive Android utility app ("Toolbox") with 16 tools across 4 categories, targeting Play Store publication. Built with Kotlin + Jetpack Compose + Material 3 with zero-permission cold start architecture. The app uses a grid dashboard with search, offline-first design, and DataStore persistence.

## Problem Frame

Phone users install multiple single-purpose utility apps or suffer through bloated, ad-heavy toolbox apps. This app provides a well-designed, privacy-respecting alternative with modern Android standards. (see origin: docs/brainstorms/2026-04-11-android-toolbox-requirements.md)

## Requirements Trace

- R1-R7: App shell, dashboard, navigation, theming, offline-first, single-module
- R8-R11: Zero-permission cold start, lazy runtime permissions, no analytics, foreground service permissions
- R12-R15: State persistence for counters, converter favorites, timer
- R16-R20: Measurement tools — Level, Compass, Protractor, Sound Meter, Ruler
- R21-R24: Conversion tools — Unit Converter, Percentage, Tip, Number Base
- R25: Flashlight with SOS/strobe
- R26-R31: Everyday tools — QR Scanner+Generator, Counter, Stopwatch/Timer, Random/Coin/Dice, Color Picker, Magnifier
- R34-R37: Haptic feedback, transitions, minSdk 26, APK < 15 MB

## Scope Boundaries

- No monetization, ads, or in-app purchases
- No user accounts or cloud sync
- No Screen Dimmer (Phase 2 — SYSTEM_ALERT_WINDOW conflicts with zero-permission listing)
- No Protractor camera mode (Phase 2)
- No Sound Meter exposure tracking (Phase 2)
- No Widget Dock or Project Clipboard (Phase 2)
- No DI framework (manual injection for MVP)

## Context & Research

### Relevant Patterns and Technology

- **Navigation**: Type-safe Navigation Compose (2.8+) with `@Serializable` route objects. Flat dashboard → tool → back pattern.
- **Theming**: `dynamicDarkColorScheme`/`dynamicLightColorScheme` on Android 12+, static `MaterialTheme` fallback on older. Theme preference in DataStore.
- **CameraX**: Use `androidx.camera:camera-compose` with `CameraXViewfinder` composable (stable since 1.5.0). Shared camera preview composable in `core/camera/` used by QR Scanner, Color Picker, Magnifier.
- **Sensors**: `DisposableEffect` + `LifecycleEventObserver` — register on RESUME, unregister on PAUSE. `SENSOR_DELAY_UI` for display tools. Low-pass filtering for level/compass smoothing.
- **ZXing**: `com.google.zxing:core:3.5.3` only (NOT `journeyapps/zxing-android-embedded` — it launches a separate Activity). Custom `ImageAnalysis.Analyzer` for scanning, `MultiFormatWriter` for generation.
- **Timer service**: Hybrid approach — `AlarmManager.setExactAndAllowWhileIdle()` for guaranteed completion trigger + foreground service (`foregroundServiceType="specialUse"`) only for live countdown notification display. This way timer completes even if the service is killed. Requires `SCHEDULE_EXACT_ALARM` permission (auto-granted for timer apps on Android 14+).
- **Persistence**: DataStore Preferences for counter values, converter favorites, theme preference. Room not needed for MVP.
- **Shared transitions**: `SharedTransitionLayout` wrapping `NavHost`, `Modifier.sharedElement()` for tile → fullscreen animations. Stable since Compose Animation 1.7.0.
- **Predictive back**: Add `android:enableOnBackInvokedCallback="true"` to manifest. Navigation Compose 2.8+ handles the gesture automatically.
- **Gradle**: Kotlin 2.0+ Compose compiler plugin (NOT legacy `kotlinCompilerExtensionVersion`). Version catalogs with Compose BOM.

### Key Library Versions

| Library | Version | Purpose |
|---------|---------|---------|
| Compose BOM | 2026.04.00 | Pin all Compose artifact versions |
| Navigation Compose | 2.9.x | Type-safe navigation (separate from BOM) |
| CameraX | 1.6.x | Camera preview + analysis |
| camera-compose | 1.6.x | Compose-native viewfinder |
| ZXing core | 3.5.3 | QR/barcode decode + generate |
| DataStore | 1.1.x | Preferences persistence |
| Lifecycle | 2.9.x | ViewModel, lifecycle-aware components |
| kotlinx-serialization | 1.7.x | Type-safe nav route serialization |

### UI Design References (Stitch MCP)

Screen designs are available in Stitch project `17370519435532616667`. Use `mcp__stitch__get_screen` to retrieve HTML/screenshots as visual reference during implementation.

| Screen | Stitch Screen ID | Description |
|--------|-----------------|-------------|
| Dashboard | `3df379e001e742cfbe1c52709748dc4c` | Grid dashboard with categorized tool tiles and search |
| Bubble Level | `a242e393b1da4d83bccb051b682ee7c8` | Circular + linear level with pitch/roll display |
| Unit Converter | `8dd8e9a0414c44999e877c6f8bd9421b` | Category chips, bidirectional input fields, favorites |
| Compass | `9fb9858efdb242e583adc0b009305318` | Compass rose with bearing and magnetic/true north toggle |
| Sound Meter | `e199cbbf851c48e38127fc9fc77d7d85` | Arc gauge with color zones, min/avg/max stats |
| QR Scanner | `2d205b828d8543868ef2fb853b91e95f` | Camera viewfinder with scan/generate tabs |
| Ruler | `666ef0eccff4402cb99c7eac51904a45` | Full-width ruler strip, cm/in toggle, calibration via credit card |

Remaining tool screens (Flashlight, Counter, Stopwatch, Protractor, Percentage, Tip Calculator, Number Base, Random/Dice, Color Picker, Magnifier) can be generated from Stitch MCP as needed during implementation using `mcp__stitch__generate_screen_from_text` with project ID `17370519435532616667`.

## Key Technical Decisions

- **No DI framework**: 16 ViewModels with simple dependencies; manual constructor injection + `ViewModelProvider.Factory` is sufficient. Add Hilt only if the dependency graph becomes painful. (see origin: R7 — single-module)
- **Individual `@Serializable` objects per tool** (not `ToolDestination(id: String)`): Gives compile-time safety and allows tool-specific arguments later without refactoring.
- **Sensor composable hooks**: Custom `remember*Data()` composables (e.g., `rememberAccelerometerData()`) that handle lifecycle registration/unregistration internally. Tools consume `State<FloatArray>` directly.
- **Compass defaults to magnetic north**: True north requires `ACCESS_COARSE_LOCATION` for magnetic declination calculation. The toggle triggers runtime permission request. (see origin: R17)
- **Color Picker samples 11x11 pixel region**: Averaging reduces noise from camera sensor. Color sampled from processed preview frame (post-ISP), matching what the user sees. (see origin: R30)
- **Flashlight brightness hidden on unsupported devices**: `CameraManager.setTorchMode` brightness API is Android 13+ and device-specific. Hide the slider entirely when unsupported. (see origin: R25)
- **Credit card reference for ruler calibration**: User places a standard credit card (85.6mm × 53.98mm) on screen and adjusts a reference rectangle to match. App computes actual DPI from the size ratio. (see origin: R20)

## Open Questions

### Resolved During Planning

- **Navigation library**: Type-safe Navigation Compose with `@Serializable` routes — standard for flat Compose apps
- **Module architecture**: Single module with package-per-category — multi-module over-engineering for solo MVP
- **Single vs multi-Activity**: Single Activity — only viable option for Compose-first app
- **ML Kit vs ZXing**: ZXing core only — no INTERNET permission, ~0.5 MB, compatible with single-Activity
- **DPI calibration approach**: Credit card reference — well-established UX pattern used by on-screen ruler apps

### Deferred to Implementation

- **Exact low-pass filter alpha value** for level/compass smoothing — tune by feel on physical devices
- **Sound Meter dB calibration offset** — varies per device microphone, may need user-adjustable offset or disclaimer
- **Compose animation specs** for shared element transitions — tune enter/exit animations during polish phase

## High-Level Technical Design

> *This illustrates the intended approach and is directional guidance for review, not implementation specification. The implementing agent should treat it as context, not code to reproduce.*

```
┌──────────────────────────────────────────────┐
│ MainActivity (Single Activity)               │
│  ├── ToolboxApp composable                   │
│  │   ├── ToolboxTheme (Material 3 / You)     │
│  │   └── SharedTransitionLayout              │
│  │       └── NavHost (type-safe routes)      │
│  │           ├── Dashboard (grid + search)   │
│  │           ├── Level → LevelScreen         │
│  │           ├── Compass → CompassScreen     │
│  │           ├── ... (16 tool screens)       │
│  │           └── Magnifier → MagnifierScreen │
│  └── TimerService (foreground, bound)        │
│                                              │
│ Package Layout:                              │
│  com.toolbox/                                │
│   ├── core/                                  │
│   │   ├── sensor/    (accelerometer, mag,    │
│   │   │               microphone hooks)      │
│   │   ├── camera/    (shared CameraX preview)│
│   │   ├── permission/(runtime perm gate)     │
│   │   ├── persistence/(DataStore repo)       │
│   │   └── ui/        (theme, ToolScaffold,   │
│   │                   shared components)     │
│   ├── dashboard/     (grid, search, tool     │
│   │                   definitions)           │
│   ├── measurement/   (level, compass,        │
│   │                   protractor, sound,     │
│   │                   ruler)                 │
│   ├── conversion/    (unit, percentage,      │
│   │                   tip, numberbase)       │
│   ├── lighting/      (flashlight)            │
│   └── everyday/      (qr, counter, stopwatch,│
│                       random, colorpicker,   │
│                       magnifier)             │
└──────────────────────────────────────────────┘
```

**Data flow for sensor tools:**
```
SensorManager → LifecycleEventObserver (register/unregister)
  → SensorEventListener → MutableState<FloatArray>
    → Compose recomposition → UI update
```

**Data flow for camera tools:**
```
CameraX Provider → Preview + ImageAnalysis
  → CameraXViewfinder composable (preview)
  → Analyzer callback (QR decode / color sample)
    → ViewModel StateFlow → Compose UI
```

## Implementation Units

### Phase 1: Project Scaffold

- [x] **Unit 1: Gradle project setup**

  **Goal:** Create the Android project with all dependencies, version catalog, and build configuration.

  **Requirements:** R7, R36, R37

  **Dependencies:** None

  **Files:**
  - Create: `settings.gradle.kts`
  - Create: `build.gradle.kts` (root)
  - Create: `app/build.gradle.kts`
  - Create: `gradle/libs.versions.toml`
  - Create: `gradle.properties`
  - Create: `app/src/main/AndroidManifest.xml`
  - Create: `app/proguard-rules.pro`

  **Approach:**
  - Version catalog with Compose BOM, Navigation, CameraX, ZXing, DataStore, Lifecycle, kotlinx-serialization
  - Kotlin 2.0+ Compose compiler plugin (`org.jetbrains.kotlin.plugin.compose`)
  - minSdk 26, targetSdk 35, compileSdk 35
  - Manifest: single Activity, `enableOnBackInvokedCallback="true"`, permissions for CAMERA, RECORD_AUDIO, ACCESS_COARSE_LOCATION (all runtime-only), FOREGROUND_SERVICE, FOREGROUND_SERVICE_SPECIAL_USE, POST_NOTIFICATIONS, SCHEDULE_EXACT_ALARM (auto-granted for timer apps)
  - ProGuard rules for ZXing and kotlinx-serialization

  **Patterns to follow:**
  - Standard Android project layout with version catalogs
  - Compose BOM pattern for dependency management

  **Test scenarios:**
  - Happy path: Project builds successfully with `./gradlew assembleDebug`
  - Happy path: APK size is under 15 MB

  **Verification:**
  - Clean build succeeds with no warnings about missing dependencies

- [x] **Unit 2: Theme and app shell**

  **Goal:** Set up Material 3 theming with dynamic color, light/dark support, and the single-Activity entry point.

  **Requirements:** R4, R5, R8

  **Dependencies:** Unit 1

  **Files:**
  - Create: `app/src/main/java/com/toolbox/MainActivity.kt`
  - Create: `app/src/main/java/com/toolbox/ToolboxApp.kt`
  - Create: `app/src/main/java/com/toolbox/core/ui/theme/Color.kt`
  - Create: `app/src/main/java/com/toolbox/core/ui/theme/Type.kt`
  - Create: `app/src/main/java/com/toolbox/core/ui/theme/Theme.kt`
  - Create: `app/src/main/java/com/toolbox/core/persistence/UserPreferencesRepository.kt`
  - Test: `app/src/test/java/com/toolbox/core/ui/theme/ThemeTest.kt`

  **Approach:**
  - `Theme.kt`: Check `Build.VERSION.SDK_INT >= S` for dynamic color, fallback to generated static scheme
  - Theme preference stored in DataStore as enum (`System`, `Light`, `Dark`)
  - `MainActivity` reads theme preference and passes to `ToolboxTheme`
  - Generate static color scheme using Material Theme Builder — pick a teal/blue primary
  - `ToolboxApp` composable is the root — hosts `ToolboxTheme` → `SharedTransitionLayout` → `NavHost`

  **Patterns to follow:**
  - Standard Material 3 dynamic color pattern with `dynamicDarkColorScheme`/`dynamicLightColorScheme`

  **Test scenarios:**
  - Happy path: App launches with Material 3 theme applied
  - Edge case: On pre-Android 12 device, static fallback color scheme is used
  - Happy path: Theme preference (Light/Dark/System) persists across app restarts

  **Verification:**
  - App renders with correct theme colors on both Android 12+ and older devices

- [x] **Unit 3: Dashboard and navigation**

  **Goal:** Build the grid dashboard with categorized tool tiles, search bar, and navigation to tool screens.

  **Requirements:** R1, R2, R3, R35

  **Dependencies:** Unit 2

  **Files:**
  - Create: `app/src/main/java/com/toolbox/dashboard/ToolDefinition.kt`
  - Create: `app/src/main/java/com/toolbox/dashboard/DashboardScreen.kt`
  - Create: `app/src/main/java/com/toolbox/dashboard/DashboardViewModel.kt`
  - Create: `app/src/main/java/com/toolbox/dashboard/SearchBar.kt`
  - Create: `app/src/main/java/com/toolbox/nav/Destinations.kt`
  - Create: `app/src/main/java/com/toolbox/core/ui/components/ToolScaffold.kt`
  - Create: `app/src/main/java/com/toolbox/core/sensor/SensorAvailability.kt`
  - Test: `app/src/test/java/com/toolbox/dashboard/DashboardViewModelTest.kt`

  **Approach:**
  - `ToolDefinition`: sealed class or enum listing all 16 tools with name, icon resource, category, required sensor type (nullable), search keywords, and navigation destination
  - `DashboardScreen`: `LazyVerticalGrid` with category headers. Each tile shows icon + label. Disabled state for tools missing required sensor (check via `SensorManager.getDefaultSensor()` returning null)
  - `SearchBar`: Material 3 `SearchBar` composable. Filters `ToolDefinition` list by name and keywords
  - `Destinations.kt`: 16 `@Serializable` objects (one per tool) + `Dashboard` object
  - `ToolScaffold`: Shared wrapper for all tool screens — top app bar with title and back button
  - `SensorAvailability`: Checks sensor presence at app start, provides availability map to dashboard
  - Wire up `SharedTransitionLayout` → `NavHost` with placeholder screens for all 16 tools
  - Predictive back works automatically with `enableOnBackInvokedCallback="true"` + Navigation Compose 2.8+

  **Patterns to follow:**
  - Type-safe Navigation Compose with `composable<T>` pattern
  - Material 3 `LazyVerticalGrid` for dashboard layout

  **Test scenarios:**
  - Happy path: Dashboard shows all 16 tools organized by category
  - Happy path: Tapping a tool tile navigates to the tool screen
  - Happy path: Back navigation returns to dashboard
  - Happy path: Search filters tools by name (typing "comp" shows Compass)
  - Happy path: Search filters by keyword (typing "angle" shows Protractor)
  - Edge case: Search with no matches shows empty state
  - Edge case: Tool with missing sensor shows disabled state with explanation on tap
  - Happy path: Shared element transition animates tile icon to tool screen header

  **Verification:**
  - All 16 tiles visible on dashboard, each navigates to a placeholder tool screen and back

- [x] **Unit 4: Permission gate composable**

  **Goal:** Build a reusable permission request composable that wraps camera/mic/location tools.

  **Requirements:** R8, R9

  **Dependencies:** Unit 2

  **Files:**
  - Create: `app/src/main/java/com/toolbox/core/permission/PermissionGate.kt`
  - Test: `app/src/test/java/com/toolbox/core/permission/PermissionGateTest.kt`

  **Approach:**
  - Generic composable that takes a permission string (or list), rationale text, and content lambda
  - If permission granted: show content
  - If permission not yet requested: show rationale card with "Grant" button, triggers `rememberLauncherForActivityResult(RequestPermission)`
  - If permission permanently denied: show message with "Open Settings" button
  - Uses `ActivityResultContracts.RequestPermission` / `RequestMultiplePermissions`

  **Patterns to follow:**
  - Standard Accompanist-style permission handling, but built directly with Compose APIs (no Accompanist dependency needed since Compose 1.7+)

  **Test scenarios:**
  - Happy path: Content shown when permission already granted
  - Happy path: Permission rationale shown when permission not yet requested
  - Happy path: After granting permission, content appears
  - Edge case: Permanently denied permission shows "Open Settings" guidance
  - Edge case: Multiple permissions requested simultaneously (e.g., camera + location)

  **Verification:**
  - Permission gate correctly blocks tool content until permission is granted

### Phase 2: Zero-Permission Tools (no runtime permissions needed)

- [x] **Unit 5: Unit Converter**

  **Goal:** Build a full unit converter with 10 categories, bidirectional input, and favorite/recent persistence.

  **Requirements:** R21, R13

  **Dependencies:** Unit 3

  **Files:**
  - Create: `app/src/main/java/com/toolbox/conversion/unitconverter/UnitConverterScreen.kt`
  - Create: `app/src/main/java/com/toolbox/conversion/unitconverter/UnitConverterViewModel.kt`
  - Create: `app/src/main/java/com/toolbox/conversion/unitconverter/ConversionEngine.kt`
  - Create: `app/src/main/java/com/toolbox/conversion/unitconverter/UnitCategory.kt`
  - Test: `app/src/test/java/com/toolbox/conversion/unitconverter/ConversionEngineTest.kt`
  - Test: `app/src/test/java/com/toolbox/conversion/unitconverter/UnitConverterViewModelTest.kt`

  **Approach:**
  - `UnitCategory` enum: Length, Weight, Volume, Temperature, Speed, Area, Time, Data, Pressure, Energy
  - Each category has a list of units with conversion factors to a base unit. Temperature uses formula-based conversion (not factor-based)
  - `ConversionEngine`: Pure function — `convert(value, fromUnit, toUnit) -> Double`
  - Two input fields — typing in either field updates the other in real time
  - Category selector at top (horizontal scrolling chips or dropdown)
  - Favorite conversions stored in DataStore as serialized list, shown pinned at top
  - Recent conversions tracked (last 10), also in DataStore

  **Patterns to follow:**
  - Material 3 `FilterChip` for category selection
  - `OutlinedTextField` for input fields

  **Test scenarios:**
  - Happy path: Convert 1 km to 1000 m
  - Happy path: Convert 100°F to 37.78°C (temperature formula)
  - Happy path: Bidirectional — typing in "to" field updates "from" field
  - Happy path: Favorite conversion persists across app restart
  - Edge case: Empty input shows empty output (not 0)
  - Edge case: Very large numbers don't crash (e.g., 999999999999)
  - Edge case: Negative values handled correctly (temperature)

  **Verification:**
  - All 10 categories convert correctly. Favorites persist.

- [x] **Unit 6: Percentage, Tip, and Number Base calculators**

  **Goal:** Build 3 simple calculation tools.

  **Requirements:** R22, R23, R24

  **Dependencies:** Unit 3

  **Files:**
  - Create: `app/src/main/java/com/toolbox/conversion/percentage/PercentageScreen.kt`
  - Create: `app/src/main/java/com/toolbox/conversion/tipcalculator/TipCalculatorScreen.kt`
  - Create: `app/src/main/java/com/toolbox/conversion/numberbase/NumberBaseScreen.kt`
  - Test: `app/src/test/java/com/toolbox/conversion/percentage/PercentageCalculatorTest.kt`
  - Test: `app/src/test/java/com/toolbox/conversion/tipcalculator/TipCalculatorTest.kt`
  - Test: `app/src/test/java/com/toolbox/conversion/numberbase/NumberBaseConverterTest.kt`

  **Approach:**
  - **Percentage**: Tab/segmented button for 3 modes. Two input fields per mode, result displayed live
  - **Tip**: Bill amount input, preset tip buttons (10%, 15%, 18%, 20%, 25%, custom), split-by-N slider/stepper, per-person total displayed prominently
  - **Number Base**: Four fields (bin/oct/dec/hex) all visible simultaneously. Typing in any field updates the other three live. Input validation per base (e.g., only 0-1 for binary)

  **Test scenarios:**
  - Happy path: 15% of 200 = 30
  - Happy path: 50 is what % of 200 = 25%
  - Happy path: % change from 100 to 150 = 50%
  - Happy path: $100 bill, 18% tip, split 4 ways = $29.50/person
  - Happy path: Decimal 255 → hex FF, binary 11111111, octal 377
  - Edge case: Binary input rejects characters other than 0 and 1
  - Edge case: Tip with 0 bill shows $0
  - Edge case: Split by 1 person shows full total

  **Verification:**
  - All 3 calculators produce correct results with valid input and handle edge cases gracefully

- [x] **Unit 7: Counter / Tally**

  **Goal:** Build a counter with named counters, haptic feedback, and persistent state.

  **Requirements:** R27, R12, R34

  **Dependencies:** Unit 3

  **Files:**
  - Create: `app/src/main/java/com/toolbox/everyday/counter/CounterScreen.kt`
  - Create: `app/src/main/java/com/toolbox/everyday/counter/CounterViewModel.kt`
  - Test: `app/src/test/java/com/toolbox/everyday/counter/CounterViewModelTest.kt`

  **Approach:**
  - Large center tap target (fills most of screen) for increment
  - Smaller decrement button at bottom
  - Current count displayed large and centered
  - "Add Counter" button to create new named counter, horizontal pager or tab row to switch
  - Reset button with confirmation dialog
  - Haptic feedback via `HapticFeedbackType.LongPress` on Compose or `Vibrator` API
  - Counter values serialized to DataStore as JSON map (`Map<String, Int>`)

  **Test scenarios:**
  - Happy path: Tap increments counter by 1
  - Happy path: Decrement reduces counter by 1
  - Happy path: Counter value persists after app restart
  - Happy path: Create a new named counter, switch between counters
  - Edge case: Decrement at 0 stays at 0 (or allows negative — decide during implementation)
  - Edge case: Reset shows confirmation, canceling does not reset
  - Integration: Haptic feedback fires on tap

  **Verification:**
  - Multiple named counters work, values persist, haptics trigger

- [x] **Unit 8: Stopwatch & Timer**

  **Goal:** Build stopwatch with laps and timer with foreground service for background operation.

  **Requirements:** R28, R11, R14, R34

  **Dependencies:** Unit 3, Unit 4

  **Files:**
  - Create: `app/src/main/java/com/toolbox/everyday/stopwatch/StopwatchTimerScreen.kt`
  - Create: `app/src/main/java/com/toolbox/everyday/stopwatch/StopwatchViewModel.kt`
  - Create: `app/src/main/java/com/toolbox/everyday/stopwatch/TimerService.kt`
  - Create: `app/src/main/java/com/toolbox/everyday/stopwatch/TimerAlarmReceiver.kt`
  - Test: `app/src/test/java/com/toolbox/everyday/stopwatch/StopwatchViewModelTest.kt`

  **Approach:**
  - Two tabs: Stopwatch / Timer
  - **Stopwatch**: Start/pause/reset buttons. Lap button adds current split to scrollable list. Uses `System.nanoTime()` differences in a coroutine, recomposes every ~16ms for smooth display. Resets on process death (acceptable per R14)
  - **Timer**: Preset duration buttons (1m, 3m, 5m, 10m) + custom time picker input. Start binds to `TimerService` foreground service
  - **TimerService**: Hybrid approach — on start, schedules an `AlarmManager.setExactAndAllowWhileIdle()` alarm for the completion time (guarantees notification fires even if service is killed or device is in Doze). Also starts foreground service with `specialUse` type for live countdown notification display. `StateFlow<Long>` for remaining seconds. On completion: fires notification with sound/vibration, cancels alarm if still pending. Requests POST_NOTIFICATIONS permission (via PermissionGate from Unit 4) before first start on Android 13+. Requires `SCHEDULE_EXACT_ALARM` permission (auto-granted for timer apps)
  - Create a `TimerAlarmReceiver` (BroadcastReceiver) that fires the completion notification if the AlarmManager alarm triggers before the service completes
  - Service binds/unbinds via `DisposableEffect` + `ServiceConnection` composable

  **Patterns to follow:**
  - Foreground service with `ServiceCompat.startForeground()` and `FOREGROUND_SERVICE_TYPE_SPECIAL_USE`

  **Test scenarios:**
  - Happy path: Stopwatch starts, pauses, resumes, resets
  - Happy path: Lap button records split times
  - Happy path: Timer counts down and fires notification on completion
  - Happy path: Timer continues running when app is backgrounded
  - Happy path: POST_NOTIFICATIONS permission requested on first timer start (Android 13+)
  - Edge case: Starting a new timer while one is running replaces the previous one
  - Edge case: Timer notification shows correct remaining time
  - Integration: TimerService survives process death, completion notification fires

  **Verification:**
  - Timer completes and notifies even when app is in background

- [x] **Unit 9: Random / Coin Flip / Dice**

  **Goal:** Build random number generator, coin flip, and dice roll with animations.

  **Requirements:** R29, R34

  **Dependencies:** Unit 3

  **Files:**
  - Create: `app/src/main/java/com/toolbox/everyday/random/RandomScreen.kt`
  - Create: `app/src/main/java/com/toolbox/everyday/random/RandomViewModel.kt`
  - Test: `app/src/test/java/com/toolbox/everyday/random/RandomViewModelTest.kt`

  **Approach:**
  - Three modes via tabs or segmented button: Random Number, Coin Flip, Dice
  - **Random Number**: Min/max input fields, "Generate" button, large result display
  - **Coin Flip**: Tap to flip, rotation animation on a coin graphic (heads/tails), haptic on land
  - **Dice**: Select 1-6 dice, tap to roll, each die shows rolling animation then settles, total displayed. Haptic on roll
  - Animations: Compose `Animatable` for coin rotation, `animateFloatAsState` for dice tumble
  - Use `kotlin.random.Random` for generation

  **Test scenarios:**
  - Happy path: Random number falls within specified min-max range
  - Happy path: Coin flip shows heads or tails with animation
  - Happy path: Dice roll shows valid values (1-6) for each die
  - Edge case: Min > max in random number shows error or swaps values
  - Edge case: 6 dice rolled simultaneously all show valid results and total is correct

  **Verification:**
  - All three modes produce valid random results with smooth animations

- [x] **Unit 10: On-Screen Ruler**

  **Goal:** Build an on-screen ruler with credit card calibration.

  **Requirements:** R20

  **Dependencies:** Unit 3

  **Files:**
  - Create: `app/src/main/java/com/toolbox/measurement/ruler/RulerScreen.kt`
  - Create: `app/src/main/java/com/toolbox/measurement/ruler/RulerViewModel.kt`
  - Test: `app/src/test/java/com/toolbox/measurement/ruler/RulerViewModelTest.kt`

  **Approach:**
  - Full-screen ruler drawn with Compose `Canvas` — tick marks at mm intervals, labels at cm and inch marks
  - Dual scale: cm on one edge, inches on the other
  - Default DPI from `DisplayMetrics.xdpi` / `ydpi`
  - Calibration mode: show a rectangle outline matching standard credit card dimensions (85.6mm × 53.98mm). User drags the edges to match their physical card. App computes actual DPI from the ratio of expected vs displayed size
  - Calibration factor stored in DataStore
  - Ruler is scrollable/draggable for measuring objects longer than the screen

  **Test scenarios:**
  - Happy path: Ruler displays correct mm/cm/inch markings at default DPI
  - Happy path: Calibration adjusts ruler accuracy
  - Happy path: Calibration persists across app restart
  - Edge case: Landscape orientation adjusts ruler to use longer axis

  **Verification:**
  - Ruler measures a known object (credit card) correctly after calibration

### Phase 3: Sensor Tools (runtime permissions for sensors)

- [x] **Unit 11: Sensor composable hooks**

  **Goal:** Build reusable composable hooks for accelerometer, magnetometer, and microphone data.

  **Requirements:** R16, R17, R18, R19

  **Dependencies:** Unit 2

  **Files:**
  - Create: `app/src/main/java/com/toolbox/core/sensor/AccelerometerHook.kt`
  - Create: `app/src/main/java/com/toolbox/core/sensor/MagnetometerHook.kt`
  - Create: `app/src/main/java/com/toolbox/core/sensor/OrientationHook.kt`
  - Create: `app/src/main/java/com/toolbox/core/sensor/AudioRecordHook.kt`
  - Create: `app/src/main/java/com/toolbox/core/sensor/LowPassFilter.kt`
  - Modify: `app/src/main/java/com/toolbox/core/sensor/SensorAvailability.kt` (created in Unit 3 — enrich with sensor-specific utilities but do not recreate)
  - Test: `app/src/test/java/com/toolbox/core/sensor/LowPassFilterTest.kt`

  **Approach:**
  - `rememberAccelerometerData()`: Returns `State<FloatArray>` (x, y, z). Registers/unregisters with `LifecycleEventObserver` on RESUME/PAUSE. `SENSOR_DELAY_UI`
  - `rememberMagnetometerData()`: Same pattern for `TYPE_MAGNETIC_FIELD`
  - `rememberOrientationData()`: Combines accelerometer + magnetometer via `SensorManager.getRotationMatrix()` + `getOrientation()`. Returns azimuth, pitch, roll. Low-pass filtered
  - `rememberAudioLevel()`: Uses `AudioRecord` with `ENCODING_PCM_16BIT`, calculates RMS dB from PCM buffer on a coroutine. Returns `State<Float>` (dB value). CRITICAL: must use `DisposableEffect` with `AudioRecord.release()` in `onDispose` — `LaunchedEffect` cancellation alone does not release native microphone resources. Read loop runs inside a coroutine launched within the `DisposableEffect`
  - `LowPassFilter`: Pure function `lowPass(input, output, alpha)` for smoothing sensor noise

  **Patterns to follow:**
  - `DisposableEffect` + `LifecycleEventObserver` pattern for hardware sensor lifecycle
  - `DisposableEffect` with explicit `release()` in `onDispose` for `AudioRecord` (different from sensor pattern)

  **Test scenarios:**
  - Happy path: Low-pass filter smooths noisy input (verify with known input sequence)
  - Edge case: Sensor hook handles null sensor gracefully (device without magnetometer)
  - Edge case: AudioRecord hook handles RECORD_AUDIO permission denial (returns 0 dB)

  **Verification:**
  - Sensor hooks produce stable, lifecycle-aware data streams consumed by tool screens

- [x] **Unit 12: Bubble Level**

  **Goal:** Build a bubble level with circular and linear modes.

  **Requirements:** R16

  **Dependencies:** Unit 11, Unit 4

  **Files:**
  - Create: `app/src/main/java/com/toolbox/measurement/level/LevelScreen.kt`
  - Create: `app/src/main/java/com/toolbox/measurement/level/LevelViewModel.kt`
  - Test: `app/src/test/java/com/toolbox/measurement/level/LevelViewModelTest.kt`

  **Approach:**
  - Uses `rememberAccelerometerData()` from Unit 11
  - **Circular level**: Full-screen circle with a bubble that moves opposite to tilt. Center crosshair indicates level. Green color when within ±0.5° of level
  - **Linear level**: Horizontal bar level for single-axis measurement
  - Degree display showing pitch and roll values
  - Lock/hold button: freezes the current reading for reference
  - Canvas-based drawing for bubble and level indicators
  - Low-pass filter applied to sensor data for smooth bubble movement

  **Test scenarios:**
  - Happy path: Bubble moves in response to tilt
  - Happy path: Degree values update in real time
  - Happy path: Lock/hold freezes the reading
  - Happy path: Green indicator when surface is level (within tolerance)
  - Edge case: Device flat on table shows 0°/0°

  **Verification:**
  - Level reads 0° on a known flat surface and responds smoothly to tilt

- [x] **Unit 13: Compass**

  **Goal:** Build a compass with magnetic/true north toggle.

  **Requirements:** R17

  **Dependencies:** Unit 11, Unit 4

  **Files:**
  - Create: `app/src/main/java/com/toolbox/measurement/compass/CompassScreen.kt`
  - Create: `app/src/main/java/com/toolbox/measurement/compass/CompassViewModel.kt`
  - Test: `app/src/test/java/com/toolbox/measurement/compass/CompassViewModelTest.kt`

  **Approach:**
  - Uses `rememberOrientationData()` from Unit 11
  - Rotating compass rose drawn with Compose Canvas, cardinal directions (N/S/E/W), degree markings
  - Bearing displayed as degrees (0-360°) and cardinal direction text (e.g., "NNE 22°")
  - Toggle switch for magnetic vs true north. True north toggle triggers `ACCESS_COARSE_LOCATION` permission (via PermissionGate). Uses `GeomagneticField` class with lat/lng/altitude to compute declination
  - Smooth rotation animation with low-pass filter to avoid jitter

  **Test scenarios:**
  - Happy path: Compass rose rotates smoothly as device rotates
  - Happy path: Bearing displays correct cardinal direction
  - Happy path: Magnetic north shown by default, no permission requested
  - Happy path: True north toggle requests location permission, adjusts bearing by declination
  - Edge case: Location permission denied — true north toggle disabled with explanation
  - Edge case: Magnetic interference warning when sensor accuracy is low

  **Verification:**
  - Compass points north consistently and switches between magnetic/true north

- [x] **Unit 14: Protractor and Sound Meter**

  **Goal:** Build accelerometer-based protractor and microphone-based sound meter.

  **Requirements:** R18, R19

  **Dependencies:** Unit 11, Unit 4

  **Files:**
  - Create: `app/src/main/java/com/toolbox/measurement/protractor/ProtractorScreen.kt`
  - Create: `app/src/main/java/com/toolbox/measurement/soundmeter/SoundMeterScreen.kt`
  - Create: `app/src/main/java/com/toolbox/measurement/soundmeter/SoundMeterViewModel.kt`
  - Test: `app/src/test/java/com/toolbox/measurement/soundmeter/SoundMeterViewModelTest.kt`

  **Approach:**
  - **Protractor**: Uses `rememberAccelerometerData()`. Shows angle of inclination (0-360°) with a visual arc gauge drawn on Canvas. Lock/hold button for freezing reading. Simple single-axis mode — phone flat = 0°, phone upright = 90°
  - **Sound Meter**: Uses `rememberAudioLevel()`. Wrapped in PermissionGate for RECORD_AUDIO. Visual gauge (arc or bar) showing current dB level. Color-coded zones (green < 70dB, yellow 70-85dB, red > 85dB). Min/max/avg displayed for current session. Reset button clears session stats. Disclaimer text: "Readings are approximate and not calibrated for professional use"

  **Test scenarios:**
  - Happy path: Protractor shows 0° when phone is flat, ~90° when upright
  - Happy path: Sound meter shows ambient dB level with visual gauge
  - Happy path: Min/max/avg update during session
  - Happy path: Sound meter reset clears session statistics
  - Edge case: Sound meter without mic permission shows PermissionGate rationale
  - Edge case: Very quiet environment shows low dB reading (not 0 or negative)

  **Verification:**
  - Protractor reads correct angles. Sound meter responds to ambient noise levels.

### Phase 4: Camera Tools (camera permission required)

- [x] **Unit 15: Shared camera composable**

  **Goal:** Build a reusable CameraX preview composable with image analysis support.

  **Requirements:** R26, R30, R31

  **Dependencies:** Unit 4

  **Files:**
  - Create: `app/src/main/java/com/toolbox/core/camera/CameraPreview.kt`
  - Create: `app/src/main/java/com/toolbox/core/camera/YuvToRgb.kt`

  **Approach:**
  - Use `androidx.camera:camera-compose` with `CameraXViewfinder` composable
  - Composable parameters: `modifier`, `onFrameAnalyzed: ((ImageProxy) -> Unit)?`, `zoomRatio: Float`, `enableTorch: Boolean`
  - Binds Preview + optional ImageAnalysis use cases to lifecycle
  - `ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST` backpressure
  - `YuvToRgb.kt`: Utility to convert YUV_420_888 plane data to RGB values for Color Picker sampling
  - Wrapped in PermissionGate for CAMERA permission

  **Patterns to follow:**
  - CameraX lifecycle binding pattern with `ProcessCameraProvider`

  **Test scenarios:**
  - Happy path: Camera preview renders when CAMERA permission is granted
  - Happy path: Image analysis callback receives frames
  - Happy path: Torch toggle enables/disables flash LED
  - Happy path: Zoom ratio adjusts camera zoom
  - Edge case: Camera permission denied shows PermissionGate rationale

  **Verification:**
  - Camera preview composable works across 3 tool screens (QR, Color Picker, Magnifier)

- [x] **Unit 16: QR / Barcode Scanner + Generator**

  **Goal:** Build QR/barcode scanner with ZXing and a QR/barcode generator.

  **Requirements:** R26

  **Dependencies:** Unit 15

  **Files:**
  - Create: `app/src/main/java/com/toolbox/everyday/qrscanner/QrScannerScreen.kt`
  - Create: `app/src/main/java/com/toolbox/everyday/qrscanner/QrScannerViewModel.kt`
  - Create: `app/src/main/java/com/toolbox/everyday/qrscanner/QrAnalyzer.kt`
  - Create: `app/src/main/java/com/toolbox/everyday/qrscanner/QrGeneratorScreen.kt`
  - Test: `app/src/test/java/com/toolbox/everyday/qrscanner/QrAnalyzerTest.kt`

  **Approach:**
  - Two tabs: Scan / Generate
  - **Scanner**: `CameraPreview` with `QrAnalyzer` as the frame analyzer. `QrAnalyzer` implements `ImageAnalysis.Analyzer`, uses `MultiFormatReader` with hints for QR_CODE, UPC_A, UPC_E, EAN_13, EAN_8, CODE_128. On decode success, shows result card with action buttons: Open URL (if URL), Copy Text, Share. Vibration on successful scan
  - **Generator**: Text/URL input field, format selector (QR Code, Code 128, EAN-13), generate button. Uses `MultiFormatWriter` + `BinaryBitmap` to create barcode. Renders as `Image` composable. Share/save button exports as PNG to gallery using `MediaStore`
  - `QrAnalyzer` must close `ImageProxy` in finally block to prevent pipeline stall

  **Patterns to follow:**
  - ZXing `PlanarYUVLuminanceSource` from `ImageProxy` planes[0] buffer

  **Test scenarios:**
  - Happy path: Scanner detects QR code and shows decoded text
  - Happy path: Scanner detects UPC barcode and shows number
  - Happy path: "Copy" button copies scanned text to clipboard
  - Happy path: Generator creates valid QR code image from text input
  - Happy path: Generated QR code can be saved to gallery
  - Edge case: No barcode in frame — scanner continues without error
  - Edge case: Generator with empty input shows validation message

  **Verification:**
  - Scanner decodes at least QR and EAN-13 barcodes. Generator produces scannable QR codes.

- [x] **Unit 17: Color Picker**

  **Goal:** Build a camera-based color picker that displays hex/RGB/HSL values.

  **Requirements:** R30

  **Dependencies:** Unit 15

  **Files:**
  - Create: `app/src/main/java/com/toolbox/everyday/colorpicker/ColorPickerScreen.kt`
  - Create: `app/src/main/java/com/toolbox/everyday/colorpicker/ColorPickerViewModel.kt`
  - Create: `app/src/main/java/com/toolbox/everyday/colorpicker/ColorSampler.kt`
  - Test: `app/src/test/java/com/toolbox/everyday/colorpicker/ColorSamplerTest.kt`

  **Approach:**
  - Full-screen camera preview with crosshair overlay at center
  - `ColorSampler`: ImageAnalysis analyzer that samples an 11x11 pixel region around the center of each frame. Converts YUV to RGB using `YuvToRgb` from Unit 15. Averages the pixel values
  - Below the preview: color swatch showing sampled color, hex value, RGB values, HSL values
  - Tap to freeze/capture — pauses image analysis, keeps last color displayed
  - "Copy" buttons next to each color format — copies to clipboard
  - RGB to HSL conversion utility

  **Test scenarios:**
  - Happy path: Color values update in real time as camera moves
  - Happy path: Tap freezes the color reading
  - Happy path: Copy button copies hex value to clipboard
  - Happy path: Hex, RGB, and HSL values are consistent for the same color
  - Edge case: Very dark scene shows near-black color values
  - Edge case: Very bright scene doesn't overflow RGB values

  **Verification:**
  - Color picker shows plausible color values that match the visible scene

- [x] **Unit 18: Magnifier**

  **Goal:** Build a camera-based magnifier with zoom and torch.

  **Requirements:** R31

  **Dependencies:** Unit 15

  **Files:**
  - Create: `app/src/main/java/com/toolbox/everyday/magnifier/MagnifierScreen.kt`
  - Create: `app/src/main/java/com/toolbox/everyday/magnifier/MagnifierViewModel.kt`

  **Approach:**
  - Full-screen camera preview using `CameraPreview` with zoomRatio parameter
  - Zoom control: pinch gesture (Compose `detectTransformGestures`) and a slider at bottom
  - Zoom range: 1x to max camera zoom (query from `CameraInfo.zoomState`)
  - Torch toggle button — uses `enableTorch` parameter on shared camera composable
  - Freeze frame button — captures current preview frame and displays as static `Image`

  **Test scenarios:**
  - Happy path: Pinch gesture zooms in/out
  - Happy path: Slider adjusts zoom level
  - Happy path: Torch toggle enables/disables flash
  - Happy path: Freeze frame captures and displays static image
  - Edge case: Zoom doesn't exceed device maximum

  **Verification:**
  - Magnifier zooms smoothly and torch toggles on/off

### Phase 5: Flashlight and Polish

- [x] **Unit 19: Flashlight**

  **Goal:** Build flashlight with SOS and strobe modes.

  **Requirements:** R25

  **Dependencies:** Unit 3

  **Files:**
  - Create: `app/src/main/java/com/toolbox/lighting/flashlight/FlashlightScreen.kt`
  - Create: `app/src/main/java/com/toolbox/lighting/flashlight/FlashlightViewModel.kt`
  - Test: `app/src/test/java/com/toolbox/lighting/flashlight/FlashlightViewModelTest.kt`

  **Approach:**
  - Large toggle button for on/off using `CameraManager.setTorchMode()`
  - Mode selector: Steady / SOS / Strobe
  - **SOS mode**: Blinks the Morse code pattern for SOS (· · · — — — · · ·) on repeat. Pattern timing on background coroutine
  - **Strobe mode**: Frequency slider (1-20 Hz). Minimum 50ms interval enforced. Toggle runs on `Dispatchers.Default` coroutine to avoid main thread ANR
  - **Brightness slider**: Visible only on Android 13+ devices where `CameraManager` supports torch strength. Check `CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL`. Hidden (not disabled) when unsupported
  - Uses `CameraManager` directly (not CameraX — flashlight doesn't need preview)

  **Test scenarios:**
  - Happy path: Toggle turns flash on/off
  - Happy path: SOS mode blinks correct Morse pattern
  - Happy path: Strobe mode blinks at specified frequency
  - Happy path: Brightness slider adjusts torch intensity on supported devices
  - Edge case: Brightness slider hidden on unsupported devices
  - Edge case: Strobe frequency capped at minimum 50ms interval
  - Edge case: Flash turns off when leaving the tool screen

  **Verification:**
  - Flash toggles reliably. SOS and strobe patterns are correct on physical device.

- [x] **Unit 20: Polish and Play Store prep**

  **Goal:** Final polish, transitions, testing, and Play Store listing preparation.

  **Requirements:** R34, R35, R37

  **Dependencies:** Units 1-19

  **Files:**
  - Modify: `app/src/main/java/com/toolbox/ToolboxApp.kt` (transition animations)
  - Create: `app/src/main/res/mipmap-*/ic_launcher.webp` (app icon)
  - Create: `app/src/main/res/values/strings.xml` (all user-facing strings)
  - Modify: `app/src/main/AndroidManifest.xml` (final permissions audit)

  **Approach:**
  - **Shared element transitions**: Wire up `SharedTransitionLayout` → `Modifier.sharedElement()` on dashboard tile icons → tool screen header icons. Tune animation specs
  - **Haptic feedback audit**: Verify haptics work on counter, coin flip, dice roll, QR scan success
  - **Permissions audit**: Verify manifest declares only CAMERA, RECORD_AUDIO, ACCESS_COARSE_LOCATION (dangerous, runtime-only), FOREGROUND_SERVICE, FOREGROUND_SERVICE_SPECIAL_USE, POST_NOTIFICATIONS. No INTERNET permission. Verify Play Store listing preview shows no dangerous install-time permissions
  - **APK size check**: Run `./gradlew assembleRelease`, verify < 15 MB
  - **App icon**: Design or generate a toolbox-themed adaptive icon
  - **Play Store listing**: App title, short description, full description, screenshots (phone + tablet), feature graphic, privacy policy (no data collected)
  - **String extraction**: Move all hardcoded strings to `strings.xml` for future localization

  **Test scenarios:**
  - Happy path: All 16 tools accessible and functional end-to-end
  - Happy path: Shared element transitions animate smoothly between dashboard and tools
  - Happy path: APK < 15 MB
  - Happy path: No dangerous permissions shown on Play Store listing at install
  - Edge case: App works on Android 8.0 (minSdk 26) device without crashes
  - Edge case: Dark mode and light mode render correctly across all tools
  - Integration: Material You dynamic colors apply to all screens on Android 12+

  **Verification:**
  - App passes Play Store pre-launch report. All 16 tools tested on 2+ physical devices.

### Phase 6: New Tools & Enhancements (Post-MVP)

- [x] **Unit 21: Unit Converter — Comprehensive expansion**

  **Goal:** Expand from 10 categories to 17+ categories, add missing units within existing categories.

  **Requirements:** Full unit coverage across all common measurement domains

  **Dependencies:** Unit 5

  **Files:**
  - Modify: `app/src/main/java/com/toolbox/conversion/unitconverter/UnitCategory.kt`

  **Approach:**
  - Add new categories: Power (W, kW, HP, BTU/hr), Force (N, lbf, dyne, kgf), Torque (N·m, ft·lb, in·lb), Density (kg/m³, g/cm³, lb/ft³, lb/gal), Fuel Economy (mpg, km/L, L/100km — inverse formula-based), Angle (degrees, radians, gradians, arcminutes, arcseconds), Frequency (Hz, kHz, MHz, GHz, RPM)
  - Add missing units within existing categories: Volume → Imperial Gallon, Imperial Pint, Cubic Meter, Cubic Foot, Cubic Inch; Weight → Stone, US Ton (short ton), Imperial Ton (long ton), Carat; Length → Nautical Mile, Micrometer; Speed → ft/s, cm/s
  - Fuel Economy uses inverse relationship (L/100km ↔ mpg) — needs formula-based conversion like Temperature

  **Test scenarios:**
  - Happy path: All new categories appear in category selector
  - Happy path: Fuel Economy handles inverse conversion (L/100km)
  - Happy path: Force, Torque, Density convert correctly

  **Verification:**
  - 17+ categories all converting correctly. All existing conversions unchanged.

- [x] **Unit 22: Vibrometer (Surface Vibration Analyzer)**

  **Goal:** Use accelerometer at high sample rate to detect and visualize surface vibrations.

  **Requirements:** Extend sensor capabilities for diagnostics use case

  **Dependencies:** Unit 11 (sensor hooks)

  **Files:**
  - Create: `app/src/main/java/com/toolbox/measurement/vibrometer/VibrometerScreen.kt`
  - Modify: `app/src/main/java/com/toolbox/dashboard/ToolDefinition.kt` (add tool entry)
  - Modify: `app/src/main/java/com/toolbox/nav/Destinations.kt` (add route)
  - Modify: `app/src/main/java/com/toolbox/ToolboxApp.kt` (wire nav)

  **Approach:**
  - Register accelerometer with `SENSOR_DELAY_FASTEST` for higher sample rate
  - Display live waveform (rolling Canvas line chart) showing vibration magnitude over time
  - Compute dominant frequency via simple zero-crossing detection
  - Show magnitude (peak, RMS) and approximate frequency
  - "Record Baseline" button to capture a reference, then "Compare" mode highlights deviations
  - No new permissions needed — accelerometer is zero-permission

  **Test scenarios:**
  - Happy path: Live waveform responds to phone vibration/tapping
  - Happy path: Baseline recording and comparison works
  - Edge case: Stationary phone shows near-zero readings

  **Verification:**
  - Waveform renders smoothly. Frequency estimate is reasonable on vibrating surface.

- [x] **Unit 23: Mirror (True Mirror)**

  **Goal:** Full-screen front camera feed as a mirror with standard/true mirror toggle.

  **Requirements:** Simple utility leveraging existing CameraX infrastructure

  **Dependencies:** Unit 15 (shared CameraX composable)

  **Files:**
  - Create: `app/src/main/java/com/toolbox/everyday/mirror/MirrorScreen.kt`
  - Modify: `app/src/main/java/com/toolbox/dashboard/ToolDefinition.kt`
  - Modify: `app/src/main/java/com/toolbox/nav/Destinations.kt`
  - Modify: `app/src/main/java/com/toolbox/ToolboxApp.kt`

  **Approach:**
  - Use `CameraPreview` with `CameraSelector.DEFAULT_FRONT_CAMERA`
  - Default: standard mirror (Android default — flipped horizontally)
  - "True Mirror" toggle: apply `Modifier.graphicsLayer { scaleX = -1f }` to flip the preview to show how others see you
  - Brightness/contrast boost slider (adjust PreviewView overlay brightness)
  - Freeze-frame button to capture current view
  - Full-screen mode hiding top bar for maximum mirror area

  **Test scenarios:**
  - Happy path: Front camera displays as mirror
  - Happy path: True mirror toggle flips the image
  - Happy path: Freeze frame captures current view
  - Edge case: Device without front camera shows error state

  **Verification:**
  - Mirror displays correctly. True mirror flip is visually correct.

- [x] **Unit 24: Ambient Light Meter (Lux Logger)**

  **Goal:** Display and log ambient light sensor readings with rolling chart and threshold alerts.

  **Requirements:** Zero-permission tool using ambient light sensor

  **Dependencies:** Unit 3

  **Files:**
  - Create: `app/src/main/java/com/toolbox/core/sensor/LightSensorHook.kt`
  - Create: `app/src/main/java/com/toolbox/measurement/lightmeter/LightMeterScreen.kt`
  - Modify: `app/src/main/java/com/toolbox/dashboard/ToolDefinition.kt`
  - Modify: `app/src/main/java/com/toolbox/nav/Destinations.kt`
  - Modify: `app/src/main/java/com/toolbox/ToolboxApp.kt`

  **Approach:**
  - New sensor hook: `rememberLightSensorData()` using `Sensor.TYPE_LIGHT`, same DisposableEffect + LifecycleEventObserver pattern as accelerometer
  - Display current lux value prominently with descriptive label (Dark < 10, Dim < 50, Indoor 50-500, Bright 500-10000, Sunlight > 10000)
  - Rolling line chart (Canvas) showing lux over last 5 minutes
  - Named presets with threshold references: Reading (300+ lux), Office (500+ lux), Grow Light (2000+ lux)
  - Min/Avg/Max stats with reset
  - Zero permissions required — ambient light sensor is not a dangerous permission

  **Test scenarios:**
  - Happy path: Current lux displayed and updates with ambient light changes
  - Happy path: Rolling chart shows historical readings
  - Edge case: Device without light sensor shows error state

  **Verification:**
  - Lux readings respond to covering/uncovering the sensor. Chart scrolls.

- [x] **Unit 25: Magnetic Field Detector**

  **Goal:** 3-axis magnetic field strength display with metal detection mode.

  **Requirements:** From Phase 2 backlog — Metal Detector

  **Dependencies:** Unit 11 (magnetometer hook)

  **Files:**
  - Create: `app/src/main/java/com/toolbox/measurement/metaldetector/MetalDetectorScreen.kt`
  - Modify: `app/src/main/java/com/toolbox/dashboard/ToolDefinition.kt`
  - Modify: `app/src/main/java/com/toolbox/nav/Destinations.kt`
  - Modify: `app/src/main/java/com/toolbox/ToolboxApp.kt`

  **Approach:**
  - Use existing `rememberMagnetometerData()` hook for 3-axis field strength (µT)
  - Display total field magnitude (√(x²+y²+z²)) as primary reading
  - Arc gauge similar to Sound Meter design — green (normal ~25-65µT) → orange → red (anomaly)
  - Audio pitch feedback: tone frequency proportional to field magnitude using `AudioTrack` or `ToneGenerator`
  - "Calibrate" button to set current reading as baseline; subsequent readings show deviation from baseline
  - Individual X/Y/Z axis bars for directional analysis
  - No new permissions — magnetometer is zero-permission

  **Test scenarios:**
  - Happy path: Magnitude increases near metal objects
  - Happy path: Audio pitch rises near ferrous material
  - Happy path: Calibration sets baseline correctly
  - Edge case: Device without magnetometer shows error state

  **Verification:**
  - Detects metal objects (keys, screws) at close range. Audio feedback follows magnitude.

- [x] **Unit 26: Shareable Measurement Cards**

  **Goal:** Export any tool reading as a branded visual card (PNG) for sharing.

  **Requirements:** Cross-tool engagement feature

  **Dependencies:** Units 1-25

  **Files:**
  - Create: `app/src/main/java/com/toolbox/core/sharing/MeasurementCard.kt`
  - Modify: Various tool screens to add share button

  **Approach:**
  - Composable that renders a card with: tool icon, reading value, unit, timestamp, optional label, subtle "Toolbox" branding
  - Use `Canvas` or `Picture` to render card to `Bitmap`, save as PNG to cache dir
  - Share via `FileProvider` + `Intent.ACTION_SEND` with image/png type
  - Add a share icon button to ToolScaffold that tools can opt into by providing current reading data
  - Card design: Material 3 card with tool-specific accent color, clean typography

  **Test scenarios:**
  - Happy path: Share button generates card PNG and opens share sheet
  - Happy path: Card shows correct reading, tool name, timestamp
  - Edge case: Card renders correctly in both light and dark theme

  **Verification:**
  - Shared card image is visually clean and contains accurate data.

- [x] **Unit 27: Quick Settings Tiles**

  **Goal:** Register Android Quick Settings tiles for top tools (Flashlight, Sound Meter, Level, Timer).

  **Requirements:** Zero-friction access to frequently-used tools

  **Dependencies:** Units 12, 13, 14, 19

  **Files:**
  - Create: `app/src/main/java/com/toolbox/tiles/FlashlightTileService.kt`
  - Create: `app/src/main/java/com/toolbox/tiles/ToolLaunchTileService.kt`
  - Modify: `app/src/main/AndroidManifest.xml` (register tile services)

  **Approach:**
  - `FlashlightTileService` extends `TileService`: directly toggles torch via `CameraManager.setTorchMode()` without launching the app. Updates tile icon state (on/off)
  - `ToolLaunchTileService` (generic): parameterized tile that deep-links to a specific tool via Intent. Register separate service entries for Sound Meter, Level, Timer
  - Manifest: declare `<service>` entries with `android.service.quicksettings.action.QS_TILE` intent filter and appropriate labels/icons
  - Users manually add tiles from Quick Settings edit mode

  **Test scenarios:**
  - Happy path: Flashlight tile toggles torch without opening the app
  - Happy path: Sound Meter tile launches directly into Sound Meter screen
  - Edge case: Tiles work from lock screen (Flashlight should, others launch to permission gate)

  **Verification:**
  - Tiles appear in Quick Settings edit panel. Flashlight toggles instantly. Other tiles deep-link correctly.

### Phase 7: Pressure & Environment Sensors

- [x] **Unit 28: Barometer / Altimeter**

  **Goal:** Display atmospheric pressure and estimate relative altitude changes using the pressure sensor.

  **Requirements:** Zero-permission tool using `TYPE_PRESSURE` sensor

  **Dependencies:** Unit 11 (sensor hooks pattern)

  **Files:**
  - Create: `app/src/main/java/com/toolbox/core/sensor/PressureSensorHook.kt`
  - Create: `app/src/main/java/com/toolbox/measurement/barometer/BarometerScreen.kt`
  - Modify: `app/src/main/java/com/toolbox/dashboard/ToolDefinition.kt`
  - Modify: `app/src/main/java/com/toolbox/nav/Destinations.kt`
  - Modify: `app/src/main/java/com/toolbox/ToolboxApp.kt`

  **Approach:**
  - New sensor hook: `rememberPressureData()` using `Sensor.TYPE_PRESSURE`, same lifecycle pattern as existing hooks
  - Display current pressure in hPa prominently with weather tendency indicator (rising/steady/falling based on trend over last 30 min)
  - Altitude estimation using barometric formula: `SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, currentPressure)` for relative altitude
  - "Set Reference" button to zero the altitude at current position, then show relative altitude change (useful for hiking, stair counting)
  - Rolling line chart showing pressure over last 60 minutes
  - Unit toggle: hPa / mbar / inHg / mmHg
  - Weather reference labels: Low (<1000 hPa), Normal (1013 hPa), High (>1025 hPa)
  - Zero permissions required — pressure sensor is not a dangerous permission

  **Test scenarios:**
  - Happy path: Current pressure displayed and updates
  - Happy path: Altitude changes when moving between floors
  - Happy path: Unit toggle switches between hPa/inHg/mmHg
  - Happy path: Reference point zeroes altitude correctly
  - Edge case: Device without pressure sensor shows disabled tile on dashboard

  **Verification:**
  - Pressure readings are reasonable (~950-1050 hPa at sea level). Altitude changes detectably between floors (~3m per floor).

- [x] **Unit 29: Humidity Meter**

  **Goal:** Display relative humidity reading from the humidity sensor (available on select devices).

  **Requirements:** Zero-permission tool using `TYPE_RELATIVE_HUMIDITY` sensor

  **Dependencies:** Unit 11 (sensor hooks pattern)

  **Files:**
  - Create: `app/src/main/java/com/toolbox/core/sensor/HumiditySensorHook.kt`
  - Create: `app/src/main/java/com/toolbox/measurement/humidity/HumidityScreen.kt`
  - Modify: `app/src/main/java/com/toolbox/dashboard/ToolDefinition.kt`
  - Modify: `app/src/main/java/com/toolbox/nav/Destinations.kt`
  - Modify: `app/src/main/java/com/toolbox/ToolboxApp.kt`

  **Approach:**
  - New sensor hook: `rememberHumidityData()` using `Sensor.TYPE_RELATIVE_HUMIDITY`
  - Large percentage display with comfort zone indicator: Dry (<30%), Comfortable (30-60%), Humid (>60%)
  - Arc gauge similar to Sound Meter — green (comfortable) → yellow (borderline) → red (too dry/too humid)
  - If both humidity and temperature sensors available, compute dew point using Magnus formula
  - Prominent "Sensor not available" state since most phones lack this sensor — link to explanation
  - Zero permissions required

  **Test scenarios:**
  - Happy path: Humidity percentage displayed on supported devices
  - Happy path: Comfort zone label updates correctly
  - Edge case: Device without humidity sensor shows clear unavailable state (most devices)

  **Verification:**
  - Humidity reading is reasonable on supported devices. Unavailable state is clear and non-confusing on unsupported devices.

### Phase 8: Motion & Activity Tools

- [x] **Unit 30: Pedometer (Step Counter)**

  **Goal:** Count steps using the hardware step counter with distance estimation.

  **Requirements:** Zero-permission tool using `TYPE_STEP_COUNTER` / `TYPE_STEP_DETECTOR` sensors

  **Dependencies:** Unit 11 (sensor hooks pattern)

  **Files:**
  - Create: `app/src/main/java/com/toolbox/core/sensor/StepSensorHook.kt`
  - Create: `app/src/main/java/com/toolbox/everyday/pedometer/PedometerScreen.kt`
  - Create: `app/src/main/java/com/toolbox/everyday/pedometer/PedometerViewModel.kt`
  - Modify: `app/src/main/java/com/toolbox/dashboard/ToolDefinition.kt`
  - Modify: `app/src/main/java/com/toolbox/nav/Destinations.kt`
  - Modify: `app/src/main/java/com/toolbox/ToolboxApp.kt`

  **Approach:**
  - Use `TYPE_STEP_COUNTER` (cumulative since boot) — capture initial value on screen open, show delta as session steps
  - Large step count display with circular progress ring toward daily goal (default 10,000, adjustable)
  - Estimated distance: steps × stride length (default 0.75m, configurable in settings)
  - Estimated calories: steps × 0.04 kcal (rough average)
  - Session stats: steps, distance, duration, pace
  - "Reset Session" button to start fresh count
  - DataStore persistence for stride length preference and daily goal
  - Note: This is a session-only pedometer (active while screen is open), NOT a background step tracker — keeps it zero-permission and simple

  **Test scenarios:**
  - Happy path: Step count increments while walking
  - Happy path: Distance and calorie estimates update with steps
  - Happy path: Reset clears session count
  - Edge case: Device without step sensor shows unavailable state

  **Verification:**
  - Steps count accurately during walking. Distance estimate is in reasonable range.

- [x] **Unit 31: Gyroscope Meter**

  **Goal:** Display real-time rotation rate on all 3 axes with visual feedback.

  **Requirements:** Zero-permission tool using `TYPE_GYROSCOPE` sensor

  **Dependencies:** Unit 11 (sensor hooks pattern)

  **Files:**
  - Create: `app/src/main/java/com/toolbox/core/sensor/GyroscopeHook.kt`
  - Create: `app/src/main/java/com/toolbox/measurement/gyroscope/GyroscopeScreen.kt`
  - Modify: `app/src/main/java/com/toolbox/dashboard/ToolDefinition.kt`
  - Modify: `app/src/main/java/com/toolbox/nav/Destinations.kt`
  - Modify: `app/src/main/java/com/toolbox/ToolboxApp.kt`

  **Approach:**
  - New sensor hook: `rememberGyroscopeData()` using `Sensor.TYPE_GYROSCOPE` at `SENSOR_DELAY_UI`
  - Display rotation rate (°/s) for X, Y, Z axes as horizontal bar gauges
  - Total rotation magnitude prominently displayed
  - Stability indicator: "Stable" when total rotation < 0.5°/s, "Moving" when moderate, "Spinning" when high
  - Rolling waveform chart showing rotation rate over time (reuse Canvas pattern from Vibrometer)
  - Peak rotation rate tracking with reset
  - Zero permissions required

  **Test scenarios:**
  - Happy path: Rotation rates update when rotating the phone
  - Happy path: Stability indicator changes appropriately
  - Edge case: Stationary phone shows near-zero on all axes

  **Verification:**
  - Responsive to rotation. Stable phone reads near-zero. Fast spin shows high values.

### Phase 9: Camera & Software-Based Tools

- [x] **Unit 32: Heart Rate Monitor**

  **Goal:** Estimate heart rate by detecting blood pulse through fingertip placed on the camera lens with flash on.

  **Requirements:** Camera permission (already used by other tools)

  **Dependencies:** Unit 15 (shared CameraX composable)

  **Files:**
  - Create: `app/src/main/java/com/toolbox/everyday/heartrate/HeartRateScreen.kt`
  - Create: `app/src/main/java/com/toolbox/everyday/heartrate/HeartRateAnalyzer.kt`
  - Modify: `app/src/main/java/com/toolbox/dashboard/ToolDefinition.kt`
  - Modify: `app/src/main/java/com/toolbox/nav/Destinations.kt`
  - Modify: `app/src/main/java/com/toolbox/ToolboxApp.kt`

  **Approach:**
  - Use CameraX `ImageAnalysis` with torch enabled — user places fingertip over rear camera + flash
  - `HeartRateAnalyzer` implements `ImageAnalysis.Analyzer`: extract average red channel intensity from each frame
  - Detect periodic brightness fluctuations caused by blood pulse (PPG — photoplethysmography)
  - Peak detection on red channel signal to count beats over a 15-30 second window
  - Display: BPM reading, live pulse waveform (Canvas), measurement progress bar
  - States: "Place finger on camera", "Measuring..." (with countdown), "Result: XX BPM"
  - Disclaimer: "For reference only. Not a medical device."
  - Uses existing camera permission — no new permissions needed

  **Test scenarios:**
  - Happy path: Detects heart rate when finger covers camera with flash
  - Happy path: Waveform shows periodic pulse signal
  - Edge case: No finger detected — shows instruction state
  - Edge case: Too much movement — shows "Hold steady" warning

  **Verification:**
  - BPM reading is in reasonable range (50-120) for resting heart rate. Waveform shows clear periodic signal.

- [x] **Unit 33: Spectrum Analyzer (Audio FFT)**

  **Goal:** Real-time frequency spectrum display of ambient audio using FFT.

  **Requirements:** Microphone permission (already used by Sound Meter)

  **Dependencies:** Unit 14 (Sound Meter audio infrastructure)

  **Files:**
  - Create: `app/src/main/java/com/toolbox/measurement/spectrum/SpectrumScreen.kt`
  - Create: `app/src/main/java/com/toolbox/measurement/spectrum/FFTProcessor.kt`
  - Modify: `app/src/main/java/com/toolbox/dashboard/ToolDefinition.kt`
  - Modify: `app/src/main/java/com/toolbox/nav/Destinations.kt`
  - Modify: `app/src/main/java/com/toolbox/ToolboxApp.kt`

  **Approach:**
  - Reuse `AudioRecord` setup from Sound Meter (44100 Hz sample rate, mono)
  - `FFTProcessor`: apply Hann window to audio buffer, compute FFT using a simple radix-2 Cooley-Tukey implementation (no external lib needed for 1024/2048-point FFT)
  - Display as bar chart spectrum (Canvas): X-axis = frequency bands (20 Hz - 20 kHz, log scale), Y-axis = magnitude (dB)
  - Color-coded frequency regions: Sub-bass, Bass, Low-mid, Mid, High-mid, Presence, Brilliance
  - Peak hold: show peak markers that decay slowly
  - Toggle between bar and waterfall (spectrogram) visualization
  - Uses existing microphone permission — no new permissions needed

  **Test scenarios:**
  - Happy path: Spectrum responds to different audio frequencies
  - Happy path: Peak hold markers visible and decay
  - Happy path: Bar and waterfall views both render
  - Edge case: Silence shows flat/minimal spectrum

  **Verification:**
  - Playing a known tone (e.g., 440 Hz) shows clear peak at correct frequency. Broadband noise shows spread across spectrum.

### Phase 10: Location-Based Tools

- [x] **Unit 34: Speedometer (GPS Speed Meter)**

  **Goal:** Display real-time speed using GPS with max/average tracking.

  **Requirements:** `ACCESS_FINE_LOCATION` permission (new — runtime only)

  **Dependencies:** Unit 4 (PermissionGate)

  **Files:**
  - Create: `app/src/main/java/com/toolbox/measurement/speedometer/SpeedometerScreen.kt`
  - Create: `app/src/main/java/com/toolbox/measurement/speedometer/SpeedometerViewModel.kt`
  - Modify: `app/src/main/java/com/toolbox/dashboard/ToolDefinition.kt`
  - Modify: `app/src/main/java/com/toolbox/nav/Destinations.kt`
  - Modify: `app/src/main/java/com/toolbox/ToolboxApp.kt`
  - Modify: `app/src/main/AndroidManifest.xml` (add `ACCESS_FINE_LOCATION`)

  **Approach:**
  - Use `LocationManager` or `FusedLocationProviderClient` with high-accuracy request (1-second interval)
  - Large speedometer gauge (arc Canvas) showing current speed
  - Unit toggle: km/h / mph / m/s / knots
  - Stats: current speed, max speed, average speed, distance traveled
  - Trip mode: start/stop/reset for tracking a journey segment
  - GPS accuracy indicator so user knows reading quality
  - PermissionGate wrapping the screen with rationale: "Speed measurement requires location access"
  - Note: This adds `ACCESS_FINE_LOCATION` as a dangerous permission — only requested when user opens this tool (lazy permission model)

  **Test scenarios:**
  - Happy path: Speed updates while moving (driving/walking)
  - Happy path: Unit toggle switches correctly
  - Happy path: Max and average speed track correctly
  - Edge case: Indoors with poor GPS shows accuracy warning
  - Edge case: Permission denied shows PermissionGate rationale

  **Verification:**
  - Speed reading matches actual speed within reasonable margin. Works in a moving vehicle.

- [x] **Unit 35: GPS Altitude**

  **Goal:** Display current elevation from GPS with trip tracking.

  **Requirements:** `ACCESS_FINE_LOCATION` permission (shared with Speedometer)

  **Dependencies:** Unit 34 (shares location permission)

  **Files:**
  - Create: `app/src/main/java/com/toolbox/measurement/altitude/AltitudeScreen.kt`
  - Modify: `app/src/main/java/com/toolbox/dashboard/ToolDefinition.kt`
  - Modify: `app/src/main/java/com/toolbox/nav/Destinations.kt`
  - Modify: `app/src/main/java/com/toolbox/ToolboxApp.kt`

  **Approach:**
  - Use same location provider as Speedometer — extract `Location.altitude` (meters above WGS84 ellipsoid)
  - Display current altitude prominently with unit toggle: meters / feet
  - If barometer is also available, show both GPS altitude and barometric altitude for comparison
  - Rolling altitude chart over time
  - Ascent/descent tracking for hiking: total elevation gain and loss
  - GPS accuracy indicator
  - PermissionGate with rationale: "Altitude measurement requires location access"

  **Test scenarios:**
  - Happy path: Altitude displays and changes with elevation
  - Happy path: Ascent/descent tracking accumulates correctly
  - Edge case: Indoor GPS gives less accurate altitude — show accuracy warning
  - Edge case: Combined barometer + GPS reading when both available

  **Verification:**
  - Altitude reading is reasonable for known location. Elevation gain tracks when climbing stairs/hills.

## System-Wide Impact

- **Interaction graph**: Dashboard ↔ Navigation ↔ 16 tool screens. TimerService ↔ StopwatchTimerScreen via bound service + StateFlow. TimerAlarmReceiver as fallback completion trigger. DataStore ↔ Counter, Unit Converter, Theme. CameraX provider shared across QR Scanner, Color Picker, Magnifier (only one camera binding active at a time via lifecycle)
- **Camera binding guard**: `CameraPreview` composable must unbind camera in `DisposableEffect.onDispose`. If navigating directly between two camera tools, the leaving screen's `onDispose` fires before the entering screen composes (Navigation Compose default). However, if `SharedTransitionLayout` keeps both compositions alive during transition animation, both screens may try to bind simultaneously. Mitigation: skip shared element transition for camera-tool-to-camera-tool navigation, or defer camera binding until enter transition completes
- **Screen orientation locking**: Sensor measurement tools (Level, Compass, Protractor, Sound Meter) should lock to portrait orientation programmatically via `Activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT` on enter, reset to `SCREEN_ORIENTATION_UNSPECIFIED` on exit. Implement as a `ToolScaffold` parameter (`lockOrientation: Boolean = false`) that handles the lifecycle
- **Error propagation**: Sensor unavailability surfaces as disabled dashboard tiles (R1). Permission denial surfaces via PermissionGate composable with rationale/settings redirect. Camera binding failure shows error state within `CameraPreview` composable itself (error callback or internal error UI). `SensorManager.registerListener()` returning false treated as sensor unavailable
- **State lifecycle risks**: CameraX provider must unbind on lifecycle pause to release camera hardware. Timer foreground service must `stopSelf()` on completion to avoid lingering notification. `AudioRecord.release()` must be called in `DisposableEffect.onDispose` to prevent native resource leaks. DataStore writes are atomic (no partial-write risk). Level/Compass lock/hold frozen value belongs in ViewModel (survives config change), not in composable state
- **Known limitation**: If app process is killed while flashlight is on, torch remains on until system or another app toggles it off. This is standard Android behavior — accepted for MVP
- **Unchanged invariants**: Android system permissions model — dangerous permissions always runtime-only, normal permissions auto-granted

## Risks & Dependencies

| Risk | Mitigation |
|------|------------|
| Sensor accuracy varies wildly between devices | Add disclaimers on measurement tools; ruler has credit card calibration; compass has accuracy indicator |
| Play Store may reject foreground service usage | Hybrid AlarmManager + foreground service approach — timer completes even without the service. `specialUse` type with clear justification; timer is a legitimate use case |
| Camera binding conflict during tool-to-tool navigation | Skip shared element transition for camera-to-camera nav, or defer camera binding until enter transition completes |
| APK size exceeds 15 MB target | ZXing core (~0.5 MB) chosen over ML Kit (~3-5 MB); monitor with `./gradlew assembleRelease` after each phase |
| CameraX version incompatibility with camera-compose | Pin CameraX versions together via explicit version catalog; test on 3+ device models |
| Some devices lack magnetometer or barometer | SensorAvailability check at dashboard load; affected tools shown as disabled |
| Humidity/Temperature sensors extremely rare | Clear "Sensor not available" UI; these tools may be unusable on most devices — acceptable as bonus features |
| Heart rate PPG accuracy varies by device | Prominent "Not a medical device" disclaimer; require 15-30s measurement window for stability |
| GPS altitude is inaccurate indoors | Show GPS accuracy indicator; recommend barometer for indoor altitude (Phase 7) |
| `ACCESS_FINE_LOCATION` is a sensitive permission | Only requested lazily when Speedometer/Altitude is opened; never at install time; clear rationale in PermissionGate |
| FFT implementation performance on low-end devices | Use 1024-point FFT (not 4096); profile on budget devices; drop to lower resolution if frame drops |

## Sources & References

- **Origin document:** [docs/brainstorms/2026-04-11-android-toolbox-requirements.md](docs/brainstorms/2026-04-11-android-toolbox-requirements.md)
- Navigation Compose type-safe routes: https://developer.android.com/guide/navigation/design/type-safety
- CameraX Compose integration: https://developer.android.com/jetpack/androidx/releases/camera
- Shared element transitions: https://developer.android.com/develop/ui/compose/animation/shared-elements/navigation
- ZXing core: https://github.com/zxing/zxing
- Material 3 dynamic color: https://developer.android.com/develop/ui/compose/designsystems/material3
- Foreground service types: https://developer.android.com/develop/background-work/services/foreground-services
