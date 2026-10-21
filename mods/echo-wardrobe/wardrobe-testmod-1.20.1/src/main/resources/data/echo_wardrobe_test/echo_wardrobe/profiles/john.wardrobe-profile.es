namespace echo_wardrobe_test.wardrobe.profiles;

class JohnWardrobe : WardrobeProfileResource
{
    private void Init()
    {
        Id("echo_wardrobe_test:john");
        BaseSkin("echo_content_player:player/base");
        Slots(FACE, TOP, BOTTOM, FEET);
        Coverage(REPORT);
    }
}
