# Architecture & Implementation Plan: Timer Animations & Sunset Effects Suite

**Feature Identifier**: `004-timer-and-sunset-animations`

## Technical Approach

### 1. Liquid Ripple / Warp Effect (Phase 1)
- **Shader Component**: AGSL `RuntimeShader` (`RenderEffect.createRuntimeShaderEffect`).
- **Attribution**: "Inspired by Sameera Wijerathna's essentials (sameerasw/essentials) wobble/warp animation."
- **Integration**: Compose modifier `Modifier.liquidWarp(...)` attached to the main screen layout or dial wrapper, driven by an `Animatable(0f)` on start button trigger.
- **Verification**: Measure rendering performance via Compose layout & frame metrics, and verify zero battery impact when idle.

### 2. Sunset Warning Animations Suite (Phase 2)
- **Data Model**: `SunsetAnimation` enum in `TimerPreferences.kt`, persisted via Jetpack DataStore.
- **Sensors**: Extended 2D gravity vector $(g_x, g_y)$ in `LockTimerService` to compute continuous $360^\circ$ angle $\theta = \operatorname{atan2}(-g_x, g_y)$.
- **Overlay Dispatcher**: `ComposeOverlayManager` renders the appropriate overlay composable (`FluidLiquidOverlay`, `ClassicWaveOverlay`, `MaterialShapesOverlay`, `SnowflakesOverlay`), or bypasses display if `OFF`.
- **Text Counter-Rotation**: `Modifier.graphicsLayer { rotationZ = -orientationAngle }` applied to the center countdown display.

### 3. Morphe-Inspired Reverse Circular Wavy Progress (Phase 3)
- **Component**: Integration of M3 Expressive wavy stroke mathematics in `ZenTimerDial.kt`.
- **Reverse Countdown**: Smooth progression from full arc ($1.0$) down to zero ($0.0$) while maintaining phase ripple animation.

### 4. Native Android 12+ Blur / Glass Frost Effect Toggle (Phase 4)
- **Settings Toggle**: Preference to enable/disable native Android 12+ (`minSdk = 33` hardware `RenderEffect.createBlurEffect`) frosted glass blur.
- **Glass Frost Applications**:
  - Sunset warning overlay background/scrim blur (blurring the apps/homescreen underneath the wave, liquid, and shapes).
  - Dialogs, bottom sheets, and navigation drawers across the app.
- **Performance & Hardware Fallback**: Hardware accelerated via `RenderEffect`, with clean fallback to translucent scrim when disabled or in battery saver mode.

## Phased Delivery & Testing
- **Phase 1**: Ripple/warp button effect $\to$ test $\to$ commit (`Release-As: 0.0.6`) $\to$ pause for user testing.
- **Phase 2**: Sunset warning animations suite $\to$ test $\to$ commit (`Release-As: 0.0.6`) $\to$ pause for user testing.
- **Phase 3**: Reverse circular wavy progress indicator $\to$ test $\to$ commit (`Release-As: 0.0.6`) $\to$ pause for user testing.
- **Phase 4**: Native Android 12+ blur/glass frost effect toggle and surfaces $\to$ test $\to$ commit (`Release-As: 0.0.6`) $\to$ pause for user testing.
