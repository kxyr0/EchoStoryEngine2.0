# Player Opening PA0

`Player.SpawnAt(x, y, z, yaw, pitch)` moves the current server-side `ServerPlayer`
to the requested position and rotation. It is not a respawn point writer, does not
change game mode, and does not create a fake player. The command rejects
non-finite coordinates, world-border violations, invalid build height, and blocked
spawn collision.

Opening scene order for `jdh:chapter_1_opening`:

1. `echo_player_actor:spawn_at`
2. `echo_player_actor:set_hotbar_item`
3. `echo_player_actor:select_hotbar_slot`
4. `echo_player_actor:sit_at`
5. `echo_dialogue:play` with `jdh:start_story`

Dialogue must stay after all actor commands. Durable story flags are owned by
Echo Story Engine: `chapter_1.opening_started` and
`chapter_1.opening_completed`.

`SitAt` requires exactly one registered `PlayerActorSeatProvider` that supports
the target block. PA0 includes an optional Refurbished Furniture adapter for
`refurbished_furniture:*_chair` blocks; unsupported chairs fail with a diagnostic
code instead of pretending that the player sat down.

Temporary content TODOs:

- Replace `minecraft:book` in `jdh:chapter_1_opening` with `jdh:story_book` when
  the item is added.
- Replace or add the missing `jdh:story_telling_sister_005` cutscene asset before
  wiring a `Cutscene.Start(...)` call into `jdh:start_story`.
