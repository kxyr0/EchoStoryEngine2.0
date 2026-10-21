namespace echo_wardrobe_test.wardrobe.john.origin;

class OriginJohnRobe : WearableResource
{
    private void Init()
    {
        Id("echo_wardrobe_test:origin_john_robe");
        Profile("echo_wardrobe_test:john");
        Item("echo_wardrobe_test:origin_john_robe");
        Slot(TOP);
        EquipmentSlot(CHEST);
        Collection(origin);
    }
}
