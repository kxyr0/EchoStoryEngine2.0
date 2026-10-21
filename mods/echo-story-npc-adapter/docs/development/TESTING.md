# Тестирование

Выполненные automated checks нужно указывать с точной Gradle task и результатом.

Текущие focused checks:

- `test`;
- `architectureTest`;
- `sourceShapeCheck`;
- `checkWhitespace`;
- `compileJava`;
- `verifyProductionJarContents`;
- `check`;
- `build`.

Integration checks N3:

- `n3IntegrationBuild` публикует sibling public artifacts, собирает production
  adapter и отдельный test-only acceptance mod;
- `-p integration-tests\echo-story-npc-acceptance-mod build` компилирует
  acceptance command mod и GameTest sources;
- `-p integration-tests\echo-story-npc-acceptance-mod runGameTestServer`
  запускает Forge GameTest sequence для Story-started spawn, repeated spawn,
  teleport, repeated teleport, despawn, repeated despawn, dormant chunk spawn и
  предотвращения duplicate materialization.
- `-p integration-tests\echo-story-npc-client-runner runProductionClientAcceptance`
  запускает isolated production Forge client с final Story/NPC/adapter JARs и
  отдельным acceptance mod. Он проверяет visible spawn, recovery после
  return-to-title и reload мира, visible teleport, despawn, second-world
  isolation, screenshots и clean shutdown.

Ручные acceptance checks:

- запустить три production mods плюс acceptance mod в отдельном Forge run;
- выполнить `/echo_story_npc_acceptance start spawn <executionId>`;
- выполнить `/echo_story_npc_acceptance actor echo_story_npc_test:john_father`
  и проверить `display=Отец`, materialized status и Overworld position;
- выполнить `/echo_story_npc_acceptance start teleport <executionId>` и
  проверить, что actor перешел на `x=5,y=64,z=0`;
- выполнить `/echo_story_npc_acceptance start despawn <executionId>` и
  проверить, что actor query возвращает `NOT_FOUND`;
- повторить с тем же `executionId`, чтобы проверить idempotent outcomes
  `already_present`, `already_at_target` и `already_absent`;
- выполнить `/echo_story_npc_acceptance start blocked_definition <executionId>`
  и проверить, что missing-definition actor query возвращает `NOT_FOUND`, а
  Story execution имеет `FAILED`;
- выполнить `/echo_story_npc_acceptance start dormant <executionId>`, проверить,
  что `echo_story_npc_test:dormant_father` имеет `DORMANT_CHUNK_UNLOADED`,
  `/echo_story_npc_acceptance entities echo_story_npc_test:dormant_father 8`
  возвращает `entities=0`, затем force-load chunk `[256,0]` и проверить, что
  actor стал `MATERIALIZED`, а `entities=1`;
- повторить dormant start с новым `executionId`, пока actor уже materialized,
  и проверить, что `entities=1` остается истинным.

Выполненный N3 acceptance на 2026-07-20:

- `n3IntegrationBuild` прошел;
- `-p integration-tests\echo-story-npc-acceptance-mod runGameTestServer`
  прошел с `Story NPC acceptance GameTests verified: passed=2`;
- isolated three-production-mod server прошел только с final JARs:
  Story all JAR, Echo NPC all JAR and adapter JAR;
- isolated acceptance server прошел с этими тремя JARs и отдельным
  acceptance mod JAR;
- acceptance server подтвердил outputs `created`, `already_present`, `moved`,
  `already_at_target`, `removed` и `already_absent` в persisted Story execution
  JSON;
- restart acceptance подтвердил, что spawn state, teleport target и despawn
  absence переживают clean server restarts.
- isolated blocked-definition server
  `build\n3-isolated-blocked-definition-server-20260720-081911` прошел:
  catalog загрузил пять test stories, `blocked_definition` упал на spawn node с
  `failureCode=ESE-ACTION-005`, а
  `echo_story_npc_test:missing_definition_actor` остался `NOT_FOUND`.
- isolated dormant server
  `build\n3-isolated-dormant-server-20260720-0843-entities` прошел со fresh
  final JARs и acceptance mod: catalog загрузил шесть test stories, dormant
  spawn оставил Story execution в `WAITING`, пока дальний chunk был unloaded,
  actor state был `DORMANT_CHUNK_UNLOADED`, `entities=0`; после
  `forceload add 4096 0` actor state стал `MATERIALIZED`, execution стал
  `FINISHED`, `entities=1`; repeated dormant spawn завершился с
  `result=already_present` и `entities=1`.
- isolated production client acceptance прошел:
  `-p integration-tests\echo-story-npc-client-runner runProductionClientAcceptance`
  запустил `forgeclient`/`srg` runtime с final JARs и отдельным acceptance mod,
  подтвердил `VISIBLE` spawn at `0.0,64.0,0.0`, `TITLE_CLEARED after_spawn`,
  recovery того же actor после world reload, `VISIBLE` teleport at
  `5.0,64.0,0.0`, despawn with `clientEntities=0`,
  `SECOND_WORLD_ISOLATED` в `N3ClientWorldB`, возврат на title до stop, три
  screenshots и выход с `BUILD SUCCESSFUL`.

GameTest run не заменяет final isolated production server/client acceptance
только с final JARs. Изолированные production server/client acceptance runs от
2026-07-20 являются основным evidence для final-JAR acceptance.
