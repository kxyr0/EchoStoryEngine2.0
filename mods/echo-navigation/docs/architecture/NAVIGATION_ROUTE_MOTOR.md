# Navigation Route Motor

`NpcNavigationRouteMotor` executes `NavigationRoute` edges directly.

It uses bounded `Entity.move(MoverType.SELF, delta)` calls, so Minecraft collision resolves the actual result. It tracks
blocked ticks and requests bounded replanning instead of creating a second movement.

For descending edges, progress is measured along the route edge. Decreasing Y is normal movement for `STEP_DOWN`,
`STAIR_DOWN` and `SLAB_DOWN`.
