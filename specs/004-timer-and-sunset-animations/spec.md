# Feature Specification: Timer Animations & Sunset Effects Suite

**Feature Identifier**: `004-timer-and-sunset-animations`

**Created**: 2026-11-11

**Status**: In Progress

**Input**: User request for 3 animation enhancements:
1. Liquid ripple/warp effect on clicking the start timer button (attributed to `sameerasw/essentials`).
2. Sunset warning animations suite (Off, Fluid Liquid 360° tilt with orientation-aware countdown text, Classic Wave, Material 3 Expressive shapes, Snowflakes).
3. Reverse circular wavy progress indicator for `ZenTimerDial` (inspired by `MorpheApp/morphe-manager`).

---

## User Scenarios & Testing

### User Story 1 - Liquid Ripple / Warp Start Button Effect (Priority: P1)
When the user taps the Start Timer button, an AGSL-powered liquid ripple / warp wave radiates smoothly outward from the button across the UI, providing tactile, fluid visual feedback that the lock timer has engaged.
- **Attribution**: Attributed to Sameera Wijerathna (`sameerasw/essentials`).
- **Acceptance Criteria**:
  1. Tapping the start timer button triggers a smooth radial warp distortion that propagates outwards.
  2. The shader uses Android 13+ native `RuntimeShader` (`RenderEffect`) for GPU hardware acceleration with 0 frame drops and no external library bloat.
  3. When inactive, `renderEffect = null` ensures zero GPU/battery overhead.

### User Story 2 - Sunset Warning Animations Suite (Priority: P1)
During the final countdown warning window (e.g., 30s before device lock), users can choose between multiple visual experiences or disable it entirely:
- **Options**: Off, Fluid Liquid (default), Classic Wave, Material 3 Expressive shapes, and Snowflakes.
- **Fluid Liquid 360° Tilt & Orientation**: Liquid surface aligns with the physical gravity vector across full 360° (including upside down). The countdown numbers counter-rotate or follow screen orientation so they remain easily readable even when upside down.
- **Material 3 Shapes**: Screen fills playfully with M3 expressive shapes (arch, clover, star, scallop) popping and settling.
- **Snowflakes**: Gentle seasonal snow particles falling and accumulating.

### User Story 3 - Reverse Circular Wavy Progress Dial (Priority: P2)
While the timer runs, the dial renders a smooth Material 3 Expressive wavy progress ring that undulates continuously at vsync rate and recedes in reverse (from 100% to 0%), replacing the rough 2-degree line segment approximation.

---

## Requirements

### Functional Requirements
- **FR-001**: Start button click MUST trigger a radial liquid warp effect using AGSL `RuntimeShader`.
- **FR-002**: Source code and credits MUST attribute the ripple/warp concept to `sameerasw/essentials`.
- **FR-003**: `TimerPreferences` MUST persist `sunsetAnimation` choice (OFF, FLUID_LIQUID, CLASSIC_WAVE, MATERIAL_SHAPES, SNOWFLAKES).
- **FR-004**: Sunset overlay MUST respect the chosen animation mode; when OFF, no overlay is shown.
- **FR-005**: In FLUID_LIQUID mode, tilt angle MUST calculate from $(g_x, g_y)$ covering 360°, and countdown text MUST adapt to screen orientation.
- **FR-006**: In `ZenTimerDial`, running arc MUST render a smooth M3 Expressive wavy stroke counting down in reverse.

### Non-Functional Requirements
- **NFR-001**: MinSdk remains 33. All shaders MUST execute natively via AGSL without third-party rendering engines.
- **NFR-002**: Negligible APK size footprint (< 20 KB total increase).
- **NFR-003**: 60/120 FPS rendering during animations.
