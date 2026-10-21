namespace echo_wardrobe_test.wardrobe.john.classic;

class ClassicJohnPants : WearableResource
{
    private void Init()
    {
        Id("echo_wardrobe_test:classic_john_pants");
        Profile("echo_wardrobe_test:john");
        Item("echo_wardrobe_test:classic_john_pants");
        Slot(BOTTOM);
        EquipmentSlot(LEGS);
        Collection(classic);
    }
}
