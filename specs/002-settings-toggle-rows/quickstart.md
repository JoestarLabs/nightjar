# Quickstart: Setting Toggle Rows Tap Target Expansion

## Summary
Expands the interactive tap target of all toggle setting rows in `SettingsScreen.kt` to the full row bounding box using `Modifier.toggleable(role = Role.Switch)`.

## Manual Verification Steps

1. Launch Nightjar on an Android device or emulator.
2. Tap the settings gear icon to navigate to the **Settings** screen.
3. Under **Timer behaviour**:
   - Tap on the text label "Commitment mode" (not the switch). Verify the switch flips and state updates.
   - Tap on the description "Prevents pausing or cancelling the timer while running". Verify the switch flips back.
   - Tap directly on the switch thumb/track. Verify it toggles cleanly without jumping or double triggering.
4. Under **Sunset warning mode**:
   - Tap on the title "Sunset warning mode". Verify the overlay dialog appears (if overlay permission is not granted) and the switch updates.
   - When active, locate the "Sunset sound alert" row. Tap on the bell icon or title. Verify the audio alert switch toggles state.
5. With an active timer:
   - Start a timer from the main screen and return to Settings.
   - Tap on the locked toggle rows. Verify they do not toggle and exhibit disabled behavior.

## Automated Verification

```bash
# Run unit test suite
./gradlew test

# Run Android lint
./gradlew lint
```
