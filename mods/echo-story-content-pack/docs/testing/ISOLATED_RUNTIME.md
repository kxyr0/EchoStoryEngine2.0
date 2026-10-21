# Isolated runtime

Изолированный runtime должен содержать только final production JARs:

- Echo Animation;
- Echo Story Engine;
- Echo Dialogue;
- Story-Dialogue Adapter;
- Echo NPC;
- Story-NPC Adapter;
- Echo Story Content Pack.

Не включайте:

- internal modules;
- public API JARs;
- separate EchoScript JARs;
- source-set classpaths;
- GameTest or acceptance mods, кроме отдельного acceptance harness run;
- Maven Local artifacts.

`validateWithEchoRuntime` гарантирует staging final JAR set и фиксирует, что actual server launch должен быть отдельным evidence. Для production smoke нужно проверить:

- all mods load;
- content pack discovered;
- Story/Dialogue/NPC/Echo Animation resources load;
- health/catalog commands show expected IDs;
- no `ClassNotFoundException`;
- no `NoClassDefFoundError`;
- clean `stop`;
- exit code zero.

Story execution, Dialogue choices и NPC materialization проверяются отдельным headless acceptance harness run на `stageAcceptanceStoryPack`.

Не используйте sibling `runGameTestServer` как доказательство isolated runtime для этого content-pack: это userdev classpath, а не clean final-JAR instance. Диагностический запуск через Story Engine dev runtime подтвердил Forge discovery content-pack как valid `lowcodefml` mod, но не дошел до resource reload/GameTests из-за внешних runtime artifact/classpath проблем:

- final `*-all.jar` набор конфликтует с userdev classpath по JPMS-модулям EchoScript;
- plain Dialogue/NPC JARs в userdev runtime дают obf/deobf mismatch в Echo NPC.

Последний clean production server smoke выполнялся задачей `runProductionStoryPackSmokeServer` в `build/production-runtime-server` на seven final staged JARs из `build/story-pack/mods`.

Проверено:

- Forge 1.20.1 / 47.4.10 server startup;
- discovery всех семи production JARs;
- `echo_story_content_pack` loaded as `lowcodefml`;
- `jdh:chapter_1_home_arrival` виден в Story catalog;
- `jdh:chapter_1_home_arrival` виден в Dialogue catalog;
- `jdh:john_father` виден в NPC definitions;
- `jdh:john_father`, `jdh:john_father/neutral`, `jdh:shared/blink` видны в Echo Animation commands;
- Echo Animation reload catalog: `rigs=1`, `poses=1`, `animations=1`, `diagnostics=0`;
- `stop`, exit code 0.

Проверено задачей `runAcceptanceStoryPackServer` в `build/acceptance-runtime-server`:

- accepted Story execution reaches `FINISHED`;
- refused Story execution reaches `FINISHED`;
- live NPC actor reaches `MATERIALIZED`;
- repeated runs keep actor entity count at `1`;
- Dialogue advances through real serverbound advance/choice event paths.

Не проверено headless smoke/harness:

- client-side portrait rendering;
- visual accepted/refused UI selection by a real client.
