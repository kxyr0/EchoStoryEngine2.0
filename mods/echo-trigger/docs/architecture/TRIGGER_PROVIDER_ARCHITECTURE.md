# T2 Provider Architecture

T2 providers are data boundaries, not hardcoded adapters. EchoScript binds a compact expression into
`TriggerCondition(expressionKind, expressionName, arguments)`. Runtime consumes immutable `TriggerEvent` values from
provider capture code.

Predicate providers publish `PREDICATE_TRUE` only after reading current server-authoritative snapshots. Event providers
publish `VANILLA_GAMEPLAY` only after a real transition is captured after subscription.

Provider schema is versioned as schema 2 for T2 metadata. Recovery restores baselines and dirty revisions, then evaluates
predicates. It does not emit synthetic transition events.
