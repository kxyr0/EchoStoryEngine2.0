# 0011 Story Source Pipeline Boundary

## Status

Accepted.

## Context

M1 proves deterministic execution from a programmatic story definition. M2 needs an authored source pipeline without
letting raw JSON, parser DTOs, or partially validated data reach the runtime.

## Decision

Add one Java-only module, `story-definition`, that owns source input DTOs, strict JSON parsing, structural validation,
semantic validation, and compilation into `story-core`'s immutable `StoryDefinition` model.

Use strict JSON as the first production source adapter. Java 17 has no standard JSON parser, so `story-definition`
uses Gson as an implementation-scoped adapter dependency. Gson remains forbidden in `story-contracts`, `story-core`,
runtime state, and compiled story domain types.

Keep `StoryDefinition` as the single runtime-compatible compiled story representation. Source metadata and validation
results remain outside runtime execution state.

## Alternatives

- Put parsing in `story-core`. Rejected because it would couple runtime to source format and parser dependencies.
- Add separate parser, validator, compiler, JSON, and IR modules. Rejected because M2 needs one cohesive boundary.
- Hand-roll a JSON parser. Rejected because strict JSON parsing is better delegated to a focused library while keeping
  the dependency scoped to the source adapter module.
- Create a second compiled story model mirroring `StoryDefinition`. Rejected because duplicate representations would
  create unclear ownership and drift.

## Consequences

`story-definition` may depend on `story-core` and `story-contracts`. `story-core` must not depend on
`story-definition`. M4 can add a Forge dependency on `story-definition` when production resource loading exists.

Source validation can evolve without rewriting the deterministic runtime. Replacing JSON later requires changing the
source adapter, not the compiled runtime model.

## Enforcement

Gradle dependencies, ArchUnit rules, source pipeline tests, source-shape checks, and explicit repository scans.

## Links

- `docs/plans/0004-m2-story-source-pipeline.md`
- `docs/architecture/DEPENDENCY_RULES.md`
- `docs/architecture/PROJECT_MAP.md`
