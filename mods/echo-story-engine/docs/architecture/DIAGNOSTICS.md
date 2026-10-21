# Diagnostics

## Policy

Core does not depend on a logging framework. It emits `StoryDiagnostic` values through `DiagnosticSink`.
Platform adapters decide how to log or display diagnostics.

Echo Story Engine must not create subsystem log files such as `story.log`, `runtime.log`, `validation.log`,
`persistence.log`, `network.log`, or adapter-specific logs. Physical logs created by Forge, Gradle, or the launcher
are external build/runtime artifacts and must not be committed.

## Current Implementation

- Runtime diagnostic code families: `ESE-RUNTIME-*`, `ESE-ACTION-*`
- Source diagnostic code family: `ESE-SOURCE-*`
- Persistence diagnostic code family: `ESE-PERSISTENCE-*`
- Forge host/resource/action diagnostic code families: `ESE-FORGE-*`
- Subsystems: `runtime`, `source`, `persistence`
- Sink port: `DiagnosticSink`
- Forge adapter: `ForgeDiagnosticSink`
- Read model: `InMemoryDiagnosticBuffer`

The Forge adapter uses one logical logger namespace: `io.github.kxyr0.echostoryengine.story`.

M4 adds read-only operator commands under `/echo_story diagnostics`. Commands read the bounded buffer and host/catalog
health. They do not mutate state, dump raw `.es` source, full variables, snapshots, stack traces, or absolute storage
paths.

## Diagnostic Shape

Each diagnostic has:

- stable code;
- severity;
- subsystem;
- human-readable message;
- subject context for story, execution, node, external action, correlation, and source location;
- bounded key/value context;
- cause category;
- timestamp supplied by a port;
- optional operator hint represented as an empty string when absent.

## Severity Policy

- `TRACE`: detailed state-machine transitions only under explicit debug mode.
- `DEBUG`: developer context, not normal user events.
- `INFO`: startup summary and rare lifecycle events.
- `WARN`: recoverable deviation or fallback.
- `ERROR`: lost execution, broken external contract, corrupted data, or unhandled boundary failure.

## Anti-Spam Rules

Future adapters must deduplicate repeated diagnostics, rate-limit repeats, summarize suppressed repeats, avoid
log-and-rethrow, bound context values, and avoid dumping full story state or sensitive values.

## Diagnostic Read Model

Text commands and visual diagnostics must read from the same structured model. The current bounded buffer is in-memory
only and is not a second log stream.

## Source Diagnostics

Source processing emits structured diagnostics and returns them in explicit result values. It does not log directly
and does not create source-specific log files. M3.6 adapts EchoScript syntax/language diagnostics into the Story
diagnostic shape and emits Story-owned semantic binding diagnostics for the supported `StoryResource` profile.

Current source codes:

- `ESE-SOURCE-001`: malformed source syntax.
- `ESE-SOURCE-002`: unsupported source language or format version.
- `ESE-SOURCE-003`: missing required source field.
- `ESE-SOURCE-004`: invalid story, node, or action identifier.
- `ESE-SOURCE-005`: duplicate node id.
- `ESE-SOURCE-006`: start node reference does not exist.
- `ESE-SOURCE-007`: transition target does not exist.
- `ESE-SOURCE-008`: unreachable node warning.
- `ESE-SOURCE-009`: synchronous cycle.
- `ESE-SOURCE-010`: unsupported node kind.
- `ESE-SOURCE-011`: compilation invariant failure.
- `ESE-SOURCE-012`: invalid field for the node kind or argument model.
- `ESE-SOURCE-013`: no reachable finish node.
- `ESE-SOURCE-014`: unsupported resource profile.
- `ESE-SOURCE-015`: missing resource class.
- `ESE-SOURCE-016`: duplicate resource class.
- `ESE-SOURCE-017`: missing Build method.
- `ESE-SOURCE-018`: invalid Build signature.
- `ESE-SOURCE-019`: unsupported member.
- `ESE-SOURCE-020`: unknown type.
- `ESE-SOURCE-021`: unknown intrinsic.
- `ESE-SOURCE-022`: invalid argument count.
- `ESE-SOURCE-023`: unknown named argument.
- `ESE-SOURCE-024`: invalid argument type.
- `ESE-SOURCE-025`: missing required lifecycle method.

## Persistence Diagnostics

M3 persistence and recovery emit structured diagnostics through result values. Diagnostics must not include absolute
paths, full snapshots, raw JSON documents, or stack traces.

Current persistence codes:

- `ESE-PERSISTENCE-001`: checkpoint save failed.
- `ESE-PERSISTENCE-002`: checkpoint load failed.
- `ESE-PERSISTENCE-003`: checkpoint not found.
- `ESE-PERSISTENCE-004`: malformed checkpoint.
- `ESE-PERSISTENCE-005`: unsupported save schema version.
- `ESE-PERSISTENCE-006`: incompatible story definition fingerprint.
- `ESE-PERSISTENCE-007`: missing story definition.
- `ESE-PERSISTENCE-008`: missing current story node.
- `ESE-PERSISTENCE-009`: invalid pending action checkpoint.
- `ESE-PERSISTENCE-010`: optimistic revision conflict.
- `ESE-PERSISTENCE-011`: backup fallback used.
- `ESE-PERSISTENCE-012`: primary and backup checkpoints are corrupt.
- `ESE-PERSISTENCE-013`: recovery request created.
- `ESE-PERSISTENCE-014`: RUNNING checkpoint rejected during recovery.
- `ESE-PERSISTENCE-015`: external command dispatch suppressed.

## Forge Diagnostics

M4 Forge diagnostics are operator-facing Russian UTF-8 strings with stable English codes:

- `ESE-FORGE-RESOURCE-001`: story resource catalog installed.
- `ESE-FORGE-RESOURCE-002`: story resource catalog candidate rejected.
- `ESE-FORGE-RESOURCE-003`: incompatible duplicate Story ID.
- `ESE-FORGE-RESOURCE-004`: `.es` resource read failed.
- `ESE-FORGE-HOST-001`: Forge story host started.
- `ESE-FORGE-HOST-002`: Forge story host stopped.
- `ESE-FORGE-HOST-003`: Story ID not found in active catalog.
- `ESE-FORGE-PERSISTENCE-001`: persistence queue rejected work.
- `ESE-FORGE-PERSISTENCE-002`: persistence worker failed.
- `ESE-FORGE-PERSISTENCE-003`: checkpoint definition fingerprint is incompatible with selected StoryResource.
- `ESE-FORGE-PERSISTENCE-004`: external command suppressed after failed save.
- `ESE-FORGE-ACTION-001`: external command saved but no production handler is connected.
