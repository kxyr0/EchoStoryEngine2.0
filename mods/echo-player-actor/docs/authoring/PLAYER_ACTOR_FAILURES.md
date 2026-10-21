# Player Actor failures

Основные отказы:

- `PLAYER_ACTOR_CONTEXT_MISSING`: host не смог определить текущего actor.
- `PLAYER_ACTOR_OFFLINE`: игрок не online на сервере.
- `PLAYER_ACTOR_CONTROL_OWNER_MISMATCH`: owner не авторизован active lease.
- `PLAYER_ACTOR_STILL_SEATED`: `Move` вызван до `StandUp`.
- `PLAYER_ACTOR_INTERACTION_TOO_FAR`: target вне reach.
- `PLAYER_ACTOR_INTERACTION_NO_LINE_OF_SIGHT`: нет server LOS до блока.
- `PLAYER_ACTOR_NAVIGATION_UNAVAILABLE`: Navigation bridge недоступен или не подтвердил preflight.

При terminal отказах adapter обязан не терять body и не менять GameMode.
