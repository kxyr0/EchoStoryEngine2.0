# Persistence And Versioning

## Policy

Persistence is a port. Domain and application code must not know whether state is stored in NBT, SavedData, JSON,
files, a database, or another backend.

The current port is `ExecutionStateRepository` in `story-core`. It stores `ExecutionCheckpoint` values and returns
explicit load/save results.

## Versioning

Persistence design must account for:

- save schema version;
- story format version;
- exact compiled story fingerprint match;
- stable node ids;
- migration chain;
- atomic write;
- recovery after interrupted write;
- backup policy;
- incompatible revision diagnostics;
- unknown fields policy;
- external action recovery.

Current version values are centralized in `gradle.properties`.

## M3 Implementation

`story-persistence` owns save schema V1, strict JSON encoding, a portable durable file repository, and recovery
planning. `story-core` owns only the repository port, checkpoint payload, and save-before-dispatch ordering.

Save-before-dispatch order:

1. load checkpoint or create a new READY execution;
2. compute the next runtime state, commands, and diagnostics;
3. save the next checkpoint with an expected previous revision;
4. expose commands only when save succeeds;
5. on save failure or revision conflict, return no commands and keep the previous durable state authoritative.

The file repository uses an explicit root directory, safe SHA-256-derived filenames, UTF-8 JSON, sibling temp file,
atomic move when supported with a replace fallback, one backup file, corrupt primary fallback to backup, and stale temp
ignore policy. It does not start background threads.

WAITING recovery validates the saved fingerprint against a caller-supplied compiled definition, checks that the
current node still exists, validates pending action type/arguments/id, keeps the execution WAITING, and returns one
recovery command with the same `ActionExecutionId`. Recovery is at-least-once; adapters must be idempotent by action
execution id.

M3.6 does not change save schema V1. EchoScript `.es` source is compiled before runtime starts, and checkpoints store
only execution state plus the compiled story fingerprint. They do not store EchoScript source text, syntax trees,
semantic binding objects, source DTO collections, or frontend-specific parser state.

READY restores without auto-starting. FINISHED and FAILED restore as terminal with no command. RUNNING checkpoints
are rejected during recovery because M1 synchronous transitions should settle before durable save.

## M4 Forge Binding

`story-forge-1.20.1` now uses `FileExecutionStateRepository` for the active server world:

```text
<world>/data/echo_story_engine/executions
```

The Forge host owns a bounded single-worker persistence queue. The server thread computes runtime transitions and
immutable checkpoint payloads, the worker performs file load/save operations, and server tick drains completion
callbacks before publishing diagnostics or dispatching abstract commands.

Queue overflow, worker failure, incompatible fingerprints, and save-before-dispatch suppression are reported through
structured diagnostics. Production action handlers are not part of M4, so saved external commands without handlers are
diagnosed rather than executed.

`VolatileExecutionStateRepository` remains as a lightweight infrastructure adapter for tests or future local-only
experiments, but it is no longer the production Forge server host repository.

## Save Schema V1

Schema V1 persists:

- `saveSchemaVersion`;
- story id and execution id;
- compiled definition fingerprint and definition revision marker;
- execution revision, phase, current node, variables, failure code;
- pending action execution id, action type, and string arguments;
- bounded duplicate-completion data as the last completed action id;
- created/updated timestamps supplied by an injected clock.

The schema does not persist Java object identity, lambdas, callbacks, threads, futures, handlers, loggers, stack
traces, Forge/Minecraft types, source parser DTOs, EchoScript AST objects, raw JSON trees, caches, or service
instances.

Unknown fields, missing required fields, unsupported schema versions, invalid identifiers, invalid phases, and invalid
timestamp metadata are rejected. Deterministic map ordering is used for saved variables and pending action arguments.

## Forbidden Coupling

- Do not use array position as persistent node identity.
- Do not bind persisted models directly to public domain classes.
- Do not add serialization annotations to domain objects.
- Do not import NBT, Gson, Codec, SavedData, filesystem, or database APIs into contracts/core.
- Do not use Java object serialization for checkpoints.
- Do not pass `Path`, `File`, JSON, Gson, NBT, or database handles through core ports.
