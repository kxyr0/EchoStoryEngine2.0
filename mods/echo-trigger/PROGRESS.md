# Progress

- TV1 adds typed visibility subscription binding while concrete visibility-to-Story
  routing remains in the separate Echo Trigger Visibility Adapter.

- T1 foundation repository scaffolded.
- Platform-neutral contracts and core runtime are owned outside Forge.
- Dialogue integration is intentionally external to this repository.
- T2 contract/binder/runtime layer added for vanilla predicate/event expressions without Echo Item Pack dependency.
- Forge 1.20.1 provider bridges now emit generic vanilla gameplay events and predicate snapshots into the core
  runtime.
- Inventory/player/world baselines avoid synthetic edge events on first observation after recovery.
- T2 docs cover predicate/event recovery semantics, item IDs/tags and provider categories.
- T3 reactive callbacks, geometry shapes, fire policies, movement bridge, and callback invocation public API are in
  place.
- Echo Dialogue callback execution is externalized through the dialogue trigger adapter and public service lookup.
- `Npc.Move(...)` and `Npc.Stop(...)` are supported as generic reactive callback actions through the public callback
  invocation API; execution belongs to `echo-npc-navigation-adapter`.
## 0.5.3 backpack callback payloads

- Trigger callbacks preserve canonical `Position(x, y, z)` arguments for adapter-owned NPC backpack actions.
