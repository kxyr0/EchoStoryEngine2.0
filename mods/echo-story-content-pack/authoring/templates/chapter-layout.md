# Chapter layout template

Используйте отдельные resource trees для каждого runtime:

- Story: `src/main/resources/data/jdh/echo_story/stories/chapter_1/<location>/`
- Dialogue: `src/main/resources/data/jdh/echo_dialogue/dialogues/chapter_1/<location>/`
- NPC: `src/main/resources/data/jdh/echo_npc/npcs/chapter_1/<group>/`
- NPC skins: `src/main/resources/assets/jdh/textures/echo_npc/skins/`
- Dialogue portraits: `src/main/resources/assets/jdh/textures/echo_dialogue/portraits/`

Связь между scene parts выражается через stable IDs, а не через общий physical folder.

