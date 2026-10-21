# 0010 Persistence And Versioning Boundaries

## Status

Accepted.

## Context

Story executions eventually need durable save/load, schema migration, recovery, and diagnostics. The initial scaffold
must prove the boundary without implementing production storage.

## Decision

Define `ExecutionStateRepository` as a core port. Keep save schema, story format, API, engine, network, Minecraft,
and Forge versions distinct. Use a volatile Forge adapter only to prove current state flow.

## Alternatives

- Implement SavedData/NBT immediately. Rejected because it would exceed the scaffold scope.
- Add serialization annotations to domain classes. Rejected because it couples model and storage format.

## Consequences

Durable persistence remains future work. The current adapter must not be described as production persistence.

## Enforcement

Architecture tests, docs, and review checklist.

## Links

- `docs/architecture/PERSISTENCE.md`
- `story-core/src/main/java/io/github/kxyr0/echostoryengine/story/core/port/ExecutionStateRepository.java`
- `story-forge-1.20.1/src/main/java/io/github/kxyr0/echostoryengine/story/forge/infrastructure/VolatileExecutionStateRepository.java`
