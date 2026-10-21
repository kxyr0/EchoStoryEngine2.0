# T2 Vanilla Reference

Predicate checks current state. Event captures a transition after subscription. Recovery may reevaluate predicates but
must not synthesize events.

| expression | kind | category | arguments | payload |
| --- | --- | --- | --- | --- |
| `HasItem` | predicate | inventory | target, count optional | inventory revision, count |
| `ItemAdded` | event | inventory | target, count optional | item, tags, count delta |
| `HoldingMainHand` | predicate | hands | target | hand item/count |
| `MainHandChanged` | event | hands | from/to optional | previous/current item/count |
| `Wearing` | predicate | equipment | slot, target | equipment revision |
| `EquipmentEquipped` | event | equipment | slot, target | previous/current item |
| `HealthAtMost` | predicate | player | value | health |
| `PlayerDamaged` | event | player | source, amountAtLeast optional | source, amount, health before/after |
| `HasEffect` | predicate | effects | effect id | amplifier, duration |
| `EffectAdded` | event | effects | effect id, amplifierAtLeast optional | previous/new amplifier |
| `AdvancementCompleted` | predicate | progression | advancement id | advancement revision |
| `AdvancementGranted` | event | progression | advancement id | advancement id |
| `InDimension` | predicate | world | dimension id | dimension |
| `DimensionEntered` | event | world | dimension id | previous/current dimension |
| `BlockInteracted` | event | blocks | block/tag, area, hand | block, position, hand |
| `TimeReached` | event | time | day time | world tick |
| `WeatherChanged` | event | weather | to optional | previous/current weather |
