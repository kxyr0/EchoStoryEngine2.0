# 0012 Durable Persistence And Recovery

## Status

Accepted.

## Context

M2 compiles source into immutable runtime definitions. M3 needs crash recovery for executions waiting on external
actions without dispatching an action before the WAITING checkpoint is durable.

## Decision

Use save-before-dispatch ordering: runtime computes the next state and commands, the application saves the next
checkpoint, and commands are exposed only after save success. Save failure or revision conflict suppresses commands.

Add one Java-only module, `story-persistence`, for schema V1 snapshots, strict JSON encoding/decoding, a portable
file repository, and recovery planning. The module may use Gson internally. `story-core` keeps only neutral checkpoint
and repository result contracts.

Use `CompiledStoryFingerprint`, a deterministic SHA-256 value over canonical compiled story meaning, as the M3
definition compatibility token. Restoring a snapshot requires exact fingerprint equality.

Recovery is at-least-once. A WAITING snapshot produces a recovery request with the same `ActionExecutionId`; future
adapters must treat that id as the idempotency key.

## Alternatives

- Dispatch before save. Rejected because a crash can leave an external action running without recoverable state.
- Put file/JSON persistence in `story-core`. Rejected because core must stay storage-format and filesystem agnostic.
- Use Java object serialization. Rejected because it persists implementation graphs and class names.
- Claim exactly-once recovery. Rejected because M3 has no control over external systems across process crashes.
- Add multiple persistence submodules. Rejected because M3 needs one cohesive boundary.

## Consequences

The application layer now depends on explicit repository results. Durable adapters must enforce optimistic revision
checks. The file adapter is synchronous and portable; M4 must decide where the Forge world-specific root comes from
and how blocking file work is scheduled outside the Minecraft server thread.

## Enforcement

Gradle dependency graph, ArchUnit tests, persistence lifecycle tests, corruption/failure tests, repository scans, and
documentation in `docs/architecture/PERSISTENCE.md`.

## Links

- `docs/plans/0005-m3-durable-persistence-recovery.md`
- `docs/architecture/PERSISTENCE.md`
- `docs/architecture/DEPENDENCY_RULES.md`
