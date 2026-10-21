# 0001 Story-only Scope

## Status

Accepted.

## Context

The project can easily absorb gameplay systems because it starts as a Minecraft mod. That would make the story
runtime impossible to reuse, test, or reason about.

## Decision

Echo Story Engine owns only story definition, validation, execution, persistence through ports, and diagnostics.
Concrete gameplay systems belong in adapters, integrations, or separate mods.

## Alternatives

- Put quest/NPC/gameplay features directly in core. Rejected because it violates portability and testability.
- Keep no formal scope rule. Rejected because the repository needs automated and documented boundaries from day one.

## Consequences

Feature requests that mention concrete game domains are stopped at the core boundary. Core may expose abstract action,
event, condition, value, command, and diagnostic contracts only.

## Enforcement

`docs/architecture/SCOPE.md`, `sourceShapeCheck`, and ArchUnit dependency rules.

## Links

- `docs/architecture/SCOPE.md`
- `docs/architecture/PROJECT_MAP.md`
