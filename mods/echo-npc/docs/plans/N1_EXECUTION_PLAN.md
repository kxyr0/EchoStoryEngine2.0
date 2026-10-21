# N1 Execution Plan

## Module graph

```text
npc-contracts
  <- npc-core
  <- npc-echoscript-adapter
  <- npc-forge-1.20.1

npc-contracts
  <- npc-forge-api-1.20.1
  <- npc-forge-1.20.1
```

## Definition model

`NpcDefinition` immutable и содержит `NpcDefinitionId`, display name, optional
`NpcSkinId`, archetype и semantic fingerprint. Fingerprint строится из semantic
fields, а не из formatting source.

## Actor lifecycle

`ActorRuntime` регистрирует actors только после platform spawn success. Duplicate
same actor/same definition не создает вторую entity. Same actor/different
definition rejected. Despawn idempotent for absent actors.

## ActorId rules

`ActorId` является public logical ID. Minecraft UUID остается private platform
handle и не попадает в contracts.

## Entity mapping

Forge host хранит map `ActorId -> entity UUID`. Entity получает bounded synced
presentation fields and no autonomous behavior.

## Source syntax

Поддержан профиль `NpcResource` с `Build()` и `Npc(id, display, skin)`.
Unsupported fields rejected diagnostics.

## Reload policy

Reload создает candidate catalog отдельно. Если есть errors, active catalog не
меняется. Active actors не переписываются после reload.

## Server-thread ownership

Host создается на server lifecycle и обслуживается на server thread. Client не
владеет lifecycle.

## Renderer and skin resource policy

Renderer регистрируется только на `Dist.CLIENT`. Skin берется из resource path
`assets/<namespace>/textures/echo_npc/skins/<path>.png`; missing skin uses
`echo_npc` fallback.

## Public API

`npc-contracts` публикует `NpcService`. Forge access идет через typed event bridge
из `npc-forge-api-1.20.1`.

## Command tree

`/echo_npc health`, `definitions`, `actors`, `actor`, `spawn`, `teleport`,
`despawn`, `diagnostics`.

## Diagnostics

Diagnostics имеют stable codes, Russian message text и bounded context. Errors
block catalog replacement; warnings остаются видимыми через commands.

## Test strategy

Unit tests проверяют contracts, core lifecycle и EchoScript binding. ArchUnit
фиксирует module boundaries. GameTests проверяют Forge lifecycle and commands-level
effects. Packaging task проверяет production JAR contents.

## Packaging strategy

Public Echo NPC artifacts публикуются в `build/integration-maven`. EchoScript
public artifacts попадают в `build/external-maven` через `prepareEchoScriptArtifacts`.
Forge runtime JAR embeds internal modules and jarJars EchoScript.

## Known N1 limitations

Нет persistence across restart, recovery, animation, navigation, autonomous
movement, dialogue, Story actions, combat, inventory и wardrobe. Cross-dimension
teleport rejected.

## Rollback

Rollback N1 changes is project-local: удалить generated build outputs и вернуть
module files. Remote publishing, commits, tags и branches не создаются.

## Production type plan

Use final classes and records for concrete N1 types, public interfaces only where
there is a real boundary: `NpcService` and `PlatformActorGateway`. Avoid generic
manager/helper utility types and keep package leaves under seven production files.

## Explicit non-goals

Не добавлять placeholders для systems outside N1. N2 begins only after stable
identity, lifecycle and packaging are accepted.
