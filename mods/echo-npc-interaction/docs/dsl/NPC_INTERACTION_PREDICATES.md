# NPC Interaction Predicates

`Npc.IsInteracting(npc)` является planned DSL predicate. В NI1 runtime уже
предоставляет public query:

```java
NpcInteractionQueryService.isInteracting(npcId)
```

Семантика:

- `true`: у NPC есть active interaction status до terminal state.
- `false`: active interaction отсутствует или уже завершён.

Query не запускает movement, provider selection или verification.
