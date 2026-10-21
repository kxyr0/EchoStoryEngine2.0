# NPC Interaction Results

Status показывает стадию выполнения:

```text
CREATED
WAITING_FOR_MATERIALIZATION
VALIDATING_TARGET
SELECTING_PROVIDER
RESOLVING_APPROACH
WAITING_FOR_NAVIGATION
ALIGNING
INTERACTING
VERIFYING
COMPLETED
CANCELLED
FAILED
UNCERTAIN
RECOVERING
```

Outcome показывает итог:

```text
COMPLETED
CANCELLED
NPC_NOT_FOUND
NPC_NOT_MATERIALIZED
TARGET_INVALID
TARGET_REMOVED
TARGET_UNLOADED
NO_PROVIDER
PROVIDER_CONFLICT
NO_APPROACH
NAVIGATION_FAILED
ALIGNMENT_FAILED
TOO_FAR
NO_LINE_OF_SIGHT
INTERACTION_REJECTED
RESULT_UNVERIFIED
PLAYER_BOUND_UNSUPPORTED
TIMEOUT
INTERNAL_FAILURE
```

Status и outcome не смешиваются: status отвечает на вопрос "где команда сейчас",
outcome отвечает на вопрос "чем она закончилась".
# NI2 result evidence

Receipt revision 2 сохраняет:

- consequence kinds;
- proxy vehicle before/after;
- spawned entity IDs;
- ride handoff strategy;
- Echo seat anchor ID;
- seat occupancy ID;
- NPC passenger confirmation;
- proxy cleanup result;
- foreign vehicle cleanup result.

`MENU_OPENED`, `PROXY_RECEIVED_ITEM` без world-state эффекта и `PROXY_STATE_ONLY` возвращают typed failure, а не ложный success.
