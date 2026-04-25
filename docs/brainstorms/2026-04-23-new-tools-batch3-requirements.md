---
date: 2026-04-23
topic: new-tools-batch3
---

# New Tools — Batch 3

## Problem Frame

Toolbox currently holds ~50 tools that are almost entirely sensor readouts and calculators. The app has no tools in the "reach into local data and do something useful with it" category. Five such tools were scoped during brainstorming; after document review, three of them (Status Saver, Duplicate Photo Finder, Storage Analyzer) hit platform-reality blockers that need research spikes before planning. This document narrows Batch 3 to the **two tools that are safe to plan and build now** — Photo Cleanup and WiFi QR Share — and preserves the three deferred tools in a named section so the work isn't lost.

## Requirements

**Photo Cleanup (Compress + Strip Metadata)**
- R1. Single tool titled "Photo Cleanup" with two tabs: Compress and Strip Metadata.
- R2. Multi-select up to 20 images per batch via the system photo picker (`ACTION_PICK_IMAGES` on API 33+, `ACTION_OPEN_DOCUMENT` fallback on older).
- R3. Compress tab: JPEG quality slider (0–100, default 80) and optional max-dimension in pixels applied to the long edge.
- R4. Strip Metadata tab: preview of which EXIF fields will be removed (GPS, device, timestamps, thumbnails); an explicit toggle to keep or strip orientation.
- R4a. The preview displays field names and a present/absent badge only. Raw EXIF values (GPS coordinates, device serial numbers, owner name, raw timestamps) are never rendered in the UI, logs, or error messages — a privacy tool must not leak the values it is about to remove.
- R5. Both tabs always save results as new files under `Pictures/Toolbox/`. Originals are never modified or deleted by this tool.
- R6. Result screen shows before/after file size, EXIF tag count removed (for Strip Metadata), and a direct share-sheet action.
- R7. EXIF stripping uses a full re-encode path (not in-place EXIF edit) so embedded thumbnails and maker-note blobs are also removed.

**WiFi QR Share**
- R8. Manual entry form: SSID, password, security type (WPA/WPA2/WPA3/WEP/None), and a hidden-network checkbox.
- R9. Generates a standard `WIFI:T:<type>;S:<ssid>;P:<pwd>;H:<hidden>;;` QR code. Encoder correctly escapes the metacharacters `; , : \ "` inside SSID and password fields; all-hex SSIDs are wrapped in double quotes per the de-facto spec; `nopass` is used for the `None` security type and `P:` is omitted; the `H:` field is emitted only when hidden is true.
- R10. Save up to 5 networks locally for quick regeneration ("home", "office", "guest" are the canonical use cases). Saved networks never leave the device.
- R10a. WiFi passwords are stored encrypted at rest using `EncryptedSharedPreferences` (or an equivalent Tink-backed DataStore). The underlying file is excluded from Android Auto Backup via `data_extraction_rules.xml` / `full_backup_content.xml` so passwords never flow to Google Drive.
- R11. Actions on a generated QR: share as PNG, copy SSID, copy password (with obscure-by-default display, tap-to-reveal).
- R11a. Copy-password writes a `ClipData` flagged with `ClipDescription.EXTRA_IS_SENSITIVE` on API 33+ so the system keyboard/clipboard manager treats it as sensitive. On API < 33, the app clears the clipboard after 60 seconds if it still holds the same value.

## Success Criteria

- Photo Cleanup and WiFi QR Share ship together as Batch 3, passing QA on Android 8 (min SDK 26), 11, 14, and 15.
- No new policy-sensitive permissions are added: no `MANAGE_EXTERNAL_STORAGE`, no `READ_MEDIA_*` (Photo Cleanup uses the system Photo Picker, which is permissionless), no accessibility service, no SMS/call permissions, no background location.
- Both tools discoverable from the dashboard search with tokens matching name and common user-facing terms (e.g., "metadata", "compress", "exif" → Photo Cleanup; "wifi", "qr", "password" → WiFi QR Share).
- Cold-start behavior is unchanged: opening the app grants nothing. Permissions are requested only on the first use of the relevant tool (in practice, neither of these two tools triggers a runtime permission prompt on modern Android).

## Scope Boundaries

- No `MANAGE_EXTERNAL_STORAGE`, no accessibility service, no foreground service introduced by this batch.
- WiFi QR Share is manual-entry only. No saved-network reading, no location-based SSID scanning.
- Photo Cleanup does not overwrite originals, does not auto-share, does not upload anywhere.
- Status Saver, Duplicate Photo Finder, and Storage Analyzer are **deferred** — see the section below. Not part of this batch.
- Screenshot Stitcher (ideation idea #6) and Document Scanner (ideation idea #3) are also deferred — not part of this batch.

## Key Decisions

- **Decoupled batch.** Photo Cleanup and WiFi QR Share have zero platform-reality risk, no policy-sensitive permissions, and no shared unknowns with the three deferred tools. Shipping them together as a small, low-risk batch delivers value now and creates time for the research spikes the deferred tools need. Why: document review showed three of the five original tools have independent P0 blockers; coupling their release would multiply failure probability.
- **Photo Cleanup uses the system Photo Picker, not `READ_MEDIA_IMAGES`.** `ACTION_PICK_IMAGES` (API 33+) and `ACTION_OPEN_DOCUMENT` (older) are permissionless — the user grants per-file access implicitly by selecting. This sidesteps Play Store's Photo/Video Permissions policy review entirely.
- **Photo Cleanup never overwrites originals.** A destructive default would be a support-ticket magnet and a trust problem; the small storage cost of saving copies is worth it.
- **WiFi QR Share is permission-free.** Saved-network reading is effectively dead on Android 10+ for non-system apps; location permission just to list SSIDs is poor ergonomics for little gain.
- **WiFi QR Share saves up to 5 networks, not 10.** Scoped to the real use case — home, office, guest — without a power-user persistence surface that isn't earning its keep.

## Dependencies / Assumptions

- A new QR-encoder helper will be added under `core/qr/` using zxing-core's `QRCodeWriter` → `BitMatrix` → ARGB_8888 bitmap. The existing `QrScannerScreen` is decode-only today and is not reused. (Verified in code: no `QRCodeWriter` or `encode` usages exist in the module.)
- `core/sharing/MeasurementCard.kt` is card-specific (renders a fixed 800×480 branded measurement card to a single filename). A new `core/sharing/ImageSharer` helper will be added that shares arbitrary bitmaps/PNGs with per-call unique filenames.
- A new `core/media/MediaStoreWriter` helper will be added for saving to `Pictures/Toolbox/` with a `Build.VERSION` branch: API 26–28 (legacy file path + `WRITE_EXTERNAL_STORAGE maxSdkVersion=28`) vs. API 29+ (`RELATIVE_PATH` + `IS_PENDING`).
- `EncryptedSharedPreferences` (`androidx.security.crypto`) is available at min SDK 26. A thin wrapper is fine; we do not need to migrate the existing DataStore favorites/counter state.

## Outstanding Questions

### Resolve Before Planning
- _(none — all product decisions captured above)_

### Deferred to Planning
- [Affects R3][Technical] HEIC/HEIF input support on Android 9+ for the Compress tab. Affects whether we need a decoding fallback or can rely on the platform decoder.
- [Affects R9][Technical] zxing-core version pin and whether we need an additional quiet-zone / error-correction-level decision surfaced to the user or hard-coded.
- [Affects R10a][Technical] `EncryptedSharedPreferences` was marked deprecated in recent AndroidX releases; confirm current recommendation (stay on it, or switch to `MasterKey` + Tink AEAD directly).

## Next Steps

→ `/ce:plan` for structured implementation planning of Photo Cleanup + WiFi QR Share.

---

# Deferred — Needs Research Spike Before Planning

These three tools were part of the original Batch 3 scope but hit platform-reality blockers during document review. Each has a specific research spike that must resolve before the tool can be planned. When the spike completes, move the tool back into a dedicated brainstorm for its own batch.

## Status Saver — Deferred

**Why deferred:** On Android 11+, WhatsApp moved its Statuses folder to `Android/media/com.whatsapp/WhatsApp/Media/.Statuses/`. The `DocumentsContract` SAF picker has been progressively blocking the "Use this folder" button for `Android/media/*` subtrees on Samsung OneUI, MIUI, and ColorOS; Pixel/AOSP currently permits it inconsistently across API levels. MediaStore is not a fallback because dot-prefixed `.Statuses` directories are excluded by design. Without a reliable folder-access path, the tool is undeliverable on a large portion of real devices.

**Research spike needed:**
1. On a fresh Android 14/15 device running each of Samsung OneUI, Xiaomi MIUI, OnePlus OxygenOS, and Pixel/AOSP, launch `ACTION_OPEN_DOCUMENT_TREE` with an initial URI pointing at `Android/media/com.whatsapp/WhatsApp/Media/.Statuses/` and confirm whether the "Use this folder" button is offered.
2. If blocked, test the two-stage navigation workaround (grant `Android/media/` or a higher parent, then descend).
3. Test WA Business (`com.whatsapp.w4b`) separately — the package is different and likely needs a second grant.

**Tentative requirements (to revisit):** R1–R8 from the prior draft — SAF-based status browser, grid, single-save, multi-select, generic naming. Also blocked pending resolution: whether a single SAF grant can cover both WA and WA Business, or whether two separate grants are required.

**Acceptance bar to un-defer:** SAF grant works on at least 3 of 4 tested OEMs on Android 14/15; either single-grant covers both sources OR the two-grant UX is designed upfront.

## Duplicate Photo Finder — Deferred

**Why deferred:** Two independent P0 blockers.

1. **Deletion API gap.** `MediaStore.createDeleteRequest()` is API 30+; app min SDK is 26. On API 26–28 deletion requires `File.delete()` + `WRITE_EXTERNAL_STORAGE` (no system trash); API 29 requires `RecoverableSecurityException` + `IntentSender`. Three code paths, and the "30-day recover via trash" promise breaks on ~10% of the install base.
2. **Perceptual-threshold tuning is the product.** A poorly-tuned pHash threshold deletes non-duplicate photos (burst shots, near-duplicates with meaningful differences). This is not a planning detail; it is what separates "category leader" from "review-bombed". Requires a test corpus and a tuned precision target before the tool can ship.

Adjacent risks: Android 14's partial-grant `READ_MEDIA_VISUAL_USER_SELECTED` silently breaks duplicate-detection semantics (user picks 7 photos, app reports "no duplicates"). Play Store Photo/Video Permissions policy specifically scrutinizes duplicate-finder-class apps.

**Research spike needed:**
1. Decide whether to raise app-wide min SDK to 30 (Android 11, ~94% coverage in 2026) — that removes two of the three delete paths and the "no trash on older" caveat in one move.
2. Build a test corpus (N≥500) spanning burst shots, edited-then-saved variants, screenshots, and near-duplicates. Pick a pHash library and tune a threshold until precision ≥ 0.98.
3. Prototype the partial-grant UX: detect `READ_MEDIA_VISUAL_USER_SELECTED`, show a banner explaining that full access is required for duplicate detection across the whole library, with a "re-pick" affordance.
4. Evaluate whether the Photo Picker's user-selected-photos model can be reframed as a feature ("find duplicates in the folder you pick") to avoid `READ_MEDIA_IMAGES` entirely.

**Tentative requirements (to revisit):** R9–R17 from the prior draft. R17a (cache safety) already proven correct during review and should carry over as-is.

**Acceptance bar to un-defer:** min SDK decision made; tuned threshold hits ≥ 0.98 precision on the corpus; partial-grant UX designed; Photo Picker route evaluated as Plan A, `READ_MEDIA_IMAGES` as Plan B.

## Storage Analyzer — Deferred

**Why deferred:** R24's premise was unimplementable. `StorageStatsManager.queryStatsForUser/queryExternalStatsForUser` require the `PACKAGE_USAGE_STATS` AppOp (granted via a Settings deep-link, not a runtime prompt). Without it, only `getTotalBytes/getFreeBytes` work — no category breakdown. The apps/photos/videos/audio/documents breakdown actually requires either `PACKAGE_USAGE_STATS` (for app-category bytes) or `READ_MEDIA_*` (for media categories aggregated from MediaStore). The requirements doc promised category breakdown with no runtime permission, which no API surface provides.

Secondary concern raised by the product reviewer: a read-only storage tool ships a half-solution ("shows the problem, refuses to fix it") and is a rating-risk pattern. The stated MANAGE_EXTERNAL_STORAGE avoidance is the right constraint, but "read-only analyzer" is not the only response to it.

**Research spike needed:**
1. Decide which reduced scope is the product: **(a)** "free/used + folder drill-down" (no permissions, no categories), **(b)** media-category breakdown via `READ_MEDIA_*` + MediaStore aggregates (drops "no runtime permission"), or **(c)** full app + media breakdown via `PACKAGE_USAGE_STATS` deep-link (drops "lazy permissions" ethos).
2. Revisit the "read-only" decision. SAF-bounded deletion (the user picks a folder, selects a file, deletes that specific file) does not require `MANAGE_EXTERNAL_STORAGE` and makes the tool actionable without pulling the app into file-manager policy territory.
3. If keeping the Overview view, define what makes it different from Android Settings → Storage, otherwise the tool ships as a duplicate.

**Tentative requirements (to revisit):** R24–R28 from the prior draft, plus R17a-style cache safety applied to folder-walk results.

**Acceptance bar to un-defer:** permission model chosen; action model (read-only vs. SAF-bounded delete) chosen; product differentiation from system Settings articulated.
