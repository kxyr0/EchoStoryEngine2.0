# Plans

## Milestone ES1: Language Frontend Foundation

Status: implemented and verified for the initial foundation.

## Execution Plan

1. Create a two-module Java 17 Gradle build with wrapper, dependency rules, and quality gates.
2. Define immutable public API contracts for source identity, spans, diagnostics, limits, parse request/result, and syntax.
3. Implement a deterministic hand-written lexer for V1 tokens, comments, strings, spans, and lexical diagnostics.
4. Implement a recoverable recursive-descent parser for the V1 syntax subset.
5. Add the canonical `.es` fixture and tests for lexer, parser, safety, and architecture requirements.
6. Document architecture, language grammar, diagnostics, safety, and deferred non-goals.
7. Run required verification commands and record results in `PROGRESS.md`.

## Proposed Structure

- `echo-script-api`: no project dependencies; stable public contracts and immutable syntax records.
- `echo-script-language`: depends only on `echo-script-api`; owns lexer, parser, and internal diagnostics.
- `docs`: architecture, language, development, decisions, and future planning notes.

## Limits

Initial defaults are documented in `docs/language/SAFETY.md` and enforced by `ProcessingLimits`.

## Dependencies

Runtime dependencies are limited to the API module dependency from `echo-script-language`.
Test and verification dependencies are JUnit 5, ArchUnit, Checkstyle, and PMD.

## Risks

- ES1 intentionally supports only a small syntax subset.
- No license has been selected.
- Parser package type count is 9, below the hard cap of 12 but above the preferred target of 7.

## Verification

Verified commands include `projects`, `test`, `architectureTest`, `check`, `build`, `checkWhitespace`, and
`git diff --check`.
