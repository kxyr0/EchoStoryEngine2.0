# Echo Story Content Pack

## Куда писать content

| Что пишем | Resource type | Путь |
|---|---|---|
| Story flow | StoryResource `.es` | `src/main/resources/data/jdh/echo_story/stories/` |
| Dialogue lines и choices | DialogueResource `.dialogue.es` | `src/main/resources/data/jdh/echo_dialogue/dialogues/` |
| NPC definition | NpcResource `.npc.es` | `src/main/resources/data/jdh/echo_npc/npcs/` |
| Magic spells | SpellResource `.es` | `src/main/resources/data/jdh/echo_magic/spells/<school>/` |
| Face rig | FaceRigResource `.face-rig.es` | `src/main/resources/data/jdh/echo_animation/face_rigs/` |
| Face pose | FacePoseResource `.face-pose.es` | `src/main/resources/data/jdh/echo_animation/face_poses/` |
| Facial animation | FacialAnimationResource `.face-animation.es` | `src/main/resources/data/jdh/echo_animation/face_animations/` |
| Face PNG layers | pixel PNG | `src/main/resources/assets/jdh/textures/echo_animation/faces/` |
| Exported neutral NPC skin | 64x64 PNG | `src/main/resources/assets/jdh/textures/echo_npc/skins/` |
| Полный NPC skin | 64x64 PNG | `src/main/resources/assets/jdh/textures/echo_npc/skins/` |
| Dialogue portrait fallback | PNG | `src/main/resources/assets/jdh/textures/echo_dialogue/portraits/` |
| Optional static SkinHead fallback | 64x64 PNG | `src/main/resources/assets/jdh/textures/echo_dialogue/skins/` |

Canonical namespace для authored IDs: `jdh`.

D6 в Echo Dialogue разрешает runtime provider заменить authored display/visual перед отправкой LINE или CHOICE клиенту. Без N4 provider-а authored fallback из `Alias(...)` остается рабочим и authoritative для content authoring. N4 реализован отдельным `echo-dialogue-npc-adapter`; content pack только хранит matching `SpeakerId`/`ActorId` и NPC-owned skin resources.

Echo Animation A0 хранит authoritative face definitions только в EchoScript `.es`. Legacy JSON5 разрешён только как input для dev-only `echo-animation-authoring`; production resources и final content JAR не должны содержать JSON5. Старый body PNG может оставаться faceless, потому что neutral NPC skin экспортируется authoring tool в `assets/jdh/textures/echo_npc/skins/`.

## Быстрые команды

```powershell
.\gradlew.bat validateContent
.\gradlew.bat build
.\gradlew.bat verifyContentJar
.\gradlew.bat contentReport
.\gradlew.bat stageStoryPack
```

Безопасная live-copy в Prism выполняется только merge-only скриптом:

```powershell
.\tools\Deploy-PrismStoryContentPack.ps1 -WhatIf
.\tools\Deploy-PrismStoryContentPack.ps1
```

Он не удаляет файлы в `minecraft/story`; подробности: `docs/development/PRISM_DEPLOY_SAFETY.md`.

Финальный content JAR:

`build/libs/echo-story-content-pack-0.3.0-SNAPSHOT.jar`

Именно этот JAR ставится в `mods` вместе с final production JARs runtime-модов. Content JAR нужен и на dedicated server, и на client: `data/` читается сервером, `assets/` нужны клиентскому rendering.

Для A0 production pack дополнительно требует final Echo Animation JAR `echo-animation-0.1.0-SNAPSHOT-all.jar`. Dev-only `echo-animation-authoring` в production pack не включается.

## Entry point

Начинайте с `docs/authoring/START_HERE.md`. Шаблоны лежат в `authoring/templates/`.

NPC: [EchoScript 2](docs/authoring/WRITING_NPCS.md). Архив: [инструкция](docs/authoring/STORY_ARCHIVE.md).
