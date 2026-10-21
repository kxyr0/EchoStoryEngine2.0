# Writing chapters

Рекомендуемая организация chapter 1:

- Story: `src/main/resources/data/jdh/echo_story/stories/chapter_1/home/`
- Dialogue: `src/main/resources/data/jdh/echo_dialogue/dialogues/chapter_1/home/`
- NPC definitions: `src/main/resources/data/jdh/echo_npc/npcs/chapter_1/family/`
- NPC skins: `src/main/resources/assets/jdh/textures/echo_npc/skins/`
- Dialogue portraits: `src/main/resources/assets/jdh/textures/echo_dialogue/portraits/`

Не требуется складывать все scene files в один physical folder. Story, Dialogue и NPC читаются разными loaders и должны оставаться в своих canonical resource trees.

Связь scene parts выражается через IDs:

- `StoryId`: `jdh:chapter_1_home_arrival`
- `DialogueId`: `jdh:chapter_1_home_arrival`
- `ActorId`: `jdh:john_father`
- `NpcDefinitionId`: `jdh:john_father`
- outcomes: `jdh:told_truth`, `jdh:lied`

Когда leaf folder достигает семи script files, разбивайте его по реальному смыслу: location, scene, chapter segment или quest line.

