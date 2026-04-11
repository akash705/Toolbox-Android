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

## Session Log
- 2026-04-11: Initial ideation — 40 ideas generated across 5 frames, ~28 after dedupe, 7 survivors + full tool inventory. User confirmed comprehensive toolbox direction. Moving to brainstorm.
