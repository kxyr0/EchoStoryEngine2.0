# Code Style

## Defaults

- Java 17.
- Immutability by default.
- Constructor injection.
- Manual composition.
- No hidden global mutable state.
- No service locator.
- No runtime classpath scanning.
- No reflection-based registration without ADR.

## Size And Complexity

- Typical production type target: 40-80 logical lines.
- A production type over 120 logical lines needs responsibility review.
- A production type over 180 logical lines fails the source-shape check.
- Typical method target: 5-15 logical lines.
- Methods over 40 lines fail Checkstyle.
- Cyclomatic complexity above 10 fails Checkstyle.
- Nesting deeper than 3 fails Checkstyle.
- Public methods should use no more than 4 ordinary parameters.

## Names

Use subject-matter names. Avoid vague names such as `Manager`, `Helper`, `Utils`, `Common`, `GlobalService`,
or `Impl` unless an approved ADR documents why no clearer name exists.

## State And Errors

- Validate invariants at object creation.
- Do not accept or return `null`.
- Use defensive copies at boundaries.
- Use expected result values for expected failures.
- Use exceptions for broken invariants and unexpected infrastructure failures.
- No empty catches, catch-and-ignore, or log-and-rethrow.

## Comments

Comments explain why, constraints, or non-obvious decisions. Do not leave commented dead code.
Production TODOs must link to `PROGRESS.md`, a plan, or an issue.
