# NPC sleep persistence

Sleep attachment хранится в actor save schema version 2. Старые schema version 1 записи читаются без attachment и остаются совместимыми.

Recovery flow:

```text
durable NPC
-> rest attachment
-> chunk reload
-> rematerialization
-> provider validation
-> sleeping state restored
```

Если provider не может валидировать attachment, runtime не оставляет NPC в воздухе. Actor переводится в failure/blocking state, а diagnostics получает стабильный код: `NPC_SLEEP_BED_REMOVED`, `NPC_SLEEP_EXPECTED_BLOCK_MISMATCH` или `NPC_SLEEP_RECOVERY_FAILED`.

Attachment содержит owner `NpcId`, provider id, authored bed, expected block, canonical bed, sleep anchor, yaw, pose, revision, provider state, occupied flag и command id.
