# Actor Lifecycle

N2 actor состоит из двух частей:

- durable desired state в actor repository;
- runtime materialization в виде текущей Minecraft entity.

Durable record является authority. Entity NBT используется только как technical
linkage для реконструкции и не может заменить actor storage.

## States

- `MATERIALIZED` - desired ACTIVE actor имеет связанную loaded entity.
- `DORMANT_CHUNK_UNLOADED` - desired ACTIVE actor сохранен, но target chunk не
  загружен; N2 не force-loads chunks.
- `PENDING_SPAWN`, `PENDING_MOVE`, `PENDING_REMOVAL` - mutation accepted and
  waiting for repository result.
- `BLOCKED_DEFINITION_MISSING` - persisted definition ID отсутствует в catalog.
- `BLOCKED_DEFINITION_INCOMPATIBLE` - definition fingerprint не совпал.
- `BLOCKED_WORLD_MISSING` - persisted world отсутствует.
- `BLOCKED_CORRUPT_RECORD` - actor record невозможно безопасно восстановить.
- `PLATFORM_FAILURE` - repository succeeded, но platform materialization failed.

## Operations

`spawn`, `teleport` и `despawn` являются crash-safe mutations.

- `spawn` валидирует request и definition, сохраняет ACTIVE desired state, затем
  materializes entity только после successful save.
- `teleport` сохраняет новый desired target, затем перемещает existing entity или
  оставляет actor dormant, если chunk не загружен.
- `despawn` сначала сохраняет ABSENT/tombstone, затем удаляет physical entity.
- `query`, `actors`, `blocked` и `health` синхронно читают runtime read model.

Accepted mutation сразу возвращает `QUEUED` и `operationId`. Terminal result
читается через `/echo_npc operation <operationId>` или `NpcService.operation()`.

External integration layer может передать stable `ActorMutationId` для
spawn/teleport/despawn. В этом режиме `operationId` совпадает с
`ActorMutationId.value()`, а повторный вызов с тем же ID переиспользует
известную operation вместо повторной записи в repository queue. Это не меняет
`ActorId` и не раскрывает Minecraft entity UUID.

## Shutdown

При остановке `MinecraftServer` host прекращает принимать mutations, ждёт worker
до 5 секунд и удаляет только runtime-owned loaded entities. Durable actor records
остаются в world storage для следующего startup recovery.
