---
date: 2026-04-12
topic: android-toolbox-phase3-features
---

# Toolbox — Phase 3 Feature Requirements

## Context

These tools extend the MVP (16 tools) and Phase 2 backlog with new capabilities that maintain the app's core principles: offline-first, privacy-respecting, zero unnecessary permissions. Each tool is categorized and specced to the same standard as the MVP requirements.

---

## New Tools — Practical & High-Value (5 tools)

### R40. Text Scanner (OCR)

Camera-based text extraction using ML Kit's on-device text recognition (no internet required).

- Camera viewfinder with real-time text detection overlay (bounding boxes around detected text blocks)
- Tap to capture and extract text from the frozen frame
- Extracted text shown in a scrollable, selectable text area below the preview
- Action buttons: Copy All, Share, Select (allows partial text selection)
- Supports Latin script. Non-Latin script support deferred — document as a known limitation
- **Permission:** CAMERA (requested on first tap, consistent with existing camera tools)
- **Dependency:** ML Kit Text Recognition v2 on-device model (~5 MB). This is the key trade-off — increases APK size. Bundle as a separate dynamic feature module if APK size exceeds 20 MB target
- **Note:** No INTERNET permission needed — ML Kit on-device runs fully offline

### R41. Mirror

Front-facing camera preview displayed full-screen as a simple mirror.

- Uses CameraX front camera preview (already a dependency for other tools)
- Full-screen viewfinder, no UI chrome except a minimal top bar with back navigation
- Freeze frame button (bottom center) — taps to pause/resume the preview
- Brightness boost toggle — temporarily sets screen brightness to maximum while mirror is active, restores on exit
- No image capture, no storage, no gallery — this is purely a live preview
- **Permission:** CAMERA (shared with existing camera tools, already requested if user has used color picker/magnifier/QR scanner)

### R42. White Noise Generator

Procedurally generated ambient sounds for focus and sleep.

- Sound options: White Noise, Brown Noise, Pink Noise, Rain, Fan, Ocean Waves
- Rain/Fan/Ocean use short (~5 second) loopable audio samples bundled in assets (total ~500 KB). White/Brown/Pink noise generated procedurally via `AudioTrack` with appropriate spectral filtering
- Play/pause button (large, centered)
- Volume slider (independent of system volume — multiplied against system media volume)
- Sleep timer: auto-stop after 15m, 30m, 1h, 2h, or custom duration
- Runs as a foreground service with notification controls (play/pause/stop) so audio continues when app is backgrounded
- **Permission:** FOREGROUND_SERVICE (already declared for timer). FOREGROUND_SERVICE_MEDIA_PLAYBACK service type required on Android 14+
- **No microphone, no internet required**

### R43. Aspect Ratio Calculator

Calculate dimensions for common and custom aspect ratios.

- Input: width OR height, plus selected ratio
- Output: the other dimension, calculated instantly as user types
- Preset ratios: 16:9, 4:3, 1:1, 21:9, 3:2, 9:16, 2:3 (portrait variants shown separately)
- Custom ratio input: two fields for W:H
- Swap button to flip width/height
- Results display: both dimensions, total pixel count (W x H), and megapixel equivalent
- Copy result to clipboard
- **Permission:** None
- **Complexity:** Low — pure math, minimal UI

### R44. Vibration Patterns

Custom vibration patterns for silent alerts and fun.

- Preset patterns: Heartbeat, SOS, Pulse, Escalating, Rhythmic
- Custom pattern builder: tap a button rhythmically to record a pattern (captures on/off timing), then replay
- Play/stop controls with haptic feedback
- Repeat toggle (loop pattern continuously)
- Uses `Vibrator` / `VibrationEffect` API (Android 8.0+ baseline already met)
- **Permission:** VIBRATE (normal permission, auto-granted, no user prompt)

---

## New Tools — DIY / Maker (3 tools)

### R45. Screw & Bolt Reference

Interactive hardware size reference chart.

- Two tabs: Metric (M2 through M24) and Imperial (#0 through 1")
- Each entry shows: nominal diameter, thread pitch (mm or TPI), head sizes (hex, Phillips), recommended drill bit size, and clearance hole size
- Search/filter by size
- Visual scale diagram: on-screen representation of bolt size (calibrated using the same credit card reference approach as the on-screen ruler, R20 — share the calibration value)
- Tap any entry to see detailed specifications in a bottom sheet
- **Permission:** None
- **Complexity:** Low — static data, no sensors

### R46. Wire Gauge Reference

AWG and SWG wire gauge lookup with ampacity tables.

- Gauge selector: scrollable list or number picker (AWG 0000 through 40)
- For each gauge: diameter (mm and inches), cross-section area (mm^2), resistance (ohms/km at 20C), max ampacity for chassis wiring and power transmission
- Material toggle: Copper / Aluminum (adjusts resistance and ampacity)
- **Permission:** None
- **Complexity:** Low — static reference data

### R47. Paint & Wallpaper Calculator

Room coverage calculator for paint and wallpaper.

- Input: room dimensions (length, width, height) with unit toggle (ft/m)
- Subtract openings: add doors (preset 80x36" / 203x91cm) and windows (preset 48x36" / 122x91cm) with adjustable dimensions and quantity
- Output for paint: total wall area, number of coats selector (1-3), paint needed in gallons/liters (assumes 350 sq ft / 32.5 sq m per gallon)
- Output for wallpaper: number of standard rolls (assumes 56 sq ft / 5.2 sq m per roll with 15% waste factor)
- Coverage rate is editable (different paints have different coverage)
- **Permission:** None
- **Complexity:** Low — arithmetic with a form UI

---

## New Tools — Student & Education (3 tools)

### R48. Scientific Calculator

Full scientific calculator beyond the stock Android calculator.

- Standard mode: basic arithmetic (matches stock calculator)
- Scientific mode (default): sin, cos, tan (and inverses), log, ln, e^x, x^y, sqrt, nth root, factorial, abs, pi, e
- Angle mode toggle: Degrees / Radians (persistent across sessions via DataStore)
- Expression input: shows full expression as typed (not just running result), evaluates on `=`
- Calculation history: scrollable list of previous expressions and results for the current session. Tap to reuse a result
- Parentheses support with visual nesting indicators
- Landscape mode: shows extended function grid
- **Permission:** None
- **Note:** Use a well-tested expression parser. Recommended: mXparser (open source, ~200 KB, handles operator precedence and functions correctly). Do NOT hand-roll expression parsing
- **Complexity:** Medium — expression parsing is the main challenge

### R49. Unit Circle Reference

Interactive trigonometry unit circle.

- Full-screen unit circle diagram with standard angles marked (0, 30, 45, 60, 90... through 360 degrees)
- Tap any angle to highlight it and show: degrees, radians, sin, cos, tan values (exact fractions where applicable, e.g., sqrt(3)/2)
- Drag finger around the circle to see values update continuously for any angle
- Toggle between: degrees/radians labels, exact/decimal values
- Color-coded quadrants with sign indicators (+/-)
- **Permission:** None
- **Complexity:** Medium — custom Compose Canvas drawing

### R50. Morse Code Translator

Bidirectional text-to-Morse translation with multi-modal output.

- Text to Morse tab: type text, see Morse code (dots and dashes) below in real time
- Morse to Text tab: tap two buttons (dot / dash) with a space/letter-break button, see decoded text above
- Output modes (toggleable, can combine):
  - Visual: flashing screen or animated dot/dash display
  - Audio: tone beeps at standard Morse timing (dit = 1 unit, dah = 3 units, inter-element gap = 1 unit, inter-letter = 3 units, inter-word = 7 units)
  - Haptic: vibration pattern matching the audio timing
  - Flashlight: camera flash blinks (pairs with existing flashlight tool's SOS mode)
- Playback speed control: WPM (words per minute) slider, 5-25 WPM
- Copy Morse/text to clipboard
- **Permission:** CAMERA only if flashlight output is activated (requested on toggle, not on tool open). VIBRATE (normal, auto-granted)
- **Complexity:** Medium — Morse encoding is trivial, timing and multi-modal playback need careful coroutine management

---

## New Tools — Health & Wellness (2 tools)

### R51. Breathing Exercise

Guided breathing patterns with visual animation.

- Preset patterns:
  - Box Breathing: 4s inhale, 4s hold, 4s exhale, 4s hold
  - 4-7-8: 4s inhale, 7s hold, 8s exhale
  - Relaxing: 4s inhale, 6s exhale (no hold)
  - Custom: user sets inhale/hold/exhale/hold durations (1-15 seconds each)
- Visual: expanding/contracting circle animation synchronized to the current phase. Phase label ("Inhale", "Hold", "Exhale") displayed inside the circle
- Optional audio cue: gentle tone at phase transitions (uses system media volume). Toggle on/off
- Optional haptic pulse at phase transitions
- Session duration: number of cycles (3, 5, 10, custom) or continuous until stopped
- Session summary on completion: total time, cycles completed
- **Permission:** None
- **Complexity:** Low — animation + timers

### R52. Lap Counter

Dedicated lap/set counter with per-lap timing.

- Large "LAP" button (full bottom half of screen) — taps to record a lap
- Running total: current lap number, total elapsed time, current lap time
- Lap list: scrollable table showing lap number, lap time, cumulative time, and delta from average (+ or - colored)
- Best/worst lap highlighted in the list
- Reset with confirmation dialog
- Haptic feedback on lap tap
- **Distinction from Stopwatch (R28):** Stopwatch tracks total time with optional laps as a secondary feature. Lap Counter is optimized for repetitive counting (swimming, running, gym sets) — the lap tap is the primary interaction, and per-lap analytics are front and center
- **Permission:** None
- **Complexity:** Low

---

## New Tools — Utility & Quality of Life (3 tools)

### R53. Screen Protector Alignment Grid

On-screen guide for aligning screen protectors.

- Displays edge guidelines: thin lines inset 1-2mm from each screen edge (adjustable via slider)
- Center crosshair for orientation
- Corner guides: L-shaped markers at each corner showing exact alignment points
- Notch/punch-hole camera cutout indicator (reads display cutout info via `WindowInsets` API)
- Screen brightness set to maximum while tool is active (makes lines visible under the protector)
- Grid color toggle: white lines on black / black lines on white
- Full-screen immersive mode (hides status bar and nav bar)
- **Permission:** None
- **Complexity:** Low

### R54. Parking Timer

Simple countdown timer with notification for parking meter expiry.

- Preset durations: 15m, 30m, 1h, 2h, 3h
- Custom duration input
- Large countdown display
- Notification at expiry (uses existing notification permission from timer tool, R11)
- Warning notification at 5 minutes remaining
- Optional: save parking location (tapping "Save Location" opens Google Maps intent with current coordinates — requires ACCESS_FINE_LOCATION, requested only on tap)
- **Permission:** POST_NOTIFICATIONS (shared with timer, R11). ACCESS_FINE_LOCATION only if user opts into save-location feature
- **Complexity:** Low — reuses foreground service infrastructure from Stopwatch/Timer (R28)

### R55. Text-to-Speech Reader

Paste or type text and hear it read aloud.

- Large text input area (paste or type)
- Play / Pause / Stop controls
- Voice selection: lists available TTS voices installed on device (via `TextToSpeech.getVoices()`)
- Speed slider: 0.5x to 2.0x
- Pitch slider: 0.5x to 2.0x
- Highlights current sentence/word being spoken (using `UtteranceProgressListener` range callbacks)
- Uses Android's built-in `TextToSpeech` engine — no additional dependencies, no internet
- **Permission:** None (TTS engine is a system service)
- **Complexity:** Low-Medium — TTS API is straightforward, word-level highlighting requires careful callback handling

---

## Dashboard Category Updates

With 16 new tools, update dashboard categories:

| Category | MVP Tools | New Additions |
|----------|-----------|---------------|
| Measurement & Sensors | 5 | — |
| Conversion & Calculation | 4 | Aspect Ratio Calculator (R43), Scientific Calculator (R48) |
| Lighting & Display | 1 | — |
| Everyday Tools | 6 | Text Scanner (R40), Mirror (R41), Morse Code (R50), Screen Protector Grid (R53), Parking Timer (R54), TTS Reader (R55) |
| **NEW: Health & Wellness** | — | Breathing Exercise (R51), Lap Counter (R52), White Noise (R42) |
| **NEW: DIY & Reference** | — | Screw & Bolt (R45), Wire Gauge (R46), Paint Calculator (R47), Unit Circle (R49) |
| **NEW: Fun & Fidget** | — | Vibration Patterns (R44) |

Consider merging "Fun & Fidget" into "Everyday Tools" if a single-tool category feels sparse. The Health & Wellness and DIY & Reference categories each have 3+ tools, justifying their own section.

---

## Permission Impact

| Permission | Type | Trigger | Tools |
|------------|------|---------|-------|
| CAMERA | Dangerous | On tool tap | R40 (OCR), R41 (Mirror), R50 (Morse flashlight mode) — shared with existing R26, R30, R31 |
| VIBRATE | Normal | Auto-granted | R44 (Vibration), R50 (Morse haptic) — shared with existing R27, R29 |
| FOREGROUND_SERVICE | Normal | Auto-granted | R42 (White Noise) — shared with existing R28 |
| ACCESS_FINE_LOCATION | Dangerous | On opt-in tap | R54 (Parking location only) |
| POST_NOTIFICATIONS | Dangerous (13+) | On first use | R54 (Parking Timer) — shared with existing R28 |
| None | — | — | R43, R45, R46, R47, R48, R49, R51, R52, R53, R55 |

**12 of 16 new tools require zero permissions.** The zero-permission-first story remains strong.

---

## APK Size Impact

| Addition | Estimated Size |
|----------|---------------|
| ML Kit Text Recognition (on-device) | ~5 MB |
| mXparser (scientific calc) | ~200 KB |
| Audio samples (white noise) | ~500 KB |
| Static data (screw/wire tables) | ~50 KB |
| UI code for 16 tools | ~1-2 MB |
| **Total estimated increase** | **~7-8 MB** |

With MVP at ~10-12 MB, Phase 3 would push to ~18-20 MB. If this exceeds comfort zone, ML Kit can be delivered as a Play Feature Delivery module (downloaded on first OCR use, not at install).

---

## Priority Ranking

Ordered by value-to-effort ratio:

1. **Breathing Exercise (R51)** — Zero permissions, low complexity, high retention, wellness category opener
2. **Scientific Calculator (R48)** — Zero permissions, universally useful, fills obvious gap
3. **Morse Code Translator (R50)** — Leverages existing flashlight infra, educational + fun
4. **Text Scanner / OCR (R40)** — High perceived value, camera infra exists, but APK size cost
5. **Parking Timer (R54)** — Reuses existing timer/notification infra, solves real problem
6. **White Noise Generator (R42)** — High retention (daily use), but needs foreground service work
7. **Paint Calculator (R47)** — Zero permissions, practical, simple math
8. **Aspect Ratio Calculator (R43)** — Trivial to build, useful for creators
9. **Mirror (R41)** — Trivial to build with existing CameraX, surprisingly popular
10. **Lap Counter (R52)** — Simple, pairs with existing stopwatch
11. **Screen Protector Grid (R53)** — Minimal code, surprisingly useful niche
12. **TTS Reader (R55)** — Uses system APIs, no dependencies
13. **Vibration Patterns (R44)** — Fun, minimal effort
14. **Screw & Bolt Reference (R45)** — Static data, niche but valuable for DIYers
15. **Unit Circle Reference (R49)** — Custom drawing, student niche
16. **Wire Gauge Reference (R46)** — Very niche, but trivial to build

---

## Next Steps

- Decide which tools to include in Phase 3 vs defer further
- Update dashboard category structure if adding Health & Wellness / DIY categories
- Evaluate ML Kit APK size impact — prototype and measure before committing to OCR
- Plan implementation units following the same phased approach as MVP
