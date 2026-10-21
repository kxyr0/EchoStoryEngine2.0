# Wardrobe runtime

Production host: `wardrobe-forge-1.20.1`.

Runtime classes:

- `EchoWardrobeMod` registers Forge entrypoint, commands, service lookup, and equipment observation.
- `ForgeWardrobeRuntimeState` stores accepted `WardrobeCatalog`, derived snapshots, diagnostics, and resolved outfits.
- `PlayerWardrobeSynchronizer` recomputes appearance from current Minecraft equipment.
- `ExactOutfitResolver` turns `EquipmentItemSnapshot` into `ResolvedOutfit`.
- `WardrobeCatalog` publishes immutable profile, wearable, and outfit catalogs.

Durable equipped storage is not owned by Wardrobe. Minecraft stores `ItemStack`; Wardrobe derives visual appearance from current registry IDs.
