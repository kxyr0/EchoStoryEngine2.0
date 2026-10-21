# Scope Firewall

Echo Story Engine owns only story definition, validation, execution, persistence through ports, and diagnostics.

It may know abstract namespaced action, condition, event, value, command, operation, wait, and diagnostic concepts.
It must not know what an adapter does with those abstractions in Minecraft.

## Required Questions

Before adding a feature, answer:

1. Is it needed to define, validate, execute, persist, or diagnose a story?
2. Can it exist as a standalone gameplay system without Story Engine?
3. Does it mention a concrete Minecraft mechanic, entity, third-party mod, or gameplay domain?

If answer 2 or 3 is yes, implementation does not belong in story core.

## Required Response To Scope Violations

- Stop implementation inside core.
- Name the boundary violation.
- Propose a platform adapter or integration module.
- Name only the minimal SPI contract that Story Engine needs.
- Do not move gameplay code into Story Engine temporarily.

## Forbidden Domains

Forbidden package or feature meanings include NPCs, actors as entities, navigation, pathfinding, roads, traffic,
vehicles, weapons, combat, Epic Fight, magic, animation, cinematics, cameras, doors, quest items, worldgen,
biomes, sound, concrete dialog UI, concrete Minecraft entities, and direct third-party mod integrations.

## Forbidden Bucket Names

Do not create packages or types with vague bucket meanings such as `utils`, `helpers`, `misc`, `common`,
`global`, `everything`, `Manager`, `Helper`, `Utils`, `Common`, `GlobalService`, or `Impl`.

An exception needs the procedure in `ARCHITECTURE_GUARDRAILS.md`.
