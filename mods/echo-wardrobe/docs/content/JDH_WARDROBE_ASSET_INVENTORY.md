# JDH wardrobe asset inventory

Live scan target:

```text
C:/Users/kxyr0/AppData/Roaming/PrismLauncher/instances/Форма Мысли/minecraft/story/content-pack/jdh/assets/jdh/textures/echo_wardrobe
```

Observed PNG groups:

| Group | Count | Notes |
| --- | ---: | --- |
| `skins/classic` | 6 direct/grouped files | Classic John source skins. |
| `skins/classic/john_outfit` | 32 | Existing generated classic outfit candidates. |
| `skins/combo` | 70 | Existing generated exact-combination candidates. |
| `skins/origin` | 5 | Includes `jdh_full_dressed.png`, used by W2 origin outfit. |
| `wearables/*/textures` | 3 | Model/accessory texture assets, not outfit skins. |

Current W2 exact outfit references:

- `echo_content_player:player/base`
- `jdh:echo_wardrobe/skins/origin/jdh_full_dressed`

The existing `combo` and `classic` PNG sets can be promoted by adding one `OutfitVariantResource` per exact `OutfitKey`. They must not be consumed through runtime composition.
