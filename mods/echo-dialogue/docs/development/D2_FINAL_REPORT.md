# D2 Final Report

## Краткий итог

D2 добавляет durable persistence/recovery для Echo Dialogue без Echo Story Engine integration. Сессия теперь сохраняется как portable checkpoint до любого client presentation update. После restart compatible ACTIVE snapshot восстанавливается detached с тем же `DialogueSessionId`, `AudienceId`, `revision` и cursor; terminal snapshots остаются queryable до acknowledgement/cleanup.

Не добавлялись choices, branching, NPC lookup, portraits, animation, voice, quests, generic UI, Kotlin, mixins, database/cloud backend или Story-Dialogue Adapter.

## Gradle Structure

Итоговый module graph:

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

`dialogue-persistence` — plain Java 17 module. Он зависит от `dialogue-core`, `dialogue-contracts` и Gson. `dialogue-core` не зависит от `dialogue-persistence`.

## Dialogue Save Schema V1

`DialogueSaveV1` хранит ровно эти поля:

- `saveSchemaVersion`;
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
- `terminalResult`;
- `cancellationReason`;
- `metadata`;
- `repositoryRevision`.

Принципиально не сохраняются raw `.dialogue.es`, EchoScript tokens/AST, source path, comments/whitespace, speaker/line text как authority, Minecraft/Forge objects, `ServerPlayer`, `MinecraftServer`, `Level`, client overlay state, input debounce, network packets, callbacks, loggers, stack traces, compiled object graphs и diagnostic history.

## Storage

Forge root:

```text
<world-root>/data/echo_dialogue/sessions/
```

На одну session используются bounded files:

- `session-<sha256(sessionId)>.json`;
- `session-<sha256(sessionId)>.bak`;
- `session-<sha256(sessionId)>.tmp`.

Write policy: encode UTF-8 JSON, write temp, force temp data, move previous primary to one backup, replace primary with atomic move when filesystem supports it, otherwise documented replace fallback. Backup-only snapshots after interrupted replacement участвуют в `loadAll()`.

## Repository Revisions

Initial save uses `SaveExpectation.expectedEmpty()` and creates `repositoryRevision=0`. Updates require exact expected `DialogueRepositoryRevision`; stale writers receive `REVISION_CONFLICT` and cannot overwrite newer snapshots. `acknowledgeTerminal` also requires exact terminal repository revision.

## Save-Before-Presentation

Start:

1. Resolve compiled dialogue.
2. Preview immutable ACTIVE state.
3. Queue durable save.
4. Apply save result on server thread.
5. Only after `CREATED`, install state and send open.

Advance/cancel/complete:

1. Validate sender/session/revision.
2. Preview next immutable state.
3. Queue durable save with expected repository revision.
4. Apply result on server thread.
5. Only after `UPDATED`, install state and send next open or close.

Save failure, revision conflict, queue saturation or shutdown does not advance authoritative state and sends a resync of the previous durable presentation when one exists. Runtime exceptions inside save/load are converted to typed failed results (`EDLG-FORGE-017`, `EDLG-FORGE-018`) so the worker does not silently drop completion.

## Fingerprint

`CompiledDialogueFingerprinter` hashes canonical compiled meaning:

- `DialogueId`;
- start node;
- ordered lines;
- line ordinal;
- node id;
- speaker id;
- speaker display name;
- line text.

Raw source text, comments, whitespace, LF/CRLF, source path, AST identity, parser class names, diagnostics, Forge/Minecraft version and client layout are excluded. Exact fingerprint match is required for recovery.

## Recovery

- `ACTIVE`: exact fingerprint required; restores detached with same session/audience/revision/current line.
- `COMPLETED`: retained terminal record, no overlay reopen.
- `CANCELLED`: retained terminal record, no overlay reopen.
- missing compiled dialogue: blocked, snapshot retained.
- incompatible fingerprint: blocked, snapshot retained.
- corrupt schema/cursor: corrupt diagnostic, no partial session.
- duplicate ACTIVE for one audience: deterministic ordering by session id; first restored, later duplicate blocked with `EDLG-FORGE-016`.

GameTest evidence for durable restore: `ForgeDialoguePersistenceGameTests.durableSnapshotRestoresSameLine` saves `sessionId=gametest-durable-session`, `audienceId=gametest-audience`, then reloads through a fresh repository and asserts `revision=1`, `lineOrdinal=1`. The fixture line at ordinal 1 is speaker `Игрок`, text `Гулял.`.

## Offline And Reattach

Logout only detaches host attachment state and records `EDLG-FORGE-009` if an ACTIVE session exists. It does not cancel the session and does not increment revision. Login calls reattach; current presentation is sent with the same session id/revision and diagnostic `EDLG-FORGE-010`.

## Terminal Retention

Terminal records stay queryable after `COMPLETED` or `CANCELLED`. Explicit acknowledgement deletes only terminal records. Cleanup is bounded by caller-provided `nowEpochMillis`, `maxAgeMillis` and `maxRecords`, and never deletes ACTIVE snapshots.

## Forge I/O Scheduler

`ForgePersistenceScheduler` is host-owned and serial:

- operation queue: `64`;
- completion queue: `64`;
- per-tick completion drain: `32`;
- shutdown flush timeout: `5s`;
- no common pool;
- no Minecraft API on I/O worker;
- completion mutation happens only after server-thread drain.

Shutdown stops accepting work, waits bounded time, drains completions, clears in-memory sessions and emits `EDLG-FORGE-007`. Timeout emits `EDLG-FORGE-011`.

## Production Types

Created/changed D2 production types and responsibilities:

- `DiagnosticCategory`: D2 persistence/recovery categories.
- `CompiledDialogueFingerprint`: stable fingerprint value.
- `CompiledDialogueFingerprinter`: canonical compiled meaning hash.
- `DialogueCheckpoint`: portable checkpoint contract.
- `DialogueRepositoryRevision`: optimistic repository revision.
- `DialogueRepositoryLoadResult`: typed load result.
- `DialogueRepositoryWriteResult`: typed write/ack/cleanup result.
- `DialogueSessionRepository`: core-owned persistence port.
- `DialogueSessionEngine`: exposes presentation read model for recovery/resync.
- `ActiveDialogueSessions`: preview/install/dialogue lookup for save-before-presentation.
- `DialogueSaveV1`: persisted schema DTO.
- `DialogueSaveJsonCodec`: explicit Gson codec and validation mapping.
- `SessionFileLayout`: safe deterministic filenames.
- `SessionFileStore`: primary/backup/temp file operations.
- `SessionSnapshotReader`: load/loadAll and backup fallback.
- `FileDialogueSessionRepository`: synchronous durable repository.
- `TerminalSessionCleanup`: bounded terminal cleanup.
- `DialogueRecoveryPlan`: recovery decision model.
- `DialogueRecoveryPlanner`: compatible/blocked/terminal/corrupt planning.
- `ForgeWorldPersistenceRoot`: world-local storage root resolver.
- `ForgePersistenceScheduler`: bounded serial I/O worker.
- `ForgeDialogueCompositionRoot`: creates repository/scheduler per server and wires login/logout/tick.
- `ForgeDialogueHost`: server-thread owner and lifecycle facade.
- `ForgeDialogueCheckpointFactory`: transition-to-checkpoint mapping.
- `ForgeDialogueSaveFlow`: save-before-presentation orchestration.
- `ForgeDialogueRecovery`: recovery and player reattach.
- `ForgeDialogueStatusTracker`: bounded operator read model counters.
- `ForgeDialogueHostStatus`: command/read-model snapshot.
- `ForgeDialogueFailures`: host failure transitions.
- `EchoDialogueCommands`: exposes active/recovered/blocked/terminal status.

## Counts And Limits

`sourceShapeCheck` passed. High-water leaf package counts:

- `dialogue-forge-1.20.1/.../forge/host`: 7 files;
- `dialogue-core/.../core/session`: 7 files;
- `dialogue-echoscript-adapter/.../binding`: 6 files;
- `dialogue-contracts/.../identity`: 6 files;
- `dialogue-core/.../core/persistence`: 5 files;
- `dialogue-persistence/.../file`: 5 files.

Production class hard limit 180 logical LOC passed after moving recovery load failure handling out of `ForgeDialogueHost`.

## Tests

Plain-JVM XML after final run:

```text
tests=50, failures=0, errors=0, skipped=0
```

Focused coverage includes identifiers/diagnostics, core transitions, active-session policy, fingerprint compatibility, schema encode/decode, repository revisions, backup/temp recovery, terminal acknowledgement/cleanup, duplicate ACTIVE recovery conflict, client stale state guards and Forge host lifecycle.

GameTest server:

```text
discovered=8, executed=8, passed=8
```

Markers observed:

- `ForgeDialogueGameTests.echoDialogueCommandsExist`;
- `ForgeDialogueGameTests.dialogueResourceCompilesInForgeRuntime`;
- `ForgeDialogueGameTests.controlledAudienceSessionLifecycle`;
- `ForgeDialogueGameTests.invalidCandidateRetainsCatalog`;
- `ForgeDialoguePersistenceGameTests.durableSnapshotRestoresSameLine`;
- `ForgeDialoguePersistenceGameTests.incompatibleFingerprintBlocksRecovery`;
- `ForgeDialoguePersistenceGameTests.terminalRecordCanBeAcknowledged`;
- `ForgeDialoguePersistenceGameTests.worldRootsAreIsolated`.

## Server And Artifact Acceptance

Normal `runServer` after manual `eula=true`:

- reached `Done (7.830s)!`;
- logged `EDLG-FORGE-003 Reload диалогов принят: 1`;
- logged `EDLG-FORGE-006 Dialogue host активен`;
- no Java process remained after external shutdown;
- clean `stop` through Gradle PTY was not confirmed because stdin was not delivered;
- additional `cmd /c "echo stop| gradlew.bat :dialogue-forge-1.20.1:runServer --console=plain"` attempt also did not produce clean-stop acceptance and was stopped externally, so it is not counted as a passed stop check.

Isolated final-JAR server:

- `build/smoke/server/mods` contained only `echo-dialogue-0.1.0-SNAPSHOT.jar`;
- hash matched final JAR: `27E5EBF1D689243CE78D17B9E0D68B6292563959E4B4A35E07C3314F7C59D351`;
- reached `Done (7.436s)!`;
- logged `EDLG-FORGE-003`, `EDLG-FORGE-006`, `EDLG-FORGE-007`;
- exited with code `0`;
- no Java process remained.

Manual visual client restart/reconnect was not executed by оператор. It remains user-pending: start `sample:greeting`, advance to a middle line, restart server/client, reconnect to the same world, confirm same session id/revision/current line, complete dialogue.

## Final JAR

Path:

```text
D:\JDHSeason\mods\echo-dialogue\dialogue-forge-1.20.1\build\libs\echo-dialogue-0.1.0-SNAPSHOT.jar
```

Size: `264915`.

SHA-256:

```text
27E5EBF1D689243CE78D17B9E0D68B6292563959E4B4A35E07C3314F7C59D351
```

Artifact scan:

```text
entries=204
persistenceEntries=17
GsonEntries=0
MinecraftEntries=0
ForgeEntries=0
GameTestEntries=0
JUnitEntries=0
StoryEntries=0
```

Final users install only this JAR.

## Executed Commands

- `.\gradlew.bat projects test architectureTest sourceShapeCheck checkWhitespace :dialogue-forge-1.20.1:verifyProductionJarContents --console=plain` — success.
- `.\gradlew.bat check build --console=plain` — success.
- `.\gradlew.bat :dialogue-forge-1.20.1:runGameTestServer --console=plain` — success, 8/8.
- `.\gradlew.bat :dialogue-forge-1.20.1:runServer --console=plain` — load smoke success; clean stop through Gradle PTY not confirmed.
- `cmd /c "echo stop| gradlew.bat :dialogue-forge-1.20.1:runServer --console=plain"` — load smoke reached server startup, but clean stop was not confirmed; process was stopped externally and this command failed by design.
- `build/smoke/server/run.bat --nogui` — isolated final-JAR server success and exit code 0.
- `git diff --check` — success.

Additional scans:

- Story Engine imports in production sources: no matches.
- Forge/Minecraft imports outside Forge module: no matches.
- Java serialization/Object streams/`CompletableFuture`/`Path`/Gson in `dialogue-core`/`dialogue-contracts`: no matches.
- `TODO`/`FIXME` in main/test Java sources: no matches.

## Not Executed

- Real visual client restart/reconnect: requires interactive client observation; not claimed.
- Normal Gradle `runServer` clean `stop`: attempted twice through the automation terminal, stdin did not reach the server console. Direct userdev reconstruction was rejected by Forge module-path setup and was not counted as an acceptance run. Isolated final-JAR clean stop is executed and passed.

## Risks

- Visual reconnect still needs manual observation for UI confidence.
- Gradle 8.10.2 prints a ForgeGradle deprecation warning for `runtimeClasspathCopy`; it does not fail current checks.
- Normal `runServer` stop remains awkward under the automation terminal; use isolated `run.bat` or a real terminal for clean-stop acceptance.

## Deferred

D3 remains next: Story-Dialogue Adapter using Echo Story Engine M5 extension API. D2 did not implement Story Engine dependency, `ActionExecutionId`, choices, branching, NPCs, portraits, animation, voice, cinematics or gameplay extensions.
