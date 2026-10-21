# Player appearance resolution

Echo Content Player owns the neutral appearance boundary. Wardrobe contributes a resolved outfit through a thin adapter; Content Player does not import Wardrobe.

W2 resolution flow:

```text
Minecraft equipment
-> item registry IDs
-> active WearableDefinition per slot
-> OutfitKey(profile, FACE, TOP, BOTTOM, FEET)
-> exact OutfitVariantDefinition lookup
-> ResolvedOutfit(worldSkin, faceSkin, models)
-> ResolvedPlayerAppearance contribution
```

Rules:

- No runtime skin composition.
- No overlay blending.
- No UV masks.
- No wildcard or closest match.
- Missing exact outfit means profile base skin fallback plus diagnostic.

Dialogue, Animation, and other consumers read Content Player's public resolved appearance, never Wardrobe implementation.
