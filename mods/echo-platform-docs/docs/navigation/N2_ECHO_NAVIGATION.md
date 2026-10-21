# N2 Echo Navigation

Echo Navigation 0.2.0 keeps `Npc.Move(...)` stable and moves backend choice into runtime configuration.

Backends:

- `echo_navigation:vanilla` - Minecraft `PathNavigation`;
- `echo_navigation:astar` - surface-aware A* plus custom NPC route motor.

Planning:

```text
Npc.Move
    -> Navigation request
    -> world snapshot
    -> surface graph
    -> A*
    -> NavigationRoute
```

Movement:

```text
NavigationRoute
    -> current traversal edge
    -> custom motor
    -> collision-aware entity movement
    -> actual progress
```

Echo Navigation does not import Echo NPC, Echo Animation or Echo Debug implementations. Adapter repositories remain on
public APIs.
