# Full skins in W2

W2 accepts full 64x64 PNG resources only as exact outfit output. It does not diff them against the base skin.

Canonical asset path:

```text
assets/<namespace>/textures/echo_wardrobe/skins/<collection>/<file>.png
```

Reference the asset without `textures/` and without `.png`:

```es
Skin("jdh:echo_wardrobe/skins/origin/jdh_full_dressed");
```

The outfit binder reports `WARDROBE_OUTFIT_SKIN_NOT_FOUND` when the PNG cannot be confirmed as an available 64x64 resource.
