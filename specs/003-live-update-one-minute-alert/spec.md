# Feature Specification: Live Update Chip — 1-Minute Alert Visual Signal

**Feature Branch**: `003-live-update-one-minute-alert`

**Created**: 2026-09-30

**Status**: Draft

**Input**: User description: "Live Updates Notifications: can't the live update notifications chip in the status bar be made to flash (or change color) or catch the user's attention in some native and clean way when the 1 minute mark passes?"

## Clarifications

### Session 2026-09-30

- Q: What minimum contrast ratio should the urgency colour meet so the chip remains accessible to users with colour vision deficiency? → A: WCAG-AA minimum (3:1 contrast for UI components against light & dark system backgrounds)

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Chip changes to an urgent visual state at 1-minute mark (Priority: P1)

A user has set a lock timer of more than one minute. When exactly one minute of remaining time is reached, the status-bar Live Update chip changes its visual appearance — colour, icon, or both — so the user notices the imminent lock without looking at the notification drawer or needing any popup.

**Why this priority**: The live update chip is the always-visible, always-on-screen element that represents the timer during normal use. A clear visual change at the 1-minute mark is the most direct, least disruptive way to alert the user while they are using their device.

**Independent Test**: Can be fully tested by starting a timer of two minutes, waiting until one minute remains, and observing that the chip appearance noticeably changes. Delivers the core value — a passive, ambient alert — independently of any other enhancement.

**Acceptance Scenarios**:

1. **Given** a timer is running with more than 60 seconds remaining, **When** the remaining time reaches exactly 60 seconds (or crosses below, if a tick is skipped), **Then** the chip visual style transitions to an urgency state (e.g., amber/red colour or a distinct icon) that is visibly different from the normal running state.
2. **Given** the chip has transitioned to the urgency state, **When** the timer continues counting down, **Then** the chip remains in the urgency state for the remainder of the countdown (it does not revert to the normal colour).
3. **Given** a timer is started with 60 seconds or fewer remaining, **When** the timer starts, **Then** the chip begins immediately in the urgency state (no normal-to-urgent transition occurs).
4. **Given** a timer is cancelled or expires, **When** the foreground service stops, **Then** the chip is removed by the OS and no urgency state persists.

---

### User Story 2 - Chip urgency state is consistent with existing heads-up alert (Priority: P2)

When the 1-minute heads-up notification fires, the chip visual change is coordinated with (or complementary to) that alert, so the two signals feel like one coherent event rather than two unrelated notifications.

**Why this priority**: Consistency between the chip state and the heads-up notification prevents user confusion about which signal belongs to which timer event.

**Independent Test**: Can be tested by confirming the chip colour change and the heads-up notification both appear within the same tick interval when the timer crosses the 60-second boundary.

**Acceptance Scenarios**:

1. **Given** the timer crosses the 60-second boundary, **When** the 1-minute alert fires, **Then** the chip colour update and the heads-up alert are posted in the same notification update cycle.
2. **Given** the chip is in the urgency state, **When** the user dismisses the heads-up notification, **Then** the chip stays in the urgency state — its visual change is independent of the heads-up lifetime.

---

### Edge Cases

- What happens if the timer duration is set to exactly 60 seconds? The chip must start in the urgency state immediately.
- What happens if the device is running an OS version that does not support the Live Update chip (API < 36)? No chip-specific change is applied; the existing heads-up-only path continues unchanged.
- What happens if a tick is skipped and remaining time jumps below 60 (e.g., 62→59)? The `<= ONE_MINUTE_SECONDS` guard already handles this; the chip must still transition correctly.
- What happens when the chip urgency colour conflicts with the system's Dynamic Colour theme? The colour must remain distinguishable in both light and dark system themes.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: When remaining timer seconds transition to ≤ 60 (and the total duration is > 60 seconds), the system MUST update the Live Update chip with a visually distinct urgency colour for the progress segment (on devices that support the chip).
- **FR-002**: The urgency colour change MUST be applied in the same notification update that posts the 1-minute heads-up alert (single coordinated update).
- **FR-003**: Once in the urgency state, the chip MUST remain in that state for every subsequent countdown tick until the timer expires or is cancelled.
- **FR-004**: On devices that do not support the Live Update chip (API < 36), the system MUST NOT attempt to apply the urgency colour change; the existing heads-up-only alert path MUST remain fully functional.
- **FR-005**: The urgency colour MUST be visually distinguishable from both the normal running colour and the surrounding system UI in standard light and dark themes.
- **FR-006**: When a timer is started with a duration of ≤ 60 seconds, the chip MUST begin in the urgency state immediately (no transition tick required).
- **FR-007**: Cancelling or stopping the timer MUST result in the chip being removed; no urgency state must persist after the foreground service stops.

### Key Entities

- **Live Update chip**: The status-bar chip rendered by the OS from the promoted ongoing notification (API 36+). Its colour is controlled by the `ProgressStyle` segment colour in the notification.
- **1-minute alert tick**: The single moment in the countdown at which `remaining <= 60` and `alertFired == false` and `durationSeconds > 60`. This is the trigger for both the heads-up alert and the chip urgency update.
- **Urgency state**: The visual configuration of the chip after the 1-minute mark, using a distinct colour (e.g., amber) for its progress segment.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: The chip visual state changes within one countdown tick (≤ 1 second) of the remaining time crossing the 60-second boundary on supported devices.
- **SC-002**: The chip urgency colour is applied in 100% of countdown cycles once the 1-minute boundary is crossed for the remainder of that timer run.
- **SC-003**: On devices with API < 36, no code path related to the chip urgency change is executed, and the existing heads-up alert continues to function without regression.
- **SC-004**: The urgency colour achieves a minimum 3:1 contrast ratio (WCAG-AA for UI components) against standard Material You light and dark system backgrounds, verified for common colour-vision-deficiency profiles.
- **SC-005**: A corresponding unit test verifies that the correct urgency colour branch is taken when `remainingSeconds <= 60`.

## Assumptions

- The Live Update chip (`android.requestPromotedOngoing`) is already functional for this app on API 36+ devices, as it is used in the existing `buildNotification` path.
- The urgency colour will reuse the existing `notification_alert_color` resource (currently used during the 1-minute alert tick in `buildNotification`), extended to persist for all subsequent ticks rather than just one tick.
- The chip's expand/pulse animation is entirely system-controlled and cannot be triggered programmatically; the feature targets colour change only.
- A timer started with ≤ 60 seconds is considered a short timer and is treated as urgency-state from the outset; this is an edge case and does not need a separate settings toggle.
- No new user-facing settings or toggles are required for this feature; it is always-on behaviour consistent with the existing 1-minute alert.
