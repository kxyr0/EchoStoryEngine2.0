# Navigation Quickstart

Production pieces:

1. Install `echo-navigation`.
2. Install `echo-npc`.
3. Install `echo-npc-navigation-adapter`.
4. Keep the selected backend as `echo_navigation:vanilla`.

The backend can be inspected with:

```text
/echo_navigation backend
/echo_navigation settings
```

Manual movement through the navigation command:

```text
/echo_navigation move jdh:john_father 200 64 180
```

Authoring rule:

- A destination is a block coordinate.
- Runtime target is the center of that block on X/Z: `x + 0.5`, `z + 0.5`.
- Y is the feet surface level.
- Echo Navigation never teleports the NPC as a fallback.
- `PathNavigation.isDone()` is not treated as arrival by itself.

Current script status:

- Dialogue can use `Npc.Move(father, x, y, z)` inside a dialogue node; normal `Line(...)` continues while the NPC walks.
  Use `LineAfterMove(...)` for the first line that must wait for arrival.
- Trigger can use `Npc.Move("namespace:npc", x, y, z)` inside reactive callbacks; it is fire-and-forget.
- Story can use `Npc.Move("node_id", "namespace:npc", x, y, z, next: "next_node")` because Story nodes are explicit
  graph nodes.
