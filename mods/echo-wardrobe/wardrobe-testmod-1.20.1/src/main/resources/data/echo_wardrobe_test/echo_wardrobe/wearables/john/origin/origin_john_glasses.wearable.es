namespace echo_wardrobe_test.wardrobe.john.origin;

class OriginJohnGlasses : WearableResource
{
    private void Init()
    {
        Id("echo_wardrobe_test:origin_john_glasses");
        Profile("echo_wardrobe_test:john");
        Item("echo_wardrobe_test:origin_john_glasses");
        Slot(FACE);
        EquipmentSlot(HEAD);
        Collection(origin);
        Model("echo_wardrobe_test:item/origin_john_glasses");
        ModelPosition(0, 0, 0);
        ModelRotation(0, 0, 0);
        ModelScale(1, 1, 1);
    }
}
