# Sleep predicates

Публичные predicates читают только `NpcService` snapshots и durable rest attachment:

```echoscript
Npc.IsMaterialized(Sasha);
Npc.IsSleeping(Sasha);
Npc.IsSleepingAt(Sasha, SisterBed);
```

`IsSleeping` true только когда у actor есть rest attachment. `IsSleepingAt` сравнивает authored bed position и canonical bed position, поэтому вызов работает и для head, и для foot половины vanilla bed.

Predicate proximity не считается доказательством сна. Если сохраненный `BlockPosition` есть, но provider больше не валидирует кровать после restart, recovery ставит диагностику и не оставляет NPC лежать в воздухе.
