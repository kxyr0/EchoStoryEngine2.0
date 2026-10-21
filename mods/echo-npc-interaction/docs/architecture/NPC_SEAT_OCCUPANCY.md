# NPC Seat Occupancy

Seat occupancy keyed by:

```text
DimensionId + BlockPosition
```

Occupancy хранит:

- `NpcId`;
- command ID;
- anchor ID;
- observed foreign vehicle ID;
- revision.

`Npc.Interact` создаёт occupancy только после подтверждения, что именно NPC стал passenger. `Npc.StandUp` освобождает occupancy и снимает NPC с anchor.

Pre-existing vehicles не удаляются. Foreign transient vehicle очищается только если он был создан текущим use и остался пустым после handoff.
