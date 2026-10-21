# Acceptance

Canonical acceptance IDs:

- `NpcDefinitionId`: `jdh:content_pack_father`
- `ActorId`: `jdh:content_pack_father`
- `DialogueId`: `jdh:content_pack_greeting`
- `SpeakerId`: `jdh:content_pack_father`
- accepted outcome: `jdh:content_pack_accepted`
- refused outcome: `jdh:content_pack_refused`
- accepted `StoryId`: `jdh:content_pack_smoke`
- refused `StoryId`: `jdh:content_pack_smoke_refused`

Acceptance-only resources лежат под:

`src/acceptance/resources/`

Они не входят в production content JAR. Test-only Java harness лежит в `acceptance-harness/` и тоже не входит в production content JAR.

## Build

```powershell
.\gradlew.bat validateContent
.\gradlew.bat verifyAcceptanceContentJar
.\gradlew.bat verifyAcceptanceHarnessJar
.\gradlew.bat stageAcceptanceStoryPack
.\gradlew.bat runAcceptanceStoryPackServer -PskipSiblingBuild
```

`stageAcceptanceStoryPack` собирает 8 JARs под `build/acceptance-story-pack/mods`:

- 6 dependency/runtime JARs: Echo Animation, Echo Dialogue, Echo NPC, Echo Story Engine, Story-Dialogue Adapter, Story-NPC Adapter;
- `echo-story-content-pack-0.1.0-SNAPSHOT-acceptance.jar`;
- `echo-story-content-pack-acceptance-harness-0.1.0-SNAPSHOT.jar`.

## Harness Commands

Команды выполняются в clean Forge dedicated server с staged acceptance `mods`.

```text
echo_story_content_pack_acceptance cleanup
echo_story_content_pack_acceptance run accepted jdh:c1_acceptance_accepted
echo_story_content_pack_acceptance run refused jdh:c1_acceptance_refused
echo_story_content_pack_acceptance run accepted jdh:c1_after_restart
echo_story_content_pack_acceptance start_only accepted jdh:c1_restart_line
echo_story_content_pack_acceptance resume accepted jdh:c1_restart_line
echo_story_content_pack_acceptance run_until_choice accepted jdh:c1_restart_choice
echo_story_content_pack_acceptance resume_choice accepted jdh:c1_restart_choice
echo_story_content_pack_acceptance status
```

Harness пишет machine-readable markers `ESCPC-ACCEPTANCE ...` в server log.
`resume_choice` перед отправкой choice ждет, пока public `DialogueService`
снова покажет ACTIVE session на `revision=2`; это важно после restart, когда
Dialogue session восстанавливается detached не строго одновременно с console
command.

`runAcceptanceStoryPackServer` запускает clean production Forge server в `build/acceptance-runtime-server` и включает auto-mode:

```text
ESCPC-ACCEPTANCE AUTO_START scenario=accepted
ESCPC-ACCEPTANCE PASS scenario=accepted
ESCPC-ACCEPTANCE AUTO_START scenario=refused
ESCPC-ACCEPTANCE PASS scenario=refused
ESCPC-ACCEPTANCE AUTO_PASS scenarios=accepted,refused
```

## Covered Evidence

- accepted vertical slice: Story starts, NPC reaches `MATERIALIZED`, Dialogue opens, choice `accept` is sent through the real Dialogue serverbound event path, terminal outcome is `jdh:content_pack_accepted`, Story reaches `FINISHED`, actor entity count stays `1`;
- refused vertical slice: Story starts, NPC is reused without duplicate, Dialogue choice `refuse` is sent through the real Dialogue serverbound event path, terminal outcome is `jdh:content_pack_refused`, Story reaches `FINISHED`, actor entity count stays `1`;
- restart-at-CHOICE slice: `run_until_choice` pauses on ACTIVE Dialogue `revision=2`, a restarted server runs `resume_choice`, waits for the recovered ACTIVE `revision=2`, sends the same choice through the real Dialogue serverbound event path, reaches `revision=3` and terminal outcome `jdh:content_pack_accepted`;
- automated server shutdown: runner sends console `stop` after `AUTO_PASS`, server exits with code `0`.

## Manual Client Checklist

1. Запустите game client с production staged `mods` или acceptance staged `mods`.
2. Подключитесь к server instance с тем же staged pack.
3. Запустите соответствующий Story через тестовую команду harness или другой runtime trigger.
4. Проверьте, что NPC видим в мире и не дублируется после повторного запуска.
5. Проверьте, что Dialogue UI показывает русские линии и choices.
6. Проверьте, что portrait fallback из `Alias(... visual: Portrait(...))` виден клиенту.
7. Проверьте accepted/refused outcome на клиентском UI.
8. Для production NPC `jdh:john_father` проверьте, что Echo Animation face layers видны на голове: sclera, pupils,
   eyelids, brows и glasses.
9. Если нужно подвинуть слой, редактируйте `Layer(...)` fields `placeX/placeY/placeZ` в
   `src/main/resources/data/jdh/echo_animation/face_rigs/chapter_1/family/john_father.face-rig.es`, затем пересоберите
   content JAR, переустановите его и перезапустите client.

Автоматическая визуальная проверка portrait fallback требует client observation. Не помечайте ее passed без фактического просмотра.

## Known Runtime Shape

Текущий Story DSL не имеет `else` branch. Acceptance dialogue содержит accepted/refused outcomes, но один Story не может корректно разветвиться по обоим outcomes без нового Story runtime feature. Поэтому acceptance использует отдельные StoryResources для accepted/refused.

Public `echo_story` commands сейчас: `health`, `stories`, `executions`, `execution`, `extensions`, `extension`, `diagnostics`. Harness запускает Story через фактический server-side bridge, а не через несуществующую public start command.

## Evidence Files

- production smoke log: `build/reports/runtime/production-story-pack-smoke-latest.log`;
- acceptance server log: `build/reports/runtime/acceptance-story-pack-server-latest.log`;
- live acceptance Forge log: `build/acceptance-runtime-server/logs/latest.log`.
