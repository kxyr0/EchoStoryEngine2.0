# Actor status policy

Resolvable:

- `ACTIVE`;
- `PLATFORM_MISSING`;
- `MATERIALIZED`;
- `DORMANT_CHUNK_UNLOADED`;
- `PENDING_SPAWN`;
- `PENDING_MOVE`.

Not claimed:

- `desiredPresence == ABSENT`.

Temporarily unavailable:

- `definitionCompatibility != COMPATIBLE`;
- `PENDING_REMOVAL`;
- `BLOCKED_DEFINITION_MISSING`;
- `BLOCKED_DEFINITION_INCOMPATIBLE`;
- `BLOCKED_WORLD_MISSING`.

Failure:

- `BLOCKED_CORRUPT_RECORD`;
- `PLATFORM_FAILURE`;
- `HOST_STOPPED`.

Both temporary unavailable and failure let Echo Dialogue use authored fallback.
