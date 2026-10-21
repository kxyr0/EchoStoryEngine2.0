# 0008 Processing Safety Limits

## Status

Accepted.

## Context

Malformed or hostile input must not cause unbounded memory use, unbounded diagnostics, or infinite parsing.

## Decision

Each parse request carries immutable `ProcessingLimits`, with documented defaults.

## Alternatives

- No limits.
- Global mutable parser settings.

## Consequences

Limit failures return structured diagnostics and stop safely when the exceeded limit is fatal.

## Enforcement

Safety tests cover source, token, diagnostic, nesting, member, statement, argument, array, and string limits.

## Related Documents

- `docs/language/SAFETY.md`
- `docs/development/TESTING.md`
