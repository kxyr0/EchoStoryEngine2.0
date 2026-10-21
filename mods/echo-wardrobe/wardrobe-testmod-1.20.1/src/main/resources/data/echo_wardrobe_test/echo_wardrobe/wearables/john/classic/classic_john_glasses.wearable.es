namespace echo_wardrobe_test.wardrobe.john.classic;

class ClassicJohnGlasses : WearableResource
{
    private void Init()
    {
        Id("echo_wardrobe_test:classic_john_glasses");
        Profile("echo_wardrobe_test:john");
        Item("echo_wardrobe_test:classic_john_glasses");
        Slot(FACE);
        EquipmentSlot(HEAD);
        Collection(classic);
        Model("echo_wardrobe_test:item/classic_john_glasses");
        ModelPosition(0, 0, 0);
        ModelRotation(0, 0, 0);
        ModelScale(1, 1, 1);
    }
}
