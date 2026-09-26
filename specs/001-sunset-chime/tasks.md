# Tasks: Audible Sunset Warning Chime

**Feature**: `001-sunset-chime` | **Spec**: [specs/001-sunset-chime/spec.md](spec.md) | **Plan**: [specs/001-sunset-chime/plan.md](plan.md)

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Verify build environment and project baseline

- [x] T001 Verify project build baseline and existing tests via `./gradlew testDebugUnitTest`

---

## Phase 2: Foundational (Data Layer & Audio Infrastructure)

**Purpose**: Core DataStore keys, audio player contract, and Hilt injection prerequisites

- [x] T002 Add `sunset_audio_enabled` key and `sunsetAudioEnabled: Boolean = true` field to `TimerPreferences` and `TimerPreferencesDataSource` in `app/src/main/java/com/bl4ckswordsman/nightjar/data/TimerPreferences.kt`
- [x] T003 [P] Create `SunsetAudioPlayer` interface and `AndroidSunsetAudioPlayer` implementation in `app/src/main/java/com/bl4ckswordsman/nightjar/service/SunsetAudioPlayer.kt`
- [x] T004 [P] Provide `SunsetAudioPlayer` binding in Hilt `AppModule` in `app/src/main/java/com/bl4ckswordsman/nightjar/di/AppModule.kt`
- [x] T005 [P] Add unit tests for `sunsetAudioEnabled` default and persistence in `app/src/test/java/com/bl4ckswordsman/nightjar/data/TimerPreferencesTest.kt` and `app/src/test/java/com/bl4ckswordsman/nightjar/data/TimerPreferencesDataSourceTest.kt`

**Checkpoint**: Foundation ready — DataStore persistence and audio player contracts are testable.

---

## Phase 3: User Story 1 - Audible Sunset Warning Alert (Priority: P1) 🎯 MVP

**Goal**: Play gentle audio chime once when active timer countdown reaches the sunset warning threshold.

**Independent Test**: Start a timer with sunset mode enabled, observe that chime plays once at the exact moment remaining seconds reach the sunset duration window.

- [x] T006 [US1] Inject `SunsetAudioPlayer` and observe `sunsetAudioEnabled` preference in `app/src/main/java/com/bl4ckswordsman/nightjar/service/LockTimerService.kt`
- [x] T007 [US1] Add single-shot trigger state (`sunsetChimePlayed`) inside `LockTimerService.kt` countdown loop to execute `sunsetAudioPlayer.playChime()` when `remaining <= sunsetDuration && sunsetEnabled && sunsetAudioEnabled && !sunsetChimePlayed`
- [x] T008 [US1] Reset `sunsetChimePlayed = false` on timer start, timer cancel, and timer finish in `app/src/main/java/com/bl4ckswordsman/nightjar/service/LockTimerService.kt`

**Checkpoint**: User Story 1 is functional — countdown audio warning sounds at the sunset threshold.

---

## Phase 4: User Story 2 - User Setting to Enable/Disable Chime (Priority: P2)

**Goal**: Allow users to toggle the sunset audio chime on/off in the Settings screen.

**Independent Test**: Navigate to Settings, toggle "Sunset sound alert" off, and verify that crossing the sunset warning threshold produces no sound.

- [x] T009 [P] [US2] Add string resources `settings_sunset_audio_title` ("Sunset sound alert") and `settings_sunset_audio_summary` ("Play a gentle chime when the sunset warning begins") in `app/src/main/res/values/strings.xml`
- [x] T010 [US2] Add `sunsetAudioEnabled` preference update handler in `app/src/main/java/com/bl4ckswordsman/nightjar/viewmodel/SettingsViewModel.kt` (or repository binding)
- [x] T011 [US2] Add Material 3 Switch preference row for sunset audio alert in the Sunset section of `app/src/main/java/com/bl4ckswordsman/nightjar/ui/screen/SettingsScreen.kt`, enabled conditionally when visual sunset mode is active

**Checkpoint**: User Story 2 is functional — user can toggle the audio alert and setting persists across launches.

---

## Phase 5: User Story 3 - System Sound Profile & Silence Respect (Priority: P3)

**Goal**: Guarantee sunset chime adheres to system silent, vibrate, and Do Not Disturb profiles.

**Independent Test**: Put device in Silent mode; verify reaching sunset warning phase produces zero audible output.

- [x] T012 [US3] Configure `AudioAttributes.USAGE_NOTIFICATION_EVENT` and `AudioAttributes.CONTENT_TYPE_SONIFICATION` in `AndroidSunsetAudioPlayer` in `app/src/main/java/com/bl4ckswordsman/nightjar/service/SunsetAudioPlayer.kt` to ensure OS enforces notification ringer mode

**Checkpoint**: User Story 3 verified — system silent rules respected.

---

## Phase 6: Polish & Quality Gates

**Purpose**: End-to-end verification and compliance review

- [x] T013 Run unit test suite `./gradlew test` and verify all tests pass
- [x] T014 Run static analysis `./gradlew lint` and verify 0 lint errors
- [x] T015 Verify end-to-end manual flows against [quickstart.md](quickstart.md)

---

## Dependencies & Execution Order

### Phase Dependencies
- **Setup (Phase 1)**: Can start immediately
- **Foundational (Phase 2)**: Depends on Phase 1; blocks all User Stories
- **User Story 1 (Phase 3)**: Depends on Phase 2; provides MVP
- **User Story 2 (Phase 4)**: Depends on Phase 2; can proceed in parallel with or after US1
- **User Story 3 (Phase 5)**: Validates US1 audio attributes
- **Polish (Phase 6)**: Depends on all user stories being complete

### Parallel Opportunities
- T003 (`SunsetAudioPlayer.kt`) and T004 (`AppModule.kt`) can be developed in parallel with T002 (`TimerPreferences.kt`).
- T009 (string resources) can be added in parallel with T006-T008.

---

## Implementation Strategy (MVP First)
1. Complete Foundational DataStore & Audio contract (T002 - T005).
2. Wire up service trigger (T006 - T008) to deliver working audio alert (MVP).
3. Add user configuration toggle in Settings (T009 - T011).
4. Run verification gates (`lint`, `test`) to ensure 100% constitution compliance.
