# Блоки

Events:

- `BlockInteracted(BlockIdOrTag, area: Area, hand: ANY_HAND)`
- `BlockBroken(BlockIdOrTag, area: Area)`
- `BlockPlaced(BlockIdOrTag, area: Area)`
- `ContainerOpened(BlockIdOrTag, area: Area)`
- `BedEntered()`, `BedLeft()`

`area` is optional. When present, block position must be inside the area and dimension must match; player position does
not have to be inside the area.

`BlockInteracted` defaults to `hand: ANY_HAND`. Duplicate Forge callbacks for one committed action are coalesced and
reported through `TRIGGER_BLOCK_EVENT_DUPLICATE_COALESCED`.
