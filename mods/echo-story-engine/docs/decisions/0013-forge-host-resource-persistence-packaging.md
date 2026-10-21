# 0013 Forge Host Resource Persistence Packaging

## Status

Accepted.

## Context

M3.6 can compile EchoScript `.es` StoryResource text and M3 can persist checkpoints through a Java file repository,
but Forge still needs the platform host that connects server resources, world-specific storage, lifecycle, diagnostics,
GameTest, and distributable packaging.

## Decision

Keep all Forge/Minecraft integration in `story-forge-1.20.1`.

Use server data resources under `data/<namespace>/echo_story/stories/**/*.es`. Resource reload compiles all discovered
`.es` files into an immutable catalog candidate. Accepted candidates atomically replace the current catalog; invalid
candidates keep the previous catalog.

Bind durable execution storage to:

```text
<world>/data/echo_story_engine/executions
```

Use the existing `FileExecutionStateRepository` behind the core port. The Forge host owns a bounded single-worker
persistence queue, while story state and command dispatch stay on the server thread.

Expose read-only diagnostics through `/echo_story diagnostics`. M4 does not introduce production external action
handler discovery; missing production handlers produce diagnostics and remain an M5 extension-contract problem.

The Forge jar embeds Java-only Story Engine modules plus EchoScript/Gson runtime classes and verifies that
Forge/Minecraft and GameTest-only classes are absent from the distributable artifact.

## Alternatives

- Put resource loading in `story-definition`. Rejected because Minecraft resource APIs belong to the platform adapter.
- Store checkpoints in core-owned files. Rejected because core must not know filesystem paths or storage technology.
- Dispatch commands directly from the persistence worker. Rejected because story state and platform calls must stay on
  the server thread.
- Add production action registry in M4. Rejected because M4 validates the host boundary; public extension contracts are
  separate M5 work.
- Depend on Maven Local at runtime. Rejected because users install one Forge mod jar.

## Consequences

`story-forge-1.20.1` now depends on `story-definition`, `story-echoscript-adapter`, `story-persistence`, EchoScript,
and Gson. Lower modules remain free of Forge/Minecraft imports. The production host can diagnose missing action
handlers but cannot execute gameplay-specific effects until a future extension contract exists.

## Enforcement

Gradle dependency graph, ArchUnit rules, `sourceShapeCheck`, Forge unit tests, GameTest, packaging inspection, docs,
and repository scans for forbidden platform leakage.

## Links

- `docs/plans/0007-m4-forge-host-resource-loading-packaging.md`
- `docs/forge/FORGE_HOST.md`
- `docs/forge/RESOURCE_LOADING.md`
- `docs/forge/DISTRIBUTION_PACKAGING.md`
- `docs/architecture/PROJECT_MAP.md`
