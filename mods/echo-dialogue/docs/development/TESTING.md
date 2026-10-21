# Testing D6

## Required commands

```powershell
.\gradlew.bat projects --console=plain
.\gradlew.bat test --console=plain
.\gradlew.bat architectureTest --console=plain
.\gradlew.bat sourceShapeCheck --console=plain
.\gradlew.bat checkWhitespace --console=plain
.\gradlew.bat :dialogue-forge-1.20.1:verifyProductionJarContents --console=plain
.\gradlew.bat publishDialogueIntegrationArtifacts --console=plain
.\gradlew.bat buildPresentationTestProvider --console=plain
.\gradlew.bat verifyPresentationProviderIntegrationArtifacts --console=plain
.\gradlew.bat check build --console=plain
.\gradlew.bat :dialogue-forge-1.20.1:runGameTestServer --console=plain
git diff --check
```

## Focused module tests

```powershell
.\gradlew.bat :dialogue-contracts:test --console=plain
.\gradlew.bat :dialogue-presentation-api:test --console=plain
.\gradlew.bat :dialogue-core:test --console=plain
.\gradlew.bat :dialogue-persistence:test --console=plain
.\gradlew.bat :dialogue-echoscript-adapter:test --console=plain
.\gradlew.bat :dialogue-forge-1.20.1:test --console=plain
```

Plain-JVM tests cover:

- IDs and immutable model;
- session transitions, stale/duplicate/wrong-session rejection and completion;
- second active session policy;
- UTF-8 Russian text;
- DialogueResource binding;
- syntax and semantic diagnostics with source/line/column when available;
- invalid candidate known-good retention;
- client state stale revision guard, wait lock and clear;
- host shutdown idempotency;
- formatter source location output.
- compiled fingerprint compatibility;
- schema V1 encode/decode validation;
- repository optimistic revisions;
- primary/backup/temp recovery behavior;
- terminal acknowledgement and bounded cleanup;
- backup-only loadAll recovery after interrupted replacement;
- duplicate ACTIVE audience recovery conflict;
- attached/detached/terminal read model.

GameTests cover command registration, Forge runtime compile, invalid semantic candidate retention, controlled-audience lifecycle, durable snapshot restore, incompatible fingerprint block, terminal acknowledgement and world-root isolation.

For D5 they also cover visual alias compile, invalid visual known-good retention, LINE/CHOICE visual descriptors and visual fingerprint recovery block.

For D6 they also cover provider API value bounds/versioning, deterministic provider catalog resolution, separate provider JAR registration through public Forge event, dynamic LINE/CHOICE presentation, fallback scenarios, ambiguity and provider session close.

## Smoke tests

- Normal `runServer`: validates development dedicated server lifecycle, EULA permitting.
- Isolated final-JAR server: validates built artifact outside Gradle source-set classpath.
- Isolated final-JAR client: validates built artifact client load/connect logs.
- Manual restart/reconnect: validates durable ACTIVE reattach and terminal retention in a real world.
- Real visual overlay: requires actual direct visual observation and must not be inferred from logs.
- Separate provider mod: validates compilation against `build/integration-maven` public artifacts without Maven Local as a required source.

## D5 focused checks

```powershell
.\gradlew.bat :dialogue-core:test :dialogue-echoscript-adapter:test :dialogue-forge-1.20.1:test --console=plain
.\gradlew.bat :dialogue-forge-1.20.1:compileGameTestJava --console=plain
.\gradlew.bat :dialogue-forge-1.20.1:runGameTestServer --console=plain
.\gradlew.bat :dialogue-forge-1.20.1:verifyProductionJarContents --console=plain
```

D5 manual client checks:

- `/echo_dialogue play sample:greeting` text-only regression;
- `/echo_dialogue play sample:visual_showcase` portrait/skin-head visual;
- missing-resource fallback through a temporary datapack/resource pack;
- client resource reload without revision increment;
- LINE and CHOICE restart/reconnect.

## D6 focused checks

```powershell
.\gradlew.bat :dialogue-presentation-api:test :dialogue-forge-1.20.1:test --console=plain
.\gradlew.bat publishDialogueIntegrationArtifacts --console=plain
.\gradlew.bat buildPresentationTestProvider --console=plain
.\gradlew.bat verifyPresentationProviderIntegrationArtifacts --console=plain
.\gradlew.bat :dialogue-forge-1.20.1:preparePresentationProviderClientAcceptanceMod --console=plain
.\gradlew.bat :dialogue-forge-1.20.1:runGameTestServer --console=plain
```

D6 manual client checks:

- final Echo Dialogue JAR only: authored fallback still works;
- final Echo Dialogue JAR + `echo_dialogue_presentation_test`: `echo_dialogue_test:dynamic_provider_showcase` shows dynamic LINE name/skin;
- CHOICE prompt resolves dynamic speaker while choices stay ordered;
- provider absent after restart shows authored fallback without recovery block;
- ambiguous speaker shows authored fallback and bounded diagnostic.
