# Architecture

Echo NPC N2 строится как standalone runtime вокруг immutable NPC definitions,
durable desired actor state и server-scoped materialization.

## Поток данных

```text
.npc.es
  -> EchoScript parser
  -> NpcResource semantic binding
  -> NpcDefinition
  -> NpcDefinitionCatalog
  -> ActorDesiredState
  -> ActorFileRepository
  -> ActorRuntime
  -> ForgeNpcHost
  -> EchoNpcEntity
  -> client synced presentation data
```

## Владение

- `npc-contracts` владеет публичными value types и service contracts.
- `npc-core` владеет catalog, desired actor state, runtime registry и
  repository port без Minecraft classes.
- `npc-persistence` владеет Actor Save Schema V1, JSON codec, file layout,
  backup/temp/tombstone handling и repository implementation.
- `npc-echoscript-adapter` владеет только чтением EchoScript AST и binding.
- `npc-forge-api-1.20.1` владеет typed Forge event bridge.
- `npc-forge-1.20.1` владеет Minecraft entity, commands, resources и renderer.

## Runtime

На каждый `MinecraftServer` создается один host. Он содержит `NpcCatalogState`,
`ActorRuntime`, bounded persistence queue, entity index и diagnostics buffer.
Actor record хранится в world root: `data/echo_npc/actors`.

Mutating API (`spawn`, `teleport`, `despawn`, `recovery retry`) сначала ставит
durable write в очередь и возвращает `QUEUED` с `operationId`. Entity создается,
перемещается или удаляется только после успешного repository result на server
thread.

На startup host загружает bounded actor records, проверяет definition fingerprint
и восстанавливает только actors в загруженных chunks. Остальные остаются dormant
без forced chunk loading.

## Клиент

Клиент не владеет actor lifecycle. Он получает bounded presentation data через
`SynchedEntityData` и выбирает texture из resource packs. Missing skin заменяется
fallback texture и диагностируется один раз на skin ID. `Npc.ChangeSkin` меняет
durable `activeSkinId`; при materialization применяется приоритет
`active skin -> form skin -> definition skin`.
