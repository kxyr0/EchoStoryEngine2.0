# Reactive Geometry Triggers

Echo Trigger 0.3.0 adds callback-style resources for area membership changes.

Reactive resources can omit `Start(...)` when they declare at least one callback:

- `OnTriggerEnter()`, `OnTriggerEnter(PlayerRef player)`, or `OnTriggerEnter(TriggerEvent event)`
- `OnTriggerExit()`, `OnTriggerExit(PlayerRef player)`, or `OnTriggerExit(TriggerEvent event)`
- `OnTriggerStay()`, `OnTriggerStay(PlayerRef player)`, or `OnTriggerStay(TriggerEvent event)`

Each callback kind may have one method only. Overloads are rejected because dispatch would be ambiguous.

## Init Contract

Reactive `Init()` separates dimension from geometry:

```csharp
private TriggerArea House = BoxArea(Vector3(180, 64, 192), Vector3(188, 70, 200));

private void Init()
{
    Id("jdh:house_boundary");
    Scope(PLAYER);
    Dimension("minecraft:overworld");
    Bind(House);
}
```

One reactive resource may bind exactly one primary area. Missing `Dimension(...)`, missing `Bind(...)`, or multiple
`Bind(...)` calls are compile diagnostics.

## Shapes

Supported shapes:

- `BoxArea(min: Vector3(...), max: Vector3(...))`
- `SphereArea(center: Vector3(...), radius: 4)`
- `CylinderArea(center: Vector3(...), radius: 4, minY: 64, maxY: 70)`
- `CircleArea(Vector3(...), 4)` as a one-block-high cylinder alias
- `PolygonArea(points: [Vector3(...), Vector3(...), Vector3(...)], minY: 64, maxY: 70)`

`BoxArea` uses inclusive min and exclusive max. `CylinderArea` and `PolygonArea` use inclusive `minY` and exclusive
`maxY`. Polygon points must share `minY` and must not self-intersect.

## Policies

Default reactive mode is `REPEAT`.

- `FireOnlyOnce()` accepts the first callback for the definition and owner.
- `EnterOnlyOnce()`, `ExitOnlyOnce()`, and `StayOnlyOnce()` scope one-shot behavior to one callback kind.
- `FireCount(n)` caps total callback dispatches for the definition and owner.
- `Cooldown(Ticks(n))` suppresses callbacks until the cooldown expires.
- `StayInterval(Ticks(n))` defaults to 20 ticks.
- `StayPolicy(SKIP_WHILE_RUNNING)` is the default. `QUEUE_ONE` is accepted for future async executors.

`FireOnlyOnce()` conflicts with `FireCount(...)` and with explicit `Mode(REPEAT)`. `Mode(ONCE)` plus
`FireOnlyOnce()` is accepted.

## Actions

Callback bodies are compiled as neutral action descriptors. Echo Trigger does not link to Dialogue, Story, or Player
implementations. The Echo Dialogue adapter listens for public callback invocation events and handles
`Dialogue.Start("namespace:path")` through Echo Dialogue public service lookup.
