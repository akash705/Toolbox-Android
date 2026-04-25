---
date: 2026-04-23
topic: new-tools-batch3
focus: "WhatsApp Status Saver-style tools — local-data/filesystem utilities"
---

# Ideation: New Tools Batch 3 — Local-Data Utilities

## Codebase Context

Android Toolbox app (Kotlin + Jetpack Compose + Material 3, min SDK 26, target 36). ~50 tools across `measurement/`, `conversion/`, `everyday/`, `lighting/`, `motion/`, plus `favorites/`, `tiles/`, `widgets/`. Single-module, package-per-feature. Shared infrastructure in `core/` for camera (CameraX 1.6+), sensors, sharing, persistence (DataStore), shortcuts, permissions.

**Hard constraints that shape ideation:**
- No network requests — fully offline.
- No analytics, no crash reporting, no ads.
- Runtime permissions requested lazily only when a tool is opened.
- Zero-permission cold start — launching the app grants nothing.

**Key leverage points:**
- Existing CameraX stack → image-based tools are cheap to add.
- Existing ZXing + QR generator → QR-extension tools are cheap.
- Existing `core/sharing` export → new tools can share measurement cards for free.
- SAF (Storage Access Framework) is the unlocked door for file-based tools under scoped storage.

**Gap observed:** Current tool set is almost entirely sensors + calculators. There is no "reach into local data and do something useful with it" category. The user's seed idea (WhatsApp Status Saver) points at that gap.

## Ranked Ideas

### 1. WhatsApp Status Saver
**Description:** SAF-based browser for `WhatsApp/Media/.Statuses/` (and WA Business equivalent). Grid preview of images/videos with one-tap save-to-gallery. Persisted SAF tree permission so the user grants access once.
**Rationale:** User's seed idea. Proven #1 utility-app category on Play Store. Zero-network, read-only SAF access — perfect fit for the app's privacy ethos. Likely the highest-downloads single feature of any tool in the app.
**Downsides:** Scoped-storage quirks across Android 10/11/12+; WA may change paths (WA Business uses a different root); needs clear UX for the one-time SAF permission grant.
**Confidence:** 90%
**Complexity:** Medium
**Status:** Unexplored

### 2. Duplicate Photo Finder
**Description:** Scan gallery for visually similar or identical photos using perceptual hashing (pHash/dHash) on-device. Group duplicates, show side-by-side compare, let user delete selected or keep best-quality.
**Rationale:** Massive user pain point — every phone gallery has thousands of near-duplicates. pHash runs locally in milliseconds per image. Strong differentiator vs. competitors that upload photos to cloud for matching.
**Downsides:** `READ_MEDIA_IMAGES` permission draws Play Store scrutiny; scan time on 10k+ galleries requires progress UI + cancellability; false-positive tuning needed.
**Confidence:** 85%
**Complexity:** Medium-High
**Status:** Unexplored

### 3. Document Scanner (Multi-page → PDF)
**Description:** Camera-based document capture with auto edge detection → perspective correction → grayscale/B&W filter → multi-page PDF export. Uses Google ML Kit Document Scanner API (fully on-device).
**Rationale:** One of the most-asked-for missing tools in toolbox apps. Reuses existing CameraX stack. ML Kit Document Scanner is on-device and handles edge detection/cropping. Pairs naturally with the planned OCR tool (R40).
**Downsides:** ML Kit Document Scanner adds ~3MB to APK; PDF generation code needs care (PdfDocument API).
**Confidence:** 80%
**Complexity:** High
**Status:** Unexplored

### 4. Image Compressor + EXIF Stripper
**Description:** Two-in-one photo utility. Batch-compress images with a quality slider and/or strip EXIF metadata (GPS, camera model, timestamps) before sharing. Operates on files picked via SAF.
**Rationale:** EXIF stripping is a genuine privacy tool that reinforces the app's brand story. Compression is a universal need (photos for forms, WhatsApp, email). Low implementation cost.
**Downsides:** Competes with built-in compression in some share sheets; UI must make metadata removal tangible (before/after view).
**Confidence:** 80%
**Complexity:** Low-Medium
**Status:** Unexplored

### 5. Storage Analyzer
**Description:** Visual breakdown (treemap or sorted bar) of what's filling device storage — by folder, file type, and largest-file list. Read-only, no deletion (avoid destructive permissions).
**Rationale:** Universal pain point; read-only keeps it low-risk. Uses `StorageStatsManager` + SAF-based filesystem walk. Complements the existing Device Info tool.
**Downsides:** `MANAGE_EXTERNAL_STORAGE` is a Play Store red flag — must work via SAF, which limits which folders are scannable. Design must embrace that constraint rather than fight it.
**Confidence:** 70%
**Complexity:** Medium
**Status:** Unexplored

### 6. Screenshot Stitcher
**Description:** Pick 2+ screenshots → auto-detect overlap regions → stitch vertically into one long image → export to gallery.
**Rationale:** Niche but delightful — for sharing long chats, receipts, articles. No toolbox-category competitor does this well. Pure image math, fully offline, no special permissions beyond media read.
**Downsides:** Overlap-detection tuning; non-overlapping screenshots require a manual-align fallback UI.
**Confidence:** 70%
**Complexity:** Medium
**Status:** Unexplored

### 7. WiFi QR Share
**Description:** Enter SSID + password (or select from saved networks, permission-gated) → generate the standard `WIFI:T:WPA;S:<ssid>;P:<pwd>;;` QR code for guests to scan and auto-join. Inverse of Android 10's built-in share, but offline and works on older devices.
**Rationale:** Tiny feature, huge daily utility. Extends the existing QR generator. Roughly a one-day build.
**Downsides:** Android 10+ has a native equivalent in system settings — differentiator is older devices, multi-network save/recall, and home-screen shortcut.
**Confidence:** 65%
**Complexity:** Low
**Status:** Unexplored

## Rejection Summary

| #  | Idea | Reason Rejected |
|----|------|-----------------|
| 1  | Instagram/Facebook/TikTok Story Downloader | Requires network — violates the app's offline-only constraint |
| 2  | Online Translator | Requires network; on-device ML Kit Translate is a maybe-later alternative |
| 3  | Reverse Image Search | Requires network |
| 4  | Fake Caller (schedule fake incoming call) | Bad Play Store quality signal; adjacent to scam-tool category |
| 5  | Lie Detector (toy) | Gimmick, lowers perceived app quality |
| 6  | SMS Backup | `READ_SMS` is a restricted permission with heavy Play Store review friction |
| 7  | Contact Backup | Sensitive permission + overlaps with Google account backup |
| 8  | SOS Emergency Button | Safety-critical — liability risk not appropriate for MVP |
| 9  | App Lock (overlay / accessibility) | Accessibility-service usage is a Play Store policy minefield |
| 10 | Clipboard Manager (history) | Background access to clipboard on Android 10+ is heavily restricted; policy minefield |
| 11 | Screen Recorder | MediaProjection complexity, audio-sync footguns, high implementation cost |
| 12 | Generic Sticky Notes / To-Do / Kanban | Commodity glut; does not leverage unique device capabilities |
| 13 | Habit Tracker | Commodity glut; nothing device-specific to add |
| 14 | Expense Tracker | Commodity glut; every user already has one |
| 15 | Meme Maker | Commodity glut; not aligned with "tool" positioning |
| 16 | Photo Collage Maker | Commodity glut; weak differentiation |
| 17 | GIF Maker (photos → GIF) | Implementation cost high for modest novelty |
| 18 | Voice Recorder | Commodity; system recorder covers the need |
| 19 | Text OCR (standalone) | Already planned as R40 — not a new idea |
| 20 | Offline Translator (ML Kit on-device models) | Interesting but adds 30+MB language packs — revisit later |
| 21 | Dog Whistle / Pest Repeller | Overlaps with existing Tone Generator |
| 22 | Night Mode Red Screen | Overlaps with Screen Flash; thin idea alone |
| 23 | Hash Calculator (MD5/SHA) | Too niche; very small audience |
| 24 | Battery Health Monitor | Android does not expose real charge-cycle data without root — would be an estimation gimmick |
| 25 | Business Card Scanner (OCR → contact) | Degenerate case of document scanner + OCR; subsume into those |
| 26 | Silent Camera | Illegal in some jurisdictions (JP/KR); Play Store risk |

## Session Log
- 2026-04-23: Initial ideation — ~40 candidates generated across 6 frames, 7 survivors kept after adversarial filter. Focus: local-data/filesystem utilities in the spirit of WhatsApp Status Saver.
