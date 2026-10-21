# 0003 Syntax-Only Responsibility

## Status

Accepted.

## Context

Names such as `DialogueResource`, `Line`, and `NodeDef` belong to consumers, not to the language frontend.

## Decision

EchoScript parses declarations, statements, expressions, and literals as syntax only.

## Alternatives

- Add Story semantic binding.
- Add Dialogue semantic binding.
- Add type checking for consumer domains.

## Consequences

The syntax tree preserves names and calls but does not resolve or execute them.

## Enforcement

Production source scans reject consumer-domain dependencies and runtime execution technology.

## Related Documents

- `docs/language/GRAMMAR.md`
- `docs/architecture/ARCHITECTURE.md`
