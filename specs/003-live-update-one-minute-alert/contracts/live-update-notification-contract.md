# Contract: Live Update Notification & Visual Styling

**Feature**: `003-live-update-one-minute-alert`  
**Date**: 2026-09-30  

## Component Boundary

This contract specifies the behavior of `LockTimerService` when assembling and dispatching ongoing notifications across Android versions and timer states.

```text
+--------------------------------------------------------------+
|                      LockTimerService                        |
|                                                              |
|  +------------------------+      +-------------------------+ |
|  | Countdown Loop (Tick)  | ---> |   buildNotification()   | |
|  +------------------------+      +-------------------------+ |
+-----------------------------------------------|--------------+
                                                |
                     +--------------------------+--------------------------+
                     | (API >= 36)                                         | (API < 36)
                     v                                                     v
       +-------------------------------+                     +-------------------------------+
       | Notification.ProgressStyle    |                     | NotificationCompat.Builder    |
       | - Ongoing promoted chip       |                     | - Ongoing status bar icon     |
       | - Segment color (Green/Amber) |                     | - Progress bar (0..total)     |
       | - Chronometer count-down      |                     | - Large icon bitmap lock      |
       +-------------------------------+                     +-------------------------------+
                     |                                                     |
                     +--------------------------+--------------------------+
                                                |
                                                v
                               +---------------------------------+
                               | NotificationManager.notify()    |
                               | (ID = NOTIFICATION_ID = 1001)   |
                               +---------------------------------+
```

---

## 1. Notification Builder Parameters Contract

### Function Signature
```kotlin
internal fun buildNotification(
    durationSeconds: Long,
    countdownEndEpochMs: Long,
    remainingSeconds: Long,
    alertOnce: Boolean = true,
    isUrgent: Boolean = remainingSeconds <= ONE_MINUTE_SECONDS
): Notification
```

### Input Parameters

| Parameter | Type | Required | Default | Invariants |
| :--- | :--- | :--- | :--- | :--- |
| `durationSeconds` | `Long` | Yes | N/A | Must be $> 0$. Represents the total duration. |
| `countdownEndEpochMs` | `Long` | Yes | N/A | Epoch millisecond timestamp of timer expiration. |
| `remainingSeconds` | `Long` | Yes | N/A | Must be $\ge 0$ and $\le durationSeconds$. |
| `alertOnce` | `Boolean` | No | `true` | `true` disables sound/vibration on notification updates; `false` allows alerting on initial post or urgent alert. |
| `isUrgent` | `Boolean` | No | `remainingSeconds <= ONE_MINUTE_SECONDS` | Dictates segment color. `false` selects normal green; `true` selects alert amber. |

---

## 2. Platform Output Contracts

### Contract A: Android 16+ (API $\ge 36$)

- **Style**: `Notification.ProgressStyle`
- **Progress Value**: `(durationSeconds - remainingSeconds).toInt()`
- **Segment Config**:
  - Segment max: `durationSeconds.toInt()`
  - Segment color:
    - If `!isUrgent`: `ContextCompat.getColor(context, R.color.bamboo_green_40)`
    - If `isUrgent`: `ContextCompat.getColor(context, R.color.notification_alert_color)`
- **End Icon**: Lock icon (`R.drawable.ic_lock_notification`) tinted with `R.color.notification_icon_tint`
- **Ongoing Request**: `Bundle` with `android.requestPromotedOngoing = true`
- **Chronometer**: `usesChronometer = true`, `chronometerCountDown = true`, `when = countdownEndEpochMs`

### Contract B: Legacy Platforms (API $33..35$)

- **Style**: Standard `NotificationCompat.Builder`
- **Progress**: `setProgress(durationSeconds.toInt(), (durationSeconds - remainingSeconds).toInt(), false)`
- **Large Icon**: 120x120 Bitmap of `ic_lock_notification` tinted with `R.color.notification_icon_tint`
- **Chronometer**: `setUsesChronometer(true)`, `setChronometerCountDown(true)`, `setWhen(countdownEndEpochMs)`
- **No ProgressStyle**: Does not invoke `Notification.ProgressStyle` APIs to maintain backward compatibility.

---

## 3. Alert Event Contract (`postOneMinuteAlert`)

When `remaining <= ONE_MINUTE_SECONDS && !alertFired && durationSeconds > ONE_MINUTE_SECONDS`:
1. Dispatch heads-up notification to `NightjarApp.CHANNEL_ALERT_ID` with `NOTIFICATION_ALERT_ID = 1002`.
2. Schedule cancellation of `NOTIFICATION_ALERT_ID` after 5,000 ms.
3. On API $\ge 36$: Immediately update ongoing notification `NOTIFICATION_ID` with `alertOnce = false` and `isUrgent = true`.
4. Subsequent ticks maintain `isUrgent = true` and `alertOnce = true`.
