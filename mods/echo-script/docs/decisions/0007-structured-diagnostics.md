# 0007 Structured Diagnostics Without Logging

## Status

Accepted.

## Context

Consumers need diagnostics they can adapt to their own UI or systems without log side effects.

## Decision

EchoScript returns immutable `LanguageDiagnostic` records with stable codes, severity, source identity, spans, messages,
bounded context, and optional hints.

## Alternatives

- Throw expected parse exceptions.
- Log directly.
- Reuse Story or Dialogue diagnostics.

## Consequences

Malformed source is reported as data. The library creates no log files.

## Enforcement

Architecture tests scan for logging and domain diagnostic dependencies.

## Related Documents

- `docs/language/DIAGNOSTICS.md`
- `docs/architecture/DEPENDENCY_RULES.md`
