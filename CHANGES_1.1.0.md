# Temporatic v1.1.0 — Accessibility Service Removal + App Folder Organization

## Version

- **Version**: 1.1.0 (was 1.0.4-dev)
- **Version Code**: 26053001

## Summary

Removed the `AccessibilityService` and all app-categorization logic to make the app Play Store-compliant. Google's 2023–2024 policy changes prohibit accessibility service usage for non-accessibility purposes (foreground app tracking, volume key interception).

The app now uses only Play Store-safe APIs: `MediaProjection`, `SYSTEM_ALERT_WINDOW`, and SAF.

---

## Files Deleted

| File | Reason |
|---|---|
| `app/src/main/java/.../service/TemporaticAccessibilityServiceV2.kt` | Accessibility service — tracked foreground app + intercepted volume keys |
| `app/src/main/res/xml/accessibility_service_config.xml` | Configuration for the accessibility service |
| `app/src/main/java/.../processing/ContextEngine.kt` | Enriched raw screenshot events with foreground app info (depended on accessibility service) |
| `app/src/main/java/.../processing/AppNameResolver.kt` | Resolved package names to app labels (only used by ContextEngine + TemporaticService) |
| `app/src/main/java/.../ui/AppListScreen.kt` | App-based gallery listing (no longer needed — no app categorization) |
| `app/src/main/java/.../ui/AppDetailScreen.kt` | Per-app screenshot grid (same reason) |
| `integration_files/TemporaticAccessibilityServiceV2.kt` | Mirror of deleted main file |
| `integration_files/ContextEngine.kt` | Mirror of deleted main file |
| `integration_files/AppNameResolver.kt` | Mirror of deleted main file |
| `integration_files/AppListScreen.kt` | Mirror of deleted main file |
| `integration_files/AppDetailScreen.kt` | Mirror of deleted main file |

---

## Files Modified

### `AndroidManifest.xml`
- Removed `QUERY_ALL_PACKAGES` permission
- Removed accessibility service declaration, intent filter, and meta-data

### `MainActivity.kt`
- Removed accessibility import, polling, and setup card (Step 1 of wizard)
- Renumbered wizard steps: now 2 steps (Overlay + Folder)
- `allReady` guard no longer checks accessibility
- Navigation routes simplified: `app_list` + `app_detail/{appLabel}` → single `gallery` route
- Removed `openAccessibilitySettings()` method
- "View Captured Screenshots" now opens in-app gallery via NavController

### `TemporaticService.kt`
- Removed `AppNameResolver` injection
- Removed all `TemporaticAccessibilityServiceV2` references (foreground app lookup)
- Simplified `handleCapture()` — no app label resolution, just saves the screenshot

### `StorageManager.kt`
- Removed `appLabel` parameter from `saveScreenshot()` and `copyScreenshot()`

### `domain/ScreenshotEvent.kt`
- Removed `EnrichedScreenshot` data class (was used by ContextEngine → FileProcessor pipeline)

### `processing/FileProcessor.kt`
- Signature changed: takes `ScreenshotEvent.SystemScreenshotDetected` directly instead of `EnrichedScreenshot`
- No more app enrichment — just copies file, creates record, deletes original

### `service/ScreenshotProcessingOrchestrator.kt`
- Removed `ContextEngine` dependency
- Simplified to pipe events directly to `FileProcessor`

### `data/ScreenshotRecord.kt`
- Removed columns: `appLabel`, `appPackage`, `sessionId`

### `data/ScreenshotDao.kt`
- Removed queries: `getByApp`, `getAllApps`, `countByApp`, `getBySession`

### `data/ScreenshotRepository.kt`
- Removed matching methods for deleted DAO queries

### `data/ScreenshotDatabase.kt`
- Version bumped to 2 (schema change)
- Added `fallbackToDestructiveMigration(false)` to handle the column removals

### `ui/ScreenshotViewModel.kt`
- Removed app-based state (`selectedApp`, `allApps`, `getScreenshotsForApp`, `selectApp`)
- Simplified to expose `allScreenshots` flow (flat list of all records)

### `res/values/strings.xml`
- Removed `accessibility_service_description` string resource

### Integration files (in `integration_files/`)
- All mirror files updated to match their main counterparts

---

## New Files

| File | Description |
|---|---|
| `app/src/main/java/.../ui/ScreenshotGalleryScreen.kt` | Single flat gallery showing all screenshots in a 2-column grid. Replaces both `AppListScreen` + `AppDetailScreen`. |

---

## Added Back: App Folder Organization (v1.1.0 feature)

After stripping the AccessibilityService, a toggle on the main screen (`"Organize by App"`) was added to let users choose between flat storage and app-named subfolders.

### What it does
- **Flat mode** (default): All live captures save to the root SAF folder.
- **App-folder mode**: Each live capture is saved into a subfolder named after the foreground app (e.g., `Temporatic/Clash of Clans/Screenshot_...png`). The app label is stored in Room so the gallery can be filtered by app.

### Files modified for this feature

| File | Change |
|---|---|
| `AndroidManifest.xml` | Added `PACKAGE_USAGE_STATS` permission |
| `MainActivity.kt` | Added "Organize by App" Switch + divider; re-added `AppListScreen`/`AppDetailScreen` nav routes |
| `TemporaticService.kt` | Injected `AppNameResolver`, `ForegroundAppTracker`, `ScreenshotRepository`; detects foreground app when org mode on; inserts DB record after each capture |
| `StorageManager.kt` | Added optional `subfolder` param to `saveScreenshot()`/`copyScreenshot()`; added `getOrCreateSubfolder()` |
| `ScreenshotRecord.kt` | Re-added `appLabel: String?` column |
| `ScreenshotDao.kt` | Re-added `getByApp`, `getAllApps`, `countByApp` queries |
| `ScreenshotRepository.kt` | Re-added matching methods |
| `ScreenshotDatabase.kt` | Version bumped to 3 |
| `ScreenshotViewModel.kt` | Re-added `allApps`, `getScreenshotsForApp()`, `countByApp()` |
| `ui/AppListScreen.kt` | Re-created with "All Screenshots" option + per-app list |
| `ui/AppDetailScreen.kt` | Re-created with per-app grid |
| `processing/AppNameResolver.kt` | Re-created (package → label resolver via PackageManager) |

### Files created for this feature

| File | Description |
|---|---|
| `processing/ForegroundAppTracker.kt` | Detects foreground app via `UsageStatsManager.queryUsageStats()` |

---

## Remaining API Surface

| API | Purpose |
|---|---|
| `MediaProjection` | Screen capture via `VirtualDisplay` + `ImageReader` |
| `SYSTEM_ALERT_WINDOW` | Floating camera button + flash overlay |
| `PACKAGE_USAGE_STATS` | Foreground app detection for app-folder organization (opt-in toggle) |
| `FOREGROUND_SERVICE` + `mediaProjection` type | Live capture foreground service |
| `FOREGROUND_SERVICE` + `dataSync` type | Screenshot observer foreground service |
| `READ_MEDIA_IMAGES` | System screenshot detection via MediaStore |
| SAF (`OpenDocumentTree`) | User-chosen save folder with persistable URI permission |
| `POST_NOTIFICATIONS` | Foreground service notifications |
