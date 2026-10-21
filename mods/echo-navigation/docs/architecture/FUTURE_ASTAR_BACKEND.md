# Future A* Backend

Future backend ID: `echo_navigation:astar`.

Not implemented in N1.

The future backend must reuse:

- `NavigationRequest`;
- `NavigationMoveId`;
- `NavigationStatus`;
- `NavigationOutcome`;
- `NavigationFailure`;
- `NavigationBackendRegistrationEvent`;
- the same script and adapter APIs.

It may own authored navigation graphs, roads, sidewalks or crosswalks. It must not require content authors to change
`.es` files when switching away from `echo_navigation:vanilla`.

Do not add a fake A* backend that internally calls vanilla `PathNavigation`.
