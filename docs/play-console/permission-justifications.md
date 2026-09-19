# Play Console — Permission & Foreground Service Justifications

App: **Toolbox** (`com.vedtechnologies.toolbox`)
Last updated: 2026-09-19 (Batch 7)

This is the source of truth for what to paste into the Play Console when it asks
why the app uses a sensitive permission or foreground-service type. Every entry
is written to be copy-paste ready. Keep it in sync with `AndroidManifest.xml`.

---

## 1. Foreground service — `specialUse` (subtype `timer_countdown`)

**Where:** `TimerService`, `FocusTimerService` (subtype `timer_countdown`),
`StopwatchService` (subtype `stopwatch_elapsed_time`).

**Play Console prompt:** "Describe why your app needs to use the specialUse
foreground service type."

**Justification (paste this):**

> These foreground services run only while the user has explicitly started a
> user-visible countdown timer, Pomodoro focus session, or stopwatch and it is
> actively counting. The service exists so the timer keeps accurate time and can
> fire its end-of-phase alert when the screen is off or the app is backgrounded —
> the core promise of a timer. It shows a persistent notification with live
> remaining time and Pause/Skip/Reset actions for the entire time it runs, is
> never started at app launch, and stops the moment the timer is reset, finished,
> or dismissed. None of the predefined foreground-service types (mediaPlayback,
> location, etc.) match a countdown timer, which is why the specialUse type with
> the standard `timer_countdown` subtype is used.

**Why specialUse and not another type:** Android defines no `timer` foreground
type. The documented convention for user-visible countdown/stopwatch timers is
`specialUse` with a descriptive subtype, declared via
`PROPERTY_SPECIAL_USE_FGS_SUBTYPE`. This is settled precedent — these services
already shipped and passed review in prior releases.

---

## 2. Foreground service — `mediaProjection`

**Where:** `ScreenRecordService`.

**Play Console prompt:** For `FOREGROUND_SERVICE_MEDIA_PROJECTION`, select
**"Media and content projection, streaming"** (not "Other").

**Justification (paste this):**

> The user taps Record in the app's Screen Recorder tool, which triggers the
> system MediaProjection consent dialog. Only after the user grants that dialog
> does the app start this foreground service to capture the screen (and,
> optionally, the microphone) to a local video file the user then saves or
> shares. The service runs a persistent notification with a Stop action for the
> whole recording, never starts on its own, never starts before the user grants
> the system consent dialog, and stops as soon as the user stops recording.
> Recordings are stored locally on the device and are never uploaded.

---

## 3. `RECORD_AUDIO`

**Used by:** Sound Meter, Spectrum Analyzer, Pitch Tuner, Voice Recorder, and
(Batch 7) optional microphone audio in Screen Recorder.

**Justification (paste this):**

> The microphone is used only inside tools the user explicitly opens: a sound-level
> meter, a spectrum analyzer, a pitch tuner, a voice-memo recorder, and an optional
> microphone track for screen recordings. Permission is requested at the moment the
> user starts one of these tools, never at app launch. Audio is processed on-device;
> voice memos and screen recordings are stored locally and are never uploaded.

---

## 4. `READ_MEDIA_IMAGES` (Batch 7 — Duplicate Photo Finder)

**Android 13+ (API 33+).** On API 26–32 the equivalent is
`READ_EXTERNAL_STORAGE` (`maxSdkVersion="32"`).

**Play Console:** This is a Photo/Video permission. Complete the **Photo and
Video Permissions declaration**; the tool needs broad library access (it scans
for duplicates across the whole library), so the Android Photo Picker is not a
substitute — state that in the declaration.

**Justification (paste this):**

> The Duplicate Photo Finder tool reads the user's image library to detect
> duplicate and near-duplicate photos so the user can review and delete them and
> reclaim storage. Finding duplicates inherently requires scanning the whole
> library, which the system Photo Picker cannot do (it only returns a manual
> hand-picked selection). Permission is requested only when the user opens this
> specific tool, never at app launch. All image analysis (hashing/comparison)
> happens entirely on-device; no image or derived data ever leaves the device or
> is uploaded to any server.

---

## 5. `MANAGE_EXTERNAL_STORAGE` (Batch 7 — Storage Analyzer)  ⚠ HIGH-RISK

**Android 11+ (API 30+).** On API 26–29 the tool falls back to
`READ_EXTERNAL_STORAGE`.

> ⚠ **Policy risk:** All-Files-Access is a highly restricted permission. Google
> Play requires a separate **All files access** declaration in the Console and
> reviews it manually; utility/analyzer apps are sometimes rejected. If review is
> refused, the fallback is to ship Storage Analyzer as a **scoped SAF (folder
> picker)** tool instead — that path needs no declaration. Keep that fallback
> ready before submitting.

**Play Console:** Under App content → **Permissions declaration**, declare the
"All files access" permission. Core-functionality reason: **"File management"**
(the app presents a file/storage manager and analyzer).

**Justification (paste this):**

> The Storage Analyzer tool shows the user what is consuming space on their
> device — largest files, biggest folders, and breakdown by file type — so they
> can find and delete space-hogging content. To size and categorize files across
> the whole device (including large media, downloads, and app-exported files in
> arbitrary folders) the tool must enumerate the shared storage volume, which
> scoped/MediaStore access cannot fully do. All-Files-Access is used solely for
> this on-device analysis; the app performs no automatic deletion, uploads
> nothing, and reads file metadata (path, size, type, modified date) to build the
> report. Every deletion is an explicit user action on a specific file.

**Fallback if rejected:** Re-submit Storage Analyzer using
`ACTION_OPEN_DOCUMENT_TREE` (SAF) — the user grants one folder subtree and the
tool analyzes only that. No declaration required.

---

## Data-safety recap (all Batch 7 additions)

- No data collection, no data sharing, no network transmission of any media,
  file, or recording. All processing is on-device.
- Every sensitive permission is requested lazily, at the point the specific tool
  is opened — cold start still grants nothing.
