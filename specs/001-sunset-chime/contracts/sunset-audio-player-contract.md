# Interface Contract: Sunset Audio Player

**Feature**: `001-sunset-chime`
**Date**: 2026-09-26

## 1. Domain Interface Contract

```kotlin
package com.bl4ckswordsman.nightjar.service

/**
 * Audio playback contract for alerting users during timer warning transitions.
 */
interface SunsetAudioPlayer {
    /**
     * Plays the sunset warning chime if device audio profile allows.
     * Must be non-blocking and safe to call from background coroutines/services.
     */
    fun playChime()
}
```

## 2. Implementation Specifications

### `AndroidSunsetAudioPlayer`
- **Location**: `com.bl4ckswordsman.nightjar.service.AndroidSunsetAudioPlayer`
- **Injection**: Injected via Hilt (`@Singleton` or `@Provides` in `ServiceModule` / `AppModule`).
- **Dependencies**: `@ApplicationContext context: Context`.
- **Behavior**:
  - Obtains `RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)`.
  - Configures `AudioAttributes.Builder()`:
    - `.setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)`
    - `.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)`
  - Plays chime asynchronously.
  - Catches and logs any security/platform exceptions without crashing the background timer service.

## 3. UI Settings Toggle Contract

### Settings Screen Component
- **Title**: `Sunset sound alert`
- **Subtitle**: `Play a gentle chime when the sunset warning begins`
- **Widget**: Material 3 `Switch` or `SwitchPreference`
- **Bound State**: `TimerPreferences.sunsetAudioEnabled`
- **Enabled Condition**: Active only if `TimerPreferences.sunsetModeEnabled == true` (dimmed or disabled if sunset warning itself is off).
