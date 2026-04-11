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

Remaining tool screens (Flashlight, Counter, Stopwatch, Protractor, Ruler, Percentage, Tip Calculator, Number Base, Random/Dice, Color Picker, Magnifier) can be generated from Stitch MCP as needed during implementation using `mcp__stitch__generate_screen_from_text` with project ID `17370519435532616667`.

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

- [ ] **Unit 1: Gradle project setup**

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

- [ ] **Unit 2: Theme and app shell**

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

- [ ] **Unit 3: Dashboard and navigation**

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

- [ ] **Unit 4: Permission gate composable**

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

- [ ] **Unit 5: Unit Converter**

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

- [ ] **Unit 6: Percentage, Tip, and Number Base calculators**

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

- [ ] **Unit 7: Counter / Tally**

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

- [ ] **Unit 8: Stopwatch & Timer**

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

- [ ] **Unit 9: Random / Coin Flip / Dice**

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

- [ ] **Unit 10: On-Screen Ruler**

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

- [ ] **Unit 11: Sensor composable hooks**

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

- [ ] **Unit 12: Bubble Level**

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

- [ ] **Unit 13: Compass**

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

- [ ] **Unit 14: Protractor and Sound Meter**

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

- [ ] **Unit 15: Shared camera composable**

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

- [ ] **Unit 16: QR / Barcode Scanner + Generator**

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

- [ ] **Unit 17: Color Picker**

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

- [ ] **Unit 18: Magnifier**

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

- [ ] **Unit 19: Flashlight**

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

- [ ] **Unit 20: Polish and Play Store prep**

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

## Sources & References

- **Origin document:** [docs/brainstorms/2026-04-11-android-toolbox-requirements.md](docs/brainstorms/2026-04-11-android-toolbox-requirements.md)
- Navigation Compose type-safe routes: https://developer.android.com/guide/navigation/design/type-safety
- CameraX Compose integration: https://developer.android.com/jetpack/androidx/releases/camera
- Shared element transitions: https://developer.android.com/develop/ui/compose/animation/shared-elements/navigation
- ZXing core: https://github.com/zxing/zxing
- Material 3 dynamic color: https://developer.android.com/develop/ui/compose/designsystems/material3
- Foreground service types: https://developer.android.com/develop/background-work/services/foreground-services
