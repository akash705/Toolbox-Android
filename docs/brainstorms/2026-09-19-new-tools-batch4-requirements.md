---
date: 2026-09-19
topic: new-tools-batch4
---

# New Tools — Batch 4

## Problem Frame

Toolbox ships ~56 offline, no-ads, zero-permission-cold-start utilities. Batch 4 fills two genuine catalog gaps — no way to *generate* a QR code for arbitrary content (only a WiFi-specific tool exists), and no focus/productivity timer despite the app already owning a white-noise + breathing "focus-adjacent" cluster — and adds one cheap, sticky everyday tool (Notepad). It also lands a small enhancement to the **already-shipped** Flashlight tool, and formally schedules the **Status Saver research spike** that has blocked that tool since Batch 3.

This is a completeness-and-reuse batch, not a demand-validated one: the app has no analytics, so tool selection is driven by reuse leverage and obvious gaps, not measured demand. That is an explicit, accepted tradeoff (see Key Decisions).

## Requirements

**QR Generator**
- R1. Tool titled "QR Generator" that encodes user content into a QR code, reusing the existing `core/qr/QrEncoder`. QR only — no 1D/retail barcodes, no WiFi (the existing WiFi QR Share tool owns WiFi).
- R2. Content-type picker with structured input forms for: Plain Text, URL, Email (address + subject + body), Phone, SMS (number + message), vCard contact (name, phone, email, org), and Geo location (lat/long). Default type on open is Plain Text.
- R3. Live preview: the QR regenerates as the user edits fields.
- R3a. Preview states are explicit: (a) an empty/placeholder preview before any valid content exists — the encoder is never called with blank content; (b) per-field validation with inline errors, and the preview holds the last valid code (or placeholder) while input is invalid; (c) when encoded content exceeds QR capacity, catch the encoder exception and show an inline "content too long" message rather than crashing or emitting an unscannable code.
- R4. Switching content type preserves each type's entered data independently (no wipe on switch).
- R5. Actions on a generated QR: share as PNG (reuse `core/sharing/ImageSharer`) and save to `Pictures/Toolbox/` (reuse `core/media/MediaStoreWriter`). Both actions show success confirmation (with save location) and a failure state.
- R6. Encoder formats each content type per its de-facto standard (`mailto:`, `tel:`, `smsto:`, `BEGIN:VCARD`, `geo:`) with proper escaping of special characters. The exported PNG includes a proper quiet-zone margin.
- R7. Discoverable via dashboard search on tokens: "qr", "generate", "vcard", "contact", "url".

**Focus Timer**
- R8. Tool titled "Focus Timer" implementing a Pomodoro cycle: configurable work duration, short break, long break, and sessions-before-long-break (defaults 25/5/15/4).
- R9. Phases **auto-advance** (work → break → work …); the phase-end alert is a notification (plus optional sound/vibration), not a required tap. In-app and notification both expose Pause and Skip.
- R10. Running session continues in the background and while the screen is off, backed by a foreground service that **reuses the existing `specialUse` + `timer_countdown` pattern already established by `everyday/stopwatch/TimerService`** (extend or mirror it — do not introduce a second, inconsistent service or a new FGS type).
- R11. In-app timer screen defines all states — idle / running / paused / phase-transition / cycle-complete — with their controls (start, pause/resume, skip, reset), kept consistent with the notification actions.
- R12. Notification-denied fallback: if the user denies `POST_NOTIFICATIONS` (Android 13+), the timer still runs with full in-app controls and in-app phase-end alerts (sound/vibration). The tool degrades, it does not break. Show a rationale before the prompt.
- R13. Optional ambient sound during work phases, off by default. See Key Decisions — this requires extracting the White Noise generator out of its Compose screen into a service-drivable component; it is **not** free reuse. May be deferred to a follow-up if the refactor proves large (see Outstanding Questions).
- R14. Session tracking: show completed focus sessions for the current day (transient/local; no long-term analytics store in v1).
- R15. Discoverable via dashboard search on tokens: "focus", "pomodoro", "timer", "productivity", "study".

**Notepad / Quick Notes**
- R16. Tool titled "Notepad" — a local scratchpad with a list of notes and a plain-text editor.
- R17. Autosave on edit; notes persist locally via the existing DataStore/persistence pattern. No cloud, no account, no network.
- R18. Basic note management: create, edit, delete, and a share-as-text action.
- R19. Discoverable via dashboard search on tokens: "notepad", "notes", "memo", "scratchpad", "text".

**Flashlight Enhancement (existing tool, not new)**
- R20. Extend the shipped `lighting/FlashlightScreen` (already does steady/SOS/strobe via `CameraManager.setTorchMode`) with a **user-adjustable strobe rate** replacing today's fixed ~5 Hz.
- R21. Photosensitivity safeguard: cap the strobe to a hardware-and-safety-validated maximum (well below the 3–30 Hz seizure-risk peak), show a one-time caution before first strobe use, and a visible note near the slider.
- R22. Runtime robustness: handle `setTorchMode` failures when the flash is present but busy (held by camera/another app) — distinct from the no-flash-hardware case — with a clear message and the screen-flash fallback offered in both cases.
- R23. SOS/strobe carve-out from any "release on screen-off" rule: while an explicit SOS/strobe session is active, the LED may continue with the screen off (releasing only on app-background/exit), so the emergency use case survives the anti-stuck-on safeguard.

## Success Criteria
- QR Generator, Focus Timer, and Notepad are built; QR Generator and Notepad (zero policy risk) may release independently of Focus Timer (see Scope Boundaries). All pass QA on Android 8 (min SDK 26), 11, 14, and 15.
- QR Generator produces codes that scan correctly in Google Lens and the app's own scanner across all seven content types, and never crashes on empty/partial/over-capacity input.
- Focus Timer keeps accurate time when backgrounded and screen-off for a full 25-minute work phase, and remains usable (in-app controls + alerts) even when notifications are denied.
- Flashlight strobe is adjustable, safety-capped, and gated behind a first-use caution; torch reliably releases on exit with no stuck-on LED in QA.
- No new policy-sensitive permissions beyond the notification permission already established for the stopwatch/timer services. No `CAMERA`, no `MANAGE_EXTERNAL_STORAGE`, no accessibility service. Cold start still grants nothing.

## Scope Boundaries
- **QR Generator is generate-only** — no WiFi (existing tool), no 1D/retail barcodes (Code128/EAN/UPC).
- **Torch is NOT a new tool.** The app already ships Flashlight; Batch 4 only enhances it (R20–R23). No second flashlight entry, no duplicate search tokens.
- **Focus Timer reuses the existing timer foreground-service pattern.** It introduces no new foreground-service type and no new special-use declaration.
- **Focus Timer v1 has no task list, no cloud sync, no historical charts/streaks** — timer, auto-advancing phase cycle, today's session count.
- **Ship gate is decoupled.** QR Generator + Notepad do not wait on Focus Timer; Focus Timer ships when its in-app fallback and (optional) audio question are resolved.
- Backlog tools and the Status Saver spike are **not** part of Batch 4 delivery.

## Key Decisions
- **Batch composition = 2 net-new tools + 1 backfill + 1 enhancement.** QR Generator and Notepad are the cheap, zero-risk, high-reuse wins. Focus Timer is included for gap-coverage and is a higher-effort tool (background service, permission fallback, optional audio refactor) — it is *not* claimed to be "cheap," and it ships on its own gate. Rationale: honest risk accounting per the Batch-3 "don't couple independent risk" lesson.
- **Torch dropped; Flashlight enhanced instead.** Discovery during review: `lighting/FlashlightScreen.kt` already implements the hardware LED torch with steady/SOS/strobe. Building a "Torch" tool would duplicate it. The real, small opportunity is an adjustable + safety-capped strobe on the existing tool.
- **Focus Timer reuses the proven `specialUse`/`timer_countdown` service pattern.** `TimerService`/`StopwatchService` already ship this on Play, so the FGS type is settled precedent, not an open risk. (An exact-alarm + chronometer-notification approach was considered; reusing the existing accepted service is lower-risk and consistent, and supports live Pause/Skip actions an alarm cannot.)
- **QR Generator reuses the Batch-3 encoder/share/save helpers.** New surface is per-content-type forms, formatting/escaping, and the preview state machine (R3a).
- **White Noise integration is optional and not free.** The generator is currently owned by its Compose screen; driving it from the timer service needs extraction. It stays off by default and may be deferred if the refactor is disproportionate.

## Dependencies / Assumptions
- Reuses verified helpers: `core/qr/QrEncoder.kt`, `core/sharing/ImageSharer.kt`, `core/media/MediaStoreWriter.kt`, `everyday/stopwatch/TimerService` (FGS pattern), existing DataStore persistence (Notepad), `lighting/FlashlightScreen.kt` (enhancement target).
- `QrEncoder.encode` currently passes content straight to ZXing with no blank/length guard — R3a requires adding guards at the call sites (or in the helper).
- Notification permission + foreground-service permissions already declared in the manifest for the stopwatch/timer services; Focus Timer inherits them.

## Backlog — Captured, Not in Batch 4

Prioritized for future batches. Grouped by build confidence.

**Ready to build (low risk, fits ethos)**
- Voice Recorder, PDF Toolkit, Unit-Price Comparator, World Clock / Timezone Planner, Sleep / Nap Timer, Text Utilities, Bill Split, Habit / Streak Tracker, Hash & Encode toolkit, Eye-Test Suite. (Notepad promoted into Batch 4.)

**Reality-gated (need a spike or carry policy risk)**
- Screen Recorder (MediaProjection; policy friction). Clipboard History (Android 10+ restricts background clipboard reads — mostly non-viable for non-default apps).

**Deferred from Batch 3 (own research spikes)**
- Status Saver (spike below), Duplicate Photo Finder, Storage Analyzer, Document Scanner, Screenshot Stitcher.

**Governing rule for future background-capable tools:** a foreground service is acceptable only for a user-visible active timer or while audio is actively playing (`mediaPlayback`). Backlog tools (Sleep Timer, Voice/Screen Recorder) are tested against this rule, not re-litigated case by case — this keeps the zero-permission-cold-start ethos intact.

## Status Saver Spike (scheduled, gates the tool)

Status Saver remains the #1-ranked deferred idea, blocked on Android 11+ SAF access to `Android/media/com.whatsapp/WhatsApp/Media/.Statuses/`. Before it can be planned:
1. Test `ACTION_OPEN_DOCUMENT_TREE` with an initial URI at the `.Statuses` path across Samsung OneUI, Xiaomi MIUI, OnePlus OxygenOS, and Pixel/AOSP — and across **Android 11, 12, 13, 14, and 15** (the blocker begins at 11; min SDK is 26), confirming whether "Use this folder" is offered.
2. If blocked, test the two-stage workaround (grant a higher parent, then descend).
3. Test WA Business (`com.whatsapp.w4b`) separately (different package, likely a second grant).

**Outcome gate:** weight by real install-base share in the app's target markets — require the **dominant skins specifically** to pass, not a raw "3 of 4." Even on a pass, ship with a degradation plan: WhatsApp can change the `.Statuses` storage model at any time, so the tool must detect access loss and inform the user rather than fail silently.

## Outstanding Questions

### Resolve Before Planning
- _(none — product decisions captured above)_

### Deferred to Planning
- [Affects R13][Technical][Needs research] Size of the White Noise generator extraction from its Compose screen into a service-drivable component; if large, defer R13 to a follow-up.
- [Affects R20][Technical][Needs research] Hardware-safe maximum strobe rate `setTorchMode` can sustain across the supported device range (existing code strobes at ~5 Hz; higher rates may throttle/throw).
- [Affects R6][Technical] vCard version to emit (2.1 vs 3.0) for broadest scanner compatibility.
- [Affects R10][Technical] Whether to extend `TimerService` directly or mirror its pattern in a Focus Timer service; and OEM battery-killer resilience (MIUI/OneUI) for a full 25-minute phase.

## Next Steps
→ `/ce:plan` for structured implementation planning of Batch 4 (QR Generator + Focus Timer + Notepad + Flashlight enhancement). Run the Status Saver spike in parallel; it does not block Batch 4.
