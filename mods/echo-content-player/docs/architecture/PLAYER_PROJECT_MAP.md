# Карта проекта Echo Content Player

Репозиторий:

```text
D:\JDHSeason\mods\echo-content-player
```

Модули:

- `player-contracts`: immutable public contracts, без Minecraft/Forge/EchoScript implementation imports.
- `player-core`: plain Java validation, mutation, template binding, persistence DTO, recovery, synchronization.
- `player-forge-api-1.20.1`: public Forge-facing API для других модов.
- `player-forge-1.20.1`: Forge entrypoint, lifecycle hooks, commands, networking, SavedData host, client mirror.
- `player-testmod-1.20.1`: development-only manual hooks, не входит в production jar.

Запрещённые зависимости:

- Echo Story Engine;
- Echo Dialogue;
- Echo NPC;
- Echo Animation;
- Echo Trigger;
- Echo Item Pack;
- Echo Wardrobe;
- их implementation JAR или внутренние классы.

EchoScript используется только как опубликованный Maven artifact.
