# Echo Story Engine

Изменения: [реестр](docs/changelogs/README.md).

Echo Story Engine is a story-only runtime scaffold for Minecraft Forge 1.20.1 on Java 17.

The current repository contains architecture documentation, ADRs, automated guardrails, a deterministic runtime
vertical slice, a StoryResource `.es` source compiler backed by the external EchoScript language artifact, a portable
durable persistence adapter, and a Forge 1.20.1 server host for resource loading, diagnostics, world-bound checkpoint
persistence, external action extension dispatch, GameTest, and production jar packaging. It does not implement
gameplay systems, NPCs, combat, navigation, GUI, networking, or concrete external gameplay handlers.

Canonical project identity:

- mod id: `echo_story_engine`
- display name: `Echo Story Engine`
- Maven group and base package: `io.github.kxyr0.echostoryengine`
- artifact name: `echo-story-engine`
- root project name: `echo-story-engine`

## Modules

```text
story-contracts <- story-core <- story-definition
story-contracts <- story-core <- story-definition <- story-echoscript-adapter
story-contracts <- story-core <- story-persistence
story-contracts <- story-extension-api <- story-extension-forge-1.20.1
story-contracts <- story-core <- story-definition <- story-echoscript-adapter <- story-forge-1.20.1
story-contracts <- story-core <- story-persistence <- story-forge-1.20.1
story-extension-api <- story-forge-1.20.1
story-extension-forge-1.20.1 <- story-forge-1.20.1
story-contracts <- story-core <- story-forge-1.20.1
story-contracts <- story-forge-1.20.1
```

- `story-contracts`: Java-only public API/SPI contracts.
- `story-core`: Java-only compiled story model, deterministic runtime, application use case, ports, and diagnostics.
- `story-definition`: Java-only source DTOs, structural validation, semantic validation, compiler boundary,
  and compilation into the core compiled model.
- `story-echoscript-adapter`: Java-only EchoScript `.es` StoryResource adapter that binds the supported source
  profile into neutral source DTOs.
- `story-extension-api`: Java-only loader-neutral public contract for external Story action providers.
- `story-extension-forge-1.20.1`: Forge registration event bridge for independent adapter mods.
- `story-persistence`: Java-only save schema V1, strict JSON checkpoint codec, durable file repository, and
  recovery planning.
- `story-forge-1.20.1`: Forge entrypoint, composition root, resource reload, server host, diagnostic commands,
  world-bound persistence scheduling, GameTest, and production packaging.

Implemented source format version: `1`.
Implemented save schema version: `1`.

The canonical ES2 StoryResource profile infers identity from the data namespace and filename, accepts an optional
stable `Id(...)` override, and lowers one ordered `OnStart()` intent section to strict start/set-variable/
equality-condition/external-action/finish nodes. Authors no longer write `Build()`, node IDs, `next` links, or a
terminal node. The legacy explicit graph profile remains readable.
Runtime execution uses compiled immutable `StoryDefinition` objects, not EchoScript syntax trees or source DTOs.

Local development resolves EchoScript from Maven local as
`io.github.kxyr0.echoscript:echo-script-language:0.1.0-SNAPSHOT`. CI and release builds need a reproducible Maven
repository with the same coordinates.

Durable runtime persistence uses save-before-dispatch ordering: the runtime computes the next state and abstract
commands, saves the checkpoint, and exposes commands only after save success. Recovery is at-least-once and reissues
WAITING external actions with the same `ActionExecutionId`.

External action providers are registered by separate Forge mods through `EchoStoryExtensionRegistrationEvent`.
The public extension API version is `1.0`; duplicate extension ids or duplicate action owners are rejected before
handler dispatch. The separate test adapter build lives under `integration-tests/echo-story-extension-test-mod`.

Forge server resources are loaded from:

```text
data/<namespace>/echo_story/stories/**/*.es
```

Forge checkpoints are stored per world under:

```text
<world>/data/echo_story_engine/executions
```

Operator diagnostics are exposed through the read-only `/echo_story` command tree:
`health`, `stories`, `executions`, `execution <executionId>`, `extensions`, `extension <extensionId>`, and
`diagnostics [limit]`.

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

## Feature unlock shorthand (Story Engine 1.9.0)

Inside an ES2 `OnStart()` intent section:

```es
Feature.Unlock("canvas");
Feature.Unlock("magic");
Feature.Lock("magic");
Feature.Set("magic", true);
```

These frontend calls lower to the external `echo_core:set_flag` contract. Echo Core >=0.2.0 remains responsible
for state, save persistence, and enforcing access in gameplay mods. `canvas` aliases `thought_space_unlocked`;
`magic` aliases `magic_unlocked`. Any other valid flag identifier passes through unchanged. Unlock/Lock supply
true/false, the default target is all online players (`@a`), and graph links are inferred in statement order.
Use `Feature.Unlock("canvas", player: "Alex")` for a named player or provide an explicit UUID. Generic Action
and full flag names remain supported. This shorthand introduces no gameplay state into Story core.
