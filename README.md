# Echo Story Engine

Echo Story Engine is a story-only runtime scaffold for Minecraft Forge 1.20.1 on Java 17.

The current repository contains architecture documentation, ADRs, automated guardrails, a deterministic runtime
vertical slice, a minimal strict-JSON source pipeline, and a portable durable persistence adapter. It does not
implement a custom DSL, gameplay systems, NPCs, combat, navigation, GUI, networking, Forge resource loading,
or a Forge world-bound persistence host.

Canonical project identity:

- mod id: `echo_story_engine`
- display name: `Echo Story Engine`
- Maven group and base package: `io.github.kxyr0.echostoryengine`
- artifact name: `echo-story-engine`
- root project name: `echo-story-engine`

## Modules

```text
story-contracts <- story-core <- story-definition
story-contracts <- story-core <- story-persistence
story-contracts <- story-core <- story-forge-1.20.1
story-contracts <- story-forge-1.20.1
```

- `story-contracts`: Java-only public API/SPI contracts.
- `story-core`: Java-only compiled story model, deterministic runtime, application use case, ports, and diagnostics.
- `story-definition`: Java-only source DTOs, strict JSON parsing, structural validation, semantic validation,
  and compilation into the core compiled model.
- `story-persistence`: Java-only save schema V1, strict JSON checkpoint codec, durable file repository, and
  recovery planning.
- `story-forge-1.20.1`: Forge entrypoint, composition root, and platform adapters.

Implemented source format version: `1`.
Implemented save schema version: `1`.

The minimal source pipeline supports stable story ids, start node ids, start/set-variable/equality-condition/
external-action/finish nodes, explicit transitions, and string-valued external action arguments. Runtime execution
uses compiled immutable `StoryDefinition` objects, not raw JSON or source DTOs.

Durable runtime persistence uses save-before-dispatch ordering: the runtime computes the next state and abstract
commands, saves the checkpoint, and exposes commands only after save success. Recovery is at-least-once and reissues
WAITING external actions with the same `ActionExecutionId`.

## Verification

Use the repository wrapper:

```powershell
.\gradlew.bat test
.\gradlew.bat architectureTest
.\gradlew.bat check
.\gradlew.bat build
.\gradlew.bat checkWhitespace
git diff --check
```

## Documentation

Start with:

- `AGENTS.md`
- `docs/architecture/PROJECT_MAP.md`
- `docs/architecture/SCOPE.md`
- `docs/development/DEFINITION_OF_DONE.md`
- `docs/decisions/README.md`
