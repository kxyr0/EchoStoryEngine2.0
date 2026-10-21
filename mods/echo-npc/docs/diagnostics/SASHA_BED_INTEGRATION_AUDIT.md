# Sasha bed integration audit

Эта страница заполняется после того, как автор предоставляет реальные координаты и block id кровати. Не угадывайте значения.

## Required fields

- NpcId: `jdh:sasha`
- Bed BlockPosition: `TODO_BED_X TODO_BED_Y TODO_BED_Z`
- Expected BlockId: `TODO:actual_bed_block_id`
- Actual BlockId from `/echo_npc inspect_bed`: `TODO`
- Provider: `echo_npc:vanilla_bed` или `TODO:custom_provider`
- Facing: `TODO`
- Head/foot input: `TODO`
- Canonical bed: `TODO`
- Occupied before SleepAt: `TODO`
- Occupied after SleepAt: `TODO`
- Sleep anchor: `TODO`
- Sleep yaw: `TODO`

## Acceptance

Dialogue may start only after `Npc.SpawnAt` completed, scene-ready succeeded, `Npc.SleepAt` completed and `Npc.IsSleepingAt(Sasha, SisterBed)` returned true.
