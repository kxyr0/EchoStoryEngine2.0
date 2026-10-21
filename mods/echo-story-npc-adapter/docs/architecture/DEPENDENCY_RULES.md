# Правила зависимостей

Production-адаптер может зависеть от:

- `story-contracts`;
- `story-extension-api`;
- `story-extension-forge-1.20.1`;
- `npc-contracts`;
- `npc-forge-api-1.20.1`;
- Forge/Minecraft loader APIs needed by the mod entrypoint.

Production-адаптер не должен зависеть от:

- implementation-пакетов Story;
- implementation-пакетов Echo NPC;
- EchoScript;
- Echo Dialogue;
- Story-Dialogue Adapter;
- библиотек анимации;
- пакетов navigation/pathfinding/AI;
- Gson или прямых file APIs;
- renderer/entity internals.

Public API classes являются compile/runtime зависимостями, которые поставляют
основные моды. Они не встраиваются в adapter JAR.
