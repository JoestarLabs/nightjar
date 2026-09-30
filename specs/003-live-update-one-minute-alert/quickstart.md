# Quickstart: Validating Live Update Chip 1-Minute Urgency Alert

**Feature**: `003-live-update-one-minute-alert`  
**Date**: 2026-09-30  

## Automated Verification

### 1. Run Unit Tests
Execute the local unit test suite covering `LockTimerService` notification generation, color evaluation, and countdown state logic:

```bash
./gradlew testDebugUnitTest --tests "com.bl4ckswordsman.nightjar.service.*"
```

Expected result: All tests pass without warnings or failures.

### 2. Run Lint & Static Analysis
Verify that all XML color resources and Kotlin code changes satisfy Android lint and project rules:

```bash
./gradlew lint
```

Expected result: `BUILD SUCCESSFUL` with 0 lint errors.

---

## Manual Verification (Physical Device or Emulator API 36+)

### Prerequisites
- Device or emulator running Android 16 (API 36+).
- Notifications permission granted to Nightjar.

### Scenario 1: Standard Countdown (1-minute transition)
1. Launch Nightjar and set a timer for 1 minute 15 seconds (75s).
2. Start the timer and return to the home screen or another application.
3. Observe the status bar:
   - **T = 75s to 61s**: The Live Update chip appears with a green segment progress indicator (`bamboo_green_40`).
4. Wait until the timer crosses the 1-minute mark (60s):
   - **T = 60s**: The heads-up notification drops down briefly announcing "1 minute remaining".
   - **T = 60s to 1s**: The Live Update chip progress segment changes to warm amber (`notification_alert_color`) and **remains amber** on every subsequent tick.
5. Wait until expiration (0s):
   - The device locks screen as expected.

### Scenario 2: Short Duration Timer (< 60 seconds)
1. Set a timer for 30 seconds.
2. Start the timer and return to the home screen.
3. Observe the status bar:
   - **T = 30s**: The Live Update chip appears **immediately** with the amber segment color (`notification_alert_color`).
   - No unnecessary 1-minute heads-up notification is posted.
   - The chip stays amber until timer expiration at 0s.

### Scenario 3: Light & Dark Theme Appearance
1. Switch device system theme to **Light Mode**.
2. Run Scenario 1 or 2: Verify that the amber chip progress indicator is clearly legible and satisfies contrast against the light status bar.
3. Switch device system theme to **Dark Mode**.
4. Run Scenario 1 or 2: Verify that the golden amber chip is luminous and clearly visible against the dark status bar.
