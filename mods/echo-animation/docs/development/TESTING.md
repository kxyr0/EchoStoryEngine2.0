# Тестирование

Выполненные production-команды:

```powershell
.\gradlew.bat projects --console=plain
.\gradlew.bat test --console=plain
.\gradlew.bat sourceShapeCheck checkWhitespace verifyForbiddenText --console=plain
.\gradlew.bat check --console=plain
.\gradlew.bat :animation-forge-1.20.1:runGameTestServer --console=plain
git diff --check
```

`check` включает unit tests, Checkstyle, PMD, publication metadata и production JAR content verification.
`runGameTestServer` пишет один `EANIM-GAMETEST-RUN` marker на каждый required Forge test и проверяет финальную строку
`All 9 required tests passed`.

Текущий production plain-JVM счетчик: 63 tests, 0 failures, 0 errors, 0 skipped.

Текущие production test classes:

- `IdentityContractsTest`: 4
- `CoreEvaluationTest`: 6
- `CoreRequirementMatrixTest`: 20
- `DefinitionValidationTest`: 1
- `DefinitionRequirementMatrixTest`: 13
- `EchoScriptAnimationCompilerTest`: 4
- `EchoScriptRequirementMatrixTest`: 8
- `LayerDrawPlannerTest`: 3
- `LayerDrawPlannerRequirementMatrixTest`: 4

Текущие Forge GameTests:

- `ForgeAnimationGameTests.echoAnimationCommandsExist`
- `ForgeAnimationGameTests.faceRigResourceCompilesInForgeRuntime`
- `ForgeAnimationGameTests.facePoseResourceCompilesInForgeRuntime`
- `ForgeAnimationGameTests.facialAnimationResourceCompilesInForgeRuntime`
- `ForgeAnimationGameTests.invalidCandidateRetainsCatalog`
- `ForgeAnimationGameTests.serviceLookupResolvesCatalog`
- `ForgeAnimationA2GameTests.twoSubjectsShareGraphWithIndependentStates`
- `ForgeAnimationA2GameTests.trackingLifecyclePublishesFullDeltaAndRemoval`
- `ForgeAnimationA2GameTests.invalidBodyClipRetainsKnownGoodCatalog`

Осталось для полного A0 acceptance:

- real client visual inspection of Echo NPC face pixels;
- real client visual inspection of authoring preview.

Совместная production-проверка с content pack выполняется из `D:\JDHSeason\mods\echo-story-content-pack`:

```powershell
.\gradlew.bat validateContent validateWithEchoRuntime build verifyContentJar contentReport stageStoryPack stageAcceptanceStoryPack runProductionStoryPackSmokeServer runAcceptanceStoryPackServer -PskipSiblingBuild --console=plain
```

Headless Forge server проверяет загрузку финальных JAR, resources и story/dialogue/npc vertical slice. Он не может
подтвердить client pixels; визуальный слой лица нужно смотреть в клиенте с тем же набором `mods`.
