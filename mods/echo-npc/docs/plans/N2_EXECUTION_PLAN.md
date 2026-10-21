# N2 Execution Plan

Дата начала: 2026-07-19.

## Цель

N2 добавляет world-scoped durable actor persistence, startup recovery и
deterministic entity reconstruction. Durable logical actor record становится
authority; Minecraft entity остается только текущей materialized projection.

## Audit Summary

- N1 принят пользователем как prerequisite для N2.
- Текущий module graph: `npc-contracts`, `npc-core`, `npc-echoscript-adapter`,
  `npc-forge-api-1.20.1`, `npc-forge-1.20.1`.
- `ActorRuntime` сейчас runtime-only: platform spawn происходит до регистрации
  actor, а `stop()` удаляет entities и очищает registry.
- `EchoNpcEntity` пишет в vanilla NBT `ActorId`, `NpcDefinitionId`, display name
  и skin ID; N2 заменяет это на minimal technical linkage plus diagnostics data.
- `ForgeNpcEntityIndex` хранит `ActorId -> Entity UUID` только runtime-local.
- `NpcDefinition.fingerprint()` уже semantic SHA-256 и подходит для exact
  recovery compatibility.
- `npc-contracts/api/service` уже содержит семь production-файлов, поэтому N2
  расширяет существующие public records/enums вместо добавления восьмого файла
  в тот же leaf package.
- `git status` невозможен: каталог не является git repository.
- Proven packaging pattern из Echo Story Engine и Echo Dialogue: internal modules
  встраиваются в Forge `jar`/`jarJar`; EchoScript остается jarJar dependency.

## Module Graph

Добавить ровно один production module:

```text
npc-contracts
  <- npc-core
  <- npc-persistence
  <- npc-forge-1.20.1

npc-contracts
  <- npc-forge-api-1.20.1
  <- npc-forge-1.20.1

npc-contracts
  <- npc-core
  <- npc-echoscript-adapter
  <- npc-forge-1.20.1
```

`npc-persistence` зависит только от `npc-core` и `npc-contracts`.

## Boundaries

`npc-contracts`:

- public IDs, requests, results, health, read models;
- no Forge/Minecraft, no Path/File/JSON/Gson, no `EntityBindingId` by default.

`npc-core`:

- `ActorDesiredState`, materialization status vocabulary, repository port;
- mutation orchestration contracts and recovery planning;
- definition fingerprint compatibility;
- no filesystem implementation.

`npc-persistence`:

- Actor Save Schema V1 DTO and mapper;
- deterministic UTF-8 JSON codec;
- file layout, hash key, validation, backup/temp/tombstone handling;
- portable synchronous repository implementation;
- no Forge/Minecraft/EchoScript AST/renderer/network/logging backend.

`npc-forge-1.20.1`:

- world root resolution through Minecraft APIs;
- bounded serial I/O scheduler;
- server-thread result application;
- entity materialization/adoption/removal;
- entity/chunk lifecycle hooks;
- commands and diagnostics routing.

## Actor Save Schema V1

One JSON file per logical actor under:

```text
<world-root>/data/echo_npc/actors/<sha256(canonical ActorId)>.json
```

Required persisted fields:

- `schemaVersion`: `1`;
- `actorId`;
- `definitionId`;
- `definitionFingerprint`;
- `presence`: `ACTIVE` or `ABSENT`;
- `worldId`;
- `position`: finite `x`, `y`, `z`;
- `rotation`: finite `yaw`, `pitch` validated like public `NpcRotation`;
- `entityBindingId`;
- `actorRevision`;
- `repositoryRevision`;
- optional `createdAtEpochMillis` and `updatedAtEpochMillis` from injected clock.

Never persist:

- Minecraft Entity UUID as actor authority;
- numeric entity ID, `Entity`, `ServerLevel`, `MinecraftServer`;
- display name or skin as independent authority;
- full `NpcDefinition`, `.npc.es`, AST, tokens;
- renderer/client cache, PNG bytes, animation/navigation/AI/Story/Dialogue state;
- executor/future/callback/logger/diagnostic history.

## Desired State And Materialization

`ActorDesiredState` is durable logical intent. It contains actor ID, definition
identity, exact fingerprint, desired presence, world, position, rotation,
actor revision, repository revision and `EntityBindingId`.

`ActorMaterializationState` is runtime-only. Planned statuses:

- `MATERIALIZED`;
- `DORMANT_CHUNK_UNLOADED`;
- `PENDING_SPAWN`;
- `PENDING_MOVE`;
- `PENDING_REMOVAL`;
- `BLOCKED_DEFINITION_MISSING`;
- `BLOCKED_DEFINITION_INCOMPATIBLE`;
- `BLOCKED_WORLD_MISSING`;
- `BLOCKED_CORRUPT_RECORD`;
- `PLATFORM_FAILURE`;
- `HOST_STOPPED`.

The public read model exposes desired presence, materialization status,
definition compatibility, target world/position/rotation, optional current
materialized position, actor revision, repository revision and blocked reason.

## EntityBindingId

Introduce internal opaque immutable `EntityBindingId`.

- Generated before platform entity creation.
- Persisted in actor record.
- Copied to `EchoNpcEntity` NBT and synced linkage.
- Not exposed as logical actor identity.
- Not derived from display name, position or hash code.
- Rotated when duplicate/conflicting physical entities are reconciled.

Validation always compares durable `ActorId + EntityBindingId` against entity
`ActorId + EntityBindingId`.

## Save-Before Ordering

Spawn:

1. Validate request, world and current definition.
2. Generate `EntityBindingId`.
3. Save ACTIVE desired state through repository.
4. Apply repository result on server thread.
5. Materialize only after save success and only if target chunk is loaded.

Teleport:

1. Load current durable state.
2. Save updated target and increment actor revision with expected repository
   revision.
3. Apply success on server thread.
4. Move existing entity only after save, or leave actor dormant when chunk is not
   loaded.

Despawn:

1. Save ABSENT/tombstone first.
2. Apply success on server thread.
3. Remove current and stale loaded entities.
4. Final cleanup only when safe; a valid ABSENT primary must prevent ACTIVE
   backup resurrection.

## Repository Port

Define narrow explicit results in `npc-core`:

- load one actor: `FOUND`, `NOT_FOUND`, `CORRUPT`, `UNSUPPORTED_SCHEMA`,
  `FAILED`;
- load all bounded records;
- create checkpoint: `CREATED`, `REVISION_CONFLICT`, `FAILED`;
- update checkpoint: `UPDATED`, `REVISION_CONFLICT`, `FAILED`;
- write absence/tombstone: `TOMBSTONED`, `REVISION_CONFLICT`, `FAILED`;
- finalize record removal: `REMOVED`, `NOT_FOUND`, `REVISION_CONFLICT`,
  `FAILED`;
- optional bounded health view.

No `null`, boolean-only conditions, last-write-wins, Path/File/JSON/Gson in the
core port, or unchecked exceptions for expected repository outcomes.

## Atomic Write Policy

Repository save flow:

1. Map domain checkpoint to Schema V1 DTO.
2. Validate DTO.
3. Encode deterministic UTF-8 JSON.
4. Write sibling temp file.
5. Flush and close.
6. Decode generated content when practical.
7. Preserve at most one previous backup.
8. Atomic replace when supported.
9. Document fallback move when atomic replace is unsupported.
10. Remove stale temp after success.

Keep at most primary, one backup, one temp, and one bounded removal marker when
needed. Temp is never authoritative.

## Startup Recovery

Forge host lifecycle:

```text
STARTING -> LOADING_ACTORS -> RECONCILING -> ACTIVE
                                      \-> DEGRADED
```

Startup loads all bounded actor records from the world-specific root on the I/O
worker, applies results on the server thread, validates each record independently,
resolves exact definition ID and fingerprint, builds desired registry, marks
blocked/corrupt actors, and reconciles only currently loaded chunks.

Valid unrelated actors must recover even when one actor record is corrupt.

## Definition Compatibility

Recovery requires matching `NpcDefinitionId` and exact
`NpcDefinitionFingerprint`.

- Missing definition: actor is blocked, record preserved, no entity created.
- Fingerprint mismatch: actor is blocked, record preserved, no migration or
  closest-match fallback.
- Compatible definition after `/reload`: retry may unblock without duplicate
  entity creation.

## Chunk And Entity Policy

- No forced chunk tickets in N2.
- ACTIVE actor in loaded target chunk must have exactly one entity.
- ACTIVE actor in unloaded target chunk becomes dormant.
- Chunk unload never deletes actor state.
- Entity load validates actor ID, binding ID, desired presence, world and
  fingerprint before adoption.
- Orphan, stale-binding, malformed and duplicate entities are removed.
- Duplicate exact bindings are resolved deterministically by removing conflicts,
  rotating binding ID durably and materializing one clean entity.

## Bounded I/O Scheduler

Forge host owns one serial worker per server host.

- Bounded queue capacity: `64`.
- Named thread: `EchoNpcPersistence-<serverHash>`.
- No Minecraft APIs on worker.
- No actor state mutation on worker.
- Results are drained/applied on server thread.
- Queue saturation rejects mutation explicitly and emits bounded diagnostic.
- Shutdown rejects new mutations, flushes accepted work with finite timeout
  `5s`, reports timeout and never calls `System.exit`.

## Public API Evolution

Mutating N1 methods remain as compatibility entry points but return immediate
submission results:

- accepted operation: status `QUEUED`, stable `ActorMutationId`, actor ID and
  message;
- rejected operation: status `REJECTED` plus reason;
- terminal status is queryable through small operation read model.

Read-only methods remain synchronous: `query`, `listActors`, `health`.

`NpcResults` and existing public records will be extended in-place because the
service package is already at the seven-file limit.

Terminal operation records are retained in memory with bounded count `256`.

## Commands

Preserve N1 commands and update output to Russian durable operation wording:

- `/echo_npc health`
- `/echo_npc definitions`
- `/echo_npc actors`
- `/echo_npc actor <actorId>`
- `/echo_npc spawn <actorId> <definitionId> [pos]`
- `/echo_npc teleport <actorId> <pos>`
- `/echo_npc despawn <actorId>`
- `/echo_npc diagnostics [limit]`

Add:

- `/echo_npc operations`
- `/echo_npc operation <operationId>`
- `/echo_npc blocked`
- `/echo_npc recovery retry <actorId>`
- `/echo_npc persistence status`

No raw paths, raw JSON, entity dumps, repository deletion or UUID reassignment.

## Diagnostics

Use existing Forge/Minecraft logging and `InMemoryDiagnosticBuffer`.

Add N2 diagnostic codes for root init, queued/saved/failed records, queue
saturation, recovery, dormant actors, materialization, adoption, reconstruction,
blocked definitions/worlds/corruption, backup fallback, orphan/stale/duplicate
entities, binding rotation, durable teleport, tombstone and shutdown timeout.

Routine successful ticks must not log.

## Tests

Plain JVM tests:

- schema mapping/validation/determinism;
- repository create/update/revision/temp/backup/corruption/tombstone/removal;
- recovery planning compatibility/world/chunk/entity cases;
- mutation ordering and queue saturation;
- world root isolation.

Orchestration tests:

- fake repository and fake platform prove save-before-spawn/teleport/despawn;
- crash-window recovery plans;
- duplicate/adoption/stale/orphan handling;
- shutdown and out-of-order result handling.

Forge GameTests:

- keep all N1 required markers green;
- add N2 markers for durable save before materialization, host reconstruction,
  UUID may differ, one entity, position/rotation/display/skin recovery,
  teleport/despawn reconstruction, crash windows, missing/incompatible definition,
  chunk dormancy/reload, orphan/stale/duplicate entities, backup fallback, corrupt
  block isolation, world isolation and invalid reload preservation.

No wall-clock sleeps; use deterministic tick drains and operation polling.

## Packaging

Add `npc-persistence` to:

- `settings.gradle`;
- Forge `embeddedNpcProjects`;
- Forge dependencies;
- production JAR required entries;
- architecture tests;
- source-shape and forbidden scans.

Do not bundle duplicate Gson. Prefer a small deterministic schema-specific codec
inside `npc-persistence` unless actual runtime dependencies make a better option
safe.

## Documentation

Update Russian docs:

- README, PLANS, PROGRESS;
- architecture/dependency/project/portability docs;
- NPC persistence, recovery, entity binding, chunk lifecycle and corruption docs;
- Forge lifecycle/bridge/testing/packaging docs;
- Testing and Definition of Done.

Add ADRs for:

- world-scoped actor persistence;
- durable logical state as authority over entity NBT;
- `EntityBindingId`;
- save-before-materialization;
- dormant actors without forced chunk loading;
- exact fingerprint recovery;
- actor file repository and atomic writes.

## Risks

- Forge entity/chunk lifecycle APIs must be verified against actual 1.20.1
  mappings before final hook names are chosen.
- Public API compatibility pressure conflicts with async mutation semantics.
- Full manual client/server restart acceptance needs EULA/user interaction and
  cannot be claimed if not actually performed.
- The workspace is not a git repo, so diff-based verification is unavailable.

## Rollback

Rollback is project-local: remove `npc-persistence`, revert module wiring,
return `ActorRuntime`, Forge host, entity NBT and commands to N1 behavior, then
rerun N1 automated checks. No commits, branches, tags or pushes are created.

## Explicit Non-Goals

N2 does not implement animation, GeckoLib, Blockbench playback, navigation,
pathfinding, schedules, behavior trees, AI goals, combat, inventory, equipment,
wardrobe, dialogue initiation, Echo Dialogue integration, Echo Story Engine
integration, Story-Dialogue Adapter, vehicles, cinematics, remote skins, HTTP,
database backends, event sourcing, Fabric, NeoForge, Kotlin or mixins.
