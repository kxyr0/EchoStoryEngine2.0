# Echo Wardrobe W2 `.es` reference

## `WardrobeProfileResource`

Supported calls:

- `Id("namespace:profile")`
- `BaseSkin("namespace:path")`
- `Slots(FACE, TOP, BOTTOM, FEET)`
- `Coverage(REPORT | STRICT)`

## `WearableResource`

Supported calls:

- `Id("namespace:wearable")`
- `Profile("namespace:profile")`
- `Item("namespace:item")`
- `Slot(FACE | TOP | BOTTOM | FEET)`
- `EquipmentSlot(HEAD | CHEST | LEGS | FEET)`
- `Collection(origin)`
- `Model("namespace:item/model")`
- `ModelPosition(x, y, z)`
- `ModelRotation(pitch, yaw, roll)`
- `ModelScale(x, y, z)`

The slot and equipment slot must match: `FACE -> HEAD`, `TOP -> CHEST`, `BOTTOM -> LEGS`, `FEET -> FEET`.

## `OutfitVariantResource`

Supported calls:

- `Id("namespace:outfit")`
- `Profile("namespace:profile")`
- `Match(face: "...", top: "...", bottom: "...", feet: "...")`
- `Skin("namespace:path")`
- `WorldSkin("namespace:path")`
- `FaceSkin("namespace:path")`

Omitted `Match` named arguments are typed empty selections. The serialized key uses fixed order:

```text
profile|FACE=<id-or->|TOP=<id-or->|BOTTOM=<id-or->|FEET=<id-or->
```

`Skin(...)` sets both world and face skin to the same full PNG resource. Use `WorldSkin(...)` and `FaceSkin(...)` only when portrait/runtime resources intentionally differ.

Legacy skin calls are not W2 authoring API. They are rejected by the binder.
