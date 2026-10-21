# C1 Content Pack Foundation Plan

## Audit evidence

Фактические loader paths:

- Story: `data/<namespace>/echo_story/stories/**/*.es`;
- Dialogue: `data/<namespace>/echo_dialogue/dialogues/**/*.dialogue.es`;
- NPC: `data/<namespace>/echo_npc/npcs/**/*.npc.es`.

Фактические asset paths:

- NPC skin: `assets/<namespace>/textures/echo_npc/skins/<path>.png`;
- Dialogue portrait: `assets/<namespace>/textures/echo_dialogue/portraits/<path>.png`;
- Dialogue SkinHead: `assets/<namespace>/textures/echo_dialogue/skins/<path>.png`.

Фактические action types:

- `echo_dialogue:play`;
- `echo_npc:spawn`;
- `echo_npc:teleport`;
- `echo_npc:despawn`.

## Project identity

- Mod ID: `echo_story_content_pack`;
- display name: `Echo Story Content Pack`;
- version: `0.1.0-SNAPSHOT`;
- resource namespace: `jdh`;
- Minecraft: `1.20.1`;
- Forge: `47.4.10`;
- Java runtime target for Gradle tooling: 17.

## Folder structure

Production content:

- `src/main/resources/data/jdh/echo_story/stories/`;
- `src/main/resources/data/jdh/echo_dialogue/dialogues/`;
- `src/main/resources/data/jdh/echo_npc/npcs/`;
- `src/main/resources/assets/jdh/textures/echo_npc/skins/`;
- `src/main/resources/assets/jdh/textures/echo_dialogue/portraits/`;
- `src/main/resources/assets/jdh/textures/echo_dialogue/skins/`.

Acceptance-only content:

- `src/acceptance/resources/`.

Authoring support:

- `authoring/templates/`;
- `docs/authoring/`.

## Dependency metadata

Use `lowcodefml`; declare mandatory dependencies on Forge, Minecraft, Echo Story Engine, Echo Dialogue, Story-Dialogue Adapter, Echo NPC and Story-NPC Adapter. Do not declare EchoScript direct dependency.

## Authoring workflow

Documented in `docs/authoring/WORKFLOW.md`. Normal order: NPC, skin, dialogue, portrait, Story, `validateContent`, runtime validation, build, staging.

## ID conventions

Use `jdh:*`. Keep `SpeakerId`, `ActorId`, `NpcDefinitionId`, `DialogueId`, `StoryId` and `DialogueOutcomeId` as separate domain types.

## Asset conventions

NPC skins and SkinHead are 64x64 PNG. Portraits are bounded PNG. Source art does not enter `src/main/resources`.

## Sample vertical slice

Production starter:

- Story: `jdh:chapter_1_home_arrival`;
- Dialogue: `jdh:chapter_1_home_arrival`;
- NPC: `jdh:john_father`.

Acceptance-only slice:

- Story: `jdh:content_pack_smoke`;
- Dialogue: `jdh:content_pack_greeting`;
- NPC: `jdh:content_pack_father`;
- outcomes: `jdh:content_pack_accepted`, `jdh:content_pack_refused`.

## Validation strategy

Build-time validation checks layout, UTF-8, assets and archive shape. Semantic validation is delegated to actual runtime catalogs through staged final JARs.

## Runtime acceptance strategy

Stage final JARs with `stageStoryPack`, then run an isolated Forge 1.20.1 server/client using only staged final artifacts. Record health/catalog/reload/action results in `PROGRESS.md`.

## Final JAR structure

Allowed entries:

- `META-INF/MANIFEST.MF`;
- `META-INF/mods.toml`;
- `pack.mcmeta`;
- `data/jdh/...`;
- `assets/jdh/...`.

## Release staging

`stageStoryPack` creates `build/story-pack/` with `mods`, `README.txt` and `versions.txt`.

## Known limitations

- Current Story DSL has no non-failing `else` branch.
- Automated isolated Forge launch is not embedded in build-time validation yet.
- Visual acceptance requires actual client observation.

## Rollback

Remove this content pack JAR from `mods` and remove staged `build/story-pack/`. No sibling repositories are modified by content authoring.

## Non-goals

No N4, animation, navigation, AI, behavior trees, schedules, inventory, combat, custom items, visual editor, EchoScript language changes, semantic binder duplication, remote content updates, database, cloud saves or Maven publishing.

