# 0004 Hand-Written Lexer and Parser

## Status

Accepted.

## Context

ES1 needs a small, replaceable, deterministic frontend without generated parser runtime dependencies.

## Decision

Use a hand-written lexer and recoverable recursive-descent parser.

## Alternatives

- ANTLR.
- Parser generators.
- Regex parsing.

## Consequences

The code is explicit and testable, but grammar growth must be managed by cohesive parser classes.

## Enforcement

No parser-generator dependency is present. Source-shape gates force parser responsibility splits.

## Related Documents

- `docs/language/GRAMMAR.md`
- `docs/architecture/ARCHITECTURE_GUARDRAILS.md`
