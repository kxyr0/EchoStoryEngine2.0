# Project map

- `quest-contracts`: IDs, immutable definitions and conditions, progress DTOs, and `QuestService`.
- `quest-core`: definition validation, catalog hosting, and deterministic state transitions.
- `quest-echoscript-adapter`: ES2 parsing and `QuestResource` semantic binding.
- `quest-forge-api-1.20.1`: event-based `QuestService` discovery for other Forge mods.
- `quest-forge-1.20.1`: data-pack reload, SavedData persistence, Echo Time scheduling, vanilla player-condition
  evaluation, commands, and mod composition root.

Dependency direction keeps concrete gameplay systems outside Quest core. Those systems start a quest or dispatch a
named event through the API; Quest state never invokes Story, Dialogue, NPC, or item code directly.
