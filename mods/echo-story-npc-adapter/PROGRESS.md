# PROGRESS

Дата отчета: 2026-07-20.

## Статус

N1 complete. N2 complete.

N3 complete. Production adapter реализован, Story/NPC public artifact path
проверен, добавлен отдельный acceptance mod, выполнены Forge GameTest,
isolated three-production-mod server smoke, isolated acceptance server smoke,
server restart acceptance для spawn/teleport/despawn, negative acceptance для
blocked missing NPC definition, dormant chunk acceptance для отложенной
материализации без дублей и isolated production client acceptance с визуальным
spawn/recovery/teleport, despawn, return-to-title и second-world isolation.
Animation, movement/navigation, Dialogue integration и AI остаются
отложенными за пределами N3. Рекомендуемый следующий milestone: N4 -
Dialogue-NPC Presentation Adapter или A1 - Echo Animation Core с per-Actor
state machines, согласно принятой roadmap.

## Выполнено

- Подтвержден фактический Story Engine path: `D:\JDHSeason\mods\echo-story-engine`.
- Подтверждено отсутствие adapter repo до старта N3.
- Проверены public Story extension types:
  `ActionStartRequest`, `ActionRecoveryRequest`, `ActionCancelRequest`,
  `ActionCompletion`, `ActionCompletionSink`, descriptor/capabilities/session
  and Forge registration event.
- Проверены public Echo NPC service/bridge types.
- В Echo NPC добавлен N3-facing `ActorMutationId` и caller-supplied mutation ID
  для spawn/teleport/despawn.
- Для Echo NPC выполнены:
  `:npc-contracts:test`,
  `:npc-forge-1.20.1:compileJava`,
  `:npc-forge-1.20.1:architectureTest`,
  `sourceShapeCheck`,
  `checkWhitespace`.
- После исправления `ForgeMutationQueue.submit` выполнен полный Echo NPC
  `check`; production JAR verification: `entries=177`.
- Опубликованы Story public artifacts через `publishExtensionIntegrationArtifacts`.
- Опубликованы Echo NPC public artifacts через `publishNpcIntegrationArtifacts`.
- Реализован production adapter path:
  Story extension registration, three action handlers, deterministic mutation
  mapping, payload validation, public `NpcService` submission, bounded
  observation and exact success outputs.
- Добавлены plain JVM tests и architecture tests.
- Выполнены adapter checks:
  - `compileJava`;
  - `test`;
  - `architectureTest`;
  - `sourceShapeCheck`;
  - `checkWhitespace`;
  - `check`;
  - `build`.
- После повторной публикации Echo NPC artifacts повторно выполнены adapter
  `check` и `build`; оба завершились успешно.
- Выполнены adapter orientation checks:
  - `projects` - root project без subprojects;
  - `dependencies --configuration compileClasspath` - compile classpath содержит
    Story public artifacts, Echo NPC public artifacts и Forge.
- `verifyProductionJarContents` подтвердил production JAR
  `echo-story-npc-adapter-0.1.0-SNAPSHOT.jar`, `entries=34`, без bundled
  Story/NPC API classes.
- В Story Engine добавлена поддержка flat string-valued payload arguments в
  EchoScript `Action(...)`: дополнительные named literal arguments сохраняются
  в `StoryNode.actionArguments()`, Story reserved fields остаются
  `type`, `output`, `next`.
- Повторно выполнен полный Story Engine `check` и публикация
  `publishExtensionIntegrationArtifacts`.
- Повторно выполнены Echo NPC `publishNpcIntegrationArtifacts` и
  `verifyPublicationMetadata`.
- В adapter добавлены orchestration tasks:
  `verifySiblingPublicArtifacts`, `buildAcceptanceMod`, `n3IntegrationBuild`.
- Добавлен отдельный test-only acceptance Forge mod:
  `integration-tests\echo-story-npc-acceptance-mod`.
- Acceptance mod содержит canonical NPC `echo_story_npc_test:father`,
  display name `Отец`, skin `echo_story_npc_test:father`, actor
  `echo_story_npc_test:john_father`, actors
  `echo_story_npc_test:dormant_father`,
  `echo_story_npc_test:missing_definition_actor` и реальные Story resources
  для `spawn`, `teleport`, `despawn`, `combined`, `dormant`,
  `blocked_definition`.
- Выполнен `n3IntegrationBuild`: public artifacts опубликованы, production
  adapter собран, acceptance mod собран.
- Выполнен Forge GameTest
  `EchoStoryNpcAcceptanceGameTests.storySpawnTeleportDespawnSequence`;
  выполнен Forge GameTest
  `EchoStoryNpcAcceptanceGameTests.storySpawnDormantChunkThenMaterializesOnce`;
  лог подтвердил `All 2 required tests passed`; сценарии покрывают spawn,
  repeated spawn, teleport, repeated teleport, despawn, repeated despawn,
  dormant chunk spawn, delayed materialization и duplicate materialization
  prevention.
- Добавлен test-only RCON helper
  `integration-tests\tools\Invoke-RconCommands.ps1` для headless dedicated
  server acceptance.
- Выполнен isolated three-production-mod server smoke:
  `build\n3-isolated-three-mod-server-20260720-075705`, `mods` содержит ровно
  `echo-story-engine-0.1.0-all.jar`, `echo-npc-0.1.0-SNAPSHOT-all.jar`,
  `echo-story-npc-adapter-0.1.0-SNAPSHOT.jar`; сервер дошел до `Done`, поднял
  RCON, принял Story extension, остановился через `stop`, process exit code `0`.
- Выполнен isolated acceptance server smoke:
  `build\n3-isolated-acceptance-server-20260720-075602`, production JARs плюс
  `echo-story-npc-acceptance-1.0.0.jar`; RCON подтвердил catalog `ACCEPTED`,
  четыре test Story resources, actor `NOT_FOUND` до spawn, `Отец`
  `MATERIALIZED` at `0.0,64.0,0.0` после spawn, позицию `5.0,64.0,0.0` после
  teleport и `NOT_FOUND` после despawn.
- Persisted Story execution JSON подтвердил outputs:
  `created`, `already_present`, `moved`, `already_at_target`, `removed`,
  `already_absent`.
- Restart acceptance на той же isolated acceptance world подтвердил, что spawn
  state, teleport target и despawn absence переживают restart.
- Выполнен isolated blocked-definition acceptance:
  `build\n3-isolated-blocked-definition-server-20260720-081911`, production
  JARs плюс `echo-story-npc-acceptance-1.0.0.jar`; RCON подтвердил catalog
  `ACCEPTED`, пять test Story resources, запуск `blocked_definition`, actor
  `echo_story_npc_test:missing_definition_actor` остался `NOT_FOUND`, persisted
  execution `phase=FAILED`, `failureCode=ESE-ACTION-005`.
- Выполнен isolated dormant chunk acceptance:
  `build\n3-isolated-dormant-server-20260720-0843-entities`, production JARs
  плюс `echo-story-npc-acceptance-1.0.0.jar`; RCON подтвердил catalog
  `ACCEPTED`, шесть test Story resources, actor
  `echo_story_npc_test:dormant_father` сначала `NOT_FOUND`, после запуска
  `dormant` стал `DORMANT_CHUNK_UNLOADED` с `desired=ACTIVE` и позицией
  `4096.0,64.0,0.0`, Story execution остался `WAITING`, а
  `/echo_story_npc_acceptance entities echo_story_npc_test:dormant_father 8`
  вернул `entities=0`.
- После `forceload add 4096 0` тот же isolated dormant server подтвердил
  `MATERIALIZED`, Story execution `FINISHED`, `entities=1`; повторный запуск
  `dormant` с новым executionId завершился `result=already_present` и сохранил
  `entities=1`.
- Persisted dormant Story execution JSON подтвердил `result=created` для
  первого запуска и `result=already_present` для повторного; persisted Echo NPC
  actor JSON содержит `presence=ACTIVE`, position `4096.0,64.0,0.0` и
  `entityBindingId`.
- Выполнен isolated production client acceptance:
  `.\gradlew.bat -p integration-tests\echo-story-npc-client-runner runProductionClientAcceptance --console=plain`;
  production Forge client запущен через `forgeclient`/`srg` runtime с final
  `echo-story-engine-0.1.0-all.jar`, `echo-npc-0.1.0-SNAPSHOT-all.jar`,
  `echo-story-npc-adapter-0.1.0-SNAPSHOT.jar` и отдельным
  `echo-story-npc-acceptance-1.0.0.jar`; run завершился `BUILD SUCCESSFUL`.
- Client acceptance log подтвердил: `VISIBLE` spawn at `0.0,64.0,0.0` with
  `clientEntities=1`, `TITLE_CLEARED after_spawn`, recovery of the same actor
  after world reload with `records=1`, teleport visibility at
  `5.0,64.0,0.0`, despawn with `clientEntities=0`,
  `TITLE_CLEARED after_despawn`, `SECOND_WORLD_ISOLATED` in `N3ClientWorldB`,
  `TITLE_CLEARED before_stop` and final
  `ESNPC-CLIENT-ACCEPTANCE PASS visibleSpawn=true recovery=true teleport=true despawn=true titleClear=true secondWorldIsolation=true`.
- Client acceptance screenshots созданы:
  `n3-client-spawn-visible.png`, `n3-client-recovery-visible.png`,
  `n3-client-teleport-visible.png`.
- Production client runner проверил отсутствие forbidden markers:
  `NoSuchMethodError`, `ClassNotFoundException`, `NoClassDefFoundError`,
  `Cound not schedule mailbox`, `RejectedExecutionException`.
- После перевода `mods.toml` metadata на русские UTF-8 строки через Unicode
  escapes повторно выполнен isolated production client acceptance на новом
  adapter JAR `41054` bytes; PASS зафиксирован в `latest.log` в 10:06:26,
  forbidden markers отсутствуют, screenshots обновлены.
- Финальные checks после исправлений client harness и документации:
  `projects`, `dependencies`, `test architectureTest sourceShapeCheck
  checkWhitespace verifyProductionJarContents check build`, `n3IntegrationBuild`,
  acceptance mod `runGameTestServer`, focused Echo NPC API gates и Story Engine
  `check publishExtensionIntegrationArtifacts`.

## Выявленные ограничения

- Production `/echo_story` commands являются read-only и не запускают execution;
  acceptance start command живет только в test-only support mod.
- Forge GameTest использует ForgeGradle deobfuscated runtime copies of final mod
  JAR contents. Для production-JAR proof дополнительно выполнены headless
  isolated dedicated-server runs с final JARs.

## Не заявлено

- `git diff --check` не выполнен как git-проверка, потому что adapter repo и
  Echo NPC directory не являются git repositories.
