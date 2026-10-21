# NPC sleep runtime

Sleep runtime разделен на contracts, durable state и Forge provider boundary.

```text
NpcId
-> BlockPosition
-> expected BlockId
-> provider
-> canonical bed
-> anchor
-> occupancy
-> sleep pose
```

Основные классы:

- `NpcRestService`, `NpcRestQueryService`, `NpcRestProviderRegistry` в `npc-contracts`;
- `NpcRestAttachment` в `npc-core`;
- `RestSaveRecord` и schema version 2 в `npc-persistence`;
- `ForgeNpcRestOperations`, `ForgeRestRuntime`, `VanillaBedRestProvider` в Forge runtime.

`Npc.Move`/teleport sleeping NPC отклоняется `NPC_MOVE_WHILE_SLEEPING`, чтобы durable rest attachment не расходился с фактической позой.
