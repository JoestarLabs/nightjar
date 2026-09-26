# Implementation Plan: Expand Hit Targets for Setting Toggle Rows

**Branch**: `002-settings-toggle-rows` | **Date**: 2026-09-26 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/002-settings-toggle-rows/spec.md`

## Summary

Expand the tap targets of all switch-bearing setting rows in `SettingsScreen.kt` from the isolated `Switch` control to the entire `ListItem` container row. This is accomplished using `Modifier.toggleable` with `role = Role.Switch` on the parent `ListItem` while nullifying `onCheckedChange` on the child `Switch` to ensure accessible, unified semantics and smooth touch ripple feedback across the entire row surface.

## Technical Context

**Language/Version**: Kotlin 2.1.0 / JDK 17  
**Primary Dependencies**: Jetpack Compose (BOM 2025.02.00), Compose Material 3 (1.4.0-alpha10), Compose Foundation, Hilt 2.55  
**Storage**: Jetpack DataStore (existing `TimerPreferencesDataSource`)  
**Testing**: JUnit 4, Compose UI Test (`createComposeRule`), Mockito-Kotlin  
**Target Platform**: Android (minSdk = 33, targetSdk = 36, compileSdk = 37)  
**Project Type**: Android Mobile App  
**Performance Goals**: Instantaneous toggle response (<16ms frame deadline) with smooth ripple feedback  
**Constraints**: Zero regression in TalkBack screen reader accessibility; adhere strictly to Android touch target guidelines  
**Scale/Scope**: 1 UI screen (`SettingsScreen.kt`), 3 setting rows  

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] **Principle I (Declarative UI & State Hoisting)**: Uses standard declarative Compose Foundation modifier (`toggleable`) and preserves hoisted ViewModel state.
- [x] **Principle II (UDF & MVVM)**: Toggle events invoke existing ViewModel action methods; no ViewModel state references violated.
- [x] **Principle III (Dependency Injection)**: No new DI dependencies required; leverages existing injected `TimerViewModel`.
- [x] **Principle IV (Test-Driven Verification)**: Automated verification via `./gradlew test` and `./gradlew lint`.
- [x] **Principle V (Service Boundaries)**: No changes to background services or broadcast receivers.
- [x] **Technical Constraints**: Targets minSdk 33 / targetSdk 36 / compileSdk 37; passes R8 minification and lint checks.

## Project Structure

### Documentation (this feature)

```text
specs/002-settings-toggle-rows/
├── spec.md              # Feature specification
├── plan.md              # This implementation plan
├── research.md          # Technical research on Compose toggleable semantics
├── quickstart.md        # Manual and automated verification guide
├── checklists/
│   └── requirements.md  # Specification quality checklist
└── tasks.md             # Implementation tasks (generated via speckit-tasks)
```

### Source Code (repository root)

```text
app/src/main/java/com/bl4ckswordsman/nightjar/
└── ui/
    └── screen/
        └── SettingsScreen.kt   # Updates to Commitment, Sunset Warning, and Sunset Audio ListItems
```

## Complexity Tracking

*No constitutional violations. All guidelines satisfied with standard Compose patterns.*
