# NPC Interaction Arguments

Аргументы `Npc.Interact`:

- `npc`: `NpcId`/`ActorId` или alias host-а.
- `x`, `y`, `z`: exact `BlockPosition`.
- `position`: `BlockPosition`.
- `face`: `AUTO` или явная грань.
- `hand`: `MAIN_HAND` или `OFF_HAND`.
- `approach`: `AUTO`.
- `dimension`: server dimension id.

Defaults:

```text
face = AUTO
hand = MAIN_HAND
approach = AUTO
dimension = minecraft:overworld
```

NI1 не принимает обязательные `BlockId`, provider id, backend id или expected
result.

