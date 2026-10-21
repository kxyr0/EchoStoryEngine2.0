# Echo Core

Forge **1.20.1 / 47.x**, Java 17. A chapter-save backend with an optional adapter for **FancyMenu 3.9.14**. The requested blank chapter screen is provided as a surface for a user-authored FancyMenu layout. No layouts, buttons, visible text, or story progress are created. FancyMenu action/placeholder names are editor registration metadata only.

## Chapter 1

The supplied map is the template: `<minecraft>/saves/world`. New runs use a separate folder: `<minecraft>/saves/echo_core_chapter_1`. Installing the mod creates no playthrough and does not reset either world.

- **Start:** create a template copy if no save exists, then load it. If a save already exists, keep it and continue.
- **Continue:** load the same saved singleplayer world. A missing save returns `NO_SAVE`, logs a clear reason, and changes no files.
- **Restart:** prepare a fresh template copy and replace only this chapter's owned playthrough, then load it. With no old save, this creates the first run.

Leave the current world before these actions. They return `WORLD_OPEN` while any world/server connection is open. Saves, template locks, invalid data, duplicate actions, and unknown IDs return explicit result codes. Successful operations use Minecraft's existing world-loading flow; the chapter menu is separate from the save actions.

Action `echo_core_open_chapter_menu` opens a blank black screen without creating, loading, or resetting a save. Customize screen `io.github.kxyr0.echocore.client.ChapterMenuScreen` in FancyMenu. Add your own buttons using chapter start/continue/restart actions with value `chapter_1`. Escape returns to the preceding screen. No built-in widgets or labels are present.

## Hooking up FancyMenu manually

The mod registers these generic actions through the [official FancyMenu ActionRegistry](https://github.com/Keksuccino/FancyMenu-Dev-Docs/wiki/Actions):

| Action ID | Value |
| --- | --- |
| `echo_core_open_chapter_menu` | No value |
| `echo_core_chapter_start` | `chapter_1` |
| `echo_core_chapter_continue` | `chapter_1` |
| `echo_core_chapter_restart` | `chapter_1` |

Add the desired action to your own action script. It works from a main-menu layout without a server or an open world. Do not use FancyMenu's server-command action for these main-menu operations. There is no automatically created script/layout.

These generic [placeholders](https://github.com/Keksuccino/FancyMenu-Dev-Docs/wiki/Placeholders) expose data for your own requirements and presentation:

```text
{"placeholder":"echo_core_chapter_state","values":{"chapter":"chapter_1"}}
{"placeholder":"echo_core_chapter_save_exists","values":{"chapter":"chapter_1"}}
{"placeholder":"echo_core_chapter_template_exists","values":{"chapter":"chapter_1"}}
{"placeholder":"echo_core_chapter_can_continue","values":{"chapter":"chapter_1"}}
{"placeholder":"echo_core_chapter_last_result","values":{"chapter":"chapter_1"}}
{"placeholder":"echo_core_chapter_last_error","values":{"chapter":"chapter_1"}}
```

Boolean placeholders return `true` / `false`. State is one of `NO_SAVE`, `READY`, `ACTIVE`, `BUSY`, `INVALID_SAVE`, `UNKNOWN_CHAPTER`. `save_exists` indicates the presence of a folder, even when corrupt; use `can_continue` to decide whether Continue is currently available. `last_error` is empty when no error occurred. Errors are exposed and logged rather than displayed in an automatically created UI.

## Commands and Java API

Client commands registered with Forge (available through in-game chat):

```text
/echo_core chapter chapter_1 status
/echo_core chapter chapter_1 start
/echo_core chapter chapter_1 continue
/echo_core chapter chapter_1 restart
```

Operations invoked from a running world are rejected; these chat commands are mainly diagnostic. Use the registered FancyMenu actions or Java API from the main menu to actually load/reset a world.

```java
var chapters = io.github.kxyr0.echocore.api.ChapterApi.get();
var state = chapters.snapshot("chapter_1");
chapters.continueChapter("chapter_1").thenAccept(result -> {
    // result.code() == NO_SAVE, LOAD_REQUESTED, ...
});
AutoCloseable subscription = chapters.onResult(result -> {
    // ACTIVE confirms the player joined the chapter world.
});
```

`start`, `continueChapter`, and `restart` return completion stages. `LOAD_REQUESTED` means loading was handed to Minecraft; it does not mean the player has joined yet. `ACTIVE` is reported afterwards through the result hook. A cancelled/failed loading flow reports `LOAD_CANCELLED`. Copying runs on a dedicated worker; Minecraft operations and result callbacks run on its client thread. A listener exception is logged without changing the operation result.

## Adding chapters

On first launch, `config/echo-core/chapters.properties` is created with:

```properties
chapter_1.template=saves/world
```

To add chapters, configure additional map templates and restart Minecraft:

```properties
chapter_2.template=templates/chapter_2
chapter_3.template=templates/chapter_3
```

Paths are relative to the Minecraft folder or absolute. Use forward slashes in Windows absolute paths in this Java properties file. Save folders are derived automatically as `echo_core_<chapter ID>`; configuration cannot redirect deletion to arbitrary directories. All chapters share the same implementation.

## Save behavior and limits

Restart copies the full template baseline, including its map, player data, datapacks, server config, and mod data. It excludes the template's root `session.lock` and Echo Core ownership marker. The template should represent the intended beginning of that chapter and must not be open in another instance.

The new copy is validated in a staging directory before the old run is moved. Promotion failures attempt to restore the previous run. Cleanup failures leave a `.echo-core-*.previous` folder rather than deleting the newly created save. Staging/previous folders are internal transaction files; they are not extra playable chapters. A power loss between filesystem moves may require manual recovery of a previous folder; this minimal version does not include a crash-recovery journal.

Only directories marked `.echo-core-owner` for the same chapter can be replaced/deleted. Unmarked preexisting folders with the managed name are rejected as `FOREIGN_SAVE`. Symlinks/junctions are rejected. `level.dat` is checked as Minecraft NBT before loading. The backend does not implement completion, checkpoints, progress percentages, or story variables.

## Build and validation

```powershell
.\gradlew.bat check build
```

Output: `build/libs/echo-core-0.2.0.jar`. FancyMenu and Konkrete are compile-only; they are not bundled. The default build finds the user's installed jars in the Prism instance; override with `-Pfancymenu_mods_dir=<mods folder>` on another machine. FancyMenu can be absent at runtime; the API/commands remain available.

Tests cover the save lifecycle, isolated chapter folders, absent/corrupt saves, invalid templates, ownership, locked worlds, config expansion, and calls against the actual installed FancyMenu classes. Launching the chapter inside the real modpack still requires an in-game smoke test.

## Story feature unlocks (0.2.0)

Feature flags are server-authoritative, per player UUID, and stored in the world's `data/echo_core_features.dat`. Missing flags are false. Client snapshots are refreshed on login, respawn, dimension changes, and mutation; logout clears client state. Continue preserves flags. Restart restores the template baseline, including any flags deliberately stored in the template. Keep the template flags unset/false for a locked new playthrough.

With Echo Core installed, Echo Thought Space 0.7.0 requires `thought_space_unlocked` to open the canvas (P or command); setting it false closes an open canvas. Echo Magic 0.8.18 requires `magic_unlocked` for gesture input, drawing slowdown, and server-side casts (including cast commands). All spell schools share this gate. Without Echo Core these two mods retain their standalone behavior. This gate does not remove learned spells or implement individual school unlocks. Already created spell effects are not retroactively removed.

Operator commands (permission level 2), command blocks, and server console:

```text
/echo_core flag @s thought_space_unlocked true
/echo_core flag @s magic_unlocked true
/echo_core flag @s magic_unlocked false
/echo_core flag @s magic_unlocked
```

Use `@a` or an explicit player name instead of `@s` for a server console/command block. The last form reads the current value. Arbitrary flags may be stored using lowercase letters, digits and underscores, up to 64 characters and 256 flags per player; an additional mechanic must call the FeatureFlags API to enforce its own flag.

Echo Story Engine 1.8.0 is optional. When present, Echo Core registers a public extension action `echo_core:set_flag`. Add it at the desired point in your existing story; no story files are modified automatically:

```csharp
Action("unlock_canvas",
    type: "echo_core:set_flag",
    player: "@a",
    flag: "thought_space_unlocked",
    value: true,
    next: "next_node");

Action("unlock_magic",
    type: "echo_core:set_flag",
    player: "@a",
    flag: "magic_unlocked",
    value: true,
    next: "next_node");
```

`player` is required and accepts an online player name, an explicit UUID (also offline), or `@a` for all currently online players. `@s` is not accepted by a Story action because Story has no implicit Minecraft command sender. Boolean values must be exactly true/false. Set operations are idempotent and recovery reapplies the same value. Invalid targets/values report a rejected Story action.

Java hooks, for integrations on the server thread:

```java
FeatureFlags.set(playerUuid, "magic_unlocked", true);
boolean serverAllowed = FeatureFlags.serverAllowed(playerUuid, "magic_unlocked");
boolean clientAllowed = FeatureFlags.clientAllowed(playerUuid, "magic_unlocked");
```

A multiplayer server must also install Echo Core for the feature-state channel. No new UI or layouts accompany these flags.

## Feature unlock shorthand (Story Engine 1.9.0)

Inside an ES2 `OnStart()` intent section:

```es
Feature.Unlock("canvas");
Feature.Unlock("magic");
Feature.Lock("magic");
Feature.Set("magic", true);
```

These frontend calls lower to the external `echo_core:set_flag` contract. Echo Core >=0.2.0 remains responsible
for state, save persistence, and enforcing access in gameplay mods. `canvas` aliases `thought_space_unlocked`;
`magic` aliases `magic_unlocked`. Any other valid flag identifier passes through unchanged. Unlock/Lock supply
true/false, the default target is all online players (`@a`), and graph links are inferred in statement order.
Use `Feature.Unlock("canvas", player: "Alex")` for a named player or provide an explicit UUID. Generic Action
and full flag names remain supported. This shorthand introduces no gameplay state into Story core.
