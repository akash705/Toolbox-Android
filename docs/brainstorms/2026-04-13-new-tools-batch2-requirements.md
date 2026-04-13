---
date: 2026-04-13
topic: new-tools-batch2
---

# New Tools — Batch 2

## Problem Frame

Toolbox is a free, offline, no-ads Android utility app with 25+ tools. Users expect a comprehensive toolbox — gaps like "no calculator" or "no document scanner" reduce perceived completeness. This batch adds 7 high-impact tools across new and existing categories to fill obvious gaps and introduce unique capabilities (NFC, frequency generation) that differentiate from competitors.

## Requirements

**Loan/EMI Calculator**
- R1. User enters loan amount, interest rate, and tenure (months or years)
- R2. Display monthly EMI, total interest, and total payment
- R3. Show pie chart breaking down principal vs. interest
- R4. Show full amortization schedule table (month, principal, interest, balance)
- R5. Support both reducing balance and flat-rate calculation methods

**Device Info**
- R6. Display device model, manufacturer, Android version, API level, kernel version
- R7. Show display specs: resolution, density, refresh rate, HDR support
- R8. Show storage breakdown (used/free for internal and external storage)
- R9. Show RAM (total/available), CPU architecture, core count
- R10. Show battery health, charging status, and temperature; show capacity (mAh) where available via system files (display "N/A" on unsupported devices)
- R11. Show sensor list with vendor and version info

**Network Info**
- R12. Show current connection type (Wi-Fi, mobile data, none)
- R13. For Wi-Fi: SSID, BSSID, signal strength (dBm), frequency, link speed, channel
- R14. Show IP addresses (local IPv4/IPv6 and public IP if available — note: public IP requires network access)
- R15. Show mobile network info: carrier, network type (4G/5G), signal strength
- R16. Support copy-to-clipboard for any displayed value

**NFC Toolkit**
- R17. Read NFC tags and display content (URL, text, contact, raw NDEF records)
- R18. Write user-provided data (text, URL, contact) to writable NFC tags
- R19. Show tag technical details: tag type (NDEF, MIFARE, etc.), UID, memory size, read-only status
- R20. Format tags to NDEF
- R21. Erase/wipe tag content
- R22. Display hex dump of raw tag data
- R23a. Display confirmation dialog before destructive operations (format, erase, write) with clear warning about data loss

**Frequency/Tone Generator**
- R23. Generate a continuous tone at user-selected frequency (20 Hz – 20,000 Hz) using user-selected waveform (see R26)
- R24. Frequency selection via slider and precise numeric input
- R25. Preset buttons: high frequency (18 kHz), standard tuning (440 Hz A4), common musical notes (C4-B4)
- R26. Support waveform selection: sine, square, triangle, sawtooth
- R27. In-app volume slider that scales AudioTrack gain (note: overall output is still bounded by system media volume)
- R28. Visual frequency display (waveform preview)

**Formula Reference & Solver**
- R29. Browse formulas organized by subject: Math (algebra, geometry, trigonometry, calculus), Physics (mechanics, optics, thermodynamics, electricity), Chemistry (stoichiometry, gas laws, solutions), Finance (compound interest, ROI, break-even)
- R30. Each formula displayed with clear variable labels and the mathematical expression
- R31. Interactive solver: tap a formula, enter known values, solve for any unknown variable
- R32. Search across all formulas by name or keyword
- R33. Favorite formulas for quick access
- R34. Solver shows step-by-step substitution so users can verify the calculation

**Document Scanner**
- R35. Camera-based document capture with edge detection and auto-crop
- R36. Manual corner adjustment after capture
- R37. Image enhancement filters: grayscale, high contrast, black & white, original
- R38. Save scanned documents as images (JPEG/PNG) to gallery
- R39. Multi-page scanning — combine multiple captures into one document
- R40. Save multi-page scans as PDF

## Success Criteria

- Each tool works fully offline (except R14 public IP which gracefully degrades; NFC Toolkit requires NFC hardware and is hidden on devices without it)
- Tools follow existing app patterns: Material 3, Compose, share measurement cards where applicable
- NFC Toolkit handles devices without NFC hardware gracefully (show message, hide from dashboard)
- Frequency generator produces clean audio without clicks or artifacts at frequency transitions
- Document scanner edge detection works on common document shapes (A4, letter, receipts, cards)
- Document scanner shows an actionable error if camera permission is denied or no rear camera is available

## Scope Boundaries

- No cloud storage or sync — all data stays on device
- No OCR for document scanner (text recognition) — out of scope for this batch
- No currency conversion (requires online data)
- No interactive graphing or plotting in Formula Solver
- No Wi-Fi speed test (requires sustained network access, conflicts with offline positioning)
- NFC tag cloning limited to writable tags — no cracking read-only/encrypted tags

## Key Decisions

- **Charts in Loan/EMI**: Use Compose Canvas for pie chart and amortization table rather than adding a charting library — keeps the app lean and dependency-free
- **NFC full toolkit**: Go comprehensive since NFC toolkits are rare in free apps — strong differentiator
- **Formula Reference replaces Scientific Calculator**: Scientific Calculator already exists in the app — Formula Reference & Solver fills a different, complementary gap
- **Formula solver shows work**: Step-by-step substitution builds trust and helps students learn
- **PDF export for scanner**: Include PDF support since multi-page scans without PDF defeat the purpose

## Dependencies / Assumptions

- NFC requires `android.permission.NFC` and `android.hardware.nfc` — permission and feature must be declared; feature uses `android:required="false"` to not filter out non-NFC devices from Play Store
- R14 public IP fetch requires `android.permission.INTERNET` (normal permission, no runtime prompt needed)
- Frequency generator uses `AudioTrack` API for low-latency tone generation
- Document scanner edge detection can use OpenCV Android or a lighter custom approach with Compose Canvas
- PDF generation can use Android's `PdfDocument` API (no external library needed)
- Refresh rate (R7) uses `Display.getSupportedModes()` — available API 23+; HDR support via `Display.getHdrCapabilities()` available API 24+

## Outstanding Questions

### Resolve Before Planning
(none)

### Deferred to Planning
- [Affects R35][Needs research] Best approach for edge detection — OpenCV Android vs. custom heuristic with contour detection (ML Kit requires Play Services, violates offline constraint; OpenCV adds ~30 MB — weigh against dependency-free principle)
- [Affects R23][Technical] AudioTrack buffer size and sample rate for artifact-free frequency sweeps; define transition behavior (immediate on slider release vs. continuous retune)
- [Affects R17-R22][Technical] NFC foreground dispatch vs. reader mode API — which gives better UX for continuous scanning in single-Activity Compose architecture
- [Affects R6-R11][Technical] Which device info fields require runtime permissions on Android 12+; confirm StatFs suffices for R8 external storage without MANAGE_EXTERNAL_STORAGE
- [Affects R13][Technical] SSID/BSSID requires ACCESS_FINE_LOCATION (API ≤ 32) or NEARBY_WIFI_DEVICES (API 33+) — define graceful degradation when permission denied
- [Affects R15][Technical] Mobile carrier/signal info requires READ_PHONE_STATE — define fallback when permission denied
- [Affects R12-R16][User decision] Should existing WifiSignal tool be retired, merged into Network Info, or kept as separate tool?
- [Affects R38, R40][Technical] Storage strategy: use MediaStore for gallery images, MediaStore Downloads or SAF picker for PDFs on API 29+
- [Affects R14][Technical] Define fallback UI when public IP fetch fails — show "Unavailable" with retry button; specify timeout
- [Affects R5][Technical] Reducing balance should be default method; switching methods recalculates immediately

## Next Steps

-> `/ce:plan` for structured implementation planning
