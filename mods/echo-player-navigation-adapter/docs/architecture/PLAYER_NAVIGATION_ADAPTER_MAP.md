# Player Navigation Adapter map

## Responsibilities

- `PlayerActorId` → `NavigationSubjectId`;
- `PlayerActorMovementBridgeRegistrationEvent` listener;
- Navigation service preflight;
- delegation to `PlayerActorMotor`;
- `Player.Stop` propagation.

## Dependencies

Allowed:

- `player-actor-contracts`;
- `player-actor-forge-api-1.20.1`;
- `navigation-contracts`;
- `navigation-forge-api-1.20.1`.

Forbidden:

- Echo Navigation implementation;
- Echo Player Actor implementation;
- fake player or `Mob` wrapper for `ServerPlayer`.
