# 0007 Structured Diagnostics Pipeline

## Status

Accepted.

## Context

Diagnostics must serve logs, commands, and future visual tools without creating separate subsystem log files.

## Decision

Core emits `StoryDiagnostic` through `DiagnosticSink`. Forge maps diagnostics into the standard logging pipeline
using one logical logger namespace. A bounded in-memory buffer stores recent diagnostics as a read model.

## Alternatives

- Direct SLF4J logging in core. Rejected because core must remain logging-framework-free.
- Separate log files per subsystem. Rejected because it fragments diagnostics and creates log spam risk.

## Consequences

All diagnostic consumers read structured diagnostics. Future adapters must add deduplication and rate limiting before
high-frequency diagnostics are introduced.

## Enforcement

ArchUnit forbids SLF4J/Mojang logging in core. `docs/architecture/DIAGNOSTICS.md` owns diagnostic policy.

## Links

- `docs/architecture/DIAGNOSTICS.md`
- `story-core/src/main/java/io/github/kxyr0/echostoryengine/story/core/diagnostic/InMemoryDiagnosticBuffer.java`
- `story-forge-1.20.1/src/main/java/io/github/kxyr0/echostoryengine/story/forge/infrastructure/ForgeDiagnosticSink.java`
