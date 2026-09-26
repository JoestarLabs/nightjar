# Technical Research: Audible Sunset Warning Chime

**Feature**: `001-sunset-chime`
**Date**: 2026-09-26

## 1. Audio Playback Strategy in Android 13+ (API 33+)

### Decision
Use Android's framework-provided `RingtoneManager` playing `RingtoneManager.TYPE_NOTIFICATION` with `AudioAttributes.USAGE_NOTIFICATION_EVENT` and `AudioAttributes.CONTENT_TYPE_SONIFICATION`, encapsulated behind a `SunsetAudioPlayer` interface.

### Rationale
- **Platform Politeness & Silence Rules**: `Ringtone` configured with notification audio attributes automatically obeys system ringer states (Silent, Vibrate, Do Not Disturb) without requiring manual `AudioManager` volume inspections or runtime audio permissions.
- **Zero Binary Overhead**: Does not bundle external audio asset files (MP3/OGG/WAV) into the APK, keeping the build lean and avoiding audio asset licensing concerns.
- **Service Lifecycle Compatibility**: Can be safely triggered from `LockTimerService` during the background countdown loop without blocking the main looper or memory-leaking media player resources.
- **Testability**: An injected interface allows 100% test isolation with Mockito in unit tests without invoking real audio hardware.

### Alternatives Considered
- **`MediaPlayer` with bundled raw resource**: Rejected because it requires managing `MediaPlayer` state machine transitions, handling release lifecycle, and adds unnecessary asset weight to the app bundle.
- **`SoundPool`**: Good for rapid repeated game sounds, but excessive for a one-time chime. It also does not automatically synchronize with user system notification ringtone preferences.

---

## 2. Preference Storage & DataStore Schema

### Decision
Extend `TimerPreferences` and `TimerPreferencesDataSource` with a new boolean property:
- Preference Key: `sunset_audio_enabled` (`booleanPreferencesKey("sunset_audio_enabled")`)
- Model property: `sunsetAudioEnabled: Boolean = true` (default: enabled)
- Methods: `saveSunsetAudioEnabled(enabled: Boolean)`

### Rationale
- Matches the established pattern in `TimerPreferences.kt` where `sunsetModeEnabled` and `sunsetDurationSeconds` already reside.
- Exposes a reactive `Flow<TimerPreferences>` that both `SettingsViewModel` and `LockTimerService` consume.

### Alternatives Considered
- **Separate DataStore file**: Rejected as unnecessary fragmentation; all timer-related preferences are consolidated in `nightjar_timer_prefs`.

---

## 3. Trigger & Duplicate Prevention Mechanism

### Decision
Maintain a boolean flag `var sunsetChimePlayed = false` in `LockTimerService`.
- Reset to `false` when a timer starts (`ACTION_START`) or stops.
- When `remaining <= sunsetDuration && sunsetEnabled && sunsetAudioEnabled && !sunsetChimePlayed`:
  - Invoke `sunsetAudioPlayer.playChime()`
  - Set `sunsetChimePlayed = true`

### Rationale
- Ensures the chime fires strictly once per timer run.
- Survives sensor tilt events, orientation changes, and wave overlay redraws without repeating.
