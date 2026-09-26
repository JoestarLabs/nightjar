# Quickstart & Verification Guide: Audible Sunset Warning Chime

**Feature**: `001-sunset-chime`
**Date**: 2026-09-26

## 1. Automated Verification (Unit Tests)

Run existing and new unit tests to ensure preferences and service triggers behave as expected:

```bash
# Run unit tests covering TimerPreferences and Audio Player triggers
./gradlew testDebugUnitTest --tests "com.bl4ckswordsman.nightjar.data.TimerPreferencesTest"
./gradlew testDebugUnitTest --tests "com.bl4ckswordsman.nightjar.data.TimerPreferencesDataSourceTest"
```

Expected output:
```text
BUILD SUCCESSFUL
All tests pass.
```

---

## 2. Manual Verification Workflow

### Test Scenario A: Chime Plays on Warning Threshold
1. Open Nightjar and ensure Sunset Mode is enabled.
2. Verify in Settings that **"Sunset sound alert"** is toggled ON.
3. Start a 45-second timer with a 30-second sunset duration.
4. Watch the countdown:
   - At 45s down to 31s: no sound.
   - Exactly at 30s: the Rising Wave overlay begins AND a gentle notification chime plays once.
   - At 29s down to 0s: wave continues, but NO repeat chime sounds.

### Test Scenario B: Chime Disabled in Settings
1. Go to Settings and toggle **"Sunset sound alert"** to OFF.
2. Start a 45-second timer.
3. Observe at 30s:
   - Rising Wave overlay appears as normal.
   - No audio chime is emitted.

### Test Scenario C: Device in Silent / Do Not Disturb
1. Put the physical device or emulator into Silent or Do Not Disturb mode.
2. Ensure the chime toggle is ON.
3. Run the countdown past the 30-second threshold.
4. Verify no audible alert interrupts the silent profile.
