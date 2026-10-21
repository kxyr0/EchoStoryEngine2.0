# Прогресс Echo Dialogue

## D1

- Реализован standalone Forge 1.20.1 мод для линейного диалога.
- Добавлены `dialogue-contracts`, `dialogue-core`, `dialogue-echoscript-adapter`, `dialogue-forge-1.20.1`.
- Реализованы compiled dialogue model, server-authoritative sessions, resource reload, command surface, networking, centered client overlay и sample dialogue.

## D1.1

- Acceptance & Production Hardening завершён и принят как baseline для D2.
- Подтверждены real centered overlay, actual client input, non-zero GameTests, normal dedicated server, isolated final-JAR server/client, invalid reload known-good retention и final-JAR packaging verification.

## D2

- Добавлен production module `dialogue-persistence`.
- Реализованы portable save schema V1, JSON codec, file repository, primary/backup/temp policy, optimistic repository revisions, recovery planning, terminal retention/acknowledgement и bounded cleanup.
- В `dialogue-core` добавлены compiled-dialogue fingerprint и narrow persistence port/checkpoint contract без filesystem/JSON/Gson/Forge/Minecraft.
- Forge host переведён на save-before-presentation: open/update/close отправляются только после durable save completion на server thread.
- Player logout теперь detach, а login reattach без увеличения domain revision.
- Recovery восстанавливает compatible ACTIVE snapshots detached, удерживает terminal records, блокирует missing/incompatible/duplicate snapshots и не открывает неверный overlay.
- Final JAR содержит runtime `dialogue-persistence`; отдельный `dialogue-persistence.jar` пользователю не нужен.

## Проверено 2026-07-16

- `.\gradlew.bat projects test architectureTest sourceShapeCheck checkWhitespace :dialogue-forge-1.20.1:verifyProductionJarContents --console=plain` — успешно.
- `.\gradlew.bat check build --console=plain` — успешно.
- `.\gradlew.bat :dialogue-forge-1.20.1:runGameTestServer --console=plain` — успешно, discovered=8, executed=8, passed=8.
- Plain-JVM XML: tests=50, failures=0, errors=0, skipped=0.
- Normal `runServer` после ручного `eula=true` дошёл до `Done`, `EDLG-FORGE-003` и `EDLG-FORGE-006`; clean `stop` через Gradle PTY/pipe не подтвердился, процесс закрыт извне, Java-процессов не осталось.
- Isolated final-JAR server с единственным `echo-dialogue-0.1.0-SNAPSHOT.jar` дошёл до `Done`, записал `EDLG-FORGE-003`, `EDLG-FORGE-006`, `EDLG-FORGE-007` и завершился exit code 0.
- Final JAR: `dialogue-forge-1.20.1/build/libs/echo-dialogue-0.1.0-SNAPSHOT.jar`, size=264915, SHA-256 `27E5EBF1D689243CE78D17B9E0D68B6292563959E4B4A35E07C3314F7C59D351`, entries=204.
- Artifact scan: persistence entries=17, Gson/Minecraft/Forge/GameTest/JUnit/Story entries=0.
- `git diff --check` — успешно.

## Осталось вручную

- Visual client restart/reconnect acceptance: открыть реальный клиент, начать `sample:greeting`, перезапустить server/client, подтвердить тот же `DialogueSessionId`, revision и строку, затем завершить диалог.
- Normal Gradle `runServer` clean stop через the automation terminal остаётся не подтверждённым из-за недоставленного stdin; isolated `run.bat` clean stop подтверждён.

## Следующий milestone

D3 — Story-Dialogue Adapter using Echo Story Engine M5 extension API.

## D3 public API preparation

Дополнено 2026-07-17:

- Production runtime artifact switched to Forge Jar-in-Jar:
  `dialogue-forge-1.20.1/build/libs/echo-dialogue-0.1.0-SNAPSHOT-all.jar`.
- EchoScript is now nested under `META-INF/jarjar` instead of root-level `io/github/kxyr0/echoscript/**`, fixing the
  split-package failure when Echo Dialogue and Echo Story Engine load together.
- Реализован server-scoped provider `ForgeDialoguePublicService` внутри Forge host.
- Добавлен `DialoguePublicSessionReadModel` для pending views, durable terminal views, blocked recovery views и bounded
  terminal observers.
- `ForgeDialogueCompositionRoot` отвечает на `EchoDialogueServiceLookupEvent` только для matching active
  `MinecraftServer`.
- Client overlay anchor переведён на центр через `ClientDialogueOverlayStyle.ANCHOR_X/Y`.
- Добавлен документ `docs/forge/PUBLIC_DIALOGUE_SERVICE.md`.

Проверено после provider-реализации:

- `.\gradlew.bat :dialogue-core:compileJava :dialogue-forge-1.20.1:compileJava --console=plain` - успешно.
- `.\gradlew.bat sourceShapeCheck checkWhitespace --console=plain` - успешно.
- `.\gradlew.bat :dialogue-forge-1.20.1:test --console=plain` - успешно.
- `.\gradlew.bat :dialogue-forge-1.20.1:check --console=plain` - успешно.
- `.\gradlew.bat publishDialogueIntegrationArtifacts --console=plain` - успешно.

- Добавлен module `dialogue-forge-api-1.20.1` с public Forge service lookup event
  `EchoDialogueServiceLookupEvent`.
- В `dialogue-contracts` добавлены neutral public contracts для external adapter: session view/status, service
  start/query/cancel contracts и terminal observation contracts.
- Добавлена task `publishDialogueIntegrationArtifacts`, публикующая `dialogue-contracts` и
  `dialogue-forge-api-1.20.1` в `build/integration-maven`.
- Final Forge JAR теперь embeds public Forge API bridge; `verifyProductionJarContents` проверяет наличие bridge event.

Проверено 2026-07-17:

- `.\gradlew.bat projects :dialogue-contracts:compileJava :dialogue-forge-api-1.20.1:compileJava :dialogue-forge-1.20.1:compileJava publishDialogueIntegrationArtifacts --console=plain` — успешно.
- `.\gradlew.bat sourceShapeCheck checkWhitespace :dialogue-forge-1.20.1:verifyProductionJarContents --console=plain` — успешно.

D3 завершён:

- Изолированный three-mod adapter runtime acceptance подтверждён через Story-Dialogue Adapter GameTest runtime.
- Echo Dialogue public API bridge остаётся отдельным от Story Engine и не добавляет Story-зависимость в Echo Dialogue.

## D4 choices/outcomes

Дополнено 2026-07-18:

- Добавлены public `ChoiceId` и `DialogueOutcomeId`; default outcome: `echo_dialogue:completed`.
- Compiled dialogue model переведён с линейного списка на graph nodes: lines, ordered choices и optional terminal completion.
- EchoScript binding поддерживает `Choice("id", "text", next: "node")` и `Complete("namespace:path")`.
- Session engine стал server-authoritative для `ADVANCE` и `SELECT_CHOICE`: stale/duplicate/wrong-session requests не двигают state.
- Persistence schema V2 сохраняет `presentationKind` и terminal outcome; V1 terminal `"completed"` мигрирует в `echo_dialogue:completed`.
- Network protocol поднят до version `2`; добавлен serverbound choice packet.
- Client overlay оставлен centered/compact, убраны hint-подсказки, line presentation выводится как `[Name]: text`, choice presentation строится по тексту.
- Choice overlay теперь показывает последний line prompt сверху и numbered choices под ним; выбор доступен ПКМ и клавишами `1`..`4`.
- Добавлены 7 bundled manual choice scripts в `manual_choices`: `sample:manual_choice_1` .. `sample:manual_choice_7`.
- Public service/read model сохраняет terminal fields из durable retained view.
- Story Dialogue Adapter теперь отдаёт canonical Story output `outcome` из `DialogueOutcomeId`, сохраняя `dialogue.*` diagnostic outputs.

Дополнено 2026-07-19:

- Подключена external neutral library `io.github.kxyr0.echo:echo-ui-animations:0.1.0-SNAPSHOT`.
- Echo UI Animations добавлена в Forge dev runtime classpath и final production JAR через Jar-in-Jar.
- Client overlay sample-ит presets `PANEL_OPEN`, `PANEL_CLOSE`, `PANEL_RESIZE` и `TYPEWRITER`.
- Fade/scale in/out, typewriter reveal и smooth content resize остаются client-only presentation state и не меняют server revision.

Проверено 2026-07-18:

- `.\gradlew.bat :dialogue-core:test --console=plain` — успешно.
- `.\gradlew.bat :dialogue-echoscript-adapter:test --console=plain` — успешно.
- `.\gradlew.bat :dialogue-persistence:test --console=plain` — успешно.
- `.\gradlew.bat :dialogue-forge-1.20.1:test --console=plain` — успешно.
- `.\gradlew.bat publishDialogueIntegrationArtifacts --console=plain` — успешно.
- `.\gradlew.bat :dialogue-forge-1.20.1:compileGameTestJava --console=plain` — успешно.
- `.\gradlew.bat :dialogue-forge-1.20.1:runGameTestServer --console=plain` — успешно, discovered=9, executed=9, passed=9.
- `.\gradlew.bat test architectureTest check build sourceShapeCheck checkWhitespace --console=plain` — успешно.
- `.\gradlew.bat :dialogue-forge-1.20.1:runGameTestServer --console=plain` — повторно успешно после финального style refactor, discovered=9, executed=9, passed=9.
- `.\gradlew.bat sourceShapeCheck checkWhitespace :dialogue-forge-1.20.1:checkstyleMain --console=plain` — успешно после choice layout update.
- `.\gradlew.bat :dialogue-core:test :dialogue-forge-1.20.1:test --console=plain` — успешно после choice layout update.
- `.\gradlew.bat :dialogue-forge-1.20.1:runGameTestServer --console=plain` — успешно после bundled manual choice scripts, reload accepted=8, discovered=9, executed=9, passed=9.
- `.\gradlew.bat :dialogue-forge-1.20.1:test sourceShapeCheck checkWhitespace :dialogue-forge-1.20.1:checkstyleMain --console=plain` — успешно после animation hookup.
- `.\gradlew.bat :dialogue-forge-1.20.1:verifyProductionJarContents --console=plain` — успешно; `echo-ui-animations` найден в `META-INF/jarjar`.
- `git diff --check` — успешно.
- Adapter repository: `.\gradlew.bat test --console=plain` — успешно.

D4 завершён:

- Manual visual check подтверждён пользователем после реального client flow и перезахода.
- Compact centered line/choice overlay, animation timing, keyboard choice selection и mouse selection считаются baseline для D5.

## D5 speaker visuals

Дополнено 2026-07-19:

- Добавлены neutral `SpeakerVisualKind`, `SpeakerVisualId`, `SpeakerVisualRef` в `dialogue-core`.
- `Alias(...)` поддерживает optional `visual: Portrait("namespace:path")` и `visual: SkinHead("namespace:path")`.
- `DialoguePresentation` и `DialogueOpenMessage` переносят optional visual descriptor kind/id; network protocol поднят до `3`.
- Fingerprint включает visual presence/kind/id; Save Schema V2 не изменён и не хранит visual/cache state.
- Client-only resolver мапит visual IDs в `assets/<namespace>/textures/echo_dialogue/portraits|skins/<path>.png`, кэширует resolution bounded и очищается на client resource reload/logout.
- Renderer добавляет left visual column для resolved portrait/head и deterministic text-only fallback при missing/invalid/narrow layout.
- Добавлен public sample `sample:visual_showcase` с `sample:father` skin-head и `sample:merchant` portrait.
- Live NPC/entity speaker resolution, Mojang/GameProfile lookup, remote URLs, animation/voice/camera остаются deferred.

Проверено 2026-07-19:

- `.\gradlew.bat projects --console=plain` — успешно.
- `.\gradlew.bat dependencies --console=plain` — успешно.
- `.\gradlew.bat test architectureTest check build --console=plain` — успешно.
- `.\gradlew.bat sourceShapeCheck checkWhitespace :dialogue-core:test :dialogue-echoscript-adapter:test :dialogue-forge-1.20.1:test :dialogue-forge-1.20.1:compileGameTestJava --console=plain` — успешно.
- `.\gradlew.bat :dialogue-forge-1.20.1:runGameTestServer --console=plain` — успешно, discovered=13, executed=13, passed=13.
- `.\gradlew.bat :dialogue-forge-1.20.1:verifyProductionJarContents --console=plain` — успешно.
- `git diff --check` — успешно.
- Normal Gradle `runServer` дошёл до `Done`, `EDLG-FORGE-003` и `EDLG-FORGE-006`; clean stop через Gradle PTY не подтверждён.
- Isolated final-JAR server с единственным `echo-dialogue-0.1.0-SNAPSHOT-all.jar` дошёл до `Done`, записал `EDLG-FORGE-007` и завершился exit code 0.
- `publishDialogueIntegrationArtifacts` — успешно.
- Story Engine `publishExtensionIntegrationArtifacts` — успешно в `D:\JDHSeason\mods\echo-story-engine`.
- Story-Dialogue Adapter `test check build -PstoryEngineRoot=D:\JDHSeason\mods\echo-story-engine --console=plain` — успешно.
- Story-Dialogue Adapter `runGameTestServer -PstoryEngineRoot=D:\JDHSeason\mods\echo-story-engine --console=plain` — успешно, discovered=3, executed=3, passed=3.

Ручная acceptance D5 подтверждена пользователем 2026-07-20:

- Real client static portrait, skin-head, missing-resource fallback, client resource reload, LINE restart и CHOICE restart проверены в реальном клиенте.
- D5 считается завершённым baseline для D6.

## D6 dynamic speaker presentation provider boundary

Стартовано 2026-07-20:

- Цель D6: добавить public extension boundary, через который отдельный мод сможет динамически подменять runtime-представление `SpeakerId` перед отправкой LINE/CHOICE клиенту.
- D6 не должен добавлять зависимость на Echo NPC, Story Engine, entity lookup, GameProfile, remote skins, HTTP или persistence dynamic output.

Дополнено 2026-07-20:

- Добавлен production module `dialogue-presentation-api` с neutral public API: provider id, API version, descriptor, request, resolved presentation, resolution и provider session.
- D5 `SpeakerVisualKind`, `SpeakerVisualId`, `SpeakerVisualRef` перенесены в public `dialogue-contracts` как neutral visual contracts для authored fallback и dynamic provider output.
- Добавлен Forge public bridge event `EchoDialogueSpeakerPresentationRegistrationEvent`; внешние моды регистрируют factories, Echo Dialogue создаёт один provider session на server host и закрывает registration до runtime.
- Forge host получил immutable sorted provider catalog; LINE/CHOICE presentation перед отправкой клиенту проходит через deterministic provider-id resolution.
- Ровно один claiming provider может заменить полный display name и optional visual. No provider, unavailable, failure, exception, malformed result, stopped catalog или multiple claims используют authored fallback с bounded diagnostics.
- Dynamic provider output остаётся runtime-only: не сохраняется в persistence, не меняет compiled fingerprint и не поднимает network protocol D5 `3`.
- Добавлены read-only команды `/echo_dialogue providers` и `/echo_dialogue provider <providerId>`.
- Добавлен отдельный Forge test-provider mod `integration-tests/echo-dialogue-presentation-test-mod`, который собирается отдельным Gradle build против artifacts из `build/integration-maven`, без копирования public API classes.
- Добавлен dynamic provider showcase script `echo_dialogue_test:dynamic_provider_showcase` и provider skin-head resource `echo_dialogue_test:dynamic_father`.
- Документация D6 добавлена в `docs/dialogue/DYNAMIC_SPEAKER_PRESENTATION.md`, `docs/dialogue/PROVIDER_RESOLUTION.md`, `docs/forge/PRESENTATION_PROVIDER_REGISTRATION.md`, `docs/forge/PUBLIC_API_DEPENDENCIES.md`, `docs/forge/ISOLATED_TESTING.md` и ADR-0001..0003.

Проверено 2026-07-20:

- `.\gradlew.bat projects --console=plain` - успешно.
- `.\gradlew.bat dependencies --console=plain` - успешно.
- `.\gradlew.bat test --console=plain` - успешно.
- `.\gradlew.bat architectureTest --console=plain` - успешно.
- `.\gradlew.bat check --console=plain` - успешно; production JAR entries=252, D6 integration artifacts verified.
- `.\gradlew.bat build --console=plain` - успешно.
- `.\gradlew.bat sourceShapeCheck checkWhitespace --console=plain` - успешно.
- `.\gradlew.bat publishDialogueIntegrationArtifacts --console=plain` - успешно.
- `.\gradlew.bat buildPresentationTestProvider --console=plain` - успешно; provider JAR entries=22.
- `.\gradlew.bat verifyPresentationProviderIntegrationArtifacts --console=plain` - успешно; staged `echo-dialogue-0.1.0-SNAPSHOT-all.jar` и `echo-dialogue-presentation-test-0.1.0-SNAPSHOT.jar`.
- `.\gradlew.bat :dialogue-forge-1.20.1:runGameTestServer --console=plain` - успешно, discovered=19, executed=19, passed=19.
- `git diff --check` - успешно.

Осталось вручную для D6 acceptance:

- Real client visual acceptance: запустить client/server с staged D6 JARs, открыть `echo_dialogue_test:dynamic_provider_showcase`, проверить dynamic display name, dynamic skin head, authored fallback cases и команды provider diagnostics.
Дополнено после isolated provider smoke 2026-07-20:

- Первый изолированный server smoke с final Echo Dialogue JAR и внешним provider JAR выявил `NoClassDefFoundError` на `SpeakerPresentationProviderFactory`: API classes были в JAR, но Java module name не совпадал с Forge mod id.
- У final Echo Dialogue JAR добавлен manifest `Automatic-Module-Name: echo_dialogue`; у test-provider JAR добавлен `Automatic-Module-Name: echo_dialogue_presentation_test`.
- `verifyPresentationProviderIntegrationArtifacts` теперь проверяет не только наличие API в Echo Dialogue JAR и отсутствие API copies в provider JAR, но и runtime module names.
- После clean-пересборки final JAR не содержит stale `io/github/kxyr0/echodialogue/core/visual/**`; public visual classes остаются в `io/github/kxyr0/echodialogue/api/visual/**`.
- Восстановлен Forge `1.20.1-47.4.10` isolated server layout в `dialogue-forge-1.20.1/build/smoke/server`; `eula.txt` скопирован из ранее принятого пользователем `dialogue-forge-1.20.1/run/eula.txt`.
- Isolated final-JAR server smoke с `echo-dialogue-0.1.0-SNAPSHOT-all.jar` и `echo-dialogue-presentation-test-0.1.0-SNAPSHOT.jar` дошёл до `Done`, записал `EDLG-FORGE-003`, `EDLG-PRESENT-001`, два `EDLG-PRESENT-003`, `EDLG-PRESENT-002`, `EDLG-FORGE-006`, по `stop` записал `EDLG-FORGE-007` и завершился exit code 0.
- Лог isolated provider smoke не содержит `ClassNotFoundException`, `NoClassDefFoundError` и `LoadingFailedException` по проверенным паттернам.

Проверено после D6 packaging fix 2026-07-20:

- `.\gradlew.bat verifyPresentationProviderIntegrationArtifacts --console=plain` - успешно; manifest module names и staged JARs проверены.
- `.\gradlew.bat check build sourceShapeCheck checkWhitespace --console=plain` - успешно; production JAR entries=252, provider JAR entries=22, D6 integration artifacts verified.
- `.\gradlew.bat :dialogue-forge-1.20.1:runGameTestServer --console=plain` - успешно, discovered=19, executed=19, passed=19.
- `.\gradlew.bat projects --console=plain` - успешно; `dialogue-presentation-api` присутствует.
- `.\gradlew.bat dependencies --console=plain` - успешно; root project has no configurations.
- `.\gradlew.bat :dialogue-contracts:test :dialogue-presentation-api:test :dialogue-core:test :dialogue-persistence:test :dialogue-echoscript-adapter:test :dialogue-forge-1.20.1:test --console=plain` - успешно.
- `.\gradlew.bat :dialogue-forge-api-1.20.1:test --console=plain` - успешно; test source отсутствует, задача `NO-SOURCE`.
- `.\gradlew.bat checkWhitespace --console=plain` - успешно после обновления `PROGRESS.md`.
- `git diff --check` - успешно.

## NPC navigation command integration

- Added neutral compiled dialogue commands and public `DialogueCommandPayload` encoding for adapter-owned commands.
- `.dialogue.es` node methods now accept `Npc.Move(...)` and `Npc.Stop(...)`; `Npc.Move` pauses the session through a
  durable external wait before the next `Line`.
- Forge posts `DialogueExternalWaitDispatchEvent` only after the WAITING checkpoint has been saved, so external
  adapters dispatch after durable state exists.

Explicit D6 scans 2026-07-20:

- Production-source scan по Echo NPC/Story/ActorId/NpcService/NpcSkinId/EchoNpcEntity/GameProfile/HTTP/URL нашёл только expected `SpeakerVisualId` URL-validation и unit-test raw URL rejection; интеграции с NPC/Story/HTTP нет.
- Neutral API scan по `dialogue-presentation-api` и public visual contracts не нашёл Forge/Minecraft/Entity/GameProfile/Path/File/network/client types; expected mention `URL` находится в тексте validation error для запрета raw visual URL.
- Persistence/fingerprint scan по `ResolvedSpeakerPresentation`, `SpeakerPresentationResolution`, `SpeakerPresentationProvider`, `provider`, `dynamic` в persistence/fingerprint packages не нашёл provider output.
- Registration scan по `ServiceLoader`, reflection, classpath scanning, mutable static registry, priority, async/thread helpers не нашёл provider registry violations; совпадения `static` являются static factory/helper methods, не mutable provider storage.
- `TODO`/`FIXME`/`@Disabled` scan не нашёл активных main/test/doc debt markers, кроме исторической строки в `docs/development/D2_FINAL_REPORT.md`.
- Artifact scan: production entries=252, productionTestProvider=0, productionNpcStory=0, productionRootEchoScript=0, productionRootUiAnimations=0, productionCoreVisualStale=0, productionPublicVisual=4, productionPresentationApi=10, productionForgeApi=5.
- Artifact scan: provider entries=22, providerApiCopies=0, providerOwnClasses=3, providerData=4, providerAssets=5.
- Production package count scan: все production packages имеют не больше 7 `.java` files; `dialogue-presentation-api` содержит ровно 7 public API types, `dialogue-forge-1.20.1/forge/presentation` содержит 6 runtime types.
- Staged final JAR: `echo-dialogue-0.1.0-SNAPSHOT-all.jar`, size=427136, SHA-256 `A488BE15295C3A9A7EFD064F424F062E5413EA741DB5F8685C927C996C4925CB`, automatic module `echo_dialogue`.
- Staged provider JAR: `echo-dialogue-presentation-test-0.1.0-SNAPSHOT.jar`, size=8379, SHA-256 `83DE77A58E7F1896327184B834F592304702BC0B25D32F6B6C0149A91CC62692`, automatic module `echo_dialogue_presentation_test`.
- `docs/forge/ISOLATED_CLIENT_TESTING.md` уточнён: tracked `run-production-client.ps1` helper отсутствует, quick visual acceptance выполняется через `.\gradlew.bat :dialogue-forge-1.20.1:runClient --console=plain`, production isolated client acceptance требует реальный Forge 1.20.1-47.4.10 client profile/launcher со staged JARs и остаётся user-pending до наблюдения.
- Добавлена task `:dialogue-forge-1.20.1:preparePresentationProviderClientAcceptanceMod`, которая явно готовит внешний D6 provider JAR в `dialogue-forge-1.20.1/run/mods` перед ручной `runClient` acceptance, без зависимости от предварительного GameTest запуска.
- `.\gradlew.bat :dialogue-forge-1.20.1:preparePresentationProviderClientAcceptanceMod --console=plain` - успешно; `run/mods` содержит `echo-dialogue-presentation-test-0.1.0-SNAPSHOT.jar`, size=8379.
- `.\gradlew.bat checkWhitespace sourceShapeCheck --console=plain` - успешно после добавления acceptance task и обновления docs.
- `git diff --check` - успешно после добавления acceptance task и обновления docs.
## 0.9.0-SNAPSHOT backpack external waits

- Dialogue nodes accept `Npc.EquipBackpack` and `Npc.UnequipBackpack`.
- Inline `Position(x, y, z)` and `new BlockPosition(...)` fields compile to adapter-owned external wait payloads.
