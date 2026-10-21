# Entity Lifecycle

`ForgeNpcHost` является adapter между durable actor state и Minecraft world.

## Spawn

- Request валидируется на server thread.
- ACTIVE desired state сохраняется в repository через bounded worker.
- Только после successful save host вызывает materializer.
- `EchoNpcEntity` получает `ActorId`, `EntityBindingId`, `NpcDefinitionId`,
  display name и skin ID.
- Minecraft entity UUID сохраняется только как runtime platform handle.

## Teleport

Teleport сначала сохраняет новый desired world/position/rotation. После success
host перемещает existing entity. Cross-dimension recreation в N2 не скрывается:
если entity находится в другом dimension, операция получает platform rejection or
actor remains materialized according to durable state.

## Despawn

Despawn сначала сохраняет ABSENT/tombstone. После success host удаляет current
linked entity and releases runtime index. Valid ABSENT primary prevents active
backup resurrection.

## Loaded Entity Reconciliation

При load/join `EchoNpcEntity` validates technical linkage:

- malformed entity без actor/binding удаляется;
- entity without durable ACTIVE actor удаляется как orphan;
- stale binding удаляется;
- exact `ActorId + EntityBindingId` может быть adopted.

Nearby unrelated Echo NPC entities are not removed during materialization.

## Shutdown

Host stops accepting mutations, shuts down persistence worker with bounded
timeout and removes only runtime-owned loaded entities. Durable records remain in
the world repository for next startup recovery.
