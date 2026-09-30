# Implementation Plan: Live Update Chip 1-Minute Urgency Alert

**Branch**: `feat/003-live-update-one-minute-alert` | **Date**: 2026-09-30 | **Spec**: [specs/003-live-update-one-minute-alert/spec.md](spec.md)

**Input**: Feature specification from `specs/003-live-update-one-minute-alert/spec.md`

## Summary

Make the status bar Live Update notification chip (Android 16+ / API 36+) catch the user's attention at the 1-minute remaining mark by transitioning its `ProgressStyle` segment color to an accessible, high-contrast urgency color (amber) and keeping it in this urgency state persistently until the timer ends.

Key modifications:
1. **Decouple Urgency from `alertOnce` in `LockTimerService`**:
   - Introduce an explicit `isUrgent` parameter in `buildNotification` (defaulting to `remainingSeconds <= ONE_MINUTE_SECONDS`).
   - When `isUrgent` is `true`, set `Notification.ProgressStyle.Segment` color to `R.color.notification_alert_color`.
   - Ensure the urgency segment color persists across every tick from 60s down to 1s.
   - For timers started with $\le 60$ seconds, ensure the initial foreground notification starts immediately in the urgency state.
2. **WCAG-AA Accessibility Color Contrast**:
   - Provide theme-specific `notification_alert_color` definitions:
     - `res/values/colors.xml` (Light theme): `#A05A00` ($\ge 4.2:1$ contrast against light background).
     - `res/values-night/colors.xml` (Dark theme): `#FFB03A` ($\ge 9.9:1$ contrast against dark background).
3. **Automated Testing**:
   - Add unit tests in `LockTimerServiceNotificationTest` to verify notification segment coloring and short-timer behavior under Robolectric.

## Technical Context

**Language/Version**: Kotlin 2.2.10 (JVM target 17 / JDK 17), Android SDK (minSdk 33, targetSdk 36, compileSdk 37)

**Primary Dependencies**:
- Android Framework Notifications (`android.app.Notification`, `Notification.ProgressStyle`)
- AndroidX Core (`androidx.core.app.NotificationCompat`)
- Google Hilt DI
- Kotlin Coroutines & Flow

**Storage**: N/A (runtime notification visual styling only; no new persistence keys needed)

**Testing**: JUnit 4, Robolectric, Mockito-Kotlin

**Target Platform**: Android 13+ (API 33+) with Android 16+ (API 36+) enhancements

**Project Type**: Mobile Application (Android)

**Performance Goals**: Zero additional allocations or frame delays during the 1 Hz countdown loop. Notification update dispatch overhead $< 1$ ms per tick.

**Constraints**:
- Must comply with Android 16 Live Updates / Rich Ongoing Notifications API guidelines.
- Must satisfy WCAG-AA ($\ge 3:1$) contrast ratio in both Light and Dark system themes.
- Must maintain backward compatibility for API 33..35 without regressions.

**Scale/Scope**: 1 service class updated (`LockTimerService.kt`), 2 resource files updated (`values/colors.xml`, `values-night/colors.xml`), 1 new test file (`LockTimerServiceNotificationTest.kt`).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Requirement | Plan Alignment | Status |
| :--- | :--- | :--- | :--- |
| **I. Declarative UI & State Hoisting** | Stateless composables, hoisted state, M3 Expressive | Notification rendering is a service presentation layer; no Compose UI regressions | **PASS** |
| **II. Unidirectional Data Flow & MVVM** | UDF, read-only `StateFlow`, repository `Flow`, no context leaks | Timer state flow remains pure; notification consumes existing service countdown variables | **PASS** |
| **III. Dependency Injection via Hilt** | Hilt injection for services and components | `LockTimerService` retains existing `@AndroidEntryPoint` and injected dependencies | **PASS** |
| **IV. Test-Driven Verification** | Unit tests for data/logic, `./gradlew lint` & `./gradlew test` pass | New unit tests added in `LockTimerServiceNotificationTest.kt` verifying segment colors | **PASS** |
| **V. Clean Service Boundaries** | Decoupled background service, Android 13+ compliance | Contained entirely within `LockTimerService` foreground service lifecycle | **PASS** |

## Project Structure

### Documentation (this feature)

```text
specs/003-live-update-one-minute-alert/
├── spec.md              # Feature specification
├── plan.md              # Implementation plan (this file)
├── research.md          # Technical research & decisions (Phase 0)
├── data-model.md        # State transitions & color models (Phase 1)
├── quickstart.md        # Manual and automated verification guide (Phase 1)
├── contracts/
│   └── live-update-notification-contract.md # Notification builder contract (Phase 1)
└── checklists/
    └── requirements.md  # Requirements quality gate checklist
```

### Source Code (repository layout)

```text
app/src/main/
├── java/com/bl4ckswordsman/nightjar/
│   └── service/
│       └── LockTimerService.kt        # Update buildNotification & postOneMinuteAlert
└── res/
    ├── values/
    │   └── colors.xml                 # Light theme notification_alert_color (#A05A00)
    └── values-night/
        └── colors.xml                 # Dark theme notification_alert_color (#FFB03A)

app/src/test/java/com/bl4ckswordsman/nightjar/
└── service/
    └── LockTimerServiceNotificationTest.kt # Robolectric unit tests for segment colors
```

**Structure Decision**: Standard Android single-application structure leveraging existing `service` package and Android resource qualifiers (`values` and `values-night`).

## Complexity Tracking

*No constitution violations or unjustifiable complexities introduced.*
