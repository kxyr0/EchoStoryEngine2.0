# Changelog

## 1.9.0 - 2026-08-26

- Added first-class high-level Echo Dialogue authoring with inferred IDs, `Node`, `start Node`, symbolic `GoTo`, sparse `Speaker`, and `Formerly` migration helpers.
- Updated syntax highlighting, snippets, hover/signature help, navigation, rename, formatting coverage, and workspace indexing for the simplified form.
- Kept legacy DialogueResource authoring and tooling compatible.

## 1.8.5 - 2026-08-01

- Prioritizes Russian EchoScript hover summaries so methods such as `Npc.Spawn(...)` show Russian purpose text first.
- Expands EchoScript hover/signature/method completion coverage for player actor/vitals commands, NPC `WakeUp`/`SetForm`/`StopInteraction`, sleep `Blink`, cutscene/dialogue commands, environment helpers, NPC forms, blackspace/screen actions, `PlayerProfile(...)`, trigger callbacks/events/predicates, and NPC `NameColor(...)`/`nameColor(...)`.
- Adds current Echo Effects, Wardrobe, and high-level Face DSL method hovers/signatures/completions, including `Effect.ApplyToPlayer(...)`, `Movement.SpeedMultiplier(...)`, `Screen.Tint(...)`, `Sound.PlayAt(...)`, `WearableResource` init methods, `OutfitVariantResource` matching methods, `FaceRig(...)`, `BlinkControl(...)`, `TextureArea(...)`, and blink `Timing(...)`.
- Adds Echo Effects `VanillaEffect.Apply(...)` hover, signature help, argument completions, and snippets for applying vanilla Minecraft effects such as `minecraft:nausea` from `EffectResource`.
- Adds Russian hover/signature coverage for `Npc.WalkTo(...)`, `Npc.WalkToPlayer(...)`, `BlockPosition`, `Frame(...).Rotation(...)`, `Frame(...).Scale(...)`, facial-animation surface base classes, and camera-track text DSL keywords such as `schema`, `key`, `mode`, `target`, and `framing`.
- Fixes `Npc(..., nameColor:)` completion to insert a compiler-valid `NameColor(...)` helper instead of a raw string.
- Adds smoke coverage for the expanded Russian hover docs, signatures, signature help targets, and method completions.

## 1.8.4 - 2026-08-01

- Adds indexing, workspace symbols, completions, navigation, and rename coverage for `echo_player/profile_templates/**/*.player.es`, `echo_item/items/**/*.weapon.es`, and `echo_trigger/triggers/**/*.trigger.es`.
- Adds EchoScript support for new player/item/trigger DSL builders including `PlayerProfile(...)`, `Weapon(...)`, `TriggerResource`, `Id`, `Scope`, `Mode`, `Subscribe`, `StartStory`, `Wait`, `Area`, `Vector3`, shape helpers, `Any`/`All`/`Not`, `PlayerSleep.*`, trigger predicates, and trigger events.
- Adds snippets and signature help for `PlayerProfileTemplate`, `WeaponResource`, `TriggerResource`, `PlayerSleep.Confirmed`, and trigger wait flows.
- Adds static completions for player profile modes, skin modes/models, weapon rarity/equipment slots, trigger scope/mode, and stay policy values.
- Expands smoke coverage for the new resource roots, id extraction from string fields, trigger `Id(...)` declarations, `StartStory(...)` references, grammar, snippets, diagnostics, completions, and signature help.

## 1.8.3 - 2026-08-01

- Adds indexing, symbols, completions, and navigation for `echo_story/environments/**/*.es` and `echo_npc/forms/**/*.{form.es,es}` resources.
- Adds EchoScript support for new engine intrinsics: `Environment.Set`, `Dialogue.Load`, `Screen.SetBlack`, `Story.Set`, `Player.SpawnAt`, hotbar/sit/teleport helpers, `Npc.SpawnAt`, `Npc.SleepAt`, `Npc.Interact`, `Npc.StopInteraction`, `Npc.Stop`, `Blackspace.Create`, and `Sleep.*`.
- Adds `EnvironmentResource` and `NpcFormResource` snippets, plus updated NPC default-form authoring.
- Updates grammar, hovers, signature help, smoke tests, README, and package version for the new engine surface.

## 1.8.2 - 2026-07-28

- Adds EchoScript hover/signature coverage for current modular builders including `Story(...)`, graph `Action(...)`, `Npc(...)`, `AnimationProfile(...)`, `AnimationRig(...)`, `AnimationClip(...)`, `FacialAnimation(...)`, `FacePose(...)`, and `NpcAnimationBinding(...)`.
- Adds named-argument hover docs for new graph/action and animation fields such as `type`, `animation`, `target`, `await`, `rig`, `profile`, `clip`, `surfaces`, and `skin`.
- Accepts current dialogue line face-animation fluent helpers such as `.SetAnimationFace(...)`, `.StopFaceAnimation()`, and `.SetAnimationFaceClear()` in completions and diagnostics.
- Stops warning on current PascalCase resource-id constants such as `private FaceAnimationId FatherAngryBrows = "..."` and high-level face DSL component declarations such as `Bone Glasses;`.
- Expands EchoScript grammar highlighting for modular resource keywords, resource namespaces, and `*Id` typed declarations.

## 1.8.1 - 2026-07-28

- Replaces primary EchoScript snippets for new authoring with current `StoryResource`, `DialogueResource`, `NpcResource`, animation, face, and NPC animation binding resource templates.
- Removes old `EventResource` and `SceneResource` guidance from the main EchoScript snippets.
- Treats current resource builder ids such as `Story("...")`, `Dialogue("...")`, `AnimationClip("...")`, `Npc(id: "...")`, and high-level `face Name("...")` as declarations for references and workspace rename.
- Adds navigation/completion coverage for new resource arguments such as `rig`, `profile`, `clip`, `actor`, `skin`, and for `Skin(...)`, `SkinHead(...)`, and `Portrait(...)` calls.
- Adds smoke coverage for the new declaration, rename, skin-reference, and completion behavior.

## 1.8.0 - 2026-07-28

- Starts the migration from the old `mods\echostoryengine` JSON5 layout to the current modular Echo Story workspace.
- Changes the default index root to `D:\JDHSeason\mods` and the default data directory to `echo-story-content-pack/src/main/resources/data`.
- Indexes current EchoScript resource roots for stories, dialogues, NPCs, NPC animation bindings, animation definitions, animation profiles, animation rigs, animation clips, face rigs, face poses, face animations, and triggers.
- Extracts ids from current resource builders such as `Story(...)`, `Dialogue(...)`, `Npc(id: ...)`, `Animation(...)`, `FacialAnimation(...)`, `AnimationProfile(...)`, `AnimationRig(...)`, `AnimationClip(...)`, `FacePose(...)`, `NpcAnimationBinding(...)`, and high-level `face Name("id")`.
- Adds EchoScript named-argument reference navigation for new resource arguments such as `dialogueId`, `definitionId`, `actorId`, `animationId`, `faceAnimationId`, and `storyId`.
- Treats new StoryResource `Set("node", variable: "...")` calls as state-key writes without indexing the node id as a state key.
- Updates README, AGENTS, manifest strings, smoke tests, and package version for the new engine target.

## 1.7.1 - 2026-07-01

- Cleans remaining snippet display names so lifecycle/NPC snippets show as `StartEffects`, `CompleteEffects`, `AbortEffects`, and `Npc.Spawn` instead of `EchoScript ... Method` / `EchoScript NPC Spawn`.
- Corrects `Npc.Spawn` hover/signature help to match the compiler-supported form `Npc.Spawn(npc, dimension, x, y, z, yaw?, pitch?)`.
- Translates local EchoScript completion details such as `local method` and indexed-reference labels to Russian.
- Adds smoke coverage for Russian local-method completion details and compiler-accurate `Npc.Spawn` signature help.
- Bumps the local VSIX/package version to `1.7.1`.

## 1.7.0 - 2026-07-01

- Replaces confusing EchoScript snippet prefixes such as `ese-start-effects`, `ese-complete-effects`, and `ese-npc-spawn` with visible method/resource names such as `StartEffects`, `CompleteEffects`, `AbortEffects`, and `Npc.Spawn`.
- Adds an EchoScript Signature Help provider for common runtime helpers, including `Npc.Spawn(...)`, `Npc.SpawnNearPlayer(...)`, `Npc.Move(...)`, `Npc.Path(...)`, `Npc.HoldItem(...)`, `Npc.PlayAnimation(...)`, `Navigation.SetTarget(...)`, `World.SetNavigationTarget(...)`, and `Quest.Update(...)`.
- Adds Russian signature-help documentation for `Npc.Spawn(...)` overloads and active parameters while typing after `(` and `,`.
- Switches EchoScript hover summaries and navigation argument hover text to Russian while keeping code signatures unchanged.
- Adds smoke coverage for clean method snippet labels, `Npc.Spawn` signature help, navigation signature help, and Russian hover/signature documentation.
- Bumps the local VSIX/package version to `1.7.0`.

## 1.6.9 - 2026-07-01

- Adds smoke coverage proving every registered EchoScript method hover has a short purpose summary.
- Adds regression coverage that inlay hints, hovers, completions, and workspace symbol containers do not render long indexed file paths.
- Exposes the pure EchoScript hover-word registry for smoke tests without changing runtime behavior.
- Bumps the local VSIX/package version to `1.6.9`.

## 1.6.8 - 2026-07-01

- Removes long target file paths from inline inlay hints; resolved references now show compact labels such as `state`, `dialogue`, `scene`, or `camera`.
- Removes indexed source paths from reference/localization hovers while keeping Go to Definition and document links available for opening the source.
- Adds short purpose summaries to EchoScript method hovers and adds fallback summaries so existing method hovers are not just raw signatures.
- Expands navigation hover coverage for `Navigation.SetTarget`, `World.SetNavigationTarget`, `ClearTarget`, and named navigation arguments such as `dimensionId:`, `profile:`, `icon:`, and `label:`.
- Adds JSON5 field hover docs for navigation target fields including `targetId`, `x`, `y`, `z`, `label`, `iconId`, `icon`, `profile`, and `navigationProfile`.
- Bumps the local VSIX/package version to `1.6.8`.

## 1.6.7 - 2026-07-01

- Adds shared EchoScript static-symbol metadata so completions and diagnostics use the same supported values for `QuestStatus.`, `Layer.`, `Hand.`, `Items.`, `Portraits.`, `States.`, and `OldFriend.`.
- Adds `ESL0610` Error Lens-compatible diagnostics for unsupported static symbols such as `Layer.Body`, `Hand.Mainhand`, `Items.Item`, `Portraits.Default`, `QuestStatus.Failed`, and `States.Walk`.
- Adds smoke coverage for valid and invalid static-symbol diagnostics plus installed-copy packaging of the shared static-symbol module.
- Bumps the local VSIX/package version to `1.6.7`.

## 1.6.6 - 2026-07-01

- Adds static namespace dot completions for compiler-known EchoScript symbols including `QuestStatus.`, `Layer.`, `Hand.`, `Items.`, `Portraits.`, `States.`, and `OldFriend.`.
- Adds context-aware static suggestions for named arguments such as `status:`, `layer:`, and `hand:` inside the helper calls that accept those symbols.
- Updates EchoScript snippets/completion placeholders to avoid unsupported symbols such as `Layer.Body`, `Hand.Mainhand`, `Items.Item`, and `Portraits.Default`.
- Expands hovers for built-in static namespaces and adds smoke coverage for the new dot and argument completions.
- Bumps the local VSIX/package version to `1.6.6`.

## 1.6.5 - 2026-07-01

- Expands `Scene().` completions with compiler-supported lifecycle and alias members such as `OpeningCamera`, `Alias`, `SpeakerAlias`, `StartEffects`, `CompleteEffects`, and `AbortEffects`.
- Adds EchoScript hover and syntax-highlight coverage for `BuildQuest`, `BuildAnimation`, `BuildCharacter`, `AbortEffects`, `Money`, `Field`, `Obj`, `Frame`, `FacePose`, and `CameraSample`.
- Adds a scene `AbortEffects()` snippet and smoke coverage for lifecycle/build helper highlighting.
- Removes noisy default build-method preference diagnostics for compiler-supported `Build()` fallback, keeping current real mod `.es` files free of false `ESL0303` reports.
- Bumps the local VSIX/package version to `1.6.5`.

## 1.6.4 - 2026-07-01

- Adds argument-aware EchoScript reference completions so calls such as `Scene.Start(...)`, `Dialogue.Start(...)`, `Npc.Path(...)`, `Npc.PlayAnimation(...)`, `CameraPathCue(...)`, `Line(...)`, and named arguments such as `thenDialogue:`, `cinematic:`, `face:`, and `anim:` suggest only the relevant indexed resources.
- Adds indexed namespace completions for plural resource refs such as `Dialogues.`, `Scenes.`, `Speakers.`, `Animations.`, `FaceAnimations.`, and `CameraPaths.`.
- Expands EchoScript completion snippets for current NPC item, animation, and vehicle helpers including `Npc.HoldItem`, `Npc.ClearHeldItem`, `Npc.PlayAnimation`, `Npc.StopAnimation`, and `Npc.EnterVehicle`.
- Bumps the local VSIX/package version to `1.6.4`.

## 1.6.3 - 2026-07-01

- Extends EchoScript member-completion inference from local method return types, so calls such as `PayMoney().` and `SashaBehavior().` offer the same builder-specific members as `Action().` and `Behavior().`.
- Infers builder context for explicitly typed variables and variables initialized from typed local methods, such as `private ActionDef cachedAction; cachedAction.` and `var action = PayMoney(); action.`.
- Extends unknown-member diagnostics to those inferred local-method and typed-variable contexts, while keeping real mod `.es` files free of false `ESL060x` reports.
- Bumps the local VSIX/package version to `1.6.3`.

## 1.6.2 - 2026-07-01

- Expands EchoScript context-aware member completions after `.` for real builder contexts including `Animation()`, `FaceAnimation()`, `CameraPath()`, `Cutscene()`, `Localization()`, `Behavior()`, `Schedule()`, `Line()`, `Choice()`, `Node()`, `Bone()`, `Part()`, and `StoryIndex()`.
- Adds named-argument completions inside common EchoScript calls such as `Quest.Update(...)`, `Npc.Path(...)`, `Navigation.SetTarget(...)`, `BlackScreen.Show(...)`, `Vehicle.Trip(...)`, and `Vehicle.Sequence(...)`, hiding arguments already used in the current call.
- Fixes `Navigation.SetTarget` snippets and hovers to use the current compiler-supported `x:`, `y:`, `z:`, `dimensionId:` form.
- Expands EchoScript grammar keywords/types and adds snippets for navigation targets, quest updates, and scene resources.
- Bumps the local VSIX/package version to `1.6.2`.

## 1.6.1 - 2026-06-30

- Adds EchoScript completion provider for `.es`: local methods/fields, explicit `StateRef` fields, indexed dialogues/scenes/NPCs/speakers/animations/camera paths, state keys, and member snippets.
- Adds `Vehicle.EnsureSpawn`, `Vehicle.Trip`, `Vehicle.Sequence`, `Npc.EnterVehicleAt`, `Npc.EnterNearestVehicle`, and `Npc.LeaveVehicle` completions, snippets, hovers, syntax highlighting, semantic tokens, and navigation coverage.
- Updates dialogue snippets and hovers to prefer class-level `private NodeDef[] dialogueNodes = [...]` with `Nodes()` returning that field.
- Adds JSON5 value completions for vehicle backend/category and cinematic completion/collision policy ids.
- Bumps the local VSIX/package version to `1.6.1`.

## 1.6.0 - 2026-06-30

- Updates EchoScript dialogue authoring for separate `Init()`, `NodeDef[] Nodes()`, and `Effect[] StartEffects()` / `Effect[] CompleteEffects()` methods instead of dialogue config/effect chains.
- Adds formatter support that collapses multiline `Line(...)` statements to one line.
- Updates snippets, hovers, diagnostics, navigation, and smoke tests for explicit `StateRef` fields, camelCase/PascalCase naming rules, dynamic state changes (`State.Add(...)` / `stateRef++`), typed locals/fields, assignment, and expanded `Npc.Move(...)` named arguments.
- Bumps the local VSIX/package version to `1.6.0`.

## 1.5.3 - 2026-06-30

- Adds EchoScript Go to Definition for typed state refs such as `RoomDone` and `OldFriend.Paid`, resolving `Set(...)` to write keys and `exists(...)` / `if (...)` to read keys.
- Adds smoke coverage for typed state-ref definition targets.
- Bumps the local VSIX/package version to `1.5.3`.

## 1.5.2 - 2026-06-30

- Extends JSON5 Rename Symbol to indexed reference fields, shortcut refs, and resource `id` declarations, using the same workspace rename path as EchoScript string refs.
- Keeps local dialogue-node rename as the priority path inside dialogue JSON5 files.
- Bumps the local VSIX/package version to `1.5.2`.

## 1.5.1 - 2026-06-30

- Extends EchoScript F2 rename to workspace edits for typed identifiers across `.es` files and string resource references across indexed `.es` / `.json5` story files.
- Fixes EchoScript string-reference context detection so `Scene.Start("...")`, `Dialogue.Start("...")`, and typed NPC string refs can participate in Go to Definition and Rename Symbol.
- Adds rename scanner smoke coverage for EchoScript typed refs, EchoScript string refs, and JSON5 shortcut refs.
- Bumps the local VSIX/package version to `1.5.1`.

## 1.5.0 - 2026-06-30

- Adds the bundled `icon.png` to the VS Code extension manifest.
- Registers Go to Definition and Rename Symbol for EchoScript `.es` files.
- Adds EchoScript navigation for local methods, local dialogue node ids, typed scene refs such as `Chapter1RoomToHomeRelax`, and typed NPC refs such as `Sasha`.
- Registers semantic tokens for EchoScript and highlights typed helper refs in calls such as `Scene.Start(...)`, `Npc.Spawn(...)`, `Set(...)`, and `exists(...)`.
- Indexes typed EchoScript state refs such as `RoomDone` and `OldFriend.Paid` as state keys for workspace completions/search.
- Fixes false EchoScript diagnostics for named `if:` arguments and multiline `return Node(...).Line(...)` chains.
- Aligns snippets and linter hints with compiler-supported syntax: `Set(...)`, fluent `.StartEffects(...)` / `.CompleteEffects(...)`, `Line(...)`, `Choices { ... }`, typed `Npc.Spawn(npc, dimension, x, y, z)`.
- Removes hints for unsupported EchoScript sugar such as direct `memory.* = ...` assignments, `StartEffects { ... }` blocks, and alias-line shorthand.
- Bumps the local VSIX/package version to `1.5.0`.

## 1.4.1 - 2026-06-30

- Fixes false `Return statements must end with ;` and variable-semicolon diagnostics on multiline EchoScript fluent chains.
- Expands EchoScript method hovers with overloads and optional arguments for resource builders, dialogue methods, effects, NPC helpers, scene helpers, animations, and sequence helpers.
- Adds extension-side highlighting, snippets, and linter hints for `StartEffects { ... }`, `CompleteEffects { ... }`, memory assignments, typed `Scene.Start(...)`, alias lines, and typed `Npc.Spawn/Despawn(...)`.
- Bumps the local VSIX/package version to `1.4.1`.

## 1.4.0 - 2026-06-30

- Adds EchoScript diagnostics `ESL0420..ESL0422` for deprecated leading-dot local dialogue calls: `.Line(...)`, `.Choice(...)`, and `.NextNode(...)`.
- Updates the EchoScript formatter to rewrite those context-local calls to `Line(...)`, `Choice(...)`, and `NextNode(...)`.
- Updates dialogue snippets and syntax highlighting for `NodeDef` methods with `Choices { ... }`.
- Bumps the local VSIX/package version to `1.4.0`.

## 1.3.0 - 2026-06-30

- Updates EchoScript syntax support for C#-first resources: `var`, explicit local types, `new`, `Vector3`, `EventDef`, `BuildEvent`, `AreaEnterDelayed`, `Npc.Move`, `thenDialogue`, and `if / else if / else`.
- Adds EchoScript linter diagnostics `ESL0301..ESL0310` for local variable ordering/shadowing and deprecated event/NPC authoring forms.
- Updates snippets to prefer `BuildEvent()`, `new Event()`, `new Vector3(...)`, typed symbols, `thenDialogue`, and `Npc.Move(...)`.
- Bumps the local VSIX/package version to `1.3.0`.

## 1.2.0 - 2026-06-29

- Adds EchoScript diagnostics to the extension diagnostic collection so Error Lens can show `.es` linter and syntax issues inline.
- Adds EchoScript method-signature hovers for common builders and effect helpers.
- Adds a simple EchoScript formatter provider and enables the existing Format Current command for `.es` files.
- Adds smoke coverage for EchoScript diagnostics, formatter output, and signature metadata.

## 1.1.0 - 2026-06-29

- Updates EchoScript snippets for 0.3 condition spelling: `RunIf(...)` for event gates and `VisibleIf(...)` for action visibility.
- Keeps `if:` choice snippets and `If`/`Elif`/`Else` conditional effect snippets as the supported branch vocabulary.
- Bumps the local VSIX/package version to `1.1.0`.

## 1.0.0 - 2026-06-28

- Initial EchoStory Engine authoring release.
- Adds JSON5 syntax highlighting, bracket matching, snippets, formatting, completion, hovers, document symbols, diagnostics, and workspace id indexing.
- Adds Go to Definition, clickable document links, shared reference resolution, command type completions, command diagnostics, quick fixes, and richer root/property snippets.
- Adds semantic tokens, workspace symbol search, dialogue node/choice completions, speaker alias completions, speaker-alias diagnostics, and dialogue graph diagnostics.
- Adds Find References, reference target inlay hints, and safe Rename Symbol support for local dialogue node ids.
- Adds unquoted JSON5 boolean literal completions for common toggle fields such as `enabled`, `visible`, and `fireOnce`.
- Adds localization key indexing, completions, workspace symbols, missing-key diagnostics, hovers, document links, and Go to Definition for explicit localization fields.
- Adds workspace signal/state key indexing and completions for trigger keys, `on...Signal` fields, and state-key authoring fields.
- Adds quest status visuals for `quest_update`: colored badges, overview ruler markers, hover details, and CodeLens labels for active/completed/incomplete/failed/locked quests.
- Adds Marketplace packaging and publishing scripts.
- Adds EchoScript `.es` language registration, syntax highlighting, snippets, and shallow workspace indexing for migrated story resources.
- Updates conditional authoring hints to prefer `if`/`If`/`Elif`/`Else` instead of legacy `when` spellings.
