# Progress

## ES2 class-level intent sections

2026-09-08: added syntax-only class intent sections (`Name(...) { ... }`) for high-level resources such as
background NPC schedules. Version advanced to `0.4.0-SNAPSHOT`.

## ES2 intent sections

2026-08-30: added syntax-only invocation sections (`Name(...) { ... }`) so domain binders can represent bounded
high-level blocks such as Observation `OnComplete` without a second parser. Version advanced to `0.3.0-SNAPSHOT`.

## ES2 high-level dialogue frontend

2026-08-26: added contextual typed syntax for `DialogueResource` class calls and `Node` / `start Node`
declarations while keeping `namespace` optional and the parser syntax-only. Version advanced to `0.2.0-SNAPSHOT`.

## Current Milestone

ES1: safe, deterministic, immutable, syntax-only EchoScript language frontend.

## Completed

- Initial directory inspection completed.
- Git status checked after Git metadata appeared in the working directory.
- Execution plan recorded in `PLANS.md`.
- Gradle Wrapper generated.
- Two-module Java 17 Gradle build created.
- Immutable public API added.
- Hand-written lexer and recursive-descent parser added.
- Canonical fixture added at `echo-script-language/src/test/resources/fixtures/chapter_1_home.es`.
- Lexer, parser, safety, and architecture tests added.
- Required documentation and ADRs added.
- Verification command set completed successfully.

## In Progress

- None.

## Next

- ES2: stabilize public API and publish locally.
- Story-EchoScript Adapter.
- Dialogue-EchoScript Adapter.

## Blockers

- None.

## Accepted Decisions

- EchoScript is standalone and syntax-only.
- ES1 uses Java 17, Gradle, JUnit 5, a hand-written lexer, and a hand-written recursive-descent parser.
- No license is created until the owner selects one.
- Source positions use zero-based offsets and one-based line/column values.

## Known Risks

- The V1 grammar is intentionally narrow.
- The parser is recoverable but not a formatter or lossless syntax tree.
- Parser package type count is above the preferred target but below the hard package limit.

## Intentional Technical Debt

- No semantic binding layer exists in this repository.
- No source rewriting or formatting model exists.
- None for ES1 foundation.

## Verification Status

- `.\gradlew.bat projects`: passed.
- `.\gradlew.bat test`: passed.
- `.\gradlew.bat architectureTest`: passed.
- `.\gradlew.bat check`: passed.
- `.\gradlew.bat build`: passed.
- `.\gradlew.bat checkWhitespace`: passed.
- `git diff --check`: passed.
- Runtime dependency report: `echo-script-api` has no runtime dependencies.
- Runtime dependency report: `echo-script-language` depends on `project :echo-script-api` only.
- Explicit forbidden dependency, execution technology, mutable static, generic package, and task-marker scans: clean.
