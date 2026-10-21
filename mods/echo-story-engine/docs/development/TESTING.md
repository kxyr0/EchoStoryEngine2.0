# Testing

## Current Test Types

- Domain/application unit tests in `story-core`.
- Structural validation, semantic validation, neutral compiler, fixture, and architecture tests in `story-definition`.
- EchoScript StoryResource adapter, binding, fingerprint, runtime integration, recovery integration, and architecture
  tests in `story-echoscript-adapter`.
- Durable persistence, strict save schema, file repository, recovery, crash-window, and architecture tests in
  `story-persistence`.
- Diagnostic buffer tests in `story-core`.
- Forge resource catalog, persistence queue, save-before-dispatch orchestration, diagnostic formatting, packaging, and
  architecture tests in `story-forge-1.20.1`.
- Forge GameTest server coverage in `story-forge-1.20.1/src/gameTest`.
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
- More Forge integration tests for reload/recovery edge cases.
- Dedicated server smoke test.
- Client classloading isolation.

Coverage reports are generated, but no global percentage gate is used as a substitute for meaningful branch coverage.

## Source Pipeline Tests

EchoScript source fixtures live under `story-echoscript-adapter/src/test/resources`. Production resources do not
contain demo stories.

Story-definition tests cover:

- structural validation success and required-field/version/id/node-kind failures;
- semantic validation success, missing targets, missing start node, unreachable-node warnings, synchronous cycles,
  and invalid action type ids;
- neutral source compilation into immutable `StoryDefinition`;
- defensive copying from source collections into compiled output;
- invalid source producing no compiled story;
- source diagnostics carrying source and node context where available.

Story-echoscript-adapter tests cover:

- valid `.es` StoryResource compilation into the runtime model;
- syntax and language diagnostics adapted from EchoScript;
- StoryResource binding failures for unsupported profiles, missing methods, invalid signatures, unsupported members,
  unknown types, unknown intrinsics, and invalid arguments;
- execution of compiled `.es` source through the deterministic runtime to `WAITING` and `FINISHED`;
- fingerprint invariance for comments, line endings, source path, and independent declaration order;
- fingerprint changes for semantic action and variable changes;
- M3 recovery from a real file repository using compiled `.es` source and rejection of incompatible fingerprints;
- architecture rules for EchoScript containment, no copied EchoScript source or jars, no JSON source frontend
  leftovers, and no DialogueResource semantics in production code.

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

## Forge Tests

M4 Forge tests cover:

- `.es` resource candidate compilation and duplicate Story ID rejection;
- invalid reload candidate retention through catalog install result;
- bounded persistence queue capacity, drain, and shutdown behavior;
- save-before-dispatch suppression when save fails;
- incompatible checkpoint fingerprint rejection before dispatch;
- Russian diagnostic command formatting and mojibake marker guard;
- production jar packaging contents through `forgeJarContainsRuntimeClasses`;
- GameTest end-to-end host execution on a real Forge GameTest server.

Dedicated server smoke is run only when `story-forge-1.20.1/run/eula.txt` already contains `eula=true`; automation
must not accept the Minecraft EULA.
