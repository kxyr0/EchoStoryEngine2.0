# Persistence D5

`dialogue-persistence` — portable Java 17 module для durable dialogue session checkpoints. Он зависит от `dialogue-core` и `dialogue-contracts`, но не знает Forge, Minecraft, network packets, client UI, EchoScript runtime или Echo Story Engine.

## Storage Path

Forge host создаёт repository на каждый started server/world:

```text
<world-root>/data/echo_dialogue/sessions/
```

Путь принадлежит world save. Разные worlds не разделяют snapshots.

## Save Schema V2

Новые snapshots пишутся как schema V2. V1 остаётся read-only legacy decode path для существующих saves.

Каждый V2 snapshot хранит:

- `saveSchemaVersion`;
- `sessionId`;
- `dialogueId`;
- compiled dialogue fingerprint;
- `audienceId`;
- `phase`;
- session `revision`;
- current node id;
- presentation kind: `LINE` или `CHOICE`;
- line ordinal;
- created/updated timestamps;
- terminal outcome для `COMPLETED`;
- cancellation reason для `CANCELLED`;
- bounded metadata;
- repository revision.

Не сохраняются:

- raw `.dialogue.es` source;
- source path;
- comments/whitespace;
- full diagnostic history;
- network packet instances;
- callbacks;
- client overlay/UI state;
- client visual cache/resource state;
- `SpeakerVisualRef`, resource paths, PNG bytes, image dimensions;
- dynamic provider display name, dynamic provider visual, provider ID, provider status, provider diagnostics;
- Forge/Minecraft objects;
- stack traces.

## Fingerprint

Fingerprint считается в `dialogue-core` по canonical compiled meaning:

- dialogue id;
- start node id;
- ordered line node ids;
- line ordinal;
- speaker id;
- speaker display name;
- speaker visual presence/kind/id;
- line text;
- ordered choices: `ChoiceId`, text, target node;
- terminal `DialogueOutcomeId`.

Raw source path, comments, formatting, LF/CRLF, whitespace вне compiled meaning и physical PNG bytes не участвуют. Semantic text/order/speaker/visual/choice/outcome changes меняют fingerprint.

D5 не вводит Schema V3: visual descriptor восстанавливается из compatible compiled definition, а Schema V2 cursor и fingerprint уже достаточны. Замена PNG bytes под тем же visual ID не блокирует recovery; изменение authored visual ID или kind блокирует recovery как incompatible fingerprint.

D6 также не вводит Schema V3. Dynamic provider output является runtime-only presentation state: после restart provider sessions создаются заново, текущая presentation снова строится из authored compiled definition и active provider catalog. Если provider отсутствует или недоступен, recovery не блокируется и показывается authored fallback.

## Repository

Repository port возвращает explicit results:

- load: `FOUND`, `NOT_FOUND`, `FAILED`;
- save: `CREATED`, `UPDATED`, `REVISION_CONFLICT`, `FAILED`;
- acknowledge/cleanup: `COMPLETED`, `NOT_FOUND`, `REVISION_CONFLICT`, `FAILED`.

Stale writer не может перезаписать более новый snapshot: update требует expected repository revision.

## File Policy

На session используются bounded files:

- one primary `.json`;
- one backup `.bak`;
- one temp `.tmp`.

Write flow:

1. Encode schema V2 в UTF-8 JSON.
2. Записать temp file.
3. `FileChannel.force(true)` для temp data.
4. Переместить previous primary в one backup.
5. Переместить temp в primary через atomic move, если filesystem поддерживает.
6. Если atomic move недоступен, используется documented replace move fallback.

Primary corrupt + valid backup: load возвращает backup и diagnostic `BACKUP_FALLBACK_USED`.

Primary + backup corrupt: load возвращает `FAILED` и diagnostic `PRIMARY_AND_BACKUP_CORRUPT`.

Backup-only snapshot после interrupted replacement участвует в `loadAll()` recovery.

## Terminal Retention

Terminal records остаются queryable после `COMPLETED`/`CANCELLED`, чтобы future external owner мог прочитать result после crash window:

```text
dialogue completes
-> terminal state saved
-> server crashes before external consumer observes completion
```

Terminal record можно release через explicit acknowledgement с expected repository revision.

Cleanup policy bounded:

- explicit acknowledgement preferred;
- cleanup удаляет только terminal records;
- ACTIVE и blocked snapshots cleanup не удаляет;
- max age и max terminal-record count применяются детерминированно;
- cleanup не создаёт unbounded history.

Текущие conservative defaults задаются вызывающей стороной cleanup; repository не запускает фоновые cleanup tasks сам.

## Forge I/O

Portable repository выполняет blocking I/O синхронно. Forge не вызывает его напрямую из gameplay transition path: host-owned `ForgePersistenceScheduler` принимает bounded queue, выполняет операции serial worker thread и возвращает completion callbacks на server tick.

Queue limits:

- operation queue: `64`;
- completion queue: `64`;
- per-tick drain: `32`;
- shutdown flush timeout: `5s`.

Queue saturation, dropped completion и shutdown timeout пишутся в общий diagnostic pipeline.
