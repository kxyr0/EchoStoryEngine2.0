namespace JDH.Chapter1.Home.Triggers;

class FatherVanishTrigger : TriggerResource
{
    private VisibilityWatchId FatherVanish = "jdh:home/father_vanish";
    private StoryId RemoveFather = "jdh:chapter_1/remove_father";

    private void Init()
    {
        Id("jdh:father_vanish_trigger");
        Subscribe(Player.LostSightOf(FatherVanish));
        StartStory(RemoveFather);
        FireOnlyOncePerArm();
    }
}
