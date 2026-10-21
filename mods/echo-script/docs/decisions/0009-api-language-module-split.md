# 0009 API and Language Module Split

## Status

Accepted.

## Context

Consumers need stable contracts while the lexer and parser remain replaceable.

## Decision

Use two Gradle modules: `echo-script-api` and `echo-script-language`.

## Alternatives

- One module.
- Many small lexer/parser/AST modules.

## Consequences

The API has no project dependencies. The language implementation depends only on the API.

## Enforcement

Gradle build files and architecture tests verify the dependency direction.

## Related Documents

- `docs/architecture/DEPENDENCY_RULES.md`
- `docs/architecture/PROJECT_MAP.md`
