# Troubleshooting

Wrong folder:

- Story должен лежать под `data/jdh/echo_story/stories/`;
- Dialogue должен лежать под `data/jdh/echo_dialogue/dialogues/`;
- NPC должен лежать под `data/jdh/echo_npc/npcs/`.

Wrong extension:

- Story: `.es`;
- Dialogue: `.dialogue.es`;
- NPC: `.npc.es`.

Missing ID:

- Story ID задается в `Story("jdh:...")`;
- Dialogue ID задается в `Dialogue("jdh:...")`;
- NPC definition ID задается в `Npc(id: "jdh:...")`.

Missing referenced DialogueId:

- проверьте `.dialogue.es`;
- запустите runtime catalog validation;
- проверьте `/echo_dialogue dialogues`.

Missing referenced NpcDefinitionId:

- проверьте `.npc.es`;
- проверьте `/echo_npc definitions`;
- убедитесь, что Story action использует `definitionId`.

Missing skin PNG:

- `Skin("jdh:john_father")` требует `assets/jdh/textures/echo_npc/skins/john_father.png`;
- skin должен быть PNG 64x64.

Missing portrait PNG:

- `Portrait("jdh:john_father")` требует `assets/jdh/textures/echo_dialogue/portraits/john_father.png`.

Runtime JAR missing:

- запустите `.\gradlew.bat stageStoryPack`;
- проверьте `build/story-pack/mods`;
- не добавляйте API, sources, GameTest или EchoScript JARs вручную.

Incompatible mod version:

- проверьте `META-INF/mods.toml` в content JAR;
- текущие ranges описаны в `docs/architecture/RUNTIME_DEPENDENCIES.md`.

