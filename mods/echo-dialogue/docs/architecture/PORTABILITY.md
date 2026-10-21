# Portability D2

D2 persistence portable на уровне Java 17 и не привязан к Forge runtime. Portable часть живёт в `dialogue-core` и `dialogue-persistence`.

## Portable

- `DialogueCheckpoint`;
- `DialogueSessionRepository` port;
- repository load/write result types;
- `CompiledDialogueFingerprint`;
- `DialogueSaveV1`;
- UTF-8 JSON codec;
- file repository с primary/backup/temp policy;
- recovery planner;
- terminal acknowledgement/cleanup.

## Platform-Owned

Forge layer владеет:

- root path `<world-root>/data/echo_dialogue/sessions/`;
- lifecycle создания repository на `ServerStartedEvent`;
- bounded serial worker для blocking I/O;
- server-thread completion drain;
- network open/update/close;
- player login/logout reattach/detach;
- operator command read model.

## Portability Boundary

Portable repository получает root directory уже снаружи. Он не ищет Minecraft world, не читает Forge config и не хранит глобальный server reference.

Persisted schema хранит portable strings/numbers:

- `sessionId`, `dialogueId`, `compiledFingerprint`, `audienceId`;
- phase/revision/cursor;
- timestamps;
- terminal/cancel fields;
- bounded metadata;
- repository revision.

Minecraft player objects, `ServerPlayer`, `ResourceLocation`, GUI coordinates, overlay state, network packets and EchoScript AST не пересекают portability boundary.
# Portability D6

Dynamic speaker presentation provider output is runtime-only. It is not portable save data and does not alter compiled dialogue fingerprints.

Portable state remains:

- `DialogueSessionId`;
- `DialogueId`;
- `AudienceId`;
- current node/line cursor;
- presentation kind `LINE` or `CHOICE`;
- session revision;
- terminal outcome;
- authored compiled fingerprint.

Non-portable runtime state remains excluded:

- provider ID;
- provider session state;
- dynamic display name;
- dynamic `SpeakerVisualRef`;
- provider diagnostics/counters;
- Minecraft server/player/entity objects;
- resource manager cache;
- client animation state.

On restart, Echo Dialogue rebuilds the current presentation from the authored compiled dialogue and the current provider catalog. If the provider is absent or unavailable, authored fallback is shown without blocking recovery.

