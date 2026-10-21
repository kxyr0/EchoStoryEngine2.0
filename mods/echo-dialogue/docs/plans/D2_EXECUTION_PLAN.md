# D2 execution plan

## 1. Аудит текущего состояния

- Текущий Gradle graph содержит четыре D1/D1.1 модуля: `dialogue-contracts`, `dialogue-core`, `dialogue-echoscript-adapter`, `dialogue-forge-1.20.1`.
- `dialogue-core` не импортирует Forge, Minecraft, EchoScript или filesystem API.
- Скрытой persistence-логики в Forge host нет: active sessions живут только в `ActiveDialogueSessions`.
- `CompiledDialogue` содержит `DialogueId`, `startNodeId` и ordered lines; compiled fingerprint отсутствует.
- Durable cursor сейчас представлен только как `lineIndex`; для D2 line ordinal допустим только вместе с exact compiled fingerprint.
- `DialogueSessionState` хранит `sessionId`, `dialogueId`, `audienceId`, `lineIndex`, `phase`, `revision`.
- `ForgeDialogueHost` сейчас сразу вызывает `DialogueNetwork.sendOpen/sendClose` после transition; D2 должен вставить durable save перед каждым presentation event.
- `PlayerLoggedOutEvent` сейчас вызывает `cancel(player)`; D2 должен заменить это на detach без изменения durable ACTIVE session.
- `DialogueCatalogState` сохраняет previous known-good catalog при invalid reload; active sessions pinned к compiled definition.
- Существующие GameTests покрывают 4 D1/D1.1 сценария и должны остаться non-zero guard.
- Лимит seven production files по leaf package проходит; текущий максимум: `core/session` уже имеет 7 файлов.
- Документы D1.1 местами всё ещё помечают acceptance как pending, хотя D2-спецификация считает D1.1 complete/accepted. В D2-доках это нужно привести к новому статусу.

## 2. Обновление module boundary

D2 явно требует добавить один production-модуль `dialogue-persistence`. Это меняет D1-only правило из `AGENTS.md` о четырёх production-модулях, но не меняет запреты на Story Engine, choices, NPC, portraits, animation, voice, quests и generic UI.

Итоговый graph:

```text
dialogue-contracts
  <- dialogue-core
      <- dialogue-echoscript-adapter
          <- dialogue-forge-1.20.1

dialogue-contracts
  <- dialogue-core
      <- dialogue-persistence
          <- dialogue-forge-1.20.1
```

`dialogue-core` не зависит от `dialogue-persistence`; `dialogue-echoscript-adapter` не зависит от `dialogue-persistence`.

## 3. Текущая D1 session model

- Start создаёт immutable ACTIVE state с revision `0` и cursor `lineIndex=0`.
- Advance при exact `sessionId` и `expectedRevision` двигает ровно на одну строку и увеличивает revision на `1`.
- Final advance переводит phase в `COMPLETED`, presentation отсутствует, close отправляет Forge host.
- Cancel переводит phase в `CANCELLED`.
- `FAILED` используется для host/command failures и не должен автоматически рестартовать dialogue.
- Одна ACTIVE session на `AudienceId`.

## 4. Snapshot ownership

- `dialogue-core` владеет portable checkpoint contract и immutable state types, которые не знают про JSON/Path/File.
- `dialogue-persistence` владеет schema V1 DTO, codec, file repository, validation, terminal retention и recovery planning.
- `dialogue-forge-1.20.1` владеет привязкой root к world path, bounded I/O scheduler, server-thread apply и network delivery.

## 5. Repository port ownership

В `dialogue-core` добавить узкий порт без filesystem понятий:

- load by `DialogueSessionId`;
- load all retained records;
- save with explicit expected repository revision;
- acknowledge terminal record;
- cleanup terminal records.

Результаты должны быть sealed-like records/enums, не `null` и не boolean-only:

- load: found, not found, failed;
- save: created, updated, revision conflict, failed;
- acknowledge/delete: completed, not found, revision conflict, failed.

## 6. Storage layout

Forge bind:

```text
<world-root>/data/echo_dialogue/sessions/
```

Portable repository receives this root explicitly. Session file key is deterministic safe key derived from `DialogueSessionId`, not raw unsanitized arbitrary path.

Per session:

- primary: `<safe-session-id>.json`;
- backup: `<safe-session-id>.bak`;
- temp sibling: `<safe-session-id>.tmp`.

## 7. Dialogue Save Schema V1

Persisted fields:

- `saveSchemaVersion = 1`;
- `sessionId`;
- `dialogueId`;
- `compiledFingerprint`;
- `audienceId`;
- `phase`;
- `revision`;
- `currentNodeId`;
- `lineOrdinal`;
- `createdAtEpochMillis`;
- `updatedAtEpochMillis`;
- `terminalResult` for `COMPLETED`;
- `cancellationReason` for `CANCELLED`;
- bounded `metadata`;
- `repositoryRevision`.

Excluded fields:

- raw `.dialogue.es`;
- EchoScript tokens/AST;
- speaker display name and line text as authority;
- Minecraft/Forge objects;
- client overlay/input state;
- packets;
- callbacks;
- loggers;
- stack traces;
- compiled dialogue object graph;
- parser implementation names;
- diagnostic history.

## 8. Compiled dialogue fingerprint

Add the smallest deterministic compiled fingerprint in `dialogue-core`.

Canonical meaning input:

- `DialogueId`;
- `startNodeId`;
- each line in order: `nodeId`, `lineIndex`, `speakerId`, `speakerDisplayName`, `text`.

Excluded from fingerprint:

- raw source text;
- whitespace/comments/LF/CRLF;
- source path;
- AST identity;
- Java `hashCode`;
- parser class names;
- diagnostics;
- Forge/Minecraft version;
- client layout.

Restore requires exact fingerprint match. Missing dialogue or incompatible fingerprint creates blocked recovery, preserves snapshot and opens no overlay.

## 9. Save-before-presentation ordering

Introduce a narrow Forge application orchestration layer, not a generic workflow framework.

Start:

1. Resolve compiled dialogue.
2. Compute ACTIVE state and presentation.
3. Queue durable save expected-empty.
4. On server-thread save success, install state and send open.
5. On failure/conflict/saturation, do not install state and send no open.

Advance:

1. Validate player/session/revision against current server state.
2. Compute next immutable state.
3. Queue durable save with expected repository revision.
4. On server-thread save success, apply state and send next open or close.
5. On failure/conflict/saturation, keep previous state authoritative and resynchronize/unlock client with previous presentation.

Cancel/complete:

- close packet is sent only after terminal state is durably saved.

## 10. Recovery states

- `ACTIVE`: validate snapshot, resolve exact compatible dialogue, restore same session/audience/revision/current line, remain detached until player is available.
- `COMPLETED`: restore terminal result, no overlay reopen.
- `CANCELLED`: restore cancellation state, no overlay reopen.
- `FAILED`: restore as terminal or blocked according to state meaning; never restart.
- unknown phase: reject snapshot as malformed.
- duplicate active sessions for one audience: deterministic conflict, attach none.

## 11. Offline-player policy

- Disconnect does not cancel durable ACTIVE session.
- Host-only attachment state is cleared on disconnect/world leave.
- Restart loads sessions without requiring player objects.
- When matching player joins and networking is ready, host sends same current line with same `DialogueSessionId` and revision.
- Reattachment does not increment domain revision.

## 12. Terminal-result retention

- Terminal records remain queryable by `DialogueSessionId`.
- Explicit acknowledgement releases a terminal record.
- Cleanup is bounded by injected clock, max age and max terminal-record count.
- Cleanup never removes ACTIVE or blocked sessions.
- No Story Engine names, no ActionExecutionId, no event sourcing.

Default proposal:

- max terminal age: 7 days;
- max retained terminal records: 256 per world;
- cleanup deterministic by `updatedAtEpochMillis`, then `sessionId`.

## 13. Corruption policy

- Malformed JSON, unsupported schema, missing fields, invalid IDs, unknown phase, invalid cursor and incompatible fingerprint produce explicit failed/blocked results.
- Primary corrupt + valid backup loads backup and emits bounded warning.
- Primary and backup corrupt preserves both and degrades host health; unrelated sessions continue.
- Temp is never authoritative and stale temp is removed only when safe.
- Full snapshot contents and line text are not logged.

## 14. Atomic-write strategy

1. Encode schema V1 deterministic UTF-8 JSON bytes.
2. Write temp sibling.
3. Flush and close.
4. Decode/validate generated content when practical.
5. Move previous primary to one backup.
6. Atomic replace primary when supported.
7. Fallback documented non-atomic replace if atomic move unsupported.
8. Remove stale temp after successful save.

## 15. Background-I/O strategy

`dialogue-persistence` repository stays synchronous.

Forge adds one host-owned bounded serial scheduler:

- one named worker thread per host;
- queue capacity: 64 accepted operations;
- result queue capacity: 64 server-thread completions;
- no common pool, no static executor;
- immutable requests/results;
- no Minecraft API on I/O thread;
- no state mutation on I/O thread;
- shutdown flush timeout: 5 seconds.

Queue saturation returns explicit result and does not mutate session or presentation.

## 16. Shutdown strategy

- Stop accepting new operations.
- Flush bounded accepted work until timeout.
- Report unflushed operations through diagnostic.
- Close worker.
- Do not hang indefinitely and never call `System.exit`.

## 17. Planned production types

Planned `dialogue-core` additions:

- `CompiledDialogueFingerprint`;
- `DialogueSessionCheckpoint`;
- `DialogueRepositoryPort`;
- repository result records/enums;
- `TerminalDialogueRecord`.

Planned `dialogue-persistence` additions:

- schema V1 DTO;
- schema validator;
- deterministic JSON codec;
- file key/layout;
- file repository;
- checkpoint mapper;
- recovery planner;
- terminal retention policy.

Planned Forge additions:

- world storage root resolver;
- bounded persistence scheduler;
- save-before-presentation host orchestration;
- recovery/reattach read model;
- persistence status/retry command surface if needed for acceptance.

Names such as `PersistenceManager`, `RecoveryManager`, `FileUtils`, `JsonHelper` and vague helper packages are forbidden.

## 18. Planned tests

Plain-JVM:

- schema mapping/validation;
- fingerprint equivalence and semantic-change cases;
- file repository save/update/conflict/corruption/backup/temp;
- recovery active/terminal/blocked/conflict;
- terminal acknowledgement and cleanup bounds.

Orchestration:

- start/advance/completion/cancel expose no presentation before save success;
- save failure keeps previous authoritative state;
- queue saturation does not advance client;
- stale writer cannot overwrite newer state;
- bounded shutdown behavior.

GameTests:

- D1/D1.1 four existing markers remain.
- Add D2 markers for save-before-open, save-before-advance, restart restore, offline recovery, reattach, completion after recovery, incompatible definition, missing dialogue, corrupt primary backup, invalid reload pinned session, dedicated server client-class safety, world isolation.

## 19. Risks

- `core/session` already has seven files, so new core types need new leaf packages such as `core/persistence` and `core/fingerprint`.
- `ForgeDialogueHost` can become monolithic; orchestration must split by real responsibilities.
- Async save-before-presentation is easy to accidentally violate if delivery remains inside transition code.
- Gson availability must be verified before choosing codec dependency strategy.
- PTY console may not support graceful `stop`; reports must distinguish load smoke from clean stop.
- Manual visual reconnect/restart checks may remain user-pending.

## 20. Rollback

- Не создавать commit, branch, tag или push.
- Revert only D2 changes by targeted patches if runtime breaks.
- Keep D1/D1.1 behavior and tests intact while adding D2.
- If external client/server smoke is blocked, leave acceptance pending with exact reason.

## 21. Explicit non-goals

- Echo Story Engine dependency or Story–Dialogue Adapter.
- ActionExecutionId.
- choices, branching or player-selected outcomes.
- NPC lookup, portraits, head rendering, animation, voice, sound, camera, movement locking, typewriter.
- localization framework, history UI, generic UI.
- cloud/database backends, multiple persistence backends, encryption, compression.
- event sourcing, unlimited terminal history, source migrations, automatic definition migrations.
- Kotlin and mixins.
