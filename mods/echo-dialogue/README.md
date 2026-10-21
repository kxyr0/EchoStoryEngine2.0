# Echo Dialogue

Echo Dialogue — самостоятельный Forge 1.20.1 мод для диалогов D1-D6.

Проверяемый поток:

`.dialogue.es` -> EchoScript parser -> semantic binding `DialogueResource` -> immutable compiled dialogue graph с optional speaker visual refs -> durable session checkpoint -> server-authoritative session -> dynamic speaker presentation provider boundary -> network sync -> centered client overlay -> client resource visual resolution -> validated advance/choice -> durable terminal outcome.

Echo Dialogue не зависит от Echo Story Engine. EchoScript остаётся syntax-only dependency через Maven coordinates:

- `io.github.kxyr0.echoscript:echo-script-api:0.5.0-SNAPSHOT`
- `io.github.kxyr0.echoscript:echo-script-language:0.5.0-SNAPSHOT`

Лицензия пока не выбрана; решение по лицензии явно не закрыто.

## Модули

- `dialogue-contracts`
- `dialogue-presentation-api`
- `dialogue-core`
- `dialogue-persistence`
- `dialogue-echoscript-adapter`
- `dialogue-forge-api-1.20.1`
- `dialogue-forge-1.20.1`

## Основные проверки

```powershell
.\gradlew.bat projects --console=plain
.\gradlew.bat test --console=plain
.\gradlew.bat architectureTest --console=plain
.\gradlew.bat sourceShapeCheck --console=plain
.\gradlew.bat checkWhitespace --console=plain
.\gradlew.bat :dialogue-forge-1.20.1:verifyProductionJarContents --console=plain
.\gradlew.bat verifyPresentationProviderIntegrationArtifacts --console=plain
.\gradlew.bat check build --console=plain
.\gradlew.bat :dialogue-forge-1.20.1:runGameTestServer --console=plain
git diff --check
```

Final JAR:

```text
D:\JDHSeason\mods\echo-dialogue\dialogue-forge-1.20.1\build\libs\echo-dialogue-0.15.0-all.jar
```

## Приёмка

- D1 implementation complete: базовый вертикальный поток реализован.
- D1.1 acceptance hardening: проверяет actual client, dedicated server, isolated final JAR, invalid reload, lifecycle cleanup и packaging boundaries.
- D2 durable persistence/recovery: добавляет save-before-presentation, world-scoped session snapshots, recovery, offline reattach и terminal retention без зависимости от Echo Story Engine.
- D3 public API preparation: добавляет public Dialogue service lookup для внешнего adapter-а.
- D4 choices/outcomes: добавляет ordered choices, branching nodes, explicit `DialogueOutcomeId`, schema V2 и adapter output `outcome`.
- D5 speaker visuals: добавляет optional `Portrait("namespace:path")` и `SkinHead("namespace:path")` на `Alias(...)`, client-only texture resolution и text-only fallback.
- D6 dynamic speaker presentation: добавляет public API version `1.2`, `dialogue-presentation-api`, optional speaker name color, Forge registration event `EchoDialogueSpeakerPresentationRegistrationEvent`, server-scoped provider sessions, deterministic provider-ID ordering, authored fallback и отдельный provider test mod без Echo NPC dependency.
- Visual client acceptance не заменяется логами: если оператор не наблюдает UI напрямую, соответствующие пункты остаются user-verified или pending.

Звуки между репликами: [руководство Audio DSL](../../docs/runtime/AUDIO_DSL.md).
Изменения: [реестр changelog](docs/changelogs/README.md).
