# Testing

## Current Test Types

- Domain/application unit tests in `story-core`.
- Parser, structural validation, semantic validation, compiler, pipeline, fixture, and architecture tests in
  `story-definition`.
- Durable persistence, strict save schema, file repository, recovery, crash-window, and architecture tests in
  `story-persistence`.
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

- Persistence migration tests.
- Forge integration tests.
- Minecraft GameTests for critical platform scenarios.
- Dedicated server smoke test.
- Client classloading isolation.

Coverage reports are generated, but no global percentage gate is used as a substitute for meaningful branch coverage.

## Source Pipeline Tests

Source fixtures live under `story-definition/src/test/resources`. Production resources do not contain demo stories.

M2 tests cover:

- valid strict JSON parsing;
- malformed JSON syntax diagnostics;
- structural validation success and required-field/version/id/node-kind failures;
- semantic validation success, missing targets, missing start node, unreachable-node warnings, synchronous cycles,
  and invalid action type ids;
- compilation into immutable `StoryDefinition`;
- execution of compiled source through the deterministic runtime to `WAITING` and `FINISHED`;
- deterministic compilation for identical source and reordered JSON fields;
- defensive copying from source collections into compiled output;
- invalid source producing no compiled story;
- source diagnostics carrying source and node context where available.

## Persistence Tests

M3 tests cover:

- save/load round-trip for READY, WAITING, FINISHED, and bounded duplicate-completion data;
- save-before-dispatch suppression through core application tests;
- optimistic revision success and conflict;
- primary, backup, and temp file behavior;
- safe filename derivation and created/updated metadata;
- malformed JSON, unsupported schema, unknown/missing fields, invalid ids, invalid phase, corrupt primary/backup;
- recovery of WAITING with the same `ActionExecutionId`;
- READY restore without auto-start, terminal restore without commands, RUNNING rejection;
- missing definition, incompatible fingerprint, missing current node, and invalid pending action rejection;
- crash windows around WAITING save, dispatch before completion save, recovered completion, and duplicate completion;
- architecture rules for module direction, JSON isolation, no file/path types through core port, and no Java object
  serialization APIs.
