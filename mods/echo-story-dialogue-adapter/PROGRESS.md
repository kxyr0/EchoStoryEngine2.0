# Прогресс Echo Story Dialogue Adapter

## D3

Статус: production adapter собран, прошёл JVM/architecture/build-local checks, sibling GameTest checks, adapter Forge
GameTest с terminal recovery и isolated three-mod server smoke.
Полный игровой acceptance ещё не закрыт, потому что не запускались isolated client, acceptance-content server и полный
runtime restart acceptance.

Выполнено:

- Создан независимый Gradle/Forge 1.20.1 проект `echo-story-dialogue-adapter`.
- Реализована регистрация Story extension `echo_story_dialogue_adapter:bridge`.
- Реализован единственный action handler для `echo_dialogue:play`.
- Реализована строгая схема payload: только `dialogueId` и `audienceId`.
- Реализовано детерминированное соответствие `ActionExecutionId` -> `DialogueSessionId`.
- Реализованы start, recover, cancel и terminal observation через public Echo Dialogue API.
- Реализован build-local resolve из `../echostoryengine/build/integration-maven` и `../echo-dialogue/build/integration-maven`.
- Добавлены plain-JVM tests, architecture tests, source shape, whitespace и JAR content verification.
- Добавлена русская документация по контракту, потокам, зависимостям, тестированию и ADR.
- Исправлен `mods.toml` range для `echo_dialogue`: `[0.1.0-SNAPSHOT,)`.
- Adapter `mods.toml` использует `license="All Rights Reserved"` вместо unresolved placeholder.
- Выявлен и устранён sibling packaging conflict: Story и Dialogue production runtime используют Forge Jar-in-Jar
  EchoScript artifacts, чтобы не экспортировать duplicate root packages.

Проверки, которые уже проходили:

- `.\gradlew.bat projects --console=plain`
- `.\gradlew.bat dependencies --console=plain`
- `.\gradlew.bat test --console=plain`
- `.\gradlew.bat architectureTest --console=plain`
- `.\gradlew.bat check --console=plain`
- `.\gradlew.bat test architectureTest check build sourceShapeCheck checkWhitespace --console=plain`
- `.\gradlew.bat check build sourceShapeCheck checkWhitespace --console=plain`
- `.\gradlew.bat buildAdapterAgainstPublishedArtifacts --console=plain`
- `.\gradlew.bat runGameTestServer --console=plain`, `discovered=3`, `executed=3`, `passed=3`.
  Adapter GameTest запускал Story и Dialogue как ForgeGradle deobf runtime dependencies из sibling final JARs;
  `run/mods` после подготовки пустой, production reobf JAR туда не копируются.
  Проверяются регистрация модов/команд, public Dialogue terminal cancellation -> Story completion и recovery
  terminal Dialogue после закрытия исходного observer-а -> Story completion.
- Echo Story Engine: `.\gradlew.bat :story-forge-1.20.1:runGameTestServer --console=plain`, `discovered=4`, `executed=4`, `passed=4`.
- Echo Dialogue: `.\gradlew.bat :dialogue-forge-1.20.1:runGameTestServer --console=plain`, `discovered=8`, `executed=8`, `passed=8`.
- Adapter JAR: `build\libs\echo-story-dialogue-adapter-0.1.0-SNAPSHOT.jar`, 18 515 bytes, 20 entries, forbidden-content scan пустой.
- isolated three-mod server smoke:
  `D:\JDHSeason\mods\echostoryengine\story-forge-1.20.1\build\isolated-production-smoke\run.bat --nogui`
  reached `Done (6.415s)`, accepted adapter extension and exited with code 0 after `stop`
  when repeated on 2026-07-18.

Ограничения статуса:

- `git diff --check` не выполнен в adapter root, потому что каталог пока не является git-репозиторием.
- Isolated three-production-mod client, acceptance-content server and full runtime restart acceptance require separate Minecraft/Forge runtime execution.
- Story-side generic output prerequisite closed in Echo Story Engine plan
  `docs/plans/0009-generic-external-action-output-binding.md`: `ActionCompletion.outputs()` is forwarded into neutral
  runtime input, bound through generic `Action(..., output: "...")`, persisted as Story variables and covered by Story
  Engine checks.

## D4

Дополнено 2026-07-18:

- Completion mapping теперь отдаёт canonical output `outcome` из public `DialogueSessionView.terminalOutcomeId()`.
- Старые `dialogue.sessionId`, `dialogue.status`, `dialogue.terminalResult`, `dialogue.cancellationReason`,
  `dialogue.blockedReason` и `dialogue.failureReason` сохранены как diagnostic/compatibility outputs.
- Story-side consumption остаётся generic: `Action(..., output: "outcome")`.

Проверено:

- Echo Dialogue `publishDialogueIntegrationArtifacts` — успешно.
- Adapter `.\gradlew.bat test --console=plain` — успешно.
- Adapter `.\gradlew.bat check build sourceShapeCheck checkWhitespace --console=plain` — успешно.
- Adapter `.\gradlew.bat runGameTestServer --console=plain` — успешно, discovered=3, executed=3, passed=3.
