# Feature Specification: Expand Hit Targets for Setting Toggle Rows

**Feature Branch**: `002-settings-toggle-rows`

**Created**: 2026-09-26

**Status**: Draft

**Input**: User description: "currently, setting entries with a toggle, can only be toggled by only clicking the toggle, not by clicking the actual entry area (box with title, desc, toggle). Fix this so that clicking anywhere in the entry row toggles the setting."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Toggle Setting via Full Row Tap (Priority: P1)

As a user customizing application behavior in the Settings screen, I want to tap anywhere within a setting's row (including its icon, title, description, and background) so that the associated setting is toggled easily without requiring high-precision taps on the small toggle control.

**Why this priority**: Core usability and ergonomic improvement. Small touch targets frustrate users and violate mobile UI ergonomics, whereas full-row tap targets are the expected modern standard.

**Independent Test**: Navigate to Settings and tap on the title or description text of each toggle setting row (Commitment Mode, Sunset Warning Mode, Sunset Sound Alert). The switch toggles state and the preference is saved.

**Acceptance Scenarios**:

1. **Given** the user is on the Settings screen and a toggle setting is off, **When** the user taps the title or supporting description of that setting row, **Then** the switch turns on, the setting state updates, and a visual touch ripple covers the entire row.
2. **Given** a toggle setting is on, **When** the user taps the row area outside the switch, **Then** the switch turns off and the updated state is persisted.
3. **Given** a toggle setting row, **When** the user taps directly on the switch thumb/track, **Then** the switch toggles state exactly once without duplicate trigger events.
4. **Given** a toggle setting row, **When** the user presses and holds down anywhere on the row or switch, **Then** the switch thumb visibly expands to provide tactile press feedback, and returns to normal rest width upon release.

---

### User Story 2 - Unified Accessibility & TalkBack Semantics (Priority: P2)

As a user relying on Android accessibility tools (e.g., TalkBack, Switch Access), I want each toggle setting row to be presented as a single cohesive switch control, so that the control type, label, and checked status are announced together in one focus node.

**Why this priority**: Prevents redundant navigation stops and fragmented announcements where a user must navigate past separate text labels to find an isolated switch.

**Independent Test**: Inspect the Settings screen semantics tree or enable TalkBack; verify that each toggle row is focused as a single node with switch role and state announcement ("on"/"off"), rather than separate non-interactive text and disconnected switch nodes.

**Acceptance Scenarios**:

1. **Given** TalkBack or an accessibility service is active, **When** focus lands on a toggle setting row, **Then** the screen reader announces the setting title, description, role ("switch"), and current state ("checked" / "not checked") as a single unified node.
2. **Given** an accessibility service is active, **When** the user triggers the primary action on the focused row, **Then** the state toggles appropriately.

---

### User Story 3 - Consistent Disabled State During Active Timer (Priority: P3)

As a user viewing the Settings screen while a timer session is running, I want settings locked against modification to disable the entire row tap interaction consistently.

**Why this priority**: Consistency and safety. If settings changes are prevented during an active timer, the entire row must reject tap interactions to avoid confusing the user.

**Independent Test**: Start a timer, navigate to Settings, and tap on locked toggle rows; verify neither row tap nor direct switch tap changes the setting state.

**Acceptance Scenarios**:

1. **Given** an active timer is running and settings changes are locked, **When** the user taps anywhere on a locked toggle row, **Then** no state change occurs and no interaction ripple is triggered.

---

### Edge Cases

- **Special Action Triggers (Overlay Permission)**: When enabling Sunset Warning Mode, the app prompts for overlay permission if not already granted. Tapping anywhere on the Sunset Warning Mode row must trigger the permission prompt identically to tapping the switch directly.
- **Child Widget Event Handling**: Direct taps on the switch must not trigger duplicate event handling or race conditions with the row-level click handler.
- **Conditional Visibility**: The Sunset Sound Alert toggle is conditionally displayed only when Sunset Warning Mode is active. Its row-level tap handling must behave correctly upon appearing or disappearing.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST treat the entire container bounds of each setting entry that contains a toggle switch as a unified touch target.
- **FR-002**: Tapping anywhere within the row bounds of a toggle setting MUST flip the checked state of the setting.
- **FR-003**: The entire row MUST expose switch semantics (`Role.Switch`) to accessibility frameworks, combining label, supporting text, and toggle state into a single semantic node.
- **FR-004**: Tapping directly on the switch control MUST toggle the setting cleanly without causing secondary or conflicting events.
- **FR-005**: When timer state dictates that settings are locked, the entire row's toggle interaction MUST be disabled.
- **FR-006**: Setting-specific side effects (such as overlay permission checks when enabling Sunset Warning Mode) MUST execute regardless of whether the user tapped the text, icon, row background, or switch.
- **FR-007**: The system MUST forward row touch press and hover interactions to the child switch so that the switch thumb executes its native Material 3 expansion and tactile visual feedback while pressed.

### Key Entities

- **Toggle Setting Entry**: A user-configurable preference rendered in the Settings list consisting of an icon, title, description, and switch state (Commitment Mode, Sunset Warning Mode, Sunset Sound Alert).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of setting rows containing switches (Commitment Mode, Sunset Warning Mode, Sunset Sound Alert) toggle their state upon tapping any part of the row.
- **SC-002**: Accessibility inspection reveals zero duplicate or unlabelled interactive child nodes inside each toggle row.
- **SC-003**: Row interaction ripple feedback displays across the full width and height of the row on press.
- **SC-004**: 100% pass rate on existing automated tests and lint checks with zero regressions in timer preferences persistence.
- **SC-005**: Pressing and holding anywhere on a toggle row visibly triggers the switch thumb enlargement animation prior to release/toggle.

## Assumptions

- The three target toggle settings in the Nightjar Settings screen are Commitment Mode, Sunset Warning Mode, and Sunset Sound Alert.
- Non-toggle settings entries (such as Custom Presets and Sunset Warning Duration) retain their existing dialog and dropdown tap behaviors.
