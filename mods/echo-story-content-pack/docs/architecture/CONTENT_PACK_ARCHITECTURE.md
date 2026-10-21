# Content pack architecture

## Reload responsibility

Hot reload не принадлежит content-pack JAR. JAR хранит seed under
`story/content-pack/jdh`; Echo Story Engine извлекает его во внешнюю папку
`<gameDir>/story/content-pack/jdh`.

Echo Story Engine registers that external folder as both `SERVER_DATA` and
`CLIENT_RESOURCES`. Authored `.es` changes are applied by manual `/reload`.
Authored PNG assets under `<gameDir>/story/content-pack/jdh/assets` are exposed
to the client resource manager and can be refreshed with the normal Minecraft
client resource reload path, for example `F3+T`.

The root `assets/` in the installed content JAR remains a seed/fallback for
fresh installs, but day-to-day authoring should happen in the external
`story/content-pack/jdh/assets` folder.

Echo Story Content Pack is a content-only Forge mod.

Он содержит:

- `META-INF/mods.toml`;
- root `pack.mcmeta` for mod/resource metadata;
- root `assets/jdh/...` for normal Forge client resource loading;
- embedded seed pack under `story/content-pack/jdh/pack.mcmeta`;
- embedded seed `story/content-pack/jdh/data/jdh/...`;
- embedded seed `story/content-pack/jdh/assets/jdh/...`.

Он не содержит:

- production Java;
- `@Mod` bootstrap;
- Story Runtime;
- Dialogue Runtime;
- NPC Runtime;
- adapter logic;
- EchoScript parser или binder;
- copied API classes;
- nested dependency JARs;
- root runtime `data/` fallback.

Packaging decision: `lowcodefml`.

Forge 1.20.1 / 47.4.10 contains `LowCodeModLanguageProvider` with loader name
`lowcodefml`, so a separate Java bootstrap is not needed.

Разделение систем:

- Echo Story Engine выполняет Story definitions;
- Echo Dialogue выполняет Dialogue definitions;
- Echo NPC выполняет NPC definitions;
- Story-Dialogue Adapter связывает Story action `echo_dialogue:play` with Echo Dialogue;
- Story-NPC Adapter связывает Story actions `echo_npc:*` with Echo NPC;
- Echo Story Content Pack содержит authored project content.

Content JAR ставится на server и client как seed и как носитель client assets.
Echo Story Engine при первом запуске извлекает seed в
`<gameDir>/story/content-pack/jdh`; после этого data runtime читает внешнюю
папку через manual `/reload`, а client textures грузятся из root `assets/` в
установленном content JAR.
