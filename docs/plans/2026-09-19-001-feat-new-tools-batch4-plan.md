---
title: "feat: New Tools Batch 4 — QR Generator, Focus Timer, Notepad, Flashlight strobe"
type: feat
status: active
date: 2026-09-19
deepened: 2026-09-19
origin: docs/brainstorms/2026-09-19-new-tools-batch4-requirements.md
---

# feat: New Tools Batch 4 — QR Generator, Focus Timer, Notepad, Flashlight strobe

## Overview

Batch 4 adds three new tools — QR Generator, Notepad (the backfill for the dropped Torch), and the higher-effort Focus Timer — plus a small enhancement to the already-shipped Flashlight. All work reuses existing infrastructure verified during review: `core/qr/QrEncoder`, `core/sharing/ImageSharer`, `core/media/MediaStoreWriter`, the `everyday/stopwatch/TimerService` foreground-service *pattern* (not its shared identifiers — see Unit 5), the `core/persistence` DataStore pattern, and the `lighting/FlashlightScreen` torch code.

The ship gate is decoupled **by release sequencing**, not by a runtime flag. Units 1–4 (QR Generator + Notepad) touch no manifest and carry zero policy risk, so they ship as one release *before* any Focus Timer code lands. Focus Timer (Units 5–6) adds a third `specialUse` `<service>` to the manifest — which Play evaluates from the shipped APK regardless of in-app routing — so it is a **separate, later release that carries its own Play review**. There is no "QR+Notepad release that also contains the Focus Timer service"; the decoupling is real only because the manifest change lands last (see System-Wide Impact).

## Problem Frame

Toolbox has ~56 tools but no arbitrary-content QR generator and no focus/productivity timer, and its Flashlight strobe is a fixed rate. This is a completeness-and-reuse batch, not a demand-validated one — the app has no analytics (see origin).

## Requirements Trace

**QR Generator**
- R1. Tool titled "QR Generator" (QR only; no WiFi, no 1D barcodes).
- R2. Content-type picker + structured forms: Text, URL, Email, Phone, SMS, vCard, Geo; default Text.
- R3. Live preview regenerating as fields edit.
- R3a. Explicit preview states: placeholder (empty), invalid (field validation), valid (bitmap), too-long (capacity); never crash.
- R4. Per-type entered data preserved on type switch.
- R5. Share PNG + save to `Pictures/Toolbox/`, each with success/failure feedback.
- R6. De-facto-standard formatting + escaping; PNG quiet-zone margin.
- R7. Search tokens.

**Focus Timer**
- R8. Pomodoro cycle: configurable work/short-break/long-break/sessions.
- R9. Auto-advance phases; alert on phase end; Pause/Skip in-app and in notification.
- R10. Background + screen-off continuity via a dedicated foreground service using the `specialUse`/`timer_countdown` pattern.
- R11. In-app state machine (idle/running/paused/transition/complete) with controls.
- R12. Notification-denied fallback: full in-app controls + in-app alerts.
- R13. Optional White Noise ambience (off by default; deferrable — see Unit 8).
- R14. Today's completed-session count.
- R15. Search tokens.

**Notepad**
- R16. Notes list + plain-text editor.
- R17. Local durable persistence, autosave.
- R18. Create/edit/delete/share-as-text.
- R19. Search tokens.

**Flashlight enhancement**
- R20. Adjustable strobe rate (replaces fixed ~5 Hz) on the existing tool.
- R21. Photosensitivity safeguard: safety cap + blocking first-use caution + slider note.
- R22. Distinguish no-flash-hardware from present-but-busy at runtime; message + screen fallback for both.
- R23. Torch releases on tool-exit/destroy; SOS/strobe continue through screen-off (already the current behavior). See Unit 7 for the re-baselined scope.

## Scope Boundaries

- QR Generator is generate-only — no WiFi, no 1D/retail barcodes.
- Torch is NOT a new tool; only the existing Flashlight is enhanced. No second flashlight entry or duplicate tokens.
- Focus Timer adds a **dedicated** foreground service using the existing `specialUse`/`timer_countdown` type (no new FGS type), with its own identifiers.
- Focus Timer v1 has no task list, cloud sync, or historical charts.
- No new automated-test harness is introduced (the repo has zero tests + no test dependencies).
- Flashlight gets no foreground service; keeping the LED on with the screen off while releasing on background is explicitly out of scope (would require a service — see Unit 7).

## Context & Research

### Relevant Code and Patterns

- **Tool registration:** `dashboard/ToolDefinition.kt` — `data class ToolDefinition(id, name, icon, category, description, requiredSensorType?, requiresCamera, searchKeywords)` in `allTools`.
- **Navigation:** `nav/Destinations.kt` (`@Serializable object <Name>`), wired in `ToolboxApp.kt` NavHost.
- **FGS timer pattern:** `everyday/stopwatch/TimerService.kt` — `Service` + `LocalBinder`, coroutine tick, in-memory `endTimeMs` wall-clock, `AlarmManager.setExactAndAllowWhileIdle` **guarded by `if (!am.canScheduleExactAlarms()) return`**, `START_NOT_STICKY`, `NotificationCompat`. **Shared identifiers that must NOT be reused:** `NOTIFICATION_ID=1001`, `COMPLETION_NOTIFICATION_ID=1002`, channels `timer_channel`/`timer_alarm_channel`, PendingIntent request code `0`, and the global `object ActiveTimerState`. `TimerAlarmReceiver` currently only posts a static "Timer complete!" notification — it does not restart the service or advance state.
- **Persistence:** `core/persistence/UserPreferencesRepository.kt` — Preferences DataStore.
- **QR encode/share/save:** `core/qr/QrEncoder.kt` — `object` with `encode(content, sizePx): Bitmap` (throws on blank/oversized); **called by the shipped `everyday/wifiqr/WifiQrShareScreen.kt` (~line 97)**. `core/sharing/ImageSharer.kt`, `core/media/MediaStoreWriter.kt`.
- **Flashlight:** `lighting/FlashlightScreen.kt` — `CameraManager.setTorchMode`; steady/SOS/strobe loops in `LaunchedEffect` (survive `onStop`, so already continue with screen off); teardown only via `DisposableEffect(isOn, mode)` on nav-away. No screen-off release exists.
- **Lifecycle observers:** existing `MagnetometerHook`/`StepSensorHook` use `ON_PAUSE`/`ON_STOP` — which fire identically for screen-off and Home-press (relevant to R23).
- **Manifest:** declares `FOREGROUND_SERVICE_SPECIAL_USE`, `POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM` (the non-calendar variant — denied by default on API 34+ general apps), and two `specialUse` services.

### Institutional Learnings

- `docs/brainstorms/2026-04-23-new-tools-batch3-requirements.md` established the QR encoder / ImageSharer / MediaStoreWriter helpers reused here.

### External References

- Skipped — every subsystem has a strong local pattern. The one genuinely uncertain external item (Play policy for a 3rd special-use service) is tracked as a pre-submission checklist item, not researched here.

## Key Technical Decisions

- **Focus Timer is a dedicated `FocusTimerService`, mirroring the `TimerService` *design* but sharing none of its runtime identifiers.** It gets its own notification IDs (≥1004), its own channels, its own `BroadcastReceiver` + distinct PendingIntent request code/action, and its own run-state holder (NOT `ActiveTimerState`). Shared scaffolding (coroutine tick, wall-clock math, notification build/teardown) is extracted into a small helper both services can consume rather than copy-pasted. Rationale: a naive clone collides with the shipped countdown timer (identical PendingIntent cancels its alarm; shared notification ID/channel/singleton stomp its state) when both run at once. (Resolves the earlier extend-vs-sibling ambiguity: **sibling + extracted helper**.)
- **Auto-advance survives *recoverable* kills only; force-stop is unrecoverable by design.** Two kill types must not be conflated: (a) a low-memory / Doze / OEM background reap leaves pending alarms intact, so persisting phase + `endTimeMs` (DataStore) plus a receiver that reloads state, advances the phase, and re-arms the next alarm keeps auto-advance working; (b) a user Force Stop or an aggressive OEM task-killer puts the app in `FLAG_STOPPED` — the OS **cancels all alarms and delivers no broadcasts** until manual relaunch, so no receiver fires and recovery is impossible mid-cycle. For (b), recovery happens **on next app launch**: read persisted `endTimeMs`, reconcile elapsed wall-clock, skip past any phases whose boundaries already passed, and resume or report. A device reboot behaves like (b) (v1 declares no `RECEIVE_BOOT_COMPLETED` receiver) and is reconciled the same way on next open. The receiver reads DataStore via `goAsync()` + a coroutine (`dataStore.data.first()`) within the ~10 s window — never `runBlocking` on the main thread (ANR risk).
- **Exact alarms are the primary path; the inexact fallback is background-approximate, not merely drifting.** `SCHEDULE_EXACT_ALARM` is denied by default for a general toolbox app on Android 14/15, and the scheduler silently no-ops when `canScheduleExactAlarms()` is false. So: if `canScheduleExactAlarms()` is **true**, use `setExactAndAllowWhileIdle`; if **false**, deep-link the user to system Settings via `ACTION_REQUEST_SCHEDULE_EXACT_ALARM` (not a runtime dialog) with a rationale that accurate background timing needs it. If the user declines, the timer is fully accurate **while open** (coroutine tick) but its background/screen-off alarms use `setAndAllowWhileIdle`, which Doze rate-limits to roughly one fire per ~9–15 min — so a 5-minute break can run ~2–3× long and the chained re-arm compounds it; the tool must warn that background timing is *approximate*, not "slightly off." **WorkManager is not used for phase boundaries** (15-min minimum interval). `USE_EXACT_ALARM` (auto-granted, no Settings toggle) was considered and rejected: its Play policy gate requires the app to be a genuine alarm/clock app, which a general toolbox likely fails.
- **After a kill, the receiver — not the service — produces the phase-end alert, and can only restart the FGS in the exact-alarm branch.** An exact-alarm fire grants a temporary background-FGS-start allowlist, so in that branch the receiver may `startForegroundService` to resume the live countdown notification. The **inexact fallback grants no such exemption**, so there the receiver can only post a notification + sound/vibration; the live countdown does not resume until the user reopens the app. R10's "continuity via a foreground service" therefore holds only in the exact-alarm branch.
- **QR preview validity is a ViewModel concern; the encoder is not changed in place.** `QrEncoder.encode(): Bitmap` stays intact for the shipped WiFi QR tool. A **new** guarded entry point (e.g. `encodeResult()` returning Blank/Ok(bitmap)/TooLong) is added. The four preview states map as: Placeholder = empty input; Invalid = ViewModel per-field validation failure; Valid/TooLong = encoder result. Validation and the encoder result are distinct sources.
- **Notepad uses DataStore + JSON, protected against logical *and* file-level loss.** Consistent with "DataStore not Room". A parse failure **returns last-good and refuses to overwrite** rather than silently falling back to empty (which would wipe all notes). Because a same-file "last-good key" would die with the file under a DataStore `CorruptionException`, the last-good copy is written to a **separate small file** (and a `corruptionHandler` restores from it), so logical *and* file-level corruption are both covered — not just a bad serialize.
- **No test harness is added.** The repo has 0 tests and no test dependencies; pure-logic verification (QR formatting, Pomodoro phase math) is done by manual/interactive check. Adding a JUnit `testImplementation` for optional unit tests is a future option, explicitly out of this batch's scope. No `src/test` files are prescribed below, to keep this consistent.
- **Flashlight gets no foreground service.** R23 is re-baselined to the achievable behavior (see Unit 7).

## Open Questions

### Resolved During Planning

- FGS type: reuse `specialUse`/`timer_countdown` (already declared) — but see the Play-declaration checklist item below.
- Service shape: dedicated `FocusTimerService` with private identifiers + extracted shared helper.
- QR guarding: new method, existing `encode(): Bitmap` untouched.
- Notes storage: DataStore + JSON with atomic write + last-good backup.
- Test approach: manual/interactive only; no harness, no `src/test` files.
- Exact-alarm strategy: `SCHEDULE_EXACT_ALARM` via Settings deep-link as the primary path, `setAndAllowWhileIdle` (approximate) fallback; `USE_EXACT_ALARM` rejected (Play alarm-app gate). No WorkManager for phase boundaries.
- Ship-gate: release sequencing (Units 1–4 ship before the Focus Timer manifest service lands), not a runtime flag.

### Deferred to Implementation

- **[Pre-submission checklist]** Confirm against current Play policy that adding a *third* `specialUse` service under the existing app-level `FOREGROUND_SERVICE_SPECIAL_USE` declaration needs no new/updated justification; note the fallback if it does. Do not treat as settled.
- White Noise generator extraction size (R13) — assess when Unit 8 starts; if it requires the `mediaPlayback` FGS type or destabilizes the shipped White Noise tool, defer R13.
- Hardware-safe maximum strobe rate (R21) — determine on-device (existing ~5 Hz).
- vCard version (2.1 vs 3.0) for scanner compatibility (R6).
- Reboot handling: v1 reconciles on next launch (no `RECEIVE_BOOT_COMPLETED`); revisit if users expect a Pomodoro to survive a reboot.
- Whether `FocusTimerService` runs continuously across a full set or is stopped/started per phase (affects how often the Android 12+ background-FGS-start restriction is hit) — decide when implementing Unit 5.

## Implementation Units

- [x] **Unit 1: QR content model + guarded encoder entry point**

**Goal:** Structured input → correctly-formatted, capacity-safe payloads, without changing the shipped encoder contract.

**Requirements:** R6 (and the encoder foundation for R3a).

**Dependencies:** None.

**Files:**
- Create: `app/src/main/java/com/toolbox/everyday/qrgen/QrContent.kt` (sealed type + `toPayload()`)
- Modify: `app/src/main/java/com/toolbox/core/qr/QrEncoder.kt` — **add** `encodeResult(...)` returning a sealed Blank/Ok(bitmap)/TooLong; leave `encode(): Bitmap` unchanged so `WifiQrShareScreen` keeps compiling.

**Approach:** Sealed `QrContent` (Text/Url/Email/Phone/Sms/VCard/Geo), each rendering its payload (`mailto:`,`tel:`,`smsto:`,`BEGIN:VCARD…`,`geo:`) with escaping. `encodeResult` guards blank + catches ZXing capacity `WriterException` → TooLong.

**Patterns to follow:** existing `QrEncoder.encode`; `@Serializable` usage.

**Test scenarios (manual/interactive):**
- Happy path: each type yields the correct payload prefix/shape (vCard has `BEGIN:VCARD`+`FN:`+`TEL:`; geo → `geo:<lat>,<lng>`).
- Edge: blank input → Blank, ZXing never called.
- Edge: special chars escaped, not dropped.
- Error: oversized vCard → TooLong, no exception.
- Regression: WiFi QR Share still generates its code (existing `encode` untouched).

**Verification:** All seven payloads scan to the intended action in the app scanner + Google Lens; WiFi QR unaffected.

- [x] **Unit 2: QR Generator screen + ViewModel + registration**

**Goal:** Full UI with a guarded live-preview state machine and share/save.

**Requirements:** R1, R2, R3, R3a, R4, R5, R7.

**Dependencies:** Unit 1.

**Files:**
- Create: `app/src/main/java/com/toolbox/everyday/qrgen/QrGeneratorScreen.kt`, `QrGeneratorViewModel.kt`
- Modify: `dashboard/ToolDefinition.kt` (`id="qr_generator"`, EverydayTools, tokens `qr/generate/vcard/contact/url`), `nav/Destinations.kt`, `ToolboxApp.kt`

**Approach:**
- **IA:** a horizontally-scrollable segmented/chip row of the 7 types pinned above a single scrolling form region whose fields swap per type; the preview docks at the top so a multi-field vCard form can scroll without pushing it off-screen.
- **State:** ViewModel keeps per-type form state (R4: preserved on switch), current type (default Text), and derived preview state — Placeholder (empty) / Invalid(message, from per-field validation) / Valid(bitmap) / TooLong (both from `encodeResult`). Preview holds the last valid bitmap while Invalid.
- **Validation table (per type):** required fields + format checks — Email (address non-empty, `@` present), Phone/SMS (non-empty digits), Geo (lat −90..90, lng −180..180), vCard (name required), URL (non-empty). Each failure yields a specific inline message.
- Share via `ImageSharer`, save via `MediaStoreWriter`; both emit one-shot success (with location) / failure surfaced as a snackbar.

**Patterns to follow:** `everyday/wifiqr/WifiQrShareScreen.kt`; `ImageSharer`/`MediaStoreWriter` call sites.

**Test scenarios (manual/interactive):**
- Happy: fill each type → preview updates; share + save both confirm.
- Edge: switch types mid-entry → each type's data retained.
- Edge: clear all fields → placeholder; invalid field → inline message + last-valid preview held.
- Error: oversized content → "too long", share/save blocked.
- Error: save failure → failure snackbar, no false success.

**Verification:** All four preview states reachable; codes scan; feedback on share/save.

- [x] **Unit 3: Notes persistence (loss-resistant)**

**Goal:** Durable local notes storage that cannot silently wipe on a bad write.

**Requirements:** R17.

**Dependencies:** None.

**Files:**
- Create: `app/src/main/java/com/toolbox/everyday/notepad/NotesRepository.kt`, `Note.kt` (`@Serializable data class Note(id, body, updatedAt)`)

**Approach:** Own `preferencesDataStore(name="notepad")` holding a JSON `List<Note>` under one key. After each successful write, mirror the JSON to a **separate last-good file** (not another key in the same store — a `CorruptionException` takes the whole store file with it). On read parse failure, return last-good and **refuse to overwrite the primary with empty**; register a DataStore `corruptionHandler` that restores from the last-good file. Expose `Flow<List<Note>>` + suspend upsert/delete.

**Patterns to follow:** `core/persistence/UserPreferencesRepository.kt`.

**Test scenarios (manual/interactive):**
- Happy: upsert → read returns it; update replaces by id; delete removes.
- Edge: empty store → empty list.
- Failure: a corrupt primary blob → last-good restored, notes not lost.

**Verification:** Notes persist across restart; an induced corrupt write does not wipe existing notes.

- [x] **Unit 4: Notepad screen + ViewModel + registration**

**Goal:** Notes list + editor with autosave, share, and defined empty/edge states.

**Requirements:** R16, R18, R19.

**Dependencies:** Unit 3.

**Files:**
- Create: `app/src/main/java/com/toolbox/everyday/notepad/NotepadScreen.kt`, `NotepadViewModel.kt`
- Modify: `dashboard/ToolDefinition.kt` (`id="notepad"`, tokens `notepad/notes/memo/scratchpad/text`), `nav/Destinations.kt`, `ToolboxApp.kt`

**Approach:** **List → full-screen editor** (phone-first, not master/detail). **Empty-list state:** a short empty message + a prominent New Note FAB as the primary create affordance. **Empty-note rule:** on leaving the editor, discard a note whose body is empty/whitespace (never create a ghost list entry); autosave (debounced) begins only once content is non-empty. Delete with a light confirm. Share current note as plain text.

**Patterns to follow:** existing list-style tool screens; text-sharing util.

**Test scenarios (manual/interactive):**
- Happy: create → type → leave → reopen shows saved text; delete removes; share opens sheet with note text.
- Edge: New Note, type nothing, leave → no ghost entry appears.
- Edge: zero notes → empty state + FAB shown.

**Verification:** CRUD survives restart; no empty ghost notes; share carries note text.

- [x] **Unit 5: FocusTimerService (Pomodoro engine, collision-safe, kill-resilient)**

**Goal:** Background phase-cycling timer that neither collides with the shipped timer nor stops advancing after an OEM kill.

**Requirements:** R8, R9, R10, R14.

**Dependencies:** None.

**Files:**
- Create: `app/src/main/java/com/toolbox/everyday/focustimer/FocusTimerService.kt`, `FocusPhase.kt`, `FocusSessionState.kt`, `FocusTimerAlarmReceiver.kt`, and a small shared FGS helper (extracted from `TimerService` scaffolding, e.g. `everyday/stopwatch/` or `core/`)
- Modify: `AndroidManifest.xml` (register `FocusTimerService` `foregroundServiceType="specialUse"` + `timer_countdown` subtype, and the new receiver)

**Approach:**
- Phase state machine (Work/ShortBreak/LongBreak + config) over the wall-clock design; auto-advance on completion; Pause/Skip via service actions/notification buttons.
- **Own identifiers:** notification IDs ≥1004, new channels (e.g. `focus_timer_channel`), a dedicated `FocusTimerAlarmReceiver` with its own PendingIntent request code + intent action, and its own run-state holder (not `ActiveTimerState`).
- **Persistence + re-arm:** persist current phase + `endTimeMs` to DataStore. `FocusTimerAlarmReceiver.onReceive` calls `goAsync()`, launches a coroutine that reads `dataStore.data.first()`, advances the phase, re-arms the next alarm, posts the alert, and calls `pendingResult.finish()` — all within the ~10 s budget (no `runBlocking`). In the exact-alarm branch it also restarts the FGS to resume the live notification; in the inexact branch it posts a notification + sound/vibration only.
- **Exact-alarm handling:** if `canScheduleExactAlarms()` is **true**, use `setExactAndAllowWhileIdle`. If **false**, deep-link to Settings (`ACTION_REQUEST_SCHEDULE_EXACT_ALARM`) with rationale; if the user declines, use `setAndAllowWhileIdle` (no WorkManager) and warn that background timing is approximate — a 5-min break may run ~9–15 min under Doze. Accurate while the app is open regardless. See Key Technical Decisions.
- **Kill recovery:** recoverable (low-memory/Doze/OEM) kills re-arm via the surviving alarm; force-stop/reboot are unrecoverable mid-cycle and reconcile on next app launch from persisted `endTimeMs` (skip past phases whose boundaries passed).
- Track today's completed work sessions (reset on date change).

**Execution note:** Read `TimerService.kt` + `TimerAlarmReceiver.kt` fully first; extract shared scaffolding into a helper rather than cloning.

**Patterns to follow:** `TimerService.kt` design (not its constants); `TimerAlarmReceiver.kt` (as the anti-pattern to improve on — it must actually re-arm).

**Test scenarios (adversarial manual QA, named devices across MIUI/OneUI/Pixel):**
- Happy: start work, background app, screen off 25 min → phase-end alert fires; auto-advances to break.
- Recoverable-kill resilience: trigger a low-memory / background reap (not force-stop) mid-cycle → surviving alarm fires, receiver re-arms, next phase auto-advances.
- Force-stop / reboot: force-stop mid-cycle → timer stops (by design; alarms cancelled). Reopen the app → it reconciles from persisted `endTimeMs` and resumes/reports the correct phase.
- Collision: run the shipped countdown Timer and Focus Timer simultaneously → both notifications and both alarms behave independently.
- Exact-alarm-denied: on a device where `canScheduleExactAlarms()` is false → fallback path runs, user warned, timer still advances (allowing drift).
- Edge: Skip advances immediately; Pause halts and the notification reflects it; full cycle reaches long break after the configured count.
- Instrumentation: a debug logcat trace of alarm-fire vs tick so a missed phase is observable, not inferred.

**Verification:** Accurate screen-off timing for a full phase; auto-advance survives a force-stop; no interference with the shipped timer; no stuck notification after stop.

- [x] **Unit 6: Focus Timer screen + ViewModel + notification-denied fallback + registration**

**Goal:** In-app UI and graceful degradation without notifications.

**Requirements:** R8, R9, R11, R12, R14, R15.

**Dependencies:** Unit 5.

**Files:**
- Create: `app/src/main/java/com/toolbox/everyday/focustimer/FocusTimerScreen.kt`, `FocusTimerViewModel.kt`
- Modify: `dashboard/ToolDefinition.kt` (`id="focus_timer"`, tokens `focus/pomodoro/timer/productivity/study`), `nav/Destinations.kt`, `ToolboxApp.kt` — these registration edits, and the Unit 5 manifest `<service>`, land **only after** the QR+Notepad release has shipped (release sequencing, not a flag), so no released APK declares the Focus Timer service until Focus Timer itself ships.

**Approach:**
- In-app state machine (idle/running/paused/transition/complete) with start, pause/resume, skip, reset, config, and today's session count. Define the notification action set (Pause/Skip) vs the fuller in-app set (adds Reset/config); Reset is in-app only, and the two never diverge on the shared Pause/Skip/phase state.
- Lazily request `POST_NOTIFICATIONS` on first start with rationale. If denied, run with full in-app controls and in-app phase-end alerts. **Because a denied notification is worst exactly when backgrounded**, the phase-end sound/vibration is emitted by whichever component is alive at the boundary — the service if running, otherwise the alarm receiver (after a kill only the receiver runs) — so the alert fires even with no visible notification and no live service.

**Test scenarios (manual/interactive):**
- Happy: all in-app states render with correct controls, consistent with the notification.
- Error: deny notifications → timer runs, phase-end alert (sound/vibration) still fires, including when backgrounded.
- Edge: config changes apply to the next phase, not the running one.

**Verification:** Fully usable with notifications denied; in-app and notification never diverge on shared state.

- [x] **Unit 7: Flashlight strobe + safety + robustness enhancement**

**Goal:** Enhance the existing Flashlight; re-baseline R23 to what the code can actually do without a service.

**Requirements:** R20, R21, R22, R23.

**Dependencies:** None.

**Files:**
- Modify: `app/src/main/java/com/toolbox/lighting/FlashlightScreen.kt`; description only in `dashboard/ToolDefinition.kt` (existing `id="flashlight"` — do NOT add an entry)

**Approach:**
- Replace the fixed ~5 Hz strobe with a slider mapped to a validated, safety-capped range.
- **R21 caution:** a **blocking first-use dialog** the user must acknowledge before strobe activates, with photosensitive-epilepsy warning copy, persisted via a DataStore flag; plus a persistent caution note beside the slider.
- **R22:** wrap `setTorchMode` to distinguish no-flash-hardware from `CameraAccessException` (present-but-busy), each with a clear message + the screen-flash fallback offered.
- **R23 (re-baselined):** the code has **no** screen-off release today — SOS/strobe already continue through screen-off via `LaunchedEffect`, and `ON_STOP` can't distinguish screen-off from Home-press. So R23's target is simply: ensure the torch releases on tool-exit/`onDestroy` (extend the existing `DisposableEffect` teardown to cover app-exit), and accept that SOS/strobe persist through screen-off as they already do. Keeping the LED on with the screen off while releasing on background is **out of scope** (would need a foreground service, which Flashlight does not get).

**Execution note:** On-device test the strobe upper bound before fixing the cap value (R21 deferred question).

**Patterns to follow:** existing inline torch loops in `FlashlightScreen.kt`; `lighting/screenflash` fallback; `DisposableEffect` teardown.

**Test scenarios (manual/interactive):**
- Happy: slider changes strobe rate up to the cap.
- Safety: first strobe use shows the blocking caution; strobe cannot start until acknowledged; rate cannot exceed the cap.
- Error: open the camera app then Flashlight → busy message + screen fallback offered; no-flash device → distinct message + fallback.
- Edge: exit the tool (swipe/back) → LED releases; SOS with screen off → continues (documented behavior).

**Verification:** Adjustable capped strobe gated by an acknowledged caution; busy vs no-hardware handled distinctly; LED releases on exit.

- [x] **Unit 8 (optional/deferrable): White Noise ambience for Focus Timer**

**Goal:** Optional work-phase ambient sound (R13).

**Requirements:** R13.

**Dependencies:** Unit 5, Unit 6.

**Files:**
- Create: `app/src/main/java/com/toolbox/core/audio/NoiseEngine.kt` (extract from `everyday/whitenoise/WhiteNoiseScreen.kt`)
- Modify: `WhiteNoiseScreen.kt`, `FocusTimerService.kt` / `FocusTimerViewModel.kt`

**Approach:** Extract the AudioTrack generation out of the Compose screen into a reusable engine; drive it during work phases only, off by default. **FGS-type caveat:** continuous audio from the timer service likely requires the `mediaPlayback` FGS type (or a separate media service) — which would break the "no new FGS type / no new Play declaration" claim. Re-verify before shipping R13; if it holds true, keep R13 deferred.

**Execution note:** Scope the extraction + the FGS-type question first; if either is large/risky, defer R13 to a follow-up plan.

**Test scenarios (manual/interactive):**
- Happy: enable ambience → sound plays during work, stops on break/pause/stop.
- Regression: the shipped White Noise tool still works unchanged after extraction.

**Verification:** White Noise tool unchanged; ambience toggles correctly; no audio leaks past a phase; FGS-type compliance confirmed.

## System-Wide Impact

- **Interaction graph:** new NavHost composables + 3 `allTools` entries + 1 Flashlight description edit; new `FocusTimerService` + `FocusTimerAlarmReceiver` in the manifest.
- **Ship-gate = release sequencing.** The Play risk lives in the manifest `<service>` (a Unit 5 edit), which Play evaluates from the shipped APK regardless of in-app routing — so a runtime flag or a revertible tool-registration commit would NOT keep the third `specialUse` service out of a "QR+Notepad" APK. The only honest decoupling is order: ship Units 1–4 (no manifest change) first; land the Focus Timer manifest service (Unit 5) and registration (Unit 6) only for the later Focus Timer release, which carries its own Play review.
- **Error propagation:** QR errors → preview states (no crash); torch failures → messages + fallback; notification denial → in-app degradation; notes parse failure → last-good, never wipe.
- **State lifecycle risks:** FGS notification lifecycle (stuck notification, LED left on) — Focus Timer uses its own IDs/channel/receiver/state holder to avoid stomping the shipped timer; torch release on exit (Unit 7); Focus Timer persists phase/endTimeMs for kill-recovery.
- **API surface parity:** none — no exported API changes; `QrEncoder.encode(): Bitmap` preserved for the WiFi QR caller.
- **Unchanged invariants:** shipped `TimerService`/`StopwatchService`, `ActiveTimerState`, the WiFi QR tool, and the White Noise tool must remain behaviorally unchanged; the existing Flashlight `id`/tokens stay single.

## Risks & Dependencies

| Risk | Mitigation |
|------|------------|
| Background reap stops Pomodoro auto-advance | Persist phase+endTimeMs; receiver (`goAsync()`) reloads + re-arms next phase (Unit 5). |
| Force-stop / reboot kills the timer | Unrecoverable mid-cycle by design; reconcile from persisted `endTimeMs` on next app launch (Unit 5). Not claimed as auto-advance survival. |
| Exact alarms denied by default (Android 14/15) → inexact fallback unreliable for 5-min breaks | Primary path: prompt for exact-alarm via Settings deep-link; if declined, `setAndAllowWhileIdle` + warn timing is *approximate* (Doze ~9–15 min); accurate while app open (Unit 5). |
| Sibling service collides with shipped timer (PendingIntent/notification/state) | Dedicated receiver, request code, notification IDs ≥1004, channels, and run-state holder (Unit 5). |
| Notes single-blob write / file corruption wipes everything | Separate last-good file + `corruptionHandler`; refuse-overwrite on parse failure (Unit 3). |
| Changing shared `QrEncoder` breaks WiFi QR | Add new `encodeResult`; leave `encode(): Bitmap` intact (Unit 1). |
| Strobe rate unsafe/unsupported | Safety cap + blocking first-use caution; on-device rate test (Unit 7). |
| R13 audio needs `mediaPlayback` FGS type | Isolated to optional Unit 8; re-verify or defer. |
| 3rd special-use service draws Play re-review | Pre-submission checklist item (Open Questions) — not assumed resolved. |
| No test harness → timing regressions unseen | Adversarial manual QA on named devices + logcat instrumentation (Unit 5). |

## Documentation / Operational Notes

- Pre-submission: verify the Play Console `FOREGROUND_SERVICE_SPECIAL_USE` justification still covers a third (Pomodoro) timer service before release.
- `PRIVACY_POLICY.md`: likely no change (notification/FGS already used); confirm.

## Sources & References

- **Origin document:** docs/brainstorms/2026-09-19-new-tools-batch4-requirements.md
- Related code: `everyday/stopwatch/TimerService.kt`, `TimerAlarmReceiver.kt`, `ActiveTimerState.kt`, `core/qr/QrEncoder.kt`, `everyday/wifiqr/WifiQrShareScreen.kt`, `core/persistence/UserPreferencesRepository.kt`, `lighting/FlashlightScreen.kt`, `dashboard/ToolDefinition.kt`, `nav/Destinations.kt`, `ToolboxApp.kt`, `AndroidManifest.xml`
- Prior batch: docs/plans/2026-04-13-001-feat-new-tools-batch2-plan.md
