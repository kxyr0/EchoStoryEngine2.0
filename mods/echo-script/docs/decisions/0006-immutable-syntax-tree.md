# 0006 Immutable Syntax Tree

## Status

Accepted.

## Context

Consumers need parse results they can inspect without parser state leakage or mutation hazards.

## Decision

Syntax is represented by Java records and sealed interfaces where useful. Collections are defensive immutable copies.

## Alternatives

- Mutable syntax nodes.
- One giant nullable node.
- Parser-state references inside nodes.

## Consequences

Consumers cannot mutate parse output. Future adapters receive stable syntax data.

## Enforcement

Unit tests assert collection immutability and public records enforce non-null invariants.

## Related Documents

- `docs/language/GRAMMAR.md`
- `docs/architecture/PROJECT_MAP.md`
