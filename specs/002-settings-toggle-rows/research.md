# Research: Setting Toggle Rows Tap Target Expansion

## 1. Jetpack Compose Full-Row Toggle Ergonomics

### Problem
Currently in `SettingsScreen.kt`, settings rows with switches use `ListItem` where only the child `Switch` composable defines an `onCheckedChange` lambda. The parent `ListItem` row container has no click/toggle modifier.
As a result:
- Users must hit the small ~52dp x 32dp switch widget area.
- Tapping on the row's title, description, or icon produces no action and no visual feedback.
- Accessibility tools (TalkBack) treat the row text and the switch as separate nodes or require users to locate the switch control specifically.

### Solution: `Modifier.toggleable` with `Role.Switch`
Jetpack Compose foundation provides `Modifier.toggleable`:
```kotlin
Modifier.toggleable(
    value = <Boolean>,
    enabled = <Boolean>,
    role = Role.Switch,
    onValueChange = { <lambda> }
)
```

Applying this to the `ListItem` modifier achieves:
1. **Full-Row Touch Target**: The tap target expands to the entire width and height of the `ListItem` container (well exceeding the recommended minimum 48x48dp touch target).
2. **Unified Semantics**: With `role = Role.Switch`, accessibility services treat the entire row as a single switch component, announcing the title, description, and state (e.g., "Commitment mode, off, switch, double-tap to toggle").
3. **Ripple Propagation**: Touch ripple animations naturally originate from the tap location and span the entire row surface.

### Child `Switch` Configuration
When `Modifier.toggleable` is placed on the parent `ListItem`, the child `Switch` must specify `onCheckedChange = null`:
- `Switch(checked = value, onCheckedChange = null, interactionSource = sharedInteractionSource, enabled = enabled)`
- Setting `onCheckedChange = null` makes the `Switch` purely presentational within its semantic parent, preventing:
  - Duplicate click listeners.
  - Conflicting touch event consumption.
  - Nested TalkBack focus stops.

### Shared `MutableInteractionSource` & Thumb Press Expansion
In Material 3, the `Switch` thumb dot expands from 16dp/24dp to 28dp when pressed or hovered (`isPressed by interactionSource.collectIsPressedAsState()`).
When `onCheckedChange = null`, if no interaction source is passed to `Switch`, it defaults to an isolated internal interaction source that never receives press events because the parent `ListItem` consumes the touch gesture.
By passing the same hoisted `remember { MutableInteractionSource() }` to both:
- `Modifier.toggleable(interactionSource = sharedSource, indication = ripple(), ...)` on the `ListItem`
- `Switch(interactionSource = sharedSource, ...)`

The press gesture anywhere on the row immediately activates the switch's built-in thumb enlargement animation and tactile visual response in sync with the row ripple.

## 2. Setting Targets in Nightjar

Reviewing `app/src/main/java/com/bl4ckswordsman/nightjar/ui/screen/SettingsScreen.kt`, exactly three rows feature switches:
1. **Commitment Mode**:
   - State: `commitmentMode` (`Boolean`)
   - Action: `timerViewModel.setCommitmentMode(it)`
   - Guard: `enabled = !timerIsRunning`
2. **Sunset Warning Mode**:
   - State: `sunsetModeEnabled` (`Boolean`)
   - Action: Check `if (isChecked && !Settings.canDrawOverlays(context)) showOverlayDialog = true` then `timerViewModel.setSunsetModeEnabled(isChecked)`
   - Guard: `enabled = !timerIsRunning`
3. **Sunset Sound Alert**:
   - State: `sunsetAudioEnabled` (`Boolean`)
   - Action: `timerViewModel.setSunsetAudioEnabled(isChecked)`
   - Guard: `enabled = !timerIsRunning`

Other rows in the settings container:
- *Custom presets*: Opens presets dialog via `.clickable { showPresetsDialog = true }` (already full-width clickable).
- *Sunset warning duration*: Expands dropdown menu via `.clickable(enabled = !timerIsRunning) { isDurationMenuExpanded = true }` (already full-width clickable).

Thus, converting the three switch rows brings uniform tap-target ergonomics to the entire Settings screen.

## 3. Constitution & Quality Gates Alignment
- **Constitution Principle I (Declarative UI & Material 3)**: Adheres strictly to Compose Foundation and Material 3 design guidelines for selection controls.
- **Constitution Principle IV (Test-Driven Verification)**: Passes unit test suite (`./gradlew test`) and Android lint (`./gradlew lint`) with zero errors.
