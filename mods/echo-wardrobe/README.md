# Echo Wardrobe

Echo Wardrobe is the W2 exact outfit resolver for Echo Content Player.

Runtime appearance is resolved by exact key:

```text
Minecraft equipment -> item IDs -> WearableDefinition -> OutfitKey
-> exact OutfitVariantDefinition -> full 64x64 PNG -> ResolvedOutfit
```

There is no runtime skin composition, overlay blending, UV mask extraction, wildcard match, or closest-match fallback. If an exact outfit variant is absent, the player falls back to the profile base skin and emits `WARDROBE_OUTFIT_VARIANT_NOT_FOUND`.

Production code does not register clothing items and does not depend on Echo Item Pack, Echo Dialogue, Echo Animation, Story Engine, Echo NPC, or Echo Trigger implementation modules. Development fixtures live only in `wardrobe-testmod-1.20.1`.

## Modules

- `wardrobe-contracts`: immutable public contracts.
- `wardrobe-core`: plain Java profile/wearable/outfit binders, exact catalogs, resolver, coverage, reload, and migration inventory.
- `wardrobe-forge-api-1.20.1`: public Forge service/query boundary.
- `wardrobe-forge-1.20.1`: production Forge mod host.
- `wardrobe-content-player-adapter-1.20.1`: thin adapter that publishes Wardrobe resolved appearance to Echo Content Player's public appearance boundary.
- `wardrobe-testmod-1.20.1`: development-only test items and fixtures.

## Build

```powershell
.\gradlew.bat check
```
