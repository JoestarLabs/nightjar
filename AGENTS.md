# Nightjar Agent Guidelines

## Spec-Driven Development (Spec Kit)
- This project uses GitHub Spec Kit for specification-driven development.
- The project constitution at `.specify/memory/constitution.md` is non-negotiable. All proposed designs and implementations must comply with its principles.
- For new features, refactoring, or architectural changes, follow the Spec Kit workflow:
  1. Specify (`speckit-specify`) -> Clarify (`speckit-clarify`)
  2. Plan (`speckit-plan`) -> Tasks (`speckit-tasks`) -> Analyze (`speckit-analyze`)
  3. Implement (`speckit-implement`) -> Converge (`speckit-converge`)
- Avoid ad-hoc implementations without corresponding specs or tasks unless explicitly directed by the user.

## Testing & Quality Gates
- Verify changes using `./gradlew lint` and `./gradlew test`.
- Ensure new or modified business logic is covered by unit or instrumented tests according to the constitution.

## Git Standards
- Follow Conventional Commits format (`feat:`, `fix:`, `docs:`, `chore:`, etc.) for automated releases via Release Please.
