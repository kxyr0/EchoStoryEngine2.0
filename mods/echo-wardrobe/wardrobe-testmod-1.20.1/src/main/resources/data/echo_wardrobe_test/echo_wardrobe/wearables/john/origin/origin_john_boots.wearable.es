namespace echo_wardrobe_test.wardrobe.john.origin;

class OriginJohnBoots : WearableResource
{
    private void Init()
    {
        Id("echo_wardrobe_test:origin_john_boots");
        Profile("echo_wardrobe_test:john");
        Item("echo_wardrobe_test:origin_john_boots");
        Slot(FEET);
        EquipmentSlot(FEET);
        Collection(origin);
    }
}
