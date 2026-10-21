# Wardrobe item binding

`WearableDefinition` maps a plain Minecraft item registry ID to a typed wardrobe selection.

```text
ItemStack
-> item registry ID string
-> WearableCatalog.activeByItemId
-> WardrobeSlot
-> WearableSelection
-> OutfitKey
```

Echo Wardrobe does not import Echo Item Pack. Future Item Pack items are visible through normal Forge/Minecraft registry IDs.

Unknown item IDs receive status `UNRESOLVED_ITEM`, emit `WARDROBE_ITEM_UNRESOLVED`, and do not participate in active resolution until the item exists in the registry.
