# Changelog

## 0.5.1

- Load `.trigger.es` resources from both `story/triggers` and every
  `story/content-pack/**/echo_trigger/triggers` tree.

## 0.5.0

- Added declarative `Player.LostSightOf`, `Player.GainedSightOf`, and
  `Player.TargetIsOutOfView` visibility selectors.
- Added the `FireOnlyOncePerArm()` authoring spelling for visibility generation routing.

## 0.4.0

- Added declarative story subscription contracts: `TriggerEventSelector`, `TriggerSubscriptionId`,
  `TriggerStorySubscription`.
- Added `Subscribe(PlayerSleep.*)` plus `StartStory(...)` binding for callback-free sleep story routing.
- Added public Forge runtime lookup bridge under `forge.api.runtime` for adapters.
- Added subscription catalog view so adapters can build exact selector indexes.
- Added `/echo_trigger subscriptions ...` diagnostic commands for sleep subscription indexes and conflicts.
- Rejected manual player sleep callback authoring in Trigger resources.
- Extended `sourceShapeCheck` forbidden vague directory segments with PS1.1 names.
- Updated the EchoScript dependency to the sibling `0.1.1-SNAPSHOT` artifact so clean recompiles resolve locally.

## 0.3.1

- Added public authoring catalog/source contracts for Echo Debug.
- Added Forge authoring service lookup event and bridge.
- Added real parser/binder validation and canonical `<gameDir>/story/triggers` reload provider.
- Compatibility: T3 trigger semantics are unchanged.

## 0.3.0

- Added reactive `OnTriggerEnter`, `OnTriggerExit`, and `OnTriggerStay` callback resources.
- Added neutral `Vector3` geometry and `BoxArea`, `SphereArea`, `CylinderArea`, `CircleArea`, and `PolygonArea`.
- Added `Dimension(...)` plus single primary `Bind(...)` validation for reactive resources.
- Added callback signature validation for no args, `PlayerRef`, and `TriggerEvent`.
- Added fire policies: `FireOnlyOnce`, callback one-shot policies, `FireCount`, `Cooldown`, stay interval, and stay
  policy.
- Added callback invocation contracts and public Forge callback event for adapters.
- Added Forge player movement bridge and read-only trigger area debug view boundary.
- Compatibility: T1/T2 sequence `Start(...)` and `Wait(...)` resources keep compiling without changes.
- Breaking changes: none intended for T1/T2 content.

## 0.2.0

- Added the vanilla gameplay trigger expression catalog for inventory, hands, equipment, player state, effects,
  progression, dimensions, game mode, blocks, time, and weather.
- Added predicate/event categories so `HasItem(...)` style state checks remain distinct from `ItemAdded(...)`
  transition events.
- Added neutral item ID, item tag, item matcher, hand, equipment slot, game mode, and weather contract types.
- Added generic provider event payload matching for item IDs/tags, thresholds, slots, hands, dimensions, game modes,
  effects, blocks, time, and weather.
- Added Forge 1.20.1 vanilla event bridges for inventory deltas, hand/equipment changes, player state/lifecycle,
  effects, progression, dimension/game mode, block interaction, time, day, and weather events.
- Added recovery-safe baseline snapshots so first observed inventory/player/world state does not synthesize edge
  events after restart or reconnect.
- Added diagnostics for missing registries, invalid values, provider schema/recovery failures, and invalid event
  results.
- Updated Russian authoring and architecture documentation for the T2 vanilla provider pack.
- Compatibility: T1 `.trigger.es` content and spatial expressions keep compiling without changes.
- Breaking changes: none intended for T1 content.
## 0.5.3

- Preserve `Position(x, y, z)` callback arguments for external gameplay adapters.
