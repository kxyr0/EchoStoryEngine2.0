# Reload and restart

`/reload` перезагружает только server data resources:

- Story `.es`;
- Dialogue `.dialogue.es`;
- NPC `.npc.es`;
- Echo Animation `.es`.

Catalogs reload independently. Нет одного global transaction across Story,
Dialogue, NPC and Animation.

External content changes under `<gameDir>/story/content-pack/jdh` are not
watched automatically. Save authored `.es` files, then run `/reload` manually.

Client PNG assets under `<gameDir>/story/content-pack/jdh/assets` are registered
as `CLIENT_RESOURCES`. After editing PNGs, use the normal Minecraft client
resource reload path, for example `F3+T`. A content JAR rebuild is not needed
for day-to-day PNG authoring after the runtime with external client resources is
installed.

Changing Java mod code or replacing a runtime JAR still requires a client
restart.

Active Story, Dialogue, NPC or Animation objects can remain pinned to compiled
definitions according to each runtime policy. For large linked changes, prefer
a clean restart.

Команды для проверки после reload:

- `/echo_story health`
- `/echo_story stories`
- `/echo_dialogue health`
- `/echo_dialogue dialogues`
- `/echo_npc health`
- `/echo_npc definitions`
