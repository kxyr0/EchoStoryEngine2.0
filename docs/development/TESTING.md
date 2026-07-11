# Testing

## Current Test Types

- Domain/application unit tests in `story-core`.
- Diagnostic buffer tests in `story-core`.
- Architecture tests in `story-forge-1.20.1`.
- Static checks through Checkstyle, PMD, and custom Gradle tasks.

## Rules

- Test behavior, not private implementation.
- Use deterministic input.
- No sleeps.
- No random UUIDs without fixed seed or explicit values.
- Do not mock domain value objects.
- Use test fakes for ports in test source sets only.
- Do not delete tests to pass the build.
- Do not change expected behavior without documenting the behavior change.

## Future Test Areas

- Validation tests.
- Port contract tests.
- Persistence migration tests.
- Forge integration tests.
- Minecraft GameTests for critical platform scenarios.
- Dedicated server smoke test.
- Client classloading isolation.

Coverage reports are generated, but no global percentage gate is used as a substitute for meaningful branch coverage.
