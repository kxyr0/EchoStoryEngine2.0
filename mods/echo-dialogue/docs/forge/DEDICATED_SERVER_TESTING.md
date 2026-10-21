# Dedicated Server Testing D5

## Gradle runServer smoke

Команда:

```powershell
.\gradlew.bat :dialogue-forge-1.20.1:runServer --console=plain
```

EULA rule:

- оператор не принимает Minecraft EULA автоматически.
- Если `eula.txt` отсутствует или содержит `eula=false`, server acceptance остаётся pending.
- Пользователь должен вручную изменить файл на `eula=true`.

Проверить в logs:

- Forge starts;
- `echo_dialogue` discovered;
- `EDLG-FORGE-006 Dialogue host активен`;
- `EDLG-FORGE-003 Reload диалогов принят` with D5 sample resources;
- server compiles visual IDs but does not decode PNG files;
- persistence root created under `<world-root>/data/echo_dialogue/sessions`;
- command tree registered;
- `/echo_dialogue active` distinguishes attached/detached, retained terminal, blocked and corrupt counts;
- `/reload` не ломает server;
- no `ClassNotFoundException`;
- no `NoClassDefFoundError`;
- no client-only classloading;
- no client visual resolver/renderer classloading;
- `stop` приводит к `EDLG-FORGE-007 Dialogue host остановлен; сессии очищены`;
- process exits without hang.

## GameTest server

```powershell
.\gradlew.bat :dialogue-forge-1.20.1:runGameTestServer --console=plain
```

Required marker names:

- `ForgeDialogueGameTests.echoDialogueCommandsExist`
- `ForgeDialogueGameTests.dialogueResourceCompilesInForgeRuntime`
- `ForgeDialogueGameTests.controlledAudienceSessionLifecycle`
- `ForgeDialogueGameTests.choiceSessionBranchesToExplicitOutcome`
- `ForgeDialogueGameTests.invalidCandidateRetainsCatalog`
- `ForgeDialoguePersistenceGameTests.durableSnapshotRestoresSameLine`
- `ForgeDialoguePersistenceGameTests.incompatibleFingerprintBlocksRecovery`
- `ForgeDialoguePersistenceGameTests.terminalRecordCanBeAcknowledged`
- `ForgeDialoguePersistenceGameTests.worldRootsAreIsolated`
- `ForgeDialogueVisualGameTests.visualAliasesCompile`
- `ForgeDialogueVisualGameTests.invalidVisualRetainsCatalog`
- `ForgeDialogueVisualGameTests.presentationsTransferVisualDescriptors`
- `ForgeDialogueVisualGameTests.changedVisualBlocksRecovery`
- `ForgeDialoguePresentationProviderGameTests.providerJarRegistersThroughPublicEvent`
- `ForgeDialoguePresentationProviderGameTests.dynamicLinePresentationUsesProvider`
- `ForgeDialoguePresentationProviderGameTests.dynamicChoicePresentationKeepsChoices`
- `ForgeDialoguePresentationProviderGameTests.providerFallbackScenariosUseAuthoredPresentation`
- `ForgeDialoguePresentationProviderGameTests.ambiguousProvidersUseAuthoredFallback`
- `ForgeDialoguePresentationProviderGameTests.providerSessionsCloseWithCatalog`

Gradle post-check fails the task on zero discovery/execution or missing marker.

D6 `runGameTestServer` автоматически собирает отдельный provider mod и кладёт его в `dialogue-forge-1.20.1/run/mods`.

## Isolated final-JAR server

See `docs/forge/DISTRIBUTION_PACKAGING.md`.

Runtime `mods` directory must contain only:

```text
echo-dialogue-0.1.0-SNAPSHOT-all.jar
```

Do not add separate module jars, EchoScript jars, source-set outputs, test classes or Echo Story Engine.
