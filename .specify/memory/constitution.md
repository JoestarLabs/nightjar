# Nightjar Constitution

## Core Principles

### I. Declarative UI & State Hoisting
All user interface components MUST be built using Jetpack Compose and adhere to Material 3 Expressive guidelines. UI composables MUST be stateless by default or hoist state to state holders (ViewModels). Composables MUST NOT execute side-effects directly in composition; side-effects MUST be managed via Compose effect handlers (`LaunchedEffect`, `rememberCoroutineScope`, `DisposableEffect`). Reusable UI components MUST include interactive previews where practical.

### II. Unidirectional Data Flow & MVVM
The application MUST follow unidirectional data flow (UDF). ViewModels MUST expose UI state via read-only `StateFlow` and process user intents via explicit methods. Repositories MUST encapsulate data sources (e.g., Jetpack DataStore, system services) and expose asynchronous data streams using Kotlin Coroutines `Flow`. ViewModels MUST NOT hold direct references to Android Views, Activities, or context objects (except Application context when strictly necessary and mediated via DI).

### III. Dependency Injection via Hilt
All dependencies across Activities, ViewModels, Repositories, and Services MUST be provided via Google Hilt. Manual service locator patterns, global mutable singletons, or ad-hoc constructor instantiation of managed dependencies are prohibited. Injection bindings MUST be scoped appropriately (`@Singleton`, `@ViewModelScoped`, etc.) to prevent memory leaks and ensure testability.

### IV. Test-Driven Verification
All business logic, state transitions, and data operations MUST be covered by automated tests before merging to mainline branches:
- Unit tests (`./gradlew test`) MUST test ViewModels, repositories, and utility logic in isolation using JUnit, Mockito-Kotlin, and Coroutines test dispatchers.
- Instrumented tests (`./gradlew connectedAndroidTest`) MUST validate end-to-end user workflows and Compose UI interactions against supported Android API targets.
- New features or bug fixes MUST include corresponding unit or instrumentation tests demonstrating expected behavior.

### V. Clean Service & Process Boundaries
Background timer management, notification dispatch, and device lock operations MUST be decoupled into well-defined foreground services and broadcast receivers. Background components MUST handle Android platform lifecycle events gracefully, respect battery optimization constraints, and strictly comply with Android 13+ (API 33+) notification and foreground service permission models.

## Technical Constraints & Platform Boundaries

- **Target Platform**: The codebase MUST target Android with `minSdk = 33`, `targetSdk = 36`, and `compileSdk = 37`, compiled using JDK 17.
- **R8 / ProGuard Minification**: Release builds enforce code and resource shrinking (`isMinifyEnabled = true`, `isShrinkResources = true`). Any reflection, serialization, or third-party libraries requiring keep rules MUST be documented and explicitly retained in `proguard-rules.pro`.
- **Packaging & Dependency Governance**: Library dependencies MUST be cataloged in `gradle/libs.versions.toml` (or standard version catalogs) and adhere to modern AndroidX and Kotlin best practices.

## Quality Gates & Verification Workflow

- **Static Analysis & Linting**: All builds MUST pass `./gradlew lint` without unresolved errors before PR integration.
- **Commit Standards**: Commits MUST follow Conventional Commits formatting (`feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:`) to ensure seamless automation with Release Please.
- **Continuous Integration**: The CI pipeline (GitHub Actions) MUST pass both linting and automated unit tests for every pull request and push to protected branches.

## Governance

This Constitution supersedes informal agreements and ad-hoc practices. Every architectural proposal, feature specification, and code contribution MUST comply with the principles and constraints established herein.

- **Amendments**: Modifications to this constitution MUST be proposed via pull request, documented in `.specify/memory/constitution.md`, and accompanied by a semantic version increment.
- **Versioning Policy**:
  - **MAJOR** version bumps occur when principles are removed or fundamentally redefined.
  - **MINOR** version bumps occur when new principles, sections, or materially expanded architectural guidance are added.
  - **PATCH** version bumps occur for non-semantic refinements, typos, or wording clarifications.
- **Compliance**: Code reviews and Spec Kit analysis (`speckit-analyze`, `speckit-converge`) MUST verify compliance against this active document.

**Version**: 1.0.0 | **Ratified**: 2026-09-26 | **Last Amended**: 2026-09-26
