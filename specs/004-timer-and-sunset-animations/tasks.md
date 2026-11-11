# Tasks: Timer Animations & Sunset Effects Suite

## Phase 1: Start Button Liquid Ripple / Warp Effect
- [x] 1.1 Create `LiquidWarpModifier` with AGSL RuntimeShader and attribution header to `sameerasw/essentials`.
- [x] 1.2 Connect start timer trigger in `LockButton` and `HomeScreen` to animate the warp ripple outward from the button.
- [x] 1.3 Verify visual execution, memory, and frame rate.
- [x] 1.4 Commit Phase 1 with Conventional Commit message and `Release-As: 0.0.6` footer.
- [x] 1.5 Pause for user testing and feedback on performance and app size impact.

## Phase 2: Sunset Warning Animations Suite
- [x] 2.1 Add `SunsetAnimation` enum (`OFF`, `FLUID_LIQUID`, `CLASSIC_WAVE`, `MATERIAL_SHAPES`, `SNOWFLAKES`) in `TimerPreferences.kt`.
- [x] 2.2 Expose `sunsetAnimation` StateFlow and updater in `TimerViewModel.kt`.
- [x] 2.3 Update `SettingsScreen.kt` with M3 Expressive sunset animation selector.
- [x] 2.4 Update `LockTimerService.kt` to calculate 360° gravity angle $(g_x, g_y)$.
- [x] 2.5 Implement `FluidLiquidOverlay` with 360° tilt and orientation-following countdown text.
- [x] 2.6 Implement `MaterialShapesOverlay` with falling/settling M3 Expressive shapes.
- [x] 2.7 Implement `SnowflakesOverlay` with falling and accumulating winter snowflakes.
- [x] 2.8 Update `ComposeOverlayManager` to render the selected animation or suppress if `OFF`.
- [x] 2.9 Verify unit tests (`./gradlew test`).
- [x] 2.10 Commit Phase 2 with `Release-As: 0.0.6` footer and pause for user testing.

## Phase 3: Morphe-Inspired Reverse Circular Wavy Progress Dial
- [x] 3.1 Upgrade `ZenTimerDial.kt` running arc to smooth M3 Expressive wavy progress ring counting down in reverse.
- [x] 3.2 Verify wall-clock sync and visual fidelity.
- [x] 3.3 Commit Phase 3 with `Release-As: 0.0.6` footer and pause for final user review.

## Phase 4: Native Android 12+ Blur / Glass Frost Effect Toggle
- [ ] 4.1 Add `blurEffectsEnabled` setting to `TimerPreferences.kt`, `TimerViewModel.kt`, and `SettingsScreen.kt`.
- [ ] 4.2 Apply native Android 12+ `RenderEffect.createBlurEffect` frosted glass to sunset overlays (wave, liquid, shapes, snowflakes).
- [ ] 4.3 Apply frosted glass blur to in-app dialogs, bottom sheets, and drawers.
- [ ] 4.4 Verify performance, low-power mode handling, and unit tests.
- [ ] 4.5 Commit Phase 4 with `Release-As: 0.0.6` footer and pause for user testing.
