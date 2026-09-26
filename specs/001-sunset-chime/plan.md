# Implementation Plan: Audible Sunset Warning Chime

**Branch**: `001-sunset-chime` | **Date**: 2026-09-26 | **Spec**: [specs/001-sunset-chime/spec.md](spec.md)

**Input**: Feature specification from `specs/001-sunset-chime/spec.md`

## Summary

Implement an audible chime notification that fires once when an active timer enters the sunset warning phase (complementing the existing Rising Wave overlay). This includes:
1. Extending `TimerPreferences` and `TimerPreferencesDataSource` with a new `sunset_audio_enabled` DataStore boolean.
2. Introducing an injected `SunsetAudioPlayer` interface with `AndroidSunsetAudioPlayer` implementation using system notification audio attributes.
3. Hooking the trigger into `LockTimerService` to play the chime strictly once per countdown run.
4. Adding a user-facing toggle switch under the Sunset section in `SettingsScreen.kt`.
5. Adding unit tests for preferences and service audio triggers.

## Technical Context

**Language/Version**: Kotlin 2.0+ (JVM target 11 / JDK 17), Android SDK (minSdk 33, targetSdk 36, compileSdk 37)

**Primary Dependencies**:
- Jetpack Compose (Material 3 Expressive)
- AndroidX DataStore Preferences
- Google Hilt DI
- Kotlin Coroutines & Flow
- Android Media (`RingtoneManager`, `AudioAttributes`)

**Storage**: Jetpack DataStore Preferences (`nightjar_timer_prefs`)

**Testing**: JUnit 4, Mockito-Kotlin, Robolectric, Kotlinx Coroutines Test, Compose UI Test

**Target Platform**: Android 13+ (API 33+)

**Project Type**: Mobile Application (Android)

**Performance Goals**: Audio alert triggers within < 50ms of crossing warning threshold without causing frame drops on Compose liquid wave animation.

**Constraints**:
- Must not interrupt device silent/vibrate/DND profiles.
- Must not repeat playback on screen rotation, pause, or redraws.
- Must add zero binary asset overhead (utilizes system notification ringtone sound).

**Scale/Scope**: 1 new DataStore key, 1 new domain audio player interface/impl, 1 service hook in `LockTimerService`, 1 toggle row in `SettingsScreen`, 2 unit test file extensions.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Requirement | Plan Alignment | Status |
| :--- | :--- | :--- | :--- |
| **I. Declarative UI & State Hoisting** | Stateless composables, hoisted state, M3 Expressive | Settings toggle implemented in Compose hoisted to `SettingsViewModel` via `StateFlow` | **PASS** |
| **II. Unidirectional Data Flow & MVVM** | UDF, read-only `StateFlow`, repository `Flow`, no context leaks | Preference exposed via `TimerPreferencesDataSource.preferences` Flow | **PASS** |
| **III. Dependency Injection via Hilt** | Hilt injection for services and components | `SunsetAudioPlayer` injected into `LockTimerService` / Hilt modules | **PASS** |
| **IV. Test-Driven Verification** | Unit tests for data/logic, `./gradlew lint` & `./gradlew test` pass | New unit tests added for `TimerPreferences` and `TimerPreferencesDataSource` | **PASS** |
| **V. Clean Service Boundaries** | Decoupled background service, Android 13+ compliance | Triggered inside `LockTimerService` with non-blocking audio dispatch | **PASS** |

## Project Structure

### Documentation (this feature)

```text
specs/001-sunset-chime/
├── spec.md              # Feature specification
├── plan.md              # Implementation plan (this file)
├── research.md          # Technical research & decisions
├── data-model.md        # DataStore schema and state transitions
├── quickstart.md        # Manual and automated verification guide
├── contracts/
│   └── sunset-audio-player-contract.md # Domain player & UI contract
└── checklists/
    └── requirements.md  # Requirements quality gate checklist
```

### Source Code (repository layout)

```text
app/src/main/java/com/bl4ckswordsman/nightjar/
├── data/
│   ├── TimerPreferences.kt                # Add sunset_audio_enabled key & model field
│   └── TimerRepository.kt                 # (Existing repository)
├── service/
│   ├── SunsetAudioPlayer.kt               # New domain interface & Android implementation
│   ├── LockTimerService.kt                # Hook chime trigger upon entering sunset window
│   └── ComposeOverlayManager.kt           # (Existing visual wave overlay)
└── ui/
    └── screen/
        └── SettingsScreen.kt              # Add "Sunset sound alert" toggle in sunset section

app/src/test/java/com/bl4ckswordsman/nightjar/
├── data/
│   ├── TimerPreferencesTest.kt            # Unit test for default sunsetAudioEnabled
│   └── TimerPreferencesDataSourceTest.kt  # Unit test for persisting sunsetAudioEnabled
```

**Structure Decision**: Standard Android single-application structure leveraging existing `data`, `service`, and `ui` packages with Hilt dependency injection.

## Complexity Tracking

*No constitution violations or unjustifiable complexities introduced.*
