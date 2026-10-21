# Portability

## Version Boundaries

Versions are distinct and centrally recorded in `gradle.properties`:

- `engine_version`
- `api_version`
- `story_format_version`
- `save_schema_version`
- `network_protocol_version`
- `minecraft_version`
- `forge_version`

Story Engine versions must not be treated as Minecraft versions.

## Platform Boundary

Contracts and core use `NamespacedId`, `StoryId`, `ExecutionId`, and `NodeId` instead of Minecraft
`ResourceLocation`, `Entity`, `Level`, `CompoundTag`, `FriendlyByteBuf`, or Forge events.

If the project moves to another Minecraft version or loader, adapters should change while contracts/core remain stable.

`story-persistence` uses Java standard filesystem APIs behind the core repository port. The explicit root directory is
provided by the caller. In M4, `story-forge-1.20.1` selects the active world root and binds execution storage to
`<world>/data/echo_story_engine/executions`. Core never receives `Path`, `File`, JSON, NBT, database, or
Forge/Minecraft objects through persistence APIs.

## Source Frontend Boundary

`story-definition` owns neutral source DTOs, validation, diagnostics, and compilation into `StoryDefinition`.
Concrete source frontends are replaceable adapters. In M3.6, `story-echoscript-adapter` is the only production
frontend and all EchoScript parser types stay inside that module.

Runtime, persistence, and public contracts use compiled `StoryDefinition` values and `CompiledStoryFingerprint`, not
EchoScript syntax trees, `.es` source text, parser DTOs, or frontend-specific diagnostics.

## Forge Policy

ForgeGradle is applied only in `story-forge-1.20.1`.

Forge resource, lifecycle, command, GameTest, and packaging code stays in `story-forge-1.20.1`. `.es` resources are
server data resources under `data/<namespace>/echo_story/stories/**/*.es`; this resource convention is a platform
adapter concern, not a contracts/core API.

Mixins are forbidden in the initial scaffold. Adding one later requires an ADR proving that Forge events/API cannot
solve the problem.

## Java Policy

The initial scaffold is Java-only. Kotlin may be reconsidered only through a future ADR covering Java interoperability,
public API friendliness, and Forge build impact.
