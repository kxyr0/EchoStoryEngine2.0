# Navigation Project Map

Repository: `D:\JDHSeason\mods\echo-navigation`

| Module | Responsibility |
| --- | --- |
| `navigation-contracts` | Typed IDs, request, destination, status, outcome, failure, route nodes, traversal edges, snapshot, settings and diagnostics. |
| `navigation-core` | Lifecycle, validation, idempotency, ownership, progress, stuck rules and Minecraft-free A* planning. |
| `navigation-forge-api-1.20.1` | Public Forge service, query, backend and agent resolver APIs. |
| `navigation-vanilla-1.20.1` | Vanilla `PathNavigation` backend and Forge-backed `echo_navigation:astar` snapshot/motor implementation. |
| `navigation-forge-1.20.1` | Production Forge mod, host, commands, settings and SavedData writes. |
| `navigation-testmod-1.20.1` | Development-only testmod placeholder. |

Related repository: `D:\JDHSeason\mods\echo-npc-navigation-adapter`

Backend flow:

```text
Npc.Move
    -> Navigation request with active backend id
    -> echo_navigation:vanilla or echo_navigation:astar
    -> pinned movement snapshot
```

A* flow:

```text
Npc.Move
    -> world snapshot
    -> surface graph
    -> A*
    -> NavigationRoute
    -> custom route motor
    -> entity collision movement
```

Related Echo NPC patch:

- `npc-forge-api-1.20.1` exposes `NpcNavigationBody`.
- `npc-forge-1.20.1` resolves a materialized `EchoNpcEntity` to that public body.
- `EchoNpcEntity` now keeps vanilla AI enabled and has non-zero movement speed.
- N5 form changes keep the same `ActorId`; navigation uses the same subject id
  after `Npc.SetForm` and relies on Echo NPC for form-dimension placement checks.
