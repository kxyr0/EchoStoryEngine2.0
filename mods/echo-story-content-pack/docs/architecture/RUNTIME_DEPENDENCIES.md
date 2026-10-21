# Runtime dependencies

`META-INF/mods.toml` объявляет mandatory dependencies:

| Mod ID | Version range | Side | Зачем нужен |
|---|---:|---|---|
| `forge` | `[47,)` | `BOTH` | Forge loader и resource system |
| `minecraft` | `[1.20.1,1.21)` | `BOTH` | Minecraft resource/runtime version |
| `echo_story_engine` | `[1.0.0,)` | `BOTH` | загрузка и выполнение StoryResource `.es` |
| `echo_dialogue` | `[0.1.0-SNAPSHOT,)` | `BOTH` | загрузка DialogueResource и client presentation assets |
| `echo_story_dialogue_adapter` | `[0.1.0-SNAPSHOT,)` | `BOTH` | Story action `echo_dialogue:play` |
| `echo_story_animation_adapter` | `[0.1.0-SNAPSHOT,)` | `BOTH` | Story actions `echo_animation:*` |
| `echo_npc` | `[0.1.0-SNAPSHOT,)` | `BOTH` | загрузка NpcResource и rendering skins |
| `echo_animation` | `[0.1.0-SNAPSHOT,)` | `BOTH` | загрузка FaceRigResource, FacePoseResource и FacialAnimationResource |
| `echo_story_npc_adapter` | `[0.1.0-SNAPSHOT,)` | `BOTH` | Story actions `echo_npc:spawn`, `echo_npc:teleport`, `echo_npc:despawn` |

Direct dependency на EchoScript не объявляется. EchoScript является internal library внутри runtime JARs.

Echo UI Animations не объявлен direct dependency content pack, потому что current content pack не вызывает его напрямую и sibling `echo_dialogue` metadata не требует от content pack отдельной ordering dependency.

`echo-animation-authoring` не входит в production pack. Он устанавливается только в отдельный dev client instance для редактирования, migration и neutral skin export.

Лицензия: sibling Echo repositories используют unresolved/license placeholder. В этом pack применено `License decision unresolved`; отдельный license decision не выдуман.
