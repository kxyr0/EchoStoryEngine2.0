# PROGRESS.md

## Echo Quests timed conditions

- Version `0.8.2-SNAPSHOT` updates the quest template with Echo Time deadline, player-area, inventory, and failed
  fallback authoring.

## Quest ownership correction

- StoryResource documentation no longer presents stories as quests.
- Quest state, objectives, transitions, and progress are owned by the standalone `echo-quests` mod.

## ES2 high-level story authoring

2026-09-11: production stories, acceptance stories and the authoring template migrated to `Id(...)` plus
`OnStart()`. Story IDs remain stable, while start/finish nodes and transitions are inferred by Story Engine.
Content version advanced to `0.8.0-SNAPSHOT`; legacy graph markers are rejected by content validation.
`validateContent`, `processResources`, `verifyContentJar` and `verifyAcceptanceContentJar` passed. Full runtime-pack
staging was not completed because a pre-existing Story-Dialogue Adapter artifact named by the staging config is absent.

## ES2 high-level dialogue authoring

2026-08-26: стандартные dialogue templates и authoring docs переведены на `Node` / `start Node`, inferred
`DialogueId`, symbolic `GoTo`, runtime speaker presentation и explicit `Formerly` migrations. Content version
advanced to `0.7.0-SNAPSHOT`; legacy dialogue resources remain readable.

## Unified animation migration

2026-08-05: `jdh:humanoid_run` migrated from separate enter/loop/exit clip files to one procedural 17-tick
`humanoid_run.animation-clip.es`. Named segments are `idle_to_run`, `run_loop` and `run_to_idle`; the animation
resource references those views and the `neutral` point. Content version advanced to `0.6.0-SNAPSHOT` and requires
Echo Animation `0.5.0-SNAPSHOT` or newer.

## C1 progress

Статус: content-only foundation implemented; production JAR теперь хранит JDH историю как embedded seed для внешнего
`story/content-pack/jdh`; isolated final-JAR production smoke passed with Echo Animation; headless acceptance harness
passed Story/NPC/Dialogue accepted/refused auto flows; N4 adapter uses the documented `SpeakerId == ActorId` and
NPC-owned skin paths. Content pack itself remains code-free.

Сделано:

- создан независимый git-репозиторий без коммитов;
- выбран `lowcodefml`, потому что Forge 1.20.1 / 47.4.10 содержит `LowCodeModLanguageProvider` с loader name `lowcodefml`;
- production Java не добавлен;
- добавлены production resources под namespace `jdh`;
- production JAR не публикует root `data/` fallback; история лежит под `story/content-pack/jdh/` и извлекается
  Echo Story Engine во внешнюю папку, а root `assets/` остаётся обычным client-resource payload без external
  `CLIENT_RESOURCES` pack;
- `jdh:john_father` FaceRig использует `placeX/placeY/placeZ` для layer placement/depth всех текущих face layers;
- существующие PNG `sclera_left/right`, `pupil_left/right`, `brow_left/right`, `upper_eyelids`, `lower_eyelids`, `glasses`
  привязаны настоящими `Layer(...)` entries в `john_father.face-rig.es`;
- добавлен acceptance source set `src/acceptance/resources` с canonical IDs `jdh:content_pack_*`;
- добавлен test-only acceptance harness в отдельном `acceptance-harness` проекте; он не входит в production content JAR;
- `resume_choice` в test-only acceptance harness ждет восстановленную ACTIVE
  Dialogue session на `revision=2` перед отправкой choice, чтобы
  restart-at-CHOICE не зависел от порядка восстановления detached session и
  console command;
- добавлены build-time задачи `validateContent`, `contentReport`, `verifyContentJar`, `verifyAcceptanceContentJar`, `verifyAcceptanceHarnessJar`, `installContentJar`, `stageStoryPack`, `stageAcceptanceStoryPack`, `validateWithEchoRuntime`, `runProductionStoryPackSmokeServer`, `runAcceptanceStoryPackServer`;
- добавлены русские authoring, architecture, development и testing docs;
- Gradle wrapper скопирован из sibling Gradle/Forge проекта.

Факты аудита:

- фактический Story Engine путь: `data/<namespace>/echo_story/stories/**/*.es`;
- фактический Dialogue путь: `data/<namespace>/echo_dialogue/dialogues/**/*.dialogue.es`;
- фактический NPC путь: `data/<namespace>/echo_npc/npcs/**/*.npc.es`;
- фактический NPC skin путь: `assets/<namespace>/textures/echo_npc/skins/<path>.png`;
- фактический Dialogue Portrait путь: `assets/<namespace>/textures/echo_dialogue/portraits/<path>.png`;
- фактический authored Dialogue SkinHead путь: `assets/<namespace>/textures/echo_dialogue/skins/<path>.png`;
- фактический N4 dynamic NPC SkinHead путь: `assets/<namespace>/textures/echo_npc/skins/<path>.png`;
- фактический Echo Animation FaceRig путь: `data/<namespace>/echo_animation/face_rigs/**/*.face-rig.es`;
- фактический Echo Animation FacePose путь: `data/<namespace>/echo_animation/face_poses/**/*.face-pose.es`;
- фактический Echo Animation FacialAnimation путь: `data/<namespace>/echo_animation/face_animations/**/*.face-animation.es`;
- фактический seed-путь внутри JAR: `story/content-pack/jdh/{pack.mcmeta,data/jdh/...,assets/jdh/...}`;
- фактический client asset path внутри JAR: `assets/jdh/...`;
- фактический runtime-путь после первого запуска: `<gameDir>/story/content-pack/jdh/{pack.mcmeta,data/jdh/...,assets/jdh/...}`;
- Story action types adapters: `echo_dialogue:play`, `echo_npc:spawn`, `echo_npc:teleport`, `echo_npc:despawn`;
- Story action `output` хранит exact output key adapter-а; для `echo_npc:spawn` используется `output: "result"`, для `echo_dialogue:play` используется `output: "outcome"`.

Ограничения текущего состояния:

- текущий Story DSL имеет `Require(...)` без `else`; при несовпадении значения Story переходит в FAILED. Поэтому полноценная accepted/refused branch в одном Story невозможна без нового Story runtime feature, а C1 запрещает менять runtime. Acceptance покрывает обе ветки двумя StoryResources: `jdh:content_pack_smoke` и `jdh:content_pack_smoke_refused`;
- public `echo_story` command tree содержит `health`, `stories`, `executions`, `execution`, `extensions`, `extension`, `diagnostics`, но не содержит start/play command. Acceptance harness запускает Story через фактический `EchoStoryExecutionBridge`;
- headless dedicated server не может визуально подтвердить client-side portrait fallback, реальный Dialogue UI и визуальные face layers. Echo Animation runtime теперь содержит client render bridge для Echo NPC, но pixels in game остаются client-observation пунктом.

Runtime evidence:

- clean Forge server под `build/production-runtime-server` с seven final staged production JARs стартовал успешно, показал `Done`, выполнил Story/Dialogue/NPC/Echo Animation health/catalog commands и остановился через console `stop` с exit code 0;
- production smoke подтвердил первичное извлечение внешнего pack в
  `build/production-runtime-server/story/content-pack/jdh` и автоматическое подключение
  `echo_story_engine:jdh_external_content`;
- production Echo Animation catalog loaded `rigs=1`, `poses=1`, `animations=1`, `diagnostics=0`; commands showed `jdh:john_father`, `jdh:john_father/neutral`, `jdh:shared/blink`;
- production `jdh:john_father` rig contains real layer bindings for sclera, pupils, eyelids, brows and glasses;
- clean Forge server с `stageAcceptanceStoryPack` стартовал успешно и загрузил 8 JARs: 6 dependency/runtime JARs, test-only acceptance content JAR и test-only acceptance harness JAR;
- accepted vertical slice passed: Story start, NPC `MATERIALIZED`, Dialogue revisions/choice, outcome `jdh:content_pack_accepted`, Story `FINISHED`, `entities=1`;
- refused vertical slice passed: Story start, NPC reused without duplicate, Dialogue refused choice, outcome `jdh:content_pack_refused`, Story `FINISHED`, `entities=1`;
- acceptance auto mode printed `ESCPC-ACCEPTANCE AUTO_PASS scenarios=accepted,refused passes=2` and stopped through console `stop` with exit code 0.

Выполненные проверки:

- `./gradlew tasks --console=plain`: OK;
- `./gradlew validateContent --console=plain`: OK;
- `./gradlew build --console=plain`: OK;
- `./gradlew contentReport --console=plain`: OK;
- `./gradlew verifyContentJar --console=plain`: OK;
- `./gradlew verifyAcceptanceContentJar --console=plain`: OK;
- `./gradlew runProductionStoryPackSmokeServer --console=plain`: OK после перевода production content JAR на seed
  `story/content-pack/jdh`;
- `./gradlew verifyAcceptanceHarnessJar --console=plain`: OK;
- `./gradlew stageStoryPack --console=plain`: OK, staged 7 final production JARs;
- `./gradlew stageAcceptanceStoryPack --console=plain`: OK, staged 8 acceptance JARs;
- `./gradlew validateWithEchoRuntime --console=plain`: OK для staging/наличия final JAR set, без запуска Forge;
- `./gradlew runProductionStoryPackSmokeServer -PskipSiblingBuild --console=plain`: OK startup/catalog/animation commands/stop;
- `./gradlew runAcceptanceStoryPackServer -PskipSiblingBuild --console=plain`: OK accepted/refused/AUTO_PASS/stop;
- `./gradlew validateContent validateWithEchoRuntime build verifyContentJar contentReport stageStoryPack stageAcceptanceStoryPack runProductionStoryPackSmokeServer runAcceptanceStoryPackServer -PskipSiblingBuild --console=plain`: OK;
- `./gradlew verifyContentJar verifyAcceptanceContentJar --console=plain`: OK после добавления root `assets/` в JAR;
- `./gradlew build --console=plain`: OK для `echo-story-content-pack-0.1.9-SNAPSHOT.jar`;
- `git diff --check`: OK.

Итоговые артефакты:

- content jar: `build/libs/echo-story-content-pack-0.1.9-SNAPSHOT.jar`;
- acceptance content jar: `build/libs/echo-story-content-pack-0.1.9-SNAPSHOT-acceptance.jar`;
- acceptance harness jar: `acceptance-harness/build/libs/echo-story-content-pack-acceptance-harness-0.1.1-SNAPSHOT.jar`;
- staged runtime pack: `build/story-pack/mods`;
- staged acceptance runtime pack: `build/acceptance-story-pack/mods`;
- staging manifest: `build/story-pack/versions.txt`;
- production runtime evidence: `build/reports/runtime/production-story-pack-smoke-latest.log`;
- acceptance runtime evidence: `build/reports/runtime/acceptance-story-pack-server-latest.log`;
- content report: `build/reports/content/content-report.md`.

2026-10-11: NPC resources переведены на ES2 class metadata с сохранением ID,
скинов, цветов и форм. Добавлен `tools/Set-PrismStoryArchive.ps1` с ZIP-снимком,
SHA256, режимами Archive/Restore/Status и WhatIf. Сценарий переключения проверен
на временном pack: Restore сохраняет изменения рабочих файлов.
Версия: 0.10.0-SNAPSHOT. Подробности: `docs/authoring/STORY_ARCHIVE.md`.

Проверка: contentDoctor — 72 ресурса, 0 ошибок; root check и jar прошли.
