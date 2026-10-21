# Изолированное тестирование

Production three-mod server/client acceptance должен использовать только final
JARs:

- Echo Story Engine final JAR;
- Echo NPC final JAR;
- Echo Story NPC Adapter final JAR.

Acceptance support можно использовать только в отдельном run; он не должен
попадать в production adapter JAR.

## Test-only Acceptance Mod

Репозиторий содержит отдельный support mod в
`integration-tests\echo-story-npc-acceptance-mod`. Он намеренно не является
root Gradle subproject и не упаковывается в production adapter JAR.

Собрать полный набор N3 integration artifacts:

```powershell
.\gradlew.bat n3IntegrationBuild --console=plain
```

Запустить Forge GameTest acceptance sequence:

```powershell
.\gradlew.bat -p integration-tests\echo-story-npc-acceptance-mod runGameTestServer --console=plain
```

GameTest запускает Story executions через public Story execution bridge,
использует реальные EchoScript resources, dispatch-ит adapter actions и
проверяет public Echo NPC actor state после spawn, repeated spawn, teleport,
repeated teleport, despawn, repeated despawn, dormant chunk spawn и
предотвращения duplicate materialization.

Запустить isolated production client acceptance sequence:

```powershell
.\gradlew.bat -p integration-tests\echo-story-npc-client-runner runProductionClientAcceptance --console=plain
```

Production client runner требует установленный Forge client runtime в
`build\n3-production-client-runtime`. Если runtime отсутствует, установить Forge
так:

```powershell
java -jar build\n3-production-client-tools\forge-1.20.1-47.4.10-installer.jar --installClient build\n3-production-client-runtime
```

Runner копирует только final Story, Echo NPC и adapter JARs плюс отдельный
acceptance mod в isolated game directory, запускает `forgeclient`/`srg`
runtime, quick-load-ит `N3ClientWorldA`, делает screenshots для visible
spawn/recovery/teleport, проверяет despawn и isolation `N3ClientWorldB`,
возвращается на title перед остановкой и падает на известных classpath/shutdown
markers.

Headless dedicated-server checks могут использовать RCON helper:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "& 'integration-tests\tools\Invoke-RconCommands.ps1' -Port 25597 -Password 'echo_story_npc_acceptance' -Commands @('echo_story health','echo_story stories')"
```

Для ручного server acceptance установить final Story, Echo NPC и adapter JARs
плюс acceptance mod JAR из:

```text
integration-tests\echo-story-npc-acceptance-mod\build\libs\echo-story-npc-acceptance-1.0.0.jar
```

Полезные команды:

```text
/echo_story_npc_acceptance start spawn echo_story_npc_test:spawn_manual_1
/echo_story_npc_acceptance start teleport echo_story_npc_test:teleport_manual_1
/echo_story_npc_acceptance start despawn echo_story_npc_test:despawn_manual_1
/echo_story_npc_acceptance start combined echo_story_npc_test:combined_manual_1
/echo_story_npc_acceptance start blocked_definition echo_story_npc_test:blocked_manual_1
/echo_story_npc_acceptance start dormant echo_story_npc_test:dormant_manual_1
/echo_story_npc_acceptance actor echo_story_npc_test:john_father
/echo_story_npc_acceptance actor echo_story_npc_test:missing_definition_actor
/echo_story_npc_acceptance actor echo_story_npc_test:dormant_father
/echo_story_npc_acceptance entities echo_story_npc_test:dormant_father 8
/forceload add 4096 0
/forceload remove 4096 0
```

Для fresh Story execution использовать новый `executionId`. Повторно
использовать тот же `executionId` только когда намеренно проверяется
idempotent recovery/replay.

Успешный headless run 2026-07-20 использовал:

- three-production-mod server:
  `build\n3-isolated-three-mod-server-20260720-075705`;
- acceptance server:
  `build\n3-isolated-acceptance-server-20260720-075602`;
- RCON ports `25598` and `25597`;
- clean shutdown через `stop`.

Более поздний blocked-definition acceptance run использовал:

- acceptance server:
  `build\n3-isolated-blocked-definition-server-20260720-081911`;
- RCON port `25599`;
- clean shutdown через `stop`.

Более поздний dormant chunk acceptance run использовал:

- acceptance server:
  `build\n3-isolated-dormant-server-20260720-0843-entities`;
- RCON port `25601`;
- clean shutdown через `stop`.

Production client acceptance run использовал:

- game directory:
  `build\n3-production-client-acceptance\game`;
- runtime directory:
  `build\n3-production-client-runtime`;
- launch target `forgeclient`, naming domain `srg`;
- final JARs:
  `echo-story-engine-0.1.0-all.jar`,
  `echo-npc-0.1.0-SNAPSHOT-all.jar`,
  `echo-story-npc-adapter-0.1.0-SNAPSHOT.jar`;
- separate support JAR:
  `echo-story-npc-acceptance-1.0.0.jar`;
- clean shutdown через return-to-title перед `client.stop()`.

Лог three-production-mod server подтвердил `Done`, старт RCON, acceptance
adapter extension, Story host start/stop и отсутствие fatal classloading
markers.

Acceptance server подтвердил:

- Story catalog `ACCEPTED`, five test stories loaded;
- actor изначально `NOT_FOUND`;
- spawn дал `display=Отец`, `status=MATERIALIZED`, position
  `0.0,64.0,0.0`;
- teleport переместил того же actor на `5.0,64.0,0.0`;
- despawn вернул actor query в `NOT_FOUND`;
- persisted Story variables содержали `created`, `already_present`, `moved`,
  `already_at_target`, `removed`, `already_absent`;
- после clean restarts сохранились spawned actor state, teleported position и
  despawned absence.
- сценарий `blocked_definition` оставил
  `echo_story_npc_test:missing_definition_actor` в `NOT_FOUND`, сохранил
  execution как `phase=FAILED` и записал `failureCode=ESE-ACTION-005`.

Dormant acceptance server подтвердил:

- Story catalog `ACCEPTED`, six test stories loaded;
- actor `echo_story_npc_test:dormant_father` изначально `NOT_FOUND`;
- dormant spawn сохранил actor с `status=DORMANT_CHUNK_UNLOADED`,
  `desired=ACTIVE`, position `4096.0,64.0,0.0`;
- Story execution остался `WAITING`, пока chunk `[256,0]` был unloaded;
- `/echo_story_npc_acceptance entities echo_story_npc_test:dormant_father 8`
  вернул `entities=0` до force-load и `entities=1` после force-load;
- после `forceload add 4096 0` actor стал `MATERIALIZED`, а Story execution
  стал `FINISHED`;
- repeated dormant spawn завершился с persisted `result=already_present` и
  сохранил `entities=1`;
- latest log не содержал fatal crash или classloading markers.

Production client acceptance подтвердил:

- все mods загрузились из isolated game `mods` directory;
- нет missing public API class markers или production final-JAR mapping crash;
- Story и NPC resources загрузились;
- adapter extension принят Story host;
- spawn создал visible `echo_npc` entity для
  `echo_story_npc_test:john_father` at `0.0,64.0,0.0` with
  `clientEntities=1`;
- return-to-title очистил первое integrated server/client state;
- повторная загрузка `N3ClientWorldA` восстановила того же actor из persistence
  без duplicate visible entity;
- teleport переместил actor на `5.0,64.0,0.0` with `clientEntities=1`;
- despawn удалил actor и client entity;
- `N3ClientWorldB` не унаследовал actor из первого мира;
- созданы screenshots:
  `n3-client-spawn-visible.png`,
  `n3-client-recovery-visible.png`,
  `n3-client-teleport-visible.png`;
- latest log не содержал `NoSuchMethodError`, `ClassNotFoundException`,
  `NoClassDefFoundError`, `Cound not schedule mailbox` or
  `RejectedExecutionException` markers.
