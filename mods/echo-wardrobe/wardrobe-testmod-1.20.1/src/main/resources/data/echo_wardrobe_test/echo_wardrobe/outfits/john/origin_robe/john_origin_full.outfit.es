namespace echo_wardrobe_test.wardrobe.john.outfits.origin;

class JohnOriginFull : OutfitVariantResource
{
    private void Init()
    {
        Id("echo_wardrobe_test:john/full_dressed");
        Profile("echo_wardrobe_test:john");
        Match(
            face: "echo_wardrobe_test:origin_john_glasses",
            top: "echo_wardrobe_test:origin_john_robe",
            bottom: "echo_wardrobe_test:origin_john_pants",
            feet: "echo_wardrobe_test:origin_john_boots"
        );
        Skin("echo_wardrobe_test:echo_wardrobe/skins/origin/jdh_full_dressed");
    }
}
