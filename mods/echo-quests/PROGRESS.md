# Progress

## 0.2.0 timed conditions

- `At("HH:mm", when: ..., then: ..., otherwise: ...)` transitions are part of the quest state machine.
- `All`, `Any`, `Not`, `Player.InArea`, and `Player.HasItem` compile from EchoScript 2.
- Echo Time supplies exact crossing occurrences; Echo Quests does not poll every quest every tick.
- Online players are checked server-side. An offline player always takes the fallback branch.

## 0.1.0 foundation

- Dedicated QuestResource compiler and deterministic quest state machine.
- Player-scoped start, event dispatch, query, and persistent Forge SavedData.
- Atomic data-pack reload and public Forge lookup API.
- Background NPC integration remains range/visibility-gated before calling QuestService.
