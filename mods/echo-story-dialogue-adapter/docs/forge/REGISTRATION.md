# Forge Registration

`EchoStoryDialogueAdapterMod` регистрируется как Forge mod `echo_story_dialogue_adapter`.

Runtime dependency order в `mods.toml`:

- `forge` with `${forge_version_range}`
- `minecraft` with `${minecraft_version_range}`
- `echo_story_engine` with `${story_mod_version_range}`
- `echo_dialogue` with `${dialogue_mod_version_range}`

Current runtime ranges are derived from sibling JAR metadata:

- `echo_story_engine`: `[1.0.0,)`
- `echo_dialogue`: `[0.1.0-SNAPSHOT,)`

Story extension registration идёт через public `EchoStoryExtensionRegistrationEvent`. Adapter создаёт
`DialogueBridgeExtensionFactory`, а Story Engine создаёт session для конкретного server lifecycle.
