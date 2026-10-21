# Npc.SleepAt

`Npc.SleepAt` укладывает конкретного NPC на конкретную кровать. Автор обязан указать `BlockPosition` и `expectedBlock`; runtime не ищет ближайшую кровать и не выбирает случайный соседний блок.

Пример:

```echoscript
Npc.SleepAt(Sasha, SisterBed, expectedBlock: SisterBedType);
```

Проверки выполняются на сервере: dimension, loaded chunk, фактический block id, поддерживаемый rest provider, occupied state, расстояние NPC до кровати и возможность поставить sleep attachment. Если block id отличается, команда завершается `NPC_SLEEP_EXPECTED_BLOCK_MISMATCH`.

Сон является настоящим durable rest attachment: сохраняется provider id, authored bed position, canonical bed, sleep anchor, yaw, pose, revision и command id. Командный аналог: `/echo_npc sleep_at <npc_id> <bed> <expected_block_id>`.
