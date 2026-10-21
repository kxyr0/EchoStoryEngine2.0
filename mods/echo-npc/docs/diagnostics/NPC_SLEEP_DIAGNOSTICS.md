# NPC sleep diagnostics

Минимальные N4 diagnostics:

- `NPC_SLEEP_NPC_NOT_MATERIALIZED` - NPC не имеет materialized body.
- `NPC_SLEEP_NPC_REMOVED` - durable actor отсутствует.
- `NPC_SLEEP_TOO_FAR` - NPC дальше допустимого расстояния от authored bed.
- `NPC_SLEEP_EXPECTED_BLOCK_MISMATCH` - actual block id не равен `expectedBlock`.
- `NPC_SLEEP_BED_NOT_FOUND` - chunk/block отсутствует.
- `NPC_SLEEP_BED_UNSUPPORTED` - provider не поддерживает block.
- `NPC_SLEEP_BED_OCCUPIED` - кровать занята.
- `NPC_SLEEP_PROVIDER_NOT_FOUND` - нет provider.
- `NPC_SLEEP_ATTACHMENT_REJECTED` - provider отказал в attachment.
- `NPC_SLEEP_ATTACHMENT_TIMEOUT` - attachment не подтвержден вовремя.
- `NPC_SLEEP_BED_REMOVED` - recovery нашел удаленную кровать.
- `NPC_SLEEP_RECOVERY_FAILED` - восстановление сна не завершилось.
- `NPC_WAKE_UP_REJECTED` и `NPC_WAKE_POSITION_INVALID` - wake отказан.
- `NPC_MOVE_WHILE_SLEEPING` - движение sleeping NPC запрещено.

Используйте `/echo_npc inspect_rest <npc_id>`, `/echo_npc inspect_bed <bed>` и `/echo_npc diagnostics`.
