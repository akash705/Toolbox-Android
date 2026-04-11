---
date: 2026-04-11
topic: android-toolbox-app
focus: comprehensive utility toolbox for Android, Play Store target, Kotlin + Jetpack Compose
---

# Ideation: Android Toolbox App

## Codebase Context
- Greenfield project — empty repository
- Target: Play Store publication
- Stack: Kotlin + Jetpack Compose
- Concept: comprehensive multi-tool utility app (level, converter, flashlight, and many more)

## Ranked Ideas

### 1. Widget Dock — Per-Tool Home Screen Widgets
**Description:** Every tool in the app can be pinned as a standalone, resizable home screen widget that deep-links directly to that tool — not the app's home screen.
**Rationale:** The #1 reason people abandon toolbox apps is that the tool is always 2+ taps away. Widgets make it zero taps. Differentiates from competitors where widgets either don't exist or just open the main menu.
**Downsides:** Each widget type requires its own `AppWidgetProvider`. Some tools (like level) are hard to preview in widget form.
**Confidence:** 90%
**Complexity:** Medium
**Status:** Explored — selected for brainstorm

### 2. Sound Meter with Exposure Tracking (Decibel Diary)
**Description:** Real-time decibel meter with cumulative daily noise exposure logging against WHO/OSHA thresholds. Warns when approaching unsafe levels.
**Rationale:** Transforms a forgettable tool into a health tool people check daily — drives retention. Apple added hearing health features; Android has no native equivalent.
**Downsides:** Mic calibration varies across devices. Continuous mic access drains battery. Must disclaim non-medical.
**Confidence:** 80%
**Complexity:** Medium
**Status:** Explored — selected for brainstorm

### 3. Magnetometer Stud / Metal Finder
**Description:** Uses phone's magnetometer to detect ferrous materials behind walls. Live field-strength graph with audio pitch feedback. Includes calibration guide.
**Rationale:** High perceived value — replaces a physical tool. Every homeowner drills holes. Existing magnetometer apps are visually primitive.
**Downsides:** Sensitivity varies by device. Can't detect non-ferrous materials. Must manage expectations.
**Confidence:** 78%
**Complexity:** Medium
**Status:** Explored — selected for brainstorm

### 4. Project Clipboard (Job Notepad)
**Description:** Persistent scratchpad tied to named projects where measurements, converted values, and notes auto-accumulate as you use tools.
**Rationale:** #1 workaround in competing app reviews is "I wish I could save my measurements." Creates stickiness and a reason to return.
**Downsides:** State management complexity. Risk of scope creep into a notes app.
**Confidence:** 75%
**Complexity:** Medium
**Status:** Explored — selected for brainstorm

### 5. Zero-Permission Cold Start
**Description:** Every tool that doesn't need hardware works with zero permissions on first install. Camera/mic tools ask only when tapped.
**Rationale:** Privacy anxiety is the #1 reason users one-star utility apps. Leading with zero permissions is a trust moat.
**Downsides:** Architecture decision, not a visible feature. Less flashy.
**Confidence:** 85%
**Complexity:** Low
**Status:** Explored — selected for brainstorm

### 6. Blackout Shade / Screen Dimmer
**Description:** Overlays a near-black filter below hardware minimum brightness. Adjustable opacity, warm tint, notification toggle.
**Rationale:** Stock Android minimum brightness is too bright for dark rooms. Screen filter apps are top productivity downloads.
**Downsides:** Requires SYSTEM_ALERT_WINDOW. Some OEMs block overlay apps.
**Confidence:** 82%
**Complexity:** Low
**Status:** Explored — selected for brainstorm

### 7. Sun & Shadow Planner (SolarSight)
**Description:** GPS + compass to overlay sun's arc for any date/time on camera feed. Scrub through day to see shadow positions.
**Rationale:** Wow-factor feature no competing toolbox offers. Appeals to gardeners, photographers, solar panel buyers, real estate agents.
**Downsides:** Highest complexity. Requires solar position math, AR overlay, date scrubbing UI.
**Confidence:** 65%
**Complexity:** High
**Status:** Explored — selected for brainstorm

## Full Tool Inventory (Brainstorm Seed)

**Measurement & Sensors:** Bubble Level, Compass, Protractor, Sound Meter, Metal Detector, Ruler, Speedometer, Barometer/Altitude

**Conversion & Calculation:** Unit Converter, Currency Converter, Number Base Converter, Percentage Calculator, Tip Calculator, Age/Date Calculator, BMI Calculator

**Lighting & Display:** Flashlight (SOS/strobe), Screen Dimmer, Dead Pixel Tester, Camera Magnifier

**Hardware Utilities:** WiFi QR Sharer, Network Speed HUD, Battery Info, Device Info, Touch Tester

**Everyday Tools:** QR/Barcode Scanner+Generator, Counter/Tally, Stopwatch/Timer, Metronome, Random/Coin/Dice, Color Picker, Scratchpad

## Rejection Summary

| # | Idea | Reason Rejected |
|---|------|-----------------|
| 1 | Clipboard History/Purge/Timeline | Requires Accessibility Service; Play Store restricts heavily |
| 2 | Wi-Fi Password Lookup | Limited API access without root |
| 3 | Keep Screen Awake | Too simple; well-served by Quick Tiles |
| 4 | Permission Pulse | Android 12+ Privacy Dashboard covers this |
| 5 | Focus Lock / Focus Fence | Complex permissions; competitive with Digital Wellbeing |
| 6 | Reading Lens | Niche; requires overlay + accessibility |
| 7 | Precision Flatness Scanner | Questionable accuracy; too complex |
| 8 | Room Echo Mapper | Very niche; mic/speaker quality dependent |
| 9 | Vibration Analyzer | Industrial niche only |
| 10 | Indoor Pathfinder | Dead reckoning accuracy is terrible on phones |
| 11 | Paint Match Pro | Camera color accuracy unreliable |
| 12 | Camera-Based Measurement | Unreliable without LiDAR/ARCore |
| 13 | Measure Together | Complex networking for a utility app |
| 14 | Gauge Wizard | Too niche (electricians/plumbers) |
| 15 | KnotField | Reference library, not a tool |
| 16 | MixRight | Too niche for broad audience |
| 17 | SlopeSafe | Construction safety niche only |
| 18 | TradeCalc | Competitive with standalone currency apps |
| 19 | Tool of the Moment | Complex contextual logic for uncertain payoff |
| 20 | Reverse Unit Converter | Confusing UX for most users |
| 21 | Charge Guard | Too thin standalone; better as notification shortcut |

## Phase 6 Ideation: Post-MVP Feature Expansion

After completing all 16 MVP tools, a second ideation round focused on "what new features can we add" produced 48 raw ideas across 6 frames, ~25 after dedupe, 9 survivors. User cross-checked against their own wishlist and confirmed additions.

### 8. Vibrometer (Surface Vibration Analyzer)
**Description:** Uses accelerometer at high sample rate (SENSOR_DELAY_FASTEST) to detect and visualize surface vibrations — frequency, amplitude, waveform. Place phone on appliance/wall/vehicle to diagnose hums or rattles. Baseline comparison mode.
**Rationale:** Accelerometer hook already exists. Mechanics, HVAC techs, and homeowners diagnosing appliance vibrations have no lightweight tool. Android accelerometer supports 200Hz+ which resolves useful frequency data.
**Downsides:** Consumer accelerometer limits professional accuracy. Must set expectations.
**Confidence:** 70%
**Complexity:** Medium
**Status:** Unexplored — added to Phase 6 plan (Unit 22)

### 9. Mirror (True Mirror)
**Description:** Full-screen front camera feed as mirror. Toggle between standard mirror (flipped) and "true mirror" (unflipped, as others see you). Brightness boost, freeze-frame.
**Rationale:** People already use selfie camera as mirror. A dedicated tool adds true-mirror flip (no camera app offers this) plus brightness controls. CameraX front camera already supported. Low effort, high discoverability.
**Downsides:** Very simple — may feel like filler.
**Confidence:** 82%
**Complexity:** Low
**Status:** Unexplored — added to Phase 6 plan (Unit 23)

### 10. Ambient Light Meter (Lux Logger)
**Description:** Uses ambient light sensor (zero permissions) to display and log lux readings with rolling chart. Named presets (reading, office, grow light), threshold alerts, time-series export.
**Rationale:** Zero-permission differentiator preserved. No competing toolbox ships this. Fills gap between Sound Meter and Barometer. Plant care, office ergonomics, photography prep are strong use cases.
**Downsides:** Light sensor resolution varies by device. Less wow-factor.
**Confidence:** 80%
**Complexity:** Low
**Status:** Unexplored — added to Phase 6 plan (Unit 24)

### 11. Magnetic Field Detector
**Description:** 3-axis magnetic field strength display (µT) with metal detection mode. Arc gauge, audio pitch feedback, baseline calibration, directional analysis.
**Rationale:** From Phase 2 backlog. Magnetometer hook already exists. High perceived value — replaces physical tool.
**Downsides:** Sensitivity varies by device. Can't detect non-ferrous materials.
**Confidence:** 78%
**Complexity:** Medium
**Status:** Unexplored — added to Phase 6 plan (Unit 25)

### 12. Shareable Measurement Cards
**Description:** Export any reading as a branded visual card (PNG) — tool icon, value, timestamp, label. One-tap share via Android share sheet. Subtle app branding for organic discovery.
**Rationale:** Users already screenshot readings. Formalizing as designed cards creates organic word-of-mouth (Wordle-style viral mechanic). Every share is a distribution event.
**Downsides:** Design effort per tool. Risk of over-branding.
**Confidence:** 88%
**Complexity:** Low-Medium
**Status:** Unexplored — added to Phase 6 plan (Unit 26)

### 13. Quick Settings Tiles
**Description:** Android Quick Settings tiles for Flashlight, Sound Meter, Level, Timer. Flashlight tile toggles torch directly without opening app. Others deep-link to tool.
**Rationale:** Most underused Android integration in utility apps. Reduces activation cost to swipe-and-tap. Flashlight-from-lock-screen is genuine differentiator.
**Downsides:** Limited to ~4-6 tiles. Users must manually add them.
**Confidence:** 85%
**Complexity:** Low
**Status:** Unexplored — added to Phase 6 plan (Unit 27)

### 14. Unit Converter — Comprehensive Expansion
**Description:** Expand from 10 to 17+ categories: add Power, Force, Torque, Density, Fuel Economy, Angle, Frequency. Add missing units in existing categories (Imperial volumes, Stone, Carat, Nautical Mile, etc.).
**Rationale:** User identified coverage gaps. "Every type of unit" is a reasonable expectation for a toolbox converter.
**Downsides:** More units to maintain and test. Fuel Economy needs formula-based conversion.
**Confidence:** 90%
**Complexity:** Low
**Status:** Unexplored — added to Phase 6 plan (Unit 21)

## Phase 6 Rejection Summary

| # | Idea | Reason Rejected |
|---|------|-----------------|
| 1 | Clipboard History & Formatter | Already rejected — requires Accessibility Service |
| 2 | Smart Tape Measure (AR) | Already rejected — unreliable without ARCore/LiDAR |
| 3 | Pipe & Wire Angle Planner | Too niche (electricians only) |
| 4 | Cable & Wire Length Estimator | Too niche, scope creep |
| 5 | Packing & Weight Estimator | Scope creep into travel app |
| 6 | Night Sky Light Pollution Mapper | Very niche, questionable accuracy |
| 7 | Noise Annoyance Mapper | Complex spatial UI, needs location |
| 8 | Vibration Pattern Designer | Too niche (accessibility/dev) |
| 9 | Comparative Benchmarking | Requires server, privacy concerns |
| 10 | Trigger-Based Automations | Complex, competes with Tasker |
| 11 | Offline AI Measurement Assistant | Gemini Nano limited, very high complexity |
| 12 | Scenario Mode | Overlaps Session Journal, complex UI |
| 13 | Stud Finder (Acoustic) | Questionable accuracy on consumer mics |
| 14 | EMF Visualizer | Overlaps Metal Detector |
| 15 | Surface Vibration Detector (separate) | Merged into Vibrometer survivor |
| 16 | Clinometer / Slope % | Overlaps existing Protractor + Level |
| 17 | Heart Rate / PPG | Play Store health policy risk |
| 18 | Sun & Shadow Tracker | Already explored in prior ideation |
| 19 | Noise Dose Tracker | Already explored in prior ideation |
| 20 | Sensor Fusion Visualizer | Too technical for broad audience |
| 21 | Document Scanner | Well-served by Google Drive, Adobe Scan |
| 22 | Tool Chains | High complexity; better after Session Journal ships |
| 23 | Cross-Tool Session Journal | Deferred — strong idea but needs Phase 6 tools first |
| 24 | Live Dashboard Readings | Deferred — battery/sensor lifecycle concerns need resolution |
| 25 | Device Calibration Profiles | Deferred — strong idea, planned for future phase |

## Session Log
- 2026-04-11: Initial ideation — 40 ideas generated across 5 frames, ~28 after dedupe, 7 survivors + full tool inventory. User confirmed comprehensive toolbox direction. Moving to brainstorm.
- 2026-04-11: Phase 6 ideation — 48 ideas generated across 6 frames, ~25 after dedupe, 7 survivors. User cross-checked with personal wishlist, added Vibrometer and Mirror. Final: 9 feature items added to plan as Phase 6 (Units 21-27).
