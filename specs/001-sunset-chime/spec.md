# Feature Specification: Audible Sunset Warning Chime

**Feature Branch**: `001-sunset-chime`

**Created**: 2026-09-26

**Status**: Draft

**Input**: User description: "Audible chime on Sunset Warning: play a gentle sound alert when the rising liquid wave triggers to notify that screen time is almost up."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Audible Sunset Warning Alert (Priority: P1)

As a parent or child using the device timer, I want to hear a gentle sound alert when the timer enters the sunset warning phase, so that I am notified that screen time is almost up even if I am not looking directly at the screen.

**Why this priority**: Core value of the feature. Provides non-visual awareness of the approaching timer expiration without jarring or frightening young children.

**Independent Test**: Can be tested independently by starting a timer with sunset warning enabled, waiting until the countdown enters the warning threshold, and verifying that the gentle chime plays once at the start of the warning phase.

**Acceptance Scenarios**:

1. **Given** an active timer with sunset warning and audio chime enabled, **When** the remaining time reaches the configured sunset warning threshold, **Then** a gentle audio chime plays once to signal the warning phase.
2. **Given** an active timer with sunset warning enabled, **When** the countdown is still above the warning threshold, **Then** no chime is played.
3. **Given** an active timer where the user cancels or resets before reaching the warning threshold, **When** the timer is stopped, **Then** no chime is played.

---

### User Story 2 - User Setting to Enable/Disable Chime (Priority: P2)

As a user or parent, I want to configure whether the audible chime plays during the sunset warning, so that I can keep alerts silent in quiet environments or classrooms.

**Why this priority**: Essential personalization and environment respect. Users must have control over whether audio alerts play.

**Independent Test**: Can be tested independently by navigating to settings, toggling the sunset chime option off, and verifying that reaching the warning threshold does not trigger any sound.

**Acceptance Scenarios**:

1. **Given** the sunset chime setting is toggled OFF in settings, **When** the countdown enters the sunset warning phase, **Then** the visual warning appears but no audio chime is played.
2. **Given** the user changes the sunset chime setting in settings, **When** the app is closed and reopened, **Then** the preference selection remains persisted.

---

### User Story 3 - System Sound Profile and Mute Respect (Priority: P3)

As a user whose device is in Silent mode, Vibrate mode, or Do Not Disturb, I want the sunset chime to respect system silence rules so that the app does not disturb quiet situations.

**Why this priority**: Politeness and platform harmony. Background audio alerts should never override intentional device silencing.

**Independent Test**: Can be tested independently by putting the device in Silent/Do Not Disturb mode and verifying that entering the warning phase produces no audible sound.

**Acceptance Scenarios**:

1. **Given** the device is set to Silent or Do Not Disturb, **When** the timer reaches the sunset warning threshold, **Then** the chime is suppressed or muted in accordance with system audio profile rules.

---

### Edge Cases

- **Immediate Sunset Warning**: When a user sets a very short timer (e.g. 15 seconds) that is less than or equal to the configured sunset warning duration, the chime should play promptly upon timer start without stutter or repeated triggering.
- **Sunset Mode Globally Disabled**: If the user has disabled the visual sunset warning altogether, the chime must not play regardless of the chime toggle state.
- **Multiple Triggers Prevention**: If the screen orientation rotates or the app moves between foreground and background while the sunset warning is active, the chime must not re-trigger repeatedly; it must only play once per warning transition.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST play a gentle audio alert once when the active timer countdown crosses into the sunset warning threshold.
- **FR-002**: System MUST provide a user preference toggle in the settings view to enable or disable the sunset audio chime.
- **FR-003**: System MUST persist the sunset audio chime preference across application restarts.
- **FR-004**: System MUST default the sunset audio chime to enabled for new installations.
- **FR-005**: System MUST NOT play the chime if the sunset warning feature is disabled.
- **FR-006**: System MUST respect device sound profile settings (e.g., Silent / Do Not Disturb) and suppress audible playback when silenced.
- **FR-007**: System MUST NOT play the chime more than once per countdown cycle.

### Key Entities

- **Sunset Audio Preference**: User configuration entity representing whether audio alerts should accompany the visual warning phase (attributes: enabled state, default true).
- **Sunset Transition Event**: Time-based state event representing the moment the timer enters the warning duration window (attributes: triggered timestamp, played flag).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of timer runs entering the sunset phase play the chime within 500 milliseconds of the warning threshold when enabled.
- **SC-002**: 0% false triggers or repeated chime playback occur while the countdown remains within the warning phase.
- **SC-003**: Users can toggle the audio chime on or off in under 3 taps from the main screen.
- **SC-004**: Zero sound is emitted when the user toggle is OFF or when the device audio stream is muted.

## Assumptions

- The existing visual rising wave sunset warning remains unchanged and serves as the visual counterpart to the audio alert.
- The default chime sound is a short, calm notification tone suitable for young children (not an abrupt buzzer or alarm).
- The feature does not require custom external audio file selection in this first iteration; a built-in melodic chime is sufficient.
