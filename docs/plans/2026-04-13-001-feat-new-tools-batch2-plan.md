---
title: "feat: Add 7 new tools — Loan/EMI, Device Info, Network Info, NFC Toolkit, Tone Generator, Formula Solver, Document Scanner"
type: feat
status: active
date: 2026-04-13
origin: docs/brainstorms/2026-04-13-new-tools-batch2-requirements.md
---

# New Tools — Batch 2

## Overview

Add 7 new tools to the Toolbox app: Loan/EMI Calculator, Device Info, Network Info, NFC Toolkit, Frequency/Tone Generator, Formula Reference & Solver, and Document Scanner. Each tool follows existing app patterns (single-module, package-per-feature, Compose + Material 3, no DI framework) and works fully offline.

## Problem Frame

Toolbox has 25+ tools but lacks common utilities people expect (calculator-adjacent tools, device diagnostics, document scanning) and unique differentiators (NFC toolkit, tone generator). This batch fills those gaps while maintaining the app's core value proposition: free, offline, no ads. (see origin: docs/brainstorms/2026-04-13-new-tools-batch2-requirements.md)

## Requirements Trace

- R1-R5: Loan/EMI Calculator — input, display, charts, amortization, calculation methods
- R6-R11: Device Info — hardware, display, storage, RAM, battery, sensors
- R12-R16: Network Info — connection type, Wi-Fi details, IPs, mobile network, clipboard
- R17-R22, R23a: NFC Toolkit — read, write, tag details, format, erase, hex dump, confirmation dialogs
- R23-R28: Frequency/Tone Generator — tone output, slider, presets, waveforms, volume, visual
- R29-R34: Formula Reference & Solver — browse, display, solve, search, favorites, step-by-step
- R35-R40: Document Scanner — capture, crop, filters, save images, multi-page, PDF

## Scope Boundaries

- No cloud storage or sync — all data stays on device (see origin)
- No OCR for document scanner (see origin)
- No interactive graphing or plotting in Formula Solver (see origin)
- No Wi-Fi speed test (see origin)
- NFC tag cloning limited to writable tags (see origin)
- WifiSignal tool stays as-is — Network Info complements it with static network properties, no RSSI visualization overlap

## Context & Research

### Relevant Code and Patterns

- **Navigation:** `Destinations.kt` (`@Serializable object`), `ToolboxApp.kt` (`composable<Dest> { ToolScreen(...) { Screen() } }`), `toolDestination()` mapping
- **Tool registration:** `ToolDefinition.kt` — `allTools` list with id, name, icon, category, search keywords
- **Screen pattern:** Most tools are stateless `@Composable` functions with `remember`/`rememberSaveable`; ViewModels only when needed (DataStore, complex state)
- **Permission gate:** `PermissionGate.kt` wraps screen content, shows rationale and settings redirect
- **Sensor hooks:** `DisposableEffect` + `LifecycleEventObserver` pattern in `core/sensor/`
- **Camera:** `CameraPreview.kt` wraps CameraX; used in QR scanner, magnifier, mirror, color picker
- **Audio output:** `WhiteNoiseScreen.kt` (continuous `AudioTrack.MODE_STREAM`), `MetronomeScreen.kt` (short tone)
- **MediaStore save:** `MagnifierScreen.kt` — `ContentValues` + `MediaStore.Images.Media.EXTERNAL_CONTENT_URI` + `RELATIVE_PATH`
- **Existing WiFi tool:** `WifiSignalScreen.kt` — real-time RSSI, SSID, link speed, frequency; polls every 1s
- **Sharing:** `MeasurementCardSharer` for branded image cards via `FileProvider`

### Institutional Learnings

No `docs/solutions/` directory exists yet.

## Key Technical Decisions

- **Edge detection approach:** Skip OpenCV (~30 MB native lib) and ML Kit (requires Play Services). Use CameraX `ImageCapture` for full-res photos + manual quad perspective correction via `android.graphics.Matrix`. Lighter, keeps app lean, sufficient for common document shapes. (see origin: dependency-free principle)
- **NFC API:** Use `NfcAdapter.enableReaderMode()` (API 19+) instead of foreground dispatch. Reader mode is lifecycle-safe, can be called from `DisposableEffect` via Activity reference, and avoids the `onNewIntent` complexity of foreground dispatch in single-Activity Compose architecture.
- **AudioTrack configuration:** Use `MODE_STREAM` at 44100 Hz sample rate (standard, universal hardware support). Generate waveform samples in a dedicated `Thread` (matching `WhiteNoiseScreen.kt` pattern — `AudioTrack.write()` blocks, so a raw thread avoids starving coroutine dispatchers). Use zero-crossing detection on frequency changes to avoid clicks/artifacts.
- **Network Info vs WifiSignal:** Keep both. WifiSignal is a real-time signal-strength visualizer with logging. Network Info shows static properties: connection type, IPs, carrier, BSSID, channel. Minimal field overlap (SSID shown in both for context).
- **Formula data model:** Hardcode formula catalog as Kotlin data classes — no database needed. Each formula has: name, subject, category, expression string, variable list with units. Solver evaluates by algebraic rearrangement for single-unknown formulas.
- **PDF save location:** Use `MediaStore.Downloads` on API 29+ with `RELATIVE_PATH = "Download/Toolbox"` for PDF output. On API 26-28, use `context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)` (app-private, no WRITE_EXTERNAL_STORAGE needed). No SAF picker needed — silent save is simplest UX.
- **Loan/EMI default method:** Reducing balance is default (industry standard). Method toggle recalculates immediately.
- **Charts:** Compose Canvas for pie chart and amortization table — no charting library (see origin).
- **Public IP fallback:** Show "Unavailable" with retry button when fetch fails. 5-second timeout. No caching of stale values.

## Open Questions

### Resolved During Planning

- **WifiSignal coexistence:** Keep both tools separate — WifiSignal for real-time signal monitoring, Network Info for static network properties and diagnostics.
- **Edge detection library:** Skip OpenCV and ML Kit. Use CameraX ImageCapture + Matrix-based perspective correction. Cheaper, offline, no new dependencies.
- **NFC API choice:** enableReaderMode() over foreground dispatch — lifecycle-safe in Compose.
- **AudioTrack transition:** Zero-crossing detection for artifact-free frequency changes.
- **Storage for scanner PDFs:** MediaStore.Downloads on API 29+ (minSdk 26, so also handle API 26-28 via legacy file path).
- **R5 default method:** Reducing balance is default.
- **R14 fallback:** "Unavailable" + retry button, 5s timeout.

### Deferred to Implementation

- Exact formula catalog contents (which specific formulas per subject) — curate during implementation
- AudioTrack buffer size tuning for artifact-free playback across OEM audio stacks — tune during testing
- NFC MIFARE Classic authentication handling — test with real tags, show "Authentication required" for locked sectors
- Battery capacity mAh availability across OEMs — test `/sys/class/power_supply/` paths, show "N/A" where unavailable

## Implementation Units

- [ ] **Unit 1: Shared infrastructure — navigation, tool definitions, manifest**

**Goal:** Register all 7 new tools in the navigation graph, tool catalog, and manifest so they appear on the dashboard and can be navigated to (with placeholder screens).

**Requirements:** All (infrastructure for R1-R40)

**Dependencies:** None

**Files:**
- Modify: `app/src/main/java/com/toolbox/nav/Destinations.kt`
- Modify: `app/src/main/java/com/toolbox/ToolboxApp.kt`
- Modify: `app/src/main/java/com/toolbox/dashboard/ToolDefinition.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/toolbox/conversion/emi/EmiCalculatorScreen.kt` (placeholder)
- Create: `app/src/main/java/com/toolbox/measurement/deviceinfo/DeviceInfoScreen.kt` (placeholder)
- Create: `app/src/main/java/com/toolbox/measurement/networkinfo/NetworkInfoScreen.kt` (placeholder)
- Create: `app/src/main/java/com/toolbox/everyday/nfc/NfcToolkitScreen.kt` (placeholder)
- Create: `app/src/main/java/com/toolbox/everyday/tonegenerator/ToneGeneratorScreen.kt` (placeholder)
- Create: `app/src/main/java/com/toolbox/conversion/formula/FormulaScreen.kt` (placeholder)
- Create: `app/src/main/java/com/toolbox/everyday/docscanner/DocScannerScreen.kt` (placeholder)

**Approach:**
- Add 7 `@Serializable object` entries to `Destinations.kt`
- Add 7 `composable<Dest> { ToolScreen(...) { Screen() } }` blocks in `ToolboxApp.kt`
- Add 7 entries to `toolDestination()` mapping
- Add 7 `ToolDefinition` entries to `allTools` in `ToolDefinition.kt` with appropriate categories, icons, and search keywords
- Add `<uses-permission android:name="android.permission.NFC" />`, `<uses-permission android:name="android.permission.INTERNET" />` to manifest
- Add `<uses-feature android:name="android.hardware.nfc" android:required="false" />` to manifest
- Each placeholder screen: `@Composable fun FooScreen() { Text("Coming soon") }`
- Categories: Loan/EMI → `ConversionCalculation`, Device Info → `MeasurementSensors`, Network Info → `MeasurementSensors`, NFC → `EverydayTools`, Tone Generator → `EverydayTools`, Formula → `ConversionCalculation`, Doc Scanner → `EverydayTools`

**Patterns to follow:**
- Existing destinations in `Destinations.kt` (e.g., `@Serializable object BubbleLevel`)
- Existing routing in `ToolboxApp.kt` (e.g., `composable<BubbleLevel> { ... }`)
- Existing tool definitions in `ToolDefinition.kt`

**Test scenarios:**
- Happy path: app builds and launches with 7 new tools visible on dashboard
- Happy path: tapping each new tool navigates to placeholder screen without crash
- Edge case: NFC tool should appear on dashboard even on non-NFC devices (runtime check happens in-screen, not via `requiredSensorType`)

**Verification:**
- App compiles, all 7 tools appear on dashboard, navigation to each placeholder works

---

- [ ] **Unit 2: Loan/EMI Calculator**

**Goal:** Implement a full loan/EMI calculator with input form, EMI display, pie chart, and amortization table.

**Requirements:** R1, R2, R3, R4, R5

**Dependencies:** Unit 1

**Files:**
- Modify: `app/src/main/java/com/toolbox/conversion/emi/EmiCalculatorScreen.kt`

**Approach:**
- State: `rememberSaveable` for loan amount, interest rate, tenure, tenure unit (months/years), calculation method (reducing/flat)
- EMI calculation: reducing balance uses standard formula `EMI = P × r × (1+r)^n / ((1+r)^n - 1)`; flat-rate uses `EMI = (P + P × R × T) / (T × 12)`
- Pie chart: Compose Canvas `drawArc()` for principal vs interest breakdown
- Amortization table: `LazyColumn` with month, principal portion, interest portion, remaining balance
- Default method: reducing balance
- Method switch: recalculate immediately on toggle
- Number input: use `keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)`
- Layout: input card at top, results summary card, pie chart, expandable amortization table

**Patterns to follow:**
- `TipCalculatorScreen.kt` and `BmiCalculatorScreen.kt` for input/output layout
- Compose Canvas usage in existing measurement tools for the pie chart

**Test scenarios:**
- Happy path: enter 100000 amount, 10% rate, 12 months → verify EMI matches known formula result
- Happy path: pie chart renders with correct principal/interest proportions
- Happy path: amortization table shows correct row count matching tenure
- Happy path: switching from reducing to flat-rate recalculates immediately with different EMI
- Edge case: zero interest rate → EMI equals principal / months, interest portion is 0
- Edge case: very long tenure (360 months) → LazyColumn scrolls smoothly through all rows
- Edge case: empty or zero inputs → no crash, show sensible default or disable calculate

**Verification:**
- EMI values match a reference calculator for both methods; pie chart and amortization table render correctly

---

- [ ] **Unit 3: Device Info**

**Goal:** Display comprehensive device hardware and software information.

**Requirements:** R6, R7, R8, R9, R10, R11

**Dependencies:** Unit 1

**Files:**
- Modify: `app/src/main/java/com/toolbox/measurement/deviceinfo/DeviceInfoScreen.kt`

**Approach:**
- No ViewModel needed — all data is read-only system info
- Sections as Material 3 cards: General (model, manufacturer, Android version, API, kernel), Display (resolution via `WindowMetrics` on API 30+ / `DisplayMetrics` below, density, refresh rate via `Display.getSupportedModes()`, HDR via `getHdrCapabilities()`), Storage (internal/external via `StatFs`), RAM (`ActivityManager.MemoryInfo`), Battery (register `BroadcastReceiver` for `ACTION_BATTERY_CHANGED` in `DisposableEffect`; capacity from `/sys/class/power_supply/battery/charge_full_design` with "N/A" fallback), Sensors (`SensorManager.getSensorList(TYPE_ALL)`)
- No runtime permissions needed — `Build.*`, `ActivityManager`, `StatFs`, `SensorManager`, `BatteryManager` all accessible without permission
- `StatFs` on `Environment.getExternalStorageDirectory()` works without `MANAGE_EXTERNAL_STORAGE`
- Copy-to-clipboard: long-press on any value copies to clipboard with `ClipboardManager`

**Patterns to follow:**
- Card-based layout similar to `UnitConverterScreen.kt`
- `DisposableEffect` for battery broadcast receiver (same lifecycle pattern as sensor hooks)

**Test scenarios:**
- Happy path: all sections render with non-empty values on a real device
- Happy path: display section shows correct resolution and density
- Happy path: storage section shows used/free that sum to approximately total
- Happy path: battery section shows health, status, temperature
- Edge case: battery capacity shows "N/A" on devices without `/sys/class/power_supply/` access
- Edge case: HDR shows "Not supported" on devices without HDR capabilities
- Edge case: external storage section handles devices with no SD card gracefully

**Verification:**
- All sections populate with accurate device info; no crashes on devices without optional features

---

- [ ] **Unit 4: Formula Reference & Solver**

**Goal:** Implement a browsable formula catalog with interactive solver and step-by-step solutions.

**Requirements:** R29, R30, R31, R32, R33, R34

**Dependencies:** Unit 1

**Files:**
- Modify: `app/src/main/java/com/toolbox/conversion/formula/FormulaScreen.kt`
- Create: `app/src/main/java/com/toolbox/conversion/formula/FormulaViewModel.kt`
- Create: `app/src/main/java/com/toolbox/conversion/formula/FormulaData.kt`
- Create: `app/src/main/java/com/toolbox/conversion/formula/FormulaSolver.kt`

**Approach:**
- **Data model** (`FormulaData.kt`): `Formula(id, name, subject, category, expression, variables: List<Variable>)`, `Variable(symbol, name, unit)`, `Subject` enum (Math, Physics, Chemistry, Finance), nested categories within each subject
- **Formula catalog**: hardcode ~40-60 common formulas as a `List<Formula>` constant. Examples: area of circle, Pythagorean theorem, Ohm's law, ideal gas law, compound interest, quadratic formula
- **Solver** (`FormulaSolver.kt`): for each formula, pre-define algebraic rearrangements for each variable. Given N-1 known values, solve for the unknown. Use `kotlin.math` functions. Return step-by-step substitution as a list of strings
- **ViewModel** (`FormulaViewModel.kt`): manages selected subject, search query, favorites (persisted via `DataStore`), selected formula, input values, solver results
- **UI** (`FormulaScreen.kt`): subject tabs/chips at top → filtered formula list → tap formula → solver bottom sheet or expanded card with input fields for each variable, "Solve" button, results with steps
- **Search**: filter formula list by name and keyword matching
- **Favorites**: star icon on each formula, persisted as Set<String> of formula IDs in DataStore

**Patterns to follow:**
- `UnitConverterViewModel.kt` for DataStore-backed favorites
- `CalculatorScreen.kt` for numeric input fields and results display
- Subject chip/tab pattern similar to category selection in `DashboardScreen.kt`

**Test scenarios:**
- Happy path: browse Math → Geometry → tap "Area of Circle" → enter radius 5 → solve → shows A = π × 5² = 78.54
- Happy path: search "ohm" → finds Ohm's law
- Happy path: favorite a formula → it appears in favorites section
- Happy path: step-by-step shows substitution for each variable
- Happy path: solve for different unknown variable (e.g., solve Ohm's law for R given V and I)
- Edge case: enter non-numeric input → show validation error, don't crash
- Edge case: division by zero in formula → show appropriate error message
- Edge case: search with no results → show empty state

**Verification:**
- Formulas browsable by subject, search works, solver produces correct results with steps, favorites persist across app restarts

---

- [ ] **Unit 5: Network Info**

**Goal:** Display network connection details including connection type, Wi-Fi properties, IP addresses, and mobile network info.

**Requirements:** R12, R13, R14, R15, R16

**Dependencies:** Unit 1

**Files:**
- Modify: `app/src/main/java/com/toolbox/measurement/networkinfo/NetworkInfoScreen.kt`

**Approach:**
- Wrap content in `PermissionGate(ACCESS_FINE_LOCATION, ...)` — needed for SSID/BSSID on API 26+
- Connection type: `ConnectivityManager.getActiveNetwork()` + `getNetworkCapabilities()` to detect Wi-Fi, cellular, or none
- Wi-Fi details: `WifiManager.connectionInfo` for SSID, BSSID, link speed, frequency; derive channel from frequency; signal strength from RSSI
- IP addresses: `LinkProperties.linkAddresses` from `ConnectivityManager` for local IPv4/IPv6
- Public IP: `URL("https://api.ipify.org").readText()` on `Dispatchers.IO` with 5s timeout; show "Unavailable" + retry button on failure; wrap in try-catch
- Mobile network: `TelephonyManager.networkOperatorName` for carrier, `networkType` for 4G/5G (requires `READ_PHONE_STATE` on API 30+); show "Permission required" for restricted fields
- Copy-to-clipboard: `IconButton` with copy icon next to each value row
- Add `READ_PHONE_STATE` permission to manifest (for R15 mobile network details)
- Layout: connection status card at top, then Wi-Fi card / Mobile card (show whichever is active), then IP addresses card
- Graceful degradation: when location denied, show SSID as "Permission required" with explanation; when READ_PHONE_STATE denied, show carrier as "Permission required"

**Patterns to follow:**
- `WifiSignalScreen.kt` for WiFi info access patterns and permission gating
- `SpeedometerScreen.kt` for `DisposableEffect`-based location/connectivity monitoring

**Test scenarios:**
- Happy path: connected to Wi-Fi → shows SSID, BSSID, signal, frequency, channel, link speed
- Happy path: local IPv4 and IPv6 addresses display correctly
- Happy path: public IP fetches and displays (requires network)
- Happy path: on mobile data → shows carrier name, network type, signal strength
- Happy path: copy any value → clipboard contains exact value, toast confirms
- Edge case: no connection → shows "Not connected" status
- Edge case: location permission denied → SSID/BSSID show "Permission required" with rationale
- Edge case: READ_PHONE_STATE denied → carrier info shows "Permission required"
- Edge case: public IP fetch fails (offline) → shows "Unavailable" with retry button
- Edge case: switch from Wi-Fi to mobile → UI updates to show mobile info

**Verification:**
- All network properties display correctly; copy works; graceful degradation when permissions denied or offline

---

- [ ] **Unit 6: Frequency/Tone Generator**

**Goal:** Generate continuous tones with selectable frequency, waveform, and volume, with visual waveform display.

**Requirements:** R23, R24, R25, R26, R27, R28

**Dependencies:** Unit 1

**Files:**
- Modify: `app/src/main/java/com/toolbox/everyday/tonegenerator/ToneGeneratorScreen.kt`
- Create: `app/src/main/java/com/toolbox/everyday/tonegenerator/ToneGenerator.kt`

**Approach:**
- **Audio engine** (`ToneGenerator.kt`): class wrapping `AudioTrack.MODE_STREAM` at 44100 Hz, 16-bit mono PCM. Dedicated `Thread` fills buffer with generated samples (matches `WhiteNoiseScreen.kt` pattern — `AudioTrack.write()` blocks, so a raw thread avoids starving coroutine dispatchers). Waveform generation functions for sine (`sin(2π × freq × t / sampleRate)`), square (`sign(sin(...))`), triangle, sawtooth. Volume: multiply samples by gain factor (0.0-1.0). Frequency changes: use zero-crossing detection — wait for current waveform to cross zero before switching to new frequency to avoid clicks
- **UI**: large frequency display at top, horizontal slider (logarithmic scale for better UX across 20-20000 Hz), numeric input field, preset chips (18 kHz, 440 Hz A4, C4 through B4), waveform selector (4 chips), volume slider, play/stop button, waveform preview via Compose Canvas
- **Waveform preview**: Canvas drawing of ~2 cycles of the current waveform shape at current frequency (visual only, not real-time audio visualization)
- **Lifecycle**: start/stop AudioTrack in `DisposableEffect` — release in `onDispose` (critical: same pattern as `WhiteNoiseScreen.kt`)
- **Frequency on slider**: apply on slider release (`onValueChangeFinished`), not during drag, for artifact-free transitions

**Patterns to follow:**
- `WhiteNoiseScreen.kt` for continuous `AudioTrack.MODE_STREAM` pattern
- `MetronomeScreen.kt` for tone generation reference
- Canvas waveform drawing similar to existing measurement visualizations

**Test scenarios:**
- Happy path: play 440 Hz sine wave → audible tone at A4 pitch
- Happy path: change to 1000 Hz → tone pitch changes without click
- Happy path: switch waveform to square → audible timbre change
- Happy path: adjust volume slider → tone gets louder/quieter
- Happy path: tap 18 kHz preset → slider moves to 18000, tone plays (barely audible)
- Happy path: tap C4 preset → plays 261.63 Hz
- Happy path: waveform preview shows correct shape for each waveform type
- Edge case: frequency at 20 Hz → low rumble audible on capable speakers
- Edge case: rapid frequency changes via slider → no clicks or artifacts
- Edge case: navigate away while playing → tone stops (DisposableEffect cleanup)
- Error path: AudioTrack initialization failure → show error message

**Verification:**
- All 4 waveforms produce distinct audio; frequency changes are artifact-free; volume control works; UI is responsive during playback

---

- [ ] **Unit 7: NFC Toolkit**

**Goal:** Read, write, format, and erase NFC tags with full technical details display.

**Requirements:** R17, R18, R19, R20, R21, R22, R23a

**Dependencies:** Unit 1

**Files:**
- Modify: `app/src/main/java/com/toolbox/everyday/nfc/NfcToolkitScreen.kt`
- Create: `app/src/main/java/com/toolbox/everyday/nfc/NfcHelper.kt`

**Approach:**
- **Hardware check**: `NfcAdapter.getDefaultAdapter(context)` — if null, show "NFC not available on this device" message with icon
- **NFC enabled check**: if adapter exists but `!isEnabled`, show "NFC is disabled" with button to open NFC settings (`Settings.ACTION_NFC_SETTINGS`)
- **Reader mode** (`NfcHelper.kt`): use `NfcAdapter.enableReaderMode()` in `DisposableEffect` — call on the Activity (obtained via `LocalContext.current as Activity`). Reader mode flags: `FLAG_READER_NFC_A or FLAG_READER_NFC_B or FLAG_READER_NFC_F or FLAG_READER_NFC_V`. Callback receives `Tag` object
- **Tag processing**: on tag discovered, read all tech types (`tag.techList`), UID (`tag.id.toHexString()`), try `Ndef.get(tag)` for NDEF content, `MifareClassic.get(tag)` or `MifareUltralight.get(tag)` for memory details
- **NDEF parsing**: `NdefMessage.records` → for each `NdefRecord`: check TNF + type to determine URL, text, contact (vCard), or raw. Display parsed content with icons
- **Write mode**: tab/mode selector (Read | Write | Tools). Write mode: user enters text/URL/contact, tap tag to write. Use `Ndef.get(tag).writeNdefMessage()` for NDEF tags, `NdefFormatable.get(tag).format()` + write for unformatted tags
- **Tools tab**: Format to NDEF, Erase (write empty NDEF message), Hex dump (`tag.id` + sector/page data as hex grid)
- **Confirmation dialogs (R23a)**: `AlertDialog` before write, format, and erase operations with clear warning text about data loss. "This will overwrite all data on the tag. This action cannot be undone."
- **Error handling**: tag removed mid-operation → catch `TagLostException`, show "Tag lost — hold tag steady and try again"
- **UI**: three tabs (Read, Write, Tools), "Waiting for tag..." animation when no tag detected, tag info cards when scanned

**Patterns to follow:**
- `DisposableEffect` lifecycle pattern from sensor hooks for reader mode enable/disable
- `PermissionGate.kt` pattern for showing unavailable state (adapted for NFC hardware check)

**Test scenarios:**
- Happy path: scan NDEF tag with URL → displays URL with link icon
- Happy path: scan NDEF tag with text → displays text content
- Happy path: write text to writable tag → success message, re-read confirms content
- Happy path: format unformatted tag → success message
- Happy path: erase tag → success message, re-read shows empty
- Happy path: hex dump shows tag UID and raw data in hex grid
- Happy path: tag details show type, UID, memory size, read-only status
- Happy path: confirmation dialog appears before write/format/erase
- Edge case: device without NFC → shows "NFC not available" message
- Edge case: NFC disabled → shows "Enable NFC" button
- Edge case: tag removed during write → "Tag lost" error with retry prompt
- Edge case: read-only tag → write/format/erase show "Tag is read-only" error
- Edge case: non-NDEF tag → show raw tech list and hex dump, no parsed content

**Verification:**
- Read, write, format, erase all work with real NFC tags; confirmation dialogs prevent accidental destructive operations; graceful handling of non-NFC devices

---

- [ ] **Unit 8: Document Scanner**

**Goal:** Camera-based document scanning with edge detection, perspective correction, filters, and PDF export.

**Requirements:** R35, R36, R37, R38, R39, R40

**Dependencies:** Unit 1

**Files:**
- Modify: `app/src/main/java/com/toolbox/everyday/docscanner/DocScannerScreen.kt`
- Create: `app/src/main/java/com/toolbox/everyday/docscanner/DocScannerViewModel.kt`
- Create: `app/src/main/java/com/toolbox/everyday/docscanner/EdgeDetector.kt`
- Create: `app/src/main/java/com/toolbox/everyday/docscanner/PerspectiveTransform.kt`
- Create: `app/src/main/java/com/toolbox/everyday/docscanner/ImageFilters.kt`

**Approach:**
- **Camera capture**: use CameraX `ImageCapture` use case (already a dependency but not yet used for capture). Add `ImageCapture` alongside `Preview` in camera binding. Capture on button tap via `takePicture()` → `ImageCaptureException` or `ImageProxy`
- **Edge detection** (`EdgeDetector.kt`): convert captured image to grayscale `Bitmap`, apply Gaussian blur, Canny-like edge detection using pixel luminance gradients, find largest quadrilateral contour. Return 4 corner points or null if no document detected. This is a simplified heuristic — not OpenCV-quality but sufficient for high-contrast documents on plain backgrounds
- **Manual corner adjustment** (R36): overlay 4 draggable corner handles on the captured image. User drags corners to refine crop. Default to detected corners or image corners if detection fails
- **Perspective transform** (`PerspectiveTransform.kt`): use `android.graphics.Matrix.setPolyToPoly()` to map the 4 source corners to a rectangular destination. Apply via `Bitmap.createBitmap()` with the matrix
- **Filters** (`ImageFilters.kt`): grayscale (`ColorMatrix.setSaturation(0)`), high contrast (increase contrast via `ColorMatrix`), black & white (grayscale + threshold), original (no-op)
- **Multi-page** (R39): ViewModel holds a `List<ScannedPage>` — each page is a processed `Bitmap`. "Add page" button returns to camera. Page thumbnail strip at bottom for reordering/deleting
- **Save as image** (R38): `MediaStore.Images.Media` with `RELATIVE_PATH = "Pictures/DocScanner"` (same pattern as `MagnifierScreen.kt`). Use JPEG compression (quality 90) via `Bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)`
- **Save as PDF** (R40): `android.graphics.pdf.PdfDocument` → create page per scanned image, draw bitmap onto page canvas. Save to `MediaStore.Downloads` on API 29+ with `RELATIVE_PATH = "Download/Toolbox"`. On API 26-28, save to `context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)` (app-private, no WRITE_EXTERNAL_STORAGE needed). Default page size: scale to A4 aspect ratio (595×842 points)
- **ViewModel** (`DocScannerViewModel.kt`): manages scanned pages list, current page state, selected filter
- **Permission**: `PermissionGate(CAMERA, ...)` wrapping entire screen

**Patterns to follow:**
- `CameraPreview.kt` for CameraX setup
- `QrScannerScreen.kt` for camera + analysis pattern
- `MagnifierScreen.kt` for MediaStore save pattern
- `HeartRateScreen.kt` for direct camera control when needed

**Test scenarios:**
- Happy path: capture document photo → edge detection highlights document boundary
- Happy path: adjust corners manually → perspective corrects to rectangular output
- Happy path: apply grayscale filter → image converts to grayscale
- Happy path: apply B&W filter → high-contrast black and white output
- Happy path: save single page as JPEG → appears in gallery
- Happy path: add multiple pages → thumbnail strip shows all pages
- Happy path: save multi-page as PDF → PDF file appears in Downloads
- Edge case: no document detected → corners default to image edges, user adjusts manually
- Edge case: camera permission denied → shows permission rationale with grant button
- Edge case: very dark image → filters still apply without crash
- Edge case: large number of pages (10+) → no OOM, thumbnails scroll
- Error path: save fails (storage full) → error toast

**Verification:**
- Full scanning flow works: capture → detect/adjust corners → correct perspective → apply filter → save as image or PDF. Multi-page PDF produces valid PDF file openable in standard viewers

## System-Wide Impact

- **Navigation graph:** 7 new destinations added to `Destinations.kt` and `ToolboxApp.kt` — existing tool navigation is unchanged
- **Dashboard:** 7 new `ToolDefinition` entries in `allTools` — dashboard grid, search, and category filtering automatically include them
- **Manifest:** NFC permission + feature declaration, INTERNET permission, READ_PHONE_STATE permission added — no impact on existing tools
- **Favorites:** new tool IDs automatically work with existing `UserPreferencesRepository` favorites system
- **Shortcuts:** `FavoriteShortcutManager.iconResForTool()` needs entries for new tool IDs or falls back to default icon
- **Quick Settings tiles:** no new tiles in this batch (can be added later)
- **APK size:** no new native libraries or large dependencies. Formula catalog and edge detection heuristic are pure Kotlin. Estimated increase: <500 KB code + resources
- **Unchanged invariants:** existing 25+ tools, WifiSignal tool, calculator, all sensor hooks, camera preview — none modified

## Risks & Dependencies

| Risk | Mitigation |
|------|------------|
| Edge detection heuristic may be inaccurate for low-contrast documents | Manual corner adjustment (R36) serves as fallback; can improve heuristic iteratively |
| NFC reader mode requires Activity reference from Composable | `LocalContext.current as Activity` is the established Compose pattern; test on multiple devices |
| AudioTrack behavior varies across OEM audio stacks | Use standard 44100 Hz sample rate; zero-crossing transitions; test on multiple devices |
| Battery capacity unavailable on some devices | Show "N/A" fallback; R10 already scoped for this |
| PDF save path differs between API 26-28 and 29+ | Two code paths: MediaStore.Downloads (29+) and legacy Environment path (26-28) |
| Large multi-page scans may cause memory pressure | Process and compress each page before adding to list; use JPEG compression |
| Public IP endpoint (api.ipify.org) could be unavailable | 5s timeout, "Unavailable" with retry; not critical to tool function |

## Sources & References

- **Origin document:** [docs/brainstorms/2026-04-13-new-tools-batch2-requirements.md](docs/brainstorms/2026-04-13-new-tools-batch2-requirements.md)
- Navigation: `app/src/main/java/com/toolbox/nav/Destinations.kt`, `app/src/main/java/com/toolbox/ToolboxApp.kt`
- Tool catalog: `app/src/main/java/com/toolbox/dashboard/ToolDefinition.kt`
- Permission gate: `app/src/main/java/com/toolbox/core/permission/PermissionGate.kt`
- Audio output: `app/src/main/java/com/toolbox/everyday/whitenoise/WhiteNoiseScreen.kt`
- Camera: `app/src/main/java/com/toolbox/core/camera/CameraPreview.kt`
- MediaStore: `app/src/main/java/com/toolbox/everyday/magnifier/MagnifierScreen.kt`
- WiFi: `app/src/main/java/com/toolbox/measurement/wifisignal/WifiSignalScreen.kt`
