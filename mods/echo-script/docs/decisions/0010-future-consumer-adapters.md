# 0010 Future Consumer Adapters

## Status

Accepted.

## Context

Story and Dialogue systems may later consume EchoScript syntax but own their semantics.

## Decision

Future Story-EchoScript and Dialogue-EchoScript adapters are external consumer boundaries, not ES1 implementation work.

## Alternatives

- Add Story binding now.
- Add Dialogue binding now.
- Add a speculative semantic plugin system.

## Consequences

ES1 exposes syntax and diagnostics only. No adapter, registry, or semantic plugin placeholder exists in this repository.

## Enforcement

Architecture tests scan production source for consumer-domain dependencies and plugin/execution technology.

## Related Documents

- `docs/architecture/ARCHITECTURE.md`
- `docs/plans/README.md`
