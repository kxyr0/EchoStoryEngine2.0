# Echo Wardrobe W2 quickstart

Canonical external scripts live under the content pack root, not under `assets`:

- `<gameDir>/story/content-pack/<namespace>/echo_wardrobe/profiles/`
- `<gameDir>/story/content-pack/<namespace>/echo_wardrobe/wearables/`
- `<gameDir>/story/content-pack/<namespace>/echo_wardrobe/outfits/`

PNG assets stay in the normal Minecraft resource tree:

- `assets/<namespace>/textures/echo_wardrobe/skins/...`
- `assets/<namespace>/textures/echo_wardrobe/wearables/textures/...` for model textures

```es
namespace jdh.wardrobe.profiles;

class JohnProfile : WardrobeProfileResource
{
    private void Init()
    {
        Id("jdh:john");
        BaseSkin("echo_content_player:player/base");
        Slots(FACE, TOP, BOTTOM, FEET);
        Coverage(REPORT);
    }
}
```

```es
namespace jdh.wardrobe.wearables.john.origin;

class OriginJohnGlasses : WearableResource
{
    private void Init()
    {
        Id("jdh:origin_john_glasses");
        Profile("jdh:john");
        Item("jdh:origin_john_glasses");
        Slot(FACE);
        EquipmentSlot(HEAD);
        Collection(origin);
        Model("jdh:item/origin_john_glasses");
        ModelPosition(0, 0, 0);
        ModelRotation(0, 0, 0);
        ModelScale(1, 1, 1);
    }
}
```

```es
namespace jdh.wardrobe.outfits.john.origin_robe;

class JohnFullDressed : OutfitVariantResource
{
    private void Init()
    {
        Id("jdh:john/full_dressed");
        Profile("jdh:john");
        Match(
            face: "jdh:origin_john_glasses",
            top: "jdh:origin_john_robe",
            bottom: "jdh:origin_john_pants",
            feet: "jdh:origin_john_boots"
        );
        Skin("jdh:echo_wardrobe/skins/origin/jdh_full_dressed");
    }
}
```

Every visible equipment combination needs its own exact `OutfitVariantResource`. There is no closest match. Use `/echo_wardrobe coverage` and `/echo_wardrobe missing` to find missing combinations.
