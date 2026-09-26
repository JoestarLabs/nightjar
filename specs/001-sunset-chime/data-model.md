# Data Model: Audible Sunset Warning Chime

**Feature**: `001-sunset-chime`
**Date**: 2026-09-26

## 1. Entities & Schema

### `TimerPreferences` (DataStore Entity)
Extended in `com.bl4ckswordsman.nightjar.data.TimerPreferences`:

| Field | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `lastDurationSeconds` | `Long` | `300L` | Previous countdown duration |
| `startedAtMillis` | `Long` | `0L` | Timestamp of running timer (0 if idle) |
| `commitmentMode` | `Boolean` | `false` | Whether emergency unlock is required |
| `customPresets` | `List<Long>` | `[300, 900, 1800, 3600]` | Preset durations in seconds |
| `sunsetModeEnabled` | `Boolean` | `true` | Whether visual sunset warning is enabled |
| `sunsetDurationSeconds` | `Long` | `30L` | Warning duration window in seconds |
| **`sunsetAudioEnabled`** | **`Boolean`** | **`true`** | **New field**: Whether audible chime plays when entering sunset warning |

### `TimerPreferenceKeys`
- `val SUNSET_AUDIO_ENABLED = booleanPreferencesKey("sunset_audio_enabled")`

---

## 2. State Lifecycle & Transitions

```
[Timer Idle]
      │
      ▼ (ACTION_START)
[Timer Running] (sunsetChimePlayed = false)
      │
      ├─────────────────────────────────────────────────┐
      │ remaining > sunsetDuration                      │ remaining <= sunsetDuration
      │                                                 │ && sunsetModeEnabled
      │                                                 │ && sunsetAudioEnabled
      │                                                 │ && !sunsetChimePlayed
      ▼                                                 ▼
[Countdown Ticking]                           [Play Sunset Chime Once]
                                                        │
                                                        ▼
                                              (sunsetChimePlayed = true)
                                                        │
                                                        ▼
                                              [Sunset Overlay Running]
                                                        │
                                                        ▼ (Timer Finished / Cancelled)
                                              [Timer Idle] (Reset state)
```

---

## 3. Validation Rules
- `sunsetAudioEnabled` defaults to `true`.
- Persisted asynchronously via DataStore edit transaction.
- If `sunsetModeEnabled == false`, `sunsetAudioEnabled` is ignored during runtime (no sound played).
