# Tasks: Live Update Chip 1-Minute Urgency Alert

**Feature**: `003-live-update-one-minute-alert`  
**Plan**: [specs/003-live-update-one-minute-alert/plan.md](plan.md)  
**Spec**: [specs/003-live-update-one-minute-alert/spec.md](spec.md)  

---

## Phase 1: Setup (Color Resources & Accessibility)

**Purpose**: Establish accessible color tokens in light and dark themes before service integration.

- [ ] T001 [P] Define light theme urgency color `#A05A00` with $\ge 4.2:1$ contrast in `app/src/main/res/values/colors.xml`
- [ ] T002 [P] Define dark theme urgency color `#FFB03A` with $\ge 9.9:1$ contrast in `app/src/main/res/values-night/colors.xml`

---

## Phase 2: Foundational (Notification Test Scaffold)

**Purpose**: Unit test harness for notification building and segment color evaluation under Robolectric.

- [ ] T003 Create unit test scaffold `LockTimerServiceNotificationTest.kt` in `app/src/test/java/com/bl4ckswordsman/nightjar/service/LockTimerServiceNotificationTest.kt`

**Checkpoint**: Foundation ready — service logic changes can be developed and verified against unit tests.

---

## Phase 3: User Story 1 - Chip changes to an urgent visual state at 1-minute mark (Priority: P1) 🎯 MVP

**Goal**: The Live Update chip segment color transitions to amber at remaining $\le 60$s and persists throughout all subsequent countdown ticks. Short timers ($\le 60$s) begin immediately in the urgency state.

**Independent Test**: Start a 75s timer; at 60s the chip turns amber and remains amber through 59s, 58s, ... 1s. Start a 30s timer; the chip starts amber immediately.

### Tests for User Story 1
- [ ] T004 [P] [US1] Unit test verifying `buildNotification` resolves standard green segment when `remainingSeconds > 60` in `app/src/test/java/com/bl4ckswordsman/nightjar/service/LockTimerServiceNotificationTest.kt`
- [ ] T005 [P] [US1] Unit test verifying `buildNotification` resolves urgency amber segment when `remainingSeconds <= 60` (including boundary at 60s) in `app/src/test/java/com/bl4ckswordsman/nightjar/service/LockTimerServiceNotificationTest.kt`
- [ ] T006 [P] [US1] Unit test verifying initial notification for short duration ($\le 60$s) resolves urgency amber segment in `app/src/test/java/com/bl4ckswordsman/nightjar/service/LockTimerServiceNotificationTest.kt`

### Implementation for User Story 1
- [ ] T007 [US1] Update `buildNotification()` in `app/src/main/java/com/bl4ckswordsman/nightjar/service/LockTimerService.kt` to decouple `alertOnce` from segment color and set `Notification.ProgressStyle.Segment` color based on `isUrgent` (`remainingSeconds <= ONE_MINUTE_SECONDS`)
- [ ] T008 [US1] Update countdown tick notification loop in `app/src/main/java/com/bl4ckswordsman/nightjar/service/LockTimerService.kt` so every tick $\le 60$s maintains the urgency segment color

**Checkpoint**: User Story 1 is fully functional and all User Story 1 unit tests pass.

---

## Phase 4: User Story 2 - Chip urgency state is consistent with existing heads-up alert (Priority: P2)

**Goal**: Coordinate the 1-minute heads-up notification and chip urgency transition in a single notification update cycle when crossing the 60s mark, ensuring the chip stays urgent even after heads-up dismissal.

**Independent Test**: At 60s, both heads-up alert and chip amber transition dispatch simultaneously; dismiss heads-up notification and confirm chip remains amber.

### Tests for User Story 2
- [ ] T009 [P] [US2] Unit test verifying `postOneMinuteAlert` posts heads-up on `CHANNEL_ALERT_ID` and updates ongoing notification with `isUrgent = true` in `app/src/test/java/com/bl4ckswordsman/nightjar/service/LockTimerServiceNotificationTest.kt`

### Implementation for User Story 2
- [ ] T010 [US2] Align `postOneMinuteAlert()` in `app/src/main/java/com/bl4ckswordsman/nightjar/service/LockTimerService.kt` to update ongoing notification `NOTIFICATION_ID` with explicit `isUrgent = true` and `alertOnce = false`
- [ ] T011 [US2] Ensure `alertFired` logic cleanly coordinates with continuous `isUrgent` ticks without duplicate heads-up alerts in `app/src/main/java/com/bl4ckswordsman/nightjar/service/LockTimerService.kt`

**Checkpoint**: User Stories 1 & 2 are complete and verified together.

---

## Phase 5: Polish & Quality Gates

**Purpose**: Final verification, linting, and regression tests.

- [ ] T012 Run full unit test suite `./gradlew testDebugUnitTest`
- [ ] T013 Run static analysis and lint checks `./gradlew lint`
- [ ] T014 Execute quickstart validation scenarios documented in `specs/003-live-update-one-minute-alert/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies
- **Phase 1 (Setup)**: Can start immediately (color resources).
- **Phase 2 (Foundational)**: Depends on Phase 1 — sets up test class.
- **Phase 3 (User Story 1 - MVP)**: Depends on Phase 2.
- **Phase 4 (User Story 2)**: Depends on Phase 3.
- **Phase 5 (Polish)**: Depends on Phase 4 completion.

### Parallel Opportunities
- T001 and T002 can run in parallel (different XML files).
- T004, T005, T006 can run in parallel within Phase 3 test writing.

---

## Implementation Strategy

### MVP First (User Story 1)
1. Complete T001-T002 (Colors)
2. Complete T003 (Test scaffold)
3. Complete T004-T008 (US1 Tests & Implementation)
4. Validate US1 independently with unit tests.

### Incremental Delivery
1. Add US2 (T009-T011) to coordinate heads-up alert dispatch with ongoing chip update.
2. Complete Polish (T012-T014) to satisfy Constitution Quality Gates (`./gradlew test` and `./gradlew lint`).
