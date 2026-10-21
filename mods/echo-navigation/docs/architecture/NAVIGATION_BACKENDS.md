# Navigation Backends

A backend is a provider behind the public Forge API.

Backend contract:

- has a stable `NavigationBackendId`;
- declares a descriptor and availability;
- starts movement from an immutable snapshot;
- ticks only the pinned movement;
- cancels by `NavigationMoveId`;
- reports terminal outcome without mutating core runtime maps.

Backends do not own:

- NPC identity;
- script waiting;
- command idempotency;
- persistence;
- backend selection policy.

The production Forge mod registers:

- `echo_navigation:vanilla` - Minecraft `PathNavigation` planner and motor.
- `echo_navigation:astar` - Echo surface graph, A* planner and custom NPC route motor.

Backend selection comes from `<gameDir>/config/echo-navigation-server.toml`. Switching backend affects new
movements only because each active movement stores the backend ID used at creation.
