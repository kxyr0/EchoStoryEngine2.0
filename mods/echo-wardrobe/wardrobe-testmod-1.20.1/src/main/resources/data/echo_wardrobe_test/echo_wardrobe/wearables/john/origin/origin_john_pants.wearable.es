namespace echo_wardrobe_test.wardrobe.john.origin;

class OriginJohnPants : WearableResource
{
    private void Init()
    {
        Id("echo_wardrobe_test:origin_john_pants");
        Profile("echo_wardrobe_test:john");
        Item("echo_wardrobe_test:origin_john_pants");
        Slot(BOTTOM);
        EquipmentSlot(LEGS);
        Collection(origin);
    }
}
