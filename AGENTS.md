# Codex Operating Rules

## Mission

Echo Story Engine is only a story runtime. It defines, validates, executes, persists through ports,
and diagnoses story control flow. It does not implement concrete Minecraft gameplay systems.

## Required Reading

Before any non-trivial change, read:

1. `AGENTS.md`
2. `docs/architecture/PROJECT_MAP.md`
3. `PROGRESS.md`
4. The architecture or development document for the area being changed
5. The active execution plan in `docs/plans`, when the change needs one

## Scope Firewall

Authoritative rule: `docs/architecture/SCOPE.md`.

Before adding any feature, answer:

1. Is it needed to define, validate, execute, persist, or diagnose a story?
2. Can it exist as a standalone gameplay system without Story Engine?
3. Does it mention a concrete Minecraft mechanic, entity, third-party mod, or gameplay domain?

If answer 2 or 3 is yes, do not implement it in story core. Propose an adapter/integration boundary instead.

## Dependency Direction

Allowed Gradle direction:

```text
story-contracts <- story-core <- story-forge-1.20.1
```

`story-contracts` and `story-core` must not import Minecraft, Forge, Mojang logging, SLF4J,
serialization frameworks, networking, filesystem adapters, GUI, or third-party mod APIs.

## Main Prohibitions

- No NPC, combat, navigation, vehicle, magic, cinematic, worldgen, sound, concrete UI, or third-party mod logic in core.
- No packages named for forbidden gameplay domains or generic buckets: `npc`, `combat`, `utils`, `helpers`,
  `misc`, `common`, `global`, and similar.
- No default names such as `Manager`, `Helper`, `Utils`, `Common`, `GlobalService`, or `Impl`.
- No Kotlin, Lombok, DI framework, mixins, reflection registration, or classpath scanning in the initial scaffold.
- No broad suppressions, disabled checks, failing-test deletion, or exception-list changes without an approved ADR token.
- No commit, branch, tag, or push unless the user explicitly asks.

## Workflow

1. Check `git status`.
2. Read the required docs.
3. Create or update an execution plan for cross-module/public/dependency/persistence/networking changes.
4. Name the expected files to change.
5. Apply the scope firewall.
6. Make the smallest coherent change.
7. Run the relevant wrapper checks.
8. Update `docs/architecture/PROJECT_MAP.md` and `PROGRESS.md` if structure, behavior, or verification changed.
9. Review the diff as a reviewer before reporting.

## Commands

Use the repository wrapper on Windows:

```powershell
.\gradlew.bat test
.\gradlew.bat architectureTest
.\gradlew.bat check
.\gradlew.bat build
git diff --check
```

Run dedicated server or GameTest only when the local environment is prepared for it.

## Definition Of Done

Authoritative checklist: `docs/development/DEFINITION_OF_DONE.md`.

A task is not done until code, docs, architecture rules, unit tests, static checks, build, and diff review match
the actual requested scope. Do not report unchecked claims as verified.

## Architecture Exceptions

Authoritative rule: `docs/architecture/ARCHITECTURE_GUARDRAILS.md`.

An exception requires an ADR with scope, reason, alternatives, consequences, removal plan, and review date,
plus a separate user message containing exactly:

```text
ARCHITECTURE_EXCEPTION_APPROVED: <ADR-ID>
```

Do not generate this token yourself.

## Project Map And Progress

Update `docs/architecture/PROJECT_MAP.md` when modules, packages, public API, ports, adapters, dependency direction,
or composition root change. Update `PROGRESS.md` only with checks that actually ran.
