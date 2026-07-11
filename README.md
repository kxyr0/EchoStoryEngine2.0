# Echo Story Engine

Echo Story Engine is a story-only runtime scaffold for Minecraft Forge 1.20.1 on Java 17.

The initial repository intentionally contains architecture documentation, ADRs, automated guardrails,
and a minimal compiling vertical slice. It does not implement a full story DSL, gameplay systems, NPCs,
combat, navigation, GUI, networking, or production persistence.

Canonical project identity:

- mod id: `echo_story_engine`
- display name: `Echo Story Engine`
- Maven group and base package: `io.github.kxyr0.echostoryengine`
- artifact name: `echo-story-engine`
- root project name: `echo-story-engine`

## Modules

```text
story-contracts <- story-core <- story-forge-1.20.1
```

- `story-contracts`: Java-only public API/SPI contracts.
- `story-core`: Java-only runtime, application use case, ports, and diagnostics.
- `story-forge-1.20.1`: Forge entrypoint, composition root, and platform adapters.

## Verification

Use the repository wrapper:

```powershell
.\gradlew.bat test
.\gradlew.bat architectureTest
.\gradlew.bat check
.\gradlew.bat build
git diff --check
```

## Documentation

Start with:

- `AGENTS.md`
- `docs/architecture/PROJECT_MAP.md`
- `docs/architecture/SCOPE.md`
- `docs/development/DEFINITION_OF_DONE.md`
- `docs/decisions/README.md`
