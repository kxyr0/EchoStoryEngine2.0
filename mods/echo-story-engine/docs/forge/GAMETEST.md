# GameTest

## Авторитетная команда

Forge GameTest запускается так:

```powershell
.\gradlew.bat :story-forge-1.20.1:runGameTestServer
```

`runClient` и `/test runall` не являются авторитетной M4-проверкой. GameTest source set подключен к
`runGameTestServer`, не пакуется в client run и не попадает в production jar.

## Обязательные тесты M4

```text
ForgeHostGameTests.hostExecutesStoryThroughDurableSave
ForgeHostGameTests.hostRecreationRecoversWaitingAction
ForgeHostGameTests.invalidCandidateRetainsCatalog
ForgeHostGameTests.echoStoryCommandsExecuteOnServerDispatcher
```

Последний проверенный запуск:

```text
Echo Story Engine GameTests verified: discovered=4, executed=4, passed=4
```

## Zero-test guard

`runGameTestServer` после завершения процесса читает `story-forge-1.20.1/run/logs/latest.log` и требует:

- discovered count больше `0`;
- executed count больше `0`;
- passed count больше `0`;
- marker `ESE-GAMETEST-RUN ...` для каждого обязательного теста.

Если Forge GameTest server обнаружит `0` Echo Story Engine тестов или потеряет любой обязательный marker, Gradle task
падает.

## Покрытие

`hostExecutesStoryThroughDurableSave` доказывает host bootstrap, выполнение до `WAITING`, durable save до dispatch,
server-thread completion, `FINISHED` persistence и отсутствие duplicate dispatch.

`hostRecreationRecoversWaitingAction` доказывает пересоздание host и recovery с тем же `ActionExecutionId`.

`invalidCandidateRetainsCatalog` доказывает, что invalid catalog candidate не заменяет предыдущий accepted catalog.

`echoStoryCommandsExecuteOnServerDispatcher` доказывает регистрацию `/echo_story` в живом server dispatcher, точный набор
веток `health`, `stories`, `executions`, `execution`, `diagnostics`, выполнение read-only команд и отказ
некорректного diagnostics limit.

Forge `.es` discovery через `ForgeStoryResourceReloadListener.prepare/apply` проверяется отдельным plain JVM
integration-style тестом. Это задокументированное отличие: полный Forge reload pipeline не вызывается внутри
GameTest, но используется тот же listener/candidate compiler/catalog install path.

## Structure template

Forge 1.20.1 resolves `@GameTest(template = "empty")` для класса тестов в:

```text
gameteststructures/forgehostgametests.empty.snbt
```
