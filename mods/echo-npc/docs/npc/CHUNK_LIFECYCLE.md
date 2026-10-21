# Chunk Lifecycle

N2 не force-loads chunks для NPC.

## Loaded Target Chunk

Если desired ACTIVE actor указывает на загруженный chunk, recovery или mutation
пытается обеспечить ровно одну linked `EchoNpcEntity`.

## Unloaded Target Chunk

Если target chunk не загружен:

- durable actor record остается `ACTIVE`;
- runtime status становится `DORMANT_CHUNK_UNLOADED`;
- entity не создается;
- chunk ticket не создается;
- next tick reconciliation повторит materialization, когда chunk станет loaded.

## Chunk Unload

Chunk unload не удаляет actor record. Loaded entity may disappear with the chunk,
but durable desired state remains authoritative.

## Teleport

Teleport сохраняет desired target before movement. Если target chunk unloaded,
actor remains durable and dormant. Когда chunk загрузится, host reconstructs the
entity at persisted position and rotation.
