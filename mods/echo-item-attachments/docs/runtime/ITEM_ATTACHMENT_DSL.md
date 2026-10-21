# Item attachment Story EchoScript DSL

These calls are compiled by the normal Story EchoScript binder into recoverable asynchronous
actions. They block until the server command reaches a terminal result.

```csharp
Npc.TakeItem(Molly, BookPosition, hand: RIGHT_HAND,
    expected: "minecraft:book");
Npc.TakeItem(Molly, BookPosition, hand: RIGHT_HAND,
    expected: "minecraft:book", animation: TakeBook);
Npc.MoveAttachedItem(Molly, from: RIGHT_HAND, to: LEFT_HAND,
    animation: SwapBook);
Npc.DropItem(Molly, hand: LEFT_HAND);
Npc.ThrowItem(Molly, hand: LEFT_HAND,
    animation: ThrowBook, release: HAND_MOTION);
```

```csharp
Player.TakeItem(BookPosition, hand: RIGHT_HAND,
    expected: "minecraft:book", animation: TakeBook);
Player.MoveAttachedItem(from: RIGHT_HAND, to: LEFT_HAND);
Player.DropItem(hand: LEFT_HAND);
Player.ThrowItem(hand: LEFT_HAND, release: HAND_MOTION);
```

`BookPosition` must resolve to `Position(x,y,z)`. `Molly` must resolve to an Actor ID or is
normalized to `jdh:molly`. The Player calls require the execution variable `player` to contain
the explicit UUID and require an authorizing active Player Actor control lease. Missing player
context is an error; no online-player, nearest-player, or random-player fallback exists.

Optional take arguments are `expected`, `count` (default 1), `radius` (maximum/default 0.75),
`dimension`, and `animation`. Release supports `release` and `speed`.
