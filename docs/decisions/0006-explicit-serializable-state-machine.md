# 0006 Explicit Serializable State Machine

## Status

Accepted.

## Context

Story execution must survive inspection, persistence, replay, and future migration.

## Decision

Represent runtime progress as data:

```text
current state + input -> new state + commands + diagnostics
```

Do not store execution position in Java stacks, callbacks, futures, lambdas, coroutine continuations, Minecraft
entities, or third-party mod objects.

## Alternatives

- Callback-driven runtime. Rejected because it is hard to serialize and recover.
- Thread-per-execution runtime. Rejected because it is not server-authoritative or scalable.

## Consequences

Current scaffold uses `StoryExecutionState`, `StoryRuntimeInput`, and `StoryRuntimeResult` as the executable boundary.

## Enforcement

Unit tests for the use case and architecture rules that keep core platform-free.

## Links

- `docs/architecture/ARCHITECTURE.md`
- `story-core/src/test/java/io/github/kxyr0/echostoryengine/story/core/application/StoryRuntimeUseCaseTest.java`
