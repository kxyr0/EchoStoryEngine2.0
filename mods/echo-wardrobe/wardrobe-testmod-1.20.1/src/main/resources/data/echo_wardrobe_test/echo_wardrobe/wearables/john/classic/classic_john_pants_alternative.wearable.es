namespace echo_wardrobe_test.wardrobe.john.classic;

class ClassicJohnPantsAlternative : WearableResource
{
    private void Init()
    {
        Id("echo_wardrobe_test:classic_john_pants_alternative");
        Profile("echo_wardrobe_test:john");
        Item("echo_wardrobe_test:classic_john_pants_alternative");
        Slot(BOTTOM);
        EquipmentSlot(LEGS);
        Collection(classic);
    }
}
