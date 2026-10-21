# Player Actor diagnostics

Diagnostics are stable, queryable and source-linked.

PA0 opening codes:

- `PLAYER_ACTOR_READY_BARRIER_UNAVAILABLE`
- `PLAYER_ACTOR_OPENING_ALREADY_COMPLETED`
- `PLAYER_ACTOR_OPENING_RECOVERY_FAILED`
- `PLAYER_ACTOR_SPAWN_POSITION_INVALID`
- `PLAYER_ACTOR_SPAWN_COLLISION`
- `PLAYER_ACTOR_SPAWN_DIMENSION_INVALID`
- `PLAYER_ACTOR_SPAWN_FAILED`
- `PLAYER_ACTOR_ITEM_NOT_FOUND`
- `PLAYER_ACTOR_ITEM_COUNT_INVALID`
- `PLAYER_ACTOR_HOTBAR_SLOT_INVALID`
- `PLAYER_ACTOR_HOTBAR_SLOT_OCCUPIED`
- `PLAYER_ACTOR_HOTBAR_UPDATE_FAILED`
- `PLAYER_ACTOR_HOTBAR_SELECTION_FAILED`
- `PLAYER_ACTOR_SEAT_NOT_FOUND`
- `PLAYER_ACTOR_SEAT_OCCUPIED`
- `PLAYER_ACTOR_ALREADY_SEATED_ELSEWHERE`
- `PLAYER_ACTOR_SEAT_PROVIDER_NOT_FOUND`
- `PLAYER_ACTOR_SEAT_PROVIDER_CONFLICT`
- `PLAYER_ACTOR_SIT_REJECTED`
- `PLAYER_ACTOR_SIT_TIMEOUT`
- `PLAYER_ACTOR_OPENING_DIALOGUE_FAILED`
- `PLAYER_ACTOR_COMMAND_IDEMPOTENCY_CONFLICT`

Runtime commands:

- `/echo_player_actor diagnostics`
- `/echo_player_actor inspect <player>`
- `/echo_player_actor opening status <player>`

Important counters:

- active actor commands;
- body transform updates;
- movement motor steps;
- collision checks;
- navigation route lookups;
- interaction dispatches;
- full player scans;
- body sync packets;
- parser calls during runtime command;
- force-loaded chunks.

PA1 target values: full player scans on normal tick = 0, EchoScript parser calls during runtime command = 0, force-loaded chunks = 0.
