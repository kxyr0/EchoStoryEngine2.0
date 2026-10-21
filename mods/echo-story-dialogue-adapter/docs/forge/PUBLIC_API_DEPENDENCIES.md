# Public API Dependencies

Adapter собирается против build-local Maven repositories:

- `../echostoryengine/build/integration-maven`
- `../echo-dialogue/build/integration-maven`

Orchestration task:

`.\gradlew.bat buildAdapterAgainstPublishedArtifacts --console=plain`

Task выполняет:

1. `publishExtensionIntegrationArtifacts` в Echo Story Engine.
2. `publishDialogueIntegrationArtifacts` в Echo Dialogue.
3. `validatePublicArtifacts`.
4. adapter `build`.
5. `verifyAdapterJarContents`.

Absolute paths и Maven Local не являются acceptance dependency.
