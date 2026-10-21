# W2 migration report for existing JDH wardrobe files

Scanned live content pack:

```text
C:/Users/kxyr0/AppData/Roaming/PrismLauncher/instances/Форма Мысли/minecraft/story/content-pack/jdh
```

Canonical W2 scripts added:

- `echo_wardrobe/profiles/john.wardrobe-profile.es`
- `echo_wardrobe/wearables/john/origin/*.wearable.es`
- `echo_wardrobe/wearables/john/classic/*.wearable.es`
- `echo_wardrobe/outfits/john/base/john_base.outfit.es`
- `echo_wardrobe/outfits/john/origin_robe/john_full_dressed.outfit.es`

Legacy non-canonical scripts found and left untouched:

- `data/jdh/echo_wardrobe/wearables/classic_john_wardrobe_profile.es`
- `data/jdh/echo_wardrobe/wearables/origin_john_wardrobe_profile.es`
- `data/jdh/echo_wardrobe/wearables/classic/*.es`
- `data/jdh/echo_wardrobe/wearables/origin/*.es`
- `echo_wardrobe/wearables/classic_john_glasses.es`

Do not delete these automatically. They are authoring history/migration inventory until a human verifies whether their contents should become W2 profile, wearable, or outfit scripts.

Scripts under `assets/<namespace>/textures/...` are never canonical W2 content. If any appear there, report `WARDROBE_SCRIPT_IN_ASSET_FOLDER` and suggest the equivalent `echo_wardrobe/wearables/john/<collection>/*.wearable.es` path.
