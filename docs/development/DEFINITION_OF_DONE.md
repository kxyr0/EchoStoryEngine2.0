# Definition Of Done

A change is complete only when all applicable items are true:

- Execution plan exists when required.
- Documentation and ADRs match the actual code.
- `AGENTS.md` remains compact and points to detailed docs.
- `docs/architecture/PROJECT_MAP.md` reflects module/package/API/port/adapter structure.
- `PROGRESS.md` reflects real status and only claims checks that ran.
- Gradle modules have clear boundaries.
- `story-contracts` and `story-core` contain no Forge/Minecraft imports.
- Architecture tests exist and pass.
- Static checks exist and pass.
- Unit tests pass.
- Root build passes.
- Forge module builds.
- No application-owned subsystem log file is created or committed.
- No concrete game logic appears in contracts/core.
- No empty speculative abstractions are added.
- No unexplained suppressions are added.
- No class, method, package-size, or cycle rules are violated.
- Architecture decisions are documented.
- `git diff --check` passes.
- Unrelated user files are not changed.

Dedicated server/GameTest may be reported only if actually run.

Do not create a commit unless the user explicitly requests it.
