# EchoStory Engine Authoring Tools

VS Code extension for the current modular Echo Story workspace under `D:\JDHSeason\mods`.

Version `1.9.0` adds the high-level Echo Dialogue syntax: optional namespace, inferred resource IDs, `Node` / `start Node`, symbolic `GoTo`, sparse `Speaker` overrides, and `Formerly` save migrations. Highlighting, snippets, hover/signature help, navigation, rename, formatting, and workspace indexing understand the new form while legacy authoring remains supported. The default index targets the story content pack and also discovers additional mod data roots under `D:\JDHSeason\mods`.

```text
D:\JDHSeason\mods\echo-story-content-pack\src\main\resources\data
```

## Current Coverage

- EchoScript language mode for `.es` resource files.
- Legacy JSON5 language mode and formatter for old authoring files.
- Workspace index for current resource roots:
  - `data/<namespace>/echo_story/stories/**/*.es`
  - `data/<namespace>/echo_story/environments/**/*.es`
  - `data/<namespace>/echo_dialogue/dialogues/**/*.dialogue.es`
  - `data/<namespace>/echo_npc/npcs/**/*.npc.es`
  - `data/<namespace>/echo_npc/forms/**/*.{form.es,es}`
  - `data/<namespace>/echo_player/profile_templates/**/*.player.es`
  - `data/<namespace>/echo_item/items/**/*.weapon.es`
  - `data/<namespace>/echo_trigger/triggers/**/*.trigger.es`
  - `data/<namespace>/echo_npc_animation/bindings/**/*.npc-animation-binding.es`
  - `data/<namespace>/echo_animation/{animations,animation_profiles,animation_rigs,animation_clips,face_rigs,face_poses,face_animations}/**/*.es`
- ID extraction from current resource builders such as `Story(...)`, `Environment(...)`, `Dialogue(...)`, `Npc(id: ...)`, `Form(id: ...)`, `PlayerProfile(id: ...)`, `Weapon(id: ...)`, trigger `Id(...)`, `Animation(...)`, `FacialAnimation(...)`, `AnimationProfile(...)`, `AnimationRig(...)`, `AnimationClip(...)`, `FacePose(...)`, `NpcAnimationBinding(...)`, and high-level `face Name("id")` rig files.
- Reference/navigation support for common EchoScript named arguments such as `dialogueId`, `environmentId`, `defaultForm`, `definitionId`, `actorId`, `animationId`, `faceAnimationId`, `playerProfileTemplateId`, `weaponId`, `storyId`, `cameraPathId`, and related legacy forms.
- State key indexing for new StoryResource `Set("node", variable: "...")` calls.
- Workspace symbol search over indexed stories, dialogues, NPCs, player profile templates, weapons, triggers, animations, animation profiles/rigs/clips, face rigs/poses/animations, bindings, skins, localization keys, state keys, and signal keys.
- EchoScript snippets, hovers, signature help, formatting, local method/node navigation, rename support, and linter diagnostics for current modular resources, effects, wardrobe files, and face DSL helpers, while keeping legacy JSON5/tooling support available.

## Install Locally

Install Node.js and npm, then run:

```powershell
npm install
npm run package
powershell -ExecutionPolicy Bypass -File scripts/install-local.ps1
```

`scripts/install-local.ps1` first tries `code --install-extension`. If VS Code CLI is not installed, it unpacks the VSIX into `%USERPROFILE%\.vscode\extensions\echostoryengine.echostoryengine-authoring-tools-1.9.0`.

## Commands

- `EchoStory: Refresh Story Index`
- `EchoStory: Show Story Index`
- `EchoStory: Format Current File`
- `EchoStory: Open Authoring Docs`

## Important Settings

- `echostoryEngine.modRoot`: defaults to `D:\JDHSeason\mods`.
- `echostoryEngine.storyDataGlob`: defaults to `echo-story-content-pack/src/main/resources/data`; semicolon-separated directories are supported.
- `echostoryEngine.validateReferences`: warns for missing indexed resource references where the extension can infer the kind.
- `echostoryEngine.autoRefreshIndex`: keeps completion/reference data updated after file changes.
- `echostoryEngine.inlayHints.enabled`: shows compact inline reference-kind hints after resolved references.
- `echostoryEngine.formatter.useTabs`: defaults to `true` for legacy JSON5 formatting.

## Verification

```powershell
npm run check
npm run smoke
npm run package
```

[История изменений](docs/changelogs/README.md)
