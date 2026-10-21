# Changelog

## 0.4.0

- Added speed in blocks per second to `Npc.Move(...)`.
- Added `Npc.Sprint(...)` with sprint gait and run-animation selection.
- Added `Vector3(...)`, positional or named speed and optional `syncAnimation` authoring.

## 0.3.0

- Changed Dialogue `Npc.Move(...)` to start NPC movement and immediately resume normal `Line(...)` dialogue text.
- Added Dialogue `LineAfterMove(...)` arrival waits through `echo_navigation:npc_move_arrival`.
- Updated the dialogue example to show walking-and-talking lines before the arrival line.

## 0.2.0

- Added Story EchoScript `Npc.Move(...)` and `Npc.Stop(...)` sugar through Story external action nodes.
- Added Dialogue external command dispatch for `Npc.Move(...)` and `Npc.Stop(...)`.
- Added Trigger callback dispatch for `Npc.Move(...)` and `Npc.Stop(...)`.
- Added example Dialogue, Trigger and Story `.es` files.

## 0.1.0

- Created standalone `echo-npc-navigation-adapter`.
- Added NPC navigation subject ID mapping.
- Added Echo NPC materialized body resolver through public Forge API.
- Registered NPCs as Echo Navigation agents.
- Added Story external actions for NPC move and stop.
- Added deterministic movement, command and owner IDs for Story action dispatch.
- Added adapter architecture documentation.
