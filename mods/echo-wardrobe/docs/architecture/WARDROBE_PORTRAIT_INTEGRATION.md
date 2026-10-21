# Wardrobe portrait integration

Portrait consumers read the same neutral Content Player resolved appearance as the player renderer. Wardrobe does not publish a separate portrait-only layer stack.

W2 portrait-relevant data:

- `ResolvedOutfit.faceSkin`
- optional `WearableModelContribution` descriptors for rendered accessories
- `appearanceRevision` for invalidation

If `FaceSkin(...)` is not authored, `Skin(...)` sets `faceSkin` to the same full 64x64 PNG as `worldSkin`. Dialogue and portrait modules must not import Wardrobe implementation packages.
