# Q1 — Quest state machine

## Scope

Create a standalone `echo_quests` Forge mod with a platform-neutral state machine and ES2 `QuestResource` authoring.
Definitions are loaded from data packs, progress is keyed by player UUID and quest ID, and integrations use narrow
service boundaries. Echo Time occurrences drive exact timed branches; concrete player checks stay in the Forge host.

## Invariants

- a started quest enters exactly one declared initial active state;
- only a declared event transition can change the current state;
- a timed transition chooses exactly one declared success or fallback target at its Echo Time occurrence;
- an offline player cannot satisfy a player condition and takes the fallback branch;
- completed and failed states are terminal;
- starting the same player quest twice is idempotent;
- a rejected reload preserves the previous catalog;
- persistence stores current state, lifecycle status, and monotonic revision;
- proximity remains the responsibility of the interaction that offers the quest;
- Minecraft position and inventory code cannot enter the platform-neutral core.

## Modules

`quest-contracts <- quest-core <- quest-echoscript-adapter <- quest-forge-1.20.1`, with
`quest-forge-api-1.20.1` exposing the Forge service lookup boundary.
