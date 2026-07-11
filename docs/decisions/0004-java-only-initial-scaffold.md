# 0004 Java-only Initial Scaffold

## Status

Accepted.

## Context

Forge mods must remain friendly to Java callers and build tooling. Adding Kotlin or another language now would expand
the build surface before the API is stable.

## Decision

Use Java only for the initial scaffold. Do not add Kotlin, Lombok, DI frameworks, or additional language loaders.
Kotlin can be reconsidered only through a future ADR.

## Alternatives

- Add Kotlin for core ergonomics. Rejected until Java interoperability and Forge build impact are verified.
- Add Lombok. Rejected because records and explicit constructors are sufficient here.

## Consequences

Public contracts remain Java-friendly. Boilerplate is accepted where it preserves explicit invariants.

## Enforcement

Gradle plugin list and dependency review.

## Links

- `build.gradle`
- `docs/architecture/PORTABILITY.md`
