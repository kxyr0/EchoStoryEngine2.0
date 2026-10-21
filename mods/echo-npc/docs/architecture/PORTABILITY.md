# Portability

N1 отделяет переносимое ядро от Forge runtime.

## Portable

- ID validation.
- Neutral positions and rotations.
- Immutable `NpcDefinition`.
- Catalog installation and lookup.
- Actor lifecycle decisions.
- Public service contracts.
- EchoScript semantic binding.

## Forge-specific

- `ResourceLocation` discovery.
- Resource reload listener.
- `MinecraftServer` scoped composition root.
- Entity spawn, move and removal.
- Commands.
- `SynchedEntityData`.
- Client renderer and texture lookup.

## Future port rule

Новый platform adapter должен реализовать `PlatformActorGateway` и использовать
`npc-contracts` как API. Он не должен копировать core logic или публиковать
собственные версии public contracts.
