# Echo Wardrobe project map

## Modules

| Module | Responsibility | Public API | Thread ownership | Dependencies | Forbidden dependencies | Production JAR |
| --- | --- | --- | --- | --- | --- | --- |
| `wardrobe-contracts` | typed IDs, slots, definitions, outfits, contributions, diagnostics | `io.github.kxyr0.echowardrobe.api` | immutable data | Java | Minecraft, Forge, Dialogue, Animation, Item Pack | embedded |
| `wardrobe-core` | profile/wearable/outfit binders, exact catalogs, resolver, coverage, migration inventory | core classes | worker/server/client by caller | contracts, EchoScript API | Minecraft, Forge, Dialogue, Animation, Item Pack | embedded |
| `wardrobe-forge-api-1.20.1` | query-only Forge-facing views | `EchoWardrobeService` | caller-owned | contracts | implementation repos | embedded |
| `wardrobe-forge-1.20.1` | Forge entrypoint, commands, equipment observation | production mod | server/client event threads | Forge, Minecraft, Content Player mod dependency, own modules | Item Pack, Dialogue, Animation | yes |
| `wardrobe-content-player-adapter-1.20.1` | contributes Wardrobe resolved outfit to Content Player appearance registry | Content Player public API | Forge event thread | Wardrobe Forge API, Content Player Forge API | implementation repos | yes |
| `wardrobe-testmod-1.20.1` | development fixtures | none for production | dev only | Forge, own modules | production inclusion | no |

## Equipment flow

```text
Minecraft ItemStack
-> Item ID
-> WearableDefinition
-> WardrobeSlot
-> OutfitKey
-> exact OutfitVariantDefinition
-> ResolvedOutfit
```

## Skin flow

```text
OutfitVariantResource
-> full 64x64 PNG
-> ResolvedOutfit.worldSkin/faceSkin
-> Content Player resolved appearance
-> player renderer / portrait / animation face consumers
```

## Glasses flow

```text
HEAD ItemStack
-> FACE wearable
-> OutfitKey FACE selection
-> exact full skin containing glasses pixels
-> optional rendered model descriptor
```

## Dependency map

```text
Echo Wardrobe
-> Echo Content Player public/runtime dependency

Dialogue
-> Content Player resolved appearance

Animation
-> Content Player resolved appearance
```

No Dialogue, Animation, or Item Pack hard dependency exists in Wardrobe production code.

## Asset flow

```text
full skin
-> exact outfit resource ID
-> availability check
-> catalog entry
```
