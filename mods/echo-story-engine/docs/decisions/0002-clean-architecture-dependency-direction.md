# 0002 Clean Architecture And Dependency Direction

## Status

Accepted.

## Context

The engine needs a public contract layer, a pure runtime/application layer, and a replaceable platform adapter.

## Decision

Use Clean Architecture and Ports and Adapters with this Gradle direction:

```text
story-contracts <- story-core <- story-forge-1.20.1
```

Core layers follow domain -> application -> ports, with infrastructure only in the platform module.

## Alternatives

- Single Forge module. Rejected because it hides platform coupling.
- More modules now. Rejected because no current use case justifies extra module boundaries.

## Consequences

The Forge module can be replaced without rewriting contracts/core. Cross-module changes need an execution plan.

## Enforcement

ArchUnit layered rules, Gradle project dependencies, and `docs/architecture/DEPENDENCY_RULES.md`.

## Links

- `docs/architecture/ARCHITECTURE.md`
- `docs/architecture/DEPENDENCY_RULES.md`
