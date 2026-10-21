# D1.1 execution plan

## Наблюдаемое состояние D1

- Сохранены четыре production-модуля: `dialogue-contracts`, `dialogue-core`, `dialogue-echoscript-adapter`, `dialogue-forge-1.20.1`.
- `.\gradlew.bat projects --console=plain` подтвердил модульный граф.
- `dialogue-core` не импортирует Forge, Minecraft или EchoScript syntax types.
- EchoScript подключён через Maven coordinates `io.github.kxyr0.echoscript:*:0.1.0-SNAPSHOT` и встроен в final Forge JAR zip-merge стратегией.
- Текущий final JAR содержит contracts/core/adapter/Forge production classes, EchoScript API/language classes, `META-INF/mods.toml` и `sample_greeting.dialogue.es`.
- Текущий GameTest guard проверяет non-zero discovery/execution и маркеры четырёх обязательных D1 GameTests.
- Пользователь ранее подтвердил real client overlay flow для `sample:greeting`: overlay появился, строки были просмотрены, диалог закрылся.

## Acceptance gaps

- `docs/development/DEFINITION_OF_DONE.md`, `docs/dialogue/PRESENTATION.md`, `docs/dialogue/NETWORKING.md`, `docs/development/TESTING.md`, `docs/architecture/README.md` и `docs/forge/DEDICATED_SERVER_TESTING.md` содержат устаревшие формулировки про невыполненные D1-пункты.
- `docs/architecture/PROJECT_MAP.md` отсутствует.
- `docs/forge/DISTRIBUTION_PACKAGING.md` и `docs/forge/ISOLATED_CLIENT_TESTING.md` отсутствуют.
- Syntax diagnostics из EchoScript пока не отображают source subject, line и column через Echo Dialogue formatter, хотя EchoScript API предоставляет `SourceSpan`.
- Client state принимает stale open/update для уже открытой session и не имеет явного clear на client disconnect.
- Forge host lifecycle не пишет явные host active/stopped diagnostics и не имеет idempotent shutdown API.
- Нет автоматической Gradle-задачи artifact content verification; JAR проверялся ручной инспекцией.

## Client test strategy

- Добавить plain-JVM тесты для `ClientDialogueState`: first open, bounded wait lock, stale open ignored, new revision unlocks, stale close ignored, disconnect clear.
- Добавить client logout handler, который очищает overlay state.
- Реальный visual smoke не утверждать заново без фактического наблюдения. В документации зафиксировать уже выполненное пользовательское подтверждение и отдельный manual checklist для resize/disconnect/isolated-client visual checks.

## Dedicated-server test strategy

- Повторить `:dialogue-forge-1.20.1:runServer` smoke после правок, не принимая EULA автоматически.
- Проверить logs на mod discovery, `EDLG-FORGE-003`, отсутствие `ClassNotFoundException`, `NoClassDefFoundError`, split-package и client-only classloading.
- Если stdin/консоль не позволяют выполнить команды интерактивно, указать это явно и не помечать эти command checks как пройденные.

## Invalid-reload strategy

- Усилить automated coverage сценарием: valid catalog applied, invalid unresolved alias candidate rejected, revision unchanged, previous `sample:greeting` remains visible, diagnostics contain stable code/category, then fixed candidate applies with incremented revision.
- В документации описать manual datapack procedure через `/reload`.

## Isolated-JAR strategy

- Добавить `verifyProductionJarContents` в Forge-модуль.
- Проверять наличие `mods.toml`, Forge entrypoint, Dialogue production classes, EchoScript API/language classes, client overlay/network classes and sample resource.
- Проверять отсутствие JUnit, ArchUnit, PMD, Checkstyle, GameTest classes, Minecraft/Forge classes, `module-info.class`, development docs/run/build metadata and Story Engine classes.
- Повторить isolated final-JAR server smoke после сборки.
- Isolated final-JAR client: автоматизировать load/connect logs, visual interaction пометить как user-pending, если не будет фактического наблюдения.

## Artifact inspection strategy

- `verifyProductionJarContents` должен зависеть от `jar`.
- `check` должен зависеть от `verifyProductionJarContents`.
- Финальный отчёт должен включать exact JAR path, size, ключевые included/excluded entries и exit code задачи.

## Lifecycle and side-safety risks

- `ForgeDialogueCompositionRoot` хранит только server-scoped host field, но shutdown сейчас не idempotent на уровне host.
- `ForgeDialogueHost` не хранит `MinecraftServer`, но должен явно закрывать active sessions on shutdown.
- Client overlay state живёт только в client package, но должен очищаться на disconnect.
- Dedicated server must not load `ClientDialogueClient`; это проверяется runServer/isolated server logs и ArchUnit/source scans.

## Networking risks

- `DialogueAdvanceMessage` содержит `sessionId` и `expectedRevision`.
- Server получает sender из Forge context и валидирует active session через audience id.
- Риск D1.1: malformed или stale client packets не должны продвигать state; это покрывается core tests, GameTest lifecycle и host/network smoke.
- Client wait lock должен разблокироваться только на новой revision или close.

## UI/input risks

- Holding Space и repeated clicks блокируются `waitingForServer`, но stale duplicate open мог преждевременно снять lock.
- Resize/GUI-scale остаются manual visual acceptance, если оператор не наблюдает клиент.
- Overlay не должен писать dialogue text в vanilla chat; manual checklist остаётся обязательным для визуальной части.

## Production files likely to change

- `dialogue-contracts/src/main/java/io/github/kxyr0/echodialogue/api/diagnostic/DiagnosticSubject.java`
- `dialogue-echoscript-adapter/src/main/java/io/github/kxyr0/echodialogue/echoscript/diagnostic/EchoScriptDiagnosticAdapter.java`
- `dialogue-forge-1.20.1/src/main/java/io/github/kxyr0/echodialogue/forge/diagnostic/ForgeDiagnosticFormatter.java`
- `dialogue-forge-1.20.1/src/main/java/io/github/kxyr0/echodialogue/forge/client/ClientDialogueState.java`
- `dialogue-forge-1.20.1/src/main/java/io/github/kxyr0/echodialogue/forge/client/ClientDialogueClient.java`
- `dialogue-forge-1.20.1/src/main/java/io/github/kxyr0/echodialogue/forge/composition/ForgeDialogueCompositionRoot.java`
- `dialogue-forge-1.20.1/src/main/java/io/github/kxyr0/echodialogue/forge/host/ForgeDialogueHost.java`
- `dialogue-forge-1.20.1/build.gradle`

## Tests to add

- `DiagnosticSubject` source location formatting test.
- EchoScript syntax diagnostic source/line/column test.
- Client state stale revision, wait lock and clear test.
- Invalid candidate retention test using a deterministic semantic error, not only duplicate IDs.
- Optional GameTest for invalid semantic source retention if package count and runtime cost remain small.

## Commands to run

- `.\gradlew.bat projects --console=plain`
- `.\gradlew.bat test --console=plain`
- `.\gradlew.bat architectureTest --console=plain`
- `.\gradlew.bat sourceShapeCheck --console=plain`
- `.\gradlew.bat checkWhitespace --console=plain`
- `.\gradlew.bat :dialogue-forge-1.20.1:verifyProductionJarContents --console=plain`
- `.\gradlew.bat check build --console=plain`
- `.\gradlew.bat :dialogue-forge-1.20.1:runGameTestServer --console=plain`
- `.\gradlew.bat :dialogue-forge-1.20.1:runServer --console=plain`
- `git diff --check`
- Explicit scans from D1.1 section 17.

## Rollback plan

- Не создавать commit, branch, tag или push.
- Если production hardening ломает runtime, откатить только собственные D1.1-правки через targeted patch, не трогая существующие D1-изменения пользователя.
- Если isolated smoke невозможен из-за внешнего launcher/EULA/stdin ограничения, оставить кодовые проверки и документацию, но финальный статус пометить `acceptance pending`.

## Explicit non-goals

- Нет Echo Story Engine integration.
- Нет persistence across restart.
- Нет choices, branching, outcomes, NPC lookup, portraits, animation, voice, sound, typewriter, camera, movement lock, generic UI, editor, hot reload или Kotlin.
- D2 начинать нельзя до честного завершения D1.1 acceptance.
