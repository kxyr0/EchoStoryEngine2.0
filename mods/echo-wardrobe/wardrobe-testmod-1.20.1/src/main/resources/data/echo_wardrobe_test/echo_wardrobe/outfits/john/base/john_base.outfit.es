namespace echo_wardrobe_test.wardrobe.john.outfits.base;

class JohnBase : OutfitVariantResource
{
    private void Init()
    {
        Id("echo_wardrobe_test:john/base");
        Profile("echo_wardrobe_test:john");
        Match();
        Skin("echo_content_player:player/base");
    }
}
