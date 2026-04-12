---
date: 2026-04-11
topic: android-toolbox-app
---

# Toolbox — Android Utility App

## Problem Frame

Phone users regularly need quick utility tools — measuring angles, converting units, scanning QR codes, toggling a flashlight — and either install separate single-purpose apps for each or dig through bloated, ad-heavy toolbox apps with poor UX. There's a gap for a well-designed, comprehensive, privacy-respecting toolbox built with modern Android standards.

Target users: general Android users, DIYers, students, travelers, and anyone who wants a single reliable utility app on their home screen.

## Requirements

**App Shell & Navigation**

- R1. Grid dashboard home screen with categorized tool tiles (icon + label). Categories: Measurement & Sensors, Conversion & Calculation, Lighting & Display, Everyday Tools. Tools whose required sensor is absent on the device are shown as disabled with a brief explanation on tap (not hidden, so the grid layout stays stable).
- R2. Search bar at the top of the dashboard that filters tools by name and keywords in real time.
- R3. Tapping a tool tile navigates to a full-screen tool view. Back navigation returns to the dashboard. Single Activity architecture with standard Navigation Compose.
- R4. Material 3 / Material You design system with dynamic color theming from the user's wallpaper (Android 12+), with a static Material 3 color scheme fallback on older devices.
- R5. Light and dark theme support, following system preference by default with manual override.
- R6. Offline-first architecture. Every MVP tool works without internet. No INTERNET permission in MVP manifest.
- R7. Single-module project with package-level organization (one package per tool category, plus a shared `core` package for sensor/camera utilities). Revisit modularization only when build times or team size demand it.

**Privacy & Permissions**

- R8. Zero-permission cold start. Tools that don't need hardware (converters, calculators, counter, random) request no permissions at install or launch.
- R9. Sensor/camera/mic tools request their specific runtime permission only when the user taps that tool for the first time.
- R10. No analytics, tracking, or data collection in MVP. No INTERNET permission in manifest.
- R11. Timer (R25) requires FOREGROUND_SERVICE and POST_NOTIFICATIONS (runtime on Android 13+). These are declared in manifest but POST_NOTIFICATIONS is requested only when user first starts a timer. FOREGROUND_SERVICE is a normal permission (auto-granted, not shown as dangerous on Play Store).

**State Persistence**

- R12. Counter/Tally: named counters and their values persist across sessions (Room or DataStore).
- R13. Unit Converter: favorite/recent conversions persist across sessions.
- R14. Stopwatch/Timer: running timer survives process death via foreground service. Stopwatch resets on process death (acceptable for MVP).
- R15. All other tools: no cross-session state persistence required in MVP.

**MVP Tools — Measurement & Sensors (5 tools)**

- R16. **Bubble Level** — Uses accelerometer. Shows a circular bubble level and a linear (horizon) level. Displays degrees of tilt. Supports lock/hold reading.
- R17. **Compass** — Uses magnetometer + accelerometer. Shows cardinal directions, bearing in degrees. Defaults to magnetic north. True north toggle available as opt-in — triggers ACCESS_COARSE_LOCATION permission request to compute magnetic declination.
- R18. **Protractor / Angle Meter** — Uses accelerometer to measure angle of inclination. Camera-based angle measurement deferred to Phase 2 (accelerometer mode alone delivers core value).
- R19. **Sound Meter** — Uses microphone. Shows real-time decibel level with a visual gauge. Displays min/max/avg readings for the current session. Exposure tracking deferred to Phase 2.
- R20. **On-Screen Ruler** — Displays a ruler on screen in cm and inches. Calibration via credit card reference object (user places a card on screen and adjusts).

**MVP Tools — Conversion & Calculation (4 tools)**

- R21. **Unit Converter** — Supports categories: length, weight/mass, volume, temperature, speed, area, time, data/storage, pressure, energy. Two-field input (type in either field to convert). Favorite/recent conversions pinned at top.
- R22. **Percentage Calculator** — Three modes: "X% of Y", "X is what % of Y", "% change from X to Y".
- R23. **Tip Calculator** — Enter bill amount, select tip percentage (preset buttons + custom), split between N people. Shows per-person total.
- R24. **Number Base Converter** — Convert between binary, octal, decimal, hexadecimal. Live conversion as you type.

**MVP Tools — Lighting & Display (1 tool)**

- R25. **Flashlight** — Toggle camera flash. SOS mode (auto-blinks Morse SOS). Strobe mode with adjustable frequency (minimum 50ms interval, runs on background coroutine). Brightness slider shown only on devices running Android 13+ with hardware support; hidden otherwise.

**MVP Tools — Everyday Tools (6 tools)**

- R26. **QR / Barcode Scanner + Generator** — Camera-based scanner using ZXing (fully offline, small footprint, no INTERNET permission needed). Detects QR codes, barcodes (UPC, EAN, Code 128). Scan results shown with action buttons (open URL, copy text, share). Generator: input text/URL, select format, generate downloadable QR or barcode image. This is one tool with two tabs (Scan / Generate), counted as 1 tool.
- R27. **Counter / Tally** — Large tap target to increment. Decrement button. Reset with confirmation. Haptic feedback on tap. Supports multiple named counters with persistent state (R12).
- R28. **Stopwatch & Timer** — Stopwatch with lap tracking. Timer with preset durations (1m, 3m, 5m, 10m) and custom input. Timer runs as a foreground service with notification; notification permission requested on first timer start (R11).
- R29. **Random / Coin Flip / Dice** — Three modes in one tool: random number generator (configurable range), coin flip (with animation), dice roll (1-6 dice, with roll animation).
- R30. **Color Picker** — Camera viewfinder with crosshair. Samples an 11x11 pixel region around center for stable color reading. Displays hex, RGB, and HSL values. Tap to freeze/capture. Copy color value to clipboard. Color is sampled from the processed preview frame (what the user sees on screen).
- R31. **Magnifier** — Camera-based zoom. Pinch to zoom or slider control. Toggle flashlight as fill light. Freeze frame button.

**Phase 2 Roadmap (not in MVP scope)**

- R32. Phase 2 tools: Screen Dimmer (requires SYSTEM_ALERT_WINDOW — deferred from MVP to preserve zero-dangerous-permission Play Store listing), Metal Detector, Speedometer, Barometer/Altitude, Currency Converter, BMI Calculator, Age/Date Calculator, WiFi QR Sharer, Network Speed Test, Battery Info, Device Info, Dead Pixel Tester, Touch Tester, Metronome, Scratchpad.
- R33. Phase 2 meta-features: Widget Dock (per-tool home screen widgets), Project Clipboard (auto-save tool outputs to named projects), Sound Meter exposure tracking, Protractor camera mode.

**Quality & Polish**

- R34. Haptic feedback on primary interactions (counter tap, coin flip, dice roll, button presses).
- R35. Smooth transitions between dashboard and tool screens using Compose shared element transitions or predictive back gestures.
- R36. Minimum SDK 26 (Android 8.0) for broad device coverage. Target SDK latest stable.
- R37. App size under 15 MB APK. ZXing (~0.5 MB) over ML Kit (~3-5 MB) supports this target.

## Success Criteria

- All 16 MVP tools (R16-R31) functional and tested on at least 2 physical devices
- Play Store listing shows only normal permissions (FOREGROUND_SERVICE) — no dangerous permissions at install. POST_NOTIFICATIONS requested at runtime only.
- Dashboard search filters tools correctly by name and keyword
- Material You dynamic color works on Android 12+ devices, graceful fallback on older
- App launches to dashboard in under 1 second on mid-range device
- Ready for Play Store listing (icons, screenshots, description, privacy policy)

## Scope Boundaries

- No monetization, ads, or in-app purchases in MVP
- No user accounts or cloud sync
- No social features or sharing (except QR code image export)
- No custom themes beyond Material You + light/dark
- Screen Dimmer deferred to Phase 2 (SYSTEM_ALERT_WINDOW conflicts with zero-permission Play Store listing)
- Protractor camera mode deferred to Phase 2
- Sound Meter exposure tracking deferred to Phase 2
- Widget Dock and Project Clipboard deferred to Phase 2
- SolarSight (sun/shadow planner) deferred to Phase 2 or later

## Key Decisions

- **Grid dashboard with search**: Familiar pattern for toolbox apps, search handles discoverability as tool count grows
- **Material 3 / Material You**: Modern, reduces custom design work, dynamic color is a delight feature
- **16 tools for MVP**: Covers all four categories with breadth, shippable scope
- **Zero-permission architecture from day one**: Structural decision that's hard to retrofit — must be baked into the architecture
- **Screen Dimmer moved to Phase 2**: SYSTEM_ALERT_WINDOW would appear in Play Store listing, undermining the zero-permission trust story
- **ZXing over ML Kit**: Fully offline, no INTERNET permission, ~0.5 MB vs ~3-5 MB, stays within 15 MB APK target
- **Single module, package-level organization**: Multi-module is over-engineering for a solo-developer MVP with 16 tools
- **Single Activity + Navigation Compose**: Standard modern Compose pattern; multi-Activity is a legacy approach
- **No monetization in MVP**: Reduces complexity, focus on quality and organic growth first
- **Offline-first with no INTERNET permission**: Utility apps must work anywhere; no permission means maximum trust

## Dependencies / Assumptions

- ZXing for QR/barcode scanning (fully offline, small footprint)
- CameraX for camera-based tools (magnifier, color picker, QR scanner)
- Device has standard sensors (accelerometer, magnetometer, microphone) — absent tools shown as disabled on dashboard, not hidden

## Outstanding Questions

### Deferred to Planning
- [Affects R20][Needs research] Best approach for screen DPI calibration for the on-screen ruler — credit card reference sizing UX
- [Affects R35][Technical] Specific Compose animation APIs for shared element transitions between dashboard tiles and tool screens

## Next Steps

→ `/ce:plan` for structured implementation planning
