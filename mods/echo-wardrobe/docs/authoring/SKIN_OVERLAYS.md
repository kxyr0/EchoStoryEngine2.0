# Removed W1 skin overlay model

W2 does not load or compose skin overlays at runtime. The resolver never blends layers, never applies UV masks, and never generates composed skins.

Use `OutfitVariantResource` instead. Each authored equipment combination points to a full 64x64 PNG:

```es
Match(
    face: "jdh:origin_john_glasses",
    top: "jdh:origin_john_robe",
    bottom: "jdh:origin_john_pants",
    feet: "jdh:origin_john_boots"
);
Skin("jdh:echo_wardrobe/skins/origin/jdh_full_dressed");
```

Old scripts under asset folders are treated as migration inventory only. They are not canonical authoring input.
