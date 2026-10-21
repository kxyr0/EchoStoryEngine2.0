# Состояние игрока

Server-authoritative predicates:

- `HealthAtMost(value)`, `HealthAtLeast(value)`
- `FoodAtMost(value)`, `FoodAtLeast(value)`
- `ExperienceLevelAtLeast(level)`
- `IsSneaking()`, `IsSprinting()`, `IsSwimming()`, `IsSleeping()`, `IsOnGround()`, `IsBurning()`

Events:

- `PlayerDamaged(amountAtLeast: 4.0, source: "minecraft:fall")`
- `PlayerHealed()`, `PlayerDied()`, `PlayerRespawned()`, `PlayerJumped()`
- `SneakStarted()`, `SneakStopped()`, `SprintStarted()`, `SprintStopped()`
- `SwimmingStarted()`, `SwimmingStopped()`, `SleepStarted()`, `SleepFinished()`
- `PlayerStartedBurning()`, `PlayerStoppedBurning()`

Validation: health is finite and `>= 0`, food is integer `0..20`, experience level is integer `>= 0`.
