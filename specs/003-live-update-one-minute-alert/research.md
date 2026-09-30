# Technical Research: Live Update Chip 1-Minute Urgency Alert

**Feature**: `003-live-update-one-minute-alert`  
**Date**: 2026-09-30  

## 1. Notification Style & Live Update Chip (Android 16+ / API 36+)

### Decision
Decouple the `alertOnce` parameter from the segment color determination in `LockTimerService.buildNotification()`. Pass an explicit `isUrgent: Boolean = remainingSeconds <= ONE_MINUTE_SECONDS` (or evaluate it directly from `remainingSeconds`), ensuring that:
1. When `remainingSeconds > 60`, `segmentColor` is `R.color.bamboo_green_40`.
2. When `remainingSeconds <= 60`, `segmentColor` is `R.color.notification_alert_color`.
3. The urgency color persists across all subsequent countdown ticks (59s, 58s, ... 1s) until timer expiration or cancellation.
4. If a timer starts with `durationSeconds <= 60`, it starts in the urgency state immediately.

### Rationale
- In Android 16 (API 36+), the Live Update chip in the status bar visualizes the ongoing notification styled with `Notification.ProgressStyle`.
- The segment color configured via `Notification.ProgressStyle.Segment(durationSeconds.toInt()).setColor(segmentColor)` dictates the tint of the progress pill.
- The previous implementation briefly set `segmentColor = notification_alert_color` only when `alertOnce == false` was passed during the single `postOneMinuteAlert()` call. The very next second tick reverted `alertOnce` to `true`, causing the chip to immediately flash amber and return to green.
- Deriving or passing `isUrgent` allows the urgency visual state to persist stably throughout the entire final minute.

### Alternatives Considered
- **Timer state machine enum extension (`TimerState.Urgent`)**: Rejected because `TimerRepository.timerState` is UI-facing (`TimerState.Running(totalSeconds, remainingSeconds, startedAtMillis)`) and the UI already knows `remainingSeconds`. The foreground notification update is purely an internal presentation concern within `LockTimerService`.
- **System animation / pulse triggering**: Rejected because Android 16 does not provide a public API to programmatically command the status bar chip to expand or strobe repeatedly; color and icon segment state are the only supported developer-controlled styling properties.

---

## 2. Color Contrast & WCAG-AA Compliance

### Decision
Define theme-aware `notification_alert_color` resources:
- In `res/values/colors.xml` (Light theme): `#A05A00` (deep ochre amber).
  - Against white (`#FFFFFF`, $L=1.0$): contrast ratio is **4.5:1** (exceeds WCAG-AA 3:1 for UI components).
  - Against washi surface (`#FFF8F5EF`, $L \approx 0.94$): contrast ratio is **4.2:1** (exceeds WCAG-AA 3:1).
- In `res/values-night/colors.xml` (Dark theme): `#FFB03A` (warm golden amber).
  - Against ink surface (`#FF111210`, $L \approx 0.008$): contrast ratio is **9.9:1** (exceeds WCAG-AA 3:1 and AAA 7:1).
  - Against black (`#000000`, $L=0$): contrast ratio is **11.5:1**.

### Rationale
- Clarification Session 2026-09-30 established that the urgency color must meet the WCAG-AA minimum 3:1 contrast ratio against both light and dark system backgrounds.
- Single static `#FFB03A` in light mode provides only ~1.8:1 contrast against light status bars and notification surfaces. Providing an adaptive resource in `values` and `values-night` guarantees legible contrast in both display modes without requiring runtime calculations.

### Alternatives Considered
- **Single high-contrast color for both modes**: Any color with $\ge 3:1$ contrast against both white and black must have luminance around $0.18$, which appears brown/muddy in dark mode and fails to deliver an intuitive "warm amber warning" aesthetic.
- **Runtime dynamic contrast calculation**: Overcomplicates notification building and adds unnecessary allocations during per-second ticks.

---

## 3. Backward Compatibility (API < 36)

### Decision
Preserve the existing `Build.VERSION.SDK_INT >= 36` conditional branch in `LockTimerService.buildNotification()`.
- On API 33..35: `NotificationCompat.Builder` with progress bar and large lock icon bitmap is used. The 1-minute alert heads-up notification on `CHANNEL_ALERT_ID` continues to fire unchanged.
- On API 36+: In addition to the heads-up notification, `Notification.ProgressStyle` receives the persistent urgency segment color.

### Rationale
- Android 16 introduces `ProgressStyle` and `requestPromotedOngoing`. Devices running earlier Android versions do not have the Live Update chip API. The existing heads-up alert path remains the primary notification cue for those platforms.

---

## 4. Test Strategy

### Decision
Add a focused Robolectric unit test class `LockTimerServiceNotificationTest` (or helper verification test) that:
1. Verifies the segment color logic for running countdowns:
   - `remainingSeconds > 60` yields standard theme green (`bamboo_green_40`).
   - `remainingSeconds <= 60` yields urgency alert color (`notification_alert_color`).
   - `remainingSeconds == 60` (exact boundary) yields urgency alert color.
2. Verifies short duration behavior (`durationSeconds <= 60` starts immediately in urgency state).
3. Verifies that color resources resolve correctly in light and night configurations.
