# A* Backend

`echo_navigation:astar` is a second backend, not a replacement for `echo_navigation:vanilla`.

Pipeline:

```text
NavigationSnapshot
    -> AStarWorldSnapshotFactory
    -> NavigationSurfaceSampler
    -> AStarRoutePlanner
    -> NavigationRoute
    -> NpcNavigationRouteMotor
```

Rules:

- the planner lives in `navigation-core` and imports no Minecraft or Forge classes;
- the Forge snapshot compiles Minecraft collision shapes into immutable route nodes and edges;
- the backend does not pass the route to `Mob.getNavigation().moveTo(...)`;
- cancellation stops the route motor and clears any old vanilla navigation path.
