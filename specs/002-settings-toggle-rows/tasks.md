# Tasks: Expand Hit Targets for Setting Toggle Rows

**Input**: Design documents from `specs/002-settings-toggle-rows/` (`spec.md`, `plan.md`, `research.md`, `quickstart.md`)

## Phase 1: Setup

- [x] T001 Verify project baseline and ensure working directory is clean in `app/src/main/java/com/bl4ckswordsman/nightjar/ui/screen/SettingsScreen.kt`

---

## Phase 2: Foundational

- [x] T002 Add required Compose Foundation and UI Semantics imports (`androidx.compose.foundation.selection.toggleable` and `androidx.compose.ui.semantics.Role`) to `app/src/main/java/com/bl4ckswordsman/nightjar/ui/screen/SettingsScreen.kt`

---

## Phase 3: User Story 1 - Toggle Setting via Full Row Tap (Priority: P1) 🎯 MVP

**Goal**: Allow users to toggle setting switches by clicking anywhere within the `ListItem` row container (icon, title, description, or background), rather than only the switch control.

**Independent Test**: Navigate to Settings and tap the text/icon area of Commitment Mode, Sunset Warning Mode, and Sunset Sound Alert rows; verify each switch toggles and state persists.

### Implementation for User Story 1

- [x] T003 [US1] Apply `Modifier.toggleable(role = Role.Switch)` to Commitment Mode `ListItem` and set child `Switch` `onCheckedChange = null` in `app/src/main/java/com/bl4ckswordsman/nightjar/ui/screen/SettingsScreen.kt`
- [x] T004 [US1] Apply `Modifier.toggleable(role = Role.Switch)` with overlay permission check logic to Sunset Warning Mode `ListItem` and set child `Switch` `onCheckedChange = null` in `app/src/main/java/com/bl4ckswordsman/nightjar/ui/screen/SettingsScreen.kt`
- [x] T005 [US1] Apply `Modifier.toggleable(role = Role.Switch)` to Sunset Sound Alert `ListItem` and set child `Switch` `onCheckedChange = null` in `app/src/main/java/com/bl4ckswordsman/nightjar/ui/screen/SettingsScreen.kt`

**Checkpoint**: All three switch rows can be toggled by tapping anywhere on their respective row bounds.

---

## Phase 4: User Story 2 - Unified Accessibility & TalkBack Semantics (Priority: P2)

**Goal**: Expose each toggle setting row as a single cohesive switch control node to accessibility services without duplicate or disconnected child switch nodes.

**Independent Test**: Inspect the semantics of the `ListItem` nodes to ensure each row acts as a single `Role.Switch` node combining title, description, and checked state.

### Implementation for User Story 2

- [x] T006 [US2] Verify semantics tree hierarchy for `SettingsScreen` toggle rows ensures child `Switch` controls without `onCheckedChange` do not emit redundant accessibility nodes.

---

## Phase 5: User Story 3 - Consistent Disabled State During Active Timer (Priority: P3)

**Goal**: Ensure row-level toggleability respects `!timerIsRunning` so that running sessions lock the entire row from accidental modification.

**Independent Test**: Verify that when `timerIsRunning` is true, clicking anywhere on the locked setting rows yields no state change and no interactive ripple.

### Implementation for User Story 3

- [x] T007 [US3] Verify `enabled = !timerIsRunning` guard is enforced uniformly across row modifiers and child widgets in `app/src/main/java/com/bl4ckswordsman/nightjar/ui/screen/SettingsScreen.kt`.

---

## Phase 6: Polish & Verification

**Purpose**: Quality gate verification and regression prevention.

- [x] T008 Run unit test suite `./gradlew test` to ensure zero regressions across data store and view model layers.
- [x] T009 Run Android lint `./gradlew lint` to verify clean build with zero lint errors.
- [x] T010 Validate git diff for code hygiene and verify no PII/usernames are introduced.

---

## Dependencies & Execution Order

### Phase Dependencies
1. **Phase 1 (Setup)**: Completed.
2. **Phase 2 (Foundational)**: Completed.
3. **Phase 3 (User Story 1)**: Completed.
4. **Phase 4 & 5 (US2 & US3)**: Completed.
5. **Phase 6 (Polish)**: Completed.
6. **Phase 7 (Convergence)**: Visual press feedback refinement.

---

## Phase 7: Convergence - Visual Press & Hover Feedback

**Purpose**: Forward row interaction events to child switches to trigger Material 3 switch thumb expansion animation during press and hold.

- [x] T011 [US1] Instantiate and remember `MutableInteractionSource` for Commitment Mode, Sunset Warning Mode, and Sunset Sound Alert rows in `app/src/main/java/com/bl4ckswordsman/nightjar/ui/screen/SettingsScreen.kt`.
- [x] T012 [US1] Pass shared `interactionSource` and `indication = ripple()` to `Modifier.toggleable` and child `Switch` for each toggle setting row in `app/src/main/java/com/bl4ckswordsman/nightjar/ui/screen/SettingsScreen.kt`.
- [x] T013 Verify `./gradlew test` and `./gradlew lint` pass with zero errors.
