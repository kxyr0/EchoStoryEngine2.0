# START_HERE

| Вопрос автора | Что редактировать | Куда класть |
|---|---|---|
| Что происходит в story? | StoryResource `.es` | `data/jdh/echo_story/stories/` |
| Какие у квеста состояния, цели и переходы? | QuestResource `.quest.es` | `data/jdh/echo_quests/quests/` |
| Что говорят персонажи и какие choices есть? | DialogueResource `.dialogue.es` | `data/jdh/echo_dialogue/dialogues/` |
| Что такое NPC definition? | NpcResource `.npc.es` | `data/jdh/echo_npc/npcs/` |
| Где описана лицевая иерархия персонажа? | FaceRigResource `.face-rig.es` | `data/jdh/echo_animation/face_rigs/` |
| Где лежат pose deltas лица? | FacePoseResource `.face-pose.es` | `data/jdh/echo_animation/face_poses/` |
| Где лежат facial animation clips? | FacialAnimationResource `.face-animation.es` | `data/jdh/echo_animation/face_animations/` |
| Где лежат PNG слои лица? | pixel PNG | `assets/jdh/textures/echo_animation/faces/` |
| Где задаются XYZ/Z-depth слоёв лица? | `Layer(...)` placement fields | `data/jdh/echo_animation/face_rigs/` |
| Куда authoring экспортирует neutral skin? | 64x64 Minecraft skin | `assets/jdh/textures/echo_npc/skins/` |
| Как выглядит полный NPC body? | 64x64 Minecraft skin | `assets/jdh/textures/echo_npc/skins/` |
| Как выглядит static dialogue portrait? | portrait PNG | `assets/jdh/textures/echo_dialogue/portraits/` |
| Что использует static authored SkinHead? | Minecraft skin PNG | `assets/jdh/textures/echo_dialogue/skins/` |
| Что дает текущие реальные NPC name и skin через N4? | D6 provider boundary + Dialogue-NPC Presentation Adapter | `echo-dialogue-npc-adapter` |
| Где живет весь actual game content? | Echo Story Content Pack | этот репозиторий |

Основные правила:

- namespace authored content: `jdh`;
- Forge mod ID: `echo_story_content_pack`;
- ES2 Story и Dialogue получают ID из data namespace и basename файла; `Id(...)` используется только для stable
  override, когда ID намеренно отличается от имени файла;
- Story, Quest, Dialogue и NPC не смешиваются в одном resource tree;
- Echo Animation face resources не пишутся в JSON5; JSON5 является только input для migration tool;
- `echo-animation-authoring` устанавливается только в dev client instance и не входит в production pack;
- final production pack требует Echo Animation runtime вместе с Story/Dialogue/NPC runtimes;
- semantic validation принадлежит actual engines, а не этому content pack;
- production JAR не содержит docs, templates, acceptance resources или Java classes.

Читайте дальше:

- `IDS.md`
- `ASSETS.md`
- `WRITING_STORIES.md`
- `WRITING_DIALOGUES.md`
- `WRITING_NPCS.md`
- `WRITING_CHAPTERS.md`
- `WORKFLOW.md`
- `RELOAD_AND_RESTART.md`
- `DYNAMIC_SPEAKERS.md`
- `TROUBLESHOOTING.md`
