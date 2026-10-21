namespace echo_wardrobe_test.wardrobe.john.classic;

class ClassicJohnBoots : WearableResource
{
    private void Init()
    {
        Id("echo_wardrobe_test:classic_john_boots");
        Profile("echo_wardrobe_test:john");
        Item("echo_wardrobe_test:classic_john_boots");
        Slot(FEET);
        EquipmentSlot(FEET);
        Collection(classic);
    }
}
