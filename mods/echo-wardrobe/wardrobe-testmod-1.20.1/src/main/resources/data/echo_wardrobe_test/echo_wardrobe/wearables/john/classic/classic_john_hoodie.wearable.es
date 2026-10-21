namespace echo_wardrobe_test.wardrobe.john.classic;

class ClassicJohnHoodie : WearableResource
{
    private void Init()
    {
        Id("echo_wardrobe_test:classic_john_hoodie");
        Profile("echo_wardrobe_test:john");
        Item("echo_wardrobe_test:classic_john_hoodie");
        Slot(TOP);
        EquipmentSlot(CHEST);
        Collection(classic);
    }
}
